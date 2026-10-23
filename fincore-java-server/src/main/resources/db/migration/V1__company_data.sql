-- Company data contract v1. Financial decimals allow 42 integer and 18 fractional digits.

CREATE TABLE data_record (
 id uuid PRIMARY KEY,
 record_type varchar(80) NOT NULL,
 schema_version integer NOT NULL CHECK (schema_version = 1),
 created_at timestamp with time zone NOT NULL,
 creation_transaction bigint NOT NULL DEFAULT txid_current()
);

CREATE TABLE company (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 slug text NOT NULL,
 display_name text NOT NULL,
 entity_kind text NOT NULL CHECK (entity_kind IN ('issuer','subsidiary','other','unknown')),
 UNIQUE NULLS NOT DISTINCT (slug)
);

CREATE TABLE source_provider (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 code text NOT NULL,
 name text NOT NULL,
 kind text NOT NULL CHECK (kind IN ('regulator','exchange','vendor','macro','manual')),
 base_url text,
 UNIQUE NULLS NOT DISTINCT (code)
);

CREATE TABLE source_endpoint (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 provider_id uuid NOT NULL,
 code text NOT NULL,
 contract_version text NOT NULL,
 documentation_url text,
 auth_scheme text NOT NULL CHECK (auth_scheme IN ('none','api_key','basic','other')),
 credential_reference text,
 supported_domains jsonb NOT NULL,
 pagination_kind text NOT NULL CHECK (pagination_kind IN ('none','offset','page','cursor','archive_files','unknown')),
 access_status text NOT NULL CHECK (access_status IN ('unknown','available','auth_required','blocked')),
 checked_at timestamp with time zone NOT NULL,
 UNIQUE NULLS NOT DISTINCT (provider_id, code, contract_version)
);

CREATE TABLE provider_entity_binding (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 provider_id uuid NOT NULL,
 company_id uuid,
 security_id uuid,
 listing_id uuid,
 source_identifier text NOT NULL,
 identifier_kind text NOT NULL,
 verification_status text NOT NULL CHECK (verification_status IN ('configured','verified','rejected')),
 verified_at timestamp with time zone,
 valid_from date,
 valid_to date,
 CHECK (num_nonnulls(company_id,security_id,listing_id) = 1),
 CHECK (valid_from IS NULL OR valid_to IS NULL OR valid_from <= valid_to)
);

CREATE TABLE raw_artifact (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 storage_uri text NOT NULL,
 sha256 text NOT NULL,
 hash_basis text NOT NULL CHECK (hash_basis IN ('response_bytes','stored_bytes')),
 content_type text NOT NULL,
 byte_length bigint NOT NULL,
 format text NOT NULL CHECK (format IN ('json','html','csv','pdf','xbrl','text')),
 stored_at timestamp with time zone NOT NULL,
 original_response_sha256 text,
 CHECK (byte_length >= 0),
 UNIQUE NULLS NOT DISTINCT (sha256, storage_uri)
);

CREATE TABLE ingestion_attempt (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 provider_id uuid NOT NULL,
 source_endpoint_id uuid NOT NULL,
 provider_entity_binding_id uuid,
 company_id uuid,
 endpoint text NOT NULL,
 request_url_redacted text NOT NULL,
 requested_at timestamp with time zone NOT NULL,
 retrieved_at timestamp with time zone,
 http_status integer,
 outcome text NOT NULL CHECK (outcome IN ('succeeded','authentication_required','forbidden','rate_limited','failed','not_attempted')),
 error_code text,
 error_message text,
 artifact_id uuid,
 request_scope jsonb NOT NULL,
 next_cursor text,
 pagination_complete boolean,
 CHECK (http_status IS NULL OR http_status BETWEEN 100 AND 599),
 CHECK (requested_at IS NULL OR retrieved_at IS NULL OR requested_at <= retrieved_at)
);

CREATE TABLE normalization_run (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 ingestion_attempt_id uuid NOT NULL,
 adapter_id text NOT NULL,
 adapter_version text NOT NULL,
 adapter_content_hash text NOT NULL,
 canonical_schema_version integer NOT NULL,
 mapping_version text NOT NULL,
 started_at timestamp with time zone NOT NULL,
 finished_at timestamp with time zone,
 status text NOT NULL CHECK (status IN ('succeeded','partial','failed')),
 output_record_ids jsonb NOT NULL,
 quality_issue_ids jsonb NOT NULL,
 CHECK (started_at IS NULL OR finished_at IS NULL OR started_at <= finished_at)
);

CREATE TABLE source_reference (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 ingestion_attempt_id uuid,
 artifact_id uuid,
 normalization_run_id uuid,
 source_url text,
 locator text,
 extraction_method text NOT NULL CHECK (extraction_method IN ('api','html_extract','csv_conversion','document_parse','configured','manual')),
 extractor_version text,
 observed_at timestamp with time zone NOT NULL,
 available_at timestamp with time zone,
 availability_precision text CHECK (availability_precision IN ('timestamp','date','unknown')),
 availability_basis text,
 CHECK (extraction_method IN ('configured','manual') OR num_nonnulls(ingestion_attempt_id,artifact_id,source_url) > 0)
);

CREATE TABLE company_identifier (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 company_id uuid NOT NULL,
 scheme text NOT NULL CHECK (scheme IN ('cik','lei','cin','ein','provider_company_id','other')),
 authority text NOT NULL,
 value text NOT NULL,
 valid_from date,
 valid_to date,
 CHECK (valid_from IS NULL OR valid_to IS NULL OR valid_from <= valid_to)
);

CREATE TABLE company_profile_observation (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 company_id uuid NOT NULL,
 field text NOT NULL CHECK (field IN ('legal_name','trading_name','former_name','description','legal_form','phone','domicile_country','incorporation_country','website','investor_relations_url','address','industry_classification','fiscal_year_end','reporting_currency','sector_classification','employee_count','logo_url','executive_summary')),
 profile_value jsonb NOT NULL,
 effective_from date,
 effective_to date,
 CHECK (effective_from IS NULL OR effective_to IS NULL OR effective_from <= effective_to)
);

CREATE TABLE company_registration (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 company_id uuid NOT NULL,
 registry text NOT NULL,
 registry_jurisdiction text NOT NULL,
 registration_number text NOT NULL,
 registration_kind text NOT NULL CHECK (registration_kind IN ('domestic','overseas','branch','unknown')),
 status_raw text,
 status_normalized text,
 legal_form text,
 incorporation_date date,
 establishment_date date,
 home_jurisdiction text,
 parent_registry text,
 governing_law text,
 objects text,
 effective_from date,
 effective_to date,
 CHECK (effective_from IS NULL OR effective_to IS NULL OR effective_from <= effective_to)
);

CREATE TABLE company_relationship (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 subject_company_id uuid NOT NULL,
 object_company_id uuid NOT NULL,
 relationship_type text NOT NULL CHECK (relationship_type IN ('subsidiary','associate','joint_venture','parent','other')),
 ownership_percent numeric(60,18),
 effective_from date,
 effective_to date,
 CHECK (subject_company_id <> object_company_id),
 CHECK (ownership_percent IS NULL OR ownership_percent BETWEEN 0 AND 100),
 CHECK (effective_from IS NULL OR effective_to IS NULL OR effective_from <= effective_to)
);

CREATE TABLE security (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 company_id uuid NOT NULL,
 security_type text NOT NULL CHECK (security_type IN ('ordinary_share','adr','preferred_share','bond','other','unknown')),
 share_class text,
 name text,
 is_fund boolean,
 is_etf boolean
);

CREATE TABLE security_identifier (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 security_id uuid NOT NULL,
 scheme text NOT NULL CHECK (scheme IN ('isin','cusip','sedol','provider_security_id','other')),
 authority text NOT NULL,
 value text NOT NULL,
 valid_from date,
 valid_to date,
 CHECK (valid_from IS NULL OR valid_to IS NULL OR valid_from <= valid_to)
);

CREATE TABLE listing (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 security_id uuid NOT NULL,
 exchange_code text NOT NULL,
 exchange_mic text,
 symbol text NOT NULL,
 trading_currency text,
 status text NOT NULL CHECK (status IN ('active','inactive','delisted','unknown')),
 first_trade_date date,
 valid_from date,
 valid_to date,
 CHECK (valid_from IS NULL OR valid_to IS NULL OR valid_from <= valid_to)
);

CREATE TABLE depositary_receipt_ratio (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 receipt_security_id uuid NOT NULL,
 underlying_security_id uuid NOT NULL,
 receipt_units numeric(60,18) NOT NULL,
 underlying_units numeric(60,18) NOT NULL,
 effective_from date NOT NULL,
 effective_to date,
 CHECK (receipt_security_id <> underlying_security_id),
 CHECK (receipt_units > 0 AND underlying_units > 0),
 CHECK (effective_from IS NULL OR effective_to IS NULL OR effective_from <= effective_to)
);

CREATE TABLE filing (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 company_id uuid NOT NULL,
 provider_id uuid NOT NULL,
 source_filing_id text NOT NULL,
 form_type text,
 filing_date date,
 accepted_at timestamp with time zone,
 report_date date,
 title text,
 source_url text,
 is_amendment boolean,
 amends_filing_id uuid,
 xbrl_status text NOT NULL CHECK (xbrl_status IN ('yes','no','unknown')),
 UNIQUE NULLS NOT DISTINCT (provider_id, company_id, source_filing_id)
);

CREATE TABLE financial_result_disclosure (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 company_id uuid NOT NULL,
 filing_id uuid NOT NULL,
 source_record_id text NOT NULL,
 period_start date,
 period_end date,
 period_label_raw text,
 fiscal_year_label_raw text,
 scope text NOT NULL CHECK (scope IN ('consolidated','standalone','unspecified')),
 audit_status text NOT NULL CHECK (audit_status IN ('audited','unaudited','reviewed','unknown')),
 accounting_standard text,
 cumulative boolean,
 broadcast_at timestamp with time zone,
 CHECK (period_start IS NULL OR period_end IS NULL OR period_start <= period_end),
 UNIQUE NULLS NOT DISTINCT (filing_id, source_record_id)
);

CREATE TABLE announcement (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 company_id uuid NOT NULL,
 listing_id uuid,
 provider_id uuid NOT NULL,
 source_record_id text NOT NULL,
 category_raw text,
 title text NOT NULL,
 body text,
 published_at timestamp with time zone,
 published_at_raw text NOT NULL,
 timestamp_timezone text,
 disseminated_at timestamp with time zone,
 UNIQUE NULLS NOT DISTINCT (provider_id, company_id, source_record_id)
);

CREATE TABLE document (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 url text NOT NULL,
 title text,
 published_at timestamp with time zone,
 language text,
 media_type text,
 download_status text NOT NULL CHECK (download_status IN ('not_requested','downloaded','failed')),
 artifact_id uuid,
 parse_status text NOT NULL CHECK (parse_status IN ('not_requested','parsed','failed')),
 parser_version text,
 CHECK (download_status <> 'downloaded' OR artifact_id IS NOT NULL),
 CHECK (parse_status <> 'parsed' OR (artifact_id IS NOT NULL AND parser_version IS NOT NULL))
);

CREATE TABLE document_link (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 document_id uuid NOT NULL,
 company_id uuid,
 filing_id uuid,
 announcement_id uuid,
 role text NOT NULL CHECK (role IN ('primary','attachment','xbrl','annual_report','presentation','investor_relations','other')),
 CHECK (num_nonnulls(company_id,filing_id,announcement_id) = 1)
);

CREATE TABLE registry_filing_detail (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 filing_id uuid NOT NULL,
 registration_id uuid NOT NULL,
 transaction_id text NOT NULL,
 category text,
 filing_type text,
 description_code text,
 description_text text,
 description_values jsonb NOT NULL,
 action_date date,
 pages integer,
 CHECK (pages IS NULL OR pages >= 0),
 UNIQUE NULLS NOT DISTINCT (registration_id, transaction_id)
);

CREATE TABLE registry_compliance_observation (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 registration_id uuid NOT NULL,
 as_of_date date,
 dissolution_date date,
 accounts_last_made_up_to date,
 accounts_next_due date,
 accounts_overdue boolean,
 confirmation_statement_last_made_up_to date,
 confirmation_statement_next_due date,
 confirmation_statement_overdue boolean,
 partial_data_available text
);

CREATE TABLE financial_concept (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 taxonomy text NOT NULL,
 taxonomy_version text,
 concept_name text NOT NULL,
 label text,
 description text,
 period_type text NOT NULL CHECK (period_type IN ('instant','duration','unknown')),
 UNIQUE NULLS NOT DISTINCT (taxonomy, taxonomy_version, concept_name)
);

CREATE TABLE metric_definition (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 code text NOT NULL,
 label text NOT NULL,
 statement text NOT NULL CHECK (statement IN ('income','balance','cash_flow','per_share','other')),
 period_type text NOT NULL CHECK (period_type IN ('instant','duration','unknown')),
 unit_kind text NOT NULL,
 definition text NOT NULL,
 sign_convention text NOT NULL,
 UNIQUE NULLS NOT DISTINCT (code)
);

CREATE TABLE concept_metric_mapping (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 concept_id uuid NOT NULL,
 metric_id uuid NOT NULL,
 company_id uuid,
 scope_constraint text CHECK (scope_constraint IN ('consolidated','standalone','unspecified')),
 accounting_standard text,
 mapping_version text NOT NULL,
 semantic_notes text NOT NULL,
 transform_method text,
 valid_from date,
 valid_to date,
 CHECK (valid_from IS NULL OR valid_to IS NULL OR valid_from <= valid_to)
);

CREATE TABLE vendor_metric_mapping (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 provider_id uuid NOT NULL,
 source_endpoint_id uuid NOT NULL,
 source_field text NOT NULL,
 metric_id uuid NOT NULL,
 mapping_version text NOT NULL,
 definition_status text NOT NULL CHECK (definition_status IN ('verified','ambiguous')),
 semantic_notes text NOT NULL,
 transform_method text,
 UNIQUE NULLS NOT DISTINCT (provider_id, source_endpoint_id, source_field, mapping_version)
);

CREATE TABLE financial_observation (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 company_id uuid NOT NULL,
 concept_id uuid NOT NULL,
 filing_id uuid NOT NULL,
 security_id uuid,
 period_type text NOT NULL CHECK (period_type IN ('instant','duration','unknown')),
 period_start date,
 period_end date NOT NULL,
 source_fiscal_year integer,
 source_fiscal_period text,
 source_frame text,
 scope text NOT NULL CHECK (scope IN ('consolidated','standalone','unspecified')),
 accounting_standard text,
 dimensions jsonb NOT NULL,
 dimensions_status text NOT NULL CHECK (dimensions_status IN ('reported','not_reported')),
 value numeric(60,18) NOT NULL,
 unit text NOT NULL,
 scale integer NOT NULL,
 reported_decimals text,
 reported_value_raw text NOT NULL,
 filed_on date,
 accepted_at timestamp with time zone,
 context_id text,
 entity_identifier text,
 assertion_key text NOT NULL,
 CHECK (period_type <> 'instant' OR period_start IS NULL),
 CHECK (period_type <> 'duration' OR period_start IS NOT NULL),
 CHECK (period_start IS NULL OR period_end IS NULL OR period_start <= period_end),
 UNIQUE NULLS NOT DISTINCT (assertion_key)
);

CREATE TABLE vendor_financial_observation (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 company_id uuid NOT NULL,
 security_id uuid,
 listing_id uuid,
 provider_id uuid NOT NULL,
 source_endpoint_id uuid NOT NULL,
 source_record_id text NOT NULL,
 source_field text NOT NULL,
 metric_id uuid,
 period_start date,
 period_end date NOT NULL,
 period_type text NOT NULL CHECK (period_type IN ('instant','duration','unknown')),
 period_label_raw text,
 reported_currency text,
 unit text NOT NULL,
 value numeric(60,18) NOT NULL,
 scale integer NOT NULL,
 reported_value_raw text NOT NULL,
 scope text NOT NULL CHECK (scope IN ('consolidated','standalone','unspecified')),
 source_filing_id text,
 filing_id uuid,
 filed_on date,
 accepted_at timestamp with time zone,
 methodology_status text NOT NULL CHECK (methodology_status IN ('reported_by_vendor','vendor_calculated','unknown')),
 mapping_version text,
 assertion_key text NOT NULL,
 CHECK (period_start IS NULL OR period_end IS NULL OR period_start <= period_end),
 UNIQUE NULLS NOT DISTINCT (assertion_key)
);

CREATE TABLE derived_metric_observation (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 company_id uuid NOT NULL,
 metric_id uuid NOT NULL,
 snapshot_id uuid NOT NULL,
 security_id uuid,
 dimensions jsonb NOT NULL,
 period_start date,
 period_end date NOT NULL,
 scope text NOT NULL CHECK (scope IN ('consolidated','standalone','unspecified')),
 value numeric(60,18) NOT NULL,
 unit text NOT NULL,
 scale integer NOT NULL,
 input_record_refs jsonb NOT NULL,
 method text NOT NULL,
 method_version text NOT NULL,
 computed_at timestamp with time zone NOT NULL,
 CHECK (period_start IS NULL OR period_end IS NULL OR period_start <= period_end)
);

CREATE TABLE market_quote (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 listing_id uuid NOT NULL,
 as_of timestamp with time zone NOT NULL,
 currency text NOT NULL,
 last_price numeric(60,18),
 bid numeric(60,18),
 ask numeric(60,18),
 volume numeric(60,18),
 market_status text,
 delay_seconds integer,
 CHECK (volume IS NULL OR volume >= 0),
 CHECK (delay_seconds IS NULL OR delay_seconds >= 0),
 CHECK (num_nonnulls(last_price,bid,ask) > 0)
);

CREATE TABLE price_bar (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 listing_id uuid NOT NULL,
 interval text NOT NULL CHECK (interval IN ('daily','minute','hour','week','month')),
 trading_date date,
 exchange_timezone text NOT NULL,
 period_start timestamp with time zone,
 period_end timestamp with time zone,
 currency text NOT NULL,
 open numeric(60,18) NOT NULL,
 high numeric(60,18) NOT NULL,
 low numeric(60,18) NOT NULL,
 close numeric(60,18) NOT NULL,
 volume numeric(60,18),
 adjustment_basis text NOT NULL CHECK (adjustment_basis IN ('raw','split','total_return','unknown')),
 adjustment_version text,
 adjusted_close numeric(60,18),
 adjusted_close_basis text,
 CHECK (high >= low AND open BETWEEN low AND high AND close BETWEEN low AND high),
 CHECK (volume IS NULL OR volume >= 0),
 CHECK ((interval = 'daily' AND trading_date IS NOT NULL) OR (interval <> 'daily' AND period_start IS NOT NULL AND period_end IS NOT NULL)),
 CHECK (period_start IS NULL OR period_end IS NULL OR period_start <= period_end)
);

CREATE TABLE market_metric_observation (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 company_id uuid NOT NULL,
 security_id uuid,
 listing_id uuid,
 metric_code text NOT NULL CHECK (metric_code IN ('MARKET_CAP','ENTERPRISE_VALUE','BETA','AVERAGE_VOLUME','DIVIDEND_PER_SHARE','DIVIDEND_YIELD','PRICE_CHANGE','PRICE_CHANGE_PERCENT','PRICE_RANGE_LOW','PRICE_RANGE_HIGH','OTHER')),
 metric_definition text NOT NULL,
 value numeric(60,18) NOT NULL,
 unit text NOT NULL,
 currency text,
 as_of timestamp with time zone,
 reference_period_start date,
 reference_period_end date,
 methodology text,
 input_record_refs jsonb NOT NULL,
 CHECK (reference_period_start IS NULL OR reference_period_end IS NULL OR reference_period_start <= reference_period_end)
);

CREATE TABLE corporate_action (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 security_id uuid NOT NULL,
 action_type text NOT NULL CHECK (action_type IN ('split','dividend','buyback','rights','other')),
 announced_at timestamp with time zone,
 ex_date date,
 record_date date,
 payment_date date,
 amount numeric(60,18),
 currency text,
 ratio_numerator numeric(60,18),
 ratio_denominator numeric(60,18),
 CHECK (ratio_denominator IS NULL OR ratio_denominator > 0),
 CHECK (ratio_numerator IS NULL OR ratio_numerator > 0)
);

CREATE TABLE fx_observation (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 base_currency text NOT NULL,
 quote_currency text NOT NULL,
 observed_at timestamp with time zone NOT NULL,
 rate numeric(60,18) NOT NULL,
 rate_kind text NOT NULL CHECK (rate_kind IN ('spot','period_average','period_end')),
 period_start date,
 period_end date,
 CHECK (rate > 0),
 CHECK (base_currency <> quote_currency),
 CHECK (period_start IS NULL OR period_end IS NULL OR period_start <= period_end)
);

CREATE TABLE macro_series (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 provider_id uuid NOT NULL,
 source_series_id text NOT NULL,
 name text NOT NULL,
 geography text NOT NULL,
 unit text NOT NULL,
 frequency text NOT NULL,
 seasonal_adjustment text,
 transformation text NOT NULL,
 unit_multiplier integer,
 notes text,
 last_updated_at timestamp with time zone,
 UNIQUE NULLS NOT DISTINCT (provider_id, source_series_id, transformation)
);

CREATE TABLE macro_observation (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 series_id uuid NOT NULL,
 observation_date date NOT NULL,
 value numeric(60,18),
 missing_reason text,
 vintage_date date,
 vintage_basis text NOT NULL CHECK (vintage_basis IN ('current_download','historical_vintage')),
 released_at timestamp with time zone,
 realtime_start date,
 realtime_end date,
 CHECK ((value IS NULL) = (missing_reason IS NOT NULL)),
 CHECK (vintage_basis <> 'historical_vintage' OR vintage_date IS NOT NULL),
 CHECK (realtime_start IS NULL OR realtime_end IS NULL OR realtime_start <= realtime_end)
);

CREATE TABLE coverage_item (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 company_id uuid,
 provider_id uuid,
 domain text NOT NULL,
 endpoint text,
 metric_id uuid,
 period_start date,
 period_end date,
 scope text CHECK (scope IN ('consolidated','standalone','unspecified')),
 unit text,
 checked_at timestamp with time zone NOT NULL,
 status text NOT NULL CHECK (status IN ('available','partial','missing','blocked','not_attempted','not_applicable')),
 reason_code text,
 message text,
 ingestion_attempt_ids jsonb NOT NULL,
 latest_observation_date date,
 expected_through date,
 freshness_policy_version text,
 CHECK (period_start IS NULL OR period_end IS NULL OR period_start <= period_end)
);

CREATE TABLE data_quality_issue (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 company_id uuid,
 record_type text NOT NULL,
 record_id uuid,
 code text NOT NULL,
 severity text NOT NULL CHECK (severity IN ('info','warning','error')),
 message text NOT NULL,
 detected_at timestamp with time zone NOT NULL,
 rule_version text NOT NULL,
 resolution text
);

CREATE TABLE company_snapshot (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 company_id uuid NOT NULL,
 cutoff_at timestamp with time zone NOT NULL,
 knowledge_basis text NOT NULL CHECK (knowledge_basis IN ('collected_by_cutoff','published_by_cutoff','current_vintage')),
 selection_policy_version text NOT NULL,
 mapping_version text NOT NULL,
 manifest_sha256 text NOT NULL,
 status text NOT NULL CHECK (status IN ('complete','partial','blocked')),
 creation_transaction bigint NOT NULL DEFAULT txid_current()
);

CREATE TABLE snapshot_member (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 snapshot_id uuid NOT NULL,
 record_id uuid NOT NULL,
 role text NOT NULL,
 selection_reason text NOT NULL,
 UNIQUE NULLS NOT DISTINCT (snapshot_id, record_id, role)
);

CREATE TABLE snapshot_financial_cell (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 snapshot_id uuid NOT NULL,
 metric_id uuid NOT NULL,
 period_start date,
 period_end date NOT NULL,
 scope text NOT NULL CHECK (scope IN ('consolidated','standalone','unspecified')),
 unit text NOT NULL,
 security_id uuid,
 dimensions jsonb NOT NULL,
 financial_observation_id uuid,
 vendor_financial_observation_id uuid,
 derived_metric_observation_id uuid,
 missing_reason text,
 cell_key text NOT NULL,
 CHECK (num_nonnulls(financial_observation_id,vendor_financial_observation_id,derived_metric_observation_id) <= 1),
 CHECK ((num_nonnulls(financial_observation_id,vendor_financial_observation_id,derived_metric_observation_id) = 0) = (missing_reason IS NOT NULL)),
 CHECK (period_start IS NULL OR period_end IS NULL OR period_start <= period_end),
 UNIQUE NULLS NOT DISTINCT (snapshot_id, cell_key)
);

CREATE TABLE officer_party (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 party_kind text NOT NULL CHECK (party_kind IN ('person','corporate','unknown')),
 display_name text NOT NULL,
 source_officer_id text,
 provider_id uuid NOT NULL,
 identifiers jsonb NOT NULL
);

CREATE TABLE officer_appointment (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 company_id uuid NOT NULL,
 registration_id uuid NOT NULL,
 officer_party_id uuid NOT NULL,
 source_appointment_id text NOT NULL,
 role text NOT NULL,
 service_address jsonb NOT NULL,
 appointed_on date,
 resigned_on date,
 is_pre1992_appointment boolean,
 birth_month integer,
 birth_year integer,
 CHECK (birth_month IS NULL OR birth_month BETWEEN 1 AND 12),
 CHECK (appointed_on IS NULL OR resigned_on IS NULL OR appointed_on <= resigned_on),
 UNIQUE NULLS NOT DISTINCT (registration_id, source_appointment_id)
);

CREATE TABLE ownership_observation (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 company_id uuid NOT NULL,
 security_id uuid,
 holder_name text NOT NULL,
 holder_company_id uuid,
 as_of_date date NOT NULL,
 shares numeric(60,18),
 ownership_percent numeric(60,18),
 voting_percent numeric(60,18),
 CHECK (shares IS NULL OR shares >= 0),
 CHECK (ownership_percent IS NULL OR ownership_percent BETWEEN 0 AND 100),
 CHECK (voting_percent IS NULL OR voting_percent BETWEEN 0 AND 100)
);

CREATE TABLE debt_instrument (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 company_id uuid NOT NULL,
 instrument_identifier text NOT NULL,
 currency text NOT NULL,
 principal numeric(60,18),
 rate_terms text,
 maturity date,
 security_seniority text,
 as_of_date date NOT NULL,
 filing_id uuid
);

CREATE TABLE credit_rating_observation (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 company_id uuid NOT NULL,
 debt_instrument_id uuid,
 agency text NOT NULL,
 rating_scale text NOT NULL,
 rating text NOT NULL,
 outlook text,
 effective_date date NOT NULL
);

CREATE TABLE research_disclosure (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 company_id uuid NOT NULL,
 filing_id uuid,
 document_id uuid NOT NULL,
 topic text NOT NULL CHECK (topic IN ('risk','litigation','strategy','other')),
 extracted_text text NOT NULL,
 page_section text NOT NULL,
 extraction_version text NOT NULL
);

CREATE TABLE reference_dataset (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 name text NOT NULL,
 publisher text NOT NULL,
 dataset_date date,
 downloaded_at timestamp with time zone NOT NULL,
 artifact_id uuid NOT NULL,
 methodology_url text,
 version text NOT NULL
);

CREATE TABLE reference_dataset_observation (
 id uuid PRIMARY KEY REFERENCES data_record(id),
 dataset_id uuid NOT NULL,
 geography text,
 industry_scheme text,
 industry_label text,
 metric_code text NOT NULL,
 value numeric(60,18) NOT NULL,
 unit text NOT NULL,
 sample_size integer,
 methodology_notes text,
 as_of_date date,
 CHECK (sample_size IS NULL OR sample_size >= 0)
);

ALTER TABLE source_endpoint ADD FOREIGN KEY (provider_id) REFERENCES source_provider(id);

CREATE INDEX ON source_endpoint (provider_id);

ALTER TABLE provider_entity_binding ADD FOREIGN KEY (provider_id) REFERENCES source_provider(id);

CREATE INDEX ON provider_entity_binding (provider_id);

ALTER TABLE provider_entity_binding ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON provider_entity_binding (company_id);

ALTER TABLE provider_entity_binding ADD FOREIGN KEY (security_id) REFERENCES security(id);

CREATE INDEX ON provider_entity_binding (security_id);

ALTER TABLE provider_entity_binding ADD FOREIGN KEY (listing_id) REFERENCES listing(id);

CREATE INDEX ON provider_entity_binding (listing_id);

ALTER TABLE ingestion_attempt ADD FOREIGN KEY (provider_id) REFERENCES source_provider(id);

CREATE INDEX ON ingestion_attempt (provider_id);

ALTER TABLE ingestion_attempt ADD FOREIGN KEY (source_endpoint_id) REFERENCES source_endpoint(id);

CREATE INDEX ON ingestion_attempt (source_endpoint_id);

ALTER TABLE ingestion_attempt ADD FOREIGN KEY (provider_entity_binding_id) REFERENCES provider_entity_binding(id);

CREATE INDEX ON ingestion_attempt (provider_entity_binding_id);

ALTER TABLE ingestion_attempt ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON ingestion_attempt (company_id);

ALTER TABLE ingestion_attempt ADD FOREIGN KEY (artifact_id) REFERENCES raw_artifact(id);

CREATE INDEX ON ingestion_attempt (artifact_id);

ALTER TABLE normalization_run ADD FOREIGN KEY (ingestion_attempt_id) REFERENCES ingestion_attempt(id);

CREATE INDEX ON normalization_run (ingestion_attempt_id);

ALTER TABLE source_reference ADD FOREIGN KEY (ingestion_attempt_id) REFERENCES ingestion_attempt(id);

CREATE INDEX ON source_reference (ingestion_attempt_id);

ALTER TABLE source_reference ADD FOREIGN KEY (artifact_id) REFERENCES raw_artifact(id);

CREATE INDEX ON source_reference (artifact_id);

ALTER TABLE source_reference ADD FOREIGN KEY (normalization_run_id) REFERENCES normalization_run(id);

CREATE INDEX ON source_reference (normalization_run_id);

ALTER TABLE company_identifier ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON company_identifier (company_id);

ALTER TABLE company_profile_observation ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON company_profile_observation (company_id);

ALTER TABLE company_registration ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON company_registration (company_id);

ALTER TABLE company_relationship ADD FOREIGN KEY (subject_company_id) REFERENCES company(id);

CREATE INDEX ON company_relationship (subject_company_id);

ALTER TABLE company_relationship ADD FOREIGN KEY (object_company_id) REFERENCES company(id);

CREATE INDEX ON company_relationship (object_company_id);

ALTER TABLE security ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON security (company_id);

ALTER TABLE security_identifier ADD FOREIGN KEY (security_id) REFERENCES security(id);

CREATE INDEX ON security_identifier (security_id);

ALTER TABLE listing ADD FOREIGN KEY (security_id) REFERENCES security(id);

CREATE INDEX ON listing (security_id);

ALTER TABLE depositary_receipt_ratio ADD FOREIGN KEY (receipt_security_id) REFERENCES security(id);

CREATE INDEX ON depositary_receipt_ratio (receipt_security_id);

ALTER TABLE depositary_receipt_ratio ADD FOREIGN KEY (underlying_security_id) REFERENCES security(id);

CREATE INDEX ON depositary_receipt_ratio (underlying_security_id);

ALTER TABLE filing ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON filing (company_id);

ALTER TABLE filing ADD FOREIGN KEY (provider_id) REFERENCES source_provider(id);

CREATE INDEX ON filing (provider_id);

ALTER TABLE filing ADD FOREIGN KEY (amends_filing_id) REFERENCES filing(id);

CREATE INDEX ON filing (amends_filing_id);

ALTER TABLE financial_result_disclosure ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON financial_result_disclosure (company_id);

ALTER TABLE financial_result_disclosure ADD FOREIGN KEY (filing_id) REFERENCES filing(id);

CREATE INDEX ON financial_result_disclosure (filing_id);

ALTER TABLE announcement ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON announcement (company_id);

ALTER TABLE announcement ADD FOREIGN KEY (listing_id) REFERENCES listing(id);

CREATE INDEX ON announcement (listing_id);

ALTER TABLE announcement ADD FOREIGN KEY (provider_id) REFERENCES source_provider(id);

CREATE INDEX ON announcement (provider_id);

ALTER TABLE document ADD FOREIGN KEY (artifact_id) REFERENCES raw_artifact(id);

CREATE INDEX ON document (artifact_id);

ALTER TABLE document_link ADD FOREIGN KEY (document_id) REFERENCES document(id);

CREATE INDEX ON document_link (document_id);

ALTER TABLE document_link ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON document_link (company_id);

ALTER TABLE document_link ADD FOREIGN KEY (filing_id) REFERENCES filing(id);

CREATE INDEX ON document_link (filing_id);

ALTER TABLE document_link ADD FOREIGN KEY (announcement_id) REFERENCES announcement(id);

CREATE INDEX ON document_link (announcement_id);

ALTER TABLE registry_filing_detail ADD FOREIGN KEY (filing_id) REFERENCES filing(id);

CREATE INDEX ON registry_filing_detail (filing_id);

ALTER TABLE registry_filing_detail ADD FOREIGN KEY (registration_id) REFERENCES company_registration(id);

CREATE INDEX ON registry_filing_detail (registration_id);

ALTER TABLE registry_compliance_observation ADD FOREIGN KEY (registration_id) REFERENCES company_registration(id);

CREATE INDEX ON registry_compliance_observation (registration_id);

ALTER TABLE concept_metric_mapping ADD FOREIGN KEY (concept_id) REFERENCES financial_concept(id);

CREATE INDEX ON concept_metric_mapping (concept_id);

ALTER TABLE concept_metric_mapping ADD FOREIGN KEY (metric_id) REFERENCES metric_definition(id);

CREATE INDEX ON concept_metric_mapping (metric_id);

ALTER TABLE concept_metric_mapping ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON concept_metric_mapping (company_id);

ALTER TABLE vendor_metric_mapping ADD FOREIGN KEY (provider_id) REFERENCES source_provider(id);

CREATE INDEX ON vendor_metric_mapping (provider_id);

ALTER TABLE vendor_metric_mapping ADD FOREIGN KEY (source_endpoint_id) REFERENCES source_endpoint(id);

CREATE INDEX ON vendor_metric_mapping (source_endpoint_id);

ALTER TABLE vendor_metric_mapping ADD FOREIGN KEY (metric_id) REFERENCES metric_definition(id);

CREATE INDEX ON vendor_metric_mapping (metric_id);

ALTER TABLE financial_observation ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON financial_observation (company_id);

ALTER TABLE financial_observation ADD FOREIGN KEY (concept_id) REFERENCES financial_concept(id);

CREATE INDEX ON financial_observation (concept_id);

ALTER TABLE financial_observation ADD FOREIGN KEY (filing_id) REFERENCES filing(id);

CREATE INDEX ON financial_observation (filing_id);

ALTER TABLE financial_observation ADD FOREIGN KEY (security_id) REFERENCES security(id);

CREATE INDEX ON financial_observation (security_id);

ALTER TABLE vendor_financial_observation ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON vendor_financial_observation (company_id);

ALTER TABLE vendor_financial_observation ADD FOREIGN KEY (security_id) REFERENCES security(id);

CREATE INDEX ON vendor_financial_observation (security_id);

ALTER TABLE vendor_financial_observation ADD FOREIGN KEY (listing_id) REFERENCES listing(id);

CREATE INDEX ON vendor_financial_observation (listing_id);

ALTER TABLE vendor_financial_observation ADD FOREIGN KEY (provider_id) REFERENCES source_provider(id);

CREATE INDEX ON vendor_financial_observation (provider_id);

ALTER TABLE vendor_financial_observation ADD FOREIGN KEY (source_endpoint_id) REFERENCES source_endpoint(id);

CREATE INDEX ON vendor_financial_observation (source_endpoint_id);

ALTER TABLE vendor_financial_observation ADD FOREIGN KEY (metric_id) REFERENCES metric_definition(id);

CREATE INDEX ON vendor_financial_observation (metric_id);

ALTER TABLE vendor_financial_observation ADD FOREIGN KEY (filing_id) REFERENCES filing(id);

CREATE INDEX ON vendor_financial_observation (filing_id);

ALTER TABLE derived_metric_observation ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON derived_metric_observation (company_id);

ALTER TABLE derived_metric_observation ADD FOREIGN KEY (metric_id) REFERENCES metric_definition(id);

CREATE INDEX ON derived_metric_observation (metric_id);

ALTER TABLE derived_metric_observation ADD FOREIGN KEY (snapshot_id) REFERENCES company_snapshot(id);

CREATE INDEX ON derived_metric_observation (snapshot_id);

ALTER TABLE derived_metric_observation ADD FOREIGN KEY (security_id) REFERENCES security(id);

CREATE INDEX ON derived_metric_observation (security_id);

ALTER TABLE market_quote ADD FOREIGN KEY (listing_id) REFERENCES listing(id);

CREATE INDEX ON market_quote (listing_id);

ALTER TABLE price_bar ADD FOREIGN KEY (listing_id) REFERENCES listing(id);

CREATE INDEX ON price_bar (listing_id);

ALTER TABLE market_metric_observation ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON market_metric_observation (company_id);

ALTER TABLE market_metric_observation ADD FOREIGN KEY (security_id) REFERENCES security(id);

CREATE INDEX ON market_metric_observation (security_id);

ALTER TABLE market_metric_observation ADD FOREIGN KEY (listing_id) REFERENCES listing(id);

CREATE INDEX ON market_metric_observation (listing_id);

ALTER TABLE corporate_action ADD FOREIGN KEY (security_id) REFERENCES security(id);

CREATE INDEX ON corporate_action (security_id);

ALTER TABLE macro_series ADD FOREIGN KEY (provider_id) REFERENCES source_provider(id);

CREATE INDEX ON macro_series (provider_id);

ALTER TABLE macro_observation ADD FOREIGN KEY (series_id) REFERENCES macro_series(id);

CREATE INDEX ON macro_observation (series_id);

ALTER TABLE coverage_item ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON coverage_item (company_id);

ALTER TABLE coverage_item ADD FOREIGN KEY (provider_id) REFERENCES source_provider(id);

CREATE INDEX ON coverage_item (provider_id);

ALTER TABLE coverage_item ADD FOREIGN KEY (metric_id) REFERENCES metric_definition(id);

CREATE INDEX ON coverage_item (metric_id);

ALTER TABLE data_quality_issue ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON data_quality_issue (company_id);

ALTER TABLE company_snapshot ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON company_snapshot (company_id);

ALTER TABLE snapshot_member ADD FOREIGN KEY (snapshot_id) REFERENCES company_snapshot(id);

CREATE INDEX ON snapshot_member (snapshot_id);

ALTER TABLE snapshot_member ADD FOREIGN KEY (record_id) REFERENCES data_record(id);

CREATE INDEX ON snapshot_member (record_id);

ALTER TABLE snapshot_financial_cell ADD FOREIGN KEY (snapshot_id) REFERENCES company_snapshot(id);

CREATE INDEX ON snapshot_financial_cell (snapshot_id);

ALTER TABLE snapshot_financial_cell ADD FOREIGN KEY (metric_id) REFERENCES metric_definition(id);

CREATE INDEX ON snapshot_financial_cell (metric_id);

ALTER TABLE snapshot_financial_cell ADD FOREIGN KEY (security_id) REFERENCES security(id);

CREATE INDEX ON snapshot_financial_cell (security_id);

ALTER TABLE snapshot_financial_cell ADD FOREIGN KEY (financial_observation_id) REFERENCES financial_observation(id);

CREATE INDEX ON snapshot_financial_cell (financial_observation_id);

ALTER TABLE snapshot_financial_cell ADD FOREIGN KEY (vendor_financial_observation_id) REFERENCES vendor_financial_observation(id);

CREATE INDEX ON snapshot_financial_cell (vendor_financial_observation_id);

ALTER TABLE snapshot_financial_cell ADD FOREIGN KEY (derived_metric_observation_id) REFERENCES derived_metric_observation(id);

CREATE INDEX ON snapshot_financial_cell (derived_metric_observation_id);

ALTER TABLE officer_party ADD FOREIGN KEY (provider_id) REFERENCES source_provider(id);

CREATE INDEX ON officer_party (provider_id);

ALTER TABLE officer_appointment ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON officer_appointment (company_id);

ALTER TABLE officer_appointment ADD FOREIGN KEY (registration_id) REFERENCES company_registration(id);

CREATE INDEX ON officer_appointment (registration_id);

ALTER TABLE officer_appointment ADD FOREIGN KEY (officer_party_id) REFERENCES officer_party(id);

CREATE INDEX ON officer_appointment (officer_party_id);

ALTER TABLE ownership_observation ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON ownership_observation (company_id);

ALTER TABLE ownership_observation ADD FOREIGN KEY (security_id) REFERENCES security(id);

CREATE INDEX ON ownership_observation (security_id);

ALTER TABLE ownership_observation ADD FOREIGN KEY (holder_company_id) REFERENCES company(id);

CREATE INDEX ON ownership_observation (holder_company_id);

ALTER TABLE debt_instrument ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON debt_instrument (company_id);

ALTER TABLE debt_instrument ADD FOREIGN KEY (filing_id) REFERENCES filing(id);

CREATE INDEX ON debt_instrument (filing_id);

ALTER TABLE credit_rating_observation ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON credit_rating_observation (company_id);

ALTER TABLE credit_rating_observation ADD FOREIGN KEY (debt_instrument_id) REFERENCES debt_instrument(id);

CREATE INDEX ON credit_rating_observation (debt_instrument_id);

ALTER TABLE research_disclosure ADD FOREIGN KEY (company_id) REFERENCES company(id);

CREATE INDEX ON research_disclosure (company_id);

ALTER TABLE research_disclosure ADD FOREIGN KEY (filing_id) REFERENCES filing(id);

CREATE INDEX ON research_disclosure (filing_id);

ALTER TABLE research_disclosure ADD FOREIGN KEY (document_id) REFERENCES document(id);

CREATE INDEX ON research_disclosure (document_id);

ALTER TABLE reference_dataset ADD FOREIGN KEY (artifact_id) REFERENCES raw_artifact(id);

CREATE INDEX ON reference_dataset (artifact_id);

ALTER TABLE reference_dataset_observation ADD FOREIGN KEY (dataset_id) REFERENCES reference_dataset(id);

CREATE INDEX ON reference_dataset_observation (dataset_id);

CREATE TABLE record_evidence (
 record_id uuid NOT NULL REFERENCES data_record(id),
 evidence_id uuid NOT NULL REFERENCES source_reference(id),
 PRIMARY KEY(record_id,evidence_id)
);
CREATE INDEX ON record_evidence(evidence_id);
CREATE INDEX ON data_record(created_at,id);
CREATE EXTENSION IF NOT EXISTS btree_gist;
ALTER TABLE company_identifier ADD CONSTRAINT company_identifier_validity_exclusion
 EXCLUDE USING gist (authority WITH =, scheme WITH =, value WITH =,
 daterange(valid_from, valid_to, '[]') WITH &&);
ALTER TABLE security_identifier ADD CONSTRAINT security_identifier_validity_exclusion
 EXCLUDE USING gist (authority WITH =, scheme WITH =, value WITH =,
 daterange(valid_from, valid_to, '[]') WITH &&);
ALTER TABLE listing ADD CONSTRAINT listing_validity_exclusion
 EXCLUDE USING gist (exchange_code WITH =, symbol WITH =,
 daterange(valid_from, valid_to, '[]') WITH &&);
CREATE FUNCTION reject_record_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 RAISE EXCEPTION 'Company evidence records are append-only' USING ERRCODE = '23514';
END;
$$;
CREATE TRIGGER immutable_data_record BEFORE UPDATE OR DELETE ON data_record
 FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();
CREATE TRIGGER immutable_record_evidence BEFORE UPDATE OR DELETE ON record_evidence
 FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();
CREATE FUNCTION enforce_creation_transaction() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE owner_transaction bigint;
BEGIN
 SELECT creation_transaction INTO owner_transaction FROM company_snapshot WHERE id = NEW.snapshot_id;
 IF owner_transaction IS DISTINCT FROM txid_current() THEN
  RAISE EXCEPTION 'Snapshot membership is frozen' USING ERRCODE = '23514';
 END IF;
 RETURN NEW;
END;
$$;
CREATE TRIGGER freeze_snapshot_member BEFORE INSERT ON snapshot_member
 FOR EACH ROW EXECUTE FUNCTION enforce_creation_transaction();
CREATE TRIGGER freeze_snapshot_cell BEFORE INSERT ON snapshot_financial_cell
 FOR EACH ROW EXECUTE FUNCTION enforce_creation_transaction();

CREATE TRIGGER immutable_company BEFORE UPDATE OR DELETE ON company FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_source_provider BEFORE UPDATE OR DELETE ON source_provider FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_source_endpoint BEFORE UPDATE OR DELETE ON source_endpoint FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_provider_entity_binding BEFORE UPDATE OR DELETE ON provider_entity_binding FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_raw_artifact BEFORE UPDATE OR DELETE ON raw_artifact FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_ingestion_attempt BEFORE UPDATE OR DELETE ON ingestion_attempt FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_normalization_run BEFORE UPDATE OR DELETE ON normalization_run FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_source_reference BEFORE UPDATE OR DELETE ON source_reference FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_company_identifier BEFORE UPDATE OR DELETE ON company_identifier FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_company_profile_observation BEFORE UPDATE OR DELETE ON company_profile_observation FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_company_registration BEFORE UPDATE OR DELETE ON company_registration FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_company_relationship BEFORE UPDATE OR DELETE ON company_relationship FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_security BEFORE UPDATE OR DELETE ON security FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_security_identifier BEFORE UPDATE OR DELETE ON security_identifier FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_listing BEFORE UPDATE OR DELETE ON listing FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_depositary_receipt_ratio BEFORE UPDATE OR DELETE ON depositary_receipt_ratio FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_filing BEFORE UPDATE OR DELETE ON filing FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_financial_result_disclosure BEFORE UPDATE OR DELETE ON financial_result_disclosure FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_announcement BEFORE UPDATE OR DELETE ON announcement FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_document BEFORE UPDATE OR DELETE ON document FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_document_link BEFORE UPDATE OR DELETE ON document_link FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_registry_filing_detail BEFORE UPDATE OR DELETE ON registry_filing_detail FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_registry_compliance_observation BEFORE UPDATE OR DELETE ON registry_compliance_observation FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_financial_concept BEFORE UPDATE OR DELETE ON financial_concept FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_metric_definition BEFORE UPDATE OR DELETE ON metric_definition FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_concept_metric_mapping BEFORE UPDATE OR DELETE ON concept_metric_mapping FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_vendor_metric_mapping BEFORE UPDATE OR DELETE ON vendor_metric_mapping FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_financial_observation BEFORE UPDATE OR DELETE ON financial_observation FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_vendor_financial_observation BEFORE UPDATE OR DELETE ON vendor_financial_observation FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_derived_metric_observation BEFORE UPDATE OR DELETE ON derived_metric_observation FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_market_quote BEFORE UPDATE OR DELETE ON market_quote FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_price_bar BEFORE UPDATE OR DELETE ON price_bar FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_market_metric_observation BEFORE UPDATE OR DELETE ON market_metric_observation FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_corporate_action BEFORE UPDATE OR DELETE ON corporate_action FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_fx_observation BEFORE UPDATE OR DELETE ON fx_observation FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_macro_series BEFORE UPDATE OR DELETE ON macro_series FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_macro_observation BEFORE UPDATE OR DELETE ON macro_observation FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_coverage_item BEFORE UPDATE OR DELETE ON coverage_item FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_data_quality_issue BEFORE UPDATE OR DELETE ON data_quality_issue FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_company_snapshot BEFORE UPDATE OR DELETE ON company_snapshot FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_snapshot_member BEFORE UPDATE OR DELETE ON snapshot_member FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_snapshot_financial_cell BEFORE UPDATE OR DELETE ON snapshot_financial_cell FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_officer_party BEFORE UPDATE OR DELETE ON officer_party FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_officer_appointment BEFORE UPDATE OR DELETE ON officer_appointment FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_ownership_observation BEFORE UPDATE OR DELETE ON ownership_observation FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_debt_instrument BEFORE UPDATE OR DELETE ON debt_instrument FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_credit_rating_observation BEFORE UPDATE OR DELETE ON credit_rating_observation FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_research_disclosure BEFORE UPDATE OR DELETE ON research_disclosure FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_reference_dataset BEFORE UPDATE OR DELETE ON reference_dataset FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE TRIGGER immutable_reference_dataset_observation BEFORE UPDATE OR DELETE ON reference_dataset_observation FOR EACH ROW EXECUTE FUNCTION reject_record_mutation();

CREATE FUNCTION enforce_evidence_creation_transaction() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE owner_transaction bigint;
BEGIN
 SELECT creation_transaction INTO owner_transaction FROM data_record WHERE id = NEW.record_id;
 IF owner_transaction IS DISTINCT FROM txid_current() THEN
  RAISE EXCEPTION 'Record evidence is frozen' USING ERRCODE = '23514';
 END IF;
 RETURN NEW;
END;
$$;
CREATE TRIGGER freeze_record_evidence BEFORE INSERT ON record_evidence
 FOR EACH ROW EXECUTE FUNCTION enforce_evidence_creation_transaction();
