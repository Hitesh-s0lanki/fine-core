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

This implements persistence and APIs, not automatic collection from providers,
Python adapter integration, financial calculations or a complete source-precedence
engine. Snapshot completeness means the submitted manifest has no missing cells
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
