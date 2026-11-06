# Assumptions and Known Deviations

1. Currency is represented explicitly; no conversion rate is presumed.
2. Attribution lookback is currently 180 days.
3. The starter volume is intended for local development; the full volume is configurable to the requested scale.
4. Synthetic opportunity data is closed-won for initial ROI walkthrough; lost/open stage distributions need an additional generator pass.
5. The initial implementation is an engineering prototype, not a full production ledger.
6. Starter PostgreSQL-backed generation, attribution, quality inspection, and live API checks have been executed. Python full-scale exports and checksums have been validated. Java full-scale database generation remains a separate scale test.
7. Lifecycle audit records currently capture CREATED events for generated DatasetRun, Campaign, Contact, Touchpoint and Opportunity entities; later business status transitions are not fully modelled.
