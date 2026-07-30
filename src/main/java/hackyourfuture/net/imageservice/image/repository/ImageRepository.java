package hackyourfuture.net.imageservice.image.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import hackyourfuture.net.imageservice.image.model.Image;
import hackyourfuture.net.imageservice.image.model.Tags;

// Reads and writes the images table. Tags are stored as jsonb.
@Repository
public class ImageRepository {

    private static final String COLUMNS =
            "image_id, user_id, object_key, content_type, status, tags, created_at";

    private final JdbcClient jdbc;
    private final JsonMapper json;
    private final RowMapper<Image> rowMapper;

    public ImageRepository(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
        this.rowMapper = (rs, rowNum) -> new Image(
                rs.getInt("image_id"),
                rs.getInt("user_id"),
                rs.getString("object_key"),
                rs.getString("content_type"),
                rs.getString("status"),
                parseTags(rs.getString("tags")),
                rs.getTimestamp("created_at").toInstant());
    }

    // Insert a new image (PENDING, no tags) and return it.
    public Image insert(int userId, String objectKey, String contentType) {
        return jdbc.sql("INSERT INTO images (user_id, object_key, content_type, status, tags)"
                        + " VALUES (?, ?, ?, 'PENDING', ?::jsonb)"
                        + " RETURNING " + COLUMNS)
                .params(userId, objectKey, contentType, writeTags(Tags.empty()))
                .query(rowMapper)
                .single();
    }

    // All images owned by the user, newest first.
    public List<Image> listByUser(int userId) {
        return jdbc.sql("SELECT " + COLUMNS
                        + " FROM images WHERE user_id = ? ORDER BY created_at DESC, image_id DESC")
                .param(userId)
                .query(rowMapper)
                .list();
    }

    // The 50 newest images.
    public List<Image> listRecent() {
        return jdbc.sql("SELECT " + COLUMNS
                        + " FROM images ORDER BY created_at DESC, image_id DESC LIMIT 50")
                .query(rowMapper)
                .list();
    }

    // Images whose tags contain the term (in objects, tags, or colors).
    public List<Image> searchByTag(String term) {
        return jdbc.sql("SELECT " + COLUMNS
                        + " FROM images"
                        + " WHERE tags @> ?::jsonb OR tags @> ?::jsonb OR tags @> ?::jsonb"
                        + " ORDER BY created_at DESC, image_id DESC"
                        + " LIMIT 50")
                .params(arrayContains("objects", term),
                        arrayContains("tags", term),
                        arrayContains("colors", term))
                .query(rowMapper)
                .list();
    }

    public Optional<Image> findById(int imageId) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM images WHERE image_id = ?")
                .param(imageId)
                .query(rowMapper)
                .optional();
    }

    public void deleteById(int imageId) {
        jdbc.sql("DELETE FROM images WHERE image_id = ?")
                .param(imageId)
                .update();
    }

    // Set the tags and status (used by tagging).
    public void updateTags(int imageId, Tags tags, String status) {
        jdbc.sql("UPDATE images SET tags = ?::jsonb, status = ? WHERE image_id = ?")
                .params(writeTags(tags), status, imageId)
                .update();
    }

    // Builds {"objects":["car"]} for the tag search.
    private String arrayContains(String category, String term) {
        return writeValue(java.util.Map.of(category, List.of(term)));
    }

    private String writeTags(Tags tags) {
        return writeValue(tags);
    }

    private String writeValue(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("could not serialize tags", e);
        }
    }

    // Read the tags json; missing arrays become empty.
    private Tags parseTags(String value) {
        try {
            Tags tags = json.readValue(value, Tags.class);
            return new Tags(
                    tags.objects() == null ? List.of() : tags.objects(),
                    tags.tags() == null ? List.of() : tags.tags(),
                    tags.colors() == null ? List.of() : tags.colors());
        } catch (JacksonException e) {
            throw new IllegalStateException("could not read tags json", e);
        }
    }
}
