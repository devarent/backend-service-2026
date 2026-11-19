package com.aryafatthurahman.backend.cache;

import com.aryafatthurahman.backend.config.AppConfig;
import com.aryafatthurahman.backend.order.dto.RevenueStatsResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.RedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived cache for the revenue aggregate.
 *
 * Redis is the primary store in production. If Redis is disabled (tests/dev) or
 * unavailable, reads and writes transparently fall back to a bounded in-process
 * map, so a cache outage degrades latency rather than availability.
 */
@ApplicationScoped
public class RevenueCache {

    private static final Logger LOG = Logger.getLogger(RevenueCache.class);
    private static final String CACHE_KEY = "orders:revenue:v1";

    private final AppConfig config;
    private final ObjectMapper objectMapper;
    private final Instance<RedisDataSource> redisDataSource;
    private final ConcurrentHashMap<String, Entry> fallback = new ConcurrentHashMap<>();

    @Inject
    public RevenueCache(AppConfig config,
                        ObjectMapper objectMapper,
                        Instance<RedisDataSource> redisDataSource) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.redisDataSource = redisDataSource;
    }

    public Optional<RevenueStatsResponse> get() {
        if (!config.cache().enabled()) {
            return Optional.empty();
        }

        if (config.cache().redisEnabled()) {
            try {
                String cached = redisDataSource.get().value(String.class).get(CACHE_KEY);
                if (cached != null) {
                    return Optional.of(objectMapper.readValue(cached, RevenueStatsResponse.class));
                }
            } catch (Exception exception) {
                LOG.debugf("Redis cache read failed, using local fallback: %s", exception.getMessage());
            }
        }

        Entry entry = fallback.get(CACHE_KEY);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            fallback.remove(CACHE_KEY, entry);
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(entry.json(), RevenueStatsResponse.class));
        } catch (JsonProcessingException exception) {
            fallback.remove(CACHE_KEY, entry);
            LOG.warnf("Discarding unreadable cached revenue document: %s", exception.getMessage());
            return Optional.empty();
        }
    }

    public void put(RevenueStatsResponse stats) {
        if (!config.cache().enabled()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(stats);
            long ttlSeconds = Math.max(1, config.cache().ttl().toSeconds());
            fallback.put(CACHE_KEY, new Entry(json, Instant.now().plus(config.cache().ttl())));

            if (config.cache().redisEnabled()) {
                try {
                    redisDataSource.get().value(String.class).setex(CACHE_KEY, ttlSeconds, json);
                } catch (Exception exception) {
                    LOG.debugf("Redis cache write failed, kept local fallback: %s", exception.getMessage());
                }
            }
        } catch (JsonProcessingException exception) {
            LOG.warnf("Could not serialize revenue document for caching: %s", exception.getMessage());
        }
    }

    private record Entry(String json, Instant expiresAt) {
    }
}
