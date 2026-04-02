package com.hr.performancepulse.exception;

import com.hr.performancepulse.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralised exception handler for all REST controllers.
 *
 * <p><b>LLD §10 – Exception Handling</b>
 * Maps application exceptions to consistent {@link ApiResponse} error envelopes
 * with RFC 7807-inspired structure:
 * <pre>
 * {
 *   "status":    "error",
 *   "code":      "ERR_CODE",
 *   "message":   "Human-readable reason",
 *   "timestamp": "2025-01-15T10:30:00"
 * }
 * </pre>
 *
 * <p>Add new {@code @ExceptionHandler} methods here as feature modules
 * (Employee, Review, Cycle, Goal) are implemented per the LLD.
 * The full list of planned exception types is documented in LLD §10.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Domain Exceptions (stubs ready for LLD modules) ──────────────────

    /**
     * LLD §10 – 404 for any missing resource.
     * Thrown by: EmployeeServiceImpl, ReviewServiceImpl, CycleServiceImpl.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("RESOURCE_NOT_FOUND", ex.getMessage()));
    }

    /**
     * LLD §10 – 409 when the same reviewer submits twice in one cycle.
     */
    @ExceptionHandler(DuplicateReviewException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateReview(DuplicateReviewException ex) {
        log.warn("Duplicate review attempt: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("DUPLICATE_REVIEW", ex.getMessage()));
    }

    /**
     * LLD §10 – 422 when a cycle state transition is illegal
     * (e.g. closing an already-closed cycle).
     */
    @ExceptionHandler(InvalidCycleStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCycleState(InvalidCycleStateException ex) {
        log.warn("Invalid cycle state transition: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error("INVALID_CYCLE_STATE", ex.getMessage()));
    }

    /**
     * LLD §10 – 409 when editing a finalised review is attempted.
     */
    @ExceptionHandler(ReviewFinalizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleReviewFinalized(ReviewFinalizedException ex) {
        log.warn("Attempt to modify finalized review: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("REVIEW_FINALIZED", ex.getMessage()));
    }

    // ── Spring / Infrastructure Exceptions ───────────────────────────────

    /**
     * LLD §10 – 400 for @Valid / @Validated bean validation failures.
     * Returns a field-level error map so the client knows exactly which
     * fields failed and why.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "invalid value" : fe.getDefaultMessage(),
                        (existing, replacement) -> existing   // keep first message on duplicates
                ));

        log.warn("Validation failed: {}", fieldErrors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .status("error")
                        .code("VALIDATION_FAILED")
                        .message("Request validation failed. See 'data' for field-level errors.")
                        .data(fieldErrors)
                        .build());
    }

    /**
     * LLD §10 – 500 catch-all.
     * Logs the full stack trace internally; returns a safe message externally.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR",
                        "An unexpected error occurred. Please try again or contact support."));
    }
}
