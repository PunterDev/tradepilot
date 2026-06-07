package com.tradepilot.observability;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "query_performance_events", indexes = {
        @Index(name = "idx_query_performance_events_created_at", columnList = "created_at"),
        @Index(name = "idx_query_performance_events_query_name_created_at", columnList = "query_name, created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryPerformanceEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query_name", nullable = false, length = 140)
    private String queryName;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
