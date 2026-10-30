# NIFTY LAB UI refinement — v2

## Goal
A calmer, clearer, compact graphite-and-jade interface with warm neutral surfaces. This revision prioritizes readability, predictable navigation, and keeping the original feature workspace as the source of truth.

## Changes
- Replaced the blue-heavy palette with graphite/green-jade/warm-neutral design tokens for both light and dark themes.
- Reduced sidebar width, navigation row height, topbar height, and content heading height to make the main working area easier to scan.
- Reduced excessive rounding and shadows; added consistent keyboard focus indicators and reduced-motion support.
- Kept the existing six navigation destinations and their mapping to the original `data-page-target` controls.
- Kept the original feature workspace served by FastAPI at `/workspace`; the React UI remains a shell around the original screens rather than a rewritten calculation engine.
- Did not edit the protected calculation/data modules or test cases in this revision.

## Validation
- `python -m pytest -q`: **26 passed**.
- Frontend production build: **not verified in this environment** because npm dependency installation previously failed with registry DNS `EAI_AGAIN`.
- Automatic live data-source behavior was not newly certified by this visual revision.

## Important
If a particular original action still behaves differently, report the exact screen and button/input sequence. The UI redesign intentionally avoids reimplementing backend calculations; functional parity should be debugged against that exact original workflow rather than guessed.
