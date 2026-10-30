from fastapi.testclient import TestClient
import app.main as main_module
from app.data import parse_bhavcopy_bytes
from app.main import app


def csv_for(expiry: str, symbol: str = "NIFTY") -> bytes:
    return f"""BizDt,Sgmt,TckrSymb,FinInstrmTp,XpryDt,StrkPric,OptnTp,OpnPric,HghPric,LwPric,ClsPric,LastPric,SttlmPric,OpnIntrst,ChngInOpnIntrst,TtlTradgVol,UndrlygPric
2026-10-22,FO,{symbol},OPTIDX,{expiry},25000,CE,100,120,90,110,111,112,1000,50,200,25020
2026-10-22,FO,{symbol},OPTIDX,{expiry},25000,PE,90,100,80,95,96,97,1200,-20,300,25020
""".encode()


def test_api_fallback_runs_when_archive_has_no_exact_expiry(monkeypatch):
    archive_df, _ = parse_bhavcopy_bytes(csv_for("2026-10-20"), "archive.csv")
    api_df, _ = parse_bhavcopy_bytes(csv_for("2026-10-27"), "api_fixture.csv")
    monkeypatch.setattr(main_module, "fetch_official_bhavcopy", lambda trade_date: (archive_df, "fixture archive"))
    monkeypatch.setattr(main_module, "fetch_nifty_close_api", lambda trade_date, symbol: (25020.0, "fixture close"))
    monkeypatch.setattr(main_module, "fetch_contract_api", lambda trade_date, expiry_date, strikes, symbol: (api_df, "fixture historical API"))
    client = TestClient(app)
    response = client.post("/api/fetch", data={
        "trade_date": "2026-10-22", "expiry_date": "2026-10-27", "symbol": "NIFTY",
        "interval": "50", "itm_count": "0", "otm_count": "0", "atm_method": "nearest100",
        "ce_reference_mode": "atm", "pe_reference_mode": "atm"
    })
    assert response.status_code == 200, response.text
    result = response.json()
    assert result["source"].startswith("NSE historical API fallback")
    assert result["trade_date"] == "2026-10-22"
    assert result["expiry_date"] == "2026-10-27"
    assert result["rows"][0]["bep"] == 102.5
    assert result["bep_analysis"][0]["bep"] == 102.5
    assert result["bep_analysis"][0]["formula"] == "(CE Close + PE Close) / 2"


def test_non_nifty_indices_are_rejected():
    client = TestClient(app)
    response = client.post("/api/fetch", data={
        "trade_date": "2026-10-22", "expiry_date": "2026-10-27", "symbol": "BANKNIFTY",
        "interval": "50", "itm_count": "0", "otm_count": "0", "atm_method": "nearest100",
    })
    assert response.status_code == 400
    assert "NIFTY 50 only" in response.text


def test_nse_expiry_auto_discovery_reads_selected_session_archive(monkeypatch):
    archive_df, _ = parse_bhavcopy_bytes(
        csv_for("2026-10-27").replace(b"OPTIDX", b"IDO") + csv_for("2026-11-03").replace(b"2026-10-27", b"2026-11-03").replace(b"OPTIDX", b"IDO"),
        "expiry_fixture.csv",
    )
    monkeypatch.setattr(main_module, "fetch_official_bhavcopy", lambda trade_date: (archive_df, "fixture archive"))
    client = TestClient(app)
    response = client.get("/api/expiries", params={"trade_date": "2026-10-22", "symbol": "NIFTY"})
    assert response.status_code == 200, response.text
    assert response.json()["expiries"] == ["2026-10-27", "2026-11-03"]


