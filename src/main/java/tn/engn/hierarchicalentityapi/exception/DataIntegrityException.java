package tn.engn.hierarchicalentityapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception thrown when data integrity violations occur.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DataIntegrityException extends RuntimeException {

    /**
     * Constructs a new DataIntegrityException with the specified detail message.
     *
     * @param message the detail message.
     */
    public DataIntegrityException(String message) {
        super(message);
    }

    /**
     * Constructs a new DataIntegrityException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause the cause of the exception.
     */
    public DataIntegrityException(String message, Throwable cause) {
        super(message, cause);
    }
}
