package com.hr.performancepulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Entry point for the Talent Performance Pulse backend.
 *
 * <p>Annotations active from day one so the LLD extension points
 * (caching, JPA auditing) are wired in without future refactoring:
 * <ul>
 *   <li>{@code @EnableCaching}    – activates Spring Cache (Caffeine now, Redis later)</li>
 *   <li>{@code @EnableJpaAuditing} – drives @CreatedDate / @LastModifiedDate on AuditEntity</li>
 * </ul>
 */
@SpringBootApplication
@EnableCaching
@EnableJpaAuditing(auditorAwareRef = "auditAwareImpl")
public class PerformancePulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(PerformancePulseApplication.class, args);
    }
}
