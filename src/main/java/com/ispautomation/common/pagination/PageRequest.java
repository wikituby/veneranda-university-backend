package com.ispautomation.common.pagination;

/**
 * Immutable request object for paginated, sortable, filterable list queries.
 */
public class PageRequest {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 200;

    private final int page;
    private final int size;
    private final String sortBy;
    private final String sortDir;
    private final String search;

    private PageRequest(Builder builder) {
        this.page = builder.page;
        this.size = builder.size;
        this.sortBy = builder.sortBy;
        this.sortDir = builder.sortDir;
        this.search = builder.search;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Convenience factory from raw query params with safe defaults.
     */
    public static PageRequest of(Integer page, Integer size, String sortBy, String sortDir, String search) {
        Builder b = builder();
        if (page != null) b.page(page);
        if (size != null) b.size(size);
        if (sortBy != null && !sortBy.isBlank()) b.sortBy(sortBy);
        if (sortDir != null && !sortDir.isBlank()) b.sortDir(sortDir);
        if (search != null && !search.isBlank()) b.search(search);
        return b.build();
    }

    public int getPage() { return page; }
    public int getSize() { return size; }
    public int getOffset() { return page * size; }
    public String getSortBy() { return sortBy; }
    public String getSortDir() { return sortDir; }
    public String getSearch() { return search; }

    public boolean isAscending() {
        return !"desc".equalsIgnoreCase(sortDir);
    }

    public static class Builder {
        private int page = DEFAULT_PAGE;
        private int size = DEFAULT_SIZE;
        private String sortBy = "id";
        private String sortDir = "asc";
        private String search;

        public Builder page(int page) {
            this.page = Math.max(0, page);
            return this;
        }

        public Builder size(int size) {
            this.size = Math.min(Math.max(1, size), MAX_SIZE);
            return this;
        }

        public Builder sortBy(String sortBy) {
            this.sortBy = sortBy;
            return this;
        }

        public Builder sortDir(String sortDir) {
            this.sortDir = sortDir;
            return this;
        }

        public Builder search(String search) {
            this.search = search;
            return this;
        }

        public PageRequest build() {
            return new PageRequest(this);
        }
    }
}