# NIFTY LAB v5 — Navigation, Data Session Layout & Motion Redesign

## User-requested changes

- Added a cinematic full-screen ribbon animation inspired by the supplied reference video: layered colorful ribbons, an animated NIFTY LAB mark/wordmark reveal, and a Skip Intro control. The animation runs once per browser session and is disabled for users who prefer reduced motion.
- Changed the React terminal from a persistent left sidebar to a single-screen workspace with horizontal top navigation tabs for Overview, Data session, Contracts, BEP analysis, Historical S&R, and Formula guide. Users can manually switch screens at any time; keyboard left/right navigation is supported in the standalone legacy view.
- Reworked the Data Session form into a more balanced three-column desktop layout, two columns on medium screens, and one column on mobile. Related inputs have consistent spacing and the fetch/clear actions remain accessible.
- Removed the inline native multi-select strike picker from the Contract Matrix table. Search and CSV/Excel exports remain. The separate searchable BEP strike-selection popup is retained from the previous version, with explicit Apply and Default Range controls.
- Added tab transitions, subtle layered shadows, ribbon movement, and 3D-style logo reveal while respecting reduced-motion preferences.

## Calculation and data integrity

No backend calculation functions or formulas were intentionally changed in this UI-only iteration. BEP, HLC ATM, S1/R1/S2/R2, historical S&R, source validation, and export processing continue through the existing application engine. The contract matrix defaults to the original selected strike range; its search filters displayed rows without changing calculation inputs.

## Validation

- Backend regression tests: **26 passed**.
- Inline JavaScript syntax checks: **passed**.
- Frontend production build: **not verified**. Installing frontend dependencies timed out in this environment, so the React/Vite production bundle could not be generated here. Build locally from `frontend/` with `npm install` and `npm run build` if using the React shell. Without a built frontend bundle, FastAPI continues to use the standalone legacy workspace at `/`.

## Launch

Extract this ZIP into a new folder, stop the old NIFTY LAB process, and launch `RUN_LOCAL.bat` from this version. The standalone workspace has the updated top-tab navigation and intro animation. If you build the React frontend locally, the top-tab shell will be used automatically.
