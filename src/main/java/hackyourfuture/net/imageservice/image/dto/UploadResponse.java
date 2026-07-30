package hackyourfuture.net.imageservice.image.dto;

import hackyourfuture.net.imageservice.image.model.Image;

// Returned on a successful upload. The image starts as PENDING; tagging fills in
// the tags and flips it to DONE later.
public record UploadResponse(int imageId, String status) {

    public static UploadResponse from(Image image) {
        return new UploadResponse(image.imageId(), image.status());
    }
}
