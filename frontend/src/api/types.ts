// Mirrors the JSON the API returns. One place to look when a DTO changes on the
// Java side.

export type ImageStatus = 'PENDING' | 'DONE' | 'FAILED'

/** The three tag arrays the vision model fills in. Empty until status is DONE. */
export interface Tags {
  objects: string[]
  tags: string[]
  colors: string[]
}

/** An image in a listing. `url` points at the raw-bytes endpoint. */
export interface ImageSummary {
  imageId: number
  status: ImageStatus
  tags: Tags
  url: string
}

/** POST /api/images. Always starts PENDING; tagging happens in the background. */
export interface UploadResult {
  imageId: number
  status: ImageStatus
}

export interface User {
  userId: number
  email: string
}

/** Every tag on an image, flattened for display. */
export function allTags(tags: Tags): string[] {
  return [...tags.objects, ...tags.tags, ...tags.colors]
}
