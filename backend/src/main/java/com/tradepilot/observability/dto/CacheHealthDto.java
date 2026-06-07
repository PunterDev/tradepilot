package com.tradepilot.observability.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CacheHealthDto {
    private String cacheName;
    private long hits;
    private long misses;
    private double hitRate;
}
