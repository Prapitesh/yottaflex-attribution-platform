# YottaFlex Attribution Intelligence Platform

Java full-stack application for explainable marketing attribution and ROI analytics, built for the YottaFlex 72-hour code challenge.

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
- Immutable entity-created event records, ground-truth labels, CSV exports and SHA-256 manifests.
- Dashboard with business KPIs, attributed campaign revenue, model selection, ROI and exceptions.

## Generate, validate and reproduce CSV datasets (no database required)
Starter:
```powershell
python tools/generate_synthetic.py
python tools/validate_dataset.py
python tools/reproducibility_check.py
```
Full scale:
```powershell
python tools/generate_synthetic.py --full
python tools/validate_dataset.py
python tools/reproducibility_check.py --full
```
Generated files are in `data/generated/` by default. The full scale uses 500 campaigns, 50,000 contacts, 200,000 baseline touchpoints and 10,000 opportunities. A separate labelled post-conversion touchpoint, duplicate order, missing campaign cost, currency mismatch and unverified identity link are included as exception examples. Attribution results and evaluation split labels are derived from the source records. The Python tooling uses only the standard library.

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
- `GET /api/v1/dashboard/summary`
- `GET /api/v1/campaigns?model=LINEAR`
- `GET /api/v1/attribution/results?model=LINEAR`
- `GET /api/v1/quality/issues`
- `POST /api/v1/admin/generate` body: `{"seed":20261022,"scale":"STARTER"}`
- `POST /api/v1/admin/recalculate`
- `POST /api/v1/admin/validate`

## Attribution definitions
- FIRST_TOUCH: full credit to earliest eligible interaction.
- LAST_TOUCH: full credit to latest eligible interaction.
- LINEAR: equal credit across eligible interactions, with rounding remainder assigned to the final interaction.
- Eligible touchpoints belong to the converting contact and occur during the 180 days before opportunity close. Touches after conversion receive no credit.
- For any attributed opportunity/model group, credits sum to 1.000000.

## Data quality policy
- Duplicate revenue keys are retained in source events for audit, but revenue totals must count one unique external revenue key once.
- Post-conversion touches are excluded.
- Campaign costs with a currency different from campaign currency are quarantined; no FX rate is assumed.
- Missing campaign cost means ROI unavailable, not 0%.
- An identity association without evidence is flagged and rejected.
- `ground_truth.csv` is evaluation-only and never used as an input feature.

## Design and disclosure
- `docs/DATA_DICTIONARY.md`
- `docs/ARCHITECTURE.md`
- `docs/VALIDATION_PLAN.md`
- `docs/SOURCE_MANIFEST.md`
- `prompts/AI_PROMPT_HISTORY.md`
- `prompts/ASSUMPTIONS.md`

## Verification recorded 2026-11-06
- `mvn -DskipTests compile`: passed.
- `npm run build`: passed; Vite warns the minified JS chunk is larger than 500 kB.
- Starter dataset: validator passed with no errors.
- Starter repeated generation: row counts and per-file checksums identical.
- Full dataset: 500 campaigns, 50,000 contacts, 200,001 touchpoints (200,000 baseline plus one labelled negative example), 10,000 opportunities, 15,120 attribution rows.
- Full dataset validator passed with no errors; 13,410 opportunity/model groups reconcile to 1.0.
- Full repeated generation: row counts and per-file checksums identical.
- PostgreSQL migration, JPA startup and REST API runtime verification are still pending because the local PostgreSQL server credentials have not yet been configured for this project.

## Honest limitations
- The Python generator/export and validators are verified at starter and full scale. The Java JPA generation endpoint is compiled but its database runtime remains to be tested.
- Full CSV-to-PostgreSQL bulk import is not yet implemented.
- Lifecycle CREATED events are recorded for Campaign, Contact, Touchpoint and Opportunity. Additional state transitions (Lead/Opportunity status changes) are not yet modelled as a workflow.
- Evaluation split labels are chronological and inherited by attribution results from opportunities; campaign leakage treatment and model quality metrics are not yet implemented.
- This is a local decision-support prototype, not a production marketing platform.

## Submission
Use a dedicated public GitHub repository. Include all source files and prompt history. Do not publish credentials, private customer data, or personal information. Test the repository link in a private browser session before submitting the URL in the YottaFlex candidate portal.
