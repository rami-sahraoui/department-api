package tn.engn.employeeapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A class to represent the structure of error responses returned to the client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
