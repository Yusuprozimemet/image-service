import { useSearchParams } from 'react-router'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import { EmptyState, ErrorState, LoadingState } from '../../components/Message'
import { ImageGrid } from './ImageGrid'
import { useSearchImages } from './useImages'

export function SearchPage() {
  // The query lives in the URL, not in component state, so a search is shareable
  // and the tag chips on every card can point straight at one.
  const [params, setParams] = useSearchParams()
  const query = params.get('q') ?? ''

  const { data, isPending, isError, error, isFetching } = useSearchImages(query)
  const searched = query.trim().length > 0

  return (
    <Stack spacing={4}>
      <Box>
        <Typography variant="h1" gutterBottom>
          Find an image
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Matches a whole tag, lowercased — “sunset”, not “sun”.
        </Typography>
      </Box>

      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={1.5}
        component="form"
        role="search"
        onSubmit={(event) => {
          event.preventDefault()
          const next = new FormData(event.currentTarget).get('q')
          setParams(typeof next === 'string' && next.trim() ? { q: next.trim() } : {})
        }}
      >
        <TextField
          name="q"
          type="search"
          fullWidth
          // Remounts when the URL changes (a tag chip), so the box shows the term
          // being searched while staying uncontrolled as you type.
          key={query}
          defaultValue={query}
          placeholder="sunset, dog, blue…"
          slotProps={{ htmlInput: { 'aria-label': 'Search images by tag' } }}
        />
        <Button type="submit" size="large" startIcon={<SearchRoundedIcon />} loading={isFetching}>
          Search
        </Button>
      </Stack>

      {!searched && (
        <EmptyState emoji="🔍" title="What are you looking for?">
          Try a tag from any image in the gallery.
        </EmptyState>
      )}
      {searched && isPending && <LoadingState label="Searching…" />}
      {isError && <ErrorState error={error} />}
      {searched &&
        data &&
        (data.length > 0 ? (
          <ImageGrid images={data} />
        ) : (
          <EmptyState emoji="🤔" title={`Nothing tagged “${query}”`}>
            Tags have to match in full.
          </EmptyState>
        ))}
    </Stack>
  )
}
