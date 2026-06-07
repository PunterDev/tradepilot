package com.tradepilot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "journal_entries",
        indexes = {
                @Index(name = "idx_journal_entries_user_id", columnList = "user_id"),
                @Index(name = "idx_journal_entries_user_account_date", columnList = "user_id, account_id, journal_date")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_journal_user_account_date",
                        columnNames = {"user_id", "account_id", "journal_date"}
                )
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    @ManyToOne
    private TradingAccount account;

    @Column(name = "journal_date", nullable = false)
    private LocalDate journalDate;

    @Column(length = 50)
    private String mood;

    @Column(name = "setup_quality", length = 100)
    private String setupQuality;

    @Column(length = 1000)
    private String mistakes;

    @Column(name = "lesson_learned", length = 1000)
    private String lessonLearned;

    @Column(length = 2000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
