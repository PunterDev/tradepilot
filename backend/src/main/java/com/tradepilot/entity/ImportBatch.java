package com.tradepilot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "import_batches",
        indexes = {
                @Index(name = "idx_import_batches_user_id", columnList = "user_id"),
                @Index(name = "idx_import_batches_account_id", columnList = "account_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}
