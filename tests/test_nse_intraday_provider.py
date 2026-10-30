from datetime import date
import pandas as pd
import pytest
from app.auto_history import fetch_nse_candles


def test_nse_intraday_provider_loads_real_csv_and_filters_dates(tmp_path, monkeypatch):
    f = tmp_path / 'nifty_5m.csv'
    f.write_text(
        'timestamp,open,high,low,close\n'
        '2026-10-26 09:15:00,25000,25010,24995,25005\n'
        '2026-10-27 09:15:00,25005,25020,25000,25015\n'
        '2026-10-27 09:20:00,25015,25025,25010,25020\n'
        '2026-10-28 09:15:00,25020,25025,25010,25015\n', encoding='utf-8'
    )
    monkeypatch.setenv('NSE_5M_CSV_DIR', str(tmp_path))
    candles, source = fetch_nse_candles(date(2026, 10, 27), date(2026, 10, 27))
    assert len(candles) == 2
    assert candles.iloc[0]['close'] == 25015
    assert 'Authorized NSE five-minute CSV exports' in source


def test_nse_intraday_provider_does_not_fabricate_when_no_source(tmp_path, monkeypatch):
    missing = tmp_path / 'missing'
    monkeypatch.setenv('NSE_5M_CSV_DIR', str(missing))
    with pytest.raises(RuntimeError, match='no official five-minute candle files'):
        fetch_nse_candles(date(2024, 1, 1), date(2026, 1, 1))


def test_nse_intraday_provider_rejects_daily_only_csv(tmp_path, monkeypatch):
    (tmp_path / 'daily.csv').write_text(
        'date,open,high,low,close\n2026-10-27,25000,25100,24900,25050\n', encoding='utf-8'
    )
    monkeypatch.setenv('NSE_5M_CSV_DIR', str(tmp_path))
    with pytest.raises(RuntimeError, match='No valid five-minute candles'):
        fetch_nse_candles(date(2026, 10, 27), date(2026, 10, 27))


def test_auto_run_reports_nse_intraday_limit_without_kite_credentials(tmp_path, monkeypatch):
    import app.auto_history as auto_module
    from fastapi.testclient import TestClient
    from app.main import app

    monkeypatch.setenv('NSE_5M_CSV_DIR', str(tmp_path / 'not-configured'))
    monkeypatch.setattr(auto_module, 'build_daily_levels', lambda start, end: (
        pd.DataFrame([{'date': start.isoformat(), 'S1': 24900, 'R1': 25100, 'S2': 24800, 'R2': 25200}]), []
    ))
    response = TestClient(app).post('/api/historical-sr/auto-run', data={
        'start_date': '2026-10-27', 'end_date': '2026-10-27',
        'touch_tolerance': '2', 'reversal_points': '15', 'confirmation_candles': '3',
        'failed_break_candles': '6', 'reset_points': '10'
    })
    assert response.status_code == 502
    assert 'NSE-only mode' in response.json()['detail']
    assert 'No candles were downloaded or fabricated' in response.json()['detail']
    assert 'KITE_API_KEY' not in response.json()['detail']
