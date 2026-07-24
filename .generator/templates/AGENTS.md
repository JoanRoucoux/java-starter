# AGENTS.md

Guidance for AI coding agents working in this repository. See the [README](README.md) for the full project overview.

## Project

{{title}} — Spring Boot 3.5 / Java 21 backend API in hexagonal architecture, generated from java-starter-api. Maven multi-module with wrapper (`./mvnw`, `mvnw.cmd` on Windows cmd). Base package: `{{basePackage}}`.

## Commands

| Command                                      | Purpose                                                   |
| -------------------------------------------- | --------------------------------------------------------- |
| `./mvnw verify`                              | Build, unit + integration tests, ArchUnit, coverage check |
| `./mvnw test`                                | Unit tests only (no Docker needed)                        |
| `./mvnw spotless:check` / `spotless:apply`   | Formatting check / fix (palantir-java-format)             |
| `./mvnw spring-boot:run -pl {{appName}}-api` | Run locally (starts PostgreSQL via compose.yaml)          |

Before considering a change done, run the same pipeline as CI: `spotless:check` then `verify` (needs Docker for the `*IT` tests).

## Architecture

- `{{appName}}-domain` — `model/`, `exception/` (`business/` holds the abstract `BusinessException` base — extending `RuntimeException` — alongside its concrete subclasses; `technical/` holds `TechnicalException` the same way), `port/in/`, `port/out/`, `service/`. **Zero compile-scope dependencies**: never add Spring, JPA or any framework here. Domain services are plain classes, instantiated by `DomainConfig` in {{appName}}-api (the composition root). A port and its failure contract live together — `MarketDataPort` and `MarketDataUnavailableException` are both here; the adapter raises the latter.
- `{{appName}}-adapter` — outbound adapters, each split by role (`entity/`, `repository/`, `adapter/` for persistence; `properties/`, `config/`, `adapter/` for the client) rather than a shared package, even where it means an `adapter.adapter` package name: `persistence/adapter/` implements the domain's read (`LoadPositionPort`) and write (`SavePositionPort`) ports with `persistence/entity/` + `persistence/repository/`; `client/adapter/` implements the market data port using the `RestClient` wired by `client/config/` from `client/properties/` (`@ConfigurationProperties` for base-url/timeouts), raising `MarketDataUnavailableException` on 5xx/timeout. **No dependency on `{{appName}}-schema`** — see below.
- `{{appName}}-api` — Spring Boot main, `application/` (`controller/` = controllers implementing the **generated** interfaces, `mapper/` = domain↔DTO mapping — **one class per resource, never a shared mapper**, `exception/` = the `@RestControllerAdvice`), `infrastructure/config/` (security, domain wiring). Depends on `{{appName}}-adapter` at **runtime scope** only — do not change that scope. The advice maps `BusinessException` → 422 and `TechnicalException` → 502; 401/403 are left to Spring Security.
- `{{appName}}-schema` — Liquibase changelogs only, no Java code. Owns the schema and is applied out-of-band, by ops or a pipeline (`./mvnw liquibase:update -pl {{appName}}-schema`) — **the running app never migrates the database itself**. `{{appName}}-api` depends on it at **test scope only** (never widen), purely so `ApplicationIT` can migrate its own throwaway Testcontainers database against the real changelog before `ddl-auto: validate` checks it.
- `{{basePackage}}.generated.*` is build output of openapi-generator (from [openapi/openapi.yaml](openapi/openapi.yaml)): never edit it, edit the spec and rebuild. Contract-first: the spec changes before the code.
- The hexagonal rules are law, enforced by the `ArchitectureTest` in {{appName}}-api. The `position` feature is the reference implementation of a full slice — model new features on it.

## Conventions

- Commits follow [Conventional Commits](https://www.conventionalcommits.org). No local git hooks: CI relies on the messages.
- Formatting is Spotless/palantir; records for immutable data; constructor injection without Lombok.
- Schema changes only through `{{appName}}-schema`'s Liquibase changesets (`ddl-auto: validate` will fail otherwise). Changeset ids are sequential and descriptive (`002-add-index`).

## Testing

- Naming drives the phase: `*Test` = surefire (unit, no Docker), `*IT` = failsafe (integration, Testcontainers).
- Controllers: `@WebMvcTest` + `@Import(SecurityConfig.class)` + `@MockitoBean` ports + `spring-security-test`'s `jwt()` post-processor (and a `@MockitoBean JwtDecoder` so the context starts).
- Persistence: `@DataJpaTest` + `@ServiceConnection` PostgreSQL container, schema generated from the JPA mapping (`spring.jpa.hibernate.ddl-auto=create-drop`, local to the test). The adapter module has a test-only `TestApplication` (`@SpringBootConfiguration`) because it contains no Spring Boot app.
- Full boot: `ApplicationIT` ({{appName}}-api) migrates its Testcontainers database with the real `{{appName}}-schema` changelog before `ddl-auto: validate` runs — the only place the real changelog is exercised.
- External clients: WireMockServer without any Spring context.
- ArchUnit rules are plain JUnit `@Test` methods over a static `ClassFileImporter` on purpose — do not migrate them to `@AnalyzeClasses`/`@ArchTest`.
- Coverage gate: 70% lines per module (JaCoCo, merged unit+IT data).

## Gotchas

- **Run Maven on JDK 21–24**: palantir-java-format calls javac internals that changed in JDK 25 (`NoSuchMethodError` in Spotless). CI pins Temurin 21.
- The demo table is named `positions` (plural): `POSITION` is a reserved word in PostgreSQL.
- **`{{appName}}-schema` stays a test-scope dependency of `{{appName}}-api` only** — never add it (or `liquibase-core`) to `{{appName}}-adapter`, and never widen its scope past `test`. The app must never be able to migrate the database itself.
- `mvnw` must stay executable on Linux CI (`git update-index --chmod=+x mvnw` if git loses the mode on Windows).
- GitHub Actions in `.github/workflows/` are pinned by commit SHA — when adding one, pin it the same way.
