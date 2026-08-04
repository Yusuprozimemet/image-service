import { useState } from 'react'
import { Link as RouterLink, Navigate, useNavigate } from 'react-router'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Link from '@mui/material/Link'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { ErrorState } from '../../components/Message'
import { useLogin, useRegister, useSession } from './useSession'

// Login and register are the same form against different mutations, so they
// share one component rather than two near-identical copies.
interface AuthPageProps {
  mode: 'login' | 'register'
}

const copy = {
  login: {
    emoji: '👋',
    title: 'Welcome back',
    subtitle: 'Log in to see your images.',
    submit: 'Log in',
    switchText: 'Need an account?',
    switchLabel: 'Sign up',
    switchTo: '/register',
  },
  register: {
    emoji: '🎉',
    title: 'Create an account',
    subtitle: 'It takes a few seconds.',
    submit: 'Sign up',
    switchText: 'Already have an account?',
    switchLabel: 'Log in',
    switchTo: '/login',
  },
} as const

export function AuthPage({ mode }: AuthPageProps) {
  const text = copy[mode]
  const navigate = useNavigate()
  const { isLoggedIn } = useSession()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  // Both mutations are created every render — hooks cannot be called
  // conditionally — and only the one for this mode is used.
  const login = useLogin()
  const register = useRegister()
  const submit = mode === 'login' ? login : register

  if (isLoggedIn) {
    return <Navigate to="/my" replace />
  }

  return (
    <Box sx={{ maxWidth: 440, mx: 'auto' }}>
      <Box sx={{ textAlign: 'center', mb: 3 }}>
        <Box sx={{ fontSize: 48, lineHeight: 1, mb: 1 }} aria-hidden>
          {text.emoji}
        </Box>
        <Typography variant="h1" gutterBottom>
          {text.title}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {text.subtitle}
        </Typography>
      </Box>

      <Paper variant="outlined" sx={{ p: { xs: 2.5, sm: 4 } }}>
        <Stack
          component="form"
          spacing={2.5}
          onSubmit={(event) => {
            event.preventDefault()
            submit.mutate({ email, password }, { onSuccess: () => navigate('/my') })
          }}
        >
          <TextField
            label="Email"
            type="email"
            required
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            fullWidth
          />
          <TextField
            label="Password"
            type="password"
            required
            autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            helperText={mode === 'register' ? 'At least 8 characters' : undefined}
            slotProps={{ htmlInput: { minLength: 8 } }}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            fullWidth
          />

          {submit.isError && <ErrorState error={submit.error} />}

          <Button type="submit" size="large" loading={submit.isPending} fullWidth>
            {text.submit}
          </Button>
        </Stack>
      </Paper>

      <Typography variant="body2" color="text.secondary" sx={{ mt: 3, textAlign: 'center' }}>
        {text.switchText}{' '}
        <Link component={RouterLink} to={text.switchTo} underline="hover" sx={{ fontWeight: 800 }}>
          {text.switchLabel}
        </Link>
      </Typography>
    </Box>
  )
}
