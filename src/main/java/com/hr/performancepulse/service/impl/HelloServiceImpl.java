package com.hr.performancepulse.service.impl;

import com.hr.performancepulse.dto.request.HelloRequest;
import com.hr.performancepulse.dto.response.HelloResponse;
import com.hr.performancepulse.service.HelloService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;

/**
 * Concrete implementation of {@link HelloService}.
 *
 * <p><b>LLD §6 – Service Layer</b>
 * All real service implementations (e.g. {@code EmployeeServiceImpl})
 * will live in this {@code impl} sub-package and follow the same rules:
 * <ul>
 *   <li>{@code @Service}           – Spring-managed bean</li>
 *   <li>{@code @Transactional}     – write path; readOnly=true for reads</li>
 *   <li>{@code @RequiredArgsConstructor} – constructor injection (no field @Autowired)</li>
 *   <li>{@code @Slf4j}             – structured logging</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HelloServiceImpl implements HelloService {

    /** Injected to expose the active Spring profile in responses. */
    private final Environment environment;

    /**
     * {@inheritDoc}
     *
     * <p>Read-only transaction: no DB writes occur.
     * Real read methods (e.g. {@code getEmployee}) will use the same pattern
     * to enable Hibernate read-optimizations and future read-replica routing.
     */
    @Override
    @Transactional(readOnly = true)
    public HelloResponse greet() {
        log.info("Serving default greeting");
        return HelloResponse.builder()
                .greeting("Hello from Talent Performance Pulse!")
                .activeProfile(activeProfileLabel())
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Write transaction boundary – mirrors how {@code submitReview()} or
     * {@code createEmployee()} will be structured: validate → build → persist → return DTO.
     * No actual DB write here yet, but the transaction scope is correct for extension.
     */
    @Override
    @Transactional
    public HelloResponse greetWithName(HelloRequest request) {
        log.info("Serving personalised greeting for name='{}'", request.getName());

        String greeting = Optional.ofNullable(request.getMessage())
                .filter(m -> !m.isBlank())
                .map(m -> String.format("Hello, %s! Your message: \"%s\"", request.getName(), m))
                .orElse(String.format("Hello, %s! Welcome to Talent Performance Pulse.", request.getName()));

        return HelloResponse.builder()
                .greeting(greeting)
                .activeProfile(activeProfileLabel())
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private String activeProfileLabel() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length == 0
                ? "default"
                : String.join(", ", Arrays.asList(profiles));
    }
}
