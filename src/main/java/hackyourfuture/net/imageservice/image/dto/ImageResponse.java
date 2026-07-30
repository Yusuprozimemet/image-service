package hackyourfuture.net.imageservice.image.dto;

import hackyourfuture.net.imageservice.image.model.Image;
import hackyourfuture.net.imageservice.image.model.Tags;

// What a client sees for an image in listings and search results. `url` is the
// proxy path that serves the actual bytes; the object storage key is never exposed.
public record ImageResponse(int imageId, String status, Tags tags, String url) {

    public static ImageResponse from(Image image) {
        return new ImageResponse(image.imageId(), image.status(), image.tags(), rawUrl(image.imageId()));
    }

    private static String rawUrl(int imageId) {
        return "/api/images/" + imageId + "/raw";
    }
}
