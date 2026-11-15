# Apple-inspired NIFTY LAB UI refresh

Branch: `apple-premium-ui-redesign`

## Scope
This change is a visual-layer refresh only. It keeps the React navigation shell, FastAPI backend, original legacy workspace, existing DOM IDs, JavaScript event handlers, API routes, calculations, chart code, and export flows in place.

## Visual updates
- Replaced the existing violet-heavy visual tokens with a restrained neutral / olive palette.
- Added coordinated light and dark theme tokens.
- Refined the React terminal shell, tabs, header, status, navigation, cards, and mobile spacing.
- Refined the legacy workspace through CSS overrides rather than replacing feature markup or JavaScript.
- Improved touch feedback, safe-area spacing, focus states, and mobile horizontal navigation.
- Added reduced-motion handling and avoided animating market values or chart data.

## Protected behavior
No Python calculation or data-source modules were modified. No API route, data model, formula, historical analysis, export, or Render configuration was changed.

## Validation
A GitHub Actions workflow now runs the frontend TypeScript/Vite build and Python regression tests on this branch. The workflow has not yet completed in this commit, so build/test success is not claimed here.

## Android packaging status
The repository does not currently contain a Capacitor/Android project or an APK. Android packaging is a separate remaining step. A working APK must be configured against the intended deployed API origin and then built and installed for device verification; no Render URL has been hard-coded and the Render deployment configuration remains unchanged.
