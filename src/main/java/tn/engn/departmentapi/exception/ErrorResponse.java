package tn.engn.departmentapi.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A class to represent the structure of error responses returned to the client.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {

    /**
     * The error message.
     */
    private String message;

    /**
     * The HTTP status code.
     */
    private int status;

    /**
     * The timestamp when the error occurred.
     */
    private LocalDateTime timestamp;
}
