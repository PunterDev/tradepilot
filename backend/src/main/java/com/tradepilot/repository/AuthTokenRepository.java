package com.tradepilot.repository;

import com.tradepilot.entity.AuthToken;
import com.tradepilot.entity.User;
import com.tradepilot.enums.AuthTokenType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
    Optional<AuthToken> findFirstByTokenHashAndTypeAndUsedAtIsNullAndExpiresAtAfter(
            String tokenHash,
            AuthTokenType type,
            Instant now
    );

    List<AuthToken> findByUserAndTypeAndUsedAtIsNull(User user, AuthTokenType type);
}
