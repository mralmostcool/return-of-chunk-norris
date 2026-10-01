# GEMINI.md

> Context file for **Gemini CLI** (and compatible Gemini agents).
> Gemini CLI loads `GEMINI.md` files hierarchically (global `~/.gemini/GEMINI.md` -> project root -> subdirectories)
> and concatenates them into the model's context for every session.
> Replace every `[PLACEHOLDER]` with project-specific values.

---

## 0. Shared Project Context (imported)

The full project rules (stack, architecture, conventions, testing, security, boundaries, definition of done)
live in `AGENTS.md` so that every AI tool shares one source of truth. It is imported below using
Gemini CLI's `@file` import syntax:

@./AGENTS.md

> **If imports don't work in your setup**, either paste the contents of `AGENTS.md` here, or configure Gemini CLI to read it directly
> by adding this to `.gemini/settings.json`:
>
> ```json
> {
>   "context": {
>     "fileName": ["AGENTS.md", "GEMINI.md"]
>   }
> }
> ```

Everything below is **Gemini-specific** and supplements (never contradicts) `AGENTS.md`.
If there is ever a conflict, the more restrictive rule wins; ask the user when unsure.

---

## 1. Quick Context Recap

- **Project:** [PROJECT_NAME] — [one-line description].
- **Backend:** Spring Boot [3.x], Java [21], built with `./mvnw` (or `./gradlew`).
- **Infra:** Docker Compose (`docker-compose.yml` + `docker-compose.override.yml`) running [PostgreSQL, Redis, ...].
- **Layers:** Controller -> Service -> Repository. DTOs at the API boundary; entities never exposed.
- **DB changes:** Flyway migrations only (`backend/src/main/resources/db/migration/`).
- **Secrets:** `.env` (git-ignored). Template in `.env.example`.

---

## 2. How Gemini Should Work in This Repo

### 2.1 Workflow: Understand -> Plan -> Implement -> Verify

1. **Understand** — Before editing, explore: list directories, read the relevant classes, tests, and config. Search for existing patterns (`grep`/glob) rather than guessing.
2. **Plan** — For anything beyond a trivial change, state a short plan (files to touch, approach, risks) and wait for confirmation if the change is large or touches an "Ask first" area from `AGENTS.md`.
3. **Implement** — Make small, focused, reviewable edits. Match surrounding style. Don't reformat unrelated code.
4. **Verify** — Run the build and the narrowest relevant tests first, then the broader suite. Report the *actual* command output summary, never claim success without running it.

### 2.2 Communication style

- Be concise and direct. Lead with the result, then details.
- When presenting changes, list files modified and *why*.
- If something is ambiguous or underspecified, ask one targeted clarifying question rather than assuming.
- If a command fails, show the relevant error, diagnose the root cause, and fix it — don't just retry blindly.
- Clearly distinguish between facts verified in the code and assumptions.

### 2.3 Scope discipline

- Do exactly what was asked. Don't add unrelated features, refactors, or dependencies.
- If you notice an unrelated bug or improvement, **mention it at the end** instead of fixing it silently.
- Prefer editing existing files to creating new ones; create new files only when they fit the layering and naming conventions.

---

## 3. Tool & Shell Usage Rules

### 3.1 Preferred commands

| Goal                          | Command                                                      |
| ----------------------------- | ------------------------------------------------------------ |
| Compile + full verification   | `cd backend && ./mvnw clean verify`                          |
| Unit tests only               | `cd backend && ./mvnw test`                                  |
| Single test class             | `cd backend && ./mvnw -Dtest=ClassNameTest test`             |
| Check formatting              | `cd backend && ./mvnw spotless:check`                        |
| Start infra only              | `docker compose up -d db redis`                              |
| Start full stack              | `docker compose up --build -d`                               |
| Validate compose file         | `docker compose config`                                      |
| Tail app logs                 | `docker compose logs --tail=200 app`                         |
| Stop stack (keep data)        | `docker compose down`                                        |

### 3.2 Safe-shell guidelines

- Use `docker compose` (v2), not the legacy `docker-compose` binary.
- Prefer **non-interactive, bounded** commands. Use `--tail`, `-n`, `head`, or `-q` to avoid flooding the context with logs.
- Never run long-lived foreground processes (`spring-boot:run`, `docker compose logs -f`) in a way that blocks the session — run detached (`-d`) or in the background and check logs after.
- Do **not** run any of the following without explicit user approval:
  - `docker compose down -v`, `docker volume rm`, `docker system prune` (data loss)
  - `rm -rf` outside of build output directories (`target/`, `build/`)
  - `git reset --hard`, `git clean -fd`, `git push --force`
  - Any command targeting a remote/staging/production database or cluster
  - Package installs or global tool changes on the host
- Never `cat`, print, or paste `.env`, key files, or tokens. If you must check whether a variable is set, check for presence only.
- Run builds from the correct directory (`backend/`), and don't assume the working directory persists between commands.

### 3.3 File editing

- Read a file before modifying it. Make surgical edits instead of rewriting whole files.
- Preserve file encoding, line endings, license headers, and existing import ordering.
- Never hand-edit generated or build output (`target/`, `build/`, generated sources).
- After editing Java files, ensure they compile (`./mvnw -q compile`) before moving on.

---

## 4. Task Playbooks

### 4.1 Add a new REST endpoint

1. Check for an existing controller/service for the domain; extend it if present.
2. Create request/response DTOs (`record`s with Bean Validation annotations).
3. Add the service method with `@Transactional` boundaries and business logic.
4. Add/extend the repository method if needed (verify query efficiency, avoid N+1).
5. Add the controller method under `/api/v1/...`, with correct status codes and springdoc annotations.
6. Add a mapper (MapStruct) rather than manual field copying if a mapper pattern exists.
7. Write tests: service unit test, `@WebMvcTest` for the controller, and an integration test if persistence is involved.
8. Run `./mvnw clean verify`.

### 4.2 Change the database schema

1. Add a **new** migration `V{next}__{description}.sql` — never edit existing ones.
2. Update the JPA entity and any affected DTOs/mappers.
3. Make the migration safe for populated tables (defaults for new NOT NULL columns, concurrent index creation where supported).
4. Write/adjust an integration test using Testcontainers to confirm the migration applies cleanly and the entity maps correctly.
5. Mention the schema change and any rollback considerations in the summary.

### 4.3 Add a new dependency

1. **Ask first.** Explain why the existing stack can't cover the need.
2. Prefer Spring-managed versions (BOM) over hard-coded versions.
3. Check for security/maintenance status; avoid abandoned libraries.
4. Add to `pom.xml`/`build.gradle`, run the full build, and note the addition in the summary.

### 4.4 Add or change a Docker Compose service

1. Pin the image version; add a `healthcheck`; use `depends_on` with `condition: service_healthy`.
2. Parameterize with env vars; add them to `.env.example` and wire into `docker-compose.yml`.
3. Verify with `docker compose config`, then `docker compose up -d` and confirm the service is healthy via `docker compose ps`.
4. Update Spring config (profile files / env mapping) and documentation.

### 4.5 Debug a failing test or build

1. Reproduce with the narrowest command (single test) and read the *first* real error, not just the tail.
2. Form a hypothesis from the stack trace and relevant source; verify before changing code.
3. Fix the root cause. Do not mute, skip, or loosen the test to pass.
4. Re-run the single test, then the module, then the full verification.

### 4.6 Investigate a runtime issue in Docker

1. `docker compose ps` — check status/health.
2. `docker compose logs --tail=200 <service>` — inspect recent logs.
3. Confirm networking (service names, ports, env vars) via `docker compose config`.
4. Check DB connectivity and migration status before suspecting application code.

---

## 5. Code Generation Standards (Quick Checklist)

Before presenting any Java code, confirm it:

- [ ] Uses constructor injection (no `@Autowired` fields)
- [ ] Keeps controllers thin; logic lives in services
- [ ] Returns DTOs, not entities
- [ ] Validates input (`@Valid`) and handles errors via `@RestControllerAdvice`
- [ ] Uses parameterized logging via SLF4J; logs no secrets/PII
- [ ] Has no hard-coded config, credentials, hosts, or ports
- [ ] Uses `Optional`/empty collections instead of `null`
- [ ] Includes appropriate tests with descriptive names
- [ ] Compiles and passes the formatter

---

## 6. Accuracy & Hallucination Guardrails

- **Do not invent** classes, methods, properties, config keys, endpoints, or library APIs. If unsure, search the codebase or consult official docs.
- When using Spring Boot APIs, match the **version in this project** (`pom.xml`/`build.gradle`). Be careful with `javax.*` vs `jakarta.*` (Spring Boot 3+ uses `jakarta.*`) and deprecated Spring Security configuration styles (use `SecurityFilterChain` beans, not `WebSecurityConfigurerAdapter`).
- If a requested change conflicts with existing architecture or `AGENTS.md`, say so and propose an alternative instead of silently complying.
- Admit uncertainty. A clearly flagged assumption is better than a confident guess.

---

## 7. Final Response Format

When finishing a task, respond with:

1. **Summary** — what was done and why (2–5 sentences).
2. **Files changed** — bullet list with one-line purpose each.
3. **Verification** — exact commands run and their outcome (pass/fail, test counts).
4. **Notes / follow-ups** — assumptions, risks, deferred items, or unrelated issues spotted.

---

## 8. Useful Gemini CLI Tips for This Project

- Use `/memory show` to inspect the loaded context and `/memory refresh` after editing this file or `AGENTS.md`.
- Use `/memory add <text>` to save a quick project fact for the current setup.
- Use `/init` to regenerate a starter context file if the repo structure changes drastically.
- Keep sensitive files out of the model's reach with a `.geminiignore` file, for example:

  ```
  .env
  .env.*
  !.env.example
  **/*.pem
  **/*.key
  **/secrets/
  target/
  build/
  ```

- Use directory-scoped `GEMINI.md` files for module-specific rules (e.g. `backend/GEMINI.md`) rather than bloating this root file.

---

*Keep this file lean and current. Put shared rules in `AGENTS.md`; put only Gemini-specific behavior here.*