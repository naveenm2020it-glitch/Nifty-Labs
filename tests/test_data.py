from datetime import date
from io import BytesIO
import pandas as pd
from app.data import parse_bhavcopy_bytes, filter_session, select_strikes, build_dashboard_rows, support_resistance
from app.main import _query

def sample_csv():
    return b"""BizDt,Sgmt,TckrSymb,FinInstrmTp,XpryDt,StrkPric,OptnTp,OpnPric,HghPric,LwPric,ClsPric,LastPric,SttlmPric,OpnIntrst,ChngInOpnIntrst,TtlTradgVol,UndrlygPric
2026-10-13,FO,NIFTY,OPTIDX,2026-10-13,25000,CE,100,120,90,110,111,112,1000,50,200,25020
2026-10-13,FO,NIFTY,OPTIDX,2026-10-13,25000,PE,90,100,80,95,96,97,1200,-20,300,25020
2026-10-13,FO,NIFTY,OPTIDX,2026-10-20,25000,CE,200,220,190,210,211,212,1000,50,200,25020
2026-10-14,FO,NIFTY,OPTIDX,2026-10-13,25000,CE,1,2,1,1,1,1,10,0,1,25050
"""

def test_parse_udiff_and_exact_session_expiry():
    df, _ = parse_bhavcopy_bytes(sample_csv(), "fixture.csv")
    result = filter_session(df, date(2026,10,13), date(2026,10,13))
    assert len(result) == 2
    assert set(result["option_type"]) == {"CE", "PE"}
    assert set(result["expiry"]) == {date(2026,10,13)}

def test_strikes_and_bep():
    df, _ = parse_bhavcopy_bytes(sample_csv(), "fixture.csv")
    result = filter_session(df, date(2026,10,13), date(2026,10,13))
    rows = build_dashboard_rows(result, [25000, 25050])
    assert rows[0]["bep"] == 102.5
    assert rows[1]["bep"] is None
    strikes, atm = select_strikes(result, 25020, 50, 1, 1, "nearest50")
    assert atm == 25000
    assert strikes == [24950, 25000, 25050]

def test_support_resistance():
    got = support_resistance(25000, 110, 95)
    assert got == {"S1": 24905.0, "R1": 25110.0, "S2": 24795.0, "R2": 25205.0}

def test_legacy_columns_are_mapped():
    payload = b"""TIMESTAMP,INSTRUMENT,SYMBOL,EXPIRY_DT,STRIKE_PR,OPTION_TYP,OPEN,HIGH,LOW,CLOSE,LTP,SETTLE_PR,CONTRACTS,OPEN_INT,CHG_IN_OI,UNDERLYING_VALUE
13-Oct-2026,OPTIDX,NIFTY,13-Oct-2026,25000,CE,100,120,90,110,111,112,200,1000,50,25020
"""
    df, _ = parse_bhavcopy_bytes(payload, "legacy.csv")
    result = filter_session(df, date(2026,10,13), date(2026,10,13))
    assert len(result) == 1
    assert result.iloc[0]["close"] == 110

def test_udiff_ido_instrument_is_recognized_as_index_option():
    payload = b"""BizDt,Sgmt,TckrSymb,FinInstrmTp,XpryDt,StrkPric,OptnTp,OpnPric,HghPric,LwPric,ClsPric,LastPric,SttlmPric,OpnIntrst,ChngInOpnIntrst,TtlTradgVol,UndrlygPric
2026-10-13,FO,NIFTY,IDO,2026-10-20,25000,CE,100,120,90,110,111,112,1000,50,200,25020
2026-10-13,FO,NIFTY,IDO,2026-10-20,25000,PE,90,100,80,95,96,97,1200,-20,300,25020
"""
    df, _ = parse_bhavcopy_bytes(payload, "udiff_ido.csv")
    result = filter_session(df, date(2026, 10, 13), date(2026, 10, 20), "NIFTY")
    assert len(result) == 2
    assert set(result["instrument"]) == {"IDO"}


def test_numeric_yyyymmdd_dates_are_parsed_exactly():
    payload = b"""TRADE_DATE,SYMBOL,EXPIRY_DATE,STRIKE_PRICE,OPTION_TYPE,CLOSE_PRICE,UNDERLYING_VALUE
20261013,NIFTY,20261020,25000,CE,110,25020
20261013,NIFTY,20261020,25000,PE,95,25020
"""
    df, _ = parse_bhavcopy_bytes(payload, "compact_dates.csv")
    result = filter_session(df, date(2026, 10, 13), date(2026, 10, 20), "NIFTY")
    assert len(result) == 2


def test_nifty_formula_outputs_are_correct():
    df, _ = parse_bhavcopy_bytes(sample_csv(), "nifty.csv")
    out = _query(
        date(2026, 10, 13), date(2026, 10, 13), "NIFTY", df, "synthetic test fixture",
        25000.0, 50, 0, 0, "manual", 25000.0, "atm", "atm", None, None,
    )
    assert out["symbol"] == "NIFTY"
    assert out["index_close"] == 25000
    assert out["rows"][0]["bep"] == 102.5
    assert out["bep_analysis"][0]["strike"] == 25000.0
    assert out["bep_analysis"][0]["ce_close"] == 110.0
    assert out["bep_analysis"][0]["pe_close"] == 95.0
    assert out["bep_analysis"][0]["bep"] == 102.5
    assert out["bep_analysis"][0]["status"] == "calculated"
    assert out["support_resistance"] == {"S1": 24905.0, "R1": 25110.0, "S2": 24795.0, "R2": 25205.0}
    assert out["ce_reference"] == 110.0
    assert out["pe_reference"] == 95.0


def test_hlc_atm_selects_minimum_absolute_ce_pe_close_difference():
    payload = b"""BizDt,Sgmt,TckrSymb,FinInstrmTp,XpryDt,StrkPric,OptnTp,OpnPric,HghPric,LwPric,ClsPric,LastPric,SttlmPric,OpnIntrst,ChngInOpnIntrst,TtlTradgVol,UndrlygPric
2026-10-13,FO,NIFTY,OPTIDX,2026-10-20,24950,CE,105,115,95,100,100,100,1000,10,100,25020
2026-10-13,FO,NIFTY,OPTIDX,2026-10-20,24950,PE,100,110,90,100,100,100,1000,10,100,25020
2026-10-13,FO,NIFTY,OPTIDX,2026-10-20,25000,CE,110,120,100,110,110,110,1000,10,100,25020
2026-10-13,FO,NIFTY,OPTIDX,2026-10-20,25000,PE,100,110,90,100,100,100,1000,10,100,25020
2026-10-13,FO,NIFTY,OPTIDX,2026-10-20,25050,CE,125,135,115,125,125,125,1000,10,100,25020
2026-10-13,FO,NIFTY,OPTIDX,2026-10-20,25050,PE,65,75,55,65,65,65,1000,10,100,25020
"""
    df, _ = parse_bhavcopy_bytes(payload, "hlc_atm.csv")
    session = filter_session(df, date(2026, 10, 13), date(2026, 10, 20), "NIFTY")
    strikes, atm = select_strikes(session, 25020, 50, 1, 1, "hlc_min_diff")
    assert atm == 24950
    assert strikes == [24900.0, 24950.0, 25000.0]

    out = _query(date(2026, 10, 13), date(2026, 10, 20), "NIFTY", df, "fixture", 25020.0,
                 50, 1, 1, "hlc_min_diff", None, "atm", "atm", None, None)
    assert out["hlc_atm"]["strike"] == 24950
    assert out["hlc_atm"]["close_difference"] == 0
    assert out["hlc_atm"]["ce"] == {"open": 105.0, "high": 115.0, "low": 95.0, "close": 100.0}
    assert out["hlc_atm"]["pe"] == {"open": 100.0, "high": 110.0, "low": 90.0, "close": 100.0}

def test_hlc_atm_minimum_difference_is_not_limited_to_spot_centred_window():
    rows = []
    # Near spot, the smallest difference is 10; a farther available strike has difference 1.
    for strike, ce, pe in [(25000, 100, 90), (25050, 110, 95), (25200, 80, 79)]:
        rows.extend([
            {"strike": float(strike), "option_type": "CE", "close": float(ce)},
            {"strike": float(strike), "option_type": "PE", "close": float(pe)},
        ])
    df = pd.DataFrame(rows)
    _, atm = select_strikes(df, 25000, 50, 1, 1, "hlc_min_diff")
    assert atm == 25200.0


def test_support_resistance_uses_close_prices_on_selected_hlc_atm():
    df, _ = parse_bhavcopy_bytes(sample_csv(), "hlc_references.csv")
    out = _query(
        date(2026, 10, 13), date(2026, 10, 13), "NIFTY", df, "fixture",
        25000.0, 50, 0, 0, "manual", 25000.0, "hlc", "hlc", None, None,
    )
    sr = out["support_resistance"]
    assert sr == {"S1": 24905.0, "R1": 25110.0, "S2": 24795.0, "R2": 25205.0}
    assert out["ce_reference"] == 110.0
    assert out["pe_reference"] == 95.0
