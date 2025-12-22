# Main App (Umi)

This package hosts the unified frontend app. The legacy `frontend` codebase has been merged under `src/legacy` and mounted under the `/admin` route prefix.

## Structure
- `src/legacy/` — migrated pages, components, services, utils, styles from the original `frontend` project.
- `src/pages` — native Umi pages for Studio.
- `tailwind.css` and `tailwind.config.js` — Tailwind setup (legacy content globs included).

## Routes
- `/admin` — legacy landing page (placeholder).
- `/admin/playground` — legacy Playground.
- `/admin/prompts` — legacy Prompts.
- `/admin/tracing` — legacy Tracing.
- `/admin/evaluation/experiment` — legacy Experiment list.
- `/admin/evaluation/gather` — legacy Gather list.
- `/admin/evaluation/evaluator` — legacy Evaluator list.

## Development
```bash
cd frontend_studio
npm install
npm run dev -w packages/main
```

App will be served at http://localhost:8000 (or the port chosen by Umi).

## Backend Proxy
API requests to `/api/*` are proxied via Umi to `process.env.WEB_SERVER` or `http://localhost:8080` by default (see `.umirc.ts`).

## Notes
- TypeScript is configured with `allowJs` to load JS/JSX from legacy.
- Global styles import includes `src/legacy/styles/tailwind.css` and `src/legacy/styles/index.css` in `src/app.tsx`.
