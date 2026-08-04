import Grid from '@mui/material/Grid'
import type { ImageSummary } from '../../api/types'
import { ImageCard } from './ImageCard'

interface ImageGridProps {
  images: ImageSummary[]
  onDelete?: (imageId: number) => void
  deletingId?: number | null
}

export function ImageGrid({ images, onDelete, deletingId }: ImageGridProps) {
  return (
    <Grid container spacing={3}>
      {images.map((image) => (
        <Grid key={image.imageId} size={{ xs: 12, sm: 6, md: 4 }}>
          <ImageCard image={image} onDelete={onDelete} deleting={deletingId === image.imageId} />
        </Grid>
      ))}
    </Grid>
  )
}
