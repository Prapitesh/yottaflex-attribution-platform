# Validation Evidence — 2026-11-06

## Verified locally

| Check | Command | Result |
|---|---|---|
| Backend compile and unit tests | `cd backend; mvn test` | PASS — 9 tests, 0 failures |
| Attribution rules | `AttributionEngineTest` | PASS — first touch, last touch, linear normalization, lookback and no-eligible-touch handling |
| Request validation | `GenerationRequestTest` | PASS — starter/full defaults and scale overrides |
| API error envelope | `ApiExceptionHandlerTest` | PASS — stable malformed request and business validation responses |
| Run event metadata | `GenerationEventRecorderTest` | PASS — creation event retains datasetRunId |
| Python starter pipeline | `python tools/generate_synthetic.py; python tools/validate_dataset.py` | PASS; zero validator errors |
| Python starter reproducibility | `python tools/reproducibility_check.py` | PASS; row counts and checksums match |
| Python full dataset | `python tools/generate_synthetic.py --full --out data/generated-full` | PASS — challenge-scale output |
| Python full validation | `python tools/validate_dataset.py --data data/generated-full` | PASS; zero validator errors |
| Python full reproducibility | `python tools/reproducibility_check.py --full` | PASS; row counts and checksums match |
| React production build | `cd frontend; npm run build` | PASS; non-fatal bundle-size warning |

## Runtime verification pending

| Check | Result |
|---|---|
| Flyway migration | BLOCKED — local PostgreSQL password authentication fails for user postgres (SQLSTATE 28P01) |
| Hibernate schema validation | PENDING — Flyway must connect first |
| Java JPA generator database integration | PENDING — requires PostgreSQL credentials |
| REST API smoke tests | PENDING — requires Spring Boot context and PostgreSQL |
| Run-separated analytics query | Compiled; SQL execution pending DB access |
| Data quality SQL against labelled anomalies | Compiled; SQL execution pending DB access |

## Acceptance rules covered by implementation/tests
- Run-scoped synthetic snapshots preserve previous generated runs.
- First-touch, last-touch and linear attribution use only eligible same-contact touchpoints in the 180 days ending at conversion.
- Post-conversion and out-of-window touches are excluded.
- Attribution calculation records are append-only.
- Revenue source rows preserve duplicate keys for audit; reporting deduplicates by external revenue key.
- Currency mismatch is quarantined; missing cost yields ROI unavailable.
- Quality issues and ground truth are appended per validation run.
- Data-quality SQL and dashboard queries can be scoped by dataset run.
- CREATED lifecycle events carry dataset-run metadata.
- Ground-truth labels are separate from attribution input features.

## Known caveats
- JPA inserts for full scale are not yet benchmarked; run the starter dataset first.
- The CSV tooling is validated at full scale; the database-backed generator is compiled but not runtime-tested.
- Entity lifecycle history records CREATED events for generated Campaign, Contact, Touchpoint, Opportunity and DatasetRun objects. All business status transitions are not yet implemented.
- The Maven environment emits a non-fatal 401 metadata warning for a configured Flyway community repository; compilation and tests still succeed via resolved dependencies.
