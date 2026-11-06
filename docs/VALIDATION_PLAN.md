# Validation Plan

| Check | Evidence / command | Status |
|---|---|---|
| Java compile | `cd backend; mvn -DskipTests compile` | Passed 2026-11-06 |
| React production build | `cd frontend; npm run build` | Passed 2026-11-06 |
| Starter synthetic data | `python tools/generate_synthetic.py` | Passed |
| Starter PK/FK/enums/dates/anomaly labels/checksums | `python tools/validate_dataset.py` | Passed; errors=[] |
| Starter reproducibility | `python tools/reproducibility_check.py` | Passed; counts and SHA-256 equal |
| Full-scale export | `python tools/generate_synthetic.py --full --out data/generated-full` | Passed |
| Full-scale data validation | `python tools/validate_dataset.py --data data/generated-full` | Passed; errors=[] |
| Full-scale two-run checksum comparison | `python tools/reproducibility_check.py --full` | Pending |
| Database migration + JPA schema validation | Run app with configured PostgreSQL credentials | Pending |
| REST API smoke tests | Verify endpoints after backend startup | Pending |
| Attribution sum per opportunity/model | Query DB after recalculation | Engine implemented; runtime pending |
| Campaign ROI reconciliation | Compare same-currency revenue attribution to cost | Runtime pending |
| Entity lifecycle event coverage | Query entity_events | Schema present; event writes pending |
| Strict chronological split assignment | Verify row-level split separation | Pending |
