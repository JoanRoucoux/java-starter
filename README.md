# java-starter-api

[![CI](https://github.com/JoanRoucoux/java-starter-api/actions/workflows/ci.yml/badge.svg)](https://github.com/JoanRoucoux/java-starter-api/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Spring Boot starter for backend APIs in **hexagonal architecture**: an `-api` module (Spring Boot main + REST exposition), a `-core` module holding `-domain` (model, ports, use cases — framework-free) and `-adapter` (PostgreSQL persistence, external API clients), and a `-schema` module (Liquibase changelogs, owned and applied independently of the running app). Built to back a frontend application (see [angular-starter-web](https://github.com/JoanRoucoux/angular-starter-web)) and attach to a database and/or one or more external APIs.

## Stack

| Tool                                       | Role                                                        |
| ------------------------------------------ | ----------------------------------------------------------- |
| Spring Boot 3.5 / Java 21                  | Application framework, Maven multi-module with wrapper      |
| openapi-generator (contract-first)         | `openapi/openapi.yaml` → generated interfaces + DTOs        |
| Spring Security (OAuth2 resource server)   | Stateless JWT validation                                    |
| Spring Data JPA + PostgreSQL               | Persistence adapter                                          |
| Liquibase (`-schema` module)               | Versioned changelogs, applied out-of-band — never by the app |
| RestClient                                 | External API client adapter (timeouts via properties)       |
| Testcontainers, WireMock, ArchUnit         | Integration tests, client tests, architecture enforcement   |
| Spotless (palantir-java-format), JaCoCo    | Formatting and coverage gates                               |

## Generating an application

New applications are scaffolded with [starter-generator](https://github.com/JoanRoucoux/starter-generator):

```bash
starter-generator portfolio-api --starter java --base-package com.acme.portfolio \
  --openapi ../specs/portfolio-api.yaml
```

Module directories, Maven artifactIds and the Java base package are renamed to your application's identity at generation time. The `position` demo feature is **kept** in generated applications as a reference implementation of a full hexagonal slice — replace it with your own features. If you provide `--openapi`, align `PositionController` with the regenerated interfaces afterwards (the compile errors point the way).

## Getting started

Prerequisites: **JDK 21** and **Docker** (PostgreSQL via Testcontainers and Docker Compose). Maven comes with the wrapper.

```bash
./mvnw verify                                # build + unit/integration tests + architecture + coverage
./mvnw liquibase:update -pl starter-schema   # migrates the local PostgreSQL (compose.yaml) — the app never does this itself
./mvnw spring-boot:run -pl starter-api       # starts PostgreSQL (compose.yaml) and the API on :8080
```

`liquibase:update` only needs to run once (and again after adding a changeset) — starting or restarting the app does not touch the schema. Without an identity provider, activate the `local` profile to disable authentication: `./mvnw spring-boot:run -pl starter-api -Dspring-boot.run.profiles=local`.

## Project structure

```
pom.xml                  Parent: BOM, plugin management, quality gates
openapi/openapi.yaml     The REST contract (source of truth, edited first)
compose.yaml             Local PostgreSQL
starter-api/             Spring Boot main
├── application/         controller/ (implements the generated interfaces), mapper/ (domain↔DTO,
│                        one class per resource), exception/ (@RestControllerAdvice → problem details)
├── infrastructure/      config/ (SecurityConfig, DomainConfig — the composition root)
└── generated/           openapi build output (never edited, never committed)
starter-core/
├── starter-domain/      model/, exception/ (business/ holds BusinessException + its
│                        subclasses, technical/ holds TechnicalException + its subclasses),
│                        port/in/ (use cases), port/out/ (read/write ports), service/ — pure Java,
│                        ZERO framework dependency
└── starter-adapter/     persistence/ (entity/, repository/, adapter/), client/ (properties/, config/, adapter/)
starter-schema/         Liquibase changelogs (db/changelog/) — owns the schema, no Java code
```

Dependency rules (enforced by ArchUnit and the Maven scopes):

- `domain` depends on nothing but the JDK.
- `adapter` implements the domain's outbound ports; it reaches the domain only through its ports, model and exceptions — never the domain services.
- `api` (`application` inbound side + `infrastructure` wiring) depends on the domain's inbound ports; the adapters are wired at **runtime scope** so the REST side cannot reach adapter internals even by accident.
- Errors map by family in the `@RestControllerAdvice`: `BusinessException` → 422, `TechnicalException` → 502 (authentication/authorization — 401/403 — are handled by Spring Security's filter chain).
- `starter-schema` is applied out-of-band (ops/pipeline, `liquibase:update`) — the running app **never** migrates the database itself. `starter-api` depends on it at **test scope only**, so `ApplicationIT` can migrate its own throwaway Testcontainers database with the real changelog.

## Contract-first workflow

1. Edit `openapi/openapi.yaml` (the contract comes first).
2. `./mvnw compile` regenerates the interfaces and DTOs (`*.generated.api`, `*.generated.model` — build output, never edited, never committed).
3. Implement the new interface methods in a controller, mapping DTOs to the domain through the inbound ports.

The same spec can drive the frontend's generated client (Orval in angular-starter-web).

## Testing

- **Unit tests** (`*Test`, surefire): domain services with plain JUnit/Mockito, controllers with `@WebMvcTest` + `spring-security-test` (`jwt()`), external clients against WireMock — no Spring context.
- **Integration tests** (`*IT`, failsafe): persistence adapter with `@DataJpaTest` + Testcontainers (schema generated from the JPA mapping — `ddl-auto: create-drop`, local to that test); full application boot with `@SpringBootTest` (the real `starter-schema` changelog is migrated first, so `ddl-auto: validate` has a schema to check; security answers 401 without contacting any IdP).
- **Architecture** ([ArchitectureTest](starter-api/src/test/java/com/example/starter/ArchitectureTest.java)): the hexagonal rules above, checked on every build.
- **Coverage**: JaCoCo gate at 70% lines per module — intentionally lower than the starter's actual coverage so generated applications are not blocked from day one.

## Quality and conventions

- Everything in the repo is written in **English** (code, comments, docs, commit messages).
- Formatting: Spotless with palantir-java-format — `./mvnw spotless:apply` / `spotless:check`. Run Maven on JDK 21–24 (palantir-java-format does not support the JDK 25 javac internals yet; CI pins 21).
- Commits follow [Conventional Commits](https://www.conventionalcommits.org). There are no local git hooks: CI and release-please are the gates.
- `spring.jpa.hibernate.ddl-auto: validate` — the schema only changes through `starter-schema`'s Liquibase changelogs, applied out-of-band (never by the app).

## Contributing

See [CONTRIBUTING.md](.github/CONTRIBUTING.md) for the contribution workflow and [CODE_OF_CONDUCT.md](.github/CODE_OF_CONDUCT.md) for community guidelines. To report a vulnerability, see [SECURITY.md](.github/SECURITY.md).

## License

This project is licensed under [MIT](LICENSE).
