import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { authApi } from '../../api/auth'
import { ApiError } from '../../api/client'
import type { User } from '../../api/types'

// The session is server state like any other: one query against /api/auth/me.
// That is the whole reason this app needs no global store — the cookie is
// HttpOnly, so the server is the only thing that knows whether we are logged in.

export const sessionKey = ['session'] as const

export function useSession() {
  const query = useQuery<User | null>({
    queryKey: sessionKey,
    queryFn: async () => {
      try {
        return await authApi.me()
      } catch (error) {
        // Not logged in. That is an answer, so it resolves rather than throws —
        // otherwise every anonymous visitor sits in an error state.
        if (error instanceof ApiError && error.isUnauthorized) {
          return null
        }
        throw error
      }
    },
    staleTime: Infinity,
  })

  return {
    user: query.data ?? null,
    isLoggedIn: !!query.data,
    // Until this settles we do not know which it is, so guarded routes wait.
    isLoading: query.isPending,
  }
}

export function useLogin() {
  const queries = useQueryClient()
  return useMutation({
    mutationFn: ({ email, password }: { email: string; password: string }) => authApi.login(email, password),
    onSuccess: () => queries.invalidateQueries({ queryKey: sessionKey }),
  })
}

export function useRegister() {
  const queries = useQueryClient()
  return useMutation({
    // Registering does not log you in — the API issues a cookie on login only.
    mutationFn: async ({ email, password }: { email: string; password: string }) => {
      await authApi.register(email, password)
      await authApi.login(email, password)
    },
    onSuccess: () => queries.invalidateQueries({ queryKey: sessionKey }),
  })
}

export function useLogout() {
  const queries = useQueryClient()
  return useMutation({
    mutationFn: () => authApi.logout(),
    // Drop every cached response, not just the session: "my images" belonged to
    // the user who just left and must not survive into the next login.
    onSuccess: () => queries.clear(),
  })
}
