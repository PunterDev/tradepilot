package com.tradepilot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_batches")
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