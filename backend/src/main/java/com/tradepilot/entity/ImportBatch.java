package com.tradepilot.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "import_batches", indexes = {@Index(name = "idx_import_batches_user_id", columnList = "user_id"), @Index(name = "idx_import_batches_account_id", columnList = "account_id")})
public class ImportBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Nullable for dev mode
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private TradingAccount account;
    private String fileName;
    private Integer totalRows;
    private Integer importedRows;
    private Integer skippedDuplicates;
    private LocalDateTime importedAt;

    @PrePersist
    void prePersist() {
        if (importedAt == null) {
            importedAt = LocalDateTime.now();
        }
    }


    @java.lang.SuppressWarnings("all")
    public static class ImportBatchBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private User user;
        @java.lang.SuppressWarnings("all")
        private TradingAccount account;
        @java.lang.SuppressWarnings("all")
        private String fileName;
        @java.lang.SuppressWarnings("all")
        private Integer totalRows;
        @java.lang.SuppressWarnings("all")
        private Integer importedRows;
        @java.lang.SuppressWarnings("all")
        private Integer skippedDuplicates;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime importedAt;

        @java.lang.SuppressWarnings("all")
        ImportBatchBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ImportBatch.ImportBatchBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ImportBatch.ImportBatchBuilder user(final User user) {
            this.user = user;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ImportBatch.ImportBatchBuilder account(final TradingAccount account) {
            this.account = account;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ImportBatch.ImportBatchBuilder fileName(final String fileName) {
            this.fileName = fileName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ImportBatch.ImportBatchBuilder totalRows(final Integer totalRows) {
            this.totalRows = totalRows;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ImportBatch.ImportBatchBuilder importedRows(final Integer importedRows) {
            this.importedRows = importedRows;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ImportBatch.ImportBatchBuilder skippedDuplicates(final Integer skippedDuplicates) {
            this.skippedDuplicates = skippedDuplicates;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ImportBatch.ImportBatchBuilder importedAt(final LocalDateTime importedAt) {
            this.importedAt = importedAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public ImportBatch build() {
            return new ImportBatch(this.id, this.user, this.account, this.fileName, this.totalRows, this.importedRows, this.skippedDuplicates, this.importedAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "ImportBatch.ImportBatchBuilder(id=" + this.id + ", user=" + this.user + ", account=" + this.account + ", fileName=" + this.fileName + ", totalRows=" + this.totalRows + ", importedRows=" + this.importedRows + ", skippedDuplicates=" + this.skippedDuplicates + ", importedAt=" + this.importedAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static ImportBatch.ImportBatchBuilder builder() {
        return new ImportBatch.ImportBatchBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public User getUser() {
        return this.user;
    }

    @java.lang.SuppressWarnings("all")
    public TradingAccount getAccount() {
        return this.account;
    }

    @java.lang.SuppressWarnings("all")
    public String getFileName() {
        return this.fileName;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getTotalRows() {
        return this.totalRows;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getImportedRows() {
        return this.importedRows;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getSkippedDuplicates() {
        return this.skippedDuplicates;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getImportedAt() {
        return this.importedAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setUser(final User user) {
        this.user = user;
    }

    @java.lang.SuppressWarnings("all")
    public void setAccount(final TradingAccount account) {
        this.account = account;
    }

    @java.lang.SuppressWarnings("all")
    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalRows(final Integer totalRows) {
        this.totalRows = totalRows;
    }

    @java.lang.SuppressWarnings("all")
    public void setImportedRows(final Integer importedRows) {
        this.importedRows = importedRows;
    }

    @java.lang.SuppressWarnings("all")
    public void setSkippedDuplicates(final Integer skippedDuplicates) {
        this.skippedDuplicates = skippedDuplicates;
    }

    @java.lang.SuppressWarnings("all")
    public void setImportedAt(final LocalDateTime importedAt) {
        this.importedAt = importedAt;
    }

    @java.lang.SuppressWarnings("all")
    public ImportBatch() {
    }

    @java.lang.SuppressWarnings("all")
    public ImportBatch(final Long id, final User user, final TradingAccount account, final String fileName, final Integer totalRows, final Integer importedRows, final Integer skippedDuplicates, final LocalDateTime importedAt) {
        this.id = id;
        this.user = user;
        this.account = account;
        this.fileName = fileName;
        this.totalRows = totalRows;
        this.importedRows = importedRows;
        this.skippedDuplicates = skippedDuplicates;
        this.importedAt = importedAt;
    }
}
