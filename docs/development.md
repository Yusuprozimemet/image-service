# Development

## Prerequisites

- **JDK 25** — the toolchain is pinned to `java.version 25` in `pom.xml`
- **Docker** — for the local Postgres and for the test suite
- **Nothing else.** Maven comes from the wrapper (`./mvnw`); Node is downloaded
  into `target/` by `frontend-maven-plugin`

## First run

```bash
git clone https://github.com/Yusuprozimemet/image-service
cd image-service
cp .env.example .env      # fill in B2_* and LLM_API_KEY
set -a && source .env && set +a
./mvnw spring-boot:run
```

On startup Spring Boot's docker-compose support starts `compose.yaml`, wires the
Postgres datasource in via `@ServiceConnection`, and Flyway applies the three
migrations. The app is then at:

| | |
| --- | --- |
| React UI | <http://localhost:8080> |
| API | <http://localhost:8080/api> |
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> |

Spring does not read `.env` itself — hence the `set -a && source` line, or an
IDE run configuration holding the same variables. See
[configuration.md](configuration.md).

It runs without credentials too: uploads return `503` without `B2_*`, and images
end `FAILED` without `LLM_API_KEY`. Everything else works.

## The two dev loops

**Backend work** — one process, restart on change:

```bash
./mvnw spring-boot:run -DskipFrontend=true
```

`-DskipFrontend=true` skips npm entirely. Worth it whenever you are not touching
`frontend/`: it cuts the slowest part of the build out of every restart.

**Frontend work** — two processes, hot reload:

```bash
./mvnw spring-boot:run -DskipFrontend=true    # terminal 1
cd frontend && npm run dev                    # terminal 2 → :5173
```

Use <http://localhost:5173>. Vite proxies `/api` to `:8080` with
`changeOrigin: false`, so the browser still sees a single origin and the
`SameSite=Strict` session cookie works exactly as it does in production.

## Everyday commands

```bash
./mvnw checkstyle:check      # style gate — warnings fail the build
./mvnw test                  # needs Docker (Testcontainers)
./mvnw clean package         # frontend + fat JAR
./mvnw spring-boot:run       # run, building the frontend first

cd frontend && npm run build # tsc -b && vite build
cd frontend && npm run lint  # oxlint
cd frontend && npm run dev   # dev server
```

Useful flags:

| Flag | Effect |
| --- | --- |
| `-DskipFrontend=true` | No npm, no Node download, no bundle in the JAR |
| `-DskipTests` | Skip the test suite (what the Dockerfile does) |
| `-Dnpm.install.command=ci` | Clean lockfile-exact install — used by CI and Docker |

`npm install` is the local default because it is fast and leaves `node_modules`
in place; `ci` deletes and reinstalls it, which is right for a fresh CI checkout
and wrong for a machine running a dev server at the same time.

## Checkstyle

[`checkstyle.xml`](../checkstyle.xml) is the style gate, and it runs first in CI
so a formatting violation fails in seconds rather than after the test suite.

Every rule is `severity=warning` and the Maven plugin sets
`violationSeverity=warning` with `failOnViolation=true`, so **any** finding
fails the build — there is no advisory tier. Test sources are included.

It is deliberately **not** `google_checks.xml`. That rule set mandates 2-space
indentation, which this codebase (like Spring itself) does not use; it alone
produced 698 of 828 findings, none of them about correctness. A gate that noisy
gets ignored, which defeats the point.

What is kept is the part that earns its place: no tabs, 120-character lines
(ignoring imports and URLs), no unused or redundant imports, naming conventions,
braces on every block, no empty blocks, whitespace rules, and the small set of
checks that catch real bug classes — `StringLiteralEquality`, `EqualsHashCode`,
`MissingOverride`, `FallThrough`, `SimplifyBooleanExpression`.

```bash
./mvnw checkstyle:check
```

Run it before pushing. The frontend equivalent is `npm run lint` (oxlint).

## Tests

Two integration tests plus the context load, all against a real PostgreSQL in a
Testcontainer:

| Test | What it protects |
| --- | --- |
| [`ImageServiceApplicationTests`](../src/test/java/hackyourfuture/net/imageservice/ImageServiceApplicationTests.java) | The context loads — every bean, filter and config class wires up |
| [`AuthIntegrationTest`](../src/test/java/hackyourfuture/net/imageservice/auth/AuthIntegrationTest.java) | Register → login → logout over HTTP: controller, Spring Security chain, bcrypt, and the `users` + `sessions` tables together |
| [`TaggingAsyncTest`](../src/test/java/hackyourfuture/net/imageservice/tagging/TaggingAsyncTest.java) | `tag()` really leaves the calling thread and runs on the `tagging-` pool |

Both integration tests declare a `postgres:16-alpine` container and let
`@ServiceConnection` wire the datasource, so Flyway runs the same migrations
production uses. Nothing is mocked at the database layer — the SQL under test is
the SQL that ships.

`TaggingAsyncTest` earns its place by guarding a *silent* failure. If `@Async`,
`@EnableAsync` or the proxying broke, tagging would quietly become synchronous
and block every upload; every other assertion in the suite would still pass,
just slower. It mocks `ImageRepository` so the first call inside `tag()` reveals
which thread the work landed on, then returns empty to stop the method there.

```bash
./mvnw test          # Docker must be running
```

Testcontainers pulls the Postgres image on first run, so the first invocation is
noticeably slower than later ones.

### What is not covered

No test exercises upload, search, delete or the Gemini client — those need a B2
double and an HTTP stub for Gemini. The obvious next additions are a
`MockRestServiceServer` test for `LlmClient` (the JSON contract with Gemini is
the most brittle thing in the app) and a magic-byte validation test for
`ImageService`, which needs no infrastructure at all.

## Working with the database

```bash
docker compose ps                                   # is Postgres up
docker exec -it $(docker compose ps -q postgres) \
  psql -U dev -d imagedb                            # a shell
```

```sql
\dt                                        -- tables
SELECT image_id, status, tags FROM images ORDER BY image_id DESC LIMIT 5;
SELECT * FROM flyway_schema_history;       -- what has been applied
```

Applied migrations are immutable — Flyway checksums them and refuses to start if
one changed. To alter the schema, add `V4__…`; never edit `V1`–`V3`. During
early local experimentation you can start over:

```bash
docker compose down -v && ./mvnw spring-boot:run   # destroys all local data
```

## Docker

```bash
docker build -t image-service .
docker run -p 8080:8080 --env-file .env image-service
```

The [`Dockerfile`](../Dockerfile) is a two-stage build: Maven + JDK 25 compiles
the frontend and the JAR, then a JRE-only image runs it. Note that the container
has no database of its own — point `SPRING_DATASOURCE_URL` at one your host can
reach. See [deployment.md](deployment.md).

## Adding a feature

The conventions to follow, in the order you would hit them:

1. **A new package under the feature it belongs to**, not a new layer folder —
   `auth/`, `image/` and `tagging/` each hold their own controller, service and
   repository.
2. **Controller binds and delegates.** It validates input, calls one service
   method, maps to a DTO. No SQL, no rules.
3. **Service holds the rules** and throws `ApiException(status, message)` on
   failure — never returns an error object. `GlobalExceptionHandler` shapes it.
4. **Repository holds the SQL**, always with bound parameters, and returns
   records.
5. **DTOs are records with a `from(...)` factory**, so the wire shape is decided
   in one place.
6. **Schema changes are a new `V*.sql`.**
7. **Frontend follows:** add the call to `api/`, the type to `api/types.ts`, the
   hook to the feature folder, then the component.
8. **Run `./mvnw checkstyle:check` and `npm run lint` before pushing** — CI runs
   them first and fails fast.

CI ([`.github/workflows/ci.yml`](../.github/workflows/ci.yml)) lints, then builds
and tests on every push and pull request to `main`.
