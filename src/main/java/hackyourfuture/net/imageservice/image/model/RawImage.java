package hackyourfuture.net.imageservice.image.model;

// The raw bytes of an image plus its content type, as fetched from the private
// bucket. Returned by the proxy endpoint so the browser can display the image.
public record RawImage(byte[] data, String contentType) {
}
