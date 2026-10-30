# NIFTY LAB v11 — Full-range candles and fixed-height reaction log

## Implemented

- The Historical S&R spot chart continues to request candles for the selected **From date** and **To date** range and plots the full returned range by default. The visible-candle slider can narrow the visible window for inspection.
- The **Dates and times each level reacted** event table now has a fixed-height, 430px scroll area (390px on small screens). Its header stays visible while scrolling, and all rows returned by the analysis remain inside the same table instead of extending the page vertically.
- The table retains all matching interactions and includes event date, event time, level, level price, reaction, approach side, OHLC, confirmation time, points and explanation. Existing level/reaction filters still work; filters narrow the rows without changing the table height.
- Set column widths and horizontal scrolling so the 13-column table remains readable without expanding the page. No event rows are intentionally paginated or truncated by the UI.
- No changes to trading formulas or event classification logic.

## Validation

- Backend tests: **26 passed**.
- Inline JavaScript syntax: `node --check` passed for both inline scripts.
- Production frontend build and live-browser end-to-end validation were not run in this environment.
- The chart can plot all genuine candles returned by the selected-range data source. It cannot display candles the configured archive/provider does not contain; missing candles are not fabricated.
