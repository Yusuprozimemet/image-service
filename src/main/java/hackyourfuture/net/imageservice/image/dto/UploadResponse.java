package hackyourfuture.net.imageservice.image.dto;

import hackyourfuture.net.imageservice.image.model.Image;

// Returned after upload. Starts PENDING; tagging sets it to DONE.
public record UploadResponse(int imageId, String status) {

    public static UploadResponse from(Image image) {
        return new UploadResponse(image.imageId(), image.status());
    }
}
