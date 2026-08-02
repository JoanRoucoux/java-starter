# {{title}}

{{title}} — Spring Boot backend in hexagonal architecture, generated from [java-starter](https://github.com/JoanRoucoux/java-starter){{starterVersion}} with these modules: **{{modules}}**.

There is no parent pom: the root `pom.xml` only aggregates, and every module is a standalone Maven project parented by `spring-boot-starter-parent`. A module can be moved to its own repository as-is.

## Stack

| Tool                                     | Role                                                        |
| ---------------------------------------- | ----------------------------------------------------------- |
| Spring Boot 3.5 / Java 25                | Application framework, Maven modules with wrapper           |
| openapi-generator (contract-first)       | `{{appName}}-api/openapi/openapi.yaml` → interfaces + DTOs  |
| Spring Security (OAuth2 resource server) | Stateless JWT validation                                    |
| RestClient                               | External API client adapter (timeouts via properties)       |
<!-- module:schema -->
| Spring Data JPA + PostgreSQL             | Persistence adapter                                         |
| Liquibase (`{{appName}}-schema`)         | Versioned changelogs, applied out-of-band — never by an app |
<!-- /module:schema -->
<!-- module:batch -->
| Spring Batch (`{{appName}}-batch`)       | Chunk-oriented jobs over the same domain as the API         |
<!-- /module:batch -->
| Testcontainers, WireMock, ArchUnit       | Integration tests, client tests, architecture enforcement   |
| Cucumber                                 | Business-scenario acceptance tests, over real HTTP          |

## Getting started

Prerequisites: **JDK 25** and **Docker**. Maven comes with the wrapper (`./mvnw`, `mvnw.cmd` on Windows cmd).

```bash
./mvnw verify                                        # build + unit/integration tests + architecture + coverage
./mvnw spring-boot:run -pl {{appName}}-api           # starts the API on :8080
```

<!-- module:schema -->
The database schema is applied separately, and only when it changes:

```bash
./mvnw liquibase:update -pl {{appName}}-schema       # migrates the local PostgreSQL (compose.yaml)
```

`liquibase:update` only needs to run once, and again after adding a changeset — starting or restarting an application never touches the schema.

<!-- /module:schema -->
<!-- module:batch -->
The demo job runs on demand and exits when it is done:

```bash
./mvnw spring-boot:run -pl {{appName}}-batch
```

<!-- /module:batch -->
Without an identity provider, activate the `local` profile to disable authentication: `./mvnw spring-boot:run -pl {{appName}}-api -Dspring-boot.run.profiles=local`.

## Project structure

```
pom.xml                    Aggregator only: <modules>, no inheritance
{{appName}}-domain/        model/, exception/ (business/ holds BusinessException + its subclasses,
                           technical/ holds TechnicalException + its), port/in/ (use cases),
                           port/out/ (external providers, repositories), service/ — plain Java,
                           ZERO dependencies (a Maven guarantee, not just a convention)
{{appName}}-adapter/       client/ (properties/, config/, adapter/), persistence/ where applicable —
                           depends on {{appName}}-domain
{{appName}}-api/           Spring Boot application: REST exposition
├── openapi/openapi.yaml   The REST contract (source of truth, edited first)
├── application/           controller/ (implements the generated interfaces), mapper/ (domain↔DTO,
│                          one class per resource), exception/ (@RestControllerAdvice)
├── infrastructure/        config/ (SecurityConfig, one XxxDomainConfig per slice)
└── generated/             openapi build output (never edited, never committed)
<!-- module:schema -->
{{appName}}-schema/        Liquibase changelogs (db/changelog/) — owns the schema, no Java code
<!-- /module:schema -->
<!-- module:batch -->
{{appName}}-batch/         Spring Boot application: Spring Batch jobs over {{appName}}-domain/{{appName}}-adapter
<!-- /module:batch -->
```

Dependency rules: `{{appName}}-domain` depends on nothing but the JDK (a Maven guarantee); `{{appName}}-adapter` implements the domain's outbound ports and reaches the domain only through its ports, model and exceptions (ArchUnit); `{{appName}}-api`/`{{appName}}-batch` depend on `{{appName}}-adapter` at **runtime scope only**, so neither can reach adapter internals even by accident. Errors map by family in the `@RestControllerAdvice` — `BusinessException` → 422, `TechnicalException` → 502; authentication and authorization (401/403) are handled by Spring Security.

<!-- module:schema -->
`{{appName}}-schema` is applied out-of-band (ops or pipeline, `liquibase:update`) — a running application **never** migrates the database itself. The application modules depend on it at **test scope only**, so their integration tests can migrate their own throwaway Testcontainers database with the real changelog.

<!-- /module:schema -->
The demo features are reference implementations of a full hexagonal slice — use them as the model for your own, then replace them.

## Contract-first workflow

1. Edit `{{appName}}-api/openapi/openapi.yaml` (the contract comes first).
2. `./mvnw compile` regenerates the interfaces and DTOs (`{{basePackage}}.generated.*` — build output, never edited).
3. Implement the new interface methods in a controller, mapping DTOs to the domain through the inbound ports.

## Testing

- **Unit tests** (`*Test`, surefire): domain services with plain JUnit/Mockito, controllers with `@WebMvcTest` + `jwt()`, external clients against WireMock.
- **Integration tests** (`*IT`, failsafe): full application boot with `@SpringBootTest`, and Testcontainers PostgreSQL wherever a database is involved.
- **Business scenarios** (`CucumberIT`, failsafe): `.feature` files under `{{appName}}-api/src/test/resources/features/` run over real HTTP through the full Spring context — `quote.feature` is the reference scenario for adding your own.
- **Architecture**: the hexagonal rules, checked on every build.
- **Coverage**: JaCoCo gate at 70% lines per module.

## Quality and conventions

- Formatting: Spotless with palantir-java-format — `./mvnw spotless:apply` / `spotless:check`. A [lefthook](https://lefthook.dev) pre-commit hook runs `spotless:apply` and re-stages the result automatically (`lefthook install` once after cloning).
- Commits follow [Conventional Commits](https://www.conventionalcommits.org).
<!-- module:schema -->
- Schema changes only through `{{appName}}-schema`'s Liquibase changelogs, applied out-of-band (`ddl-auto: validate` — never by an application).
<!-- /module:schema -->
