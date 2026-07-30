package hackyourfuture.net.imageservice.tagging.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import hackyourfuture.net.imageservice.image.model.Image;
import hackyourfuture.net.imageservice.image.model.Tags;
import hackyourfuture.net.imageservice.image.repository.ImageRepository;
import hackyourfuture.net.imageservice.image.service.FileService;
import hackyourfuture.net.imageservice.tagging.client.LlmClient;

// Tags one image: get its bytes, ask the LLM, save the tags and set DONE (or FAILED).
@Service
public class TaggingService {

    private static final Logger log = LoggerFactory.getLogger(TaggingService.class);

    private static final String PENDING = "PENDING";
    private static final String DONE = "DONE";
    private static final String FAILED = "FAILED";

    private final ImageRepository images;
    private final FileService files;
    private final LlmClient llm;

    public TaggingService(ImageRepository images, FileService files, LlmClient llm) {
        this.images = images;
        this.files = files;
        this.llm = llm;
    }

    // Tag the image. Only runs if it's still PENDING. Sets DONE on success,
    // FAILED on error.
    public void tag(int imageId) {
        Image image = images.findById(imageId).orElse(null);
        if (image == null || !PENDING.equals(image.status())) {
            return;
        }
        try {
            byte[] bytes = files.get(image.objectKey());
            Tags tags = llm.tag(bytes, image.contentType());
            images.updateTags(imageId, tags, DONE);
        } catch (RuntimeException e) {
            log.warn("tagging failed for image {}: {}", imageId, e.getMessage());
            images.updateTags(imageId, Tags.empty(), FAILED);
        }
    }
}
