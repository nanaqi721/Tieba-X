# Repository Guidelines

## Project Structure & Module Organization

Teiba-X is a Java 17, Spring Boot 3.5 multi-module application with a Vue 3 frontend. Backend modules follow Maven conventions: production code is under `src/main/java`, configuration and MyBatis XML are under `src/main/resources`, and tests are under `src/test/java`.

- `gateway/`: API gateway and authentication filters.
- `user-server/`, `post-server/`, `bar-server/`, `file-server/`: deployable services.
- `user-api/`, `post-api/`, `bar-api/`, `file-api/`: shared Feign clients, DTOs, and VOs.
- `common/`: cross-service result types, exceptions, and utilities.
- `frontend/`: Vite application; views, components, stores, routes, and API clients live in `frontend/src/`.
- `docs/`: schema, design notes, prompts, and images. Database bootstrap SQL is in `docs/sql_design/teiba-x.sql`.

## Build, Test, and Development Commands

- `mvn clean verify`: compile every backend module and run all tests.
- `mvn -pl post-server -am test`: test one service plus its module dependencies.
- `mvn -pl gateway spring-boot:run`: run a single backend application; replace `gateway` with another `*-server` module as needed.
- `cd frontend && npm ci`: install the locked frontend dependencies.
- `cd frontend && npm run dev`: start the Vite development server.
- `cd frontend && npm run build`: produce the production bundle in `frontend/dist/`.

Services depend on external infrastructure such as MySQL, Redis, Nacos, XXL-Job, and OSS. Review each module's `application.yml`; keep machine-specific credentials in ignored `application-local.yml` files.

## Coding Style & Naming Conventions

Use four spaces for Java and two spaces for Vue/JavaScript. Follow existing Spring layering: `*Controller`, `*Service`/`*ServiceImpl`, `*Mapper`, and persistence `*DO`; API payloads use `*Request`, `*DTO`, or `*VO`. Use PascalCase for Java types and Vue component filenames, camelCase for methods and JavaScript modules, and lowercase package names under `com.mint.ai`. No formatter or linter is configured, so match nearby code and keep imports organized.

## Testing Guidelines

Backend tests use JUnit 5, Spring Boot Test, MockMvc, and AssertJ. Name test classes `*Test` and behavior-focused methods such as `createPost_whenValid_shouldPersist`. Put integration tests in the owning module and use `@Transactional` when database changes should roll back. No frontend test runner or coverage threshold is currently configured.

## Commit & Pull Request Guidelines

Use Conventional Commits: `feat(post): add cursor pagination`, `fix(auth): reject expired token`, or `docs: update setup`. Keep the scope lowercase and the description imperative. Pull requests should identify affected modules, summarize behavior and configuration changes, link related issues, report test commands/results, and include screenshots for UI changes. Never commit secrets, logs, IDE metadata, `target/`, or `frontend/dist/`.

## Agent skills

- Use [docs/agents/issue-tracker.md](docs/agents/issue-tracker.md) to locate durable specifications and implementation tickets.
- Use [docs/agents/triage-labels.md](docs/agents/triage-labels.md) for issue-state labels.
- Use [docs/agents/domain.md](docs/agents/domain.md) to locate the domain glossary and architectural decisions.
