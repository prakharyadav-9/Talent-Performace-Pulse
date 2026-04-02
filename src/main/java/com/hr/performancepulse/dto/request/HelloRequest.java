package com.hr.performancepulse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request body for {@code POST /api/v1/hello}.
 *
 * <p>Demonstrates the DTO + Bean Validation pattern that every
 * real request DTO in the LLD (e.g. {@code CreateEmployeeRequest},
 * {@code SubmitReviewRequest}) will follow.
 */
@Getter
@NoArgsConstructor
public class HelloRequest {

    /**
     * The caller's name to greet.
     * Maps to {@code CreateEmployeeRequest.firstName} pattern (LLD §9.1).
     */
    @NotBlank(message = "name must not be blank")
    @Size(max = 100, message = "name must not exceed 100 characters")
    private String name;

    /**
     * Optional custom greeting message.
     */
    @Size(max = 255, message = "message must not exceed 255 characters")
    private String message;
}
