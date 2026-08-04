import Chip from '@mui/material/Chip'
import CircularProgress from '@mui/material/CircularProgress'
import CheckCircleRoundedIcon from '@mui/icons-material/CheckCircleRounded'
import ErrorRoundedIcon from '@mui/icons-material/ErrorRounded'
import type { ImageStatus } from '../../api/types'

export function StatusBadge({ status }: { status: ImageStatus }) {
  if (status === 'PENDING') {
    return (
      <Chip
        size="small"
        label="Tagging"
        icon={<CircularProgress size={13} thickness={6} sx={{ color: 'inherit !important' }} />}
        sx={{ bgcolor: 'warning.main', color: '#7A5C00' }}
      />
    )
  }

  if (status === 'FAILED') {
    return (
      <Chip
        size="small"
        label="Failed"
        icon={<ErrorRoundedIcon sx={{ color: 'inherit !important' }} />}
        sx={{ bgcolor: 'error.main', color: '#FFFFFF' }}
      />
    )
  }

  return (
    <Chip
      size="small"
      label="Tagged"
      icon={<CheckCircleRoundedIcon sx={{ color: 'inherit !important' }} />}
      sx={{ bgcolor: 'primary.main', color: '#FFFFFF' }}
    />
  )
}
