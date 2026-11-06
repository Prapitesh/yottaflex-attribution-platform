# YottaFlex Attribution Intelligence Platform

An explainable marketing attribution and ROI analytics application built for the YottaFlex 72-hour code challenge.

## Stack
- Java 21, Spring Boot 3.5, Spring Data JPA, Bean Validation
- PostgreSQL, Flyway migrations
- React, TypeScript, Vite, Recharts
- Deterministic synthetic data generator (seed: 20261022)

## What it does
1. Creates campaign, channel, creative, audience and contact master data.
2. Generates touchpoints, sessions, leads, opportunities, orders and cost records with deterministic IDs and valid references.
3. Computes first-touch, last-touch and linear multi-touch attribution from observed journeys.
4. Applies explicit anomaly detection for duplicate revenue, post-conversion touches, currency mismatch, missing campaign costs and unsupported anonymous identity links.
5. Reports campaign spend, attributed revenue, ROI and data-quality exceptions.
6. Preserves attribution results and exception findings as auditable records.

## Quick start
Requirements: Java 21, Maven 3.9+, Node 20+, PostgreSQL 16+.

1. Create a local PostgreSQL database:
   ```sql
   CREATE DATABASE yottaflex_attribution;
   ```
2. Copy `backend/.env.example` to `backend/.env` or set environment variables:
   - `DB_URL=jdbc:postgresql://localhost:5432/yottaflex_attribution`
   - `DB_USERNAME=postgres`
   - `DB_PASSWORD=your-local-password`
3. Start backend:
   ```powershell
   cd backend
   mvn spring-boot:run
   ```
4. Generate a starter dataset:
   ```powershell
   Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/admin/generate -ContentType 'application/json' -Body '{"seed":20261022,"scale":"STARTER"}'
   ```
5. Start frontend:
   ```powershell
   cd frontend
   npm install
   npm run dev
   ```
6. Open http://localhost:5173.

## API
- `GET /api/v1/dashboard/summary`
- `GET /api/v1/campaigns`
- `GET /api/v1/attribution/results?model=FIRST_TOUCH|LAST_TOUCH|LINEAR`
- `GET /api/v1/quality/issues`
- `POST /api/v1/admin/generate`
- `POST /api/v1/admin/recalculate`

## Data volumes
Starter scale is intended for fast local verification. Full scale uses the brief's target: 500 campaigns, 50,000 contacts, 200,000 touchpoints and 10,000 opportunities. Full-scale run time and memory depend on the local machine.

## Attribution definitions
- First touch: 100% credit to the earliest eligible touchpoint in the configured lookback window.
- Last touch: 100% credit to the latest eligible touchpoint at or before conversion.
- Linear: equal credit across all eligible touchpoints in the journey.
- Post-conversion touches are excluded and flagged.
- Attribution credit for an opportunity must sum to 1.0000 (within rounding tolerance).

## Ground truth and anomaly safety
Synthetic anomalies are labelled in `ground_truth` and exception records. Anomalies do not silently corrupt unrelated entities. Ground-truth labels are evaluation-only and are not used as attribution input features.

## Reproducibility
The generator uses a fixed seed and deterministic ID counters. Generate twice with identical parameters and compare the manifest checksums and row counts. Full-scale generation is configurable.

## AI use disclosure
See `prompts/AI_PROMPT_HISTORY.md`. All prompts submitted to AI tools for this project should be recorded there. AI response text is optional according to the supplied FAQ. Review and understand the implementation before submitting.

## Limitations / assumptions
- Synthetic-only data; no personal/customer information or external licensed dataset is used.
- Currency conversion is deliberately not inferred. Spend in a different currency is flagged and excluded from cross-currency ROI until reconciled.
- This is a decision-support prototype, not a production marketing automation or financial ledger.
- Update credentials through local environment variables; never commit secrets.

## Submission checklist
- [ ] Run backend and frontend from a clean checkout.
- [ ] Verify core APIs and anomaly paths.
- [ ] Generate starter and full datasets; capture row counts and checksums.
- [ ] Review all code and AI prompt history.
- [ ] Push to a dedicated **public** GitHub repository.
- [ ] Open the repository in a private browser window.
- [ ] Submit the public repository URL through the YottaFlex portal before the deadline.
