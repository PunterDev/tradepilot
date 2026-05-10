package com.tradepilot.entity;

import com.tradepilot.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Trade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false, length = 20)
    private String symbol;
    @Column(nullable = false, length = 20)
    private String market;
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    @Column(name = "entry_time")
    private LocalDateTime entryTime;
    @Column(name = "exit_time")
    private LocalDateTime exitTime;
    @Column(nullable = false, length = 20)
    private String side;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 50)
    private SetupType setup;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private SessionType session;
    @Column(name = "entry_price", precision = 18, scale = 6)
    private BigDecimal entryPrice;
    @Column(name = "stop_price", precision = 18, scale = 6)
    private BigDecimal stopPrice;
    @Column(name = "target_price", precision = 18, scale = 6)
    private BigDecimal targetPrice;
    @Column(name = "exit_price", precision = 18, scale = 6)
    private BigDecimal exitPrice;
    private Integer quantity;
    @Column(name = "risk_amount", precision = 18, scale = 2)
    private BigDecimal riskAmount;
    @Column(name = "pnl_amount", precision = 18, scale = 2)
    private BigDecimal pnlAmount;
    @Column(name = "pnl_r", precision = 10, scale = 2)
    private BigDecimal pnlR;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private OutcomeType outcome;
    @Column(name = "confidence_rating")
    private Integer confidenceRating;
    @Column(name = "execution_rating")
    private Integer executionRating;
    @Column(name = "followed_plan")
    private Boolean followedPlan;
    @Enumerated(EnumType.STRING) @Column(name = "mistake_tag", length = 50)
    private MistakeTag mistakeTag;
    @Column(columnDefinition = "text")
    private String notes;
    @Column(name = "psychology_notes", columnDefinition = "text")
    private String psychologyNotes;
    @Column(name = "lesson_learned", columnDefinition = "text")
    private String lessonLearned;
    @Column(name = "screenshot_url", length = 500)
    private String screenshotUrl;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @PrePersist public void onCreate(){ createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate public void onUpdate(){ updatedAt = LocalDateTime.now(); }
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private TradingAccount account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_batch_id")
    private ImportBatch importBatch;

    @Column(name = "external_trade_key", unique = true)
    private String externalTradeKey;
}
