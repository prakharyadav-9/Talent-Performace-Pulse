package com.hr.performancepulse.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Response payload for {@code GET /api/v1/hello} and
 * {@code POST /api/v1/hello}.
 *
 * <p>Uses {@code @Builder} – the same pattern all real response DTOs
 * (e.g. {@code EmployeeResponse}, {@code CycleSummaryResponse}) will use
 * when added per the LLD.
 */
@Getter
@Builder
public class HelloResponse {

    /** Greeting message composed by the service layer. */
    private final String greeting;

    /** Active Spring profile(s) – useful for verifying Docker env vars. */
    private final String activeProfile;

    /** Server-side timestamp of the response. */
    @Builder.Default
    private final LocalDateTime servedAt = LocalDateTime.now();
}
