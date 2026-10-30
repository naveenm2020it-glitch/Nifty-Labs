"""Historical S/R event analysis for actual 5-minute spot candles and date-specific levels.

This module deliberately does not invent or silently fetch substitute candles. The caller
provides a timestamped spot-candle CSV and a separate date-specific level CSV generated from
this application's existing formula engine. All timestamps are interpreted/displayed in IST.
"""
from __future__ import annotations
from datetime import time
import pandas as pd

CANDLE_ALIASES = {
    "timestamp": ["timestamp", "datetime", "date_time", "date time", "time"],
    "open": ["open", "o"], "high": ["high", "h"], "low": ["low", "l"], "close": ["close", "c"],
}
LEVEL_ALIASES = {"date": ["date", "trading_date", "trade_date", "session_date"],
                 "S1": ["s1", "support1", "support_1"], "R1": ["r1", "resistance1", "resistance_1"],
                 "S2": ["s2", "support2", "support_2"], "R2": ["r2", "resistance2", "resistance_2"]}
LEVELS = ["S1", "R1", "S2", "R2"]


def _normalise_columns(df: pd.DataFrame) -> pd.DataFrame:
    out = df.copy()
    out.columns = [str(c).strip().lower().replace("-", "_") for c in out.columns]
    return out


def _rename_aliases(df: pd.DataFrame, aliases: dict, required: list[str]) -> pd.DataFrame:
    df = _normalise_columns(df)
    ren = {}
    for target, choices in aliases.items():
        for choice in choices:
            if choice.lower() in df.columns:
                ren[choice.lower()] = target
                break
    df = df.rename(columns=ren)
    missing = [c for c in required if c not in df.columns]
    if missing:
        raise ValueError("CSV is missing required columns: " + ", ".join(missing))
    return df


def load_candles(payload: bytes) -> pd.DataFrame:
    try: raw = pd.read_csv(pd.io.common.BytesIO(payload))
    except Exception as exc: raise ValueError(f"Could not read candle CSV: {exc}") from exc
    df = _rename_aliases(raw, CANDLE_ALIASES, ["timestamp", "open", "high", "low", "close"])
    # Treat timezone-naive source timestamps as IST; convert aware timestamps to IST.
    ts = pd.to_datetime(df["timestamp"], errors="coerce", dayfirst=True, format="mixed")
    if getattr(ts.dt, "tz", None) is None: ts = ts.dt.tz_localize("Asia/Kolkata", ambiguous="NaT", nonexistent="NaT")
    else: ts = ts.dt.tz_convert("Asia/Kolkata")
    df["timestamp"] = ts
    for c in ["open", "high", "low", "close"]: df[c] = pd.to_numeric(df[c], errors="coerce")
    df = df.dropna(subset=["timestamp", "open", "high", "low", "close"])
    df = df[(df["high"] >= df["low"]) & (df["high"] >= df[["open", "close"]].max(axis=1)) & (df["low"] <= df[["open", "close"]].min(axis=1))]
    df = df.drop_duplicates(subset=["timestamp"], keep="last").sort_values("timestamp").reset_index(drop=True)
    if df.empty: raise ValueError("No valid OHLC candles remain after validation.")
    df["date"] = df["timestamp"].dt.strftime("%Y-%m-%d")
    return df


def load_levels(payload: bytes) -> pd.DataFrame:
    try: raw = pd.read_csv(pd.io.common.BytesIO(payload))
    except Exception as exc: raise ValueError(f"Could not read levels CSV: {exc}") from exc
    df = _rename_aliases(raw, LEVEL_ALIASES, ["date", *LEVELS])
    df["date"] = pd.to_datetime(df["date"], errors="coerce", dayfirst=True, format="mixed").dt.strftime("%Y-%m-%d")
    for c in LEVELS: df[c] = pd.to_numeric(df[c], errors="coerce")
    df = df.dropna(subset=["date"]).drop_duplicates(subset=["date"], keep="last")
    if df.empty: raise ValueError("No valid date-specific S1/R1/S2/R2 levels found.")
    return df


def _fmt_ts(ts): return ts.strftime("%d-%m-%Y %H:%M") if pd.notna(ts) else None


def analyze(candles: pd.DataFrame, levels: pd.DataFrame, tolerance: float = 2.0,
            reversal_points: float = 15.0, confirmation_candles: int = 3,
            failed_break_candles: int = 6, reset_points: float = 10.0) -> dict:
    if tolerance < 0 or reversal_points <= 0 or confirmation_candles < 1 or failed_break_candles < 1 or reset_points < 0:
        raise ValueError("Invalid event-detection settings.")
    level_map = levels.set_index("date").to_dict("index")
    events = []
    eligible_touches = {x: 0 for x in LEVELS}
    # Analyze each trading date separately; never carry an interaction across sessions.
    for day, day_df in candles.groupby("date", sort=True):
        if day not in level_map: continue
        day_df = day_df.reset_index(drop=True)
        day_levels = level_map[day]
        for level_name in LEVELS:
            level = day_levels.get(level_name)
            if pd.isna(level): continue
            support = level_name.startswith("S")
            i = 0
            while i < len(day_df):
                row = day_df.iloc[i]
                if row.high < level - tolerance or row.low > level + tolerance:
                    i += 1; continue
                start = i
                # Merge consecutive candles that interact with the same level.
                j = i + 1
                while j < len(day_df):
                    nxt = day_df.iloc[j]
                    if nxt.high >= level - tolerance and nxt.low <= level + tolerance:
                        j += 1
                    else:
                        # Keep interaction open until price has reset sufficiently away.
                        if (support and nxt.close >= level + reset_points) or ((not support) and nxt.close <= level - reset_points): break
                        if abs(float(nxt.close) - float(level)) <= tolerance + reset_points: j += 1
                        else: break
                end = min(j - 1, len(day_df) - 1)
                first = day_df.iloc[start]
                touch_id = f"{day}-{level_name}-{first.timestamp.strftime('%H%M%S')}"
                interaction = day_df.iloc[start:end + 1]
                high_seen, low_seen = float(interaction.high.max()), float(interaction.low.min())
                event_base = {"event_date": day, "event_time": _fmt_ts(first.timestamp), "level": level_name,
                              "level_value": float(level), "event_type": "Touch", "spot_open": float(first.open),
                              "spot_high": float(first.high), "spot_low": float(first.low), "spot_close": float(first.close),
                              "initial_event_datetime": _fmt_ts(first.timestamp), "confirmation_datetime": None,
                              "distance_points": round(min(abs(float(first.high)-level), abs(float(first.low)-level), abs(float(first.close)-level)), 2),
                              "result_points": None, "status": "Confirmed", "explanation": "First candle in a grouped level interaction.", "touch_id": touch_id}
                events.append(event_base)
                future = day_df.iloc[end + 1:min(len(day_df), end + 1 + max(confirmation_candles, failed_break_candles))]
                outcome = None
                # Confirm break only on candle close; failed break is a later return across level.
                break_idx = None
                for k in range(end + 1, min(len(day_df), end + 1 + failed_break_candles)):
                    rr = day_df.iloc[k]
                    broken = float(rr.close) > level + tolerance if not support else float(rr.close) < level - tolerance
                    if broken:
                        break_idx = k; break
                if break_idx is not None:
                    br = day_df.iloc[break_idx]
                    fail_end = min(len(day_df), break_idx + 1 + failed_break_candles)
                    failed_idx = None
                    for k in range(break_idx + 1, fail_end):
                        rr = day_df.iloc[k]
                        back = float(rr.close) <= level if not support else float(rr.close) >= level
                        if back: failed_idx = k; break
                    if failed_idx is not None:
                        rr = day_df.iloc[failed_idx]
                        outcome = ("Failed Breakout" if not support else "Failed Breakdown", rr, float(rr.close)-float(br.close), "Close returned across the level within the configured failure window.")
                    else:
                        outcome = ("Breakout" if not support else "Breakdown", br, float(br.close)-float(level), "5-minute candle closed beyond the level; no failure was observed within the configured window.")
                if outcome is None:
                    window = day_df.iloc[end + 1:min(len(day_df), end + 1 + confirmation_candles)]
                    for _, rr in window.iterrows():
                        move = float(rr.close)-float(level)
                        confirmed_away = move >= reversal_points if support else move <= -reversal_points
                        if confirmed_away:
                            outcome = ("Reversal", rr, move, "Price moved away from the level by the configured reversal distance."); break
                    if outcome is None and len(window) > 0:
                        max_move = max((float(x.close)-float(level) for _, x in window.iterrows()), default=0) if support else min((float(x.close)-float(level) for _, x in window.iterrows()), default=0)
                        possible = max_move > tolerance if support else max_move < -tolerance
                        if possible:
                            rr = window.iloc[-1]
                            outcome = ("Potential Reversal", rr, float(rr.close)-float(level), "Some movement away was observed, but the confirmation threshold was not met.")
                if outcome:
                    typ, rr, result, explanation = outcome
                    status = "Potential" if typ == "Potential Reversal" else "Confirmed"
                    initial_dt = _fmt_ts(br.timestamp) if break_idx is not None else event_base["initial_event_datetime"]
                    events.append({**event_base, "event_date": rr.timestamp.strftime("%Y-%m-%d"), "event_time": _fmt_ts(rr.timestamp),
                                   "event_type": typ, "spot_open": float(rr.open), "spot_high": float(rr.high),
                                   "spot_low": float(rr.low), "spot_close": float(rr.close),
                                   "initial_event_datetime": initial_dt, "confirmation_datetime": _fmt_ts(rr.timestamp),
                                   "distance_points": round(float(rr.close)-float(level), 2),
                                   "result_points": round(result, 2), "status": status, "explanation": explanation,
                                   "touch_id": touch_id})
                    # Reversal rate denominator includes only interactions with a resolved,
                    # confirmed outcome; potential and unresolved/ambiguous touches are excluded.
                    if status == "Confirmed": eligible_touches[level_name] += 1
                # Advance past the whole grouped interaction; no duplicate per-candle touches.
                i = max(end + 1, start + 1)
    events.sort(key=lambda e: (e["event_date"], e["event_time"] or "", e["level"]))
    counts = {level: {"touches": 0, "confirmed_reversals": 0, "potential_reversals": 0, "breakouts": 0,
                      "breakdowns": 0, "failed_breaks": 0, "failed_breakouts": 0, "failed_breakdowns": 0,
                      "eligible_touches": 0, "reversal_rate": 0.0} for level in LEVELS}
    for e in events:
        c = counts[e["level"]]
        if e["event_type"] == "Touch": c["touches"] += 1
        elif e["event_type"] == "Reversal" and e["status"] == "Confirmed": c["confirmed_reversals"] += 1
        elif e["event_type"] == "Potential Reversal": c["potential_reversals"] += 1
        elif e["event_type"] == "Breakout": c["breakouts"] += 1
        elif e["event_type"] == "Breakdown": c["breakdowns"] += 1
        elif e["event_type"] == "Failed Breakout":
            c["failed_breaks"] += 1
            c["failed_breakouts"] += 1
        elif e["event_type"] == "Failed Breakdown":
            c["failed_breaks"] += 1
            c["failed_breakdowns"] += 1
    for level in LEVELS:
        counts[level]["eligible_touches"] = eligible_touches[level]
        counts[level]["reversal_rate"] = round(100*counts[level]["confirmed_reversals"]/counts[level]["eligible_touches"], 2) if counts[level]["eligible_touches"] else 0.0
    matched_dates = sorted(set(candles.date) & set(levels.date))
    used = candles[candles.date.isin(matched_dates)]
    return {"status":"success", "timezone":"Asia/Kolkata", "data_range":{"start":used.timestamp.min().strftime("%d-%m-%Y %H:%M") if len(used) else None,
            "end":used.timestamp.max().strftime("%d-%m-%Y %H:%M") if len(used) else None},
            "sessions_analyzed":len(matched_dates), "candles_analyzed":int(len(used)),
            "unmatched_candle_dates":sorted(set(candles.date)-set(levels.date)),
            "level_dates_without_candles":sorted(set(levels.date)-set(candles.date)),
            "counts":counts, "events":events,
            "settings":{"touch_tolerance_points":tolerance,"reversal_distance_points":reversal_points,
                         "confirmation_candles":confirmation_candles,"failed_break_candles":failed_break_candles,"reset_points":reset_points},
            "data_note":"Only uploaded, validated 5-minute candles and matching date-specific S1/R1/S2/R2 levels were analyzed. No candles or levels were fabricated. Candle-close data cannot establish the intrabar order of high and low."}


def analyze_fixed_levels(candles: pd.DataFrame, fixed_levels: dict[str, float],
                         tolerance: float = 2.0, reversal_points: float = 15.0,
                         confirmation_candles: int = 3, failed_break_candles: int = 6,
                         reset_points: float = 10.0) -> dict:
    """Analyze the exact four fixed levels shown on the Overview tab across all supplied candles.

    Unlike ``analyze`` above, this deliberately does NOT recalculate levels per historical date.
    Each Overview value is treated as one fixed horizontal price for the full selected range.
    Each grouped interaction gets one mutually-exclusive outcome: Reversal, Breakout, Breakdown,
    Fake Breakout, Fake Breakdown, or Unresolved. Percentages use all grouped touches as the
    denominator, so unresolved touches remain visible and no event is silently discarded.
    """
    import math
    if tolerance < 0 or reversal_points <= 0 or confirmation_candles < 1 or failed_break_candles < 1 or reset_points < 0:
        raise ValueError("Invalid event-detection settings.")
    levels = {}
    for name in ["S1", "S2", "R1", "R2"]:
        value = fixed_levels.get(name)
        try:
            value = float(value)
        except (TypeError, ValueError):
            raise ValueError(f"Overview level {name} is missing or not numeric. Fetch data in the Data session first.")
        if not math.isfinite(value):
            raise ValueError(f"Overview level {name} is missing or not finite. Fetch data in the Data session first.")
        levels[name] = value
    needed = {"timestamp", "open", "high", "low", "close", "date"}
    missing = needed - set(candles.columns)
    if missing:
        raise ValueError("Validated candles are missing columns: " + ", ".join(sorted(missing)))
    if candles.empty:
        raise ValueError("No genuine five-minute candles are available in the selected date range.")

    events = []
    outcome_names = ["Reversal", "Breakout", "Breakdown", "Fake Breakout", "Fake Breakdown", "Unresolved"]
    counts = {name: {"touches": 0, **{x.lower().replace(" ", "_"): 0 for x in outcome_names},
                     **{x.lower().replace(" ", "_") + "_pct": 0.0 for x in outcome_names}}
              for name in ["S1", "S2", "R1", "R2"]}

    for day, day_df in candles.groupby("date", sort=True):
        day_df = day_df.reset_index(drop=True)
        if day_df.empty:
            continue
        for name, level in levels.items():
            i = 0
            while i < len(day_df):
                row = day_df.iloc[i]
                touches = float(row.high) >= level - tolerance and float(row.low) <= level + tolerance
                if not touches:
                    i += 1
                    continue
                start_i = i
                prior_close = float(day_df.iloc[i - 1].close) if i > 0 else float(row.open)
                approach_side = "below" if prior_close < level else ("above" if prior_close > level else ("below" if float(row.open) < level else "above"))

                # Detect close-crossing breaks starting at the touch candle itself.
                max_check = min(len(day_df), start_i + 1 + max(confirmation_candles, failed_break_candles))
                break_idx = None
                break_type = None
                previous = prior_close
                for k in range(start_i, max_check):
                    close = float(day_df.iloc[k].close)
                    if close > level + tolerance and previous <= level + tolerance:
                        break_idx, break_type = k, "Breakout"
                        break
                    if close < level - tolerance and previous >= level - tolerance:
                        break_idx, break_type = k, "Breakdown"
                        break
                    previous = close

                outcome = "Unresolved"
                confirm_idx = None
                explanation = "Price touched the fixed Overview level, but no configured outcome was confirmed."
                initial_idx = start_i
                if break_idx is not None:
                    initial_idx = break_idx
                    confirm_idx = break_idx
                    fail_end = min(len(day_df), break_idx + 1 + failed_break_candles)
                    returned_idx = None
                    for k in range(break_idx + 1, fail_end):
                        close = float(day_df.iloc[k].close)
                        returned = close <= level if break_type == "Breakout" else close >= level
                        if returned:
                            returned_idx = k
                            break
                    if returned_idx is not None:
                        outcome = "Fake Breakout" if break_type == "Breakout" else "Fake Breakdown"
                        confirm_idx = returned_idx
                        explanation = f"{break_type} close returned across the level within {failed_break_candles} candles."
                    else:
                        outcome = break_type
                        explanation = f"Five-minute close crossed the fixed level; no return across it was observed in the {failed_break_candles}-candle failure window."
                else:
                    limit = min(len(day_df), start_i + 1 + confirmation_candles)
                    for k in range(start_i + 1, limit):
                        close = float(day_df.iloc[k].close)
                        move_back_to_approach_side = (close <= level - reversal_points) if approach_side == "below" else (close >= level + reversal_points)
                        if move_back_to_approach_side:
                            outcome, confirm_idx = "Reversal", k
                            explanation = f"Price moved back to the approach side by at least {reversal_points:g} points within {confirmation_candles} candles."
                            break

                # Store one dated row for each interaction, including unresolved touches.
                first = day_df.iloc[start_i]
                last = day_df.iloc[confirm_idx if confirm_idx is not None else start_i]
                event_time = first.timestamp.strftime("%d-%m-%Y %H:%M") if hasattr(first.timestamp, "strftime") else str(first.timestamp)
                confirmation_time = last.timestamp.strftime("%d-%m-%Y %H:%M") if hasattr(last.timestamp, "strftime") else str(last.timestamp)
                events.append({
                    "event_date": str(day), "event_time": event_time.split(" ")[-1],
                    "level": name, "level_value": level, "event_type": outcome,
                    "approach_side": approach_side, "spot_open": float(first.open),
                    "spot_high": float(first.high), "spot_low": float(first.low),
                    "spot_close": float(first.close), "initial_event_datetime": event_time,
                    "confirmation_datetime": confirmation_time if confirm_idx is not None else None,
                    "result_points": round(float(last.close) - level, 2),
                    "status": "Confirmed" if outcome != "Unresolved" else "Unresolved",
                    "explanation": explanation,
                })
                count_row = counts[name]
                count_row["touches"] += 1
                key = outcome.lower().replace(" ", "_")
                count_row[key] += 1

                # Avoid duplicate events from every candle in the same near-level cluster.
                j = start_i + 1
                away_streak = 0
                while j < len(day_df):
                    rr = day_df.iloc[j]
                    hit = float(rr.high) >= level - tolerance and float(rr.low) <= level + tolerance
                    far = abs(float(rr.close) - level) >= reset_points if reset_points > 0 else not hit
                    away_streak = away_streak + 1 if (far and not hit) else 0
                    if away_streak >= 2:
                        j += 1
                        break
                    j += 1
                i = max(j, start_i + 1)

    for name, row in counts.items():
        touches = row["touches"]
        for outcome in outcome_names:
            key = outcome.lower().replace(" ", "_")
            row[key + "_pct"] = round(100 * row[key] / touches, 2) if touches else 0.0

    events.sort(key=lambda e: (e["event_date"], e["event_time"], e["level"]))
    timestamps = candles["timestamp"]
    return {
        "status": "success", "analysis_mode": "fixed_overview_levels",
        "timezone": "Asia/Kolkata", "fixed_levels": levels,
        "data_range": {"start": timestamps.min().strftime("%d-%m-%Y %H:%M"), "end": timestamps.max().strftime("%d-%m-%Y %H:%M")},
        "trading_dates": int(candles["date"].nunique()), "candles_analyzed": int(len(candles)),
        "counts": counts, "events": events,
        "settings": {"touch_tolerance_points": tolerance, "reversal_distance_points": reversal_points,
                      "confirmation_candles": confirmation_candles, "failed_break_candles": failed_break_candles,
                      "reset_points": reset_points},
        "data_note": "The exact S1/S2/R1/R2 values from the Overview tab were treated as fixed horizontal price levels across this entire historical range. Percentages are each outcome's share of grouped touches. Only validated five-minute OHLC candles were analyzed; no historical prices were fabricated."
    }
