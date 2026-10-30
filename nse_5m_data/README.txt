NIFTY 50 FIVE-MINUTE SPOT DATA ARCHIVE

This folder is scanned automatically when you click Analyze Overview levels.

Put genuine five-minute NIFTY 50 spot OHLC CSV exports from an authorized NSE intraday data service here. You can use one large CSV, several CSV files, or ZIP archives that contain CSV files. The analysis date filter is applied automatically; you do not need to upload the same file each run.

Required fields: timestamp, open, high, low, close. Separate Date and Time columns are also recognized. Timestamps without a zone are treated as IST. Data is checked for valid OHLC and a five-minute median interval; daily bars are rejected.

IMPORTANT: NSE public historical index reports and F&O bhavcopies are daily/EOD data, not a free, verified two-year five-minute spot candle API. This folder is not pre-populated with fabricated data. Obtain authorized genuine intraday history from NSE Data & Analytics or its authorized source. See README.md for official NSE data links.

Optional: set NSE_5M_CSV_DIR in .env to point to another folder. No Kite credentials are required.

FREE AUTOMATIC FIVE-MINUTE SOURCE
---------------------------------
If nse_5m_data contains no local CSV/ZIP files, the analyzer attempts to fetch public NIFTY 1-minute OHLC CSV files from https://github.com/technovusin/nifty50-historical-data and resamples them to five-minute bars. Files are cached in data_cache/public_nifty_1min so repeat analyses do not download them again. Internet access is required on the first fetch.

This is a community-maintained dataset, not an official NSE feed. The repository documents missing bars and other known data quirks. The app shows the source in results; verify data quality before relying on trading conclusions. It never fills missing candles with fabricated prices. If GitHub is unavailable, a clear fetch error is shown.
