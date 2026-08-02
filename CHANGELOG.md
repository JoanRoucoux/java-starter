# Changelog

## 1.0.0 (2026-08-02)


### ⚠ BREAKING CHANGES

* starter-core no longer exists; sibling modules and generator.config.json now reference starter-domain/starter-adapter.
* starter-domain/starter-adapter merged into starter-core; generator.config.json rewritten (modules, markers, without-schema templates) to match the new module layout.

### Features

* add a production Dockerfile ([42651e4](https://github.com/JoanRoucoux/java-starter/commit/42651e4cfb6edb6f822d5df0d3578deb38bf1b30))
* add lefthook pre-commit hook and upgrade to JDK 25 ([8b466c1](https://github.com/JoanRoucoux/java-starter/commit/8b466c1dc99607615b219249468459cca05baf6a))
* initial commit of java-starter-api ([f92387d](https://github.com/JoanRoucoux/java-starter/commit/f92387db1f8e9bc2f443b7768d85e8ac3883f7b1))
* restructure into detachable modules; add batch, quote, and Cucumber ([f02bb66](https://github.com/JoanRoucoux/java-starter/commit/f02bb6638cbaa366f86a67079a729bbbb61f4b70))
* split starter-core back into starter-domain and starter-adapter ([283a7bb](https://github.com/JoanRoucoux/java-starter/commit/283a7bbb87add828f9f0ce9b887fa79094d6601d))


### Bug Fixes

* adapt to Spring Boot 4's module split and API relocations ([2d63902](https://github.com/JoanRoucoux/java-starter/commit/2d639020a9c072874233163da198d18749827072))
* pin pnpm version in the generate CI job ([999f90f](https://github.com/JoanRoucoux/java-starter/commit/999f90f203c5303fca712c1859092f7c2a0c0e6f))

## Changelog
