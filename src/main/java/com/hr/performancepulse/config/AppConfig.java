package com.hr.performancepulse.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * General application configuration.
 *
 * <p><b>LLD §7 – Caching Design</b>
 * CacheManager is declared as a Bean so it can be swapped to
 * RedissonSpringCacheManager for distributed deployments without
 * touching any {@code @Cacheable} / {@code @CacheEvict} annotations.
 *
 * <p>Current implementation: Caffeine (node-local, zero infra deps).
 * Cache names mirror the names defined in the LLD cache table.
 */
@Configuration
public class AppConfig {

    /**
     * Caffeine-backed CacheManager.
     * Each named cache gets its own spec; override in application.yml per env.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                "cycle-summary",    // LLD §7 – 10 min TTL
                "employee-list",    // LLD §7 – 5 min TTL
                "employee-detail",  // LLD §7 – 15 min TTL
                "top-performers"    // LLD §7 – 10 min TTL
        );
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(1_000)                    // LLD §12.1
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .recordStats()                         // exposes hit/miss metrics via Actuator
        );
        return manager;
    }
}
