package com.tradepilot.entity;

import com.tradepilot.enums.AuthTokenType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "auth_tokens",
        indexes = {
                @Index(name = "idx_auth_tokens_user_id", columnList = "user_id"),
                @Index(name = "idx_auth_tokens_token_hash", columnList = "token_hash"),
                @Index(name = "idx_auth_tokens_type", columnList = "type"),
                @Index(name = "idx_auth_tokens_expires_at", columnList = "expires_at"),
                @Index(name = "idx_auth_tokens_lookup", columnList = "token_hash, type, expires_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
