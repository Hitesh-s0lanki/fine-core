-- Raw financial ingestion has an auditable result but intentionally creates no valuation snapshot.
ALTER TABLE ingestion_job ADD COLUMN result jsonb;
