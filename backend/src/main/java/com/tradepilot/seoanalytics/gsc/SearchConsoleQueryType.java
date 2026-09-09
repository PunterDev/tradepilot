package com.tradepilot.seoanalytics.gsc;

import java.util.List;

enum SearchConsoleQueryType {
    PAGE(List.of("page")),
    QUERY(List.of("query")),
    QUERY_PAGE(List.of("query", "page")),
    DATE(List.of("date"));

    private final List<String> dimensions;

    SearchConsoleQueryType(List<String> dimensions) {
        this.dimensions = dimensions;
    }

    List<String> dimensions() {
        return dimensions;
    }
}
