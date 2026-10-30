# NIFTY LAB v3 — UI polish and strike picker

## Changes
- Introduced distinct tab accents: indigo (Data Session), teal (Overview), blue (Contracts), amber (BEP Analysis), rose (Historical S&R), green (Formula Guide).
- Added subtle layered/3D surface shadows, hover lift, and more readable row spacing to existing tables and panels.
- Added a multi-select strike picker populated from actual available strikes for the selected trading date and expiry. It can show strikes outside the initially configured ITM/OTM window when the source contains them; Ctrl-click selects multiple strikes. Default range restores the original selected-range table.
- Added a restrained perspective frame around the existing historical S&R chart. Perspective is a visual treatment only and does not alter the source data or analytical results.
- Backend response adds `all_available_rows`; original `rows`, calculations, selection formula, exports, and historical analysis code paths remain intact.

## Important
The picker cannot display contracts absent from the selected date/expiry source. It does not fabricate a strike. Existing selected-range rows and calculations are unchanged.
