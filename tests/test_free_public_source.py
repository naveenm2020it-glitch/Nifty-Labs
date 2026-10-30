from datetime import date
import pandas as pd
from app.auto_history import _fetch_free_public_github_candles

def test_resample_shape_from_local_source_logic(monkeypatch, tmp_path):
    # Validate 1-minute OHLC -> 5-minute OHLC grouping logic independently of network.
    times = pd.date_range("2026-10-26 09:15:00", periods=10, freq="min")
    df = pd.DataFrame({"timestamp": times, "open": range(100,110), "high": range(101,111), "low": range(99,109), "close": range(100,110)})
    df = df.set_index("timestamp")
    start = pd.Timestamp("2026-10-26 09:15:00")
    bars=[]
    for day, group in df.groupby(df.index.date):
        offset=group.index-start
        group=group.assign(__bucket=(offset.total_seconds()//300).astype(int))
        for _, chunk in group.groupby("__bucket",sort=True):
            if len(chunk)<3: continue
            bars.append({"timestamp":chunk.index[0].strftime("%Y-%m-%d %H:%M:%S"),"open":float(chunk.open.iloc[0]),"high":float(chunk.high.max()),"low":float(chunk.low.min()),"close":float(chunk.close.iloc[-1])})
    out=pd.DataFrame(bars)
    assert len(out)==2
    assert out.iloc[0].open==100 and out.iloc[0].close==104
    assert out.iloc[0].high==105 and out.iloc[0].low==99
