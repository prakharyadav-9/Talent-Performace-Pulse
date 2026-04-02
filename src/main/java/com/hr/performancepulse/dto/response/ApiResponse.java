package com.hr.performancepulse.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Standard API response envelope used by all endpoints.
 *
 * <p><b>LLD §8.1 – REST Conventions</b>
 * <pre>
 * Success: { "status": "success", "data": {...},  "timestamp": "..." }
 * Error:   { "status": "error",   "code": "...",  "message": "...", "timestamp": "..." }
 * </pre>
 *
 * @param <T> the payload type; {@code null} for void responses
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final String status;
    private final T data;

    // Error fields – only present on failure responses
    private final String code;
    private final String message;

    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    // ── Factory helpers ───────────────────────────────────────────────────

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status("success")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .status("error")
                .code(code)
                .message(message)
                .build();
    }
}
