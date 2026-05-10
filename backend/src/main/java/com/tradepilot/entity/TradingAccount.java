package com.tradepilot.entity;

import com.tradepilot.enums.PropFirmType;
import com.tradepilot.enums.TradingAccountStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trading_accounts")
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

    private BigDecimal startingBalance;

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