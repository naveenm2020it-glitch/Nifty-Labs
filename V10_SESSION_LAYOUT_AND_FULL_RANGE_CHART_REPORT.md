# NIFTY LAB v10 — Data Session Layout and Full-Range Candle Chart

## Changes made

- Reorganized Data Session into three clearly separated groups: **Trading session**, **Strike range**, and **Optional data overrides**.
- Kept the existing form names, calculation inputs, fetch flow, HLC ATM rule, and support/resistance calculation logic unchanged.
- Moved the optional manual NIFTY close and optional NSE CSV/ZIP upload into their own section.
- Added responsive layouts for desktop, tablet, and mobile widths.
- Changed Historical S&R spot chart rendering so it plots **all candles returned for the selected From/To date range**, instead of filtering the chart to only one selected trading day.
- The visible-candle slider now defaults to the full returned range and can be reduced to zoom into the latest portion of that range.
- Added date-and-time labels for the range chart. The date selector remains available for choosing daily NSE-derived levels when no current Market Levels are available; it no longer filters candles from the chart.
- Kept the chart data source honest: the API returns only validated, genuine five-minute candles from the configured source. Missing candles are not fabricated.

## Validation

- `PYTHONPATH=. python -m pytest -q`: **26 passed**.
- Extracted inline JavaScript passed `node --check` syntax checks.
- Frontend production build was **not run** because `frontend/node_modules` is not installed in this environment.
- No live-browser end-to-end test was performed. Actual candle availability still depends on the configured historical spot-data source covering the selected dates.

## Files changed

- `static/index.html` — Data Session layout, responsive styling, and full-range spot chart behavior.

No backend calculation formulas were changed.
