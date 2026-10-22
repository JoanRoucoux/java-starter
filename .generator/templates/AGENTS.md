# AGENTS.md

Guidance for AI coding agents working in this repository. See the [README](README.md) for the full project overview.

## Project

{{title}} — Spring Boot 4.1 / Java 25 backend in hexagonal architecture, generated from java-starter with these modules: **{{modules}}**. Base package: `{{basePackage}}`.

**There is no parent pom.** The root `pom.xml` is an aggregator (`<modules>` only) and no module declares it as a `<parent>`; each module is parented by `spring-boot-starter-parent` with an empty `<relativePath/>` and carries its own dependencies, versions and quality plugins. The duplication of the quality block (Spotless, JaCoCo, Failsafe) across modules is **deliberate** — it is what makes a module extractable into its own repository. Keep the copies in sync; do not factor them out into the root.

## Commands

| Command                                        | Purpose                                                   |
| ---------------------------------------------- | --------------------------------------------------------- |
| `./mvnw verify`                                | Build, unit + integration tests, ArchUnit, coverage check |
| `./mvnw verify -DskipITs`                      | Everything except the Testcontainers tests (no Docker)    |
| `./mvnw spotless:check` / `spotless:apply`     | Formatting check / fix (palantir-java-format)             |
| `./mvnw spring-boot:run -pl {{appName}}-api`   | Run the API locally                                       |

Before considering a change done, run the same pipeline as CI: `spotless:check` then `verify` (needs Docker for the `*IT` tests).

## Architecture

- `{{appName}}-domain` — **zero compile-scope dependencies**: pure Java, no Spring, no JPA — a Maven guarantee, not just a convention. `model/`, `exception/` (`business/` holds the abstract `BusinessException` base — extending `RuntimeException` — alongside its concrete subclasses; `technical/` holds `TechnicalException` the same way), `port/in/`, `port/out/`, `service/`. Domain services are plain classes, instantiated by the composition roots in the application modules. A port and its failure contract live together — `MarketDataPort` and `MarketDataUnavailableException` are both here; the adapter raises the latter.
- `{{appName}}-adapter` — outbound adapters, each split by role (`properties/`, `config/`, `adapter/` for a client; `entity/`, `repository/`, `adapter/` for persistence — even where it means an `adapter.adapter` package name), depending on `{{appName}}-domain` at **compile scope**. `ArchitectureTest` checks the same boundaries again once the application is assembled.
- `{{appName}}-api` — Spring Boot main, `application/` (`controller/` = controllers implementing the **generated** interfaces, `mapper/` = domain↔DTO mapping — **one class per resource, never a shared mapper**, `exception/` = the `@RestControllerAdvice`), `infrastructure/config/` (security, and **one `XxxDomainConfig` per slice**). Depends on `{{appName}}-adapter` at **runtime scope only** — adapters are wired into the context but invisible at compile time. The advice maps `BusinessException` → 422 and `TechnicalException` → 502; 401/403 are left to Spring Security. The OpenAPI contract lives in this module, not at the repository root.
<!-- module:schema -->
- `{{appName}}-schema` — Liquibase changelogs only, no Java code. Owns the schema and is applied out-of-band, by ops or a pipeline (the `schema` compose service, or `./mvnw liquibase:update -pl {{appName}}-schema`) — **no running application ever migrates the database**. The application modules depend on it at **test scope only** (never widen, never add it to `{{appName}}-adapter`), purely so their integration tests can migrate their own throwaway Testcontainers database against the real changelog before `ddl-auto: validate` checks it.
- **The migration password never goes on a command line**: `{{appName}}-schema`'s `liquibase.url`, `liquibase.username` and `liquibase.password` are Maven properties defaulting to the local compose database, and exporting `LIQUIBASE_PASSWORD` activates the `password-from-environment` profile. A `-Dliquibase.password=` shows in `ps` and in image history.
- **Each runnable module has its own `Dockerfile`, built from the repository root** (`docker build -f {{appName}}-api/Dockerfile .`): the build stage copies the whole tree, and the jar paths are globs (`*-api/target/*.jar`) so no rename rule is needed. The runtime stage runs as the unprivileged user `app` and the entrypoint sizes the JVM to the container (`-XX:MaxRAMPercentage=60 -XX:+ExitOnOutOfMemoryError`). The build image stays on the runtime's JDK (Java 25): a JDK bump changes both together.
- **The `schema` compose service is a one-shot migration image** (`{{appName}}-schema/Dockerfile`, offline after build, `LIQUIBASE_PASSWORD` for the password). `schema-applied` exists because `docker compose up --wait` returns while a one-shot container still runs: it only becomes healthy once `schema` completed successfully, and its `org.springframework.boot.ignore` label keeps Spring Boot's Docker Compose support from treating it as a service. A changed changeset needs `docker compose up -d --build schema`.
<!-- /module:schema -->
<!-- module:batch -->
- `{{appName}}-batch` — second Spring Boot application over the same `{{appName}}-domain`/`{{appName}}-adapter`: `BatchApplication` (in the base package, so the component scan reaches the adapters), `batch/job/` (the chunk-oriented step, wired to ports only) and `batch/config/` (its composition root). Depends on `{{appName}}-adapter` at **runtime scope**, exactly like `{{appName}}-api`. Its job repository is in memory (`spring-boot-starter-batch` has no JDBC one), so a run leaves no job metadata in the database. Each run is its own JVM with an empty repository: nothing refuses a rerun with identical parameters.
<!-- /module:batch -->
- `{{basePackage}}.generated.*` is build output of openapi-generator: never edit it, edit the spec and rebuild. Contract-first: the spec changes before the code.
- The hexagonal rules are law, enforced by the ArchUnit tests in the application modules. The demo features are reference implementations of a full slice — model new features on them.

## Conventions

- Commits follow [Conventional Commits](https://www.conventionalcommits.org).
- Formatting is Spotless/palantir; records for immutable data; constructor injection without Lombok. A [lefthook](https://lefthook.dev) pre-commit hook runs `spotless:apply` and re-stages the result automatically (`lefthook install` once after cloning).
- Any number the API reads must fit its column: `BoundedDecimalDeserializer` (`application/json`) refuses a `BigDecimal` the demo `numeric(19,4)` cannot hold with a 400, before any arithmetic runs. Change the bounds in `DecimalBounds` together with the column.
- A `TechnicalException` message names the dependency, never the upstream response; the full text goes to the log through the cause.
- A constraint violation on a path or query parameter answers 400 (`ApiExceptionHandler`), not 500.
- Sibling modules are depended on through an explicit version property (`{{appName}}-domain.version` and friends), never `${project.version}` — that would silently mean the wrong thing once a module is extracted.
<!-- module:schema -->
- Schema changes only through `{{appName}}-schema`'s Liquibase changesets (`ddl-auto: validate` will fail otherwise). New changesets are Liquibase formatted SQL files (`--liquibase formatted sql`, then `--changeset {{appName}}:003-add-index`): ids are sequential and descriptive, the author is the application name. `changelog-master.xml` stays XML (`includeAll`).
<!-- /module:schema -->

## Testing

- Naming drives the phase: `*Test` = surefire (unit, no Docker), `*IT` = failsafe (integration, Testcontainers).
- Controllers: `@WebMvcTest` + `@Import(SecurityConfig.class)` + `@MockitoBean` ports + `spring-security-test`'s `jwt()` post-processor (and a `@MockitoBean JwtDecoder` so the context starts).
<!-- module:schema -->
- Persistence: `@DataJpaTest` + `@ServiceConnection` PostgreSQL container, schema generated from the JPA mapping (`spring.jpa.hibernate.ddl-auto=create-drop`, set locally on the test). `{{appName}}-adapter` has a test-only `TestApplication` (`@SpringBootConfiguration`) because it contains no Spring Boot app.
- Full boot: the `*IT` tests of the application modules migrate their Testcontainers database with the real `{{appName}}-schema` changelog before `ddl-auto: validate` runs.
<!-- /module:schema -->
- Business scenarios: `CucumberIT` (`{{appName}}-api`, `cucumber/` package) runs every `.feature` file under `src/test/resources/features/` over real HTTP through the full Spring context (`CucumberSpringConfiguration`), security opened up via `app.security.permit-all`. It is a `*IT` like any other. Add a feature by adding a `.feature` file plus a step-definition class in `cucumber/`; a `@Before` hook (`Hooks`) resets shared fixtures between scenarios. Cucumber glue classes must be `public`, unlike the rest of this test suite.
- Bean-wiring code (`@Bean` methods) is unit-tested by calling those methods directly, so the coverage gate does not depend on Docker being available.
- External clients: WireMockServer without any Spring context.
- ArchUnit rules are plain JUnit `@Test` methods over a static `ClassFileImporter` on purpose — do not migrate them to `@AnalyzeClasses`/`@ArchTest`. A rule whose subject matches nothing fails, so keep rules next to the code they constrain.
- Coverage gate: 70% lines per module (JaCoCo, merged unit+IT data).

## Gotchas

- The aggregator declares the Spotless plugin although it holds no Java: `spotless:check` from the root resolves the plugin prefix per project and fails on any project that lacks it.
<!-- module:schema -->
- The demo table is named `positions` (plural): `POSITION` is a reserved word in PostgreSQL.
- **`{{appName}}-schema` stays a test-scope dependency of the application modules only** — never add it (or `liquibase-core`) to `{{appName}}-domain`/`{{appName}}-adapter`, and never widen its scope past `test`. An application must never be able to migrate the database itself.
- **Without Docker, `{{appName}}-adapter`'s coverage gate fails under `-DskipITs`**: expected, not a regression — its persistence code is only exercised by `*IT` tests.
- **`spring-boot:run` runs with the module as working directory**, so Spring Boot's Docker Compose support looks for `compose.yaml` there and fails with "No Docker Compose file found". `spring.docker.compose.file: ../compose.yaml` points it at the root file. `lifecycle-management: start-only` keeps PostgreSQL up when an application exits, because the others share it.
- **`spring-boot:run -pl <module>` resolves sibling modules from `~/.m2`**, not from the reactor: run `./mvnw install -DskipTests` after changing a sibling module, or the application starts on stale jars.
<!-- /module:schema -->
<!-- module:batch -->
- **`spring-boot-{{appName}}-batch` in Spring Boot 4 has no JDBC job repository**: `spring.batch.jdbc.initialize-schema` is an unknown property there and the repository is in memory. `spring-boot-{{appName}}-batch-jdbc` provides the JDBC one, and its schema would have to be the Spring Batch 6 one, not the 5.2.2 tables.
- `{{appName}}-batch`'s `hibernate.jdbc.batch_size` equals the step's `CHUNK_SIZE` (50) so a chunk is written in one JDBC batch: keep them in step.
<!-- /module:batch -->
- **`cucumber-junit-platform-engine` must stay pinned to a version built against the same `junit-jupiter` line Spring Boot manages** (see `{{appName}}-api/pom.xml`'s `cucumber.version` comment): a newer Cucumber needs a newer JUnit Platform than this project's dependency management provides, and fails at test discovery with `NoClassDefFoundError`.
- **`CucumberIT` always reports "Tests run: 0"** in the Surefire/Failsafe console summary — cosmetic, not a sign the suite didn't run. `cucumber.plugin=pretty,summary` (`junit-platform.properties`) prints the real scenario/step counts right above it.
- `mvnw` must stay executable on Linux CI (`git update-index --chmod=+x mvnw` if git loses the mode on Windows).
- GitHub Actions in `.github/workflows/` are pinned by commit SHA — when adding one, pin it the same way.
