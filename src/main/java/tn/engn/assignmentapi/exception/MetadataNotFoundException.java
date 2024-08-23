package tn.engn.assignmentapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception thrown when a specified metadata is not found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class MetadataNotFoundException extends RuntimeException {

    /**
     * Constructs a new MetadataNotFoundException with the specified detail message.
     *
     * @param message the detail message.
     */
    public MetadataNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new MetadataNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause the cause of the exception.
     */
    public MetadataNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
