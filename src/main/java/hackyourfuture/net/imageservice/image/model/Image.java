package hackyourfuture.net.imageservice.image.model;

import java.time.Instant;

// Metadata for one uploaded image. The bytes themselves live in object storage
// (Backblaze B2) under objectKey; this record mirrors a row of the images table.
public record Image(
        int imageId,
        int userId,
        String objectKey,
        String contentType,
        String status,
        Tags tags,
        Instant createdAt) {
}
