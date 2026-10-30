# NIFTY LAB v6 — UI polish

## Requested changes
- Data Session is the first navigation tab; the former Overview is second and renamed **Market Snapshot**.
- Fetch Data and Clear Results now stay in normal document flow instead of using sticky positioning, so they no longer float over/track the scrolling form.
- Data Session fields use a consistent three-column desktop grid, two columns at medium widths, and a single column on mobile.
- Support S1/S2 values are green; Resistance R1/R2 values are red in Market Snapshot.
- Put/PE columns are red in the contract matrix and PE Close is red in BEP table.
- Tables have clearer row and column separators, stronger header dividers, and hover row emphasis.

## Calculation integrity
No Python/backend calculation logic or formulas were changed. Existing fetch, expiry, HLC ATM, BEP, export and historical analysis handlers are preserved.
