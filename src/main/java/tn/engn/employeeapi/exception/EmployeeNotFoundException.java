package tn.engn.employeeapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception thrown when a specified employee is not found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class EmployeeNotFoundException extends RuntimeException {

    /**
     * Constructs a new EmployeeNotFoundException with the specified detail message.
     *
     * @param message the detail message.
     */
    public EmployeeNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new EmployeeNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause the cause of the exception.
     */
    public EmployeeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
