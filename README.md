<img src=".github/assets/banner.png" alt="Java Starter: Spring Boot starter in hexagonal architecture" />

[![CI](https://github.com/JoanRoucoux/java-starter/actions/workflows/ci.yml/badge.svg)](https://github.com/JoanRoucoux/java-starter/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/JoanRoucoux/java-starter)](https://github.com/JoanRoucoux/java-starter/releases)
[![Java](https://img.shields.io/badge/Java-25-orange)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-brightgreen)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Spring Boot starter for backends in **hexagonal architecture**, built as **separate modules you pick from**: `-domain` (framework-free model, ports, services), `-adapter` (their implementations) and `-api` (Spring Boot main + REST exposition) always, plus `-schema` (Liquibase changelogs and the PostgreSQL persistence they own) when there is a database, `-batch` (Spring Batch over the same hexagon) when there are jobs, and `-kafka` (a worker consuming the events the API publishes) when other systems react to what it writes. Built to back a frontend application (see [angular-starter-web](https://github.com/JoanRoucoux/angular-starter-web)) and attach to a database and/or one or more external APIs.

There is **no parent pom**. The `pom.xml` at the root only aggregates: no module inherits from it; each one is parented by `spring-boot-starter-parent` and carries its own dependencies and quality gates. A module can therefore be dropped, or moved to its own repository, without touching anything else.

## The starters

| Repository                                                                | What it generates                                         |
| ------------------------------------------------------------------------- | --------------------------------------------------------- |
| [angular-starter-web](https://github.com/JoanRoucoux/angular-starter-web) | An Angular web application connected to a backend         |
| [angular-starter-ui](https://github.com/JoanRoucoux/angular-starter-ui)   | An Angular design system: component library and Storybook |
| [java-starter](https://github.com/JoanRoucoux/java-starter) (this one)    | A Spring Boot backend in hexagonal architecture           |

Create an application from any of them with [starter-generator](https://github.com/JoanRoucoux/starter-generator).

## Tech stack

| Tool                                                                                                                 | Role                                                                                        |
| -------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| [Spring Boot](https://spring.io/projects/spring-boot) / Java                                                         | Application framework, Maven modules with wrapper                                           |
| [openapi-generator](https://openapi-generator.tech) (contract-first)                                                 | `openapi/openapi.yaml` -> generated interfaces + DTOs                                       |
| Spring Security (OAuth2 resource server)                                                                             | Stateless JWT validation                                                                    |
| Spring Data JPA + PostgreSQL                                                                                         | Persistence adapter (`schema` module)                                                       |
| [Liquibase](https://www.liquibase.com) (`-schema` module)                                                            | Versioned changelogs, applied out-of-band; never by an app                                  |
| Spring Batch (`-batch` module)                                                                                       | Chunk-oriented jobs over the same domain as the API                                         |
| Spring Kafka (`-kafka` module)                                                                                       | `position.opened` event published by the API, consumed by a worker                          |
| RestClient                                                                                                           | External API client adapter (timeouts via properties)                                       |
| Micrometer Tracing + ECS JSON                                                                                        | One log line per use case, trace ids; `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs` in production |
| [Testcontainers](https://testcontainers.com), [WireMock](https://wiremock.org), [ArchUnit](https://www.archunit.org) | Integration tests, client tests, architecture enforcement                                   |
| [Cucumber](https://cucumber.io)                                                                                      | Business-scenario acceptance tests, over real HTTP                                          |
| [Spotless](https://github.com/diffplug/spotless) (palantir-java-format), JaCoCo                                      | Formatting and coverage gates                                                               |

## Getting started

Generate an application from this starter with [starter-generator](https://github.com/JoanRoucoux/starter-generator), which asks which optional modules you want:

```bash
starter-generator portfolio --starter java --base-package com.acme.portfolio \
  --modules schema,batch,kafka --openapi ../specs/portfolio.yaml
```

`--modules ""` generates an API with no database at all: the persistence adapter, the JPA dependencies and the position demo go away, and what remains is a service orchestrating external APIs. `--modules schema` is the default. `batch` and `kafka` require `schema` (the demo job and the worker read positions).

Module directories, Maven artifactIds and the Java base package take your application's identity at generation time. The demo features are **kept** as reference implementations; replace them with your own. If you provide `--openapi`, align the controllers with the regenerated interfaces afterwards (the compile errors point the way).

Then run it. Prerequisites: **JDK 25** and **Docker** (PostgreSQL via Testcontainers and Docker Compose). Maven comes with the wrapper.

```bash
./mvnw verify                             # build + unit/integration tests + architecture + coverage
docker compose up -d --wait               # starts PostgreSQL and applies the schema (compose.yaml)
./mvnw spring-boot:run -pl starter-api    # the API on :8080
```

### Commands

| Command                                                                               | Description                                                                                                    |
| ------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| `./mvnw verify`                                                                       | Build + unit/integration tests + architecture + coverage                                                       |
| `docker compose up -d --wait`                                                         | Starts PostgreSQL and applies the schema (compose.yaml); no app ever migrates it itself                        |
| `./mvnw spring-boot:run -pl starter-api`                                              | Starts the same stack when it is down, then the API on :8080                                                   |
| `./mvnw spring-boot:run -pl starter-api -Dspring-boot.run.profiles=local`             | Same, with authentication disabled (no identity provider needed)                                               |
| `./mvnw spring-boot:run -pl starter-batch`                                            | Runs the demo job, then exits                                                                                  |
| `./mvnw spring-boot:run -pl starter-kafka`                                            | Starts the broker (compose profile `kafka`) and the worker                                                     |
| `docker compose up -d --build schema` or `./mvnw liquibase:update -pl starter-schema` | Applies a new changeset                                                                                        |
| `docker build -f starter-api/Dockerfile .`                                            | Builds a module's image from the repository root (likewise `starter-batch`, `starter-kafka`, `starter-schema`) |

The `schema` service of the compose file migrates PostgreSQL from the repository. Starting or restarting an application never touches the schema.

## Project structure

```
pom.xml                  Aggregator only: <modules>, no inheritance
compose.yaml             Local PostgreSQL, schema migration, Kafka broker (profile kafka)
starter-domain/          model/, exception/ (business/ holds BusinessException + its subclasses,
                         technical/ holds TechnicalException + its), port/in/ (use cases),
                         port/out/ (repositories, external providers), service/ (plain Java,
                         ZERO dependencies: a Maven guarantee, not just a convention)
starter-adapter/         persistence/ (entity/, repository/, adapter/),
                         client/ (properties/, config/, adapter/); depends on starter-domain
starter-api/             Spring Boot application: REST exposition
|-- openapi/openapi.yaml The REST contract (source of truth, edited first)
|-- application/         controller/ (implements the generated interfaces), mapper/ (domain<->DTO,
|                        one class per resource), exception/ (@RestControllerAdvice -> problem details)
|-- infrastructure/      config/ (SecurityConfig, one XxxDomainConfig per slice; the composition root)
`-- generated/           openapi build output (never edited, never committed)
starter-schema/          Liquibase changelogs (db/changelog/): owns the schema, no Java code
starter-batch/           Spring Boot application: Spring Batch jobs over starter-domain/starter-adapter
starter-kafka/           Spring Boot worker: consumes the position.opened events the API publishes
```

Dependency rules:

- `starter-domain` depends on nothing but the JDK; no Spring, no JPA. Enforced by Maven itself: there is nothing on its classpath to depend on.
- `starter-adapter` implements the domain's outbound ports and reaches the domain only through its ports, model and exceptions; never the domain services (ArchUnit). `persistence` and `client` never see each other (ArchUnit).
- `starter-api`, `starter-batch` and `starter-kafka` depend on `starter-adapter` at **runtime scope only**: adapters are wired into the context but invisible at compile time, so none of the applications can reach adapter internals, even by accident.
- Errors map by family in the `@RestControllerAdvice`: `BusinessException` -> 422, `TechnicalException` -> 502 (authentication and authorization, 401/403, are handled by Spring Security's filter chain).
- `starter-schema` is applied out-of-band (ops or pipeline, `liquibase:update`); a running application **never** migrates the database. `starter-api`, `starter-batch` and `starter-kafka` depend on it at **test scope only**, so their integration tests can migrate their own throwaway Testcontainers database with the real changelog.

## The demo features

Two complete hexagonal slices, both meant to be replaced:

- **`quote`**: `GET /quote/{isin}` reads an instrument's price from an external provider: controller -> mapper -> inbound port -> domain service -> outbound port -> `RestClient` adapter, with an unknown instrument surfacing as 422 and a failing provider as 502. It needs no database, so it is the slice that survives in an application generated without `schema`.
- **`position`**: `POST /position` and `GET /position/{id}` add persistence: JPA entity, repository and adapter behind read/write ports, backed by a Liquibase changeset. The `-batch` module's `revaluePositionsJob` walks the same positions through the same domain, proving the API and the batch share one hexagon.

## Contract-first workflow

1. Edit `starter-api/openapi/openapi.yaml` (the contract comes first).
2. `./mvnw compile` regenerates the interfaces and DTOs (`*.generated.api`, `*.generated.model`: build output, never edited, never committed).
3. Implement the new interface methods in a controller, mapping DTOs to the domain through the inbound ports.

The same spec can drive the frontend's generated client (Orval in angular-starter-web).

## Testing

- **Unit tests** (`*Test`, surefire): domain services with plain JUnit/Mockito, controllers with `@WebMvcTest` + `spring-security-test` (`jwt()`), external clients and the batch step's glue without any Spring context.
- **Integration tests** (`*IT`, failsafe): persistence adapters with `@DataJpaTest` + Testcontainers (schema generated from the JPA mapping, `ddl-auto: create-drop`, local to those tests); full application boot, the real job and the ECS log output with `@SpringBootTest` (the real `starter-schema` changelog is migrated first, so `ddl-auto: validate` has a schema to check; security answers 401 without contacting any IdP).
- **Business scenarios** ([CucumberIT](starter-api/src/test/java/com/example/starter/cucumber/CucumberIT.java), failsafe): `.feature` files under `starter-api/src/test/resources/features/` run over real HTTP through the full Spring context, with security opened up and the market data provider stubbed with WireMock; `quote.feature` is the reference scenario for adding your own as features grow.
- **Architecture** ([ArchitectureTest](starter-api/src/test/java/com/example/starter/ArchitectureTest.java) and its counterparts): the rules above, checked on every build.
- **Coverage**: JaCoCo gate at 70% lines per module, intentionally lower than the starter's actual coverage so generated applications are not blocked from day one.

## Quality and conventions

- Everything in the repo is written in **English** (code, docs, commit messages).
- Formatting: Spotless with palantir-java-format: `./mvnw spotless:apply` / `spotless:check`. A [lefthook](https://lefthook.dev) pre-commit hook runs `spotless:apply` and re-stages the result automatically (`lefthook install` once after cloning).
- Commits follow [Conventional Commits](https://www.conventionalcommits.org): CI and release-please rely on them.
- `spring.jpa.hibernate.ddl-auto: validate`: the schema only changes through `starter-schema`'s Liquibase changelogs, applied out-of-band (never by an application).

## Contributing

See [CONTRIBUTING.md](.github/CONTRIBUTING.md) for the contribution workflow and [CODE_OF_CONDUCT.md](.github/CODE_OF_CONDUCT.md) for community guidelines. To report a vulnerability, see [SECURITY.md](.github/SECURITY.md).

## License

This project is licensed under [MIT](LICENSE).
