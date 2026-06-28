package com.tradepilot.observability.dto;

public class CacheHealthDto {
    private String cacheName;
    private long hits;
    private long misses;
    private double hitRate;

    @java.lang.SuppressWarnings("all")
    CacheHealthDto(final String cacheName, final long hits, final long misses, final double hitRate) {
        this.cacheName = cacheName;
        this.hits = hits;
        this.misses = misses;
        this.hitRate = hitRate;
    }


    @java.lang.SuppressWarnings("all")
    public static class CacheHealthDtoBuilder {
        @java.lang.SuppressWarnings("all")
        private String cacheName;
        @java.lang.SuppressWarnings("all")
        private long hits;
        @java.lang.SuppressWarnings("all")
        private long misses;
        @java.lang.SuppressWarnings("all")
        private double hitRate;

        @java.lang.SuppressWarnings("all")
        CacheHealthDtoBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CacheHealthDto.CacheHealthDtoBuilder cacheName(final String cacheName) {
            this.cacheName = cacheName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CacheHealthDto.CacheHealthDtoBuilder hits(final long hits) {
            this.hits = hits;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CacheHealthDto.CacheHealthDtoBuilder misses(final long misses) {
            this.misses = misses;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CacheHealthDto.CacheHealthDtoBuilder hitRate(final double hitRate) {
            this.hitRate = hitRate;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public CacheHealthDto build() {
            return new CacheHealthDto(this.cacheName, this.hits, this.misses, this.hitRate);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "CacheHealthDto.CacheHealthDtoBuilder(cacheName=" + this.cacheName + ", hits=" + this.hits + ", misses=" + this.misses + ", hitRate=" + this.hitRate + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static CacheHealthDto.CacheHealthDtoBuilder builder() {
        return new CacheHealthDto.CacheHealthDtoBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public String getCacheName() {
        return this.cacheName;
    }

    @java.lang.SuppressWarnings("all")
    public long getHits() {
        return this.hits;
    }

    @java.lang.SuppressWarnings("all")
    public long getMisses() {
        return this.misses;
    }

    @java.lang.SuppressWarnings("all")
    public double getHitRate() {
        return this.hitRate;
    }

    @java.lang.SuppressWarnings("all")
    public void setCacheName(final String cacheName) {
        this.cacheName = cacheName;
    }

    @java.lang.SuppressWarnings("all")
    public void setHits(final long hits) {
        this.hits = hits;
    }

    @java.lang.SuppressWarnings("all")
    public void setMisses(final long misses) {
        this.misses = misses;
    }

    @java.lang.SuppressWarnings("all")
    public void setHitRate(final double hitRate) {
        this.hitRate = hitRate;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof CacheHealthDto)) return false;
        final CacheHealthDto other = (CacheHealthDto) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getHits() != other.getHits()) return false;
        if (this.getMisses() != other.getMisses()) return false;
        if (java.lang.Double.compare(this.getHitRate(), other.getHitRate()) != 0) return false;
        final java.lang.Object this$cacheName = this.getCacheName();
        final java.lang.Object other$cacheName = other.getCacheName();
        if (this$cacheName == null ? other$cacheName != null : !this$cacheName.equals(other$cacheName)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof CacheHealthDto;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $hits = this.getHits();
        result = result * PRIME + (int) ($hits >>> 32 ^ $hits);
        final long $misses = this.getMisses();
        result = result * PRIME + (int) ($misses >>> 32 ^ $misses);
        final long $hitRate = java.lang.Double.doubleToLongBits(this.getHitRate());
        result = result * PRIME + (int) ($hitRate >>> 32 ^ $hitRate);
        final java.lang.Object $cacheName = this.getCacheName();
        result = result * PRIME + ($cacheName == null ? 43 : $cacheName.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "CacheHealthDto(cacheName=" + this.getCacheName() + ", hits=" + this.getHits() + ", misses=" + this.getMisses() + ", hitRate=" + this.getHitRate() + ")";
    }
}
