package hackyourfuture.net.imageservice.image.model;

import java.time.Instant;

// One image's info (a row of the images table). Bytes live in B2 under objectKey.
public record Image(
        int imageId,
        int userId,
        String objectKey,
        String contentType,
        String status,
        Tags tags,
        Instant createdAt) {
}
