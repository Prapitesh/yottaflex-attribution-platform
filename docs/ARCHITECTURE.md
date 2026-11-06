# Architecture and Decisions

## System overview
- Python generator: deterministic source exports, anomaly labels, attribution rows, manifests and chronological evaluation splits.
- Persistence: PostgreSQL normalized relational schema, Flyway migrations, foreign keys, effective-dated audience bridge and entity event history.
- Domain: Java services generate data, calculate attribution, inspect exceptions and aggregate ROI.
- API: Spring Boot REST.
- Presentation: React + TypeScript dashboard.

## Attribution specification
Conversion anchor is `Opportunity.closed_at`. Eligible events belong to the same Contact and satisfy `closed_at - 180 days <= Touchpoint.event_at <= closed_at`, then sort chronologically.
- FIRST_TOUCH gives 1.0 to earliest eligible event.
- LAST_TOUCH gives 1.0 to latest eligible event.
- LINEAR splits 1.0 equally, assigning rounding remainder to final event.
- No eligible event means no attribution; never invent a touch.

## Anomaly policy
- Duplicate raw revenue keys remain visible, but recognized revenue aggregates once per external revenue key.
- Post-close touch events are excluded from attribution.
- Currency mismatch is quarantined; no fabricated FX rate.
- Missing costs make ROI unavailable, not zero.
- Unverified session/contact identity association is flagged.
- Ground-truth labels are separate from input features.

## Chronological evaluation
Touchpoints use event time; leads use created time; opportunities use close date; orders use recognition date. Attribution rows inherit the opportunity's split. Ground truth never enters model features.

## Audit trail
Generated Campaign, Contact, Touchpoint and Opportunity creations append immutable CREATED records in entity_events. This iteration does not implement all later lifecycle transitions.

## Scale and resource notes
Python export tooling has been run at requested full scale locally. It currently materializes intermediate lists in memory. Java database insertion performance remains unmeasured until PostgreSQL credentials are configured.
