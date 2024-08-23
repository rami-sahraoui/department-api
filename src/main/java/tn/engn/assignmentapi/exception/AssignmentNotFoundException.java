package tn.engn.assignmentapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception thrown when a specified assignment is not found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class AssignmentNotFoundException extends RuntimeException {

    /**
     * Constructs a new AssignmentNotFoundException with the specified detail message.
     *
     * @param message the detail message.
     */
    public AssignmentNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new AssignmentNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause the cause of the exception.
     */
    public AssignmentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
