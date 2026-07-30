package hackyourfuture.net.imageservice.tagging.client;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import hackyourfuture.net.imageservice.image.model.Tags;
import hackyourfuture.net.imageservice.shared.ApiException;

// Calls Google Gemini to tag an image. Sends the image + prompt, returns
// objects / tags / colors from the reply.
@Component
public class LlmClient {

    // Prompt sent with every image.
    private static final String PROMPT = """
            You are an image tagging assistant. Look at the image and return JSON with three arrays:
            - "objects": concrete physical things visible in the image (e.g. tree, car, cloud, person).
            - "tags": everything that is not a physical object — setting, time of day, weather, mood,
              style, activity, season.
            - "colors": up to 3 of the most prominent colours, each chosen ONLY from this list of 11
              basic colours: red, orange, yellow, green, blue, purple, pink, brown, black, white, gray.
            Use lowercase single words or short phrases. Do not invent things that are not visible.""";

    // A reusable array-of-strings schema node.
    private static final Map<String, Object> STRING_ARRAY =
            Map.of("type", "ARRAY", "items", Map.of("type", "STRING"));

    // Force the reply to be { objects, tags, colors }.
    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "objects", STRING_ARRAY,
                    "tags", STRING_ARRAY,
                    "colors", STRING_ARRAY),
            "required", List.of("objects", "tags", "colors"));

    private final RestClient rest;
    private final JsonMapper json;
    private final String model;

    public LlmClient(JsonMapper json,
                     @Value("${app.gemini.base-url}") String baseUrl,
                     @Value("${app.gemini.api-key}") String apiKey,
                     @Value("${app.gemini.model}") String model) {
        this.rest = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
        this.json = json;
        this.model = model;
    }

    // Send the image to Gemini and read the tags from the reply.
    public Tags tag(byte[] imageBytes, String contentType) {
        GeminiResponse response = rest.post()
                .uri("/models/{model}:generateContent", model)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildRequest(imageBytes, contentType))
                .retrieve()
                .body(GeminiResponse.class);

        String text = extractText(response);
        try {
            return json.readValue(text, Tags.class);
        } catch (JacksonException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "could not parse tags from the LLM reply");
        }
    }

    private static GeminiRequest buildRequest(byte[] imageBytes, String contentType) {
        Part image = new Part(null, new InlineData(contentType, imageBytes));
        Part prompt = new Part(PROMPT, null);
        Content content = new Content(List.of(image, prompt));
        GenerationConfig config = new GenerationConfig("application/json", RESPONSE_SCHEMA);
        return new GeminiRequest(List.of(content), config);
    }

    // Get the text from the reply, or fail.
    private static String extractText(GeminiResponse response) {
        if (response == null
                || response.candidates() == null
                || response.candidates().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "the LLM returned no tags");
        }
        Content content = response.candidates().get(0).content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "the LLM returned no tags");
        }
        return content.parts().get(0).text();
    }

    // --- Gemini request/response shapes. Null fields are dropped. ---

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record Content(List<Part> parts) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record Part(String text, @JsonProperty("inline_data") InlineData inlineData) {
    }

    // Jackson base64-encodes the bytes.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record InlineData(@JsonProperty("mime_type") String mimeType, byte[] data) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record GenerationConfig(String responseMimeType, Object responseSchema) {
    }

    private record GeminiResponse(List<Candidate> candidates) {
    }

    private record Candidate(Content content) {
    }
}
