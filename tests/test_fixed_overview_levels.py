from fastapi.testclient import TestClient
import app.auto_history as auto_module
from app.main import app


def test_fixed_level_endpoint_uses_exact_levels_and_reports_fake_breakout():
    day = "2026-10-27"
    csv = (
        "timestamp,open,high,low,close\n"
        f"{day} 09:15:00,98,101,97,99\n"
        f"{day} 09:20:00,99,104,98,103\n"
        f"{day} 09:25:00,103,105,99,100\n"
    ).encode()
    client = TestClient(app)
    response = client.post(
        "/api/historical-sr/fixed-level-analysis",
        data={
            "start_date": day,
            "end_date": day,
            "s1": "80",
            "s2": "70",
            "r1": "100",
            "r2": "120",
            "touch_tolerance": "2",
            "reversal_points": "15",
            "confirmation_candles": "3",
            "failed_break_candles": "6",
            "reset_points": "10",
        },
        files={"candles_file": ("nifty_5m.csv", csv, "text/csv")},
    )
    assert response.status_code == 200, response.text
    result = response.json()
    assert result["analysis_mode"] == "fixed_overview_levels"
    assert result["fixed_levels"] == {"S1": 80.0, "S2": 70.0, "R1": 100.0, "R2": 120.0}
    assert result["candles_analyzed"] == 3
    assert result["counts"]["R1"]["touches"] == 1
    assert result["counts"]["R1"]["fake_breakout"] == 1
    assert result["counts"]["R1"]["fake_breakout_pct"] == 100.0
    assert result["events"][0]["event_type"] == "Fake Breakout"


def test_fixed_level_endpoint_returns_clear_error_without_candles(monkeypatch):
    def no_candles(start, end):
        raise RuntimeError("No valid five-minute spot CSV files were found.")
    monkeypatch.setattr(auto_module, "fetch_nse_candles", no_candles)
    client = TestClient(app)
    response = client.post(
        "/api/historical-sr/fixed-level-analysis",
        data={
            "start_date": "2026-10-27",
            "end_date": "2026-10-27",
            "s1": "80", "s2": "70", "r1": "100", "r2": "120",
        },
    )
    assert response.status_code in (422, 502)
    assert "candle" in response.json()["detail"].lower() or "csv" in response.json()["detail"].lower()


def test_fixed_level_background_job_returns_event_tables(monkeypatch, tmp_path):
    """The browser's one-click workflow returns a completed result through job polling."""
    day = "2026-10-27"
    csv = (
        "timestamp,open,high,low,close\n"
        f"{day} 09:15:00,98,101,97,99\n"
        f"{day} 09:20:00,99,104,98,103\n"
        f"{day} 09:25:00,103,105,99,100\n"
    ).encode()
    client = TestClient(app)
    response = client.post(
        "/api/historical-sr/fixed-level-analysis/start",
        data={
            "start_date": day, "end_date": day,
            "s1": "80", "s2": "70", "r1": "100", "r2": "120",
            "touch_tolerance": "2", "reversal_points": "15",
            "confirmation_candles": "3", "failed_break_candles": "6", "reset_points": "10",
        },
        files={"candles_file": ("nifty_5m.csv", csv, "text/csv")},
    )
    assert response.status_code == 200, response.text
    job_id = response.json()["job_id"]
    status = client.get(f"/api/historical-sr/fixed-level-analysis/status/{job_id}")
    assert status.status_code == 200, status.text
    payload = status.json()
    assert payload["status"] == "complete"
    assert payload["progress"] == 100
    assert payload["result"]["fixed_levels"] == {"S1": 80.0, "S2": 70.0, "R1": 100.0, "R2": 120.0}
    assert payload["result"]["counts"]["R1"]["fake_breakout"] == 1
    assert payload["result"]["events"][0]["event_type"] == "Fake Breakout"


def test_fixed_level_background_job_reports_missing_intraday_archive(monkeypatch, tmp_path):
    import app.auto_history as auto_module

    def no_candles(start, end):
        raise RuntimeError("No genuine five-minute candles in the configured archive.")

    monkeypatch.setattr(auto_module, "fetch_nse_candles", no_candles)
    client = TestClient(app)
    response = client.post(
        "/api/historical-sr/fixed-level-analysis/start",
        data={"start_date": "2026-10-27", "end_date": "2026-10-27",
              "s1": "80", "s2": "70", "r1": "100", "r2": "120"},
    )
    assert response.status_code == 200, response.text
    status = client.get(f"/api/historical-sr/fixed-level-analysis/status/{response.json()['job_id']}")
    assert status.json()["status"] == "error"
    assert "five-minute candles" in status.json()["error"].lower()
