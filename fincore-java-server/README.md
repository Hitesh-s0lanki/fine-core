# fincore-java-server

Spring Boot API starter for FinCore, using Java 21, Maven, Spring Web MVC, and Actuator.

## Requirements

- JDK 21, with `JAVA_HOME` set to the JDK installation directory.
- Internet access for the Maven wrapper and initial dependency downloads.

On macOS with Homebrew's `openjdk@21`, set:

```sh
export JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"
```

## Run

From this directory:

```sh
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd` in place of `./mvnw`.

The default port is 8080. Verify the running application:

```sh
curl http://localhost:8080/actuator/health
```

The JSON response should include `"status":"UP"`.

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

Test reports are generated in `target/surefire-reports` and `target/failsafe-reports`.
