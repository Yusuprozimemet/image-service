import { useRef, useState } from 'react'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Paper from '@mui/material/Paper'
import Snackbar from '@mui/material/Snackbar'
import Alert from '@mui/material/Alert'
import Typography from '@mui/material/Typography'
import CloudUploadRoundedIcon from '@mui/icons-material/CloudUploadRounded'
import { ErrorState } from '../../components/Message'
import { useUploadImage } from './useImages'

// Matches what the server accepts. This is a courtesy check for fast feedback —
// the server validates by magic bytes and enforces the size itself, so anything
// that slips past here still gets a clean 400.
const MAX_BYTES = 10 * 1024 * 1024
const ACCEPTED = 'image/jpeg,image/png,image/gif,image/webp,image/bmp'

export function UploadPanel() {
  const upload = useUploadImage()
  const inputRef = useRef<HTMLInputElement>(null)
  const [dragging, setDragging] = useState(false)
  const [rejected, setRejected] = useState<string | null>(null)
  const [celebrating, setCelebrating] = useState(false)

  function submit(file: File | undefined) {
    if (!file) {
      return
    }
    if (!file.type.startsWith('image/')) {
      setRejected(`${file.name} is not an image`)
      return
    }
    if (file.size > MAX_BYTES) {
      setRejected(`${file.name} is larger than 10MB`)
      return
    }
    setRejected(null)
    upload.mutate(file, { onSuccess: () => setCelebrating(true) })
  }

  return (
    <Box component="section">
      <Paper
        variant="outlined"
        onDragOver={(event) => {
          event.preventDefault()
          setDragging(true)
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={(event) => {
          event.preventDefault()
          setDragging(false)
          submit(event.dataTransfer.files[0])
        }}
        sx={{
          px: 3,
          py: 6,
          textAlign: 'center',
          borderStyle: 'dashed',
          borderWidth: 3,
          transition: 'background-color 120ms ease, border-color 120ms ease',
          borderColor: dragging ? 'primary.main' : 'divider',
          bgcolor: dragging ? '#F2FBE9' : 'background.paper',
        }}
      >
        <CloudUploadRoundedIcon
          sx={{ fontSize: 56, color: dragging ? 'primary.main' : 'text.secondary', mb: 1 }}
        />
        <Typography variant="h3" gutterBottom>
          {dragging ? 'Drop it!' : 'Drop an image here'}
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          JPEG, PNG, GIF, WebP or BMP · up to 10MB
        </Typography>

        <Button size="large" onClick={() => inputRef.current?.click()} loading={upload.isPending}>
          {upload.isPending ? 'Uploading' : 'Choose a file'}
        </Button>

        <input
          ref={inputRef}
          type="file"
          accept={ACCEPTED}
          hidden
          onChange={(event) => {
            submit(event.target.files?.[0])
            // Let the same file be picked again after a failed attempt.
            event.target.value = ''
          }}
        />
      </Paper>

      {rejected && (
        <Alert severity="warning" variant="outlined" sx={{ mt: 2, borderColor: 'warning.main' }}>
          {rejected}
        </Alert>
      )}
      {upload.isError && (
        <Box sx={{ mt: 2 }}>
          <ErrorState error={upload.error} />
        </Box>
      )}

      <Snackbar
        open={celebrating}
        autoHideDuration={4000}
        onClose={() => setCelebrating(false)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          severity="success"
          variant="filled"
          onClose={() => setCelebrating(false)}
          sx={{ color: '#FFFFFF' }}
        >
          Nice! Tagging it now — it will update on its own.
        </Alert>
      </Snackbar>
    </Box>
  )
}
