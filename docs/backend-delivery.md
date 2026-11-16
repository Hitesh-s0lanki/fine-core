# Backend delivery tracker

Updated: 16 November 2026.

This tracker covers the backend work needed for the planned frontend workflow:
search → company overview → financial statements → valuation → scenarios → export.
Statuses refer to the current working tree, including uncommitted work. Existing
storage models do not imply that ingestion or frontend workflows are complete.

Work proceeds one task at a time. Each task must have a documented API contract,
appropriate validation and tests, and a review before the next task starts.

## Existing foundation

- Company onboarding, local company search and company detail APIs.
- PostgreSQL persistence, Flyway migrations and sourced append-only assertions.
- Immutable snapshots and explicit snapshot overview replay.
- Profile ingestion targets, durable jobs, bounded retries and optional scheduling.
- Python adapter preparation, certification, registration and normalization.
- SEC submissions identity adapter; it does not ingest financial statements or filing history.
- Java and Python test suites and CI configurations.

## Ordered delivery queue

| ID | Task | Status | Completion criteria |
| --- | --- | --- | --- |
| B01 | SEC company discovery and onboarding | Complete | Search SEC identities beyond locally stored companies; explicitly import a verified identity without duplicates; retain source evidence; document frontend contracts and upstream failures. |
| B02 | SEC financial ingestion | Pending | Fetch and retain Company Facts; persist sourced financial observations through a richer ingestion contract; verify atomic completion and retry behavior. |
| B03 | SEC filing history | Pending | Persist sourced recent and historical filings with accession, form, filing date and links; represent incomplete history explicitly. |
| B04 | Financial normalization | Pending | Map canonical metrics; select annual periods; handle issuer-specific tags, restatements, units and source precedence; expose five years where available and explicit gaps. |
| B05 | Financial statement APIs | Pending | Return period-organized income statement, balance sheet and cash flow responses with exact values, units, currency, source references and missing-value explanations. |
| B06 | Frontend overview and snapshot discovery | Pending | Resolve a latest applicable snapshot; return sourced profile, filings, key metrics, freshness and coverage while preserving explicit historical replay. |
| B07 | Automatic financial snapshots | Pending | Apply a versioned selection policy to build financial manifests and selected cells automatically; verify refresh cannot alter older snapshots. |
| B08 | Data quality and valuation readiness | Pending | Generate coverage/reconciliation findings and report missing required valuation inputs with corrective actions. |
| B09 | DCF calculation service | Pending | Define method conventions; calculate forecasts, free cash flow, WACC, terminal value, enterprise/equity/per-share values; validate against independent reference calculations and boundary cases. |
| B10 | Saved models and scenarios | Pending | Persist versioned assumptions, pinned snapshot and method version; create, run, reopen and compare independent base/bull/bear scenarios reproducibly. |
| B11 | Valuation input sourcing | Pending | Define sourced or explicit manual inputs for prices, cash, debt, shares and rates; record dates, units, provenance and overrides. Required input policy must be settled before B09 is released. |
| B12 | Excel and PDF exports | Pending | Generate reproducible workbooks with formulas and reports with methodology and citations; expose export status and downloads. |
| B13 | Browser connectivity | Pending | Support documented frontend origins through CORS or a same-origin proxy and verify browser requests. |
| B14 | API specification | Pending | Publish Java endpoint schemas, concrete frontend response types, examples and pagination/error contracts; keep Python OpenAPI accurate. |
| B15 | Actionable API errors | Pending | Provide stable error codes and useful field/semantic validation details without exposing private provider bodies or database details. |
| B16 | Company refresh workflow | Pending | Expose company-level refresh and job discovery so frontend callers do not provision provider bindings or claim worker jobs. |
| B17 | Access and ownership policy | Pending | Establish single-user or multiuser scope; protect operational worker/admin routes and enforce ownership if accounts are enabled. |
| B18 | Java/Python calculation integration | Pending | Define versioned calculation requests/results, ownership, timeouts and failure behavior; keep financial logic deterministic. |
| B19 | Deployment and durable storage | Pending | Provide repeatable service/worker startup, persistent raw/adapter storage, backups, retention and cleanup policies, and operational configuration. |
| B20 | End-to-end MVP verification | Pending | Verify search through export for five issuers; prove provenance, missing-data behavior and saved-result reproducibility. |

The numbers retain the review's ordering for traceability. Where dependencies
require it, settle input policy (B11) and service contracts (B18) while implementing
DCF (B09), and complete browser connectivity (B13) before browser integration.
An individual task is not complete merely because a route or storage entity exists.

## Later phases

M&A and financing models, credit analysis, cited filing research, collaboration,
and Indian-market expansion remain outside the initial valuation MVP.

## Task evidence

### B01 — SEC company discovery and onboarding

- Implemented by the `company_discovery` sub-agent and reviewed by the parent agent.
- `GET /api/v1/companies/discovery/sec?query=apple&page=0&size=25` searches SEC names, tickers and CIKs, returning existing company IDs where available.
- `POST /api/v1/companies/discovery/sec/import` with `{"cik":"0000320193"}` explicitly imports an identity (201), or reuses an existing CIK (200).
- Imports retain source-byte hashes and evidence, reuse shared directory artifacts, serialize concurrent duplicate imports, and add directory-version-scoped ticker aliases for local search.
- Configure `SEC_USER_AGENT` with a real application/contact identity. Persist `SEC_RAW_DIRECTORY` with the Java service. See the Java README's discovery section for contracts and operational limits.
- Verification: Maven `verify` passed with 74 unit tests and 44 real-PostgreSQL integration tests, zero failures/errors/skips. Spotless and Checkstyle passed. Existing Python suite passed (36 tests). Whitespace checks passed.
- Database checks used an isolated temporary PostgreSQL 17 cluster; no existing application database was changed.
- Tests cover input validation, caching, retained bytes, upstream failures/backoff, response limits/timeouts, exact CIK reuse, concurrent imports, share-class tickers, ambiguous identity, source links and shared artifacts.
- A live SEC fetch was not run for this task because Java's identifying contact configuration was not supplied. Network behavior was checked using controlled test responses.
- This delivers identity discovery/import only. It does not generate financial observations, filings or snapshots, and does not establish valuation readiness. Existing companies are reused without updating their names/ticker aliases.
- Next task: B02, SEC financial ingestion. It has not started.
