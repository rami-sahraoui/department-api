package tn.engn.departmentapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception thrown when a specified parent department is not found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ParentDepartmentNotFoundException extends RuntimeException {

    /**
     * Constructs a new ParentDepartmentNotFoundException with the specified detail message.
     *
     * @param message the detail message.
     */
    public ParentDepartmentNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new ParentDepartmentNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause the cause of the exception.
     */
    public ParentDepartmentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
