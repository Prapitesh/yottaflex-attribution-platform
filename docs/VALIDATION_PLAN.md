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

## Runtime verification

| Check | Result |
|---|---|
| PostgreSQL 18.6 authentication | PASS — `yottaflex_attribution` database connected |
| Flyway migrations | PASS — V1–V3 validated/applied |
| Hibernate schema validation | PASS — EntityManagerFactory initialized with `ddl-auto: validate` |
| Java starter generation | PASS — 12 campaigns, 300 contacts, 1,201 touchpoints, 80 opportunities |
| Java attribution calculation | PASS — 846 attribution result rows |
| Java data-quality inspection | PASS — all five required anomaly categories detected |
| Dashboard summary API | PASS — HTTP 200 |
| Dataset runs API | PASS — HTTP 200 |
| Campaigns API | PASS — HTTP 200 |
| Attribution results API | PASS — HTTP 200 |
| Quality issues API | PASS — HTTP 200 |
| Frontend Vite server | PASS — HTTP 200 on port 5173; source modules served |
| Frontend/backend CORS | PASS — allowed origin `http://localhost:5173` |
| Java/PostgreSQL full-scale generation | PASS — 500 campaigns, 50,000 contacts, 200,001 touchpoints, 10,000 opportunities, 10,001 orders and 499 cost records; completed in approximately 115 seconds |

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

## Full-scale runtime evidence
- Java/PostgreSQL full generation completed in approximately 115 seconds for 500 campaigns, 50,000 contacts, 200,001 touchpoints, 10,000 opportunities, 10,001 orders and 499 cost records.
- Java attribution completed in approximately 38 seconds: 129,610 result rows across FIRST_TOUCH, LAST_TOUCH and LINEAR; 10,000 opportunities evaluated; zero without eligible touches.
- SQL reconciliation returned 30,000 opportunity/model groups, all summing to exactly 1.000000; zero invalid credit groups.
- Full-scale quality inspection found exactly five issues, one for each required anomaly class.
- A PostgreSQL SQL backup was generated and packaged as data/generated-full/yottaflex_attribution_full.sql.tar.gz; it includes schema and append-only local runs.
- Entity lifecycle history records CREATED events for generated DatasetRun, Campaign, Contact, Touchpoint and Opportunity objects. All later business status transitions are not yet implemented.
- The Maven environment emits a non-fatal 401 metadata warning for a configured Flyway community repository; compilation and tests still succeed via resolved dependencies.
