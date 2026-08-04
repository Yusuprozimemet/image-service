# Documentation

The [top-level README](../README.md) is the short version: what this is, how to
run it, and the endpoint table. These pages are the long version — one per part
of the system, each explaining not just what the code does but why it is built
that way.

| Page | What it covers |
| --- | --- |
| [architecture.md](architecture.md) | The shape of the whole thing: packages, layers, the path of a request, and why one process serves both UI and API |
| [api.md](api.md) | Every endpoint: request and response bodies, status codes, the error format, and worked `curl` examples |
| [auth.md](auth.md) | Registration, login, the opaque session cookie, `SessionAuthFilter`, and the security posture |
| [images.md](images.md) | Upload validation, B2 storage, the raw-bytes proxy, listing, search and delete |
| [tagging.md](tagging.md) | The `@Async` pool, the Gemini call, the forced JSON schema, and the `PENDING → DONE / FAILED` lifecycle |
| [database.md](database.md) | Schema, Flyway migrations, the `jsonb` tag column and how the GIN-indexed search works |
| [frontend.md](frontend.md) | The React app: routing, TanStack Query, the polling strategy, the theme, and how it is built into the JAR |
| [configuration.md](configuration.md) | Every environment variable and property, the profiles, and what happens when something is unset |
| [development.md](development.md) | Local setup, the two dev loops, Checkstyle, the test suite, and Testcontainers |
| [deployment.md](deployment.md) | The Docker image, both CI workflows, and the Render + Neon + B2 + Gemini production topology |
| [observability.md](observability.md) | Logging: the ECS JSON console, Loki shipping, labels, and useful LogQL queries |

## Reading order

If you are new to the codebase, start with [architecture.md](architecture.md)
for the map, then follow the one flow that touches almost everything:
[images.md](images.md) (upload) → [tagging.md](tagging.md) (the background work)
→ [database.md](database.md) (where the result lands) →
[frontend.md](frontend.md) (how the UI notices it happened).

If you are here to call the API, [api.md](api.md) and [auth.md](auth.md) are
enough on their own.

If you are here to run or deploy it, read [configuration.md](configuration.md),
then [development.md](development.md) or [deployment.md](deployment.md).
