# AI Prompt History

The YottaFlex Candidate FAQ requires all prompts submitted to every AI tool used for this project. Record each prompt exactly; AI response text is optional.

## 2026-11-06 — ChatGPT (this conversation)

### Prompt 1 — kickoff
"Hi aitipamula prapitesh, [candidate shared the YottaFlex AI Screening pass email and the complete 72 Hours Code Challenge problem statement, including business context, scope, all 12 entities, deterministic IDs, target synthetic volumes, five required anomaly cases, data dictionary/export/reproducibility instructions, acceptance criteria, public-data rules and portal deadline]. use java full stack [$remote-desktop-commander] Like ChatGPT, as you can see, I have opened the Intelligent Jotaflex. [candidate shared the application portal and code challenge page]. Try to start to build my project. I'm having three days. So today how much you can do, you can do it. Is it okay?"

### Prompt 2 — files and submission requirements
Candidate provided the official YottaFlex Candidate Project FAQ and Candidate Project Submission Guidelines PDFs for review.

**AI assistance disclosure:** ChatGPT assisted with architecture, scaffolding, implementation, data-generation tooling, debugging, and validation scripts. AI answer text is not required by the FAQ. The candidate must review and understand all code, verify the runtime behavior, and explain decisions. Record every additional prompt given to any AI tool or IDE assistant here before submission.


### Prompt 3 — project completion and verification (2026-11-06)
"[$remote-desktop-commander] You have my system control, right? Do it. I've opened that PJ9 also. Do everything you want, but complete the project, that's it."

**Work performed in this session:** reran the Java backend unit suite, frontend TypeScript/Vite production build, Python unit suite, starter dataset validator and reproducibility check; generated the full-scale CSV export package with seed 20261022; validated all full-scale rows and confirmed row-count and SHA-256 reproducibility. Inspected PostgreSQL 18 local connectivity and confirmed the database still requires a locally configured password. No database password was extracted, reset, or changed. No Git remote or authenticated GitHub CLI was available, so repository publication was not performed.


### Prompt 4 — finish remaining challenge work (2026-11-06)
"And I have opened my GitHub also. You can complete the balance work."

**Work performed:** verified the live React/Vite frontend and Spring Boot/PostgreSQL API, checked frontend/backend structure and conventions, executed the Java/PostgreSQL FULL-scale generator (500 campaigns, 50,000 contacts, 200,001 touchpoints, 10,000 opportunities), ran all three attribution models (129,610 result rows across 30,000 opportunity/model groups; all credits reconcile to 1.000000), and validated all five anomaly classes. Generated a compressed PostgreSQL SQL backup, SHA-256 checksums, and Java run evidence. Updated README, architecture, data dictionary, validation evidence and assumptions to reflect current runtime results. GitHub connector returned no repositories for the linked account; no local Git remote is configured, so publication remains pending a repository URL or an authenticated GitHub connection with repository access.


### Prompt 5 - public repository access (2026-11-06)
"As you can see, I opened in incognito mode and in the normal tab. See."
"do it for me"

### Prompt 6 - localhost verification (2026-11-06)
"try to do testing in locahost which i have opened if it is ok as expected then i submit github repo"
"so you are saying coding challenge is ready"

### Prompt 7 - close remaining quality gaps (2026-11-06)
"1. We haven't manually tested every browser interaction.
2. The frontend bundle-size warning remains, but it doesn't prevent the build. fix these also fix if any issues are pending in challenge"

**AI assistance disclosure update:** ChatGPT helped add Playwright browser tests, wire sidebar navigation to dashboard sections, diagnose and fix a mobile horizontal overflow issue, split the frontend bundle into vendor chunks, and update validation documentation. The candidate should personally review and understand these changes and retain any additional prompts from other AI tools or earlier conversations not represented in this file.

**Work performed:** 16 mocked-API browser tests passed across desktop/mobile Chromium; 2 additional live browser integration tests passed against the local Spring Boot/PostgreSQL instance. The live tests verified real dataset-run retrieval, dashboard data rendering, run filtering and FIRST_TOUCH model switching. Python unit tests (5), starter/full dataset validators (zero errors), and starter/full reproducibility checks (row counts and SHA-256 checksums match) passed.


### Additional exact prompts recovered from the earlier YottaFlex project conversation (2026-11-06)
"Yeah, start writing code of the backend first in the Yota Flex folder which I have opened in IntelliJ. Start doing the work."
"Complete the project. Like I have opened IntelliJ, VS Code, and have run the frontend. Now I have opened pgAdmin for the PostgreSQL connection. You can do everything you want. Complete the project."
"try to follow frontend,backend folder structure and coding standards And select which one fits to you properly, the postgresql"
"And I have opened my GitHub also. You can complete the balance work."

**Additional work performed:** implemented append-only lifecycle transition events for Campaign, Lead, Opportunity and DatasetRun. Added an allow-list for valid transitions and tests for old/new state metadata, run metadata, invalid terminal reversals and run validation. Backend suite now contains 13 passing tests.


### Prompt 8 - complete remaining challenge work before Sunday submission (2026-11-06)
"i will submit on sunday if everyting is over think like a human complete pending work in coding challenge"

**Additional work performed:** corrected the unverified anonymous session so it has no contact association; upgraded the Python export validator to check anomaly target semantics, identity-link rejection, contact-matched 180-day attribution eligibility, normalized credits, chronological split consistency, manifest row counts and SHA-256 checksums; added regression tests; corrected mixed-currency ROI aggregation so any mismatched cost row quarantines campaign ROI; added a DashboardService regression test; and added GitHub Actions CI for Python starter/full validation and reproducibility, Maven verification, frontend build and Chromium desktop/mobile tests.
