# Assumptions and Known Deviations

1. Currency is represented explicitly; no conversion rate is presumed.
2. Attribution lookback is currently 180 days.
3. The starter volume is intended for local development; the full volume is configurable to the requested scale.
4. Synthetic opportunity data is closed-won for initial ROI walkthrough; lost/open stage distributions need an additional generator pass.
5. The initial implementation is an engineering prototype, not a full production ledger.
6. The first validation pass is not complete until PostgreSQL-backed tests, all five anomaly cases, export checksums and lifecycle checks have been executed.
