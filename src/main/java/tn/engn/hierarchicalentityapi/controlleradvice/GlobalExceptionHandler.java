package tn.engn.hierarchicalentityapi.controlleradvice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tn.engn.hierarchicalentityapi.exception.*;

import java.time.LocalDateTime;

/**
 * A global exception handler to catch and handle exceptions throughout the application.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles ValidationException and returns a structured error response.
     *
     * @param ex the ValidationException.
     * @return the ErrorResponse with HTTP status 400 (Bad Request).
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), LocalDateTime.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles ParentEntityNotFoundException and returns a structured error response.
     *
     * @param ex the ParentEntityNotFoundException.
     * @return the ErrorResponse with HTTP status 404 (Not Found).
     */
    @ExceptionHandler(ParentEntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleParentEntityNotFoundException(ParentEntityNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value(), LocalDateTime.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles DataIntegrityException and returns a structured error response.
     *
     * @param ex the DataIntegrityException.
     * @return the ErrorResponse with HTTP status 409 (Conflict).
     */
    @ExceptionHandler(DataIntegrityException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityException(DataIntegrityException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), HttpStatus.CONFLICT.value(), LocalDateTime.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handles EntityNotFoundException and returns a structured error response.
     *
     * @param ex the EntityNotFoundException.
     * @return the ErrorResponse with HTTP status 404 (Not Found).
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value(), LocalDateTime.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles MethodArgumentNotValidException for validation errors on request parameters.
     *
     * @param ex the MethodArgumentNotValidException.
     * @return the ErrorResponse with HTTP status 400 (Bad Request).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        ErrorResponse errorResponse = new ErrorResponse("Validation failed for request parameters.", HttpStatus.BAD_REQUEST.value(), LocalDateTime.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles generic exceptions and returns a structured error response.
     *
     * @param ex the Exception.
     * @return the ErrorResponse with HTTP status 500 (Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse errorResponse = new ErrorResponse("An unexpected error occurred.", HttpStatus.INTERNAL_SERVER_ERROR.value(), LocalDateTime.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
