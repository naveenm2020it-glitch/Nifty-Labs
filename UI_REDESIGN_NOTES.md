# NIFTY LAB — Premium terminal UI redesign notes

## New shell

- React + TypeScript application built with Vite.
- Tailwind CSS design tokens for spacing, typography, surface/line contrast, and light/dark modes.
- Local shadcn/ui-style Button and accessible Radix Tooltip primitives.
- Lucide React navigation and action icons.
- Sidebar collapses to an icon rail on desktop; tooltips are shown in collapsed mode.
- On mobile, navigation opens as an overlay drawer, closes on outside tap or Escape, and closes after selecting a page.
- Theme and desktop-collapse preference are persisted through `localStorage` when available.
- API status is read from `/api/health`; it is not a market-data connection indicator.

## Original-workspace compatibility

The existing frontend is a large direct-DOM application with many independent workflows and tightly coupled event handlers. Instead of rewriting all of that in one pass and risking changed trading behaviour, the React terminal hosts the original workspace through the same-origin `/workspace` route. The new shell controls page navigation by activating the original page navigation buttons. The original frontend is not used to calculate a second set of values.

Only outer legacy navigation/title chrome is hidden in the embedded view. Original forms, selectors, result panels, tables, source/status messages, CSV/Excel exports, browser-print workflows and historical-analysis controls remain present.

## Colour direction

- **Primary shell:** midnight ink/navy sidebar, silver-grey workspace, and a focused electric-blue active state.
- **Supporting semantics:** muted neutral surfaces; restrained green, red, and amber are reserved for status/meaningful data states.
- **Dark mode:** deep ink panels with cool-grey dividers and the same blue accent.
- The shell uses CSS variables. Existing feature screens keep their existing theme variables, which are synchronized by the shell.

## Financial charting

The existing canvas chart remains the fallback. When the feature screen exposes genuine, time-stamped OHLC candle data, complete matching S1/S2/R1/R2 levels, unique timestamps and structurally valid OHLC values, `legacy-chart-bridge.ts` renders candlesticks and four level overlays with TradingView Lightweight Charts. Invalid, duplicate, date-only or incomplete data are not repaired or fabricated for display; the original chart remains visible instead.

Lightweight Charts is a rendering library and does not fetch market data or connect to brokers.

## Protected backend

- Existing FastAPI endpoint names and request/response behaviour are retained.
- `app/data.py`, `app/auto_history.py`, and `app/historical_sr.py` are unchanged.
- Existing formulas and source integrations remain backend-owned.
- Existing CSV/ZIP fallback, result exports, print workflows and analysis pages remain in the original feature workspace.

## Build and validation

Run `BUILD_FRONTEND.bat` on Windows or `cd frontend && npm install && npm run build`. `RUN_LOCAL.bat` builds the frontend automatically when Node.js/npm are present. The Docker image performs a multi-stage frontend build.

The current execution environment could not resolve `registry.npmjs.org` during installation (`EAI_AGAIN`), so the frontend production build could not be confirmed in this environment. Backend regression tests and Python route checks should be run independently; do not treat the frontend as build-verified until `npm run build` completes on a network-enabled machine.
