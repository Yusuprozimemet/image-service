// The only place in the app that calls fetch. Everything else goes through the
// typed wrappers in auth.ts and images.ts.

/** A non-2xx response, carrying what the server said so the UI can show it. */
export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }

  /** 401 means "no session" — the session query treats it as logged out, not as a failure. */
  get isUnauthorized(): boolean {
    return this.status === 401
  }
}

// The API answers {"error": "..."} on failure, but not for every status — an
// upstream 502 or a proxy error can be HTML. Fall back to something readable.
async function toError(response: Response): Promise<ApiError> {
  let message = `request failed (${response.status})`
  try {
    const body = await response.json()
    if (typeof body?.error === 'string') {
      message = body.error
    }
  } catch {
    // Not JSON. Keep the generic message.
  }
  return new ApiError(response.status, message)
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    // Send the session cookie. Same-origin in production, and the dev server
    // proxies /api so it stays same-origin there too.
    credentials: 'same-origin',
    ...init,
  })

  if (!response.ok) {
    throw await toError(response)
  }
  // 204 (login, logout, delete) has no body to parse.
  if (response.status === 204) {
    return undefined as T
  }
  return response.json() as Promise<T>
}

export const api = {
  get: <T>(path: string) => request<T>(path),

  postJson: <T>(path: string, body: unknown) =>
    request<T>(path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    }),

  // Multipart: no Content-Type header, the browser has to add the boundary.
  postForm: <T>(path: string, form: FormData) => request<T>(path, { method: 'POST', body: form }),

  delete: (path: string) => request<void>(path, { method: 'DELETE' }),
}
