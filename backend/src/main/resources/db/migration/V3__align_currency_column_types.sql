-- Align fixed-width currency columns with the VARCHAR(3) JPA mappings.
-- This is a forward-only migration; existing migration history is preserved.
ALTER TABLE campaigns ALTER COLUMN currency TYPE VARCHAR(3);
ALTER TABLE opportunities ALTER COLUMN currency TYPE VARCHAR(3);
ALTER TABLE orders ALTER COLUMN currency TYPE VARCHAR(3);
ALTER TABLE cost_records ALTER COLUMN currency TYPE VARCHAR(3);
