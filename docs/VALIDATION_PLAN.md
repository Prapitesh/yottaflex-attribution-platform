# Validation Evidence — 2026-11-06

| Check | Command / evidence | Result |
|---|---|---|
| Java compilation | `cd backend; mvn -q -DskipTests compile` after entity, bridge, events and detector changes | PASS |
| React type-check + production bundle | `cd frontend; npm run build` | PASS; Vite emits a non-fatal >500kB JS chunk warning |
| Starter CSV generation | `python tools/generate_synthetic.py` | PASS; 12 campaigns, 300 contacts, 1,201 touchpoints, 80 opportunities |
| Starter relational/logic validation | `python tools/validate_dataset.py` | PASS; errors=[]; 5 ground-truth labels; 117 attribution groups reconcile |
| Starter reproducibility | `python tools/reproducibility_check.py` | PASS; counts and per-file SHA-256 values match |
| Full CSV generation | `python tools/generate_synthetic.py --full --out data/generated-full` | PASS; 500 campaigns, 50,000 contacts, 200,001 touchpoints, 10,000 opportunities |
| Full relational/logic validation | `python tools/validate_dataset.py --data data/generated-full` | PASS; errors=[]; 13,410 attribution groups reconcile |
| Full reproducibility | `python tools/reproducibility_check.py --full` | PASS; counts and checksums match |
| PostgreSQL migration + JPA validation | Run backend with project DB credentials | PENDING: local PostgreSQL password not available |
| REST API smoke tests | Start backend and exercise summary/generate/recalculate/quality endpoints | PENDING |
| Attribution DB reconciliation | SQL group-by check on opportunity/model | PENDING runtime; equivalent exported group check PASS |
| Entity history verification | Query entity_events after generation | PENDING runtime; creation event writes implemented for campaign/contact/touchpoint/opportunity |

## Acceptance rule notes
- Primary-key IDs are unique; FK checks cover core generated relationships and campaign-audience bridges.
- Revenue external keys intentionally allow duplicate raw events; revenue aggregation must count one key once.
- Touchpoint eligibility requires event time <= conversion close and >= close - 180 days.
- Currency mismatch and missing costs mark ROI unavailable instead of inventing conversion/zero cost.
- Ground truth is not consumed by attribution.
- The chronological split is assigned to touchpoints/leads by event date, opportunities/orders by conversion/recognition date, and attribution results inherit the opportunity split.
