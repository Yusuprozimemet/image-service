package hackyourfuture.net.imageservice.image.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import hackyourfuture.net.imageservice.image.model.Image;
import hackyourfuture.net.imageservice.image.model.RawImage;

// Business logic for images: upload to object storage, list, delete, search, and
// fetch raw bytes for the proxy.
@Service
public class ImageService {

    // Validate (size <= 10MB, real image bytes), store in the bucket, insert a
    // PENDING row, and kick off tagging. Returns the created image.
    public Image upload(int userId, MultipartFile file) {
        throw notImplemented();
    }

    // All images owned by the given user, newest first.
    public List<Image> listMine(int userId) {
        throw notImplemented();
    }

    // Delete an image the user owns — both the DB row and the stored object.
    // Must reject (403/404) images that aren't theirs.
    public void delete(int userId, int imageId) {
        throw notImplemented();
    }

    // The 50 most recent images for the public home page.
    public List<Image> listRecent() {
        throw notImplemented();
    }

    // Free-text search over the jsonb tags (objects / tags / colors).
    public List<Image> search(String query) {
        throw notImplemented();
    }

    // The bytes + content type for one image, streamed from the private bucket.
    public RawImage getRaw(int imageId) {
        throw notImplemented();
    }

    private static UnsupportedOperationException notImplemented() {
        return new UnsupportedOperationException("ImageService not implemented yet");
    }
}
