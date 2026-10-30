# NIFTY LAB v4 — Premium UI redesign

## What changed

- Reworked the outer React terminal shell styling to use a coordinated midnight-navy, indigo, and teal palette, with matching light and dark themes.
- Reworked the embedded workspace styling so Overview, Data Session, Contracts, BEP Analysis, Historical S&R, and Formula Guide share one visual language with a restrained accent color per section.
- Added layered card surfaces, polished table headers and rows, raised controls, subtle depth, 3D entry/hover animations, animated historical result bars, and richer chart framing.
- Added a dedicated searchable BEP strike-selection dialog. Users can check individual strikes, search values such as `22100`, select/deselect visible strikes, clear picks, apply the selection, or restore the default strike range.
- BEP dialog options are populated from `available_strikes_in_file` in the selected data response. Selecting strikes uses the existing `all_available_rows` response for those actual contracts. The popup does not fabricate missing strike values.
- Added reduced-motion CSS handling and mobile layouts for the popup and tables.

## Calculation and data integrity

No Python calculation code, API route, data source selection, HLC ATM selection, BEP formula, S1/R1/S2/R2 formula, historical event classification, or export calculation was changed as part of this redesign. The BEP table continues to use:

`BEP = (CE Close + PE Close) / 2`

The BEP strike picker changes only which existing rows are displayed. Clearing the selection restores the original configured strike range. The ATM BEP summary is still taken from the available source row, even when a custom BEP filter excludes the ATM strike.

## Verification

- `python -m pytest -q`: **26 passed**.
- Inline application JavaScript: `node --check` passed.
- Static HTML parsed successfully.
- CSS parsing checks found no stylesheet/declaration parse errors in the static workspace or React shell.
- A production Vite build could not be confirmed in this environment because `npm install --no-audit --no-fund` timed out while trying to retrieve dependencies. Run `BUILD_FRONTEND.bat` on the target PC after npm dependencies can be installed. No successful production build is claimed here.

## Suggested test flow on Windows

1. Extract the ZIP to a fresh folder.
2. Run `RUN_LOCAL.bat` or the existing setup steps.
3. Open the dashboard and fetch a valid historical date/expiry.
4. Open **BEP Analysis → Choose BEP strikes**; search/select one strike (for example `22100` if the selected source contains it), press **Apply selection**, and check that only the chosen row(s) appear.
5. Press **Default range** and verify the configured BEP table returns.
6. Review existing Overview S/R calculations, exports, and Historical S&R workflows to confirm they remain unchanged.
