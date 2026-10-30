# NIFTY LAB v9 — Data Session cleanup and Historical S&R spot chart

## Requested changes

- Hid the expiry discovery technical message in Data Session, including detected-expiry count, NSE archive filename, HTTP status, and content type. Expiry auto-detection still updates the exact expiry date field; its technical hint is no longer displayed.
- Added From date and To date controls to the last tab, Historical S&R.
- Added a **Load spot chart** action that requests genuine five-minute NIFTY 50 OHLC candles for the selected range from the configured spot archive / existing automatic source path.
- Added a trading-day selector so the user can select a single session from the loaded range. The existing visible-candle slider controls zoom/candle count.
- Reuses the already fetched Data Session S1/S2/R1/R2 values as horizontal chart levels when available. No candle prices are generated.
- Chart API validates dates and candle OHLC using the existing `load_candles` validation path and reports an error if genuine data is unavailable.

## Validation

- `python -m pytest -q`: 26 passed.
- Inline JavaScript syntax: passed (2 inline scripts).
- HTML parsed and required chart controls verified.
- Frontend production build not run/verified.

## Important data note

The spot chart requires genuine five-minute NIFTY 50 OHLC data in the configured `nse_5m_data` / `NSE_5M_CSV_DIR` archive or the project's existing permitted automatic source. Public NSE options bhavcopy alone does not contain five-minute spot candles. If the requested range is unavailable, the chart shows an error rather than fabricating prices.
