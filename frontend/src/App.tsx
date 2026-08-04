import { Link as RouterLink, Route, Routes } from 'react-router'
import Button from '@mui/material/Button'
import { Layout } from './components/Layout'
import { EmptyState } from './components/Message'
import { AuthPage } from './features/auth/AuthPage'
import { RequireAuth } from './features/auth/RequireAuth'
import { HomePage } from './features/images/HomePage'
import { MyImagesPage } from './features/images/MyImagesPage'
import { SearchPage } from './features/images/SearchPage'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="search" element={<SearchPage />} />
        <Route path="login" element={<AuthPage mode="login" />} />
        <Route path="register" element={<AuthPage mode="register" />} />

        <Route element={<RequireAuth />}>
          <Route path="my" element={<MyImagesPage />} />
        </Route>

        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  )
}

function NotFound() {
  return (
    <EmptyState
      emoji="🧭"
      title="Page not found"
      action={
        <Button component={RouterLink} to="/">
          Back to the gallery
        </Button>
      }
    >
      That link does not go anywhere.
    </EmptyState>
  )
}
