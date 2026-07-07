package com.mcpgateway.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Enables Spring's caching abstraction with an in-process Caffeine cache.
 * Tool-schema reads are cached (they change rarely but are read often), which
 * cuts database round-trips. In a multi-node deployment this could be swapped
 * for a Redis-backed cache without changing the annotated methods.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String SCHEMAS_CACHE = "schemas";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(SCHEMAS_CACHE);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES));
        return manager;
    }
}
