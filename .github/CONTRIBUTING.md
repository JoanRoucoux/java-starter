# Contributing

Thanks for your interest in this project!

## Prerequisites

- JDK 21 (the Maven wrapper takes care of Maven itself)
- Docker (for the Testcontainers integration tests)

## Workflow

1. Create a branch from `main` (`feat/...`, `fix/...`).
2. Develop following the project conventions (see [README](../README.md#quality-and-conventions)).
3. Check locally before committing:
   ```bash
   ./mvnw spotless:check
   ./mvnw verify
   ```
4. Commit using a [Conventional Commits](https://www.conventionalcommits.org) message (`feat: ...`, `fix: ...`, `chore: ...`) — there are no local git hooks, but release-please and the reviewers rely on them.
5. Open a Pull Request filling in the provided template.

## Reporting a bug or requesting a feature

Use the issue templates available on GitHub.
