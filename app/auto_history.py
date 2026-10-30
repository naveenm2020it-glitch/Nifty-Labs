"""NSE-only historical S&R pipeline.

Daily option levels are sourced from official NSE F&O bhavcopy archives. NSE's
public daily archives do not provide a verified two-year five-minute spot API.
For intraday analysis, the app accepts genuine five-minute OHLC CSV exports from
an authorized NSE data service via NSE_5M_CSV_DIR, or the existing browser CSV upload.
No candles are synthesized and no undocumented intraday endpoint is called.
"""
from __future__ import annotations
from datetime import date, datetime, timedelta
from io import BytesIO
import os, sqlite3, time
from pathlib import Path
import pandas as pd
from app.data import fetch_official_bhavcopy, select_strikes, support_resistance

# Tiny .env reader keeps setup dependency-free. Existing process environment wins.
def _load_local_env():
    path = os.path.join(os.getcwd(), ".env")
    if not os.path.isfile(path): return
    try:
        with open(path, encoding="utf-8") as fh:
            for line in fh:
                line=line.strip()
                if not line or line.startswith("#") or "=" not in line: continue
                key,value=line.split("=",1); key=key.strip(); value=value.strip().strip('"').strip("'")
                if key and key not in os.environ: os.environ[key]=value
    except OSError: pass

_load_local_env()

def _db_path(): return os.getenv("CACHE_DB", "nifty_cache.sqlite3")

def _cache_init():
    with sqlite3.connect(_db_path()) as c:
        c.execute("CREATE TABLE IF NOT EXISTS sr_auto_cache (cache_key TEXT PRIMARY KEY, payload TEXT NOT NULL, source TEXT NOT NULL, saved_at TEXT DEFAULT CURRENT_TIMESTAMP)")

def _get_cached(key):
    _cache_init()
    with sqlite3.connect(_db_path()) as c:
        r=c.execute("SELECT payload,source FROM sr_auto_cache WHERE cache_key=?",(key,)).fetchone()
    return (pd.read_json(BytesIO(r[0].encode()), orient="records"),r[1]) if r else (None,None)

def _save_cache(key, df, source):
    _cache_init()
    payload=df.to_json(orient="records",date_format="iso")
    with sqlite3.connect(_db_path()) as c:
        c.execute("INSERT OR REPLACE INTO sr_auto_cache(cache_key,payload,source) VALUES(?,?,?)",(key,payload,source))

def _fetch_free_public_github_candles(start: date, end: date):
    """Fetch public community-maintained NIFTY 1-minute CSVs from GitHub and resample to 5m.

    Source repository documents NIFTY50 1-minute OHLC files since 2017. This is a
    best-effort free fallback, NOT an official NSE feed; coverage/quality can vary.
    Files and repository tree are cached locally to avoid repeat downloads.
    """
    import json, re, requests
    cache_dir = Path(os.getenv("NIFTY_PUBLIC_CACHE_DIR", "data_cache/public_nifty_1min"))
    cache_dir.mkdir(parents=True, exist_ok=True)
    tree_file = cache_dir / "github_tree_main.json"
    base_api = "https://api.github.com/repos/technovusin/nifty50-historical-data/git/trees/main?recursive=1"
    headers = {"User-Agent": "NIFTY-Historical-SR-Analyzer/1.0", "Accept": "application/vnd.github+json"}
    try:
        if tree_file.exists() and (time.time() - tree_file.stat().st_mtime) < 24 * 3600:
            tree = json.loads(tree_file.read_text(encoding="utf-8"))
        else:
            response = requests.get(base_api, headers=headers, timeout=(10, 30))
            response.raise_for_status()
            tree = response.json()
            tree_file.write_text(json.dumps(tree), encoding="utf-8")
        paths = [item["path"] for item in tree.get("tree", []) if item.get("type") == "blob"]
    except Exception as exc:
        raise RuntimeError(
            "Automatic free-source fetch failed while contacting the public GitHub NIFTY history repository. "
            f"Check internet access and retry. Details: {type(exc).__name__}: {exc}. "
            "This community dataset is a fallback, not an official NSE feed."
        ) from exc

    wanted = []
    for year in range(start.year, end.year + 1):
        # Repository documents annual files for 2017-2025 and monthly files for 2026 onward.
        annual = re.compile(rf"^NIFTY50_1min_{year}\.csv$", re.I)
        monthly = re.compile(rf"^NIFTY50_1min_{year}-(\d{{2}})\.csv$", re.I)
        for path in paths:
            name = path.rsplit("/", 1)[-1]
            if "/1min/" not in ("/" + path) or not name.lower().endswith(".csv"):
                continue
            if annual.match(name):
                wanted.append(path)
            else:
                match = monthly.match(name)
                if match and (year > start.year or int(match.group(1)) >= start.month) and (year < end.year or int(match.group(1)) <= end.month):
                    wanted.append(path)
    wanted = sorted(set(wanted))
    if not wanted:
        raise RuntimeError(f"The free public dataset lists no one-minute files for {start} through {end}.")

    frames, used, problems = [], [], []
    for relpath in wanted:
        local = cache_dir / Path(relpath).name
        try:
            if not local.exists() or local.stat().st_size == 0:
                url = "https://raw.githubusercontent.com/technovusin/nifty50-historical-data/main/" + relpath
                r = requests.get(url, headers={"User-Agent": headers["User-Agent"]}, timeout=(10, 90))
                r.raise_for_status()
                if not r.content:
                    raise ValueError("empty download")
                local.write_bytes(r.content)
            raw = pd.read_csv(local)
            norm = {str(c).strip().lower().replace("_", " "): c for c in raw.columns}
            timestamp_col = next((norm[k] for k in ("timestamp", "datetime", "date time") if k in norm), None)
            if timestamp_col is None and "epoch" in norm:
                raw["__timestamp"] = pd.to_datetime(pd.to_numeric(raw[norm["epoch"]], errors="coerce"), unit="s", utc=True).dt.tz_convert("Asia/Kolkata").dt.tz_localize(None)
                timestamp_col = "__timestamp"
            required = {k: norm.get(k) for k in ("open", "high", "low", "close")}
            if timestamp_col is None or any(v is None for v in required.values()):
                raise ValueError(f"unsupported CSV columns: {list(raw.columns)}")
            df = pd.DataFrame({"timestamp": pd.to_datetime(raw[timestamp_col], errors="coerce", format="mixed"), **{k: pd.to_numeric(raw[v], errors="coerce") for k,v in required.items()}})
            # Source timestamps are documented as IST. Convert aware timestamps to IST, then remove tz.
            if getattr(df["timestamp"].dt, "tz", None) is not None:
                df["timestamp"] = df["timestamp"].dt.tz_convert("Asia/Kolkata").dt.tz_localize(None)
            df = df.dropna(subset=["timestamp", "open", "high", "low", "close"])
            df = df[(df.timestamp.dt.date >= start) & (df.timestamp.dt.date <= end)]
            if df.empty:
                continue
            df = df.drop_duplicates("timestamp").sort_values("timestamp").set_index("timestamp")
            # Align five-minute bars to the NSE session open (09:15 IST).
            df["__session"] = df.index.normalize()
            frames.append(df)
            used.append(local.name)
        except Exception as exc:
            problems.append(f"{relpath}: {type(exc).__name__}: {exc}")

    if not frames:
        detail = "; ".join(problems[:3])
        raise RuntimeError(f"Could not download valid free public NIFTY intraday candles for {start} through {end}. {detail}")
    minute = pd.concat(frames).reset_index().drop_duplicates("timestamp").sort_values("timestamp").set_index("timestamp")
    minute = minute.between_time("09:15", "15:29", inclusive="both")
    # Resample per session with bins anchored at 09:15; only complete five-minute windows are retained.
    bars = []
    for day, group in minute.groupby(minute.index.date):
        group = group.drop(columns=["__session"], errors="ignore").sort_index()
        session_start = pd.Timestamp(day).replace(hour=9, minute=15)
        group = group[group.index >= session_start]
        if group.empty: continue
        offset = group.index - session_start
        buckets = (offset.total_seconds() // 300).astype(int)
        group = group.assign(__bucket=buckets)
        for _, chunk in group.groupby("__bucket", sort=True):
            if len(chunk) < 3:  # tolerate small source gaps, but discard clearly incomplete bins
                continue
            bars.append({"timestamp": chunk.index[0].strftime("%Y-%m-%d %H:%M:%S"), "open": float(chunk.open.iloc[0]), "high": float(chunk.high.max()), "low": float(chunk.low.min()), "close": float(chunk.close.iloc[-1])})
    out = pd.DataFrame(bars)
    if out.empty:
        raise RuntimeError("The free public source returned no usable five-minute bars for the selected date range.")
    source = ("Free community-maintained GitHub NIFTY 1-minute OHLC dataset resampled to 5-minute bars; "
              f"{len(out):,} bars from {out.timestamp.iloc[0]} to {out.timestamp.iloc[-1]}; files: {', '.join(used[:8])}" +
              (f" (+{len(used)-8} more)" if len(used) > 8 else "") +
              ". Not an official NSE feed; verify gaps and prices before relying on results.")
    return out, source


def fetch_nse_candles(start: date, end: date):
    """Automatically load real five-minute candles available in the configured NSE archive.

    The app does not call undocumented NSE endpoints. Public NSE daily reports are EOD data;
    historical intraday files must be obtained through an authorized NSE intraday data service
    and placed in the configured archive directory. The browser upload remains an override.
    The archive may contain one large CSV, multiple CSVs, or ZIPs containing candle CSVs.
    """
    import zipfile
    folder = Path(os.getenv("NSE_5M_CSV_DIR", "nse_5m_data")).expanduser()
    if not folder.is_dir():
        raise RuntimeError(
            "NSE-only mode: automatic five-minute retrieval could not find the configured archive directory "
            f"'{folder}'. no official five-minute candle files are available here. NSE's public historical index and F&O bhavcopy reports provide daily/EOD data, "
            "not two years of five-minute NIFTY spot candles. Configure the folder with genuine five-minute "
            "CSV exports obtained through an authorized NSE intraday data service, then click Analyze again. "
            "No candles were downloaded or fabricated."
        )

    sources_to_read = []
    sources_to_read.extend((path, None) for path in sorted(folder.rglob("*.csv")) if path.is_file())
    for zpath in sorted(folder.rglob("*.zip")):
        if not zpath.is_file():
            continue
        try:
            with zipfile.ZipFile(zpath) as archive:
                for member in sorted(n for n in archive.namelist() if n.lower().endswith(".csv") and not n.startswith("__MACOSX/")):
                    sources_to_read.append((zpath, member))
        except Exception:
            # Report malformed ZIPs later if no valid candles can be recovered.
            sources_to_read.append((zpath, "__INVALID_ZIP__"))

    if not sources_to_read:
        # Free automatic fallback: public community-maintained NIFTY minute data, cached locally.
        # It is not an official NSE feed; provenance is returned with the results.
        return _fetch_free_public_github_candles(start, end)

    frames = []
    errors = []
    sources = []
    aliases = {
        "timestamp": ["timestamp", "datetime", "date time", "date_time"],
        "open": ["open", "open price", "openprice"],
        "high": ["high", "high price", "highprice"],
        "low": ["low", "low price", "lowprice"],
        "close": ["close", "close price", "closeprice", "ltp"],
    }

    for path, member in sources_to_read:
        source_name = f"{path.name}:{member}" if member else path.name
        try:
            if member == "__INVALID_ZIP__":
                raise ValueError("invalid ZIP archive")
            if member:
                with zipfile.ZipFile(path) as archive:
                    raw = pd.read_csv(BytesIO(archive.read(member)))
            else:
                raw = pd.read_csv(path)
            norm = {str(c).strip().lower().replace("_", " "): c for c in raw.columns}
            chosen = {}
            # Prefer a combined timestamp. When Date and Time are separate, combine them.
            for target, choices in aliases.items():
                for choice in choices:
                    if choice in norm:
                        chosen[target] = norm[choice]
                        break
            if "timestamp" not in chosen and "date" in norm and "time" in norm:
                raw["__timestamp"] = raw[norm["date"]].astype(str).str.strip() + " " + raw[norm["time"]].astype(str).str.strip()
                chosen["timestamp"] = "__timestamp"
            if set(chosen) != {"timestamp", "open", "high", "low", "close"}:
                errors.append(f"{source_name}: missing timestamp/open/high/low/close columns")
                continue
            one = raw[[chosen[k] for k in ["timestamp", "open", "high", "low", "close"]]].copy()
            one.columns = ["timestamp", "open", "high", "low", "close"]
            parsed = pd.to_datetime(one["timestamp"], errors="coerce", format="mixed", dayfirst=True)
            if getattr(parsed.dt, "tz", None) is not None:
                parsed = parsed.dt.tz_convert("Asia/Kolkata").dt.tz_localize(None)
            one["timestamp"] = parsed
            for col in ["open", "high", "low", "close"]:
                one[col] = pd.to_numeric(one[col], errors="coerce")
            one = one.dropna(subset=["timestamp", "open", "high", "low", "close"])
            one = one[(one["timestamp"].dt.date >= start) & (one["timestamp"].dt.date <= end)]
            if one.empty:
                errors.append(f"{source_name}: no rows in selected date range")
                continue
            frames.append(one)
            sources.append(source_name)
        except Exception as exc:
            errors.append(f"{source_name}: {type(exc).__name__}: {exc}")

    merged = pd.concat(frames, ignore_index=True) if frames else pd.DataFrame(columns=["timestamp", "open", "high", "low", "close"])
    if merged.empty:
        detail = "; ".join(errors[:5])
        raise RuntimeError(
            f"No valid five-minute candles in '{folder}' for {start} through {end}. {detail}. "
            "Make sure files contain intraday timestamp, open, high, low and close columns. No data was fabricated."
        )
    merged = merged.drop_duplicates("timestamp").sort_values("timestamp").reset_index(drop=True)
    invalid = ((merged["high"] < merged[["open", "low", "close"]].max(axis=1)) |
               (merged["low"] > merged[["open", "high", "close"]].min(axis=1)))
    if invalid.any():
        merged = merged.loc[~invalid].copy()
    if merged.empty:
        raise RuntimeError("All supplied candle rows failed OHLC validation; no data was analyzed.")

    # Refuse daily bars or clearly different bar intervals masquerading as 5-minute history.
    merged["_session_date"] = merged["timestamp"].dt.date.astype(str)
    intervals = []
    for _, group in merged.groupby("_session_date", sort=False):
        differences = group["timestamp"].sort_values().diff().dt.total_seconds().div(60)
        intervals.extend(differences[(differences > 0) & (differences <= 30)].tolist())
    if not intervals:
        raise RuntimeError("The archive does not contain enough intraday timestamps to verify five-minute candles. Daily bars are not accepted.")
    median_minutes = float(pd.Series(intervals).median())
    if not 4.0 <= median_minutes <= 6.0:
        raise RuntimeError(
            f"The archive's median intraday interval is {median_minutes:g} minutes, not five minutes. "
            "Use genuine five-minute NIFTY 50 spot OHLC candles; daily, one-minute or fifteen-minute bars are not substituted."
        )

    count = len(merged)
    session_count = int(merged["_session_date"].nunique())
    merged["timestamp"] = merged["timestamp"].dt.strftime("%Y-%m-%d %H:%M:%S")
    source = (f"Authorized NSE five-minute CSV exports from '{folder}'; {count:,} validated five-minute candles across "
              f"{session_count} dates; {len(sources)} source file(s): {', '.join(sources[:12])}" +
              (f" (+{len(sources)-12} more)" if len(sources) > 12 else ""))
    return merged[["timestamp", "open", "high", "low", "close"]], source

def build_daily_levels(start: date, end: date, progress_callback=None):
    """Build levels for session D from the most recent prior session's published options close.

    This avoids look-ahead: today's level row never uses today's closing prices.
    Expiry is chosen from prior-session contracts that remain valid on target session D.
    """
    rows=[]; skipped=[]; d=start
    total_sessions=sum(1 for n in range((end-start).days+1) if (start+timedelta(days=n)).weekday()<5)
    completed_sessions=0
    while d<=end:
        if d.weekday()<5:
            key=f"nselevel|{d.isoformat()}"
            cached,src=_get_cached(key)
            if cached is not None and not cached.empty:
                rows.extend(cached.to_dict("records")); completed_sessions += 1
                if progress_callback: progress_callback(completed_sessions, total_sessions, len(rows), len(skipped), d.isoformat())
                d+=timedelta(days=1); continue
            try:
                raw=None; source=None; prior_day=None; errors=[]
                # Look back across weekends/holidays, but never use a future session.
                for back in range(1,8):
                    candidate=d-timedelta(days=back)
                    if candidate.weekday()>=5: continue
                    try:
                        candidate_df,candidate_source=fetch_official_bhavcopy(candidate)
                        if "trade_date" not in candidate_df: continue
                        date_rows=candidate_df[candidate_df["trade_date"]==candidate]
                        if not date_rows.empty:
                            raw=candidate_df; source=candidate_source; prior_day=candidate; break
                    except Exception as exc:
                        errors.append(f"{candidate}: {str(exc)[:120]}")
                if raw is None or prior_day is None:
                    raise ValueError("No prior-session NSE options bhavcopy found within 7 calendar days. " + " | ".join(errors[-3:]))
                sub=raw[(raw["trade_date"]==prior_day)&(raw["symbol"].astype(str).str.upper()=="NIFTY")&raw["option_type"].astype(str).str.upper().isin(["CE","PE"])].copy()
                if "instrument" in sub: sub=sub[sub["instrument"].astype(str).str.contains(r"OPTIDX|OPT|IDO|IO",case=False,regex=True,na=False)]
                exps=sorted(x for x in sub["expiry"].dropna().unique().tolist() if isinstance(x,date) and x>=d)
                if not exps: raise ValueError(f"No NIFTY option expiry valid on target session {d} in prior-session bhavcopy {prior_day}")
                expiry=exps[0]; session=sub[sub["expiry"]==expiry].copy()
                spot=None
                if "underlying" in session:
                    vals=pd.to_numeric(session["underlying"],errors="coerce").dropna()
                    if not vals.empty: spot=float(vals.median())
                if spot is None:
                    raise ValueError(f"NIFTY underlying close is missing from the official NSE bhavcopy for {prior_day}; no undocumented API fallback was used")
                _,atm=select_strikes(session,spot,50,7,7,"hlc_min_diff",None)
                ce=session[(session["strike"].astype(float)==float(atm))&(session["option_type"].astype(str).str.upper()=="CE")]
                pe=session[(session["strike"].astype(float)==float(atm))&(session["option_type"].astype(str).str.upper()=="PE")]
                if ce.empty or pe.empty: raise ValueError("Selected HLC ATM lacks CE/PE pair")
                sr=support_resistance(float(atm),float(ce.iloc[0]["close"]),float(pe.iloc[0]["close"]))
                record={"date":d.isoformat(),"S1":sr["S1"],"R1":sr["R1"],"S2":sr["S2"],"R2":sr["R2"],"atm_strike":float(atm),"expiry":expiry.isoformat(),"ce_close":float(ce.iloc[0]["close"]),"pe_close":float(pe.iloc[0]["close"]),"level_source_date":prior_day.isoformat(),"source":f"{source}; prior-session levels from {prior_day}; target session {d}"}
                one=pd.DataFrame([record]); _save_cache(key,one,source); rows.append(record)
            except Exception as exc:
                skipped.append({"date":d.isoformat(),"reason":str(exc)[:400]})
            completed_sessions += 1
            if progress_callback: progress_callback(completed_sessions, total_sessions, len(rows), len(skipped), d.isoformat())
            time.sleep(0.15)
        d+=timedelta(days=1)
    levels=pd.DataFrame(rows)
    if levels.empty: raise RuntimeError("Could not calculate any daily levels from prior-session NSE archives. See source errors; no levels were fabricated.")
    return levels, skipped

def automated_analysis(start: date,end: date, settings: dict):
    if end<start: raise ValueError("End date must be on or after start date.")
    if (end-start).days>800: raise ValueError("Requested range exceeds 800 calendar days. Run in smaller windows to respect provider limits.")
    # Fetch/cache official NSE daily option levels first, even when intraday
    # candles are not configured, so the user can reuse/export those real levels.
    levels, skipped=build_daily_levels(start,end)
    candles, candle_source=fetch_nse_candles(start,end)
    return candles,levels,skipped,candle_source
