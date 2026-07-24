# AGENTS.md

Guidance for AI coding agents working in this repository. See the [README](README.md) for the full project overview.

## Project

Spring Boot 3.5 / Java 21 starter for backend APIs in hexagonal architecture, built to back a frontend application and attach to PostgreSQL and/or external APIs. Maven multi-module with wrapper (`./mvnw`, `mvnw.cmd` on Windows cmd). The placeholder identity is `com.example.starter` / `starter-*`: the generator renames modules, artifactIds and packages at generation time.

## Commands

| Command                                  | Purpose                                                     |
| ---------------------------------------- | ----------------------------------------------------------- |
| `./mvnw verify`                          | Build, unit + integration tests, ArchUnit, coverage check   |
| `./mvnw test`                            | Unit tests only (no Docker needed)                          |
| `./mvnw spotless:check` / `spotless:apply` | Formatting check / fix (palantir-java-format)             |
| `./mvnw spring-boot:run -pl starter-api` | Run locally (starts PostgreSQL via compose.yaml)            |

Before considering a change done, run the same pipeline as CI: `spotless:check` then `verify` (needs Docker for the `*IT` tests).

## Architecture

- `starter-domain` — `model/`, `exception/` (`business/` holds the abstract `BusinessException` base — extending `RuntimeException` — alongside its concrete subclasses; `technical/` holds `TechnicalException` the same way), `port/in/`, `port/out/`, `service/`. **Zero compile-scope dependencies**: never add Spring, JPA or any framework here. Domain services are plain classes, instantiated by `DomainConfig` in starter-api (the composition root). A port and its failure contract live together — `MarketDataPort` and `MarketDataUnavailableException` are both here; the adapter raises the latter.
- `starter-adapter` — outbound adapters, each split by role (`entity/`, `repository/`, `adapter/` for persistence; `properties/`, `config/`, `adapter/` for the client) rather than a shared package, even where it means an `adapter.adapter` package name: `persistence/adapter/` implements the domain's read (`LoadPositionPort`) and write (`SavePositionPort`) ports with `persistence/entity/` + `persistence/repository/`; `client/adapter/` implements the market data port using the `RestClient` wired by `client/config/` from `client/properties/` (`@ConfigurationProperties` for base-url/timeouts), raising `MarketDataUnavailableException` on 5xx/timeout. **No dependency on `starter-schema`** — see below.
- `starter-api` — Spring Boot main, `application/` (`controller/` = controllers implementing the **generated** interfaces, `mapper/` = domain↔DTO mapping — **one class per resource, never a shared mapper**, `exception/` = the `@RestControllerAdvice`), `infrastructure/config/` (security, domain wiring). Depends on `starter-adapter` at **runtime scope** only — do not change that scope. The advice maps `BusinessException` → 422 and `TechnicalException` → 502; 401/403 are left to Spring Security's filter chain.
- `starter-schema` — Liquibase changelogs only, no Java code: `db/changelog/changelog-master.xml` `<includeAll>`s `changesets/`. Owns the schema and is applied out-of-band, by ops or a pipeline (`./mvnw liquibase:update -pl starter-schema`) — **the running app never migrates the database itself**. `starter-api` depends on it at **test scope only** (never widen: no `-adapter` dependency, no non-test scope in `-api`), purely so `ApplicationIT` can migrate its own throwaway Testcontainers database against the real changelog before `ddl-auto: validate` checks it.
- `com.example.starter.generated.*` is build output of openapi-generator (from [openapi/openapi.yaml](openapi/openapi.yaml)): never edit it, edit the spec and rebuild. Contract-first: the spec changes before the code.
- The hexagonal rules are law, enforced by [ArchitectureTest](starter-api/src/test/java/com/example/starter/ArchitectureTest.java): the inbound side (`application`/`infrastructure`) never reaches the adapters, adapters use the domain only through `port`/`model`/`exception` (never `service`), `persistence` and `client` stay independent, only `Business`/`Technical` subclasses live in the exception subpackages, outbound ports (`port/out`) are implemented only by `adapter`, only `application.controller`/`application.mapper` depend on generated code, and the top-level packages (`domain`, `adapter`, `application`, `infrastructure`, `generated`) stay free of cycles.

## Conventions

- Everything in the repo is written in **English** (code, comments, docs, commit messages).
- Commits follow [Conventional Commits](https://www.conventionalcommits.org). No local git hooks: CI and release-please rely on the messages.
- Formatting is Spotless/palantir; records for immutable data; constructor injection without Lombok.
- Schema changes only through `starter-schema`'s Liquibase changesets (`ddl-auto: validate` will fail otherwise). Changeset ids are sequential and descriptive (`002-add-index`, author `starter`) — see Gotchas.

## Testing

- Naming drives the phase: `*Test` = surefire (unit, no Docker), `*IT` = failsafe (integration, Testcontainers).
- Controllers: `@WebMvcTest` + `@Import(SecurityConfig.class)` + `@MockitoBean` ports + `spring-security-test`'s `jwt()` post-processor (and a `@MockitoBean JwtDecoder` so the context starts).
- Persistence: `@DataJpaTest` + `@ServiceConnection` PostgreSQL container, schema generated from the JPA mapping (`spring.jpa.hibernate.ddl-auto=create-drop`, set locally on the test — this module has no `starter-schema` dependency, so it doesn't validate against the real changelog; `ApplicationIT` does that). The adapter module has a test-only `TestApplication` (`@SpringBootConfiguration`) because it contains no Spring Boot app.
- Full boot: `ApplicationIT` (`starter-api`) migrates its Testcontainers database with the real `starter-schema` changelog (`@TestPropertySource` pointing `spring.liquibase.change-log` at it) before `ddl-auto: validate` runs — the only place the real changelog is exercised.
- External clients: WireMockServer without any Spring context.
- ArchUnit rules are written as plain JUnit `@Test` methods over a static `ClassFileImporter` **on purpose**: the dedicated `archunit-junit5` engine reported 0 tests under this surefire setup — do not migrate back to `@AnalyzeClasses`/`@ArchTest`.
- Coverage gate: 70% lines per module (JaCoCo, merged unit+IT data). Intentionally lower than actual coverage; do not raise it.

## Generator

New applications are scaffolded from this starter by [starter-generator](https://github.com/JoanRoucoux/starter-generator), a generic engine: everything starter-specific lives **here**, in [generator.config.json](generator.config.json) (removed files, module/package renames, tree-wide identity rewrites — full spec in the generator's README) and `.generator/templates/` (README, AGENTS and CI workflow rendered with `{{token}}` placeholders).

Keep them in sync with the starter:

- The `position` demo feature is **kept** in generated applications as a reference implementation — do not add its files to the manifest's `remove`.
- The manifest's `rename` entries mirror the module directories and the source roots of the base package; `replaceAll` rewrites `com.example.starter` and the `starter-*` artifactIds. When a module moves or a package root changes, update them (and `copyIgnore`). `starter-schema` has no Java source, so it only needs a module-directory `rename` and an artifactId `replaceAll` entry — no source-root renames.
- `.generator/templates/README.md`, `AGENTS.md` and `.github/workflows/ci.yml` overwrite this repo's versions in generated apps. When those files change here, re-check the templates. The generated `ci.yml` has **no `generate` job** (a generated app has no manifest).
- Template-only content (community files, release tooling) must be listed in the manifest's `remove`; new root-level files that should not ship in generated apps must be added there.
- Templates contain `{{tokens}}`: Spotless only targets `src/**/*.java`, so `.generator/templates/` is naturally excluded — keep it that way.
- The `generate` job in [ci.yml](.github/workflows/ci.yml) generates an app from the working tree and runs its quality gates — it fails when the manifest or templates drift.

## Gotchas

- **Run Maven on JDK 21–24**: palantir-java-format calls javac internals that changed in JDK 25 (`NoSuchMethodError` in Spotless). CI pins Temurin 21.
- The table is named `positions` (plural): `POSITION` is a reserved word in PostgreSQL.
- **`starter-schema` stays a test-scope dependency of `starter-api` only** — never add it (or `liquibase-core`) to `starter-adapter`, and never widen its scope past `test` in `starter-api`. The app must never be able to migrate the database itself; only `ApplicationIT` does, deliberately.
- Never produce a template path with a double-underscore word other than a generator token — `__word__` sequences in `.generator/templates/` paths are parsed as path tokens.
- `mvnw` must stay executable on Linux CI: `.gitattributes` forces LF and the file mode is committed (`git update-index --chmod=+x mvnw` after a fresh checkout on Windows if git loses it).
- On Windows, deep module paths can hit MAX_PATH: enable `git config core.longpaths true` if needed.
- GitHub Actions in `.github/workflows/` are pinned by commit SHA (Dependabot keeps them updated) — when adding one, pin it the same way.
- Nothing is committed or pushed without an explicit request from the maintainer.
