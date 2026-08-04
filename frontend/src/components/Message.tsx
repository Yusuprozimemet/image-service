import type { ReactNode } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import CircularProgress from '@mui/material/CircularProgress'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'

/** Whole-page states: loading, failed, or nothing to show. */

export function LoadingState({ label = 'Loading…' }: { label?: string }) {
  return (
    <Stack spacing={2} sx={{ py: 10, alignItems: 'center', justifyContent: 'center' }}>
      <CircularProgress thickness={5} size={40} />
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
    </Stack>
  )
}

export function ErrorState({ error }: { error: unknown }) {
  const message = error instanceof Error ? error.message : 'Something went wrong'
  return (
    <Alert severity="error" variant="outlined" sx={{ borderColor: 'error.main' }}>
      {message}
    </Alert>
  )
}

interface EmptyStateProps {
  /** A big friendly glyph. Decorative — the title carries the meaning. */
  emoji?: string
  title: string
  children?: ReactNode
  action?: ReactNode
}

export function EmptyState({ emoji = '🖼️', title, children, action }: EmptyStateProps) {
  return (
    <Paper
      variant="outlined"
      sx={{ py: 7, px: 3, textAlign: 'center', borderStyle: 'dashed', bgcolor: 'background.paper' }}
    >
      <Box sx={{ fontSize: 52, lineHeight: 1, mb: 1.5 }} aria-hidden>
        {emoji}
      </Box>
      <Typography variant="h3" gutterBottom>
        {title}
      </Typography>
      {children && (
        <Typography variant="body2" color="text.secondary">
          {children}
        </Typography>
      )}
      {action && <Box sx={{ mt: 3 }}>{action}</Box>}
    </Paper>
  )
}
