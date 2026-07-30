-- Uploaded images. The bytes live in object storage (Backblaze B2); this table
-- holds only the metadata and the AI-generated tags.
CREATE TABLE images (
    image_id     SERIAL      PRIMARY KEY,
    user_id      INTEGER     NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    object_key   TEXT        NOT NULL,              -- key of the image in the bucket
    content_type TEXT        NOT NULL,              -- used when the proxy serves the bytes
    -- Tagging lifecycle: PENDING -> DONE, or FAILED after retries.
    status       TEXT        NOT NULL DEFAULT 'PENDING'
                 CHECK (status IN ('PENDING', 'DONE', 'FAILED')),
    -- AI tags: { "objects": [...], "tags": [...], "colors": [...] }
    tags         JSONB       NOT NULL DEFAULT '{}'::jsonb,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Keyword search matches against the JSONB tag arrays.
CREATE INDEX idx_images_tags ON images USING gin (tags);

-- "My images" listing.
CREATE INDEX idx_images_user_id ON images (user_id);
