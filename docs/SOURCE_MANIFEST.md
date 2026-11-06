# Source Manifest

| Source | Use | License / provenance |
|---|---|---|
| YottaFlex-provided challenge brief | Requirements and acceptance criteria | Candidate-provided challenge material; not redistributed |
| YottaFlex FAQ | AI disclosure, individual-work, public repository and prompt history requirements | Candidate-provided challenge material |
| YottaFlex Submission Guidelines | Executable code, public GitHub and reasoning expectations | Candidate-provided challenge material |
| Synthetic generator | All generated business records and CSV/JSON exports | Original synthetic data, fixed seed 20261022; no external dataset |
| PostgreSQL export | `data/generated-full/yottaflex_attribution_full.sql.tar.gz` | Local PostgreSQL dump of synthetic runs and schema; compressed plain SQL archive; no credentials |
| Third-party dependencies | Framework/runtime components | Standard open-source packages; their licenses remain with their authors |

| Automated validation workflow | `.github/workflows/ci.yml` | Original project CI configuration; Java, Python data generation/validation/reproducibility, frontend build and browser tests |
| Export validator | tools/validate_dataset.py | Original semantic validator for keys, relationships, anomaly evidence, attribution windows, manifests and checksums |
| Reproducibility checker | tools/reproducibility_check.py | Original fixed-seed double-generation and artifact checksum comparison |
