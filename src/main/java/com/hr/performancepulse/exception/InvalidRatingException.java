package com.hr.performancepulse.exception;

/**
 * Thrown when review rating is outside valid range (1-5).
 * 
 * Maps to HTTP 400 Bad Request.
 */
public class InvalidRatingException extends RuntimeException {
    public InvalidRatingException(int rating) {
        super(String.format("Invalid rating %d. Rating must be between 1 and 5.", rating));
    }
}
