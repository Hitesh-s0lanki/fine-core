# finecore-ai-server

Python 3.12 and FastAPI starter for FinCore's financial analytics and future
filing research service. This currently provides application configuration,
process health, generated API documentation, and declarative adapter onboarding.
A separate ingestion worker fetches explicitly configured unpaged JSON profile
sources and commits evidenced profiles and snapshots through the Java API.
Valuation calculations and model-provider integrations are not implemented yet.

## Run

Install Python 3.12 and [uv](https://docs.astral.sh/uv/getting-started/installation/),
then run from this directory:

```sh
uv sync --frozen
cp .env.example .env
uv run uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```

- Health: http://127.0.0.1:8000/health returns `{"status":"UP"}`.
- Interactive API documentation: http://127.0.0.1:8000/docs.
- OpenAPI schema: http://127.0.0.1:8000/openapi.json.

Port 8000 allows this service to run alongside the Java API on port 8080.
For a non-reloading process, omit `--reload`. Set `--host 0.0.0.0` when the
process must accept connections from outside localhost.

## Configuration

Settings use the `FINECORE_` prefix and load from environment variables or a
local `.env` file. Environment variables take precedence. `.env` is ignored by Git.

| Variable | Default | Accepted values |
| --- | --- | --- |
| `FINECORE_APP_NAME` | `finecore-ai-server` | Nonempty string |
| `FINECORE_ENVIRONMENT` | `development` | `development`, `test`, `production` |

Configuration is validated when the application starts. The environment label
does not enable authentication or change the deployment topology.

## Development checks

```sh
uv run ruff check .
uv run ruff format --check .
uv run pytest
uv build
```

Use `uv run ruff format .` to format sources. Commit `uv.lock` with dependency
changes so local environments and CI use the same resolved versions.

The repository's Git hooks run Ruff on staged snapshots and add tests on push.
They require `uv` when this module is present. CI also runs checks and builds
the Python distribution.

Application code is organized directly under `src/`:

```text
src/
├── app/
│   ├── main.py                 # FastAPI application entry point
│   ├── router.py               # Central router registration
│   └── config.py               # Application settings
├── controllers/
│   ├── adapter_controller.py   # Adapter HTTP endpoints and error responses
│   └── health_controller.py    # Health endpoint
├── services/
│   └── adapter_service.py      # Mapping, validation, registry and normalization
└── schemas/
    └── adapters.py             # Request and normalized-record schemas
```

Controllers handle HTTP requests and call services. Services implement behavior
without depending on FastAPI. Schemas define the input and output contracts.
Each directory is an importable package installed from `src/`; there is no
additional project-name package layer.


## Adapter onboarding: first implementation

The workflow is `prepare → propose a mapping → validate → register → normalize`.
Adapters are declarative JSON artifacts interpreted by one shared engine. The
service never executes generated Python or shell commands. There is currently
no LLM client: preparing an input bundle does not generate an adapter. A caller
must supply the model's proposed adapter, or a manually authored baseline.

Send `POST /adapters/prepare`:

- `source_id`: stable provider/format identifier, such as `companies_house_public_extract`.
- `documentation`: relevant endpoint and field documentation supplied as text.
  URLs in `documentation_urls` are references only; the service does not fetch them.
- `source_field_meanings`: documented units, scaling, reporting scope and identity semantics.
- `samples`: at least two representative responses, each with `company_id`,
  `expected_source_identifier`, `raw_reference`, and `payload`. Prefer different
  companies and reporting periods. The trusted caller must resolve company IDs.
- `expected_records`: independently checked normalized records for certification.
  They are excluded from the returned model input, so the agent does not see answer keys.

The response includes instructions, source samples, the adapter JSON Schema, and
the normalized record JSON Schema. Send these to the selected model through a
server-side provider integration. Do not include API keys, authorization headers,
or unnecessary personal information. This prototype does not automatically redact
payloads. A model may propose a mapping or flag unsupported/ambiguous meanings for review.

`POST /adapters/validate` accepts `{adapter, samples}`. Each sample must include
its expected records. It checks exact output equality, source identifiers, field
availability, unique array selectors, and target types. Passing fixtures proves
those examples pass; it does not establish financial correctness for every filing.
Independent semantic review remains necessary before using a new mapping.

`POST /adapters/register` accepts the same body and repeats validation before
persisting a content-addressed adapter version. `POST /adapters/{adapter_id}/normalize`
accepts `{sample: {...}}`, reuses that adapter, and returns company/source IDs,
schema and adapter versions, the raw reference, a hash of the canonical parsed
input, and normalized records. Raw responses must be retained by the ingestion
caller; the hash is not a digest of the original HTTP bytes. A changed adapter
creates a new ID. Failed validation never activates a replacement.

Supported records: company profiles, selected financial facts, and announcements.
Paths traverse object keys, array indices, or unique array matches such as
`["fields", {"key": "label", "equals": "Company status"}, "value"]`.
Allowed transformations are identity, exact decimal conversion and ISO dates.
Financial amounts must be decimal strings or integers, and retain native units.
No scale guessing, currency conversion, dynamic scripts, PDF parsing, or SEC
concept-tree flattening is implemented. Those need explicit extensions and tests.
New optional provider fields are ignored; missing mapped fields fail normalization.
This is field validation, not comprehensive schema-drift detection or repair.

The registry defaults to the repository's `temp/adapter-registry/` directory for
local development. Override `FINECORE_ADAPTER_REGISTRY_DIR` for deployed storage.
These endpoints inherit the starter's lack of authentication; use localhost for
testing. Access control and an operational promotion policy are not implemented.

### Run the saved company example

From this directory, with the previously collected `temp/` files present:

```sh
uv run --frozen python ../temp/adapter-prototype/demo.py
```

This runs the actual saved Infosys and Wipro public-register extracts through
preparation, validation, registration, and replay. The example mapping is manually
authored, not LLM-generated. Inspect `temp/adapter-prototype/llm-input.json` for
exactly what the model receives, `candidate.json` for the proposed mapping and
expected outputs, and `normalized-infosys.json` / `normalized-wipro.json` for results.
The example artifacts are ignored by Git; the API and tests do not depend on them.

## Durable ingestion worker

Run this as a separate process from FastAPI, alongside the Java API:

```sh
# Configure approved source origins in .env, for example:
# FINECORE_INGESTION_ALLOWED_ORIGINS=["https://example-provider.test"]
uv run python -m services.ingestion_worker
# Claim at most one ready job and exit:
uv run python -m services.ingestion_worker --once
```

Java owns job state, scheduling, leases, domain writes and snapshots. The worker
claims a job over HTTP, fetches one response, retains the original bytes, runs the
pinned local adapter, and submits the normalized profile to Java's transactional
completion API. It does not require the FastAPI HTTP process to be running.
Use the same persistent adapter registry for onboarding and worker processes.

Configure `FINECORE_CORE_API_URL`, `FINECORE_INGESTION_ALLOWED_ORIGINS` (a JSON
array), `FINECORE_INGESTION_RAW_DIR`, and a source-appropriate
`FINECORE_INGESTION_USER_AGENT`. Raw storage defaults to `temp/ingestion-raw`.
Adapter output hashes describe parsed JSON; artifact hashes describe the exact
original response bytes. They serve different purposes.

Requests have a 30-second socket timeout and a configurable response-size cap.
Redirects are rejected. URLs containing credentials, queries or fragments are
rejected in this first implementation. Source origins must be explicitly allowed.
Successful fetches and HTTP errors within the size cap are retained before
parsing. Oversized or interrupted responses cannot be retained as complete
artifacts. Authentication, pagination, HTML extraction, PDFs and SEC concept-tree
normalization are outside this worker's first source contract.

Only one `company_profile` record per response is currently bridged. Its
registration number must match the verified provider binding. Financial facts and
announcements are explicitly rejected by this bridge until their richer Java
contracts are implemented. Normalization errors stop the job for review; transient
network errors, HTTP 429 and HTTP 5xx use Java's bounded retry policy. Unexpected
storage/core communication errors leave the lease for recovery.

### Reproduce the complete local profile flow

With Java running against a **dedicated development database/queue**, run:

```sh
uv run python -m services.ingestion_demo
```

This starts a temporary synthetic local JSON provider, creates evidenced company,
provider, endpoint and binding records through Java, certifies a manually authored
adapter against two synthetic examples, queues a refresh, executes the actual
worker and reads the resulting frozen overview. It prints job and snapshot IDs
and an overview URL. Each invocation creates new demo records and retains raw
bytes. The local provider stops when the demo exits. No external source or LLM
is used. Keep other workers stopped while running this isolated demonstration.
