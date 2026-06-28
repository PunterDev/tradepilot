package com.tradepilot.entity;

import com.tradepilot.enums.PropFirmType;
import com.tradepilot.enums.TradingAccountStatus;
import com.tradepilot.enums.TradingAccountType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trading_accounts", indexes = {@Index(name = "idx_trading_accounts_user_id", columnList = "user_id")})
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


    @java.lang.SuppressWarnings("all")
    public static class TradingAccountBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private User user;
        @java.lang.SuppressWarnings("all")
        private String name;
        @java.lang.SuppressWarnings("all")
        private PropFirmType propFirm;
        @java.lang.SuppressWarnings("all")
        private String accountNumber;
        @java.lang.SuppressWarnings("all")
        private TradingAccountType accountType;
        @java.lang.SuppressWarnings("all")
        private BigDecimal startingBalance;
        @java.lang.SuppressWarnings("all")
        private BigDecimal defaultRiskAmount;
        @java.lang.SuppressWarnings("all")
        private TradingAccountStatus status;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;

        @java.lang.SuppressWarnings("all")
        TradingAccountBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingAccount.TradingAccountBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingAccount.TradingAccountBuilder user(final User user) {
            this.user = user;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingAccount.TradingAccountBuilder name(final String name) {
            this.name = name;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingAccount.TradingAccountBuilder propFirm(final PropFirmType propFirm) {
            this.propFirm = propFirm;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingAccount.TradingAccountBuilder accountNumber(final String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingAccount.TradingAccountBuilder accountType(final TradingAccountType accountType) {
            this.accountType = accountType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingAccount.TradingAccountBuilder startingBalance(final BigDecimal startingBalance) {
            this.startingBalance = startingBalance;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingAccount.TradingAccountBuilder defaultRiskAmount(final BigDecimal defaultRiskAmount) {
            this.defaultRiskAmount = defaultRiskAmount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingAccount.TradingAccountBuilder status(final TradingAccountStatus status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingAccount.TradingAccountBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public TradingAccount build() {
            return new TradingAccount(this.id, this.user, this.name, this.propFirm, this.accountNumber, this.accountType, this.startingBalance, this.defaultRiskAmount, this.status, this.createdAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "TradingAccount.TradingAccountBuilder(id=" + this.id + ", user=" + this.user + ", name=" + this.name + ", propFirm=" + this.propFirm + ", accountNumber=" + this.accountNumber + ", accountType=" + this.accountType + ", startingBalance=" + this.startingBalance + ", defaultRiskAmount=" + this.defaultRiskAmount + ", status=" + this.status + ", createdAt=" + this.createdAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static TradingAccount.TradingAccountBuilder builder() {
        return new TradingAccount.TradingAccountBuilder();
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
    public String getName() {
        return this.name;
    }

    @java.lang.SuppressWarnings("all")
    public PropFirmType getPropFirm() {
        return this.propFirm;
    }

    @java.lang.SuppressWarnings("all")
    public String getAccountNumber() {
        return this.accountNumber;
    }

    @java.lang.SuppressWarnings("all")
    public TradingAccountType getAccountType() {
        return this.accountType;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getStartingBalance() {
        return this.startingBalance;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getDefaultRiskAmount() {
        return this.defaultRiskAmount;
    }

    @java.lang.SuppressWarnings("all")
    public TradingAccountStatus getStatus() {
        return this.status;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
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
    public void setName(final String name) {
        this.name = name;
    }

    @java.lang.SuppressWarnings("all")
    public void setPropFirm(final PropFirmType propFirm) {
        this.propFirm = propFirm;
    }

    @java.lang.SuppressWarnings("all")
    public void setAccountNumber(final String accountNumber) {
        this.accountNumber = accountNumber;
    }

    @java.lang.SuppressWarnings("all")
    public void setAccountType(final TradingAccountType accountType) {
        this.accountType = accountType;
    }

    @java.lang.SuppressWarnings("all")
    public void setStartingBalance(final BigDecimal startingBalance) {
        this.startingBalance = startingBalance;
    }

    @java.lang.SuppressWarnings("all")
    public void setDefaultRiskAmount(final BigDecimal defaultRiskAmount) {
        this.defaultRiskAmount = defaultRiskAmount;
    }

    @java.lang.SuppressWarnings("all")
    public void setStatus(final TradingAccountStatus status) {
        this.status = status;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public TradingAccount() {
    }

    @java.lang.SuppressWarnings("all")
    public TradingAccount(final Long id, final User user, final String name, final PropFirmType propFirm, final String accountNumber, final TradingAccountType accountType, final BigDecimal startingBalance, final BigDecimal defaultRiskAmount, final TradingAccountStatus status, final LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.name = name;
        this.propFirm = propFirm;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.startingBalance = startingBalance;
        this.defaultRiskAmount = defaultRiskAmount;
        this.status = status;
        this.createdAt = createdAt;
    }
}
