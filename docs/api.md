# API reference

Every route lives under `/api`. Requests and responses are JSON, except the
upload (multipart) and the raw image endpoint (image bytes).

Authentication is a cookie, not a header — see [auth.md](auth.md). There is no
token to attach, no `Authorization` header, and nothing for a client to refresh:
log in once and the browser (or your `curl` cookie jar) carries the session from
then on.

An interactive version of this page is served by the app itself at
[`/swagger-ui.html`](http://localhost:8080/swagger-ui.html), with the raw
OpenAPI document at `/v3/api-docs`. The session cookie is declared as a security
scheme in
[`OpenApiConfig`](../src/main/java/hackyourfuture/net/imageservice/config/OpenApiConfig.java),
so "Try it out" works on the authenticated routes once you have logged in
through the same browser.

## At a glance

| Method & path | Access | Success |
| --- | --- | --- |
| `POST /api/auth/register` | public | `201` `{ userId, email }` |
| `POST /api/auth/login` | public | `204` + `Set-Cookie: session=…` |
| `GET /api/auth/me` | session | `200` `{ userId, email }` |
| `POST /api/auth/logout` | session | `204` + cookie cleared |
| `POST /api/images` | session | `201` `{ imageId, status: "PENDING" }` |
| `GET /api/images/mine` | session | `200` `ImageResponse[]` |
| `DELETE /api/images/{id}` | session, owner | `204` |
| `GET /api/images` | public | `200` `ImageResponse[]` (≤ 50) |
| `GET /api/images/search?q=` | public | `200` `ImageResponse[]` (≤ 50) |
| `GET /api/images/{id}/raw` | public | `200` image bytes |

"session" means a valid session cookie is required; anything else gets a bare
`401`. "public" means the route is reachable while logged out — the three public
image routes are what let an anonymous visitor browse the gallery.

## Errors

Application errors come back in one shape, produced by
[`GlobalExceptionHandler`](../src/main/java/hackyourfuture/net/imageservice/shared/GlobalExceptionHandler.java):

```json
{ "error": "image is larger than 10MB" }
```

Bean-validation failures add a per-field breakdown:

```json
{
  "error": "validation failed",
  "fields": {
    "email": "must be a well-formed email address",
    "password": "password must be at least 8 characters"
  }
}
```

Two exceptions to be aware of when writing a client:

- **`401` has an empty body.** It is produced by Spring Security's
  `HttpStatusEntryPoint` before any controller runs, so there is no `error`
  field to read. Treat the status alone as "not logged in".
- **Framework-level `400`s** (a missing `q` parameter, malformed JSON) use
  Spring Boot's default error body, not the `{"error": …}` shape.

The frontend's [`ApiError`](../frontend/src/api/client.ts) handles all three
cases: it reads `error` when the body is JSON and falls back to
`request failed (<status>)` otherwise.

| Status | When |
| --- | --- |
| `400` | Validation failed, no file, file over 10 MB, unreadable upload, unsupported image type |
| `401` | No session cookie, or one that is unknown or expired |
| `403` | Deleting an image you do not own |
| `404` | No image with that id |
| `409` | Registering an email that already exists |
| `502` | Gemini returned nothing usable — only ever surfaces in logs, since tagging is asynchronous |
| `503` | Backblaze B2 is not configured (`B2_BUCKET` / `B2_ACCESS_KEY` / `B2_SECRET_KEY` unset) |

---

## Auth

### `POST /api/auth/register`

Creates an account. Public.

```json
{ "email": "me@example.com", "password": "hunter2hunter2" }
```

`email` must be a well-formed address; `password` must be 8–100 characters. The
password is bcrypt-hashed before it is stored and is never echoed back.

**`201`**

```json
{ "userId": 1, "email": "me@example.com" }
```

**`400`** validation failed · **`409`** `email already registered`

Registering does **not** log you in — no cookie is issued here. The frontend
calls login immediately afterwards to make signup feel like one step.

### `POST /api/auth/login`

Exchanges credentials for a session. Public.

```json
{ "email": "me@example.com", "password": "hunter2hunter2" }
```

**`204`** with the session cookie:

```http
Set-Cookie: session=<32 random bytes, base64url>; Path=/; Max-Age=604800;
            HttpOnly; SameSite=Strict
```

`Secure` is added under the `prod` profile. `Max-Age` follows
`APP_SESSION_TTL_DAYS` (default 7) and always matches the expiry stored on the
session row.

**`401`** `invalid email or password` — the same message for an unknown email
and a wrong password, so the endpoint cannot be used to discover which accounts
exist.

### `GET /api/auth/me`

The logged-in account. Session required.

**`200`** `{ "userId": 1, "email": "me@example.com" }` · **`401`** when logged out

The cookie is `HttpOnly`, so JavaScript cannot read it — this endpoint is how a
browser client learns who it is after a page load. The frontend treats the `401`
as the answer "logged out" rather than as an error; see
[`useSession`](../frontend/src/features/auth/useSession.ts).

### `POST /api/auth/logout`

Deletes the session row and expires the cookie. Session required.

**`204`**, with `Set-Cookie: session=; Max-Age=0`. Because the session is stored
server-side, the id is dead the moment this returns even if the client keeps a
copy of the cookie.

---

## Images

Listings return an array of:

```json
{
  "imageId": 12,
  "status": "DONE",
  "tags": {
    "objects": ["dog", "grass", "ball"],
    "tags": ["outdoors", "playful", "afternoon"],
    "colors": ["green", "brown", "white"]
  },
  "url": "/api/images/12/raw"
}
```

`status` is `PENDING`, `DONE` or `FAILED`. The three tag arrays are empty until
tagging finishes, and stay empty on `FAILED`. `url` is a relative path to the
raw endpoint — put it straight in an `<img src>`.

### `POST /api/images`

Uploads one image. Session required. `multipart/form-data` with a single part
named `file`.

**`201`** `{ "imageId": 12, "status": "PENDING" }`

The response is always `PENDING`: the bytes are stored and the row committed,
but tagging runs on a background thread and has not started yet. Poll a listing
until the status changes. See [tagging.md](tagging.md).

**`400`** — `no file uploaded`, `image is larger than 10MB`,
`file is not a supported image`, `could not read the uploaded file`.
Accepted types are JPEG, PNG, GIF, WebP and BMP, decided from the file's magic
bytes rather than the declared content type. **`503`** if B2 is not configured.

### `GET /api/images/mine`

The caller's own images, newest first. Session required. Not capped at 50 — you
get all of them.

### `DELETE /api/images/{id}`

Removes the row and the object in B2. Session required, and only the owner may
call it.

**`204`** · **`403`** `not your image` · **`404`** `image not found`

### `GET /api/images`

The 50 newest images from everyone — the gallery. Public.

### `GET /api/images/search?q=<term>`

Images with a matching tag in `objects`, `tags` or `colors`. Public. Max 50
results, newest first.

The match is a **whole term, lowercased**: `sunset` finds images tagged
`sunset`, but `sun` does not. It is a `jsonb` containment lookup against a GIN
index, not a text search — see [database.md](database.md) for why that trade was
made. A blank or whitespace-only `q` returns `[]`; omitting `q` entirely is a
framework-level `400`.

### `GET /api/images/{id}/raw`

Streams the bytes with the stored `Content-Type`. Public — this is the image's
URL, and it is what the `url` field in every listing points at.

The bucket itself is private, so this endpoint is the only way to read an
image. That is deliberate: no public bucket, no signed-URL expiry to manage, and
deleting a row genuinely takes the image offline.

**`404`** if the id is unknown · **`503`** if B2 is not configured

---

## Worked example

```bash
# 1. register, then log in — the cookie jar keeps the session
curl -c jar -X POST localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"me@example.com","password":"hunter2hunter2"}'

curl -c jar -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"me@example.com","password":"hunter2hunter2"}'

# 2. who am I?
curl -b jar localhost:8080/api/auth/me

# 3. upload — comes back PENDING
curl -b jar -X POST localhost:8080/api/images -F file=@photo.jpg

# 4. poll until status flips to DONE (usually a few seconds)
curl -b jar localhost:8080/api/images/mine

# 5. search everyone's images — no session needed
curl 'localhost:8080/api/images/search?q=sunset'

# 6. fetch the bytes
curl -o out.jpg localhost:8080/api/images/12/raw

# 7. clean up
curl -b jar -X DELETE localhost:8080/api/images/12
curl -b jar -X POST localhost:8080/api/auth/logout
```
