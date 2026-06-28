package com.tradepilot.service;

import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import com.tradepilot.observability.CacheMetricRecorder;

@Service
public class AggregateCacheService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AggregateCacheService.class);
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final CacheMetricRecorder cacheMetricRecorder;

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Duration ttl, Supplier<T> loader) {
        long startedAt = System.nanoTime();
        log.debug("Aggregate cache lookup start. cacheName={}, key={}", cacheName(key), key);
        CacheEntry existing = cache.get(key);
        if (existing != null && existing.expiresAt().isAfter(Instant.now())) {
            cacheMetricRecorder.record(cacheName(key), true);
            log.debug("Aggregate cache hit. cacheName={}, key={}, elapsedMs={}", cacheName(key), key, elapsedMs(startedAt));
            return (T) existing.value();
        }
        cacheMetricRecorder.record(cacheName(key), false);
        log.debug("Aggregate cache miss. cacheName={}, key={}", cacheName(key), key);
        long loaderStartedAt = System.nanoTime();
        T value = loader.get();
        long loaderElapsedMs = elapsedMs(loaderStartedAt);
        cache.put(key, new CacheEntry(value, Instant.now().plus(ttl)));
        log.debug("Aggregate cache loader completed. cacheName={}, key={}, loaderMs={}, totalMs={}", cacheName(key), key, loaderElapsedMs, elapsedMs(startedAt));
        return value;
    }

    public void evict(String key) {
        cache.remove(key);
    }

    public void evictByPrefix(String prefix) {
        cache.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private String cacheName(String key) {
        int delimiter = key.indexOf(':');
        return delimiter > 0 ? key.substring(0, delimiter) : key;
    }

    private long elapsedMs(long startedAtNanos) {
        return Duration.ofNanos(System.nanoTime() - startedAtNanos).toMillis();
    }


    private record CacheEntry(Object value, Instant expiresAt) {
    }

    @java.lang.SuppressWarnings("all")
    public AggregateCacheService(final CacheMetricRecorder cacheMetricRecorder) {
        this.cacheMetricRecorder = cacheMetricRecorder;
    }
}
