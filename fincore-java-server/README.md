# fincore-java-server

FinCore company-data API using Java 21, Spring MVC, JPA/Hibernate, PostgreSQL,
Flyway and Actuator. It stores company identities, disclosures, financial
observations, source evidence and immutable snapshots.

## Requirements

- JDK 21, with `JAVA_HOME` set to the JDK installation directory.
- Internet access for the Maven wrapper and initial dependency downloads.
- PostgreSQL 17, or Docker Compose for the included local database.
- Docker for tests using Testcontainers, or a dedicated external PostgreSQL test database.

On macOS with Homebrew's `openjdk@21`, set:

```sh
export JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"
```

## Run

From this directory:

```sh
docker compose up -d postgres
./mvnw spring-boot:run
```

The local database defaults are `jdbc:postgresql://localhost:5432/fincore`, user
`fincore`, password `fincore`. Configure `DATABASE_URL`, `DATABASE_USERNAME` and
`DATABASE_PASSWORD` for another instance. `.env.example` lists the variables;
Spring does not load a `.env` file automatically, so export them in your shell
or configure your IDE. Compose reads `.env` for its own settings. Changing its
password variable does not reset an already initialized database volume.

Flyway applies versioned migrations before JPA starts. Hibernate validates the
schema and does not create or update it. `spring.jpa.open-in-view=false` keeps
database access inside service transactions. The migration requires permission
to install PostgreSQL's `btree_gist` extension for identifier validity constraints.

On Windows, use `mvnw.cmd` in place of `./mvnw`.

The default port is 8080. Verify the running application:

```sh
curl http://localhost:8080/actuator/health
```

The JSON response should include `"status":"UP"`.

## Company data API

The Java packages follow the layer-based layout of the
[reference project](https://github.com/Hitesh-s0lanki/ticksy-booking-app/tree/main/book-my-show-be/src/main/java/com/bookmyshow):

```text
src/main/java/com/fincore/server/
├── FincoreJavaServerApplication.java
├── config/       # JSON configuration and canonical resource definitions
├── controller/   # REST controllers
├── dto/          # Request and response records grouped by domain
├── exception/    # Central HTTP exception handling
├── interfaces/   # Service contracts and the creation-request contract
├── models/       # JPA entities grouped by domain
├── repository/   # Repositories grouped using the same domains
├── service/      # Transactional service implementations
├── util/         # Exact decimal conversion
└── validation/   # Evidence, identity and financial validation
```

Controllers depend on service interfaces; their implementations live in `service/`.
The larger layers (`models/`, `dto/`, `repository/`) share domain subpackages:

```text
models/                    # The same domain names are used in dto/ and repository/
├── common/                # Shared base record
├── company/               # Identity, profile, registrations and relationships
├── credit/                # Debt instruments and credit ratings
├── disclosure/            # Filings, announcements, documents and research
├── financial/             # Concepts, metrics, mappings and observations
├── governance/            # Officers, appointments and ownership
├── macro/                 # Shared economic series and observations
├── market/                # Quotes, prices, actions, FX and market metrics
├── quality/               # Coverage and quality issues
├── reference/             # External reference datasets
├── security/              # Securities, identifiers, listings and ADR ratios
├── snapshot/              # Frozen snapshots, members and selected cells
└── source/                # Providers, ingestion, artifacts and provenance
```

For example, `models/company/Company.java`, `dto/company/CompanyRequest.java`
and `repository/company/CompanyRepository.java` belong to the same domain.
Shared response envelopes live in `dto/common/`. The smaller controller,
service, interface and configuration packages remain directly under their layers.
Each DTO has its own Java file. Flyway migrations remain in
`src/main/resources/db/migration/`, and PostgreSQL connection settings remain in
`src/main/resources/application.properties`.

- `POST /api/v1/companies`: atomically create a company and initial identifiers.
- `GET /api/v1/companies?query=INFY&page=0&size=25`: search names, slugs, identifiers or listings.
- `GET /api/v1/companies/{id}`: read the company master.
- `POST /api/v1/companies/{id}/snapshots`: freeze an explicitly reviewed selection manifest.
- `GET /api/v1/companies/{id}/overview?snapshotId={snapshotId}`: replay that frozen overview.
- `GET /api/v1/company-data`: list all 50 canonical resource types.
- `POST /api/v1/company-data/{resource}`: create an evidenced assertion.
- `GET /api/v1/company-data/{resource}/{id}`: read an assertion.
- `GET /api/v1/company-data/{resource}?companyId={id}&page=0&size=25`: bounded detail queries.

Company, snapshot, member and cell creation use the dedicated endpoints. Other
resources use explicitly allowlisted request DTOs, backed by typed repositories.
The generic controller cannot resolve arbitrary Java class names.

Records and evidence are append-only. Corrections create new assertions;
there are no update/delete endpoints. PostgreSQL rejects updates/deletes and
evidence additions to previously committed records. Snapshot membership and
cells may only be inserted in the snapshot's creation transaction. Invalid input
returns `400`, missing resources `404`, and conflicting identities/constraints
`409`, without exposing SQL or database details.

The ingestion API now coordinates durable jobs and accepts normalized company
profiles and explicitly selected SEC Company Facts from a separate Python worker.
Canonical financial normalization, calculations and complete source precedence remain
unimplemented. Snapshot completeness means the submitted manifest has no missing cells
or incomplete coverage members; it does not establish real-world completeness.
The explicit selection policy must be recorded by the caller. Application
authentication remains outside this module's current implementation.

## Build

```sh
./mvnw verify
java -jar target/fincore-java-server-0.0.1-SNAPSHOT.jar
```

Application configuration lives in `src/main/resources/application.properties`. Set the `SERVER_PORT` environment variable to override the default port.

## Formatting and editor setup

Spotless applies Google Java Format with two-space indentation, removes unused imports, and uses LF line endings. The formatter version is pinned in `pom.xml` for consistent results across machines.

```sh
./mvnw spotless:apply  # Format Java sources and tests
./mvnw spotless:check  # Check formatting without changing files
./mvnw checkstyle:check  # Check Google style rules
```

Formatting is checked during Maven's `validate` phase, so `test` and `verify` fail if sources need formatting. Enable EditorConfig support in your editor and annotation processing for Lombok. Use the Maven formatter command as the source of truth.

Checkstyle also runs during `validate`, using the local `google_checks.xml` for both production and test Java sources. All violations fail the build. Spotless fixes layout automatically; Checkstyle also checks naming, imports, Javadoc, and other Google style rules that may need manual fixes.

The rules file comes from [Checkstyle's Google configuration at version 10.26.1](https://github.com/checkstyle/checkstyle/blob/checkstyle-10.26.1/src/main/resources/google_checks.xml), paired with the same Checkstyle engine version in `pom.xml`. The only local rule adjustment allows the conventional `IT` integration-test abbreviation. The requested DigiStrique reference returned 404 through the available GitHub access, so this uses the upstream Google configuration.

## Continuous integration

Install the repository's local Git hooks once per clone with `sh scripts/install-git-hooks.sh` from the repository root. Pre-commit runs staged whitespace checks and Maven `validate` (Spotless and Checkstyle) against the staged snapshot. Pre-push runs Maven `clean verify` against each distinct pushed commit tip, including unit and integration tests. Both hooks check the complete Java module, including future sources and tests, without auto-formatting or staging files. Configure `JAVA_HOME` in the environment used by your terminal or Git client.

The [Java server workflow](../.github/workflows/java-server-ci.yml) runs on pushes and pull requests that change the server or workflow, and can be started manually. It installs Java 21, caches Maven dependencies, and runs `./mvnw clean verify` once to check formatting, Checkstyle, unit tests, and integration tests. Checkstyle and test reports are uploaded even when a check fails.

## Development dependencies

- Spring Boot DevTools restarts the development server when compiled classes change. Recompile with your IDE or `./mvnw compile` while `spring-boot:run` is running. DevTools is excluded from the packaged application.
- Lombok is configured as an annotation processor and excluded from the application JAR.
- Jakarta Bean Validation supports request constraints such as `@NotNull`, `@NotBlank`, and `@Valid`.
- Spring Boot's configuration processor generates IDE metadata for `@ConfigurationProperties` classes.
- Spring Web, Actuator, and their testing support remain available. Logging uses Spring Boot's default SLF4J/Logback setup.

Spring Boot manages dependency versions; the formatting plugin and formatter have explicit versions.

## Writing and running tests

Spring Boot's test starter supplies JUnit Jupiter (including parameterized tests), AssertJ assertions, and Mockito mocks. The MVC test starter supports controller tests with `@WebMvcTest` and MockMvc.

- Name unit and slice tests `*Test` or `*Tests`; Maven Surefire runs them with `./mvnw test`.
- Name integration tests `*IT`; Maven Failsafe runs them with `./mvnw verify`, which also runs unit tests and checks formatting.
- Use `@SpringBootTest` for tests that need the complete application. `HealthEndpointIT` demonstrates a real HTTP request against a server on a random port.

The unit suite runs without Spring Boot, PostgreSQL or Docker. The application
context check is `FincoreJavaServerApplicationIT` and runs in the integration
phase.

Test reports are generated in `target/surefire-reports` and `target/failsafe-reports`.

Integration tests run Flyway and Hibernate validation against real PostgreSQL. By default,
Testcontainers starts an isolated PostgreSQL 17 container. Without Docker,
provide a dedicated test database (not an application or production database):

```sh
TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/fincore_test \
TEST_DATABASE_USERNAME=fincore TEST_DATABASE_PASSWORD=fincore ./mvnw clean verify
```

Tests cover atomic onboarding, source evidence, exact decimal values, temporal
identifier uniqueness, foreign reference types, JSON contracts, financial cell
selection and immutable snapshot replay. Testcontainers also runs in CI.

## Durable ingestion and scheduling

The first vertical workflow supports one unpaged JSON company profile per response.
Operational `ingestion_target` and `ingestion_job` tables are mutable; source
evidence, observations and snapshots remain append-only. Flyway V4 adds these
operational tables without changing existing assertions.

All routes below are relative to `/api/v1/ingestion`:

| Method and path | Behavior |
| --- | --- |
| `POST /targets` | Configure a verified binding, source endpoint and pinned adapter. |
| `POST /targets/{id}/refresh` | Enqueue work or return the existing unfinished job. |
| `GET /jobs/{id}` | Read status, attempt count, error code and resulting snapshot ID. |
| `POST /jobs/claim` | Claim one job; returns 204 if no work is ready. |
| `POST /jobs/{id}/complete` | Commit evidence, profile observations, snapshot and success atomically. |
| `POST /jobs/{id}/fail` | Append failed-attempt evidence and apply bounded retry policy. |

Example target request (replace IDs and the adapter digest with registered values):

```json
{
  "bindingId": "11111111-1111-1111-1111-111111111111",
  "endpointId": "22222222-2222-2222-2222-222222222222",
  "fetchUrl": "https://example-provider.test/company/FC001",
  "sourceId": "example_profile",
  "adapterId": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
  "registry": "Example Registry",
  "registryJurisdiction": "GB",
  "registrationKind": "overseas",
  "refreshSeconds": 3600
}
```

The binding must be verified and directly reference the company. The endpoint must
belong to the binding's provider and declare `paginationKind: none`. Registry and
registration kind are explicit configuration; they are not inferred from field
names or company domicile. The normalized `jurisdiction` is stored as the
registration's home jurisdiction, so certify that meaning during adapter review.
Only `statusRaw` is stored; no normalized legal status is guessed.

Set `INGESTION_SCHEDULING_ENABLED=true` to enable a scan every
`INGESTION_SCHEDULING_SCAN_MS` (default 30000 milliseconds). Each due target uses
its own `refreshSeconds` (minimum 60). Omit `refreshSeconds` for manual-only work.
The first scheduled check is immediately due. Scheduler ticks only enqueue work;
start the Python worker separately. Export these variables as with other Spring
configuration; Java does not automatically load `.env`.

PostgreSQL row locks and a partial unique index prevent duplicate unfinished jobs
for one target. Workers claim with `SKIP LOCKED`, a fresh token and a five-minute
lease. Expired leases can be reclaimed; old tokens cannot commit. There are at
most three claims per job. Transient failures back off for 30 then 60 seconds.
Semantic/configuration failures stop immediately. Completion acknowledgement is
idempotent. A crash cannot leave a partially committed snapshot. Raw files may
remain orphaned if a worker crashes before committing metadata; retention and
cleanup are not yet implemented.

The snapshot selection policy is explicitly `profile-only-v1`, with no financial
cells. A complete profile manifest does not imply financial coverage or valuation
readiness. New refreshes append new records and snapshots. Older snapshots keep
their original selected records.

This is a trusted local API, matching the existing module's lack of authentication.
Keep worker/control routes on a private local interface. Production access control,
source credentials, target editing/pause controls, distributed object storage,
heartbeat extension and other financial-provider ingestion are follow-up work. Local `file:` raw
URIs refer to the Python worker host; Java stores metadata and does not read files.

For a complete runnable synthetic demonstration, see the Python README's
`python -m services.ingestion_demo` instructions. No external provider is required.

For the five SEC URLs, the Python `services.sec_live_ingest` command registers the
mapping through FastAPI and provisions real Java company/binding/target records
before ingesting. `POST /api/v1/ingestion/jobs/claim?jobId={id}` limits a claim to
that exact ready job; omitting the parameter retains general queue claiming.
See `finecore-ai-server/adapters/sec-submissions-profile/README.md` for the run
sequence and the persisted ingestion report. These targets have no refresh
interval and will not be periodically scheduled.

## SEC company discovery and explicit import

The frontend can search the official SEC ticker directory without first loading
companies into PostgreSQL. Configure `SEC_USER_AGENT` with your application name
and real contact email, then restart Java. Without it, discovery returns 503.
`SEC_RAW_DIRECTORY` defaults to `./data/sec-directory`; persist this directory
alongside the Java service. Raw source references use local file URIs on that
host, so keep these files available for evidence review and backups.

| Method and path | Behavior |
| --- | --- |
| `GET /api/v1/companies/discovery/sec?query=apple&page=0&size=25` | Search name, ticker substring, or exact CIK; no company writes. |
| `POST /api/v1/companies/discovery/sec/import` | Explicitly import the selected `{"cik":"0000320193"}` identity. |

Discovery returns `items` containing `cik`, `ticker`, `name`, and `companyId`
(null until a matching CIK is stored), plus `page`, `size`, `totalElements`,
`hasNext`, `sourceUrl`, and `retrievedAt`. Queries must contain 1–100 characters,
page must be nonnegative, and size must be 1–100. Exact ticker matches rank first.
An issuer may have multiple rows for different ticker associations. The directory
covers the SEC's published ticker associations, not all historical EDGAR filers,
and does not filter out financial-sector issuers.

Import requires a nonzero ten-digit CIK present in that directory. It returns
201 with a Location header for a new company or 200 when a company with that CIK
already exists, with `{company, created, coverage: "identity_only"}`. `coverage`
describes the scope of this operation; it does not assess any existing company's
financial coverage. The retained directory bytes, SHA-256, source URL, retrieval
time and extraction locator evidence the company and identifiers. Ticker aliases
use identifier scheme `other` and authority `SEC ticker directory:<directory SHA-256>`, allowing the
existing local company search to resolve newly imported tickers without creating
unverified exchange listings. The version-specific authority scopes these aliases
to the observed directory, permitting later ticker reassignment without treating
the ticker as a permanent global issuer identifier. Existing companies are reused without rewriting
their names or adding current ticker aliases. Ambiguous stored CIK ownership
returns 409. Concurrent imports through this endpoint serialize by CIK and reuse
one identity; unrelated manual onboarding remains subject to database constraints.

The fixed upstream URL is `https://www.sec.gov/files/company_tickers.json`.
Successful responses are cached in-process for 24 hours; `retrievedAt` is the
actual retrieval time, not a publication timestamp. Expired data is not silently
served when refresh fails. Refreshes coalesce within one Java process and failed
attempts back off for 30 seconds. HTTP redirects are rejected, connect/request
and overall wait timeouts are bounded, and response bytes are capped at 10 MiB.
A failed upstream request/invalid payload/retention error returns 502; retry during
the cooldown returns 503. Multiple Java instances and the separate Python worker
still require shared provider-wide throttling when deployed together.

Import does not fetch financials, filings, snapshots, sector or market prices.
SEC directory identity and ticker associations do not establish valuation readiness.
The routes follow the current trusted-local API model; deployment access control
and browser CORS/proxy configuration remain separate delivery tasks.

Official references: [SEC ticker mapping and developer FAQ](https://www.sec.gov/about/webmaster-frequently-asked-questions)
and [SEC access and identifying User-Agent guidance](https://www.sec.gov/search-filings/edgar-search-assistance/accessing-edgar-data).

## Selected raw SEC Company Facts ingestion (Task 2)

Request a manual-only refresh for an already onboarded company with one evidenced
current ten-digit CIK:

```http
POST /api/v1/ingestion/sec-company-facts/refresh
Content-Type: application/json

{
  "companyId": "<existing company UUID>",
  "selection": {"taxonomy": "us-gaap", "concepts": ["Assets", "NetIncomeLoss"]}
}
```

The response is `{targetId, selection, job}`. The API provisions/reuses SEC source
configuration from existing CIK evidence, reuses a target for the same sorted
selection, and reuses its unfinished job for repeated/concurrent requests. Missing
or ambiguous current CIKs return 409. Ambiguous cross-company CIK ownership is
rejected. It does not fetch remotely; the separate Python worker performs the fetch.

Configure the Python worker with a real identifying `FINECORE_INGESTION_USER_AGENT`,
`FINECORE_INGESTION_ALLOWED_ORIGINS=["https://data.sec.gov"]`, the core API URL and
persistent raw storage. Run `uv run python -m services.ingestion_worker --once`
from the Python module, or run continuously. The fixed source is
`https://data.sec.gov/api/xbrl/companyfacts/CIK<ten-digit CIK>.json`. Unexpected URLs,
redirects and mismatched payload CIKs are rejected. Entire responses are retained
before parsing. Large issuers may exceed the default 5 MB response cap; set
`FINECORE_INGESTION_MAX_BYTES` explicitly, up to 50,000,000 bytes.

Select one standard taxonomy (`us-gaap`, `ifrs-full`, `dei`, `srt`) and 1–20 distinct
raw concept names. Every returned unit, period and accession for those concepts
is included, including overlapping and amended observations. Missing/empty selected
concepts, malformed selected facts, unsupported decimal precision or more than
5,000 selected observations fail the whole job without truncating. Narrowing the
selection limits persisted records, but the API still downloads full Company Facts.

Poll `GET /api/v1/ingestion/jobs/{job.id}`. Success has `snapshotId: null` and:

```json
{
  "result": {
    "normalizationRunId": "<run UUID>",
    "recordCount": 123,
    "coverage": "selected_raw_facts",
    "parserVersion": "sec-companyfacts-v1"
  }
}
```

Read the run's `outputRecordIds` at
`GET /api/v1/company-data/normalization-runs/{normalizationRunId}`, then read those
`financial-observations` IDs. Company-wide pagination is also available:
`GET /api/v1/company-data/financial-observations?companyId=<UUID>&page=0&size=100`.
Follow each observation's `conceptId`, `filingId` and `evidenceIds` through the
generic resource read routes. Evidence retains the exact source JSON pointer,
fetch attempt, source URL and original-byte artifact/hash.

Trusted workers submit lease token, fetch times, artifact, pinned adapter/source IDs,
CIK and selected records to `POST /api/v1/ingestion/jobs/{id}/complete-financials`.
Java checks selection, unique source locators, exact numeric/token equality, issuer
and filing metadata. Artifact, attempt, evidence, concepts, minimal accession filing
records, observations, normalization run and job success commit atomically. A late
failure rolls all writes back. Completion acknowledgement is idempotent; the existing
bounded transient retry/lease recovery policy applies. Profile jobs retain their
original completion endpoint and profile-only snapshots.

Native units and exact numeric values are preserved without floating-point coercion.
`reportedValueRaw` retains the original JSON numeric token, including exponent or
negative-zero spelling. Scale is 0 (native-unit value); original XBRL decimals,
context IDs and acceptance timestamps stay unknown. Scope is `unspecified`,
dimensions are `not_reported`, and concept period metadata is `unknown`; observation
dates determine instant/duration type. Filing date gives date-precision availability,
not an acceptance timestamp. Filing stubs retain accession/form/date and point to
the actual Company Facts source; full history, original filing document URLs,
report dates and amendment linkage remain Task 3. Existing concepts/filings are
reused; contradictory accession metadata fails rather than rewriting assertions.

Refreshes append evidence-scoped observations and preserve prior facts. Assertion
keys include job and source locator, providing replay idempotency rather than
cross-refresh deduplication. Canonical metrics, restatement precedence, annual/five-year
selection, statement APIs, financial snapshots and valuation readiness remain later tasks.

The operation pins the SHA-256 of the packaged extraction descriptor at
`finecore-ai-server/src/services/sec_company_facts_contract.json`. The digest is
stored in `adapterId` and `normalization-runs.adapterContentHash`; it attests the
contract's bytes, not executable parser code. Semantic parser/descriptor changes
require a new version/digest and matching Java pin. This specialized SEC operation
does not use the generic declarative adapter registry.

The trusted-local access model still applies. Provisioning is serialized within
this API; unrelated manual provisioning relies on database constraints. Multiple
workers/services require shared provider-wide SEC rate coordination before deployment.
Tests use controlled responses and isolated PostgreSQL; no live SEC request is required.

Official source: [SEC Company Facts API and XBRL scope](https://www.sec.gov/search-filings/edgar-application-programming-interfaces).
