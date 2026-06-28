package com.tradepilot.dto.common;

import java.util.List;

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

    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements, int totalPages, boolean first, boolean last, String sortBy, String sortDir) {
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

    @java.lang.SuppressWarnings("all")
    public PagedResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public List<T> getContent() {
        return this.content;
    }

    @java.lang.SuppressWarnings("all")
    public int getPage() {
        return this.page;
    }

    @java.lang.SuppressWarnings("all")
    public int getSize() {
        return this.size;
    }

    @java.lang.SuppressWarnings("all")
    public long getTotalElements() {
        return this.totalElements;
    }

    @java.lang.SuppressWarnings("all")
    public int getTotalPages() {
        return this.totalPages;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isFirst() {
        return this.first;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isLast() {
        return this.last;
    }

    @java.lang.SuppressWarnings("all")
    public String getSortBy() {
        return this.sortBy;
    }

    @java.lang.SuppressWarnings("all")
    public String getSortDir() {
        return this.sortDir;
    }

    @java.lang.SuppressWarnings("all")
    public void setContent(final List<T> content) {
        this.content = content;
    }

    @java.lang.SuppressWarnings("all")
    public void setPage(final int page) {
        this.page = page;
    }

    @java.lang.SuppressWarnings("all")
    public void setSize(final int size) {
        this.size = size;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalElements(final long totalElements) {
        this.totalElements = totalElements;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalPages(final int totalPages) {
        this.totalPages = totalPages;
    }

    @java.lang.SuppressWarnings("all")
    public void setFirst(final boolean first) {
        this.first = first;
    }

    @java.lang.SuppressWarnings("all")
    public void setLast(final boolean last) {
        this.last = last;
    }

    @java.lang.SuppressWarnings("all")
    public void setSortBy(final String sortBy) {
        this.sortBy = sortBy;
    }

    @java.lang.SuppressWarnings("all")
    public void setSortDir(final String sortDir) {
        this.sortDir = sortDir;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof PagedResponse)) return false;
        final PagedResponse<?> other = (PagedResponse<?>) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getPage() != other.getPage()) return false;
        if (this.getSize() != other.getSize()) return false;
        if (this.getTotalElements() != other.getTotalElements()) return false;
        if (this.getTotalPages() != other.getTotalPages()) return false;
        if (this.isFirst() != other.isFirst()) return false;
        if (this.isLast() != other.isLast()) return false;
        final java.lang.Object this$content = this.getContent();
        final java.lang.Object other$content = other.getContent();
        if (this$content == null ? other$content != null : !this$content.equals(other$content)) return false;
        final java.lang.Object this$sortBy = this.getSortBy();
        final java.lang.Object other$sortBy = other.getSortBy();
        if (this$sortBy == null ? other$sortBy != null : !this$sortBy.equals(other$sortBy)) return false;
        final java.lang.Object this$sortDir = this.getSortDir();
        final java.lang.Object other$sortDir = other.getSortDir();
        if (this$sortDir == null ? other$sortDir != null : !this$sortDir.equals(other$sortDir)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PagedResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getPage();
        result = result * PRIME + this.getSize();
        final long $totalElements = this.getTotalElements();
        result = result * PRIME + (int) ($totalElements >>> 32 ^ $totalElements);
        result = result * PRIME + this.getTotalPages();
        result = result * PRIME + (this.isFirst() ? 79 : 97);
        result = result * PRIME + (this.isLast() ? 79 : 97);
        final java.lang.Object $content = this.getContent();
        result = result * PRIME + ($content == null ? 43 : $content.hashCode());
        final java.lang.Object $sortBy = this.getSortBy();
        result = result * PRIME + ($sortBy == null ? 43 : $sortBy.hashCode());
        final java.lang.Object $sortDir = this.getSortDir();
        result = result * PRIME + ($sortDir == null ? 43 : $sortDir.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "PagedResponse(content=" + this.getContent() + ", page=" + this.getPage() + ", size=" + this.getSize() + ", totalElements=" + this.getTotalElements() + ", totalPages=" + this.getTotalPages() + ", first=" + this.isFirst() + ", last=" + this.isLast() + ", sortBy=" + this.getSortBy() + ", sortDir=" + this.getSortDir() + ")";
    }
}
