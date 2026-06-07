package com.tradepilot.dto.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageRequestParams {
    private static final int DEFAULT_SIZE = 25;
    private static final int MAX_SIZE = 50;

    private PageRequestParams() {
    }

    public static Pageable pageable(int page, int size, String sortBy, String sortDir, String defaultSortBy) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        String safeSortBy = sortBy == null || sortBy.isBlank() ? defaultSortBy : sortBy;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy));
    }
}
