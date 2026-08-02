# Contributing

Thanks for your interest in this project!

## Prerequisites

- JDK 25 (the Maven wrapper takes care of Maven itself)
- Docker (for the Testcontainers integration tests)
- [lefthook](https://lefthook.dev) — install it, then run `lefthook install` once at the repository
  root so the pre-commit hook (Spotless auto-format) is active.

## Workflow

1. Create a branch from `main` (`feat/...`, `fix/...`).
2. Develop following the project conventions (see [README](../README.md#quality-and-conventions)).
3. Check locally before committing (the lefthook pre-commit hook already runs `spotless:apply`
   automatically; `verify` is not part of it, so run it yourself):
   ```bash
   ./mvnw verify
   ```
4. Commit using a [Conventional Commits](https://www.conventionalcommits.org) message (`feat: ...`, `fix: ...`, `chore: ...`) — release-please and the reviewers rely on them.
5. Open a Pull Request filling in the provided template.

## Reporting a bug or requesting a feature

Use the issue templates available on GitHub.
