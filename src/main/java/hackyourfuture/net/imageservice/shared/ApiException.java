package hackyourfuture.net.imageservice.shared;

import org.springframework.http.HttpStatus;

// An error with an HTTP status and message. GlobalExceptionHandler turns it into JSON.
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
