# YottaFlex Attribution Intelligence Platform

Java full-stack application for explainable marketing attribution and ROI analytics, built for the YottaFlex AI Forward Deployed Engineer 72-hour code challenge.

## Stack
- Java 21, Spring Boot 3.5, Spring Data JPA, Jakarta Validation
- PostgreSQL with Flyway migrations
- React 18, TypeScript, Vite, Recharts
- Python standard-library synthetic data generator (seed 20261022)

## Capabilities
- Campaign, channel, creative, audience and contact master data.
- Campaign-audience effective-dated bridge table.
- Touchpoint, web session, lead, opportunity, order and cost records with linked synthetic IDs.
- First-touch, last-touch and linear attribution using an explicit 180-day pre-conversion window.
- Data quality paths for duplicate revenue, post-conversion touchpoints, currency mismatch, omitted campaign costs and unverified identity links.
- Append-only entity event history with validated campaign, lead, opportunity and dataset-run lifecycle transitions; ground-truth labels, CSV exports and SHA-256 manifests.
- Database-backed dataset-run registry: every generation creates a traceable snapshot and never deletes previous runs.
- Run-scoped attribution, dashboard and data-quality queries; lifecycle events carry the dataset run ID.
- Structured API error responses and Java unit tests.

## Generate, validate and reproduce CSV datasets (no database required)
Starter:
```powershell
python tools/generate_synthetic.py
python tools/validate_dataset.py
python tools/reproducibility_check.py
python -m unittest discover -s tools\tests -v
```
Full scale:
```powershell
python tools/generate_synthetic.py --full
python tools/validate_dataset.py --data data\generated
python tools/reproducibility_check.py --full
```
Generated files are in `data/generated/` by default. The full scale uses 500 campaigns, 50,000 contacts, 200,000 baseline touchpoints and 10,000 opportunities. A separate labelled post-conversion touchpoint, duplicate order, missing campaign cost, currency mismatch and unverified identity link are included as exception examples. Attribution results and evaluation split labels are derived from the source records. The Python tooling uses only the standard library. Each generated dataset includes a SHA256SUMS.txt covering its exports and accompanying evidence files.

## Run the application
Requirements: Java 21, Maven 3.9+, Node 20+, PostgreSQL 16+.

1. Create a PostgreSQL database: `CREATE DATABASE yottaflex_attribution;`
2. Configure environment values `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` (see `backend/.env.example`). Do not commit secrets.
3. Start backend:
   ```powershell
   cd backend
   mvn spring-boot:run
   ```
4. Start frontend in another terminal:
   ```powershell
   cd frontend
   npm install
   npm run dev
   ```
5. Open http://localhost:5173.

## API
- `GET /api/v1/dataset-runs` — list synthetic dataset snapshots.
- `GET /api/v1/dashboard/summary?runId={id}` — run-specific KPIs (omit runId for all runs).
- `GET /api/v1/campaigns?model=LINEAR&runId={id}`
- `GET /api/v1/attribution/results?model=LINEAR&runId={id}`
- `GET /api/v1/quality/issues`
- `POST /api/v1/admin/generate` body: `{"seed":20261022,"scale":"STARTER"}`
- `POST /api/v1/admin/recalculate` — all runs.
- `POST /api/v1/admin/recalculate/{runId}` — one run.
- `POST /api/v1/admin/validate?runId={id}` — one run.

## Full-scale database verification
The Java/PostgreSQL generator has been executed at the requested full scale using seed 20261022: 500 campaigns, 50,000 contacts, 200,001 touchpoints, 10,000 opportunities, 10,001 orders and 499 cost records. Java attribution created 129,610 rows across all three models; 30,000 opportunity/model groups reconcile to 1.000000. Quality validation detected all five required anomaly classes. The export package in data/generated-full/ contains CSV/JSON files, Java run evidence, SHA-256 checksums and a compressed PostgreSQL SQL backup (yottaflex_attribution_full.sql.tar.gz). The SQL backup includes the schema and all local append-only runs.

## Attribution definitions
- FIRST_TOUCH: full credit to earliest eligible interaction.
- LAST_TOUCH: full credit to latest eligible interaction.
- LINEAR: equal credit across eligible interactions, with rounding remainder assigned to the final interaction.
- Eligible touchpoints belong to the converting contact and occur during the 180 days before opportunity close. Touches after conversion receive no credit.
- For any attributed opportunity/model group, credits sum to 1.000000.
- Each attribution calculation creates new history records rather than overwriting previous calculation output.

## Data quality policy
- Duplicate revenue keys are retained in source events for audit, but revenue totals count one unique external revenue key once.
- Post-conversion touches are excluded.
- Campaign ROI is quarantined if any cost row has a currency different from the campaign currency; no FX rate is assumed.
- Missing campaign cost means ROI unavailable, not 0%.
- An identity association without evidence is flagged and rejected.
- Quality findings and ground-truth labels are appended per inspection run. The export validator checks that anomaly labels point to real records and that each record actually exhibits the labelled exception.
- `ground_truth.csv` is evaluation-only and never used as an input feature.

## Design and disclosure
- `docs/DATA_DICTIONARY.md`
- `docs/ARCHITECTURE.md`
- `docs/VALIDATION_PLAN.md`
- `docs/SOURCE_MANIFEST.md`
- `prompts/AI_PROMPT_HISTORY.md`
- `prompts/ASSUMPTIONS.md`
- `.github/workflows/ci.yml`

## Verification recorded 2026-11-06
- Backend: Maven clean verify passed; 13 tests, zero failures. Java 21 compilation and executable JAR packaging succeeded.
- Frontend: clean npm ci installed 138 packages with zero reported vulnerabilities; npm run build passes without chunk-size warnings. Output is split into a 73.50 kB application chunk, 457.10 kB Recharts chunk, 74.94 kB D3 chunk and 14.84 kB icon chunk (all minified chunks below 500 kB).
- PostgreSQL 18.6 at `localhost:5432`: authenticated connection succeeded; Flyway validated and applied migrations V1–V3; Hibernate initialized with `ddl-auto: validate`.
- Live Spring Boot API: dashboard summary, dataset-runs, campaigns, attribution results and quality issues endpoints returned HTTP 200. CORS allows `http://localhost:5173`.
- Frontend dev server: `http://localhost:5173` returned HTTP 200; Vite served `main.tsx`, `App.tsx`, `DashboardPage.tsx` and `services/api.ts` successfully.
- Java starter run: generated 12 campaigns, 300 contacts, 1,201 touchpoints including the labelled post-conversion example, and 80 opportunities. Attribution calculation produced 846 results; quality inspection identified all five anomaly classes.
- Python: five unit tests pass. Starter and full datasets validate with zero errors, including FK integrity, all five anomaly semantics, contact-matched attribution inside the 180-day window, normalized credit groups, manifest row counts and SHA-256 checksums. Starter and full fixed-seed regeneration both reproduce identical counts and hashes.

## Known limitations and submission notes
- A lint/format enforcement gate is not configured. The frontend has a Playwright browser interaction suite covering desktop and mobile Chromium; it is not a substitute for testing every physical browser/device.
- Backend automated coverage currently contains 13 Java unit tests. Python unit coverage includes five tests for deterministic generation, relationships, anomaly labels, attribution credits and semantic validation. GitHub Actions runs Java verification, Python starter/full generation and semantic validation, fixed-seed reproducibility, the frontend production build, and desktop/mobile Chromium tests on pushes and pull requests.
- Entity audit records append-only CREATED events and validated STATUS_CHANGED transitions for Campaign (PLANNED → ACTIVE → COMPLETED), Lead (NEW → QUALIFIED → CONVERTED), Opportunity (OPEN → WON), and DatasetRun (GENERATED → VALIDATED). Entities without lifecycle status fields do not emit status transitions; delete/restore workflows are outside this prototype.
- The synthetic opportunity generator currently emphasizes closed-won journeys for the ROI walkthrough. Lost/open stage distributions and broader attribution evaluation metrics can be added in a later iteration.
- The project is a local decision-support prototype, not a production marketing platform. Review and understand every implementation decision before discussing it in an interview.

## Browser interaction tests
Install Chromium once with `npx playwright install chromium`, then run `npm run test:e2e` from `frontend/`. The suite mocks API responses for deterministic UI interaction coverage. To also run the real browser-to-Spring Boot/PostgreSQL integration test, start the backend and database, then set `YOTTAFLEX_LIVE_API=1` before `npm run test:e2e` in PowerShell.

## Submission
Public source repository: https://github.com/Prapitesh/yottaflex-attribution-platform

The repository includes source, documentation, AI prompt history, generated full-scale CSV/JSON evidence, checksums, and a compressed PostgreSQL SQL backup. Do not publish credentials, private customer data or personal information. The candidate should independently verify the public repository and submit its URL in the YottaFlex candidate portal.


## Frontend-to-database workflow verification (2026-11-06)
- Verified the Vite app root, React entry, dashboard module and Axios API module all return HTTP 200 while the local Spring Boot API and PostgreSQL 18.6 database are running.
- Verified CORS returns `Access-Control-Allow-Origin: http://localhost:5173` for the frontend origin.
- Exercised every dashboard read route against the full-scale dataset run: dataset runs, dashboard summary, campaigns for FIRST_TOUCH/LAST_TOUCH/LINEAR, attribution summaries for all three models, and run-scoped quality issues. All returned HTTP 200; each campaign query returned 500 rows.
- Exercised the complete starter write workflow through the same REST routes used by the frontend: POST generate (12 campaigns, 300 contacts, 1,201 touchpoints, 80 opportunities), POST recalculate (846 attribution rows across three models), POST validate (five required anomaly classes), then GET dashboard/campaign/quality data to confirm persisted results.
- Starter end-to-end run ID: `b4be4c6a-7808-43cb-88ca-febe04d3ea0e`.
- Re-ran full-scale attribution calculation for run `bb216bac-f11d-479f-be10-6c9c05e5de3e`: 10,000 opportunities evaluated, 129,610 result rows created, zero opportunities without eligible touches. Validation returned exactly one finding for each required anomaly class.
- Fixed a frontend timeout mismatch: full-scale generation previously exceeded the shared 60-second Axios timeout. The FULL generation request now allows 180 seconds, based on the measured ~115-second backend generation. After generation, the dashboard selects the newly created run so the visible metrics are scoped to it.
- Rebuilt after code splitting: TypeScript compilation and Vite production build pass with no chunk-size warning; largest chunk is 457.10 kB minified.
- Browser verification: Playwright executed 16 mocked-API interaction tests pass across desktop and mobile Chromium. Two optional live browser-to-Spring Boot/PostgreSQL integration tests are included and previously passed in the local environment. Coverage includes initial rendering, model/run filters, sidebar navigation, refresh, attribution/recalculation, validation, starter/full generation controls, API error feedback, live persisted data and mobile horizontal overflow.
