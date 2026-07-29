package hackyourfuture.net.imageservice.shared;

import org.springframework.http.HttpStatus;

// A business error we want to turn into a specific HTTP status and message.
// Thrown by services and translated to JSON by GlobalExceptionHandler.
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
