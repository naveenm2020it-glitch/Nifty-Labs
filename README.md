# NIFTY LAB Premium Terminal v5

This release refines the navigation and UI while preserving the original trading calculation engine.

- Full-screen ribbon/wordmark intro animation, with Skip Intro and reduced-motion support.
- Single-screen horizontal top tabs for manual page switching.
- Cleaner Data Session form layout across desktop, tablet, and mobile.
- Removed the inline strike multi-select from the contract matrix; search and exports remain.
- Kept the separate BEP strike selection popup.

See `V5_UI_AND_NAVIGATION_REPORT.md` for implementation and validation details.

## Start

On Windows, run `RUN_LOCAL.bat`. The application uses the existing FastAPI backend and original formulas. For the React shell, install dependencies in `frontend/` and run `npm run build`; frontend build was not verified in the packaging environment.
