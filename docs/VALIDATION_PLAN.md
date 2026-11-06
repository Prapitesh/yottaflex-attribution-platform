# Validation Evidence — 2026-11-06

| Check | Command | Result |
|---|---|---|
| Backend compile | `cd backend; mvn -q -DskipTests compile` | PASS |
| Frontend build | `cd frontend; npm run build` | PASS; non-fatal JS chunk >500kB warning |
| Starter dataset validation | `python tools/generate_synthetic.py; python tools/validate_dataset.py` | PASS; errors=[] |
| Starter reproducibility | `python tools/reproducibility_check.py` | PASS; counts + hashes match |
| Full dataset | `python tools/generate_synthetic.py --full --out data/generated-full` | PASS; required master/transaction volumes |
| Full dataset validation | `python tools/validate_dataset.py --data data/generated-full` | PASS; errors=[]; anomaly labels present |
| Full reproducibility | `python tools/reproducibility_check.py --full` | PASS; counts + hashes match |
| Python unit tests | `python -m unittest discover -s tools/tests -v` | PENDING rerun |
| PostgreSQL/Flyway/JPA startup | `cd backend; mvn spring-boot:run` | BLOCKED by local postgres password (SQLSTATE 28P01) |
| REST API smoke tests | Start backend and call endpoints | PENDING PostgreSQL |
| DB attribution reconciliation | Query by opportunity/model | PENDING DB; exported attribution sums validated |

## Acceptance details
- PK uniqueness, core FK links, channel enums and date order validated in CSV exports.
- Duplicate revenue is an intentional raw-event condition and reports must deduplicate by external key.
- Post-conversion touch excluded; currency mismatch quarantined; missing cost yields unavailable ROI; identity without evidence is rejected.
- Ground truth is separate from source input data.
- Per-opportunity/model attribution credits sum to one.
- Attribution split is inherited from opportunity conversion split.
