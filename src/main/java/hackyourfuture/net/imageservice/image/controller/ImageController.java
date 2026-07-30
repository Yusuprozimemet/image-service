package hackyourfuture.net.imageservice.image.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import hackyourfuture.net.imageservice.image.dto.ImageResponse;
import hackyourfuture.net.imageservice.image.dto.UploadResponse;
import hackyourfuture.net.imageservice.image.model.Image;
import hackyourfuture.net.imageservice.image.model.RawImage;
import hackyourfuture.net.imageservice.image.service.ImageService;

// Image endpoints. Upload, my images, and delete need login; home, search, and
// raw are public.
@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService images;

    public ImageController(ImageService images) {
        this.images = images;
    }

    // Upload one image. Login required.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse upload(@AuthenticationPrincipal Integer userId,
                                 @RequestParam("file") MultipartFile file) {
        return UploadResponse.from(images.upload(userId, file));
    }

    // List my images. Login required.
    @GetMapping("/mine")
    public List<ImageResponse> mine(@AuthenticationPrincipal Integer userId) {
        return toResponses(images.listMine(userId));
    }

    // Delete my image. Login required.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Integer userId, @PathVariable("id") int id) {
        images.delete(userId, id);
    }

    // Home: the 50 newest images. Public.
    @GetMapping
    public List<ImageResponse> home() {
        return toResponses(images.listRecent());
    }

    // Search by tag. Public.
    @GetMapping("/search")
    public List<ImageResponse> search(@RequestParam("q") String q) {
        return toResponses(images.search(q));
    }

    // Serve the raw image bytes. Public — this is the image's URL.
    @GetMapping("/{id}/raw")
    public ResponseEntity<byte[]> raw(@PathVariable("id") int id) {
        RawImage raw = images.getRaw(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(raw.contentType()))
                .body(raw.data());
    }

    private static List<ImageResponse> toResponses(List<Image> images) {
        return images.stream().map(ImageResponse::from).toList();
    }
}
