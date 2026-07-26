# AGENTS.md

Guidance for AI coding agents working in this repository. See the [README](README.md) for the full project overview.

## Project

Spring Boot 3.5 / Java 21 starter for backends in hexagonal architecture, built to back a frontend application and attach to PostgreSQL and/or external APIs. The placeholder identity is `com.example.starter` / `starter-*`: the generator renames modules, artifactIds and packages at generation time.

**There is no parent pom.** The root `pom.xml` is an aggregator (`<modules>` only) and no module declares it as a `<parent>`; each module is parented by `spring-boot-starter-parent` with an empty `<relativePath/>` and carries its own dependencies, versions and quality plugins. The duplication of the quality block (Spotless, JaCoCo, Failsafe) across modules is **deliberate** — it is what makes a module extractable into its own repository. Keep the copies in sync; do not "factor them out" into the root.

## Commands

| Command                                     | Purpose                                                     |
| ------------------------------------------- | ----------------------------------------------------------- |
| `./mvnw verify`                             | Build, unit + integration tests, ArchUnit, coverage check   |
| `./mvnw verify -DskipITs`                   | Everything except the Testcontainers tests (no Docker)      |
| `./mvnw spotless:check` / `spotless:apply`  | Formatting check / fix (palantir-java-format)               |
| `./mvnw spring-boot:run -pl starter-api`    | Run the API locally (starts PostgreSQL via compose.yaml)    |
| `./mvnw spring-boot:run -pl starter-batch`  | Run the demo job, then exit                                 |

Before considering a change done, run the same pipeline as CI: `spotless:check` then `verify` (needs Docker for the `*IT` tests).

## Architecture

- `starter-core` — the hexagon, in one module: `domain/` (`model/`, `exception/` with `business/` holding the abstract `BusinessException` base — extending `RuntimeException` — alongside its concrete subclasses and `technical/` holding `TechnicalException` the same way, `port/in/`, `port/out/`, `service/`) and `adapter/` (`persistence/` split into `entity/`, `repository/`, `adapter/`; `client/` split into `properties/`, `config/`, `adapter/` — even where it means an `adapter.adapter` package name). **Never add Spring or JPA imports under `domain/`**: Maven no longer separates the two, so [ArchitectureTest](starter-api/src/test/java/com/example/starter/ArchitectureTest.java) is the only guard. Domain services are plain classes, instantiated by the composition roots in the application modules. A port and its failure contract live together — `MarketDataPort` and `MarketDataUnavailableException` are both in the domain; the adapter raises the latter.
- `starter-api` — Spring Boot main, `application/` (`controller/` = controllers implementing the **generated** interfaces, `mapper/` = domain↔DTO mapping — **one class per resource, never a shared mapper**, `exception/` = the `@RestControllerAdvice`), `infrastructure/config/` (security, and **one `XxxDomainConfig` per slice**). The advice maps `BusinessException` → 422 and `TechnicalException` → 502; 401/403 are left to Spring Security's filter chain. The OpenAPI contract lives in this module (`starter-api/openapi/openapi.yaml`), not at the repository root.
- `starter-schema` — Liquibase changelogs only, no Java code: `db/changelog/changelog-master.xml` `<includeAll>`s `changesets/`. Owns the schema and is applied out-of-band, by ops or a pipeline (`./mvnw liquibase:update -pl starter-schema`) — **no running application ever migrates the database**. `starter-api` and `starter-batch` depend on it at **test scope only** (never widen, never add it to `starter-core`), purely so their integration tests can migrate their own throwaway Testcontainers database against the real changelog before `ddl-auto: validate` checks it.
- `starter-batch` — second Spring Boot application over the same `starter-core`: `BatchApplication` (in the base package, so the component scan reaches the adapters), `batch/job/` (the chunk-oriented step, wired to ports only) and `batch/config/` (its composition root). Its metadata tables come from a `starter-schema` changeset, with `spring.batch.jdbc.initialize-schema: never`.
- `com.example.starter.generated.*` is build output of openapi-generator: never edit it, edit the spec and rebuild. Contract-first: the spec changes before the code.
- The hexagonal rules are law, enforced by [ArchitectureTest](starter-api/src/test/java/com/example/starter/ArchitectureTest.java), [PersistenceArchitectureTest](starter-api/src/test/java/com/example/starter/PersistenceArchitectureTest.java) and [BatchArchitectureTest](starter-batch/src/test/java/com/example/starter/BatchArchitectureTest.java): the domain sees only itself and the JDK, the inbound side (`application`/`infrastructure`/`batch`) never reaches the adapters, adapters use the domain only through `port`/`model`/`exception` (never `service`), `persistence` and `client` stay independent, only `Business`/`Technical` subclasses live in the exception subpackages, outbound ports are implemented only by adapters, only `application.controller`/`application.mapper` depend on generated code, and the top-level packages stay free of cycles.

## Optional modules

`schema` and `batch` are optional at generation time, so the repository must stay **severable**:

- A slice that belongs to an optional module lives in **its own files** — one configuration class, one ArchUnit test, one repository interface, one adapter per port group. Removing a module must never mean editing a surviving file's imports.
- What cannot be split into files (pom dependencies, `application.yml` blocks, OpenAPI paths, doc sections) is framed by markers the generator strips: `<!-- module:schema -->` … `<!-- /module:schema -->` in XML and Markdown, `# module:schema` … `# /module:schema` in YAML. Markers sit alone on their line, and **never inside a Java import block** — palantir reorders imports and would move the comment.
- `ApplicationIT` and `cucumber/CucumberSpringConfiguration` are the two files whose variants genuinely differ (with a database they need a container, without one they must boot without a datasource). Their without-schema variant lives in `.generator/without-schema/` and is rendered only then, overwriting the with-schema version at the same path — no separate `whenAbsent.remove` entry is strictly required for that overwrite to work, but one is kept anyway (mirroring `ApplicationIT`) so the pre-rename file never survives to be needlessly renamed and rewritten.
- ArchUnit fails a rule whose subject matches nothing, so a rule that only makes sense with persistence belongs in `PersistenceArchitectureTest`, not in `ArchitectureTest`.
- `batch` requires `schema` and the manifest says so: Spring Batch needs a JDBC job repository.
- A future business scenario gated by an optional module (a `position.feature` needing `schema`, say) follows the same rule as any other slice: its own `.feature` file and its own step-definition class, both added to that module's `whenAbsent.remove` — never a shared step class edited to drop scenarios.

## Conventions

- Everything in the repo is written in **English** (code, comments, docs, commit messages).
- Commits follow [Conventional Commits](https://www.conventionalcommits.org). No local git hooks: CI and release-please rely on the messages.
- Formatting is Spotless/palantir; records for immutable data; constructor injection without Lombok.
- Schema changes only through `starter-schema`'s Liquibase changesets (`ddl-auto: validate` will fail otherwise). Changeset ids are sequential and descriptive (`003-add-index`, author `starter`).
- Sibling modules are depended on through an explicit version property (`${starter-core.version}`), never `${project.version}` — that would silently mean the wrong thing once a module is extracted.

## Testing

- Naming drives the phase: `*Test` = surefire (unit, no Docker), `*IT` = failsafe (integration, Testcontainers).
- Controllers: `@WebMvcTest` + `@Import(SecurityConfig.class)` + `@MockitoBean` ports + `spring-security-test`'s `jwt()` post-processor (and a `@MockitoBean JwtDecoder` so the context starts).
- Persistence: `@DataJpaTest` + `@ServiceConnection` PostgreSQL container, schema generated from the JPA mapping (`spring.jpa.hibernate.ddl-auto=create-drop`, set locally on the test — the real changelog is exercised by the full-boot tests instead). `starter-core` has a test-only `TestApplication` (`@SpringBootConfiguration`) because it contains no Spring Boot app.
- Full boot: `ApplicationIT` (`starter-api`) and `RevaluePositionsJobIT` (`starter-batch`) migrate their Testcontainers database with the real `starter-schema` changelog (`@TestPropertySource` pointing `spring.liquibase.change-log` at it) before `ddl-auto: validate` runs.
- Business scenarios: `CucumberIT` (`starter-api`, `cucumber/` package) runs every `.feature` file under `src/test/resources/features/` over real HTTP through the full Spring context (`CucumberSpringConfiguration`), security opened up via `app.security.permit-all` — authentication itself is already covered by `ApplicationIT` and the controller unit tests. It is a `*IT` like any other: Failsafe runs it, Surefire does not. Add a feature by adding a `.feature` file plus a step-definition class in `cucumber/`; a `@Before` hook (`Hooks`) resets shared fixtures (the WireMock stub server) between scenarios. Cucumber glue classes must be `public`, unlike the rest of this test suite.
- Bean-wiring code (the batch step's reader/processor/writer, the `RestClient` configuration) is unit-tested by calling the `@Bean` methods directly, so the coverage gate does not depend on Docker being available.
- External clients: WireMockServer without any Spring context.
- ArchUnit rules are written as plain JUnit `@Test` methods over a static `ClassFileImporter` **on purpose**: the dedicated `archunit-junit5` engine reported 0 tests under this surefire setup — do not migrate back to `@AnalyzeClasses`/`@ArchTest`.
- Coverage gate: 70% lines per module (JaCoCo, merged unit+IT data). Intentionally lower than actual coverage; do not raise it.

## Generator

New applications are scaffolded from this starter by [starter-generator](https://github.com/JoanRoucoux/starter-generator), a generic engine: everything starter-specific lives **here**, in [generator.config.json](generator.config.json) (optional modules, removed files, module/package renames, tree-wide identity rewrites and marker-block removals — full spec in the generator's README) and `.generator/` (scaffolds rendered with `{{token}}` placeholders).

Keep them in sync with the starter:

- The `quote` and `position` demo features are **kept** in generated applications as reference implementations — do not add their files to the manifest's `remove`.
- When a file is added to a slice that belongs to an optional module, add it to that module's `whenAbsent.remove`. The `generate` CI job is what catches a miss: it builds the variants without them.
- The manifest's `rename` entries mirror the module directories and the source roots of the base package; entries belonging to an optional module carry a `"module"` field so they are skipped (rather than warned about) when it is left out. `replaceAll` rewrites `com.example.starter` and the `starter-*` artifactIds — in XML those are matched as **element values** (`>starter-batch<`) and property names (`starter-core.version`), never bare: a bare `starter-batch` would also rewrite `spring-boot-starter-batch`. Keep module names out of prose in the poms for the same reason.
- `.generator/templates/README.md`, `AGENTS.md` and `.github/workflows/ci.yml` overwrite this repo's versions in generated apps. When those files change here, re-check the templates. The generated `ci.yml` has **no `generate` job** (a generated app has no manifest).
- Template-only content (community files, release tooling) must be listed in the manifest's `remove`; new root-level files that should not ship in generated apps must be added there.
- Templates contain `{{tokens}}`: Spotless only targets `src/**/*.java`, so `.generator/` is naturally excluded — keep it that way.
- The `generate` job in [ci.yml](.github/workflows/ci.yml) generates applications from the working tree, in each module combination, and runs their quality gates — it fails when the manifest or templates drift.

## Gotchas

- **Run Maven on JDK 21–24**: palantir-java-format calls javac internals that changed in JDK 25 (`NoSuchMethodError` in Spotless). CI pins Temurin 21.
- The aggregator and `starter-schema` declare the Spotless plugin although they hold no Java: `spotless:check` from the root resolves the plugin prefix per project and fails on any project that lacks it.
- The table is named `positions` (plural): `POSITION` is a reserved word in PostgreSQL.
- **`starter-schema` stays a test-scope dependency of the application modules only** — never add it (or `liquibase-core`) to `starter-core`, and never widen its scope past `test`. An application must never be able to migrate the database itself; only the integration tests do, deliberately.
- Never produce a template path with a double-underscore word other than a generator token — `__word__` sequences in `.generator/` paths are parsed as path tokens.
- `mvnw` must stay executable on Linux CI: `.gitattributes` forces LF and the file mode is committed (`git update-index --chmod=+x mvnw` after a fresh checkout on Windows if git loses it).
- On Windows, deep module paths can hit MAX_PATH: enable `git config core.longpaths true` if needed.
- GitHub Actions in `.github/workflows/` are pinned by commit SHA (Dependabot keeps them updated) — when adding one, pin it the same way.
- **`cucumber-junit-platform-engine` must stay pinned to a version built against the same `junit-jupiter` line Spring Boot manages** (`starter-api/pom.xml`'s `cucumber.version` comment explains why): a newer Cucumber needs a newer JUnit Platform than the one this project's dependency management provides, and fails at test discovery with `NoClassDefFoundError` — this project's own `dependencyManagement` always wins the resolved version over Cucumber's, never the other way round. Re-check this pairing before bumping either Spring Boot or Cucumber.
- **`CucumberIT` always reports "Tests run: 0"** in the Surefire/Failsafe console summary — Cucumber's dynamic per-scenario test tree isn't counted the same way as JUnit Jupiter's. This is cosmetic, not a sign the suite didn't run: `cucumber.plugin=pretty,summary` (`junit-platform.properties`) prints the real scenario/step counts right above it, and a genuine scenario failure still fails the build.
- Nothing is committed or pushed without an explicit request from the maintainer.
