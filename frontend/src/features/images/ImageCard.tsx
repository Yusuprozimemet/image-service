import { Link as RouterLink } from 'react-router'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Chip from '@mui/material/Chip'
import IconButton from '@mui/material/IconButton'
import Stack from '@mui/material/Stack'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import DeleteRoundedIcon from '@mui/icons-material/DeleteRounded'
import { allTags, type ImageSummary } from '../../api/types'
import { StatusBadge } from './StatusBadge'

interface ImageCardProps {
  image: ImageSummary
  /** Passed only where deleting makes sense — i.e. the owner's own listing. */
  onDelete?: (imageId: number) => void
  deleting?: boolean
}

export function ImageCard({ image, onDelete, deleting = false }: ImageCardProps) {
  const tags = allTags(image.tags)

  return (
    <Card
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        '&:hover': { transform: 'translateY(-4px)', borderColor: 'primary.main' },
        '&:hover .delete-action': { opacity: 1 },
      }}
    >
      <Box sx={{ position: 'relative', pt: '100%', bgcolor: '#F0F0F0' }}>
        <Box
          component="img"
          src={image.url}
          alt={tags.length ? `Image tagged ${tags.slice(0, 5).join(', ')}` : 'Untagged image'}
          loading="lazy"
          sx={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover' }}
        />

        <Box sx={{ position: 'absolute', top: 10, left: 10 }}>
          <StatusBadge status={image.status} />
        </Box>

        {onDelete && (
          <Tooltip title="Delete">
            <IconButton
              className="delete-action"
              onClick={() => onDelete(image.imageId)}
              disabled={deleting}
              aria-label="Delete image"
              sx={{
                position: 'absolute',
                top: 8,
                right: 8,
                bgcolor: 'rgba(255,255,255,0.92)',
                color: 'error.main',
                // Revealed on hover, but always there for keyboard and touch.
                opacity: { xs: 1, md: 0 },
                transition: 'opacity 120ms ease',
                '&:hover': { bgcolor: '#FFFFFF' },
                '&:focus-visible': { opacity: 1 },
              }}
            >
              <DeleteRoundedIcon />
            </IconButton>
          </Tooltip>
        )}
      </Box>

      <CardContent sx={{ flexGrow: 1 }}>
        {tags.length > 0 ? (
          <Stack direction="row" spacing={0.75} useFlexGap sx={{ flexWrap: 'wrap' }}>
            {tags.map((tag) => (
              // Every tag is a search: the API matches whole terms, so a tag from
              // one image always finds the others that share it.
              <Chip
                key={tag}
                label={tag}
                size="small"
                clickable
                component={RouterLink}
                to={`/search?q=${encodeURIComponent(tag)}`}
                sx={{
                  bgcolor: '#F0F7FF',
                  color: 'secondary.dark',
                  '&:hover': { bgcolor: 'secondary.main', color: '#FFFFFF' },
                }}
              />
            ))}
          </Stack>
        ) : (
          <Typography variant="body2" color="text.secondary">
            {image.status === 'PENDING' ? 'Waiting for tags…' : 'No tags'}
          </Typography>
        )}
      </CardContent>
    </Card>
  )
}
