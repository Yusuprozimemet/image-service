import { api } from './client'
import type { User } from './types'

// /api/auth. Login and logout return no body — the session lives in an HttpOnly
// cookie the browser manages for us, so `me()` is the only way to read it back.

export const authApi = {
  register: (email: string, password: string) => api.postJson<User>('/api/auth/register', { email, password }),

  login: (email: string, password: string) => api.postJson<void>('/api/auth/login', { email, password }),

  logout: () => api.postJson<void>('/api/auth/logout', {}),

  /** The logged-in user, or a 401 ApiError when there is no valid session. */
  me: () => api.get<User>('/api/auth/me'),
}
