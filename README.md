# VELA — XAUUSD Volume Profile Trading Desk

VELA is a web-based trading desk focused on XAUUSD analysis with a volume-profile strategy, charting, account connectivity, and a trading terminal.

The application source lives in `app/`.

## Technology

- React 19 and TypeScript
- TanStack Start and TanStack Router
- Vite
- Tailwind CSS 4
- Exness API integration

## Requirements

- Node.js 22 or newer
- npm

## Local development

```bash
cd app
npm ci
npm run dev
```

The development server listens on `0.0.0.0:8080`.

## Validation

Run from `app/`:

```bash
npm test
npm run typecheck
npm run lint
npm run build
```

The build script also runs the database migration step. Configure the required runtime environment before running a production build.

## Configuration and security

- Do not commit API keys, private keys, account credentials, session tokens, or production environment files.
- Keep local secrets in ignored `.env` files or the deployment platform's secret manager.
- Never paste Exness private keys into source files, issues, logs, or chat messages.
- Verify account permissions, instrument availability, order parameters, risk limits, and execution safeguards before enabling any live trading capability.
- Use a demo account first. This repository update does not authorize live orders or deployment.

## Repository layout

- `app/src/` — application routes, UI, authentication, data access, and volume-profile logic
- `app/scripts/` — project utilities and automated tests
- `app/migrations/` — database migrations
- `app/public/` — static assets
- `app/.grok/` — project-specific build and validation guidance

