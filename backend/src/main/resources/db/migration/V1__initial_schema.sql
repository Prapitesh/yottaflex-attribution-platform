CREATE TABLE dataset_runs (
 id VARCHAR(40) PRIMARY KEY,
 seed BIGINT NOT NULL,
 campaign_count INTEGER NOT NULL CHECK(campaign_count > 0),
 contact_count INTEGER NOT NULL CHECK(contact_count > 0),
 touchpoint_count INTEGER NOT NULL CHECK(touchpoint_count > 0),
 opportunity_count INTEGER NOT NULL CHECK(opportunity_count > 0),
 created_at TIMESTAMPTZ NOT NULL,
 status VARCHAR(20) NOT NULL CHECK(status IN ('GENERATED','VALIDATED','FAILED'))
);

CREATE TABLE channels (
 id VARCHAR(50) PRIMARY KEY,
 name VARCHAR(80) NOT NULL,
 channel_type VARCHAR(30) NOT NULL CHECK(channel_type IN ('PAID','ORGANIC','OFFLINE','REFERRAL','EMAIL','SOCIAL')),
 dataset_run_id VARCHAR(40) NOT NULL REFERENCES dataset_runs(id),
 UNIQUE(dataset_run_id,name)
);

CREATE TABLE campaigns (
 id VARCHAR(50) PRIMARY KEY,
 name VARCHAR(120) NOT NULL,
 dataset_run_id VARCHAR(40) NOT NULL REFERENCES dataset_runs(id),
 channel_id VARCHAR(50) NOT NULL REFERENCES channels(id),
 budget NUMERIC(16,2) NOT NULL CHECK(budget >= 0),
 currency CHAR(3) NOT NULL,
 start_date DATE NOT NULL,
 end_date DATE NOT NULL CHECK(end_date >= start_date),
 status VARCHAR(20) NOT NULL CHECK(status IN ('PLANNED','ACTIVE','PAUSED','COMPLETED'))
);

CREATE TABLE creatives (
 id VARCHAR(60) PRIMARY KEY,
 name VARCHAR(120) NOT NULL,
 format VARCHAR(30) NOT NULL,
 campaign_id VARCHAR(50) NOT NULL REFERENCES campaigns(id)
);

CREATE TABLE audiences (
 id VARCHAR(50) PRIMARY KEY,
 name VARCHAR(100) NOT NULL,
 segment_rule VARCHAR(120) NOT NULL,
 dataset_run_id VARCHAR(40) NOT NULL REFERENCES dataset_runs(id),
 UNIQUE(dataset_run_id,name)
);

CREATE TABLE contacts (
 id VARCHAR(50) PRIMARY KEY,
 segment VARCHAR(30) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 dataset_run_id VARCHAR(40) NOT NULL REFERENCES dataset_runs(id)
);

CREATE TABLE campaign_audiences (
 campaign_id VARCHAR(50) NOT NULL REFERENCES campaigns(id),
 audience_id VARCHAR(50) NOT NULL REFERENCES audiences(id),
 effective_from DATE NOT NULL,
 effective_to DATE,
 PRIMARY KEY(campaign_id,audience_id,effective_from),
 CHECK(effective_to IS NULL OR effective_to >= effective_from)
);

CREATE TABLE touchpoints (
 id VARCHAR(110) PRIMARY KEY,
 contact_id VARCHAR(50) NOT NULL REFERENCES contacts(id),
 campaign_id VARCHAR(50) NOT NULL REFERENCES campaigns(id),
 creative_id VARCHAR(60) REFERENCES creatives(id),
 channel_id VARCHAR(50) NOT NULL REFERENCES channels(id),
 event_at TIMESTAMPTZ NOT NULL,
 touch_type VARCHAR(20) NOT NULL CHECK(touch_type IN ('CLICK','IMPRESSION','EMAIL_OPEN','CALL','EVENT','STORE_VISIT')),
 source_type VARCHAR(20) NOT NULL CHECK(source_type IN ('ONLINE','OFFLINE'))
);
CREATE INDEX idx_touch_contact_time ON touchpoints(contact_id,event_at);
CREATE INDEX idx_touch_campaign ON touchpoints(campaign_id);

CREATE TABLE web_sessions (
 id VARCHAR(80) PRIMARY KEY,
 contact_id VARCHAR(50) REFERENCES contacts(id),
 anonymous_id VARCHAR(80),
 started_at TIMESTAMPTZ NOT NULL,
 landing_path VARCHAR(100) NOT NULL,
 identity_link_verified BOOLEAN NOT NULL DEFAULT FALSE,
 dataset_run_id VARCHAR(40) NOT NULL REFERENCES dataset_runs(id),
 CHECK(contact_id IS NOT NULL OR anonymous_id IS NOT NULL)
);

CREATE TABLE leads (
 id VARCHAR(80) PRIMARY KEY,
 contact_id VARCHAR(50) NOT NULL REFERENCES contacts(id),
 session_id VARCHAR(80) REFERENCES web_sessions(id),
 created_at TIMESTAMPTZ NOT NULL,
 status VARCHAR(20) NOT NULL CHECK(status IN ('NEW','QUALIFIED','DISQUALIFIED','CONVERTED'))
);

CREATE TABLE opportunities (
 id VARCHAR(80) PRIMARY KEY,
 lead_id VARCHAR(80) NOT NULL REFERENCES leads(id),
 created_at TIMESTAMPTZ NOT NULL,
 closed_at TIMESTAMPTZ NOT NULL,
 stage VARCHAR(20) NOT NULL CHECK(stage IN ('WON','LOST','OPEN')),
 expected_revenue NUMERIC(16,2) NOT NULL CHECK(expected_revenue >= 0),
 currency CHAR(3) NOT NULL,
 CHECK(closed_at >= created_at)
);

CREATE TABLE orders (
 id VARCHAR(80) PRIMARY KEY,
 opportunity_id VARCHAR(80) NOT NULL REFERENCES opportunities(id),
 external_revenue_key VARCHAR(80) NOT NULL,
 revenue NUMERIC(16,2) NOT NULL CHECK(revenue >= 0),
 currency CHAR(3) NOT NULL,
 recognized_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_orders_revenue_key ON orders(external_revenue_key);

CREATE TABLE cost_records (
 id VARCHAR(80) PRIMARY KEY,
 campaign_id VARCHAR(50) NOT NULL REFERENCES campaigns(id),
 incurred_on DATE NOT NULL,
 amount NUMERIC(16,2) NOT NULL CHECK(amount >= 0),
 currency CHAR(3) NOT NULL,
 cost_type VARCHAR(30) NOT NULL
);

CREATE TABLE attribution_results (
 id VARCHAR(36) PRIMARY KEY,
 opportunity_id VARCHAR(80) NOT NULL REFERENCES opportunities(id),
 touchpoint_id VARCHAR(110) NOT NULL REFERENCES touchpoints(id),
 model_name VARCHAR(20) NOT NULL CHECK(model_name IN ('FIRST_TOUCH','LAST_TOUCH','LINEAR')),
 credit NUMERIC(9,6) NOT NULL CHECK(credit >= 0 AND credit <= 1),
 attributed_revenue NUMERIC(16,2) NOT NULL CHECK(attributed_revenue >= 0),
 calculated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_attr_model ON attribution_results(model_name,opportunity_id,calculated_at);

CREATE TABLE entity_events (
 id BIGSERIAL PRIMARY KEY,
 entity_type VARCHAR(40) NOT NULL,
 entity_id VARCHAR(110) NOT NULL,
 event_type VARCHAR(40) NOT NULL,
 old_state VARCHAR(30),
 new_state VARCHAR(30),
 occurred_at TIMESTAMPTZ NOT NULL,
 metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX idx_entity_events_entity ON entity_events(entity_type,entity_id,occurred_at);

CREATE TABLE data_quality_issues (
 id VARCHAR(40) PRIMARY KEY,
 issue_type VARCHAR(50) NOT NULL,
 entity_type VARCHAR(40) NOT NULL,
 entity_id VARCHAR(110) NOT NULL,
 severity VARCHAR(15) NOT NULL CHECK(severity IN ('INFO','WARNING','ERROR')),
 description TEXT NOT NULL,
 ground_truth_label VARCHAR(40) NOT NULL,
 detected_at TIMESTAMPTZ NOT NULL,
 resolved BOOLEAN NOT NULL DEFAULT FALSE,
 dataset_run_id VARCHAR(40) REFERENCES dataset_runs(id)
);

CREATE TABLE ground_truth (
 id VARCHAR(40) PRIMARY KEY,
 issue_type VARCHAR(50) NOT NULL,
 entity_type VARCHAR(40) NOT NULL,
 entity_id VARCHAR(110) NOT NULL,
 expected_decision VARCHAR(100) NOT NULL,
 evaluation_split VARCHAR(20) NOT NULL,
 dataset_run_id VARCHAR(40) REFERENCES dataset_runs(id)
);
