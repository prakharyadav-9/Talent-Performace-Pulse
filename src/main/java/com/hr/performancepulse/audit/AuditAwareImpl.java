package com.hr.performancepulse.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Provides the current "actor" for Spring Data JPA auditing.
 *
 * <p><b>LLD §3.1 – AuditEntity</b>
 * Populates the {@code @CreatedBy} and {@code @LastModifiedBy} columns
 * on every entity automatically.
 *
 * <p><b>Extension point:</b> Once authentication is wired (OAuth2 / JWT
 * per LLD §15), replace the hard-coded fallback with:
 * <pre>
 *   SecurityContextHolder.getContext()
 *       .getAuthentication()
 *       .getName()
 * </pre>
 * The {@code @EnableJpaAuditing(auditorAwareRef = "auditAwareImpl")}
 * annotation in {@link com.hr.performancepulse.PerformancePulseApplication}
 * is already wired to this bean name.
 */
@Component("auditAwareImpl")
public class AuditAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        // TODO (LLD §15): extract from SecurityContextHolder once JWT auth is wired
        return Optional.of("system");
    }
}
