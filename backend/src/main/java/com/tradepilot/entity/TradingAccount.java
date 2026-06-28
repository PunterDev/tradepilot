package com.tradepilot.entity;

import com.tradepilot.enums.PropFirmType;
import com.tradepilot.enums.TradingAccountStatus;
import com.tradepilot.enums.TradingAccountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "trading_accounts",
        indexes = {
                @Index(name = "idx_trading_accounts_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradingAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nullable for dev mode while security is disabled
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropFirmType propFirm;

    private String accountNumber;

    @Enumerated(EnumType.STRING)
    private TradingAccountType accountType;

    private BigDecimal startingBalance;

    @Column(name = "default_risk_amount", precision = 18, scale = 2)
    private BigDecimal defaultRiskAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradingAccountStatus status;

    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = TradingAccountStatus.ACTIVE;
        }
    }
}
