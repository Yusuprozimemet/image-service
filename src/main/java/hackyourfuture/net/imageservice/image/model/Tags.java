package hackyourfuture.net.imageservice.image.model;

import java.util.List;

// The AI-generated tags for an image, stored as the jsonb `tags` column.
// Three categories per the assignment: concrete objects, free-form tags, and
// up to three prominent colours.
public record Tags(List<String> objects, List<String> tags, List<String> colors) {

    // The value used for a not-yet-tagged image (matches the DB default '{}').
    public static Tags empty() {
        return new Tags(List.of(), List.of(), List.of());
    }
}
