-- Run-scoped reporting and latest calculation snapshot lookups.
CREATE INDEX IF NOT EXISTS idx_campaign_run ON campaigns(dataset_run_id);
CREATE INDEX IF NOT EXISTS idx_contact_run ON contacts(dataset_run_id);
CREATE INDEX IF NOT EXISTS idx_session_run ON web_sessions(dataset_run_id);
CREATE INDEX IF NOT EXISTS idx_quality_run_detected ON data_quality_issues(dataset_run_id, detected_at DESC);
CREATE INDEX IF NOT EXISTS idx_event_metadata_run ON entity_events USING GIN(metadata);
CREATE INDEX IF NOT EXISTS idx_opportunity_lead_close ON opportunities(lead_id, closed_at);
CREATE INDEX IF NOT EXISTS idx_order_external_revenue ON orders(external_revenue_key);
CREATE INDEX IF NOT EXISTS idx_cost_campaign_currency ON cost_records(campaign_id, currency);
CREATE INDEX IF NOT EXISTS idx_attr_latest_batch ON attribution_results(model_name, calculated_at);
