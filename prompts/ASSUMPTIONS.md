# Assumptions and Known Deviations

1. Currency is represented explicitly; no conversion rate is presumed.
2. Attribution lookback is currently 180 days.
3. The starter volume is intended for local development; the full volume is configurable to the requested scale.
4. Synthetic opportunity data is closed-won for initial ROI walkthrough; lost/open stage distributions need an additional generator pass.
5. The initial implementation is an engineering prototype, not a full production ledger.
6. Starter and full-scale Java/PostgreSQL generation, attribution, quality inspection, and live API checks have been executed. Python starter and full-scale exports, validators, and reproducibility checks have also passed.
7. Lifecycle audit records capture CREATED events and append-only status transitions for Campaign, Lead, Opportunity and DatasetRun. Delete/restore workflows and transitions for entities without status fields are outside this prototype.
