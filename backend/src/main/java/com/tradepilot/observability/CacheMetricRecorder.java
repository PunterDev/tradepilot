package com.tradepilot.observability;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheMetricRecorder {
    private final CacheMetricRepository cacheMetricRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String cacheName, boolean hit) {
        cacheMetricRepository.save(CacheMetric.builder()
                .cacheName(limit(cacheName, 120))
                .hit(hit)
                .createdAt(LocalDateTime.now())
                .build());
        log.trace("Cache metric recorded. cacheName={}, hit={}", cacheName, hit);
    }

    private String limit(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
