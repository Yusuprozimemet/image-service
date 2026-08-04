import Box from '@mui/material/Box'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { EmptyState, ErrorState, LoadingState } from '../../components/Message'
import { ImageGrid } from './ImageGrid'
import { UploadPanel } from './UploadPanel'
import { useDeleteImage, useMyImages } from './useImages'

export function MyImagesPage() {
  const { data, isPending, isError, error } = useMyImages()
  const remove = useDeleteImage()

  const tagged = data?.filter((image) => image.status === 'DONE').length ?? 0

  return (
    <Stack spacing={4}>
      <Box>
        <Typography variant="h1" gutterBottom>
          My images
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {data && data.length > 0
            ? `${data.length} uploaded · ${tagged} tagged`
            : 'Upload an image and it gets tagged within a few seconds.'}
        </Typography>
      </Box>

      <UploadPanel />

      {remove.isError && <ErrorState error={remove.error} />}

      {isPending && <LoadingState label="Loading your images…" />}
      {isError && <ErrorState error={error} />}
      {data &&
        (data.length > 0 ? (
          <ImageGrid
            images={data}
            onDelete={(imageId) => remove.mutate(imageId)}
            deletingId={remove.isPending ? remove.variables : null}
          />
        ) : (
          <EmptyState emoji="📸" title="Nothing uploaded yet">
            Your images will show up here.
          </EmptyState>
        ))}
    </Stack>
  )
}
