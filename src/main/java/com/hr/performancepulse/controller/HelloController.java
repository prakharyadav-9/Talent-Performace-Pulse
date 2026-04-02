package com.hr.performancepulse.controller;

import com.hr.performancepulse.dto.request.HelloRequest;
import com.hr.performancepulse.dto.response.ApiResponse;
import com.hr.performancepulse.dto.response.HelloResponse;
import com.hr.performancepulse.service.HelloService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health-check and smoke-test controller.
 *
 * <p>Base path: {@code /api/v1/hello}
 *
 * <p><b>LLD §8 – Controller conventions demonstrated here:</b>
 * <ul>
 *   <li>Extends {@link BaseController} for consistent response wrapping.</li>
 *   <li>Depends on service interface, not concrete class.</li>
 *   <li>Uses {@code @Valid} for automatic DTO validation; errors handled by
 *       {@link com.hr.performancepulse.exception.GlobalExceptionHandler}.</li>
 *   <li>Versioned path ({@code /api/v1/}) to support future API versions.</li>
 *   <li>Full OpenAPI annotations on every operation.</li>
 * </ul>
 *
 * <p>This controller will be removed (or converted to a pure health check)
 * once the Employee, Cycle, Review, and Goal controllers are implemented.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/hello")
@RequiredArgsConstructor
@Tag(name = "Hello", description = "Smoke-test endpoint – verify the service is running")
public class HelloController extends BaseController {

    private final HelloService helloService;

    // ── GET /api/v1/hello ─────────────────────────────────────────────────

    /**
     * Returns a default greeting with server metadata.
     *
     * <p>Useful for verifying Docker networking, load balancer health,
     * and active Spring profile from within a running container.
     */
    @GetMapping
    @Operation(
            summary = "Default greeting",
            description = "Returns a static greeting and the active Spring profile. Use this to verify the container is up."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Service is running",
                    content = @Content(schema = @Schema(implementation = HelloResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Service error")
    })
    public ResponseEntity<ApiResponse<HelloResponse>> hello() {
        log.debug("GET /api/v1/hello");
        return ok(helloService.greet());
    }

    // ── POST /api/v1/hello ────────────────────────────────────────────────

    /**
     * Returns a personalised greeting from the request body.
     *
     * <p>Demonstrates the full request-validation → service → response pipeline
     * that every write endpoint in the LLD will follow.
     */
    @PostMapping
    @Operation(
            summary = "Personalised greeting",
            description = "Accepts a name and optional message; returns a personalised greeting. " +
                    "Demonstrates the validated DTO → service → response pipeline."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Greeting created",
                    content = @Content(schema = @Schema(implementation = HelloResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation failed – name is blank or too long"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Service error")
    })
    public ResponseEntity<ApiResponse<HelloResponse>> helloWithName(
            @Valid @RequestBody HelloRequest request) {
        log.debug("POST /api/v1/hello – name='{}'", request.getName());
        return created(helloService.greetWithName(request));
    }
}
