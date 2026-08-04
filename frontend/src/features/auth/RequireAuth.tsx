import { Navigate, Outlet, useLocation } from 'react-router'
import { LoadingState } from '../../components/Message'
import { useSession } from './useSession'

// Gate for the logged-in routes. Convenience only — the real check is the
// session cookie on every /api call, which this cannot see or fake.
export function RequireAuth() {
  const { isLoggedIn, isLoading } = useSession()
  const location = useLocation()

  // Redirecting before /api/auth/me answers would bounce a logged-in user to
  // the login page on every reload.
  if (isLoading) {
    return <LoadingState label="Checking your session…" />
  }

  if (!isLoggedIn) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <Outlet />
}
