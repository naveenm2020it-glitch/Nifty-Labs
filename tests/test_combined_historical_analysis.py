from datetime import date
import pandas as pd
from fastapi.testclient import TestClient

import app.auto_history as auto_module
from app.main import app


def _level_row(day='2026-10-27'):
    return pd.DataFrame([{
        'date': day, 'S1': 24990.0, 'R1': 25010.0, 'S2': 24980.0, 'R2': 25020.0,
        'atm_strike': 25000.0, 'expiry': '2026-10-29', 'ce_close': 10.0, 'pe_close': 10.0,
        'level_source_date': '2026-10-26', 'source': 'test NSE fixture'
    }])


def test_combined_analysis_returns_daily_levels_when_five_minute_data_missing(monkeypatch, tmp_path):
    monkeypatch.setenv('CACHE_DB', str(tmp_path / 'cache.sqlite3'))
    monkeypatch.setattr(auto_module, 'build_daily_levels', lambda start, end, progress_callback=None: (_level_row(start.isoformat()), []))
    def no_candles(start, end):
        raise RuntimeError('No official five-minute candle files found for requested range.')
    monkeypatch.setattr(auto_module, 'fetch_nse_candles', no_candles)

    client = TestClient(app)
    start = client.post('/api/historical-sr/analyze/start', data={
        'start_date': '2026-10-27', 'end_date': '2026-10-27'
    })
    assert start.status_code == 200, start.text
    job = client.get('/api/historical-sr/analyze/status/' + start.json()['job_id'])
    assert job.status_code == 200, job.text
    payload = job.json()
    assert payload['status'] == 'complete'
    assert payload['analysis_available'] is False
    assert payload['levels'][0]['S1'] == 24990.0
    assert 'five-minute' in payload['analysis_error'].lower()


def test_combined_analysis_uses_uploaded_chart_csv_and_counts_fake_breakout(monkeypatch, tmp_path):
    monkeypatch.setenv('CACHE_DB', str(tmp_path / 'cache.sqlite3'))
    day = '2026-10-27'
    monkeypatch.setattr(auto_module, 'build_daily_levels', lambda start, end, progress_callback=None: (_level_row(start.isoformat()), []))
    def should_not_use_folder(start, end):
        raise AssertionError('The uploaded chart CSV should take precedence over the configured folder.')
    monkeypatch.setattr(auto_module, 'fetch_nse_candles', should_not_use_folder)
    candle_csv = (
        'timestamp,open,high,low,close\n'
        f'{day} 09:15:00,25008,25012,25008,25010\n'
        f'{day} 09:20:00,25013,25030,25013,25025\n'
        f'{day} 09:25:00,25025,25026,24998,25000\n'
    ).encode('utf-8')

    client = TestClient(app)
    start = client.post('/api/historical-sr/analyze/start', data={
        'start_date': day, 'end_date': day,
        'touch_tolerance': '2', 'reversal_points': '15', 'confirmation_candles': '3',
        'failed_break_candles': '6', 'reset_points': '10'
    }, files={'candles_file': ('nifty_spot_5m.csv', candle_csv, 'text/csv')})
    assert start.status_code == 200, start.text
    job = client.get('/api/historical-sr/analyze/status/' + start.json()['job_id'])
    assert job.status_code == 200, job.text
    payload = job.json()
    assert payload['status'] == 'complete'
    assert payload['analysis_available'] is True
    assert payload['analysis']['sessions_analyzed'] == 1
    assert payload['analysis']['candles_analyzed'] == 3
    assert payload['analysis']['counts']['R1']['failed_breakouts'] >= 1
    assert 'nifty_spot_5m.csv' in payload['candle_source']
    assert payload['levels'][0]['R1'] == 25010.0
