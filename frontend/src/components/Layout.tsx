import { Link as RouterLink, NavLink, Outlet } from 'react-router'
import AppBar from '@mui/material/AppBar'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Container from '@mui/material/Container'
import Stack from '@mui/material/Stack'
import Toolbar from '@mui/material/Toolbar'
import Typography from '@mui/material/Typography'
import CollectionsRoundedIcon from '@mui/icons-material/CollectionsRounded'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import PhotoLibraryRoundedIcon from '@mui/icons-material/PhotoLibraryRounded'
import { useLogout, useSession } from '../features/auth/useSession'

const NAV = [
  { to: '/', label: 'Gallery', icon: <CollectionsRoundedIcon />, end: true, private: false },
  { to: '/search', label: 'Search', icon: <SearchRoundedIcon />, end: false, private: false },
  { to: '/my', label: 'My images', icon: <PhotoLibraryRoundedIcon />, end: false, private: true },
]

export function Layout() {
  const { user, isLoggedIn, isLoading } = useSession()
  const logout = useLogout()

  return (
    <Box sx={{ minHeight: '100dvh', display: 'flex', flexDirection: 'column' }}>
      <AppBar position="sticky">
        <Container maxWidth="lg">
          <Toolbar disableGutters sx={{ gap: 1, flexWrap: 'wrap', py: 1 }}>
            <Typography
              component={RouterLink}
              to="/"
              variant="h3"
              sx={{ mr: 2, textDecoration: 'none', color: 'primary.main', letterSpacing: '-0.03em' }}
            >
              snap<Box component="span" sx={{ color: 'secondary.main' }}>tag</Box>
            </Typography>

            <Stack direction="row" spacing={0.5} component="nav">
              {NAV.filter((item) => !item.private || isLoggedIn).map((item) => (
                <Button
                  key={item.to}
                  component={NavLink}
                  to={item.to}
                  end={item.end}
                  variant="text"
                  color="inherit"
                  startIcon={item.icon}
                  sx={{
                    color: 'text.secondary',
                    px: 1.5,
                    // NavLink sets .active on the matching route.
                    '&.active': { color: 'secondary.main', bgcolor: 'action.hover' },
                  }}
                >
                  <Box sx={{ display: { xs: 'none', sm: 'block' } }}>{item.label}</Box>
                </Button>
              ))}
            </Stack>

            <Box sx={{ flexGrow: 1 }} />

            {/* Nothing until the session query settles, so the header does not
                flip from "Log in" to the user's email on every page load. */}
            {isLoading ? null : isLoggedIn ? (
              <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
                <Typography
                  variant="body2"
                  color="text.secondary"
                  sx={{ display: { xs: 'none', md: 'block' } }}
                >
                  {user?.email}
                </Typography>
                <Button variant="outlined" color="inherit" loading={logout.isPending} onClick={() => logout.mutate()}>
                  Log out
                </Button>
              </Stack>
            ) : (
              <Stack direction="row" spacing={1}>
                <Button component={RouterLink} to="/login" variant="text" color="inherit">
                  Log in
                </Button>
                <Button component={RouterLink} to="/register">
                  Sign up
                </Button>
              </Stack>
            )}
          </Toolbar>
        </Container>
      </AppBar>

      <Container maxWidth="lg" component="main" sx={{ flexGrow: 1, py: 5 }}>
        <Outlet />
      </Container>
    </Box>
  )
}
