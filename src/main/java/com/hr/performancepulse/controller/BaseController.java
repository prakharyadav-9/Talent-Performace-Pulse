package com.hr.performancepulse.controller;

import com.hr.performancepulse.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Base class for all REST controllers.
 *
 * <p><b>LLD §8.1 – Template Method pattern</b>
 * Provides consistent {@link ApiResponse} envelope construction so that
 * every controller returns the same JSON structure without duplicating code.
 *
 * <p>All feature controllers ({@code EmployeeController},
 * {@code ReviewCycleController}, etc.) will extend this class.
 */
public abstract class BaseController {

    /**
     * Wraps a payload in a 200 OK {@link ApiResponse}.
     */
    protected <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Wraps a payload in a 201 Created {@link ApiResponse}.
     * Used by all POST endpoints that persist new resources.
     */
    protected <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(data));
    }

    /**
     * Returns a 204 No Content response.
     * Used by soft-delete and finalization endpoints.
     */
    protected ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }
}
