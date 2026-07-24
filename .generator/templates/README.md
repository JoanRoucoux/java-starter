# {{title}}

{{title}} — Spring Boot API in hexagonal architecture, generated from [java-starter-api](https://github.com/JoanRoucoux/java-starter-api){{starterVersion}}.

## Stack

| Tool                                     | Role                                                      |
| ---------------------------------------- | --------------------------------------------------------- |
| Spring Boot 3.5 / Java 21                | Application framework, Maven multi-module with wrapper    |
| openapi-generator (contract-first)       | `openapi/openapi.yaml` → generated interfaces + DTOs      |
| Spring Security (OAuth2 resource server) | Stateless JWT validation                                  |
| Spring Data JPA + PostgreSQL             | Persistence adapter                                        |
| Liquibase (`{{appName}}-schema` module)  | Versioned changelogs, applied out-of-band — never by the app |
| RestClient                               | External API client adapter (timeouts via properties)     |
| Testcontainers, WireMock, ArchUnit       | Integration tests, client tests, architecture enforcement |

## Getting started

Prerequisites: **JDK 21** and **Docker**. Maven comes with the wrapper (`./mvnw`, `mvnw.cmd` on Windows cmd).

```bash
./mvnw verify                                    # build + unit/integration tests + architecture + coverage
./mvnw liquibase:update -pl {{appName}}-schema   # migrates the local PostgreSQL (compose.yaml) — the app never does this itself
./mvnw spring-boot:run -pl {{appName}}-api       # starts PostgreSQL (compose.yaml) and the API on :8080
```

`liquibase:update` only needs to run once (and again after adding a changeset) — starting or restarting the app does not touch the schema. Without an identity provider, activate the `local` profile to disable authentication: `./mvnw spring-boot:run -pl {{appName}}-api -Dspring-boot.run.profiles=local`.

## Project structure

```
pom.xml                    Parent: BOM, plugin management, quality gates
openapi/openapi.yaml       The REST contract (source of truth, edited first)
compose.yaml               Local PostgreSQL
{{appName}}-api/           Spring Boot main
├── application/           controller/ (implements the generated interfaces), mapper/ (domain↔DTO,
│                          one class per resource), exception/ (@RestControllerAdvice → problem details)
├── infrastructure/        config/ (SecurityConfig, DomainConfig — the composition root)
└── generated/             openapi build output (never edited, never committed)
{{appName}}-core/
├── {{appName}}-domain/    model/, exception/ (business/ holds BusinessException + its
│                          subclasses, technical/ holds TechnicalException + its subclasses),
│                          port/in/ (use cases), port/out/ (read/write ports), service/ — pure Java,
│                          ZERO framework dependency
└── {{appName}}-adapter/   persistence/ (entity/, repository/, adapter/), client/ (properties/, config/, adapter/)
{{appName}}-schema/       Liquibase changelogs (db/changelog/) — owns the schema, no Java code
```

Dependency rules (enforced by ArchUnit and the Maven scopes): the domain depends on nothing but the JDK; adapters implement the domain's outbound ports and reach the domain only through its ports, model and exceptions; the api module (`application` + `infrastructure`) sees only the inbound ports (the adapters are wired at runtime scope). Errors map by family in the `@RestControllerAdvice` — `BusinessException` → 422, `TechnicalException` → 502; authentication/authorization (401/403) is handled by Spring Security. `{{appName}}-schema` is applied out-of-band (ops/pipeline, `liquibase:update`) — the app never migrates the database itself; `{{appName}}-api` depends on it at **test scope only**, so `ApplicationIT` can migrate its own throwaway Testcontainers database with the real changelog.

The `position` feature is the reference implementation of a full hexagonal slice (domain model + ports + service, JPA persistence, external market-data client, contract-first controller, every kind of test). Use it as the model for your own features, then replace it.

## Contract-first workflow

1. Edit `openapi/openapi.yaml` (the contract comes first).
2. `./mvnw compile` regenerates the interfaces and DTOs (`{{basePackage}}.generated.*` — build output, never edited).
3. Implement the new interface methods in a controller, mapping DTOs to the domain through the inbound ports.

## Testing

- **Unit tests** (`*Test`, surefire): domain services with plain JUnit/Mockito, controllers with `@WebMvcTest` + `jwt()`, external clients against WireMock.
- **Integration tests** (`*IT`, failsafe): persistence with `@DataJpaTest` + Testcontainers (schema generated from the JPA mapping, `ddl-auto: create-drop`, local to that test); full application boot with `@SpringBootTest` (migrates the real `{{appName}}-schema` changelog first, so `ddl-auto: validate` has a schema to check).
- **Architecture**: the hexagonal rules, checked on every build ({{appName}}-api/src/test).
- **Coverage**: JaCoCo gate at 70% lines per module.

## Quality and conventions

- Formatting: Spotless with palantir-java-format — `./mvnw spotless:apply` / `spotless:check`. Run Maven on JDK 21–24 (palantir-java-format does not support the JDK 25 javac internals yet; CI pins 21).
- Commits follow [Conventional Commits](https://www.conventionalcommits.org).
- Schema changes only through `{{appName}}-schema`'s Liquibase changelogs, applied out-of-band (`ddl-auto: validate` — never by the app).
