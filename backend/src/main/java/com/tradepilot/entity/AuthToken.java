package com.tradepilot.entity;

import com.tradepilot.enums.AuthTokenType;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "auth_tokens", indexes = {@Index(name = "idx_auth_tokens_user_id", columnList = "user_id"), @Index(name = "idx_auth_tokens_token_hash", columnList = "token_hash"), @Index(name = "idx_auth_tokens_type", columnList = "type"), @Index(name = "idx_auth_tokens_expires_at", columnList = "expires_at"), @Index(name = "idx_auth_tokens_lookup", columnList = "token_hash, type, expires_at")})
public class AuthToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuthTokenType type;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "used_at")
    private Instant usedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }


    @java.lang.SuppressWarnings("all")
    public static class AuthTokenBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private User user;
        @java.lang.SuppressWarnings("all")
        private String tokenHash;
        @java.lang.SuppressWarnings("all")
        private AuthTokenType type;
        @java.lang.SuppressWarnings("all")
        private Instant expiresAt;
        @java.lang.SuppressWarnings("all")
        private Instant usedAt;
        @java.lang.SuppressWarnings("all")
        private Instant createdAt;

        @java.lang.SuppressWarnings("all")
        AuthTokenBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AuthToken.AuthTokenBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AuthToken.AuthTokenBuilder user(final User user) {
            this.user = user;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AuthToken.AuthTokenBuilder tokenHash(final String tokenHash) {
            this.tokenHash = tokenHash;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AuthToken.AuthTokenBuilder type(final AuthTokenType type) {
            this.type = type;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AuthToken.AuthTokenBuilder expiresAt(final Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AuthToken.AuthTokenBuilder usedAt(final Instant usedAt) {
            this.usedAt = usedAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AuthToken.AuthTokenBuilder createdAt(final Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AuthToken build() {
            return new AuthToken(this.id, this.user, this.tokenHash, this.type, this.expiresAt, this.usedAt, this.createdAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AuthToken.AuthTokenBuilder(id=" + this.id + ", user=" + this.user + ", tokenHash=" + this.tokenHash + ", type=" + this.type + ", expiresAt=" + this.expiresAt + ", usedAt=" + this.usedAt + ", createdAt=" + this.createdAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AuthToken.AuthTokenBuilder builder() {
        return new AuthToken.AuthTokenBuilder();
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
    public String getTokenHash() {
        return this.tokenHash;
    }

    @java.lang.SuppressWarnings("all")
    public AuthTokenType getType() {
        return this.type;
    }

    @java.lang.SuppressWarnings("all")
    public Instant getExpiresAt() {
        return this.expiresAt;
    }

    @java.lang.SuppressWarnings("all")
    public Instant getUsedAt() {
        return this.usedAt;
    }

    @java.lang.SuppressWarnings("all")
    public Instant getCreatedAt() {
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
    public void setTokenHash(final String tokenHash) {
        this.tokenHash = tokenHash;
    }

    @java.lang.SuppressWarnings("all")
    public void setType(final AuthTokenType type) {
        this.type = type;
    }

    @java.lang.SuppressWarnings("all")
    public void setExpiresAt(final Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setUsedAt(final Instant usedAt) {
        this.usedAt = usedAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public AuthToken() {
    }

    @java.lang.SuppressWarnings("all")
    public AuthToken(final Long id, final User user, final String tokenHash, final AuthTokenType type, final Instant expiresAt, final Instant usedAt, final Instant createdAt) {
        this.id = id;
        this.user = user;
        this.tokenHash = tokenHash;
        this.type = type;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.createdAt = createdAt;
    }
}
