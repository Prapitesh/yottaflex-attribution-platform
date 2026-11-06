# Data Dictionary

All identifiers are immutable synthetic string keys. Timestamps are UTC ISO-8601. Monetary fields are decimal amounts, never floating-point database columns, with an accompanying three-character ISO currency code.

| Entity | PK | Fields and types | FK / cardinality | Required / constraints |
|---|---|---|---|---|
| Channel | id VARCHAR(30) | name VARCHAR(80), channel_type VARCHAR(30) | Channel 1:N Campaign; Channel 1:N Touchpoint | name unique; channel_type enum PAID/ORGANIC/OFFLINE/REFERRAL/EMAIL/SOCIAL |
| Campaign | id VARCHAR(30) | name VARCHAR(120), budget DECIMAL(16,2), currency VARCHAR(3), start_date/end_date DATE, status VARCHAR(20) | N:1 Channel; 1:N Creative/Touchpoint/CostRecord; N:M Audience through CampaignAudience | all except end? required; budget >= 0; end >= start; currency length 3; state enum PLANNED/ACTIVE/PAUSED/COMPLETED |
| Creative | id VARCHAR(30) | name VARCHAR(120), format VARCHAR(30) | N:1 Campaign; optionally referenced by Touchpoint | name and format required |
| Audience | id VARCHAR(30) | name VARCHAR(100), segment_rule VARCHAR(120) | N:M Campaign through CampaignAudience | name unique |
| CampaignAudience | campaign_id + audience_id + effective_from | effective_to DATE optional | N:1 Campaign and Audience | effective_from required; effective_to null or >= effective_from; composite primary key |
| Contact | id VARCHAR(30) | segment VARCHAR(30), created_at TIMESTAMPTZ | 1:N Touchpoint, WebSession, Lead | segment required; no real personal data |
| Touchpoint | id VARCHAR(60) | event_at TIMESTAMPTZ, touch_type VARCHAR(20), source_type VARCHAR(20) | N:1 Contact, Campaign, Channel; Creative optional | event_at required; type CLICK/IMPRESSION/EMAIL_OPEN/CALL/EVENT/STORE_VISIT; source ONLINE/OFFLINE |
| WebSession | id VARCHAR(30) | anonymous_id VARCHAR(80) optional, started_at TIMESTAMPTZ, landing_path VARCHAR(100), identity_link_verified BOOLEAN | optional N:1 Contact; 1:N Lead | either contact_id or anonymous_id required; identity association requires proof |
| Lead | id VARCHAR(30) | created_at TIMESTAMPTZ, status VARCHAR(20) | N:1 Contact; optional N:1 WebSession; 1:N Opportunity | status NEW/QUALIFIED/DISQUALIFIED/CONVERTED |
| Opportunity | id VARCHAR(30) | created_at/closed_at TIMESTAMPTZ, stage VARCHAR(20), expected_revenue DECIMAL(16,2), currency VARCHAR(3) | N:1 Lead; 1:N Order and AttributionResult | closed >= created; revenue >= 0; stage WON/LOST/OPEN |
| Order | id VARCHAR(30) | external_revenue_key VARCHAR(60), revenue DECIMAL(16,2), currency VARCHAR(3), recognized_at TIMESTAMPTZ | N:1 Opportunity | raw external key intentionally not unique so duplicate-source events are auditable; aggregate once by key |
| CostRecord | id VARCHAR(30) | incurred_on DATE, amount DECIMAL(16,2), currency VARCHAR(3), cost_type VARCHAR(30) | N:1 Campaign | amount >= 0; compare with campaign currency before ROI |
| AttributionResult | id VARCHAR(36) | model_name VARCHAR(20), credit DECIMAL(9,6), attributed_revenue DECIMAL(16,2), calculated_at TIMESTAMPTZ | N:1 Opportunity and Touchpoint | model FIRST_TOUCH/LAST_TOUCH/LINEAR; credit [0,1]; opportunity/model credits total 1 |
| EntityEvent | id BIGSERIAL | entity_type, entity_id, event_type, old_state/new_state, occurred_at, JSONB metadata | polymorphic entity reference (intentionally no DB FK) | event append-only; event timestamp required |
| DataQualityIssue | id VARCHAR(40) | issue_type, entity_type/id, severity, description, ground_truth_label, detected_at, resolved | polymorphic entity reference | severity INFO/WARNING/ERROR |
| GroundTruth | id VARCHAR(40) | issue_type, entity_type/id, expected_decision, evaluation_split | polymorphic evaluation reference | evaluation-only; never an input feature |

## ID conventions
- Campaign: CMP-2026-000001-<run-suffix>
- Touchpoint: TOUCH-<contact-id>-<sequence>-<run-suffix>
- Session: SES-2026-000001-<run-suffix>
- Lead: LEAD-2026-000001-<run-suffix>
- Opportunity: OPP-2026-000001-<run-suffix>
- Other entities use stable type prefixes, zero-padded numeric counters and a short dataset-run suffix to keep IDs unique across append-only runs.

## Units, optionality and validation
- Time values: UTC timestamp with timezone; campaign boundaries and cost date use calendar dates.
- Money: two decimal places; currency is explicit. No implicit currency conversion.
- Attribution credit: unitless decimal from 0 to 1; sum per opportunity/model equals 1 when a qualifying journey exists.
- Nullable fields: Creative on Touchpoint, Contact on WebSession (subject to identity policy), anonymous_id, Lead.session_id, and CampaignAudience.effective_to.
- Unique fields: all primary keys, Channel.name, Audience.name, Creative IDs, and Order.id. Order.external_revenue_key is not unique by design.
- Foreign keys are validated for all non-polymorphic relationships. CampaignAudience is the N:M bridge; EntityEvent/Quality/GroundTruth reference entity IDs polymorphically and preserve history/evaluation context.
- Lifecycle/status enums are enforced in the PostgreSQL migration.

## Relationship cardinality
Channel 1:N Campaign and Touchpoint; Campaign 1:N Creative/Touchpoint/CostRecord; Campaign N:M Audience via CampaignAudience; Contact 1:N Touchpoint/WebSession/Lead; WebSession 1:N Lead; Lead 1:N Opportunity; Opportunity 1:N Order/AttributionResult; Touchpoint 1:N AttributionResult.

## Evaluation splits
Rows use chronological event time / conversion close date. Attribution results inherit their opportunity's split. The three boundaries are documented in `evaluation_split.json`; `ground_truth.csv` is isolated from all input and attribution features.
