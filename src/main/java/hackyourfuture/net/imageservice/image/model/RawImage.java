package hackyourfuture.net.imageservice.image.model;

// An image's bytes plus its type.
public record RawImage(byte[] data, String contentType) {
}
