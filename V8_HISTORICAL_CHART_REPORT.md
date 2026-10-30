# NIFTY LAB v8 — Expiry hint and adjustable Historical S&R chart

## Changes
- Hid the verbose expiry-discovery diagnostic below the expiry dropdown. Expiry auto-detection and selection remain unchanged.
- Improved the Historical S&R “NIFTY 50 spot chart with Market Levels” description.
- Added a chart-window slider to adjust how many recent candles are visible for the selected trading date.
- Changed the chart from a close-only trace to genuine OHLC candlesticks (green up candles, red down candles), retaining the S1/S2/R1/R2 horizontal levels.
- Kept the trading formulas and backend logic unchanged.

## Use
1. Fetch Data Session and confirm Market Levels are available.
2. Open Historical S&R.
3. Upload a genuine five-minute NIFTY 50 spot OHLC CSV with timestamp, open, high, low and close columns.
4. Choose a trading date and use the Chart window slider to zoom in/out around the fixed S1/S2/R1/R2 levels.
5. For multi-day reaction statistics, use the existing Analyze Market Levels flow. A valid historical candle archive or CSV is required; no candles or price values are fabricated.

## Validation
- Backend regression tests: 26 passed.
- Inline JavaScript syntax check: passed.
- Frontend production build: not run in this environment.
