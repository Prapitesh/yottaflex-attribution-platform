# Validation Evidence — 2026-11-06

## Verified locally

| Check | Command | Result |
|---|---|---|
| Backend compile and unit tests | `cd backend; mvn test` | PASS — 12 tests, 0 failures |
| Attribution rules | `AttributionEngineTest` | PASS — first touch, last touch, linear normalization, lookback and no-eligible-touch handling |
| Request validation | `GenerationRequestTest` | PASS — starter/full defaults and scale overrides |
| API error envelope | `ApiExceptionHandlerTest` | PASS — stable malformed request and business validation responses |
| Run event metadata | `GenerationEventRecorderTest` | PASS — creation and status-change events retain datasetRunId and old/new state |
| Python starter pipeline | `python tools/generate_synthetic.py; python tools/validate_dataset.py` | PASS; zero validator errors |
| Python starter reproducibility | `python tools/reproducibility_check.py` | PASS; row counts and checksums match |
| Python full dataset | `python tools/generate_synthetic.py --full --out data/generated-full` | PASS — challenge-scale output |
| Python full validation | `python tools/validate_dataset.py --data data/generated-full` | PASS; zero validator errors |
| Python full reproducibility | `python tools/reproducibility_check.py --full` | PASS; row counts and checksums match |
| React production build | `cd frontend; npm run build` | PASS; vendor chunks split; no chunk-size warning |

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
- CREATED and STATUS_CHANGED lifecycle events carry dataset-run metadata; allowed state transitions are enforced for Campaign, Lead, Opportunity and DatasetRun.
- Ground-truth labels are separate from attribution input features.

## Full-scale runtime evidence
- Java/PostgreSQL full generation completed in approximately 115 seconds for 500 campaigns, 50,000 contacts, 200,001 touchpoints, 10,000 opportunities, 10,001 orders and 499 cost records.
- Java attribution completed in approximately 38 seconds: 129,610 result rows across FIRST_TOUCH, LAST_TOUCH and LINEAR; 10,000 opportunities evaluated; zero without eligible touches.
- SQL reconciliation returned 30,000 opportunity/model groups, all summing to exactly 1.000000; zero invalid credit groups.
- Full-scale quality inspection found exactly five issues, one for each required anomaly class.
- A PostgreSQL SQL backup was generated and packaged as data/generated-full/yottaflex_attribution_full.sql.tar.gz; it includes schema and append-only local runs.
- Entity lifecycle history records CREATED events for DatasetRun, Campaign, Contact, Touchpoint, Lead and Opportunity; validated STATUS_CHANGED events cover Campaign, Lead, Opportunity and DatasetRun transitions.
- The Maven environment emits a non-fatal 401 metadata warning for a configured Flyway community repository; compilation and tests still succeed via resolved dependencies.


## Browser interaction verification (2026-11-06)
- Added Playwright with Chromium desktop and mobile projects.
- 16 deterministic browser tests passed across both projects (8 scenarios per project): dashboard rendering, model selection, dataset-run filtering, section navigation, attribution action, quality validation action, starter/full generation controls, API error feedback, refresh, and mobile viewport overflow.
- Added an opt-in live browser integration scenario. With the local Spring Boot API and PostgreSQL running, YOTTAFLEX_LIVE_API=1 enabled two more tests (desktop and mobile); both passed while reading persisted dataset runs, campaign rows, run-filtered summary and FIRST_TOUCH campaign data.
- A real 390px viewport test initially exposed 39px horizontal page overflow in the mobile header. The header now stacks and the dataset selector flexes within the available width; the mobile overflow tests now pass.
- Sidebar navigation items now link to the corresponding dashboard sections instead of acting as inert anchors.
- Production build is warning-free after vendor chunking: application 73.50 kB, Recharts 457.10 kB, D3 74.94 kB, icons 14.84 kB (minified).
- Browser tests use mocked API responses for repeatable interaction checks; the opt-in live test exercises the actual local Spring Boot/PostgreSQL API. No hosted production deployment or physical iOS/Android device testing is claimed.

- Lifecycle event tests verify old/new state capture, dataset-run metadata, allowed Opportunity transitions, rejection of terminal-state reversal, and DatasetRun validation event recording.
