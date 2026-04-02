package com.hr.performancepulse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hr.performancepulse.dto.request.HelloRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link HelloController}.
 *
 * <p><b>LLD §16 – Testing Strategy</b>
 * Uses {@code @SpringBootTest} + {@code MockMvc} to exercise the full
 * controller → service pipeline against H2 (dev profile).
 *
 * <p>All future controller tests ({@code EmployeeControllerTest},
 * {@code ReviewControllerTest}, etc.) will follow this exact pattern.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class HelloControllerTest {

    private static final String BASE_URL = "/api/v1/hello";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ── GET Tests ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/hello → 200 with success envelope")
    void get_hello_returns200_withSuccessEnvelope() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.greeting", containsString("Hello")))
                .andExpect(jsonPath("$.data.activeProfile", is("dev")))
                .andExpect(jsonPath("$.data.servedAt", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    // ── POST Tests ────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/hello with valid name → 201 with personalised greeting")
    void post_hello_validName_returns201_withPersonalisedGreeting() throws Exception {
        HelloRequest request = buildRequest("Alice", null);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.greeting", containsString("Alice")))
                .andExpect(jsonPath("$.data.activeProfile", is("dev")));
    }

    @Test
    @DisplayName("POST /api/v1/hello with name and message → 201 with message in greeting")
    void post_hello_withMessage_returns201_greetingContainsMessage() throws Exception {
        HelloRequest request = buildRequest("Bob", "Great platform!");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.greeting", containsString("Bob")))
                .andExpect(jsonPath("$.data.greeting", containsString("Great platform!")));
    }

    @Test
    @DisplayName("POST /api/v1/hello with blank name → 400 VALIDATION_FAILED")
    void post_hello_blankName_returns400_validationFailed() throws Exception {
        String body = """
                { "name": "" }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.data.name", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/hello with missing name field → 400 VALIDATION_FAILED")
    void post_hello_missingName_returns400() throws Exception {
        String body = """
                { "message": "no name provided" }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("POST /api/v1/hello with name exceeding 100 chars → 400 VALIDATION_FAILED")
    void post_hello_nameTooLong_returns400() throws Exception {
        HelloRequest request = buildRequest("A".repeat(101), null);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private HelloRequest buildRequest(String name, String message) throws Exception {
        String json = message == null
                ? String.format("{\"name\":\"%s\"}", name)
                : String.format("{\"name\":\"%s\",\"message\":\"%s\"}", name, message);
        return objectMapper.readValue(json, HelloRequest.class);
    }
}
