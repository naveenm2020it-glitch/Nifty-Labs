import io
import pandas as pd
from app.historical_sr import load_candles, load_levels, analyze


def test_load_and_analyze_grouped_touch_and_reversal():
    candles = b'''timestamp,open,high,low,close\n2025-01-02 09:15,100,101,99,100\n2025-01-02 09:20,100,102,99,101\n2025-01-02 09:25,101,105,100,104\n2025-01-02 09:30,104,120,103,118\n'''
    levels = b'''date,S1,R1,S2,R2\n2025-01-02,90,100,80,110\n'''
    c = load_candles(candles)
    l = load_levels(levels)
    out = analyze(c, l, tolerance=2, reversal_points=10, confirmation_candles=2, failed_break_candles=2)
    assert out['sessions_analyzed'] == 1
    assert out['candles_analyzed'] == 4
    assert out['counts']['R1']['touches'] >= 1
    assert all('event_date' in e and 'event_time' in e for e in out['events'])


def test_bad_candle_csv_rejected():
    try:
        load_candles(b'timestamp,open,close\n2025-01-02 09:15,1,1\n')
    except ValueError as exc:
        assert 'missing required columns' in str(exc).lower()
    else:
        raise AssertionError('expected invalid candle CSV to be rejected')
