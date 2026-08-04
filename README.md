# image-service

A Spring Boot REST API and React frontend for uploading images and having them
tagged automatically by an AI vision model. Upload an image, get it back with
`objects`, `tags` and `colors` attached, and search everyone's images by keyword.

Image bytes live in a private Backblaze B2 bucket; PostgreSQL stores only the
metadata and the tags (as `jsonb`, with a GIN index for search). Tagging is slow,
so it runs on a background thread pool — the upload returns immediately with
status `PENDING` and the image flips to `DONE` once Gemini has answered.

- **Runtime:** Java 25, Spring Boot 4.1
- **Frontend:** React 19 + TypeScript, Vite, Material UI, TanStack Query — built
  into the JAR and served from `/`
- **Storage:** PostgreSQL (Spring JDBC + Flyway), Backblaze B2 via the AWS S3 SDK v2
- **AI:** Google Gemini (`gemini-flash-latest`) with a forced JSON response schema
- **Docs:** Swagger UI at `/swagger-ui.html`; the long-form docs are in [docs/](docs/)

## Live

**<https://image-service-latest.onrender.com>** — the app itself: browse the
gallery, sign up, upload something and watch it get tagged.

The API is at [`/swagger-ui/index.html`](https://image-service-latest.onrender.com/swagger-ui/index.html)
if you would rather drive it directly. Register, then log in: the browser keeps the
session cookie, so the authenticated endpoints work from there on.

Source: <https://github.com/Yusuprozimemet/image-service>. The first request after
an idle period may take a while — the container is started on demand.

## Quick start

Requires JDK 25 and Docker (Spring Boot's docker-compose support starts Postgres
for you and wires the datasource in automatically).

```bash
cp .env.example .env      # fill in B2_* and LLM_API_KEY
./mvnw spring-boot:run
```

The app boots on <http://localhost:8080> — the React UI at `/`, the API under
`/api`, Swagger UI at <http://localhost:8080/swagger-ui.html>. Flyway applies the
migrations on startup. `mvn` builds the frontend too (it downloads its own Node,
so nothing extra has to be installed); `-DskipFrontend=true` skips that.

Spring does not read `.env` on its own — export the variables first (or put them
in your IDE run configuration):

```bash
set -a && source .env && set +a
./mvnw spring-boot:run
```

Without `B2_*` the app still boots, but storage calls return **503**. Without
`LLM_API_KEY` uploads work and images stay `PENDING` → `FAILED` instead of `DONE`.

## API

All routes are under `/api`. Auth travels in an HttpOnly `session` cookie
(`SameSite=Strict`, 7-day TTL by default) — no header, no token to manage.

| Method & path | Access | What it does |
| --- | --- | --- |
| `POST /api/auth/register` | public | Create an account. `{ email, password }`, password ≥ 8 chars, bcrypt-hashed. **201** `{ userId, email }` · 409 if taken |
| `POST /api/auth/login` | public | **204** + `Set-Cookie: session=…` · 401 on bad credentials |
| `GET /api/auth/me` | auth | The logged-in account. **200** `{ userId, email }` · 401 when logged out. The cookie is HttpOnly, so this is how the frontend knows who you are |
| `POST /api/auth/logout` | auth | Deletes the session and expires the cookie. **204** |
| `POST /api/images` | auth | Multipart `file`. **201** `{ imageId, status: "PENDING" }` · 400 if > 10 MB or not an image |
| `GET /api/images/mine` | auth | The caller's own images, newest first |
| `DELETE /api/images/{id}` | auth (owner) | Removes the row and the B2 object. **204** · 403 if not yours · 404 if unknown |
| `GET /api/images` | public | Home page — the 50 newest images |
| `GET /api/images/search?q=` | public | Images with a matching tag in `objects`, `tags` or `colors`. Matches a whole term, lowercased — `sunset`, not `sun`. Max 50 |
| `GET /api/images/{id}/raw` | public | Streams the bytes with the right `Content-Type`. This is the image's URL — the bucket itself stays private |

Listings return `{ imageId, status, tags: { objects, tags, colors }, url }`, where
`url` points at the raw endpoint above.

### Example

```bash
# register + login (cookie jar keeps the session)
curl -c jar -X POST localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"me@example.com","password":"hunter2hunter2"}'
curl -c jar -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"me@example.com","password":"hunter2hunter2"}'

# upload, then poll until the status turns DONE
curl -b jar -X POST localhost:8080/api/images -F file=@photo.jpg
curl -b jar localhost:8080/api/images/mine

# search everyone's images
curl 'localhost:8080/api/images/search?q=sunset'
```

## Frontend

React 19 + TypeScript in [frontend/](frontend/), built by Vite and served by the
same Spring Boot process. Four routes: the gallery (`/`), tag search (`/search`),
login/register, and `/my` for uploading and deleting your own images.

**It ships inside the JAR on purpose.** The session cookie is `SameSite=Strict`,
so the browser sends it only when the app and the API are the same origin. Serving
the bundle from Spring keeps that true in production for free — no CORS, no CSRF
token, no second deployment. `frontend-maven-plugin` runs `npm ci && npm run build`
during `generate-resources` and the output lands on the classpath under `/static`,
where [SpaConfig](src/main/java/hackyourfuture/net/imageservice/config/SpaConfig.java)
serves it and falls back to `index.html` so client-side routes survive a reload.

```bash
cd frontend
npm install
npm run dev     # http://localhost:5173, proxies /api to :8080
```

Run it next to `./mvnw spring-boot:run -DskipFrontend=true` for hot reload. The dev
server proxies `/api`, so the browser still sees one origin and the cookie works
there too.

The npm executions are marked `<?m2e ignore?>` in the POM, so Eclipse's auto-build
leaves them alone — otherwise every save would re-run `npm ci`, and because that
empties `node_modules` it would break any command-line build running alongside it.
The frontend is built by `mvn` on the command line and in CI.

### Structure

```
api/         the only place that calls fetch; one typed module per backend feature
features/    auth/ and images/ — hooks, then the components that use them
components/  shared UI (Layout, page states)
lib/         query client
theme.ts     the whole visual language
```

The look is Material UI with a deliberately un-Material theme — light, rounded,
bright greens, and buttons with a solid bottom edge that compresses when pressed.
All of it is theme-level (`palette`, `shape`, `typography`, `components`), so the
components stay about behaviour and none of them carry one-off styling. Nunito is
self-hosted via `@fontsource`, so the app fetches nothing at runtime.

Server state is [TanStack Query](https://tanstack.com/query) throughout, and there
is no global store — the only app-wide state is "am I logged in", which is one
query against `/api/auth/me`.

That choice earns its keep on upload: the API returns `PENDING` and tags the image
on a background thread, so `useImages` sets `refetchInterval` while any image in a
listing is still `PENDING` and returns `false` once they all settle. Polling starts
and stops on its own, and a finished gallery costs nothing.

## How it works

Uploads are validated by **magic bytes**, not by the declared content type — JPEG,
PNG, GIF, WebP and BMP are accepted, up to 10 MB. The bytes go to B2 under a
random `images/<uuid>.<ext>` key, a row is inserted as `PENDING`, and the image id
is handed to the `@Async` tagging pool before the request returns.

`TaggingService` then downloads the bytes, asks Gemini for the three tag arrays,
and writes `DONE` (or `FAILED`, on any error). It re-checks that the image is
still `PENDING` first, so a duplicate submission is a no-op. If the tagging queue
is saturated the upload still succeeds — the image is simply left `PENDING` and
untagged, since the bytes are already stored.

The pool is sized in `application.yaml` under `spring.task.execution`: 2 core
threads (concurrency is bounded by Gemini's rate limit, not by CPU), a 100-deep
queue to absorb bursts, and a 30 s graceful shutdown so in-flight tagging finishes.

### Layout

```
auth/      registration, login, opaque session ids, SessionAuthFilter
image/     controller, ImageService, ImageRepository, FileService (B2)
tagging/   TaggingService (@Async), LlmClient (Gemini)
config/    SecurityConfig, SpaConfig, AsyncConfig, OpenApiConfig
shared/    ApiException, GlobalExceptionHandler
```

Each feature is controller → service → repository/client; controllers never touch
the database.

### Schema

Three Flyway migrations in [src/main/resources/db/migration/](src/main/resources/db/migration/):
`users`, `sessions`, and `images` (`object_key`, `content_type`, `status`,
`tags jsonb`). Sessions and images cascade away with their user. Indexes: GIN on
`tags` for search, plus `user_id` for the "my images" listing.

## Docs

[docs/](docs/) has the long-form version — one page per part of the system, with
the reasoning behind the design rather than just its shape.

| | |
| --- | --- |
| [architecture](docs/architecture.md) | Packages, layers, the path of a request, the async seam |
| [api](docs/api.md) | Every endpoint, error shape and status code, with `curl` examples |
| [auth](docs/auth.md) | Sessions, the cookie, `SessionAuthFilter`, security posture |
| [images](docs/images.md) | Upload validation, B2 storage, the raw proxy, search and delete |
| [tagging](docs/tagging.md) | The `@Async` pool, the Gemini call, the forced JSON schema |
| [database](docs/database.md) | Schema, migrations, `jsonb` tags and the GIN-indexed search |
| [frontend](docs/frontend.md) | Routing, TanStack Query, polling, theme, the JAR build |
| [configuration](docs/configuration.md) | Every variable and property, and what breaks without it |
| [development](docs/development.md) | Local setup, the two dev loops, Checkstyle, tests |
| [deployment](docs/deployment.md) | Docker image, both CI workflows, Render + Neon |
| [observability](docs/observability.md) | ECS JSON logs, Loki labels, LogQL queries |

## Configuration

Everything is environment variables — see [.env.example](.env.example) for the
full list with comments.

| Variable | Purpose |
| --- | --- |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Postgres; defaults match `compose.yaml`, in production they point at Neon |
| `SPRING_PROFILES_ACTIVE` | `dev` (debug logging) or `prod` (ECS JSON logs, `Secure` cookie) |
| `PORT` | HTTP port, default 8080 |
| `APP_SESSION_TTL_DAYS` | How long a login lasts — session expiry and cookie `Max-Age`. Default 7 |
| `B2_BUCKET`, `B2_ACCESS_KEY`, `B2_SECRET_KEY` | Backblaze B2 credentials (`B2_ENDPOINT` / `B2_REGION` have defaults) |
| `LLM_API_KEY` | Google AI Studio key for Gemini (`GEMINI_MODEL`, `GEMINI_BASE_URL` have defaults) |
| `LOKI_URL`, `LOKI_USERNAME`, `LOKI_API_TOKEN` | Log shipping to Grafana Cloud; `prod` profile only |

## Development

```bash
./mvnw checkstyle:check    # style gate — CI runs this first, warnings fail the build
./mvnw test                # needs Docker: tests run against a throwaway Postgres (Testcontainers)
./mvnw clean package       # builds the frontend, then the fat JAR containing it

cd frontend && npm run build   # tsc + vite on their own
cd frontend && npm run lint    # oxlint
```

CI ([.github/workflows/ci.yml](.github/workflows/ci.yml)) lints, then builds and
tests on every push and PR to `main`.

```bash
docker build -t image-service .
docker run -p 8080:8080 --env-file .env image-service
```

## Deployment

One container runs the whole thing — UI, API and tagging pool in the same process —
with every stateful dependency managed elsewhere:

| Piece | Runs on |
| --- | --- |
| The app | [Render](https://image-service-latest.onrender.com/swagger-ui/index.html), from a container image, `SPRING_PROFILES_ACTIVE=prod` |
| Database | Neon (serverless PostgreSQL); Flyway migrates it on startup |
| Image bytes | Backblaze B2, private bucket — reads go through `GET /api/images/{id}/raw` |
| Tagging | Google Gemini API |
| Logs | Grafana Cloud Loki, shipped by the logback appender |

Push to `main` and [docker-publish.yml](.github/workflows/docker-publish.yml)
builds the multi-stage image for `linux/amd64`, pushes it to
`ghcr.io/yusuprozimemet/image-service:latest` (plus a commit-SHA tag), then calls
the `RENDER_DEPLOY_HOOK` secret so Render pulls the new tag — image-backed
services do not redeploy on their own when a tag moves.

The `prod` profile ([application-prod.yaml](src/main/resources/application-prod.yaml))
switches the console to ECS JSON and marks the session cookie `Secure`. The Loki
appender sits in a `prod`-only block in
[logback-spring.xml](src/main/resources/logback-spring.xml), so dev, tests and CI
never open a connection to Grafana. Logs carry `app="image-service"` and the level
as labels, so error rates are queryable:

```logql
count_over_time({app="image-service", level="ERROR"}[5m])
```

Every credential — Neon URL, B2 keys, Gemini key, Loki token — is an environment
variable set in Render, never in the repo.