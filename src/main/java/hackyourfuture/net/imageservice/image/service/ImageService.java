package hackyourfuture.net.imageservice.image.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import hackyourfuture.net.imageservice.image.model.Image;
import hackyourfuture.net.imageservice.image.model.RawImage;
import hackyourfuture.net.imageservice.image.repository.ImageRepository;
import hackyourfuture.net.imageservice.shared.ApiException;
import hackyourfuture.net.imageservice.tagging.service.TaggingService;

// Image logic: upload, list, delete, search, and get raw bytes. Uses FileService
// for storage and ImageRepository for the database.
@Service
public class ImageService {

    // Max image size: 10 MB.
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final ImageRepository images;
    private final FileService files;
    private final TaggingService tagging;

    public ImageService(ImageRepository images, FileService files, TaggingService tagging) {
        this.images = images;
        this.files = files;
        this.tagging = tagging;
    }

    // Check size and that it's really an image, store it, save a PENDING row, then
    // tag it. Tagging runs now and never fails the upload (on error the row is FAILED).
    public Image upload(int userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "no file uploaded");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "image is larger than 10MB");
        }
        byte[] bytes = readBytes(file);
        // Check the file is really an image, from its bytes.
        String contentType = detectContentType(bytes);
        if (contentType == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "file is not a supported image");
        }
        String objectKey = "images/" + UUID.randomUUID() + extensionFor(contentType);
        files.put(objectKey, bytes, contentType);
        Image image = images.insert(userId, objectKey, contentType);
        tagging.tag(image.imageId());
        return image;
    }

    // All images owned by the given user, newest first.
    public List<Image> listMine(int userId) {
        return images.listByUser(userId);
    }

    // Delete the user's own image (row + file). 404 if missing, 403 if not theirs.
    public void delete(int userId, int imageId) {
        Image image = images.findById(imageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "image not found"));
        if (image.userId() != userId) {
            throw new ApiException(HttpStatus.FORBIDDEN, "not your image");
        }
        files.delete(image.objectKey());
        images.deleteById(imageId);
    }

    // The 50 newest images (home page).
    public List<Image> listRecent() {
        return images.listRecent();
    }

    // Search the tags. Lowercased so it's case-insensitive.
    public List<Image> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return images.searchByTag(query.trim().toLowerCase());
    }

    // The bytes + type for one image.
    public RawImage getRaw(int imageId) {
        Image image = images.findById(imageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "image not found"));
        return new RawImage(files.get(image.objectKey()), image.contentType());
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "could not read the uploaded file");
        }
    }

    // Find the image type from its first bytes. Null if not a supported image.
    private static String detectContentType(byte[] b) {
        if (startsWith(b, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }
        if (startsWith(b, 0x89, 0x50, 0x4E, 0x47)) {
            return "image/png";
        }
        if (startsWith(b, 0x47, 0x49, 0x46, 0x38)) {
            return "image/gif";
        }
        if (b.length >= 12 && startsWith(b, 0x52, 0x49, 0x46, 0x46)
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        if (startsWith(b, 0x42, 0x4D)) {
            return "image/bmp";
        }
        return null;
    }

    private static boolean startsWith(byte[] b, int... prefix) {
        if (b.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((b[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/bmp" -> ".bmp";
            default -> "";
        };
    }
}
