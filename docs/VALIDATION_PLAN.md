# Validation Evidence - 2026-11-06

## Verified locally

| Check | Command | Result |
|---|---|---|
| Backend compile and unit tests | cd backend; mvn clean verify | PASS - 15 tests, 0 failures |
| Attribution rules | AttributionEngineTest | PASS - first touch, last touch, linear normalization, lookback and no-eligible-touch handling |
| Request validation | GenerationRequestTest | PASS - starter/full defaults and scale overrides |
| API error envelope | ApiExceptionHandlerTest | PASS - malformed request and business validation responses |
| Campaign ROI currency quarantine | DashboardServiceTest | PASS - any mismatched cost row makes campaign ROI unavailable |
| Anonymous identity handling | WebSessionIdentityTest and DataQualityServiceTest | PASS - unverified sessions stay unlinked and are still detected |
| Run event metadata | GenerationEventRecorderTest | PASS - creation and status-change events retain datasetRunId and old/new state |
| Python unit tests | python -m unittest discover -s tools/tests -v | PASS - 5 tests |
| Python starter validation | python tools/validate_dataset.py --data data/generated | PASS - zero errors |
| Python starter reproducibility | python tools/reproducibility_check.py | PASS - counts and SHA-256 checksums match |
| Python full validation | python tools/validate_dataset.py --data data/generated-full | PASS - zero errors |
| Python full reproducibility | python tools/reproducibility_check.py --full | PASS - counts and SHA-256 checksums match |
| React production build | cd frontend; npm run build | PASS - vendor chunks split; no chunk-size warning |
| Browser interaction tests | cd frontend; npm run test:e2e | PASS - 16 mocked desktop/mobile Chromium tests; optional live tests run with YOTTAFLEX_LIVE_API=1 |

## Runtime verification

| Check | Result |
|---|---|
| PostgreSQL 18.6 authentication | PASS - yottaflex_attribution database connected |
| Flyway migrations | PASS - V1-V3 validated/applied |
| Hibernate schema validation | PASS - ddl-auto=validate |
| Java starter generation | PASS - 12 campaigns, 300 contacts, 1,201 touchpoints, 80 opportunities |
| Java attribution calculation | PASS - 846 attribution result rows |
| Java data-quality inspection | PASS - all five required anomaly categories detected |
| Dashboard summary API | PASS - HTTP 200 |
| Dataset runs API | PASS - HTTP 200 |
| Campaigns API | PASS - HTTP 200 |
| Attribution results API | PASS - HTTP 200 |
| Quality issues API | PASS - HTTP 200 |
| Frontend Vite server | PASS - HTTP 200 on port 5173 |
| Frontend/backend CORS | PASS - http://localhost:5173 allowed |
| Java/PostgreSQL full-scale generation | PASS - 500 campaigns, 50,000 contacts, 200,001 touchpoints, 10,000 opportunities, 10,001 orders and 499 cost records; approximately 115 seconds |

## Acceptance rules covered

- Run-scoped synthetic snapshots preserve previous generated runs.
- FIRST_TOUCH, LAST_TOUCH and LINEAR use eligible same-contact touchpoints in the 180 days ending at conversion.
- Post-conversion and out-of-window touches are excluded.
- Attribution calculation records are append-only.
- Duplicate revenue source rows are retained for audit; reporting deduplicates by external revenue key.
- Currency-mismatched campaign cost quarantines campaign ROI; no implicit FX rate is assumed.
- Missing campaign cost means ROI unavailable, not 0%.
- Unverified anonymous sessions remain unlinked to a contact.
- Quality issues and ground truth are appended per validation run.
- CREATED and STATUS_CHANGED lifecycle events carry dataset-run metadata; valid transitions are enforced for Campaign, Lead, Opportunity and DatasetRun.
- Ground-truth labels are evaluation-only and never used as attribution input features.

## Full-scale runtime evidence

- Java/PostgreSQL full generation completed in approximately 115 seconds for 500 campaigns, 50,000 contacts, 200,001 touchpoints, 10,000 opportunities, 10,001 orders and 499 cost records.
- Java attribution completed in approximately 38 seconds: 129,610 result rows across FIRST_TOUCH, LAST_TOUCH and LINEAR; 10,000 opportunities evaluated; zero without eligible touches.
- SQL reconciliation returned 30,000 opportunity/model groups, all summing to exactly 1.000000; zero invalid credit groups.
- Full-scale quality inspection found exactly five issues, one for each required anomaly class.
- PostgreSQL backup is packaged as data/generated-full/yottaflex_attribution_full.sql.tar.gz; archived anonymous-session rows were corrected to have NULL contact_id when identity is unverified.
- Python semantic validation verifies anomaly targets, unverified identity links, 180-day contact-matched attribution, credit totals, chronological split consistency, manifest counts and SHA-256 checksums, including the separate SHA256SUMS.txt artifact manifest.
- A non-fatal Maven metadata 401 warning may appear for the configured Flyway community repository; compilation, tests and packaging succeed with resolved dependencies.

## Browser interaction verification

- Playwright runs eight mocked-API scenarios on desktop Chromium and the same eight on mobile Chromium (16 tests total; all passed in the final default run).
- Two optional live browser-to-Spring Boot/PostgreSQL integration tests are included. Both passed in the earlier local live run; the final currency and anonymous-identity changes are additionally covered by DashboardServiceTest, WebSessionIdentityTest and DataQualityServiceTest.
- Coverage includes dashboard rendering, model selection, run filtering, sidebar navigation, attribution, validation, starter/full generation controls, API error feedback, refresh and mobile overflow.
- A 390px viewport test initially exposed 39px horizontal overflow; the responsive header fix resolved it.
- No hosted production deployment or physical iOS/Android device testing is claimed.

## Continuous integration

- .github/workflows/ci.yml runs on pushes to main, pull requests and manual dispatch.
- It checks Python unit tests, starter/full data generation and semantic validation, reproducibility, Maven clean verify, frontend production build and desktop/mobile Chromium browser tests.
