package com.tradepilot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "trade_rule_violations",
        indexes = {
                @Index(name = "idx_trade_rule_violations_user_id", columnList = "user_id"),
                @Index(name = "idx_trade_rule_violations_trade_id", columnList = "trade_id"),
                @Index(name = "idx_trade_rule_violations_user_trade", columnList = "user_id, trade_id")
        }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TradeRuleViolation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long tradeId;

    @Column(nullable = false, length = 80)
    private String ruleCode;

    @Column(nullable = false)
    private String ruleTitle;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(nullable = false, length = 30)
    private String source;

    private LocalDate tradeDate;
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
