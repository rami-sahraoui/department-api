package tn.engn.departmentapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception thrown when a specified department is not found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class DepartmentNotFoundException extends RuntimeException {

    /**
     * Constructs a new DepartmentNotFoundException with the specified detail message.
     *
     * @param message the detail message.
     */
    public DepartmentNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new DepartmentNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause the cause of the exception.
     */
    public DepartmentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
