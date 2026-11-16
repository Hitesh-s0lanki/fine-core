-- Operational state is mutable and deliberately separate from immutable domain evidence.
CREATE TABLE ingestion_target (
 id uuid PRIMARY KEY,
 binding_id uuid NOT NULL REFERENCES provider_entity_binding(id),
 endpoint_id uuid NOT NULL REFERENCES source_endpoint(id),
 config jsonb NOT NULL,
 refresh_seconds integer CHECK (refresh_seconds >= 60),
 next_run_at timestamptz NOT NULL DEFAULT now(),
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE ingestion_job (
 id uuid PRIMARY KEY,
 target_id uuid NOT NULL REFERENCES ingestion_target(id),
 status text NOT NULL DEFAULT 'queued' CHECK (status IN ('queued','running','succeeded','failed')),
 attempt integer NOT NULL DEFAULT 0 CHECK (attempt BETWEEN 0 AND 3),
 available_at timestamptz NOT NULL DEFAULT now(),
 lease_token uuid,
 lease_until timestamptz,
 snapshot_id uuid REFERENCES company_snapshot(id),
 error_code text,
 created_at timestamptz NOT NULL DEFAULT now(),
 finished_at timestamptz,
 CHECK ((status = 'running') = (lease_until IS NOT NULL)),
 CHECK (status <> 'running' OR lease_token IS NOT NULL)
);
CREATE UNIQUE INDEX ingestion_one_active_target ON ingestion_job(target_id)
 WHERE status IN ('queued','running');
CREATE INDEX ingestion_ready_jobs ON ingestion_job(available_at, created_at)
 WHERE status IN ('queued','running');
