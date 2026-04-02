package com.hr.performancepulse.exception;

/**
 * Thrown when a requested resource (Employee, Cycle, Review, Goal) does not exist.
 * Maps to HTTP 404 in {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName, Object id) {
        super(String.format("%s not found with id: %s", resourceName, id));
    }
}
