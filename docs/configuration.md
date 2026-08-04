# Configuration

Everything configurable is an environment variable. Nothing secret lives in the
repository — the YAML files hold defaults and structure, and every credential
arrives from the environment.

- **Files:** [`application.yaml`](../src/main/resources/application.yaml),
  [`application-dev.yaml`](../src/main/resources/application-dev.yaml),
  [`application-prod.yaml`](../src/main/resources/application-prod.yaml)
- **Template:** [`.env.example`](../.env.example) — copy to `.env` and fill in

## How values arrive

Every property in `application.yaml` is written as `'${ENV_VAR:default}'`, so a
value can come from the environment or fall back to something that works
locally. Spring Boot's relaxed binding also maps `SPRING_DATASOURCE_URL` onto
`spring.datasource.url` with no configuration at all.

**Spring does not read `.env`.** It exists for `docker compose` (which picks it
up automatically) and as a checklist for what to set. To use it with the Maven
plugin, export the contents first:

```bash
set -a && source .env && set +a
./mvnw spring-boot:run
```

or put the variables in your IDE run configuration.

## Every variable

### Database

| Variable | Default | Notes |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/imagedb` | Matches `compose.yaml`. Production points at Neon |
| `SPRING_DATASOURCE_USERNAME` | `dev` | |
| `SPRING_DATASOURCE_PASSWORD` | `dev` | |

In local development you normally set none of these:
`spring-boot-docker-compose` starts `compose.yaml` and injects the connection
via `@ServiceConnection`, overriding the defaults. They only matter when running
against a database you started yourself, or in production.

### Server and profile

| Variable | Default | Notes |
| --- | --- | --- |
| `PORT` | `8080` | Render injects this; the app must bind whatever it is given |
| `SPRING_PROFILES_ACTIVE` | *(none)* | `dev` locally, `prod` in the cloud |

With no profile active you get the base configuration: `INFO` console logging
and a non-`Secure` cookie. That is fine locally, and wrong in production — the
`prod` profile exists to correct exactly those two things.

### Session

| Variable | Default | Notes |
| --- | --- | --- |
| `APP_SESSION_TTL_DAYS` | `7` | How long a login lasts |

One number drives both the `expires_at` stored on the session row and the
cookie's `Max-Age`, because `AuthController` reads the lifetime from
`SessionService` rather than keeping its own copy. If they disagreed, a browser
would either keep sending a cookie the server has already expired, or drop one
the server still honours.

Changing it affects **new logins only** — existing sessions keep the expiry they
were created with.

`app.session.cookie-secure` has no environment variable of its own: it is
`false` by default and set to `true` by the `prod` profile. Local development
runs over plain HTTP, where a `Secure` cookie would never be sent at all.

### Backblaze B2

| Variable | Default | Notes |
| --- | --- | --- |
| `B2_BUCKET` | *(empty)* | Bucket name |
| `B2_ACCESS_KEY` | *(empty)* | Application key id |
| `B2_SECRET_KEY` | *(empty)* | Application key |
| `B2_ENDPOINT` | `https://s3.us-west-004.backblazeb2.com` | From your bucket's details in the B2 console |
| `B2_REGION` | `us-west-004` | Must match the endpoint |

Endpoint and region have working defaults; the bucket and both keys must be set
for storage to work. If any of the three is blank the S3 client is never
constructed and every storage call returns
`503 image storage is not configured` — the app still boots, and auth, the
gallery and Swagger UI keep working. See [images.md](images.md).

The bucket must be **private**. Images are served through
`GET /api/images/{id}/raw`, never directly from B2.

### Gemini

| Variable | Default | Notes |
| --- | --- | --- |
| `LLM_API_KEY` | *(empty)* | Key from Google AI Studio |
| `GEMINI_MODEL` | `gemini-flash-latest` | Alias tracking Google's current flash model |
| `GEMINI_BASE_URL` | `https://generativelanguage.googleapis.com/v1beta` | Override to point at a proxy or mock |

Without a key the app boots and uploads succeed; tagging fails and images end up
`FAILED` instead of `DONE`. See [tagging.md](tagging.md).

### Logging to Loki

| Variable | Notes |
| --- | --- |
| `LOKI_URL` | Loki base URL from your Grafana Cloud stack |
| `LOKI_USERNAME` | The numeric instance / user id |
| `LOKI_API_TOKEN` | A write-scoped access-policy token |

Read **only** under the `prod` profile — the appender lives inside a
`<springProfile name="prod">` block, so dev, tests and CI never open a
connection to Grafana. See [observability.md](observability.md).

## Properties without an environment variable

Set in YAML, tuned by editing the file rather than the environment:

| Property | Value | Why it lives here |
| --- | --- | --- |
| `spring.servlet.multipart.max-file-size` | `11MB` | Just above the app's own 10 MB rule, so oversized uploads get a clear message instead of a container error |
| `spring.servlet.multipart.max-request-size` | `12MB` | Headroom for multipart overhead |
| `spring.task.execution.pool.core-size` | `2` | The real tagging concurrency — bounded by Gemini's rate limit, not CPU |
| `spring.task.execution.pool.max-size` | `4` | Only reached once the queue is full, so effectively a ceiling |
| `spring.task.execution.pool.queue-capacity` | `100` | Absorbs upload bursts |
| `spring.task.execution.shutdown.await-termination-period` | `30s` | Lets in-flight tagging finish on shutdown |

The 10 MB upload limit itself is `MAX_BYTES` in `ImageService` — a constant, not
a property, because it is a product rule the validation message repeats verbatim.

## Profiles

### `dev`

```yaml
logging:
  level:
    root: DEBUG
    org.springframework.jdbc.core: DEBUG
```

Debug logging, including every SQL statement `JdbcClient` executes. The
datasource is inherited from `application.yaml`, but in practice
`spring-boot-docker-compose` overrides it with the container it starts.

### `prod`

```yaml
app:
  session:
    cookie-secure: true
logging:
  level:
    root: INFO
    org.springframework.web: WARN
    org.apache.catalina: WARN
  structured:
    format:
      console: ecs
```

Three changes, each one required by production rather than merely nicer:

- **`cookie-secure: true`** — production is HTTPS, and the session cookie must
  not be sent over plain HTTP.
- **ECS JSON console output** — logs are read by Loki and Grafana, not a human
  at a terminal, so structured fields beat prose.
- **Quieter framework logging** — `DEBUG` in production would ship request noise
  to Loki at real cost.

Secrets are never in `application-prod.yaml`. It is committed to the repository;
every credential arrives as an environment variable set in Render.

## Local checklist

```bash
cp .env.example .env
```

Then fill in:

- `B2_BUCKET`, `B2_ACCESS_KEY`, `B2_SECRET_KEY` — otherwise uploads return 503
- `LLM_API_KEY` — otherwise images never leave `PENDING`/`FAILED`
- leave `SPRING_DATASOURCE_*` alone unless you are running your own Postgres
- leave `LOKI_*` alone unless you set `SPRING_PROFILES_ACTIVE=prod`

`.env` is git-ignored. Never commit it.
