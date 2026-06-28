package com.tradepilot.observability;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class QueryPerformanceEventRecorder {
    private final QueryPerformanceEventRepository queryPerformanceEventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String queryName, long durationMs) {
        queryPerformanceEventRepository.save(QueryPerformanceEvent.builder().queryName(limit(queryName, 140)).durationMs(durationMs).createdAt(LocalDateTime.now()).build());
    }

    private String limit(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    @java.lang.SuppressWarnings("all")
    public QueryPerformanceEventRecorder(final QueryPerformanceEventRepository queryPerformanceEventRepository) {
        this.queryPerformanceEventRepository = queryPerformanceEventRepository;
    }
}
