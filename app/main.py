from __future__ import annotations
from datetime import date
from uuid import uuid4
from threading import Lock
from io import BytesIO
import json
import sqlite3
import os
from pathlib import Path
import pandas as pd
from fastapi import FastAPI, UploadFile, File, Form, HTTPException, BackgroundTasks
from fastapi.responses import HTMLResponse, JSONResponse, StreamingResponse, FileResponse
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
from app.historical_sr import load_candles, load_levels, analyze as analyze_historical_sr, analyze_fixed_levels
from app.auto_history import automated_analysis
from app.data import (
    fetch_official_bhavcopy, parse_bhavcopy_bytes, filter_session, select_strikes,
    get_underlying_close, build_dashboard_rows, support_resistance,
    fetch_nifty_close_api, fetch_contract_api
)

PROJECT_ROOT = Path(__file__).resolve().parent.parent
LEGACY_HTML_PATH = PROJECT_ROOT / "static" / "index.html"
FRONTEND_DIST = PROJECT_ROOT / "frontend" / "dist"

app = FastAPI(title="Index Options Intelligence", version="2.1.0")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])
app.mount("/static", StaticFiles(directory=str(PROJECT_ROOT / "static")), name="static")
@app.get("/assets/{asset_path:path}", include_in_schema=False)
def frontend_asset(asset_path: str):
    """Serve Vite-built frontend assets without coupling import to build order."""
    asset_root = (FRONTEND_DIST / "assets").resolve()
    target = (asset_root / asset_path).resolve()
    if not target.is_relative_to(asset_root) or not target.is_file():
        raise HTTPException(404, "Frontend asset not found. Run npm run build in frontend/.")
    return FileResponse(target)
DB_PATH = os.getenv("CACHE_DB", "nifty_cache.sqlite3")
_NSE_LEVEL_JOBS = {}
_NSE_LEVEL_JOBS_LOCK = Lock()

def _run_nse_levels_job(job_id: str, start_date: date, end_date: date):
    from app.auto_history import build_daily_levels
    def progress(done, total, rows, skipped, current_date):
        with _NSE_LEVEL_JOBS_LOCK:
            job = _NSE_LEVEL_JOBS.get(job_id)
            if job:
                job.update({"completed_sessions": done, "total_sessions": total,
                            "rows_found": rows, "dates_skipped": skipped,
                            "current_date": current_date, "status": "running"})
    try:
        levels, skipped = build_daily_levels(start_date, end_date, progress_callback=progress)
        with _NSE_LEVEL_JOBS_LOCK:
            _NSE_LEVEL_JOBS[job_id].update({"status": "complete", "levels": _serialize_df(levels),
                "skipped": skipped, "count": int(len(levels)), "source": "Official NSE F&O bhavcopy archives; prior-session option closes"})
    except Exception as exc:
        with _NSE_LEVEL_JOBS_LOCK:
            _NSE_LEVEL_JOBS[job_id].update({"status": "error", "error": f"{type(exc).__name__}: {exc}"})


def _run_historical_sr_analysis_job(job_id: str, start_date: date, end_date: date, settings: dict, uploaded_candles_bytes: bytes | None = None, uploaded_candles_name: str | None = None):
    """Run one end-to-end date-range analysis, always preserving usable daily levels."""
    from app.auto_history import build_daily_levels, fetch_nse_candles
    def progress(done, total, rows, skipped, current_date):
        with _NSE_LEVEL_JOBS_LOCK:
            job = _NSE_LEVEL_JOBS.get(job_id)
            if job:
                job.update({"status": "running", "stage": "Fetching NSE daily option data",
                            "completed_sessions": done, "total_sessions": total,
                            "rows_found": rows, "dates_skipped": skipped,
                            "current_date": current_date})
    try:
        with _NSE_LEVEL_JOBS_LOCK:
            _NSE_LEVEL_JOBS[job_id].update({"status": "running", "stage": "Calculating each session's S1/S2/R1/R2"})
        levels, skipped = build_daily_levels(start_date, end_date, progress_callback=progress)
        serialized_levels = _serialize_df(levels)
        with _NSE_LEVEL_JOBS_LOCK:
            _NSE_LEVEL_JOBS[job_id].update({
                "levels": serialized_levels, "skipped": skipped, "count": len(serialized_levels),
                "level_source": "Official NSE F&O bhavcopy archives; prior-session option closes",
                "stage": "Checking genuine five-minute spot candles", "rows_found": len(serialized_levels),
                "dates_skipped": len(skipped)
            })
        # Daily level rows remain a valid result when intraday candles are not configured.
        try:
            if uploaded_candles_bytes is not None:
                uploaded_frame = load_candles(uploaded_candles_bytes)
                uploaded_frame = uploaded_frame[(uploaded_frame["date"] >= start_date.isoformat()) &
                                                (uploaded_frame["date"] <= end_date.isoformat())].copy()
                if uploaded_frame.empty:
                    raise RuntimeError(f"Uploaded five-minute CSV contains no valid candles from {start_date.isoformat()} through {end_date.isoformat()}.")
                candles = uploaded_frame[["timestamp", "open", "high", "low", "close"]].copy()
                candles["timestamp"] = candles["timestamp"].dt.strftime("%Y-%m-%d %H:%M:%S")
                candle_source = f"User-uploaded genuine five-minute CSV: {uploaded_candles_name or 'spot candles CSV'} ({len(candles)} validated candles in selected range)"
            else:
                candles, candle_source = fetch_nse_candles(start_date, end_date)
        except Exception as candle_exc:
            with _NSE_LEVEL_JOBS_LOCK:
                _NSE_LEVEL_JOBS[job_id].update({
                    "status": "complete", "stage": "Daily levels ready; intraday analysis unavailable",
                    "analysis_available": False,
                    "analysis_error": f"{type(candle_exc).__name__}: {candle_exc}",
                    "candle_source": None,
                    "message": "Daily S1/S2/R1/R2 levels were calculated. Event counts require genuine five-minute NIFTY spot candles for the selected range."
                })
            return
        try:
            candle_bytes = candles.to_csv(index=False).encode("utf-8")
            level_bytes = levels.to_csv(index=False).encode("utf-8")
            analysis = analyze_historical_sr(
                load_candles(candle_bytes), load_levels(level_bytes),
                float(settings.get("touch_tolerance", 2.0)),
                float(settings.get("reversal_points", 15.0)),
                int(settings.get("confirmation_candles", 3)),
                int(settings.get("failed_break_candles", 6)),
                float(settings.get("reset_points", 10.0))
            )
            analysis["candle_source"] = candle_source
            analysis["levels_source"] = "Official NSE F&O bhavcopy; existing HLC ATM and S1/R1/S2/R2 formula engine"
            analysis["skipped_level_dates"] = skipped
            with _NSE_LEVEL_JOBS_LOCK:
                _NSE_LEVEL_JOBS[job_id].update({
                    "status": "complete", "stage": "Analysis complete", "analysis_available": True,
                    "analysis": analysis, "candle_source": candle_source,
                    "message": "Daily levels and five-minute reaction analysis completed."
                })
        except Exception as analysis_exc:
            with _NSE_LEVEL_JOBS_LOCK:
                _NSE_LEVEL_JOBS[job_id].update({
                    "status": "complete", "stage": "Daily levels ready; analysis failed",
                    "analysis_available": False,
                    "analysis_error": f"{type(analysis_exc).__name__}: {analysis_exc}",
                    "candle_source": candle_source,
                    "message": "Daily levels were retrieved, but candle validation or event analysis failed."
                })
    except Exception as exc:
        with _NSE_LEVEL_JOBS_LOCK:
            job = _NSE_LEVEL_JOBS.get(job_id)
            if job is not None:
                job.update({"status": "error", "stage": "Fetching failed", "error": f"{type(exc).__name__}: {exc}"})

def init_db():
    with sqlite3.connect(DB_PATH) as conn:
        conn.execute("""CREATE TABLE IF NOT EXISTS query_cache (
            cache_key TEXT PRIMARY KEY, source TEXT NOT NULL, rows_json TEXT NOT NULL,
            created_at TEXT DEFAULT CURRENT_TIMESTAMP)""")
init_db()

def _cache_key(symbol: str, trade_date: date, expiry_date: date):
    return f"{symbol.upper()}|{trade_date.isoformat()}|{expiry_date.isoformat()}"

@app.get("/workspace", response_class=HTMLResponse, include_in_schema=False)
def legacy_workspace():
    """Serve the original feature workspace inside the React shell.

    The legacy feature DOM and event handlers are intentionally retained as a
    compatibility layer. Only its outer navigation/heading chrome is hidden;
    feature controls, calculations, data fetching, tables, and exports remain
    the original implementation.
    """
    html = LEGACY_HTML_PATH.read_text(encoding="utf-8")
    embedded_styles = r"""
<style id="nifty-lab-embedded-overrides">
  html, body { min-width: 0 !important; width: 100% !important; min-height: 100% !important; }
  body { overflow-x: hidden !important; background: var(--app-bg) !important; }
  body > .app-shell > .sidebar, .app-main > .app-topbar, .app-main > .top, #dataNotice { display: none !important; }
  .app-shell { min-height: 100vh !important; width: 100% !important; }
  .app-main { width: 100% !important; max-width: none !important; min-width: 0 !important; margin: 0 auto !important; padding: clamp(12px, 2.2vw, 26px) clamp(10px, 2vw, 24px) 34px !important; }
  .page-intro { margin-top: 0 !important; }
  .page-view { min-width: 0 !important; }
  @media (max-width: 600px) { .app-main { padding: 13px 9px 24px !important; } }
</style>
"""
    chart_bridge = r"""
<script>
window.__niftyLabGetSpotChartState = function () {
  const selectedDate = sr$('spotChartDate')?.value || '';
  const overviewLevels = current && current.support_resistance && ['S1','S2','R1','R2'].every(k =>
    current.support_resistance[k] !== null && current.support_resistance[k] !== undefined &&
    current.support_resistance[k] !== '' && Number.isFinite(Number(current.support_resistance[k])))
    ? current.support_resistance : null;
  const sessionLevels = overviewLevels || (nseDailyLevelRows || []).find(row => spotParseDate(row.date) === selectedDate) || null;
  return {
    date: selectedDate,
    candles: (spotChartCandles || []).filter(candle => candle.date === selectedDate).map(candle => ({ ...candle })),
    levels: sessionLevels,
  };
};
</script>
<script type="module" src="/assets/legacy-chart-bridge.js"></script>
"""
    html = html.replace("</head>", embedded_styles + "\n</head>", 1)
    return html.replace("</body>", chart_bridge + "\n</body>", 1)

@app.get("/", response_class=HTMLResponse)
def home():
    built_index = FRONTEND_DIST / "index.html"
    if built_index.is_file():
        return built_index.read_text(encoding="utf-8")
    # Development fallback keeps the existing application runnable before
    # frontend dependencies have been installed and built.
    return LEGACY_HTML_PATH.read_text(encoding="utf-8")

@app.get("/api/health")
def health():
    return {"status": "ok", "app": "NIFTY LAB", "version": "2.1.0"}

def _query(
    trade_date: date, expiry_date: date, symbol: str, raw_df: pd.DataFrame, source: str,
    manual_close: float | None, interval: int, itm_count: int, otm_count: int,
    atm_method: str, manual_atm: float | None, ce_reference_mode: str,
    pe_reference_mode: str, ce_manual: float | None, pe_manual: float | None,
):
    if expiry_date < trade_date:
        # It may be a valid expired expiry historically, but it cannot be traded on a later session.
        raise ValueError("The selected expiry is before the selected trading date. Choose the contract expiry that was still valid on that historical session. Historical contracts that expired later are supported even if they have expired today.")
    symbol = symbol.upper()
    session = filter_session(raw_df, trade_date, expiry_date, symbol)
    spot, spot_source = get_underlying_close(session, manual_close)
    if spot is None:
        raise ValueError(f"{symbol} close/underlying value is missing. Re-submit with a manual {symbol} close.")
    strikes, atm = select_strikes(session, spot, interval, itm_count, otm_count, atm_method, manual_atm)
    rows = build_dashboard_rows(session, strikes)
    # Additive contract explorer data: expose every strike actually present in the
    # selected date/expiry source so the UI can pick strikes outside the initial range.
    # The original `rows` payload and all calculation formulas remain unchanged.
    available_strikes = set(session["strike"].dropna().astype(float).tolist())
    all_available_rows = build_dashboard_rows(session, sorted(available_strikes))
    # HLC ATM detail card: selected strike and full CE/PE OHLC, plus absolute close difference.
    hlc_atm_details = {"strike": float(atm), "close_difference": None, "ce": {}, "pe": {},
                       "selection_method": atm_method}
    for side in ("CE", "PE"):
        found = session[(session["strike"].astype(float) == float(atm)) & (session["option_type"] == side)]
        fields = {}
        if not found.empty:
            rec = found.iloc[0]
            for field in ("open", "high", "low", "close"):
                value = rec.get(field)
                fields[field] = float(value) if pd.notna(value) else None
        else:
            fields = {field: None for field in ("open", "high", "low", "close")}
        hlc_atm_details[side.lower()] = fields
    ce_close = hlc_atm_details["ce"].get("close")
    pe_close = hlc_atm_details["pe"].get("close")
    if ce_close is not None and pe_close is not None:
        hlc_atm_details["close_difference"] = abs(ce_close - pe_close)
    # Support/resistance uses the CLOSE prices from the selected HLC ATM strike.
    # Do not use (High + Low + Close) / 3 here: “HLC ATM” names the selected
    # strike (minimum absolute CE-close / PE-close difference), not an average.
    ce_ref = ce_close
    pe_ref = pe_close
    ce_reference_mode = "close"
    pe_reference_mode = "close"
    sr = support_resistance(float(atm), ce_ref, pe_ref)
    clean_rows = []
    for row in rows:
        clean_rows.append({k: (None if pd.isna(v) else v) if not isinstance(v, (bool, str)) else v for k, v in row.items()})
    if manual_close is None and spot is not None:
        if "NSE historical index API" in source:
            spot_source = "NSE historical index API (automatic historical close)"
    return {
        "trade_date": trade_date.isoformat(), "expiry_date": expiry_date.isoformat(),
        "symbol": symbol, "index_name": "NIFTY 50",
        # Keep legacy names so existing exports/bookmarks remain compatible; generic aliases are for all indices.
        "nifty_close": spot, "nifty_close_source": spot_source, "index_close": spot, "index_close_source": spot_source,
        "atm": atm, "hlc_atm": hlc_atm_details, "atm_method": atm_method,
        "strike_interval": interval, "strikes_requested": strikes,
        "available_strikes_in_file": sorted(available_strikes),
        "available_contract_rows": int(len(session)),
        "ce_contracts": int((session["option_type"] == "CE").sum()),
        "pe_contracts": int((session["option_type"] == "PE").sum()),
        "bep_analysis": [
            {
                "strike": row.get("strike"),
                "ce_close": row.get("ce_close"),
                "pe_close": row.get("pe_close"),
                "bep": row.get("bep"),
                "formula": "(CE Close + PE Close) / 2",
                "status": "calculated" if row.get("bep") is not None and pd.notna(row.get("bep")) else "missing_close",
            }
            for row in clean_rows
        ],
        "source": source, "status": "success",
        "warning": "Confirm source data and underlying value before using for trading. Missing contracts remain blank.",
        "ce_reference_mode": ce_reference_mode, "pe_reference_mode": pe_reference_mode,
        "ce_reference": ce_ref, "pe_reference": pe_ref, "support_resistance": sr,
        "rows": clean_rows,
        "all_available_rows": [
            {k: (None if pd.isna(v) else v) if not isinstance(v, (bool, str)) else v for k, v in row.items()}
            for row in all_available_rows
        ],
    }

def _serialize_df(df):
    # JSON-safe records, retaining NaN as null.
    return json.loads(df.to_json(orient="records", date_format="iso"))


def _run_fixed_overview_analysis_job(job_id: str, start_date: date, end_date: date,
                                     fixed_levels: dict, settings: dict,
                                     uploaded_candles_bytes: bytes | None = None,
                                     uploaded_candles_name: str | None = None):
    """Run the exact Overview-level analysis as a background job.

    When no browser upload is selected, fetch_nse_candles automatically scans the
    configured NSE_5M_CSV_DIR archive for genuine five-minute candles in the range.
    This avoids long synchronous requests and lets the UI poll visible progress.
    """
    def update(**kwargs):
        with _NSE_LEVEL_JOBS_LOCK:
            job = _NSE_LEVEL_JOBS.get(job_id)
            if job is not None:
                job.update(kwargs)
    try:
        update(status="running", stage="Loading available NSE five-minute archive", progress=10)
        if uploaded_candles_bytes is not None:
            if not uploaded_candles_bytes:
                raise ValueError("The selected five-minute candle CSV is empty.")
            payload = uploaded_candles_bytes
            candle_source = f"Browser upload: {uploaded_candles_name or 'five-minute CSV'}"
        else:
            from app.auto_history import fetch_nse_candles
            frame, candle_source = fetch_nse_candles(start_date, end_date)
            payload = frame.to_csv(index=False).encode("utf-8")
        update(stage="Validating five-minute candles", progress=45, candle_source=candle_source)
        candles = load_candles(payload)
        candles = candles[(candles["date"] >= start_date.isoformat()) &
                          (candles["date"] <= end_date.isoformat())].copy()
        if candles.empty:
            raise ValueError(f"No valid five-minute NIFTY spot candles found between {start_date.isoformat()} and {end_date.isoformat()}.")
        candles = candles.reset_index(drop=True)
        update(stage=f"Analyzing {len(candles):,} validated candles against the four fixed Overview levels", progress=70,
               candles_found=int(len(candles)), trading_dates_found=int(candles["date"].nunique()))
        result = analyze_fixed_levels(
            candles, fixed_levels,
            float(settings.get("touch_tolerance", 2.0)),
            float(settings.get("reversal_points", 15.0)),
            int(settings.get("confirmation_candles", 3)),
            int(settings.get("failed_break_candles", 6)),
            float(settings.get("reset_points", 10.0)),
        )
        result["candle_source"] = candle_source
        result["requested_range"] = {"start": start_date.isoformat(), "end": end_date.isoformat()}
        result["coverage_note"] = (
            f"Analyzed {result['trading_dates']} distinct trading dates and {result['candles_analyzed']:,} genuine, "
            f"validated five-minute candles from {result['data_range']['start']} to {result['data_range']['end']} (IST). "
            "Coverage only reflects data available in the selected archive/file."
        )
        update(status="complete", stage="Analysis complete", progress=100, result=result,
               candles_found=int(result["candles_analyzed"]), trading_dates_found=int(result["trading_dates"]),
               message="The exact S1/S2/R1/R2 values from Overview were analyzed as fixed levels.")
    except Exception as exc:
        update(status="error", stage="Fetching or analysis failed", progress=100,
               error=f"{type(exc).__name__}: {exc}",
               message="No results were fabricated. Check the configured five-minute data archive or upload a genuine five-minute CSV.")


@app.get("/api/historical-sr/spot-candles")
def historical_sr_spot_candles(start_date: date, end_date: date):
    """Return validated genuine five-minute NIFTY spot candles for the requested chart range."""
    if end_date < start_date:
        raise HTTPException(400, "End date must be on or after the start date.")
    if (end_date - start_date).days > 740:
        raise HTTPException(400, "Choose a chart range of 740 days or less.")
    try:
        from app.auto_history import fetch_nse_candles
        frame, source = fetch_nse_candles(start_date, end_date)
        candles = load_candles(frame.to_csv(index=False).encode("utf-8"))
        candles = candles[(candles["date"] >= start_date.isoformat()) &
                          (candles["date"] <= end_date.isoformat())].copy()
        if candles.empty:
            raise ValueError("No validated five-minute candles exist in the selected range.")
        records = []
        for row in candles.to_dict(orient="records"):
            records.append({
                k: (v.strftime("%Y-%m-%d %H:%M:%S") if k == "timestamp" and hasattr(v, "strftime")
                    else (None if pd.isna(v) else (float(v) if k in {"open", "high", "low", "close"} else v)))
                for k, v in row.items()
            })
        return {"candles": records, "source": source, "start_date": start_date.isoformat(),
                "end_date": end_date.isoformat(), "count": len(records),
                "trading_dates": sorted({r.get("date") for r in records if r.get("date")})}
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(422, f"Could not load genuine five-minute spot candles for {start_date} through {end_date}: {type(exc).__name__}: {exc}") from exc

@app.post("/api/historical-sr/fixed-level-analysis/start")
async def start_fixed_overview_analysis(
    background_tasks: BackgroundTasks,
    start_date: date = Form(...), end_date: date = Form(...),
    s1: float = Form(...), s2: float = Form(...), r1: float = Form(...), r2: float = Form(...),
    touch_tolerance: float = Form(2.0), reversal_points: float = Form(15.0),
    confirmation_candles: int = Form(3), failed_break_candles: int = Form(6),
    reset_points: float = Form(10.0), candles_file: UploadFile | None = File(None),
):
    """Start one-click analysis of exact Overview S1/S2/R1/R2 across available 5-minute candles."""
    if end_date < start_date:
        raise HTTPException(422, "To date must be on or after From date.")
    if (end_date - start_date).days > 800:
        raise HTTPException(422, "The selected range exceeds 800 calendar days; choose a maximum two-year range.")
    if touch_tolerance < 0 or reversal_points <= 0 or confirmation_candles < 1 or failed_break_candles < 1 or reset_points < 0:
        raise HTTPException(422, "Invalid event-detection settings.")
    fixed_levels = {"S1": s1, "S2": s2, "R1": r1, "R2": r2}
    uploaded_bytes = None
    uploaded_name = None
    if candles_file is not None and candles_file.filename:
        uploaded_bytes = await candles_file.read()
        uploaded_name = candles_file.filename
    job_id = uuid4().hex
    settings = {"touch_tolerance": touch_tolerance, "reversal_points": reversal_points,
                "confirmation_candles": confirmation_candles, "failed_break_candles": failed_break_candles,
                "reset_points": reset_points}
    with _NSE_LEVEL_JOBS_LOCK:
        _NSE_LEVEL_JOBS[job_id] = {
            "job_id": job_id, "job_type": "fixed_overview_analysis", "status": "queued",
            "stage": "Queued", "progress": 0, "start_date": start_date.isoformat(),
            "end_date": end_date.isoformat(), "fixed_levels": fixed_levels,
            "message": "The app will scan the configured NSE five-minute archive for the selected date range.",
        }
    background_tasks.add_task(_run_fixed_overview_analysis_job, job_id, start_date, end_date,
                              fixed_levels, settings, uploaded_bytes, uploaded_name)
    return {"job_id": job_id, "status": "queued", "progress": 0}


@app.get("/api/historical-sr/fixed-level-analysis/status/{job_id}")
def fixed_overview_analysis_status(job_id: str):
    with _NSE_LEVEL_JOBS_LOCK:
        job = _NSE_LEVEL_JOBS.get(job_id)
        if not job or job.get("job_type") != "fixed_overview_analysis":
            raise HTTPException(404, "Analysis job was not found. Click Analyze to start a new job.")
        return dict(job)


@app.post("/api/historical-sr/fixed-level-analysis")
async def historical_sr_fixed_level_analysis(
    start_date: date = Form(...), end_date: date = Form(...),
    s1: float = Form(...), s2: float = Form(...), r1: float = Form(...), r2: float = Form(...),
    touch_tolerance: float = Form(2.0), reversal_points: float = Form(15.0),
    confirmation_candles: int = Form(3), failed_break_candles: int = Form(6),
    reset_points: float = Form(10.0), candles_file: UploadFile | None = File(None),
):
    """Test the exact Overview S1/S2/R1/R2 values as fixed prices across a historical candle range."""
    if end_date < start_date:
        raise HTTPException(422, "To date must be on or after From date.")
    if (end_date - start_date).days > 800:
        raise HTTPException(422, "The selected range exceeds 800 calendar days; choose a maximum two-year range.")
    if touch_tolerance < 0 or reversal_points <= 0 or confirmation_candles < 1 or failed_break_candles < 1 or reset_points < 0:
        raise HTTPException(422, "Invalid event-detection settings.")
    try:
        from app.auto_history import fetch_nse_candles
        if candles_file is not None and candles_file.filename:
            payload = await candles_file.read()
            if not payload:
                raise ValueError("The selected five-minute candle CSV is empty.")
            candle_source = f"Uploaded file: {candles_file.filename}"
        else:
            frame, candle_source = fetch_nse_candles(start_date, end_date)
            payload = frame.to_csv(index=False).encode("utf-8")
        candles = load_candles(payload)
        candles = candles[(candles["date"] >= start_date.isoformat()) & (candles["date"] <= end_date.isoformat())].copy()
        if candles.empty:
            raise ValueError(f"No valid five-minute candles found between {start_date.isoformat()} and {end_date.isoformat()}.")
        candles = candles.reset_index(drop=True)
        levels = {"S1": s1, "S2": s2, "R1": r1, "R2": r2}
        result = analyze_fixed_levels(candles, levels, touch_tolerance, reversal_points,
                                      confirmation_candles, failed_break_candles, reset_points)
        result["candle_source"] = candle_source
        result["requested_range"] = {"start": start_date.isoformat(), "end": end_date.isoformat()}
        result["coverage_note"] = f"Analyzed {result['trading_dates']} distinct candle dates and {result['candles_analyzed']} validated candles in the selected range. This is only as complete as the supplied files."
        return result
    except ValueError as exc:
        raise HTTPException(422, str(exc)) from exc
    except Exception as exc:
        raise HTTPException(502, f"Fixed-level historical analysis failed: {type(exc).__name__}: {exc}") from exc


@app.post("/api/historical-sr/analyze")
async def historical_sr_analyze(
    candles_file: UploadFile = File(...),
    levels_file: UploadFile = File(...),
    touch_tolerance: float = Form(2.0),
    reversal_points: float = Form(15.0),
    confirmation_candles: int = Form(3),
    failed_break_candles: int = Form(6),
    reset_points: float = Form(10.0),
):
    """Analyze uploaded actual 5-minute spot candles against per-date calculated levels.

    The level file must contain one row per trading date with S1/R1/S2/R2 values
    produced by the existing formula engine for that historical session.
    """
    try:
        candle_bytes = await candles_file.read()
        level_bytes = await levels_file.read()
        candles = load_candles(candle_bytes)
        levels = load_levels(level_bytes)
        result = analyze_historical_sr(candles, levels, touch_tolerance, reversal_points,
                                       confirmation_candles, failed_break_candles, reset_points)
        result["candle_source"] = candles_file.filename or "uploaded candles CSV"
        result["levels_source"] = levels_file.filename or "uploaded daily levels CSV"
        return result
    except ValueError as exc:
        raise HTTPException(422, str(exc)) from exc
    except Exception as exc:
        raise HTTPException(500, f"Historical S&R analysis failed: {type(exc).__name__}: {exc}") from exc



@app.post("/api/historical-sr/analyze/start")
async def start_historical_sr_analysis(
    background_tasks: BackgroundTasks,
    start_date: date = Form(...), end_date: date = Form(...),
    touch_tolerance: float = Form(2.0), reversal_points: float = Form(15.0),
    confirmation_candles: int = Form(3), failed_break_candles: int = Form(6),
    reset_points: float = Form(10.0),
    candles_file: UploadFile | None = File(None),
):
    """Start one date-range workflow: calculate daily levels and analyze candles if available."""
    if end_date < start_date:
        raise HTTPException(422, "End date must be on or after start date.")
    if (end_date - start_date).days > 800:
        raise HTTPException(422, "Requested range exceeds 800 calendar days. Run in smaller windows.")
    if touch_tolerance < 0 or reversal_points <= 0 or confirmation_candles < 1 or failed_break_candles < 1 or reset_points < 0:
        raise HTTPException(422, "Invalid event-detection settings.")
    uploaded_candles_bytes = None
    uploaded_candles_name = None
    if candles_file is not None and candles_file.filename:
        uploaded_candles_bytes = await candles_file.read()
        uploaded_candles_name = candles_file.filename
    job_id = uuid4().hex
    total = sum(1 for n in range((end_date-start_date).days+1) if (start_date+pd.Timedelta(days=n)).weekday()<5)
    settings = {"touch_tolerance": touch_tolerance, "reversal_points": reversal_points,
                "confirmation_candles": confirmation_candles, "failed_break_candles": failed_break_candles,
                "reset_points": reset_points}
    with _NSE_LEVEL_JOBS_LOCK:
        _NSE_LEVEL_JOBS[job_id] = {"job_id": job_id, "job_type": "combined_analysis", "status": "queued",
            "stage": "Queued", "completed_sessions": 0, "total_sessions": int(total),
            "rows_found": 0, "dates_skipped": 0, "current_date": None,
            "start_date": start_date.isoformat(), "end_date": end_date.isoformat(),
            "analysis_available": False}
    background_tasks.add_task(_run_historical_sr_analysis_job, job_id, start_date, end_date, settings, uploaded_candles_bytes, uploaded_candles_name)
    return {"job_id": job_id, "status": "queued", "total_sessions": int(total)}

@app.get("/api/historical-sr/analyze/status/{job_id}")
def historical_sr_analysis_status(job_id: str):
    with _NSE_LEVEL_JOBS_LOCK:
        job = _NSE_LEVEL_JOBS.get(job_id)
        if not job or job.get("job_type") != "combined_analysis":
            raise HTTPException(404, "Historical S&R analysis job was not found. Start Analyze again.")
        return dict(job)

@app.post("/api/historical-sr/nse-levels/start")
async def start_historical_sr_nse_levels(background_tasks: BackgroundTasks, start_date: date = Form(...), end_date: date = Form(...)):
    if end_date < start_date:
        raise HTTPException(422, "End date must be on or after start date.")
    if (end_date - start_date).days > 800:
        raise HTTPException(422, "Requested range exceeds 800 calendar days. Run in smaller windows.")
    job_id = uuid4().hex
    total = sum(1 for n in range((end_date-start_date).days+1) if (start_date+pd.Timedelta(days=n)).weekday()<5)
    with _NSE_LEVEL_JOBS_LOCK:
        _NSE_LEVEL_JOBS[job_id] = {"job_id": job_id, "status": "queued", "completed_sessions": 0,
            "total_sessions": int(total), "rows_found": 0, "dates_skipped": 0,
            "current_date": None, "start_date": start_date.isoformat(), "end_date": end_date.isoformat()}
    background_tasks.add_task(_run_nse_levels_job, job_id, start_date, end_date)
    return {"job_id": job_id, "status": "queued", "total_sessions": int(total)}

@app.get("/api/historical-sr/nse-levels/status/{job_id}")
def historical_sr_nse_levels_status(job_id: str):
    with _NSE_LEVEL_JOBS_LOCK:
        job = _NSE_LEVEL_JOBS.get(job_id)
        if not job:
            raise HTTPException(404, "Historical levels job was not found. Restart the fetch.")
        return dict(job)

@app.post("/api/historical-sr/nse-levels")
async def historical_sr_nse_levels(start_date: date = Form(...), end_date: date = Form(...)):
    """Fetch/cache daily S&R levels from official NSE option bhavcopies for CSV export."""
    if end_date < start_date:
        raise HTTPException(422, "End date must be on or after start date.")
    if (end_date - start_date).days > 800:
        raise HTTPException(422, "Requested range exceeds 800 calendar days. Run in smaller windows.")
    try:
        from app.auto_history import build_daily_levels
        levels, skipped = build_daily_levels(start_date, end_date)
        return {"levels": _serialize_df(levels), "skipped": skipped,
                "source": "Official NSE F&O bhavcopy archives; prior-session option closes",
                "count": int(len(levels))}
    except Exception as exc:
        raise HTTPException(502, f"NSE daily level retrieval failed: {type(exc).__name__}: {exc}") from exc


@app.post("/api/historical-sr/auto-run")
async def historical_sr_auto_run(
    start_date: date = Form(...), end_date: date = Form(...),
    touch_tolerance: float = Form(2.0), reversal_points: float = Form(15.0),
    confirmation_candles: int = Form(3), failed_break_candles: int = Form(6),
    reset_points: float = Form(10.0),
):
    """Load genuine five-minute OHLC exports from an authorized NSE intraday data source and calculate each date's levels from official NSE bhavcopies, then analyze."""
    try:
        settings = {"touch_tolerance": touch_tolerance, "reversal_points": reversal_points,
                    "confirmation_candles": confirmation_candles, "failed_break_candles": failed_break_candles,
                    "reset_points": reset_points}
        candles, levels, skipped, candle_source = automated_analysis(start_date, end_date, settings)
        result = analyze_historical_sr(load_candles(candles.to_csv(index=False).encode()),
                                       load_levels(levels.to_csv(index=False).encode()),
                                       touch_tolerance, reversal_points, confirmation_candles,
                                       failed_break_candles, reset_points)
        result["candle_source"] = candle_source
        result["levels_source"] = "NSE daily options bhavcopy; nearest unexpired expiry per session; existing HLC ATM and S1/R1/S2/R2 formula engine"
        result["skipped_level_dates"] = skipped
        result["data_note"] += f" Automated retrieval: {candle_source}. Daily level dates unavailable from NSE: {len(skipped)}."
        return result
    except ValueError as exc:
        raise HTTPException(422, str(exc)) from exc
    except Exception as exc:
        raise HTTPException(502, f"Automated historical S&R pipeline could not complete: {type(exc).__name__}: {exc}") from exc

@app.post("/api/historical-sr/export.xlsx")
async def historical_sr_export_xlsx(payload: dict):
    """Export actual analysis event rows to an Excel workbook."""
    events = payload.get("events", [])
    summary = payload.get("summary", [])
    output = BytesIO()
    try:
        with pd.ExcelWriter(output, engine="openpyxl") as writer:
            pd.DataFrame(events).to_excel(writer, index=False, sheet_name="Historical Events")
            pd.DataFrame(summary).to_excel(writer, index=False, sheet_name="Level Summary")
        output.seek(0)
        return StreamingResponse(output, media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                 headers={"Content-Disposition": "attachment; filename=historical_sr_analysis.xlsx"})
    except Exception as exc:
        raise HTTPException(500, f"Could not create Excel export: {exc}") from exc

@app.get("/api/expiries")
def available_expiries(trade_date: date, symbol: str = "NIFTY"):
    """Discover expiry dates from the selected exchange's historical source when possible."""
    symbol = symbol.upper().strip()
    if symbol != "NIFTY":
        raise HTTPException(400, "This dashboard supports NIFTY 50 only.")
    try:
        df, source = fetch_official_bhavcopy(trade_date)
        if "trade_date" not in df or "expiry" not in df:
            raise RuntimeError("Downloaded NSE file does not contain recognized trade-date and expiry columns.")
        sub = df[(df["trade_date"] == trade_date) &
                 (df["symbol"].astype(str).str.upper() == "NIFTY") &
                 (df["option_type"].astype(str).str.upper().isin(["CE", "PE"]))]
        if "instrument" in sub:
            # NSE UDiFF bhavcopy uses IDO for index derivatives options; legacy files use OPTIDX.
            sub = sub[sub["instrument"].astype(str).str.contains(r"OPTIDX|OPT|IDO|IO", case=False, regex=True, na=False)]
        dates = sorted({x for x in sub["expiry"].dropna().tolist() if isinstance(x, date) and x >= trade_date})
        if not dates:
            symbols_found = sorted(sub["symbol"].dropna().astype(str).str.upper().unique().tolist()) if not sub.empty else sorted(df.loc[df["trade_date"] == trade_date, "symbol"].dropna().astype(str).str.upper().unique().tolist()) if "symbol" in df else []
            instrument_values = sorted(df.loc[df["trade_date"] == trade_date, "instrument"].dropna().astype(str).str.upper().unique().tolist())[:12] if "instrument" in df else []
            raise RuntimeError(f"NSE archive downloaded, but no NIFTY option expiry dates were recognized for {trade_date.isoformat()}. Symbols for that date: {', '.join(symbols_found[:12]) or 'none'}. Instrument codes: {', '.join(instrument_values) or 'unavailable'}. The archive may be a different file format or have no NIFTY option rows.")
        return {"symbol": symbol, "trade_date": trade_date.isoformat(), "expiries": [x.isoformat() for x in dates], "source": source}
    except Exception as exc:
        raise HTTPException(422, f"Could not auto-detect exact expiries: {type(exc).__name__}: {exc}. You can still type an exact expiry date manually.")


@app.post("/api/fetch")
async def fetch(
    trade_date: date = Form(...),
    expiry_date: date = Form(...),
    symbol: str = Form("NIFTY"),
    manual_close: float | None = Form(None),
    interval: int = Form(50),
    itm_count: int = Form(7),
    otm_count: int = Form(7),
    atm_method: str = Form("hlc_min_diff"),
    manual_atm: float | None = Form(None),
    ce_reference_mode: str = Form("hlc"),
    pe_reference_mode: str = Form("hlc"),
    ce_manual: float | None = Form(None),
    pe_manual: float | None = Form(None),
    upload: UploadFile | None = File(None),
):
    symbol = symbol.upper().strip()
    if symbol != "NIFTY":
        raise HTTPException(400, "This dashboard supports NIFTY 50 only.")
    if trade_date > date.today():
        raise HTTPException(400, "Historical trading date cannot be in the future.")
    if expiry_date < trade_date:
        raise HTTPException(400, "Expiry date cannot be earlier than the selected trading date. Choose an expiry that was still valid during that historical session.")
    if interval <= 0 or itm_count < 0 or otm_count < 0 or itm_count > 30 or otm_count > 30:
        raise HTTPException(400, "Invalid strike interval/count.")
    try:
        if upload and upload.filename:
            payload = await upload.read()
            df, source = parse_bhavcopy_bytes(payload, f"user upload: {upload.filename}")
            source = "User-uploaded file — " + source
        else:
            df = None
            source = ""
            archive_error = None
            exact_session_error = None

            # Do not treat a successful HTTP download as success until it contains the exact selected session/expiry.
            try:
                df, source = fetch_official_bhavcopy(trade_date)
                try:
                    filter_session(df, trade_date, expiry_date, symbol)
                except ValueError as exc:
                    exact_session_error = exc
            except Exception as exc:
                archive_error = exc

            # If archive download failed OR its contents lack the exact rows, try NSE's historical contract API.
            if df is None or exact_session_error is not None:
                try:
                    if manual_close is not None:
                        spot = float(manual_close)
                        spot_source = "manual user input"
                    else:
                        spot, spot_source = fetch_nifty_close_api(trade_date, symbol)
                    strikes, _ = select_strikes(pd.DataFrame(), spot, interval, itm_count, otm_count,
                                                atm_method, manual_atm)
                    api_df, api_source = fetch_contract_api(trade_date, expiry_date, strikes, symbol)
                    df = api_df
                    if exact_session_error is not None:
                        source = f"NSE historical API fallback (archive had no exact rows: {exact_session_error}); {api_source}; index close: {spot_source}"
                    else:
                        source = f"NSE historical API fallback after archive failure ({type(archive_error).__name__}: {archive_error}); {api_source}; index close: {spot_source}"
                    if manual_close is None:
                        df["underlying"] = float(spot)
                except Exception as api_error:
                    reason = str(exact_session_error or archive_error or "archive unavailable")
                    raise RuntimeError(
                        "Automatic retrieval could not return the exact requested contracts. "
                        f"Bulk archive result: {reason} | Historical NSE API result: {api_error}. "
                        "No alternative date or expiry was substituted. Check the selected trading date/expiry or use a compatible historical CSV/ZIP."
                    ) from api_error
        result = _query(trade_date, expiry_date, symbol, df, source, manual_close, interval,
                        itm_count, otm_count, atm_method, manual_atm,
                        ce_reference_mode, pe_reference_mode, ce_manual, pe_manual)
        # Cache normalized source rows for this exact date + expiry, not an alternate expiry.
        key = _cache_key(symbol, trade_date, expiry_date)
        session = filter_session(df, trade_date, expiry_date, symbol)
        with sqlite3.connect(DB_PATH) as conn:
            conn.execute("INSERT OR REPLACE INTO query_cache(cache_key,source,rows_json) VALUES(?,?,?)",
                         (key, source, json.dumps(_serialize_df(session))))
        return JSONResponse(result)
    except (ValueError, RuntimeError) as exc:
        raise HTTPException(422, str(exc))
    except Exception as exc:
        raise HTTPException(500, f"Unexpected processing error: {type(exc).__name__}: {exc}")

@app.get("/api/cache")
def list_cache():
    with sqlite3.connect(DB_PATH) as conn:
        rows = conn.execute("SELECT cache_key, source, created_at FROM query_cache ORDER BY created_at DESC LIMIT 100").fetchall()
    return {"items": [{"key": r[0], "source": r[1], "cached_at": r[2]} for r in rows]}

@app.post("/api/export/csv")
async def export_csv(payload: dict):
    rows = payload.get("rows", [])
    if not isinstance(rows, list):
        raise HTTPException(400, "rows must be a list")
    df = pd.DataFrame(rows)
    stream = BytesIO(df.to_csv(index=False).encode("utf-8-sig"))
    return StreamingResponse(stream, media_type="text/csv; charset=utf-8",
        headers={"Content-Disposition": f'attachment; filename="{payload.get("metadata", {}).get("symbol", "index").lower()}_options.csv'})

@app.post("/api/export/xlsx")
async def export_xlsx(payload: dict):
    rows = payload.get("rows", [])
    if not isinstance(rows, list):
        raise HTTPException(400, "rows must be a list")
    df = pd.DataFrame(rows)
    stream = BytesIO()
    with pd.ExcelWriter(stream, engine="openpyxl") as writer:
        df.to_excel(writer, index=False, sheet_name="Options")
        meta = pd.DataFrame([payload.get("metadata", {})])
        meta.to_excel(writer, index=False, sheet_name="Session")
    stream.seek(0)
    return StreamingResponse(stream, media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers={"Content-Disposition": f'attachment; filename="{payload.get("metadata", {}).get("symbol", "index").lower()}_options.xlsx'})
