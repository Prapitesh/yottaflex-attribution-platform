# Architecture and Decisions

## Workflow
1. Generate master records: Channel, Audience, Campaign and Creative, then Contact.
2. Generate dependent Touchpoints, WebSessions, Leads, Opportunities and Orders using those IDs.
3. Generate CostRecords from campaign budget-derived spend, with explicit labelled exceptions.
4. Calculate attribution from Contact journeys in chronological event order.
5. Evaluate the required data-quality rules and save exception + ground-truth records separately.
6. Expose aggregate metrics and exceptions through REST endpoints.
7. Render decision-oriented summaries in React.

## Attribution rules
- Conversion anchor: Opportunity.closed_at.
- Lookback: 180 days before close.
- Eligible touchpoints: same Contact, at or before close, and no earlier than lookback start.
- FIRST_TOUCH gives 1.0 to the earliest eligible touch.
- LAST_TOUCH gives 1.0 to the latest eligible touch.
- LINEAR splits 1.0 equally across eligible touches. Rounding remainder is assigned to the final touch so the opportunity's credit totals exactly 1.0.
- No eligible touch means unattributed opportunity; the system must not fabricate a touchpoint.

## Exceptions
- Duplicate revenue: count a unique external revenue key once.
- Post-conversion touch: exclude from attribution and report an issue.
- Currency mismatch: quarantine ROI; do not invent FX conversion.
- Missing campaign cost: mark ROI unavailable, not 0%.
- Anonymous identity link: reject unless identity evidence is recorded.

## Trade-offs
- Synthetic-only data avoids privacy and redistribution problems.
- PostgreSQL + Flyway makes constraints and schema versioning visible.
- Deterministic IDs support repeatability and traceability.
- Full dataset is generated on demand to keep local iteration quick.
