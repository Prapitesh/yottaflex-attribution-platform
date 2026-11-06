# Data Dictionary

All IDs are immutable synthetic identifiers. Timestamps are UTC instants; monetary values are decimal amounts with an ISO-4217 currency code.

| Entity | Primary key | Important fields | Foreign keys / cardinality | Validation |
|---|---|---|---|---|
| Channel | id | name, channel_type | 1:N Campaign, Touchpoint | name unique; type enum |
| Campaign | id | name, budget, currency, start_date, end_date, status | N:1 Channel; N:M Audience through campaign_audiences; 1:N Creative, Touchpoint, CostRecord | budget >= 0; end >= start; currency 3 chars |
| Creative | id | name, format | N:1 Campaign; optional on Touchpoint | Campaign required |
| Audience | id | name, segment_rule | N:M Campaign through campaign_audiences | name unique |
| Contact | id | segment, created_at | 1:N Touchpoint, Session, Lead | required synthetic segment |
| Touchpoint | id | event_at, touch_type, source_type | N:1 Contact, Campaign, Channel; optional Creative | event timestamp required; online/offline enum |
| WebSession | id | anonymous_id, started_at, landing_path, identity_link_verified | optional N:1 Contact; 1:N Lead | contact or anonymous ID required |
| Lead | id | created_at, status | N:1 Contact; optional N:1 WebSession; 1:N Opportunity | lifecycle enum |
| Opportunity | id | created_at, closed_at, stage, expected_revenue, currency | N:1 Lead; 1:N Order, AttributionResult | closed >= created; revenue >= 0 |
| Order | id | external_revenue_key, revenue, currency, recognized_at | N:1 Opportunity | external revenue key unique; revenue >= 0 |
| CostRecord | id | incurred_on, amount, currency, cost_type | N:1 Campaign | amount >= 0; currency explicit |
| AttributionResult | id | model_name, credit, attributed_revenue, calculated_at | N:1 Opportunity and Touchpoint | credit in [0,1]; unique opportunity/touch/model |

## Relationship cardinalities
- Channel 1:N Campaign; Channel 1:N Touchpoint.
- Campaign 1:N Creative, Touchpoint and CostRecord.
- Campaign N:M Audience through campaign_audiences (effective dates retained).
- Contact 1:N Touchpoint, WebSession and Lead.
- Contact 1:N Leads; Lead 1:N Opportunities.
- WebSession 1:N Leads (session link optional on Lead).
- Opportunity 1:N Orders and AttributionResults.
- Touchpoint 1:N AttributionResults across opportunities/models.
- State history is represented by entity_events, not by overwriting previous state rows.

## Fields and enums
- Campaign status: PLANNED, ACTIVE, PAUSED, COMPLETED.
- Channel type: PAID, ORGANIC, OFFLINE, REFERRAL, EMAIL, SOCIAL.
- Touch type: CLICK, IMPRESSION, EMAIL_OPEN, CALL, EVENT, STORE_VISIT.
- Source type: ONLINE, OFFLINE.
- Lead status: NEW, QUALIFIED, DISQUALIFIED, CONVERTED.
- Opportunity stage: WON, LOST, OPEN.
- Attribution model: FIRST_TOUCH, LAST_TOUCH, LINEAR.
- Issue severity: INFO, WARNING, ERROR.
- Evaluation split: chronological TRAIN, VALIDATION, TEST; anomaly labels belong in ground_truth only.

## Current implementation notes
The migration defines the requested entity relationships and key constraints. The first generator iteration populates the principal transaction chain. Some required edge-case variants and event-history exports remain on the next implementation pass; do not describe those as complete until verified.
