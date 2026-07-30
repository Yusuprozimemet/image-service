package hackyourfuture.net.imageservice.image.model;

import java.util.List;

// The AI tags for an image: objects, tags, and colors.
public record Tags(List<String> objects, List<String> tags, List<String> colors) {

    // Empty tags (not tagged yet).
    public static Tags empty() {
        return new Tags(List.of(), List.of(), List.of());
    }
}
