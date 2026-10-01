# AGENTS.md

> Guidance for AI coding agents (Codex, Cursor, Copilot, Claude Code, Gemini CLI, etc.) working in this repository.
> Replace every `[PLACEHOLDER]` with project-specific values. Delete sections that don't apply.
> Closer `AGENTS.md` files in subdirectories override this one for files in their subtree.

---

## 1. Project Overview

**Name:** [PROJECT_NAME]
**Purpose:** [One or two sentences: what the system does and who uses it.]
**Status:** [Prototype | In development | Production]

The backend is a **Spring Boot** application. All local infrastructure (database, cache, message broker, etc.) is orchestrated with **Docker Compose**.

---

## 2. Tech Stack

| Area              | Technology                                              |
| ----------------- | ------------------------------------------------------- |
| Language          | Java [21] (or Kotlin [version])                         |
| Framework         | Spring Boot [3.x]                                       |
| Build tool        | Maven (`./mvnw`)                                        |
| Database          | [PostgreSQL 16]                                         |
| Migrations        | Flyway                                                  |
| Persistence       | Spring Data JPA / Hibernate *(or JDBC / jOOQ)*          |
| Cache / Broker    | [Redis / Kafka / RabbitMQ / none]                       |
| API docs          | springdoc-openapi (Swagger UI)                          |
| Testing           | JUnit 5, Mockito, AssertJ, Testcontainers               |
| Containerization  | Docker, Docker Compose v2                               |
| CI                | [GitHub Actions / GitLab CI / Jenkins]                  |

---

## 3. Repository Layout

```
.
├── AGENTS.md
├── GEMINI.md
├── docker-compose.yml            # Base services (db, cache, app)
├── docker-compose.override.yml   # Local dev overrides (auto-loaded)
├── docker-compose.prod.yml       # Production-like overrides (if used)
├── .env.example                  # Template for environment variables (committed)
├── .env                          # Real local values (NEVER commit)
├── backend/
│   ├── Dockerfile
│   ├── pom.xml | build.gradle
│   └── src/
│       ├── main/
│       │   ├── java/[com/example/app]/
│       │   │   ├── Application.java
│       │   │   ├── config/          # @Configuration classes, security, beans
│       │   │   ├── controller/      # REST controllers (thin)
│       │   │   ├── service/         # Business logic
│       │   │   ├── repository/      # Spring Data repositories
│       │   │   ├── domain/          # JPA entities and domain model
│       │   │   ├── dto/             # Request/response objects
│       │   │   ├── mapper/          # Entity <-> DTO mappers (MapStruct)
│       │   │   ├── exception/       # Custom exceptions + @ControllerAdvice
│       │   │   └── util/
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       ├── application-test.yml
│       │       ├── application-prod.yml
│       │       └── db/migration/    # Flyway: V{n}__{description}.sql
│       └── test/
│           └── java/[com/example/app]/
├── docs/                         # Architecture notes, ADRs
└── scripts/                      # Helper scripts (seed data, etc.)
```

> Adjust the tree above to match reality. If the layout changes, update this section in the same PR.

---

## 4. Setup & Common Commands

Always prefer the **wrapper scripts** (`./mvnw` / `./gradlew`) over a globally installed build tool.

### 4.1 First-time setup

```bash
cp .env.example .env            # then edit values
docker compose up -d db redis   # start only infrastructure
```

### 4.2 Run the backend

```bash
# Option A: run on host (fast iteration, hot reload via devtools)
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Option B: run everything in containers
docker compose up --build
```

### 4.3 Build, test, lint

```bash
cd backend
./mvnw clean verify              # compile + unit + integration tests + checks
./mvnw test                      # unit tests only
./mvnw -Dtest=OrderServiceTest test         # single test class
./mvnw -Dtest=OrderServiceTest#shouldX test # single test method
./mvnw spotless:check            # formatting check   (if configured)
./mvnw spotless:apply            # auto-format         (if configured)
```

*(Gradle equivalents: `./gradlew build`, `./gradlew test`, `./gradlew test --tests "*OrderServiceTest"`.)*

### 4.4 Docker Compose cheat sheet

```bash
docker compose config                  # validate + render final config
docker compose ps                      # service status
docker compose logs -f app             # tail backend logs
docker compose exec db psql -U $POSTGRES_USER -d $POSTGRES_DB
docker compose down                    # stop (keep volumes)
docker compose down -v                 # stop AND delete volumes (destroys local data!)
docker compose build --no-cache app    # rebuild backend image
```

### 4.5 Useful endpoints (local)

| Purpose          | URL                                          |
| ---------------- | -------------------------------------------- |
| API base         | `http://localhost:[8080]/api/v1`             |
| Swagger UI       | `http://localhost:[8080]/swagger-ui.html`    |
| Health           | `http://localhost:[8080]/actuator/health`    |

---

## 5. Architecture & Layering Rules

Follow a conventional layered architecture:

```
Controller  ->  Service  ->  Repository  ->  Database
   (DTOs)      (domain)      (entities)
```

- **Controllers** are thin: validate input, delegate to a service, map the result to a DTO. No business logic, no repository access.
- **Services** hold business logic and own transaction boundaries (`@Transactional`). Controllers must not be `@Transactional`.
- **Repositories** contain data access only. No business rules.
- **Entities never cross the API boundary.** Always return DTOs. Never expose JPA entities in controller signatures.
- Dependencies point inward/downward only. A repository must never call a service; a service must never depend on a controller.
- Prefer **constructor injection** (with `final` fields / Lombok `@RequiredArgsConstructor`). Never use field injection with `@Autowired`.
- Cross-cutting concerns (auth, logging, error handling) go in filters, interceptors, aspects, or `@ControllerAdvice` — not copy-pasted into endpoints.

---

## 6. Coding Conventions

### 6.1 General

- Follow the existing code style in the file you're editing. When in doubt, match neighbors.
- Formatting is enforced by [Spotless / Checkstyle / google-java-format]. Run the formatter before committing.
- Keep methods short and single-purpose. Prefer clear names over comments.
- Use `Optional` for return values that may be absent; never pass `Optional` as a parameter or store it in a field.
- Never return `null` from collections — return empty collections.
- Prefer immutable types: Java `record` for DTOs, `final` fields, unmodifiable collections.
- No wildcard imports. No unused imports or dead code.
- Use SLF4J for logging (`@Slf4j`). **Never** use `System.out.println` or `e.printStackTrace()`.

### 6.2 Naming

| Element        | Convention                              | Example                      |
| -------------- | --------------------------------------- | ---------------------------- |
| Package        | lowercase, no underscores               | `com.example.app.order`      |
| Class          | `PascalCase`                            | `OrderService`               |
| Method/var     | `camelCase`                             | `findActiveOrders`           |
| Constant       | `UPPER_SNAKE_CASE`                      | `MAX_RETRY_COUNT`            |
| DTOs           | suffix `Request` / `Response`           | `CreateOrderRequest`         |
| Tests          | `{Class}Test` / `{Class}IT`             | `OrderServiceTest`           |
| DB tables/cols | `snake_case`, plural tables             | `order_items.created_at`     |

### 6.3 REST API design

- Base path: `/api/v1/...`. Version breaking changes (`/api/v2`), never silently alter existing contracts.
- Use nouns and plural resources: `GET /orders/{id}`, `POST /orders`.
- Correct status codes: `200`, `201` (+ `Location` header), `204`, `400`, `401`, `403`, `404`, `409`, `422`, `500`.
- Validate request bodies with Bean Validation (`@Valid`, `@NotNull`, `@Size`, ...).
- Paginate list endpoints (`Pageable`); never return unbounded lists.
- All errors return a consistent body (RFC 7807 `ProblemDetail` preferred) via a central `@RestControllerAdvice`. Do not leak stack traces or internal messages.
- Every endpoint must be documented via springdoc annotations (`@Operation`, `@ApiResponse`) where behavior isn't obvious.

### 6.4 JPA / persistence

- Use `LAZY` fetching by default; avoid `EAGER` on collections. Prevent N+1 with `@EntityGraph` or fetch joins.
- Do not use `spring.jpa.hibernate.ddl-auto=update|create` outside throwaway local experiments. **Schema changes go through migrations only** (see 6.5). In all committed profiles use `validate` or `none`.
- Implement `equals`/`hashCode` carefully on entities (based on business key or id, not all fields).
- Use `@Version` for optimistic locking on entities updated concurrently.
- Store timestamps in UTC (`Instant` / `OffsetDateTime`).

### 6.5 Database migrations

- Location: `backend/src/main/resources/db/migration/`.
- Naming: `V{next_number}__{snake_case_description}.sql` (e.g. `V12__add_order_status_index.sql`).
- **Never edit or delete a migration that has been merged.** Add a new one instead.
- Migrations must be forward-compatible and safe to run on a populated database (no destructive change without an explicit, reviewed plan).
- Test new migrations against a real database (Testcontainers) before submitting.

### 6.6 Configuration

- Use `application.yml` + profile files (`dev`, `test`, `prod`). No hard-coded hosts, ports, credentials, or URLs in Java code.
- Bind config with `@ConfigurationProperties` (typed, validated) rather than scattering `@Value`.
- Secrets come from **environment variables** (`.env` locally, a secret manager in deployed environments). Reference them as `${DB_PASSWORD}`.
- Any new env var must be added to **`.env.example`**, documented here or in `docs/`, and wired through `docker-compose.yml`.

---

## 7. Docker & Docker Compose Conventions

### 7.1 Compose

- Compose file format is the Compose Specification — do **not** add a top-level `version:` key.
- Each service must define: `image` or `build`, `restart` policy, `healthcheck`, and (where relevant) `depends_on` with `condition: service_healthy`.
- Pin image tags to specific versions (e.g. `postgres:16.4`). **Never use `latest`.**
- Read configuration through `environment:` / `env_file:`; never hard-code secrets in compose files.
- Use named volumes for persistent data (`pgdata`), and a dedicated network if more than one app stack exists.
- Inside the compose network, services reach each other by **service name** (`jdbc:postgresql://db:5432/...`), not `localhost`. On the host, use the published port.
- Only publish ports that developers actually need on the host.

Reference snippet:

```yaml
services:
  db:
    image: postgres:16.4
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 5s
      timeout: 3s
      retries: 10

  app:
    build:
      context: ./backend
    restart: unless-stopped
    env_file: .env
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/${POSTGRES_DB}
    ports:
      - "8080:8080"
    depends_on:
      db:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8080/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 10

volumes:
  pgdata:
```

### 7.2 Dockerfile (backend)

- Use **multi-stage builds**: a JDK build stage and a slim JRE runtime stage.
- Use layered jars (`spring-boot:build-image` or `-Djarmode=layertools`) to maximize layer caching.
- Run as a **non-root user**. Do not bake secrets into the image.
- Keep `.dockerignore` up to date (`target/`, `.git/`, `.env`, `*.md`, IDE files).
- Set container-aware JVM flags (e.g. `-XX:MaxRAMPercentage=75`).

---

## 8. Testing Requirements

- **Every behavior change needs tests.** Bug fixes must include a regression test.
- Test pyramid:
  - **Unit tests** (`*Test`): JUnit 5 + Mockito + AssertJ. No Spring context, no I/O. Fast.
  - **Slice tests**: `@WebMvcTest`, `@DataJpaTest` for controller/repository layers.
  - **Integration tests** (`*IT`): `@SpringBootTest` with **Testcontainers** for real PostgreSQL/Redis/Kafka. Do **not** rely on H2 to emulate production DB behavior.
- Tests must be deterministic and independent: no ordering assumptions, no shared mutable state, no real network calls, no `Thread.sleep` (use Awaitility).
- Use descriptive names: `shouldReturn404WhenOrderDoesNotExist`.
- Arrange–Act–Assert structure; one logical assertion focus per test.
- Do not weaken, skip (`@Disabled`), or delete failing tests to make a build pass. Fix the cause or ask.
- Target coverage: [80%] line coverage on new/changed code (enforced by [JaCoCo]).

---

## 9. Security Rules

- **Never commit secrets**: passwords, API keys, tokens, private keys, `.env`. If one is found, flag it immediately.
- Never log secrets, tokens, passwords, or sensitive personal data (PII).
- Validate and sanitize all external input. Use parameterized queries / JPA; **never build SQL with string concatenation.**
- Authentication/authorization is handled by Spring Security ([JWT / OAuth2 / session]). New endpoints must be explicitly secured or explicitly marked public with justification.
- Apply least privilege for DB users and service accounts.
- Keep CORS restrictive; do not use `allowedOrigins("*")` with credentials.
- Do not disable CSRF, TLS verification, or security filters to "make it work."
- Add new dependencies only from reputable sources; avoid adding a library for something trivially done with the JDK/Spring.

---

## 10. Observability

- Expose Actuator endpoints (`health`, `info`, `metrics`, `prometheus`) — restrict sensitive ones in non-dev profiles.
- Log with structured, leveled messages: `ERROR` (needs action), `WARN` (recoverable), `INFO` (business events), `DEBUG` (diagnostics).
- Include a correlation/trace ID in logs (MDC) where tracing is configured.
- Use parameterized logging: `log.info("Order {} created", id)` — not string concatenation.

---

## 11. Git & Pull Request Workflow

- Branch naming: `feature/[ticket]-short-desc`, `fix/[ticket]-short-desc`, `chore/...`.
- Commit messages follow **Conventional Commits**: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`, `build:`, `ci:`.
- Keep commits focused and PRs small. One concern per PR.
- Before opening a PR, ensure that:
  1. `./mvnw clean verify` passes locally.
  2. Formatting/lint checks pass.
  3. New env vars are in `.env.example`; new migrations follow naming rules.
  4. Docs/OpenAPI annotations are updated for any API change.
  5. `docker compose up --build` still starts cleanly (when Docker/compose files or dependencies changed).
- PR description must state **what** changed, **why**, how it was **tested**, and any **risks/rollback** notes.

---

## 12. Agent Boundaries

### ✅ Always do

- Read the relevant code and existing patterns before changing anything.
- Make the smallest change that fully solves the task.
- Run the build and relevant tests after changes; report actual results.
- Update tests, docs, `.env.example`, and OpenAPI annotations alongside code.
- Follow existing conventions even if you'd personally choose differently.

### ⚠️ Ask first

- Adding, removing, or upgrading dependencies or the Spring Boot / Java version.
- Changing database schema in a non-trivial or potentially destructive way.
- Modifying public API contracts (paths, payloads, status codes).
- Changing security configuration, auth flows, or CORS.
- Large refactors, package restructuring, or renaming widely used classes.
- Altering Docker/Compose topology (new services, ports, volumes) or CI pipelines.

### 🚫 Never do

- Commit or print secrets, or edit `.env` with real credentials.
- Run destructive commands against shared/remote resources (`docker compose down -v` is acceptable **only** for local dev data; never run `DROP`/`TRUNCATE` or equivalent on non-local databases).
- Edit already-merged Flyway/Liquibase migrations.
- Use `git push --force` to shared branches, or rewrite published history.
- Disable/delete tests, linters, or security checks to get a green build.
- Edit generated files (`target/`, `build/`, generated MapStruct/OpenAPI sources) by hand.
- Invent APIs, config keys, or library methods — verify against the code or official docs.

---

## 13. Definition of Done

A task is complete when:

- [ ] Code compiles and `./mvnw clean verify` passes
- [ ] New/changed behavior is covered by tests
- [ ] No new warnings, lint, or formatting violations
- [ ] Migrations (if any) are correct, versioned, and tested
- [ ] Config/env changes reflected in `.env.example` and compose files
- [ ] API docs updated; no breaking change without versioning
- [ ] No secrets, debug code, or stray TODOs introduced
- [ ] Summary of changes and how they were verified is provided

---

## 14. Troubleshooting

| Symptom                                       | Likely cause / fix                                                                 |
| --------------------------------------------- | ---------------------------------------------------------------------------------- |
| App can't connect to DB in compose            | Using `localhost` instead of service name `db`; or DB not healthy yet              |
| App can't connect to DB when run on host      | Container not started, or port not published / already in use                      |
| `Port already in use`                         | Another process/container bound to it: `lsof -i :8080` / `docker compose ps`       |
| Flyway checksum mismatch                      | A merged migration was edited. Revert it and add a new migration                   |
| Stale image behavior after code changes       | `docker compose build --no-cache app && docker compose up -d`                      |
| Env var changes not picked up                 | Recreate containers: `docker compose up -d --force-recreate app`                   |
| Testcontainers fails to start                 | Docker daemon not running or lacking permissions                                   |
| `LazyInitializationException`                 | Accessing lazy association outside a transaction; fetch it in the service layer    |

---

## 15. References

- Architecture decisions: `docs/adr/`
- API contract: `/swagger-ui.html` or `docs/openapi.yaml`
- Runbook / deployment notes: `docs/runbook.md`
- Team contacts / ownership: [CODEOWNERS or link]

---

*Keep this file accurate. If an agent repeatedly makes the same mistake, add a rule here.*