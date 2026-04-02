package com.hr.performancepulse.service;

import com.hr.performancepulse.dto.request.HelloRequest;
import com.hr.performancepulse.dto.response.HelloResponse;

/**
 * Service contract for the Hello resource.
 *
 * <p><b>LLD §6 – Service Layer pattern</b>
 * Every feature module will define an interface here so that:
 * <ol>
 *   <li>Controllers depend on abstractions, not concrete classes.</li>
 *   <li>Implementations can be swapped (e.g. {@code CachingAnalyticsService}
 *       wrapping the real impl) without touching the controller.</li>
 *   <li>Unit tests mock the interface trivially.</li>
 * </ol>
 *
 * <p>Future service interfaces to add per LLD:
 * {@code EmployeeService}, {@code ReviewService}, {@code CycleService},
 * {@code GoalService}, {@code AnalyticsService}.
 */
public interface HelloService {

    /**
     * Returns a static greeting with server metadata.
     *
     * @return {@link HelloResponse} containing greeting and active profile
     */
    HelloResponse greet();

    /**
     * Returns a personalised greeting built from the request payload.
     *
     * @param request validated request body
     * @return {@link HelloResponse} with personalised message
     */
    HelloResponse greetWithName(HelloRequest request);
}
