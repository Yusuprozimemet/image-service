import { api } from './client'
import type { ImageSummary, UploadResult } from './types'

// /api/images. Home, search and the raw bytes are public; the rest needs a session.

export const imagesApi = {
  /** The 50 newest images from everyone. */
  recent: () => api.get<ImageSummary[]>('/api/images'),

  /** Whole-word tag match, e.g. `sunset` — not a prefix search. */
  search: (query: string) => api.get<ImageSummary[]>(`/api/images/search?q=${encodeURIComponent(query)}`),

  mine: () => api.get<ImageSummary[]>('/api/images/mine'),

  upload: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return api.postForm<UploadResult>('/api/images', form)
  },

  remove: (imageId: number) => api.delete(`/api/images/${imageId}`),
}
