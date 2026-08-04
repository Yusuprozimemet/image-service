import { Link as RouterLink } from 'react-router'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { EmptyState, ErrorState, LoadingState } from '../../components/Message'
import { useSession } from '../auth/useSession'
import { ImageGrid } from './ImageGrid'
import { useRecentImages } from './useImages'

export function HomePage() {
  const { isLoggedIn } = useSession()
  const { data, isPending, isError, error } = useRecentImages()

  return (
    <Stack spacing={4}>
      {!isLoggedIn && (
        <Box
          sx={{
            borderRadius: 4,
            p: { xs: 3, sm: 5 },
            textAlign: 'center',
            bgcolor: '#F2FBE9',
            border: '2px solid',
            borderColor: 'primary.main',
          }}
        >
          <Box sx={{ fontSize: 48, lineHeight: 1, mb: 1 }} aria-hidden>
            ✨
          </Box>
          <Typography variant="h1" gutterBottom>
            Upload a photo, get instant tags
          </Typography>
          <Typography color="text.secondary" sx={{ mb: 3, maxWidth: 460, mx: 'auto' }}>
            An AI vision model looks at your image and labels what it sees — objects, themes and colours.
          </Typography>
          <Button component={RouterLink} to="/register" size="large">
            Get started
          </Button>
        </Box>
      )}

      <Box>
        <Typography variant="h2" gutterBottom>
          Fresh from the community
        </Typography>
        <Typography variant="body2" color="text.secondary">
          The 50 newest images. Tap any tag to find more like it.
        </Typography>
      </Box>

      {isPending && <LoadingState />}
      {isError && <ErrorState error={error} />}
      {data &&
        (data.length > 0 ? (
          <ImageGrid images={data} />
        ) : (
          <EmptyState emoji="🌱" title="Nothing here yet">
            Be the first to upload something.
          </EmptyState>
        ))}
    </Stack>
  )
}
