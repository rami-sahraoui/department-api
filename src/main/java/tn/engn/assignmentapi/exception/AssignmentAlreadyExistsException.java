package tn.engn.assignmentapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception thrown when an assignment already exists.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class AssignmentAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a new AssignmentAlreadyExistsException with the specified detail message.
     *
     * @param message the detail message.
     */
    public AssignmentAlreadyExistsException(String message) {
        super(message);
    }

    /**
     * Constructs a new AssignmentAlreadyExistsException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause the cause of the exception.
     */
    public AssignmentAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}

