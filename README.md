# YottaFlex Attribution Intelligence Platform

Explainable marketing attribution and ROI analysis for the YottaFlex 72-hour code challenge.

## Stack
- Java 21, Spring Boot 3.5, Spring Data JPA, Jakarta Validation
- PostgreSQL 16+ with Flyway migrations
- React 18, TypeScript, Vite, Recharts
- Python 3.13 standard-library synthetic data tools (seed 20261022)

## Features
- Campaign/channel/creative/audience/contact master data and relational event entities.
- Contact-level touchpoints, sessions, leads, opportunities, orders and campaign costs.
- First-touch, last-touch and linear attribution over a 180-day pre-conversion lookback.
- Data-quality exception path and separate ground_truth labels for five required anomaly types.
- CSV exports, row-count manifests, SHA-256 checksums and deterministic regeneration.
- Dashboard for revenue, spend, campaigns, attribution model selection, ROI and quality issues.

## Generate and validate datasets (no database required)
```powershell
python tools/generate_synthetic.py
python tools/validate_dataset.py
python tools/reproducibility_check.py
```
Full brief-scale data:
```powershell
python tools/generate_synthetic.py --full
python tools/validate_dataset.py
python tools/reproducibility_check.py --full
```
Outputs are written to `data/generated/` by default. The full generator creates 500 campaigns, 50,000 contacts, 200,000 touchpoints and 10,000 opportunities, plus labelled anomalies. Python uses only its standard library.

## Run the application
Requirements: Java 21, Maven 3.9+, Node 20+, PostgreSQL.

1. Create a local database: `CREATE DATABASE yottaflex_attribution;`
2. Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` (see `backend/.env.example`). Do not commit secrets.
3. Backend:
   ```powershell
   cd backend
   mvn spring-boot:run
   ```
4. Frontend:
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
- FIRST_TOUCH credits the earliest eligible event.
- LAST_TOUCH credits the latest eligible event at or before conversion.
- LINEAR equally distributes credit across eligible events.
- Eligible touchpoints belong to the converting contact and fall in the 180 days before opportunity close. Post-conversion events do not receive credit.
- Credit totals are normalized to 1.000000 per opportunity/model when at least one eligible touch exists.

## Edge cases
The synthetic CSV generator includes labelled examples for duplicate revenue, touch after conversion, spend currency mismatch, omitted campaign costs and unverified anonymous identity association. The labels reside in `ground_truth.csv` and are not input features. Cross-currency ROI is not calculated without an explicit FX policy.

## Design documentation
- `docs/DATA_DICTIONARY.md`
- `docs/ARCHITECTURE.md`
- `docs/VALIDATION_PLAN.md`
- `docs/SOURCE_MANIFEST.md`
- `prompts/AI_PROMPT_HISTORY.md`
- `prompts/ASSUMPTIONS.md`

## Verification completed on 2026-11-06
- Backend: `mvn -DskipTests compile` passed.
- Frontend: `npm run build` passed (Vite warns that the Recharts application chunk exceeds 500 kB).
- Starter CSV dataset validated with zero validator errors.
- Full dataset validated: 500 campaigns, 50,000 contacts, 200,000 baseline touchpoints (+1 labelled post-conversion anomaly), 10,000 opportunities.
- Starter fixed-seed reproducibility check passed (row counts and file checksums identical).
- Full-scale export and validation passed. Full-scale double-generation checksum check not yet completed.
- PostgreSQL application startup/API verification remains pending because the local PostgreSQL credentials have not been configured for this project.

## AI disclosure
The FAQ requires all prompts submitted to AI tools to be included. See `prompts/AI_PROMPT_HISTORY.md`. AI responses are optional. The candidate must personally review and understand the complete submission, verify behavior, and add every additional prompt used to that log.

## Honest limitations
- Full CSV generation is verified, but full-scale insertion into PostgreSQL and live API integration remain to be measured.
- Backend generation currently creates related records in JPA; richer transactional bulk import from the CSV exports is a follow-up.
- Chronological split metadata is documented; strict per-row split assignment should be completed before using model evaluation metrics.
- Keep the repository public at submission, but never publish database credentials, private data or secrets.
