package com.tradepilot.dto.common;

import lombok.Data;

import java.util.List;

@Data
public class PagedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private String sortBy;
    private String sortDir;

    public static <T> PagedResponse<T> of(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last,
            String sortBy,
            String sortDir
    ) {
        PagedResponse<T> response = new PagedResponse<>();
        response.setContent(content);
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(totalElements);
        response.setTotalPages(totalPages);
        response.setFirst(first);
        response.setLast(last);
        response.setSortBy(sortBy);
        response.setSortDir(sortDir);
        return response;
    }
}
