import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { imagesApi } from '../../api/images'
import type { ImageSummary } from '../../api/types'

// Queries and mutations for images. The interesting part is the polling: an
// upload comes back PENDING and is tagged on a background thread, so a list
// holding a PENDING image has to keep asking until the server says otherwise.

export const imageKeys = {
  all: ['images'] as const,
  recent: () => [...imageKeys.all, 'recent'] as const,
  mine: () => [...imageKeys.all, 'mine'] as const,
  search: (query: string) => [...imageKeys.all, 'search', query] as const,
}

const POLL_INTERVAL_MS = 3000

// Poll while anything is still being tagged, then stop. Returning false is what
// switches polling off, so a settled list costs nothing.
function pollWhilePending(images: ImageSummary[] | undefined) {
  return images?.some((image) => image.status === 'PENDING') ? POLL_INTERVAL_MS : false
}

export function useRecentImages() {
  return useQuery({
    queryKey: imageKeys.recent(),
    queryFn: imagesApi.recent,
    refetchInterval: (query) => pollWhilePending(query.state.data),
  })
}

export function useMyImages() {
  return useQuery({
    queryKey: imageKeys.mine(),
    queryFn: imagesApi.mine,
    refetchInterval: (query) => pollWhilePending(query.state.data),
  })
}

export function useSearchImages(query: string) {
  const trimmed = query.trim()
  return useQuery({
    queryKey: imageKeys.search(trimmed),
    queryFn: () => imagesApi.search(trimmed),
    // An empty box is not a search.
    enabled: trimmed.length > 0,
  })
}

export function useUploadImage() {
  const queries = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => imagesApi.upload(file),
    // The new image lands in both listings, and it arrives PENDING — which is
    // what starts the polling above.
    onSuccess: () => queries.invalidateQueries({ queryKey: imageKeys.all }),
  })
}

export function useDeleteImage() {
  const queries = useQueryClient()
  return useMutation({
    mutationFn: (imageId: number) => imagesApi.remove(imageId),
    onSuccess: () => queries.invalidateQueries({ queryKey: imageKeys.all }),
  })
}
