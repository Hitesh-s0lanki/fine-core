# SEC submissions: live identity adapter

This manually authored adapter reads `cik` and `name` from SEC submissions JSON.
It is shared across all five companies listed in `companies.json`. It does not
extract financial statements, filing history, company status or incorporation
jurisdiction. Missing fields fail; name changes require reviewing the independently
specified expected names rather than automatically replacing the answer keys.

Official documentation:
https://www.sec.gov/search-filings/edgar-application-programming-interfaces
https://www.sec.gov/about/developer-resources

## Prepare and certify with live responses

From `finecore-ai-server`, configure your actual identifying contact:

```sh
export FINECORE_INGESTION_USER_AGENT='FinCore Your Name your-real-contact@your-organization.tld'
export FINECORE_INGESTION_ALLOWED_ORIGINS='["https://data.sec.gov"]'
uv run python -m services.sec_live_prepare
```

No SEC API key is required. The helper fetches sequentially with a one-second
pause, retains full response bytes and hashes in `temp/sec-live-profile`, compares
all five names and CIKs against `companies.json`, registers only after all pass,
and replays the registered mapping. This pace is for one local process; multiple
workers still need coordinated provider-wide rate limiting. SEC limits automated
traffic to at most 10 requests/second in aggregate.

Output files in the repository-level `temp/sec-live-profile` directory:

- `fetch-report.json`: URLs, timestamps, HTTP status and original-byte hashes.
- `candidate.json`: the request body accepted by `/adapters/validate` and `/adapters/register`.
- `registration.json`: the actual content-addressed adapter ID.
- `normalized.json`: replay results for all five companies.
- `AAPL.json`, `MSFT.json`, `AMZN.json`, `GOOGL.json`, `NVDA.json`: original responses.

A failed live check does not register a replacement adapter. Certification company
labels are fixture labels, not production company UUIDs.

## Connect to Java ingestion

Start PostgreSQL and the Java API. For each company, provision an evidenced Java
company, a SEC provider, a source endpoint and a verified provider binding.

The source endpoint's `paginationKind: none` must describe this **identity-only
response operation**. Full submissions history references additional files and
is not fully collected by this adapter. Do not claim complete filing coverage.

The binding's `sourceIdentifier` must be the exact ten-digit string in
`companies.json`, including leading zeros. Each target uses the same adapter ID
but its own binding and fetch URL. Example request to
`POST /api/v1/ingestion/targets`:

```json
{
  "bindingId": "<actual verified Apple binding UUID>",
  "endpointId": "<actual SEC identity endpoint UUID>",
  "fetchUrl": "https://data.sec.gov/submissions/CIK0000320193.json",
  "sourceId": "sec_submissions_profile",
  "adapterId": "<adapter_id from registration.json>",
  "registry": "SEC EDGAR filer directory",
  "registryJurisdiction": "US",
  "registrationKind": "unknown"
}
```

These placeholders must be replaced before submission. The existing profile bridge
calls its identifier field `registration_number`; here it carries a **CIK**, not
an incorporation number. The resulting Java registration describes SEC directory
identity only. `registryJurisdiction` describes the directory, not the issuer's
home jurisdiction. Status and home jurisdiction stay absent. A dedicated filer
identity bridge would avoid this legacy field naming.

Then call `POST /api/v1/ingestion/targets/{id}/refresh` and run:

```sh
uv run python -m services.ingestion_worker --once
```

Check `GET /api/v1/ingestion/jobs/{id}`. Success includes `snapshotId`; use it with
`GET /api/v1/companies/{companyId}/overview?snapshotId={snapshotId}`.

Raw storage and the adapter registry must persist on the worker host. Use manual
refreshes for the initial five-company test; omit `refreshSeconds`. Keep the Java
control API local/private as authentication is not implemented.

## One-time live ingestion through both servers

With Java on `127.0.0.1:8080`, FastAPI on `127.0.0.1:8000`, and your actual SEC
contact configured in the ignored Python `.env`, run from the Python module:

```sh
uv run python -m services.sec_live_ingest
```

The runner first fetches and certifies all five real responses, calls FastAPI's
validation, registration and normalization endpoints, then provisions SEC provider,
identity endpoint, evidenced company CIKs, verified bindings and manual-only Java
targets. Java retains a reference to the adapter digest in each normalization run;
the executable mapping stays in Python's shared registry.

It claims only the five newly requested job IDs, leaving unrelated jobs alone.
Each worker fetch is a fresh live SEC request. The runner checks the committed
snapshot identity and the original-byte hash against Java artifact metadata.
There is no background polling loop or recurring schedule after the command exits.

Results are written to `temp/sec-live-profile/ingestion-report.json`, including
company, target, job and snapshot IDs. Per-run raw certification responses and
saved overviews are retained under `temp/sec-live-profile/runs/<run-id>/` so a
later run cannot overwrite earlier evidence. Worker responses remain under
`temp/ingestion-raw/<job-id>/<attempt>/response.bin`.

A rerun intentionally creates a new manual target and refresh snapshot for each
company, while reusing exact CIK company identities and SEC provider/endpoint
records. The operation is not a single five-company database transaction: earlier
successful companies remain committed if a later company fails. Inspect the
checkpointed report before rerunning a partial failure.
