from __future__ import annotations
from datetime import date
from io import BytesIO
import zipfile
import pandas as pd
import requests

UDIFF_START = date(2024, 7, 8)
HEADERS = {
    "User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/124.0 Safari/537.36",
    "Accept": "text/csv,application/zip,application/octet-stream,*/*",
    "Referer": "https://www.nseindia.com/",
}

# Canonical app fields -> possible source field names (new UDiFF + legacy).
ALIASES = {
    "trade_date": ["BizDt", "TIMESTAMP", "Trade Date", "TRADE_DATE"],
    "symbol": ["TckrSymb", "SYMBOL", "Symbol"],
    "instrument": ["FinInstrmTp", "INSTRUMENT", "Instrument"],
    "expiry": ["XpryDt", "EXPIRY_DT", "Expiry Date", "Expiry"],
    "strike": ["StrkPric", "STRIKE_PR", "Strike Price"],
    "option_type": ["OptnTp", "OPTION_TYP", "Option Type"],
    "open": ["OpnPric", "OPEN", "Open Price"],
    "high": ["HghPric", "HIGH", "High Price"],
    "low": ["LwPric", "LOW", "Low Price"],
    "close": ["ClsPric", "CLOSE", "Close Price"],
    "ltp": ["LastPric", "LTP", "Last Price"],
    "settlement": ["SttlmPric", "SETTLE_PR", "Settle Price", "Daily Settlement Price"],
    "volume": ["TtlTradgVol", "CONTRACTS", "No. of contracts", "Volume"],
    "oi": ["OpnIntrst", "OPEN_INT", "Open Int", "Open Interest"],
    "change_oi": ["ChngInOpnIntrst", "CHG_IN_OI", "Change in OI"],
    "underlying": ["UndrlygPric", "UNDERLYING_VALUE", "Underlying Value"],
    "segment": ["Sgmt", "SEGMENT"],
    # Historical NSE API field names used by the foCPV endpoint.
    "trade_date": ["BizDt", "TIMESTAMP", "Trade Date", "TRADE_DATE", "FH_TIMESTAMP", "FH_TRADE_DATE", "TRADDT", "TRADEDATE", "DATE", "TrdDt", "Trading Date", "Trade Dt"],
    "symbol": ["TckrSymb", "SYMBOL", "Symbol", "FH_SYMBOL", "UNDERLYING_SYMBOL", "INDEX_NAME", "INDEXNAME", "SCRIP"],
    "expiry": ["XpryDt", "EXPIRY_DT", "Expiry Date", "Expiry", "FH_EXPIRY_DT", "EXPIRY_DATE", "EXPIRYDATE"],
    "strike": ["StrkPric", "STRIKE_PR", "Strike Price", "FH_STRIKE_PRICE", "STRIKE_PRICE", "STRIKEPRICE"],
    "option_type": ["OptnTp", "OPTION_TYP", "Option Type", "FH_OPTION_TYPE", "OPTION_TYPE", "OPTIONTYPE", "CE_PE"],
    "open": ["OpnPric", "OPEN", "Open Price", "OPEN_PRICE", "OPENING_PRICE", "FH_OPENING_PRICE", "Opening Price", "Open Rate"],
    "high": ["HghPric", "HIGH", "High Price", "HIGH_PRICE", "FH_TRADE_HIGH_PRICE", "High Rate"],
    "low": ["LwPric", "LOW", "Low Price", "LOW_PRICE", "FH_TRADE_LOW_PRICE", "Low Rate"],
    "close": ["ClsPric", "CLOSE", "Close Price", "CLOSE_PRICE", "FH_CLOSING_PRICE", "Closing Price", "Closing Rate"],
    "ltp": ["LastPric", "LTP", "Last Price", "LAST_PRICE", "FH_LAST_TRADED_PRICE", "Last Traded Price", "Last Rate"],
    "settlement": ["SttlmPric", "SETTLE_PR", "Settle Price", "SETTLEMENT_PRICE", "Daily Settlement Price", "FH_SETTLE_PRICE"],
    "volume": ["TtlTradgVol", "CONTRACTS", "No. of contracts", "No of Contracts", "Volume", "TOTAL_TRADED_QTY", "FH_TOT_TRADED_QTY", "Traded Quantity", "Total Traded Quantity", "Total Contracts"],
    "oi": ["OpnIntrst", "OPEN_INT", "Open Int", "Open Interest", "OPEN_INTEREST", "FH_OPEN_INT", "Open Interest Qty"],
    "change_oi": ["ChngInOpnIntrst", "CHG_IN_OI", "Change in OI", "CHANGE_IN_OI", "FH_CHANGE_IN_OI", "Change in Open Interest", "OI Change"],
    "underlying": ["UndrlygPric", "UNDERLYING_VALUE", "Underlying Value", "FH_UNDERLYING_VALUE", "Underlying Close", "Index Close"],
}

def _clean_columns(df: pd.DataFrame) -> pd.DataFrame:
    df.columns = [str(c).strip().replace("\ufeff", "") for c in df.columns]
    lookup = {str(c).strip().lower(): c for c in df.columns}
    rename = {}
    for target, names in ALIASES.items():
        for name in names:
            if name.lower() in lookup:
                rename[lookup[name.lower()]] = target
                break
    return df.rename(columns=rename)

def _parse_date_col(series: pd.Series) -> pd.Series:
    """Normalize exchange dates without misreading YYYYMMDD or ISO dates."""
    raw = series.astype("string").str.strip()
    # Numeric UDiFF fields sometimes arrive as YYYYMMDD rather than ISO text.
    compact = raw.str.fullmatch(r"\d{8}", na=False)
    parsed = pd.Series(pd.NaT, index=series.index, dtype="datetime64[ns]")
    if compact.any():
        parsed.loc[compact] = pd.to_datetime(raw.loc[compact], format="%Y%m%d", errors="coerce")
    other = ~compact & raw.notna() & raw.ne("")
    if other.any():
        # format='mixed' handles a column containing both ISO and DD-Mon-YYYY dates.
        parsed.loc[other] = pd.to_datetime(raw.loc[other], format="mixed", errors="coerce", dayfirst=True)
    return parsed.dt.date

def parse_bhavcopy_bytes(payload: bytes, filename: str = "bhavcopy") -> tuple[pd.DataFrame, str]:
    """Parse an official CSV or ZIP containing a CSV. Returns normalized rows and source label."""
    if not payload:
        raise ValueError("The uploaded/downloaded file is empty.")
    source = filename
    raw = payload
    if zipfile.is_zipfile(BytesIO(payload)):
        with zipfile.ZipFile(BytesIO(payload)) as zf:
            csv_names = [n for n in zf.namelist() if n.lower().endswith(".csv") and not n.endswith("/")]
            if not csv_names:
                raise ValueError("ZIP archive contains no CSV file.")
            # Prefer FO/UDiFF-like file names, otherwise first CSV.
            chosen = next((n for n in csv_names if "bhav" in n.lower() or "fo" in n.lower()), csv_names[0])
            raw = zf.read(chosen)
            source = f"{filename} → {chosen}"
    try:
        df = pd.read_csv(BytesIO(raw), low_memory=False)
    except Exception as exc:
        raise ValueError(f"Could not parse CSV: {exc}") from exc
    df = _clean_columns(df)
    required = {"symbol", "expiry", "strike", "option_type", "close"}
    missing = required - set(df.columns)
    if missing:
        raise ValueError("File is missing required contract columns: " + ", ".join(sorted(missing)))
    for col in ["trade_date", "expiry"]:
        if col in df:
            df[col] = _parse_date_col(df[col])
    for col in ["strike", "open", "high", "low", "close", "ltp", "settlement",
                "volume", "oi", "change_oi", "underlying"]:
        if col in df:
            df[col] = pd.to_numeric(df[col], errors="coerce")
    df["symbol"] = df["symbol"].astype(str).str.strip().str.upper()
    df["option_type"] = df["option_type"].astype(str).str.strip().str.upper()
    if "instrument" in df:
        df["instrument"] = df["instrument"].astype(str).str.strip().str.upper()
    return df, source

def archive_urls(trade_date: date) -> list[str]:
    if trade_date >= UDIFF_START:
        return [
            f"https://nsearchives.nseindia.com/content/fo/BhavCopy_NSE_FO_0_0_0_{trade_date:%Y%m%d}_F_0000.csv.zip",
            f"https://archives.nseindia.com/content/fo/BhavCopy_NSE_FO_0_0_0_{trade_date:%Y%m%d}_F_0000.csv.zip",
        ]
    mon = trade_date.strftime("%b").upper()
    return [
        f"https://archives.nseindia.com/content/historical/DERIVATIVES/{trade_date:%Y}/{mon}/fo{trade_date:%d}{mon}{trade_date:%Y}bhav.csv.zip",
        f"https://nsearchives.nseindia.com/content/historical/DERIVATIVES/{trade_date:%Y}/{mon}/fo{trade_date:%d}{mon}{trade_date:%Y}bhav.csv.zip",
    ]

def fetch_official_bhavcopy(trade_date: date, timeout: int = 20) -> tuple[pd.DataFrame, str]:
    errors = []
    session = requests.Session()
    # Warm up the official site to obtain ordinary session cookies where the site permits it.
    try:
        session.get("https://www.nseindia.com/", headers=HEADERS, timeout=timeout)
    except requests.RequestException:
        pass
    for url in archive_urls(trade_date):
        try:
            response = session.get(url, headers=HEADERS, timeout=timeout)
            if response.status_code != 200:
                errors.append(f"{url}: HTTP {response.status_code}")
                continue
            content_type = response.headers.get("content-type", "").lower()
            if not response.content or (b"<html" in response.content[:500].lower()):
                errors.append(f"{url}: response was empty or HTML, not an archive")
                continue
            df, label = parse_bhavcopy_bytes(response.content, url.rsplit("/", 1)[-1])
            return df, f"NSE public archive ({label}; HTTP 200; content-type={content_type or 'unspecified'})"
        except (requests.RequestException, ValueError) as exc:
            errors.append(f"{url}: {exc}")
    raise RuntimeError("Automatic NSE archive retrieval failed. " + " | ".join(errors))

def _api_session(timeout: int = 20) -> requests.Session:
    session = requests.Session()
    session.headers.update({
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/136.0.0.0 Safari/537.36",
        "Accept": "application/json, text/javascript, */*; q=0.01",
        "Accept-Language": "en-IN,en-US;q=0.9,en;q=0.8",
        "Referer": "https://www.nseindia.com/report-detail/fo_eq_security",
        "X-Requested-With": "XMLHttpRequest",
    })
    # Normal session warm-up only; no authentication or access-control bypass.
    session.get("https://www.nseindia.com/report-detail/fo_eq_security", timeout=timeout)
    return session

def _records_from_json(payload):
    if isinstance(payload, list):
        return payload
    if isinstance(payload, dict):
        data = payload.get("data")
        if isinstance(data, list):
            return data
        # Some NSE responses wrap records one level deeper.
        for value in payload.values():
            if isinstance(value, (dict, list)):
                found = _records_from_json(value)
                if found:
                    return found
    return []

def fetch_nifty_close_api(trade_date: date, symbol: str = "NIFTY", timeout: int = 20) -> tuple[float, str]:
    """Best-effort NSE historical index API lookup for the NIFTY 50 close."""
    if symbol.upper() != "NIFTY":
        raise ValueError("This dashboard supports NIFTY 50 only.")
    index_name = "NIFTY 50"
    session = _api_session(timeout)
    params = {"indexType": index_name, "from": trade_date.strftime("%d-%m-%Y"), "to": trade_date.strftime("%d-%m-%Y")}
    url = "https://www.nseindia.com/api/historicalOR/indicesHistory"
    response = session.get(url, params=params, timeout=timeout)
    response.raise_for_status()
    payload = response.json()
    records = _records_from_json(payload)
    for rec in records:
        if not isinstance(rec, dict):
            continue
        normalized = {str(k).lower().replace("_", ""): v for k, v in rec.items()}
        close = next((normalized[k] for k in normalized if k in {"eodcloseindexval", "closeindexval", "indexclose", "close", "closingprice"}), None)
        day_value = next((normalized[k] for k in normalized if k in {"eodindexname", "indexname", "index"}), "")
        if close is not None and (not day_value or index_name.lower() in str(day_value).lower()):
            try:
                return float(str(close).replace(",", "")), "NSE historical index API"
            except ValueError:
                pass
    raise RuntimeError(f"NSE historical index API returned no recognizable {index_name} close for this date.")

def fetch_contract_api(trade_date: date, expiry_date: date, strikes: list[float], symbol: str = "NIFTY", timeout: int = 20) -> tuple[pd.DataFrame, str]:
    """Fetch exact-date index option rows from NSE's historical foCPV endpoint.

    This API is an automatic fallback when the bulk bhavcopy archive cannot be fetched.
    NSE may restrict access or limit the historical period; failures are reported to the caller.
    """
    symbol = symbol.upper()
    if symbol != "NIFTY":
        raise ValueError("This dashboard supports NIFTY 50 only.")
    session = _api_session(timeout)
    url = "https://www.nseindia.com/api/historicalOR/foCPV"
    frames = []
    errors = []
    expiry_text = expiry_date.strftime("%d-%b-%Y").upper()
    for strike in strikes:
        for side in ("CE", "PE"):
            params = {
                "from": trade_date.strftime("%d-%m-%Y"),
                "to": trade_date.strftime("%d-%m-%Y"),
                "instrumentType": "OPTIDX", "symbol": symbol,
                "year": str(expiry_date.year), "expiryDate": expiry_text,
                "optionType": side, "strikePrice": str(int(strike) if float(strike).is_integer() else strike),
            }
            try:
                response = session.get(url, params=params, timeout=timeout)
                if response.status_code != 200:
                    errors.append(f"{strike}{side}: HTTP {response.status_code}")
                    continue
                records = _records_from_json(response.json())
                if not records:
                    continue
                df = _clean_columns(pd.DataFrame(records))
                # API payloads sometimes omit symbol/expiry/strike because they are query filters.
                if "symbol" not in df: df["symbol"] = symbol
                if "expiry" not in df: df["expiry"] = expiry_date
                if "strike" not in df: df["strike"] = float(strike)
                if "option_type" not in df: df["option_type"] = side
                if "trade_date" not in df: df["trade_date"] = trade_date
                for col in ("trade_date", "expiry"):
                    if col in df: df[col] = _parse_date_col(df[col])
                for col in ("strike", "open", "high", "low", "close", "ltp", "settlement", "volume", "oi", "change_oi", "underlying"):
                    if col in df: df[col] = pd.to_numeric(df[col], errors="coerce")
                df["symbol"] = df["symbol"].astype(str).str.upper().str.strip()
                df["option_type"] = df["option_type"].astype(str).str.upper().str.strip()
                frames.append(df)
            except Exception as exc:
                errors.append(f"{strike}{side}: {type(exc).__name__}: {exc}")
    if not frames:
        raise RuntimeError("NSE historical contract API did not return requested contracts. " + " | ".join(errors[:8]))
    merged = pd.concat(frames, ignore_index=True)
    # Keep only exact requested session, expiry and requested strike/side records.
    merged = merged[(merged["symbol"] == symbol) & (merged["trade_date"] == trade_date) & (merged["expiry"] == expiry_date)]
    if merged.empty:
        raise RuntimeError("NSE historical contract API returned rows, but none matched the exact selected trading date and expiry.")
    return merged, f"NSE historical foCPV API for {symbol} (exact session/expiry; {len(merged)} rows; per-contract requests)"

def filter_session(df: pd.DataFrame, trade_date: date, expiry_date: date, symbol: str = "NIFTY") -> pd.DataFrame:
    out = df.copy()
    if "trade_date" in out:
        out = out[out["trade_date"] == trade_date]
    else:
        # A file without a date field cannot independently verify the selected session.
        raise ValueError("The source file has no recognized trade-date field; selected session cannot be verified.")
    symbol = symbol.upper()
    out = out[(out["symbol"] == symbol) & (out["option_type"].isin(["CE", "PE"]))]
    out = out[out["expiry"] == expiry_date]
    if "instrument" in out:
        # UDiFF option instrument code is OPTIDX; legacy is OPTIDX.
        out = out[out["instrument"].str.contains(r"OPTIDX|OPT|IDO|IO", case=False, regex=True, na=False)]
    if out.empty:
        # Make diagnostics useful: differentiate a missing session, symbol, or expiry.
        raw = df.copy()
        if "trade_date" in raw:
            date_rows = raw[raw["trade_date"] == trade_date]
        else:
            date_rows = raw.iloc[0:0]
        day_symbols = sorted(date_rows["symbol"].dropna().astype(str).str.upper().unique().tolist()) if "symbol" in date_rows else []
        symbol_rows = date_rows[date_rows["symbol"].astype(str).str.upper() == symbol] if "symbol" in date_rows else date_rows.iloc[0:0]
        available_expiries = sorted(str(x) for x in symbol_rows["expiry"].dropna().unique().tolist()) if "expiry" in symbol_rows else []
        details = []
        if len(date_rows) == 0:
            details.append("the source contains no rows for that trading date")
        elif symbol not in day_symbols:
            details.append("symbols found for that date: " + ", ".join(day_symbols[:12]))
        elif available_expiries:
            details.append("expiries found for the selected symbol/date: " + ", ".join(available_expiries[:12]))
        suffix = (" Diagnostics: " + "; ".join(details) + ".") if details else ""
        raise ValueError(
            f"No {symbol} CE/PE rows were found for trading date {trade_date} and exact expiry {expiry_date}. "
            "The date may be a non-trading day, the source may not contain that contract, or date/symbol/expiry fields may use an unrecognized format."
            + suffix
        )
    return out.sort_values(["strike", "option_type"]).reset_index(drop=True)

def select_strikes(df: pd.DataFrame, spot: float, interval: int = 50,
                   itm_count: int = 7, otm_count: int = 7,
                   atm_method: str = "hlc_min_diff", manual_atm: float | None = None) -> tuple[list[float], float]:
    """Choose ATM by the smallest absolute CE-close / PE-close difference.

    For HLC ATM, compare every strike in the supplied session that has both
    CE and PE closing prices. The spot price is used only to break a tie. If
    df is empty (the API fallback's pre-fetch step), return a spot-centred
    preliminary strike window so the caller can fetch contracts first.
    """
    if interval <= 0 or itm_count < 0 or otm_count < 0:
        raise ValueError("Strike interval must be positive and ITM/OTM counts cannot be negative.")
    base = round(float(spot) / interval) * interval
    if manual_atm is not None:
        atm = float(manual_atm)
    elif atm_method == "hlc_min_diff":
        if df is None or df.empty:
            atm = float(base)
        else:
            required = {"strike", "option_type", "close"}
            if not required.issubset(df.columns):
                raise ValueError("Cannot calculate HLC ATM: source is missing strike, option type, or close fields.")
            candidates = df[df["option_type"].isin(["CE", "PE"])].copy()
            candidates["strike"] = pd.to_numeric(candidates["strike"], errors="coerce")
            candidates["close"] = pd.to_numeric(candidates["close"], errors="coerce")
            candidates = candidates.dropna(subset=["strike", "close"])
            closes = candidates.pivot_table(index="strike", columns="option_type", values="close", aggfunc="first")
            if not {"CE", "PE"}.issubset(closes.columns):
                raise ValueError("Cannot calculate HLC ATM: no strike has both CE and PE closing prices.")
            closes = closes.dropna(subset=["CE", "PE"])
            if closes.empty:
                raise ValueError("Cannot calculate HLC ATM: no strike has both CE and PE closing prices.")
            closes["close_diff"] = (closes["CE"] - closes["PE"]).abs()
            closes["distance_to_spot"] = abs(closes.index.astype(float) - float(spot))
            # Smallest absolute close difference first; ties: nearest spot, then lower strike.
            chosen = closes.reset_index().sort_values(
                ["close_diff", "distance_to_spot", "strike"], ascending=[True, True, True]
            ).iloc[0]["strike"]
            atm = float(chosen)
    elif atm_method == "nearest50":
        atm = round(spot / 50) * 50
    elif atm_method == "nearest100":
        atm = round(spot / 100) * 100
    elif atm_method == "manual":
        raise ValueError("Enter a manual ATM strike when Manual strike is selected.")
    else:
        raise ValueError("ATM method must be hlc_min_diff, nearest50, nearest100, or manual.")
    strikes = [float(atm + i * interval) for i in range(-itm_count, otm_count + 1)]
    return strikes, atm

def get_underlying_close(df: pd.DataFrame, manual_close: float | None = None) -> tuple[float | None, str]:
    if manual_close is not None:
        return float(manual_close), "manual user input"
    if "underlying" in df:
        values = df["underlying"].dropna()
        if not values.empty:
            # Bhavcopy underlying value is carried from source; use the most frequent rounded value.
            return float(values.median()), "source underlying value (not independently verified index close)"
    return None, "unavailable; enter selected index close manually"

def build_dashboard_rows(df: pd.DataFrame, strikes: list[float]) -> list[dict]:
    result = []
    for strike in strikes:
        pair = {"strike": float(strike)}
        for side in ("CE", "PE"):
            matches = df[(df["strike"] == strike) & (df["option_type"] == side)]
            row = matches.iloc[0].to_dict() if not matches.empty else {}
            for field in ("open", "high", "low", "close", "ltp", "settlement", "volume", "oi", "change_oi"):
                pair[f"{side.lower()}_{field}"] = row.get(field)
            pair[f"{side.lower()}_available"] = not matches.empty
        ce, pe = pair["ce_close"], pair["pe_close"]
        pair["bep"] = (float(ce) + float(pe)) / 2 if pd.notna(ce) and pd.notna(pe) else None
        result.append(pair)
    return result

def support_resistance(atm_strike: float, ce_reference: float | None, pe_reference: float | None) -> dict:
    """Calculate requested levels using the selected HLC ATM strike as the base."""
    def valid(x): return x is not None and pd.notna(x)
    ce = float(ce_reference) if valid(ce_reference) else None
    pe = float(pe_reference) if valid(pe_reference) else None
    return {
        "S1": atm_strike - pe if pe is not None else None,
        "R1": atm_strike + ce if ce is not None else None,
        "S2": atm_strike - (ce + pe) if ce is not None and pe is not None else None,
        "R2": atm_strike + (ce + pe) if ce is not None and pe is not None else None,
    }
