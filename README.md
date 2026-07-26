# java-starter

[![CI](https://github.com/JoanRoucoux/java-starter/actions/workflows/ci.yml/badge.svg)](https://github.com/JoanRoucoux/java-starter/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Spring Boot starter for backends in **hexagonal architecture**, built as **separate modules you pick from**: `-api` (Spring Boot main + REST exposition) and `-core` (the hexagon: framework-free domain and its outbound adapters) always, plus `-schema` (Liquibase changelogs and the PostgreSQL persistence they own) when there is a database, and `-batch` (Spring Batch over the same hexagon) when there are jobs. Built to back a frontend application (see [angular-starter-web](https://github.com/JoanRoucoux/angular-starter-web)) and attach to a database and/or one or more external APIs.

There is **no parent pom**. The `pom.xml` at the root only aggregates: no module inherits from it, each one is parented by `spring-boot-starter-parent` and carries its own dependencies and quality gates. A module can therefore be dropped — or moved to its own repository — without touching anything else.

## Stack

| Tool                                     | Role                                                         |
| ---------------------------------------- | ------------------------------------------------------------ |
| Spring Boot 3.5 / Java 21                | Application framework, Maven modules with wrapper            |
| openapi-generator (contract-first)       | `openapi/openapi.yaml` → generated interfaces + DTOs         |
| Spring Security (OAuth2 resource server) | Stateless JWT validation                                     |
| Spring Data JPA + PostgreSQL             | Persistence adapter (`schema` module)                        |
| Liquibase (`-schema` module)             | Versioned changelogs, applied out-of-band — never by an app  |
| Spring Batch (`-batch` module)           | Chunk-oriented jobs over the same domain as the API          |
| RestClient                               | External API client adapter (timeouts via properties)        |
| Testcontainers, WireMock, ArchUnit       | Integration tests, client tests, architecture enforcement    |
| Cucumber                                 | Business-scenario acceptance tests, over real HTTP           |
| Spotless (palantir-java-format), JaCoCo  | Formatting and coverage gates                                |

## Generating an application

New applications are scaffolded with [starter-generator](https://github.com/JoanRoucoux/starter-generator), which asks which optional modules you want:

```bash
starter-generator portfolio --starter java --base-package com.acme.portfolio \
  --modules schema,batch --openapi ../specs/portfolio.yaml
```

`--modules ""` generates an API with no database at all: the persistence adapter, the JPA dependencies and the position demo go away, and what remains is a service orchestrating external APIs. `--modules schema` is the default. `batch` requires `schema` (Spring Batch needs a JDBC job repository, whose tables the schema module owns).

Module directories, Maven artifactIds and the Java base package take your application's identity at generation time. The demo features are **kept** as reference implementations — replace them with your own. If you provide `--openapi`, align the controllers with the regenerated interfaces afterwards (the compile errors point the way).

## Getting started

Prerequisites: **JDK 21** and **Docker** (PostgreSQL via Testcontainers and Docker Compose). Maven comes with the wrapper.

```bash
./mvnw verify                                # build + unit/integration tests + architecture + coverage
./mvnw liquibase:update -pl starter-schema   # migrates the local PostgreSQL (compose.yaml) — no app ever does this itself
./mvnw spring-boot:run -pl starter-api       # starts PostgreSQL (compose.yaml) and the API on :8080
./mvnw spring-boot:run -pl starter-batch     # runs the demo job, then exits
```

`liquibase:update` only needs to run once (and again after adding a changeset) — starting or restarting an application does not touch the schema. Without an identity provider, activate the `local` profile to disable authentication: `./mvnw spring-boot:run -pl starter-api -Dspring-boot.run.profiles=local`.

## Project structure

```
pom.xml                  Aggregator only: <modules>, no inheritance
compose.yaml             Local PostgreSQL
starter-core/            The hexagon — a library, no Spring Boot application
├── domain/              model/, exception/ (business/ holds BusinessException + its subclasses,
│                        technical/ holds TechnicalException + its), port/in/ (use cases),
│                        port/out/ (repositories, external providers), service/ — plain Java
└── adapter/             persistence/ (entity/, repository/, adapter/),
                         client/ (properties/, config/, adapter/)
starter-api/             Spring Boot application: REST exposition
├── openapi/openapi.yaml The REST contract (source of truth, edited first)
├── application/         controller/ (implements the generated interfaces), mapper/ (domain↔DTO,
│                        one class per resource), exception/ (@RestControllerAdvice → problem details)
├── infrastructure/      config/ (SecurityConfig, one XxxDomainConfig per slice — the composition root)
└── generated/           openapi build output (never edited, never committed)
starter-schema/          Liquibase changelogs (db/changelog/) — owns the schema, no Java code
starter-batch/           Spring Boot application: Spring Batch jobs over starter-core
```

Dependency rules, enforced by ArchUnit on every build:

- `domain` depends on nothing but the JDK — no Spring, no JPA.
- `adapter` implements the domain's outbound ports and reaches the domain only through its ports, model and exceptions — never the domain services. `persistence` and `client` never see each other.
- The inbound side (`application`, `infrastructure`, and the batch's `job`) depends on the inbound ports, never on an adapter or on a service implementation. Since `domain` and `adapter` share one module, this rule — not a Maven scope — is what keeps the REST and batch sides out of adapter internals.
- Errors map by family in the `@RestControllerAdvice`: `BusinessException` → 422, `TechnicalException` → 502 (authentication and authorization — 401/403 — are handled by Spring Security's filter chain).
- `starter-schema` is applied out-of-band (ops or pipeline, `liquibase:update`) — a running application **never** migrates the database. `starter-api` and `starter-batch` depend on it at **test scope only**, so their integration tests can migrate their own throwaway Testcontainers database with the real changelog.

## The demo features

Two complete hexagonal slices, both meant to be replaced:

- **`quote`** — `GET /quote/{isin}` reads an instrument's price from an external provider: controller → mapper → inbound port → domain service → outbound port → `RestClient` adapter, with an unknown instrument surfacing as 422 and a failing provider as 502. It needs no database, so it is the slice that survives in an application generated without `schema`.
- **`position`** — `POST /position` and `GET /position/{id}` add persistence: JPA entity, repository and adapter behind read/write ports, backed by a Liquibase changeset. The `-batch` module's `revaluePositionsJob` walks the same positions through the same domain, proving the API and the batch share one hexagon.

## Contract-first workflow

1. Edit `starter-api/openapi/openapi.yaml` (the contract comes first).
2. `./mvnw compile` regenerates the interfaces and DTOs (`*.generated.api`, `*.generated.model` — build output, never edited, never committed).
3. Implement the new interface methods in a controller, mapping DTOs to the domain through the inbound ports.

The same spec can drive the frontend's generated client (Orval in angular-starter-web).

## Testing

- **Unit tests** (`*Test`, surefire): domain services with plain JUnit/Mockito, controllers with `@WebMvcTest` + `spring-security-test` (`jwt()`), external clients and the batch step's glue without any Spring context.
- **Integration tests** (`*IT`, failsafe): persistence adapters with `@DataJpaTest` + Testcontainers (schema generated from the JPA mapping — `ddl-auto: create-drop`, local to those tests); full application boot and the real job with `@SpringBootTest` (the real `starter-schema` changelog is migrated first, so `ddl-auto: validate` has a schema to check; security answers 401 without contacting any IdP).
- **Business scenarios** ([CucumberIT](starter-api/src/test/java/com/example/starter/cucumber/CucumberIT.java), failsafe): `.feature` files under `starter-api/src/test/resources/features/` run over real HTTP through the full Spring context, with security opened up and the market data provider stubbed with WireMock — `quote.feature` is the reference scenario for adding your own as features grow.
- **Architecture** ([ArchitectureTest](starter-api/src/test/java/com/example/starter/ArchitectureTest.java) and its counterparts): the rules above, checked on every build.
- **Coverage**: JaCoCo gate at 70% lines per module — intentionally lower than the starter's actual coverage so generated applications are not blocked from day one.

## Quality and conventions

- Everything in the repo is written in **English** (code, comments, docs, commit messages).
- Formatting: Spotless with palantir-java-format — `./mvnw spotless:apply` / `spotless:check`. Run Maven on JDK 21–24 (palantir-java-format does not support the JDK 25 javac internals yet; CI pins 21).
- Commits follow [Conventional Commits](https://www.conventionalcommits.org). There are no local git hooks: CI and release-please are the gates.
- `spring.jpa.hibernate.ddl-auto: validate` — the schema only changes through `starter-schema`'s Liquibase changelogs, applied out-of-band (never by an application).

## Contributing

See [CONTRIBUTING.md](.github/CONTRIBUTING.md) for the contribution workflow and [CODE_OF_CONDUCT.md](.github/CODE_OF_CONDUCT.md) for community guidelines. To report a vulnerability, see [SECURITY.md](.github/SECURITY.md).

## License

This project is licensed under [MIT](LICENSE).
