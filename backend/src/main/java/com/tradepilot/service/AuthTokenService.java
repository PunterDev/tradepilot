package com.tradepilot.service;

import com.tradepilot.entity.AuthToken;
import com.tradepilot.entity.User;
import com.tradepilot.enums.AuthTokenType;
import com.tradepilot.repository.AuthTokenRepository;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class AuthTokenService {
    private static final int TOKEN_BYTES = 32;
    private final SecureRandom secureRandom = new SecureRandom();
    private final AuthTokenRepository authTokenRepository;

    public String createToken(User user, AuthTokenType type, Duration ttl) {
        invalidateActiveTokens(user, type);
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        authTokenRepository.save(AuthToken.builder().user(user).type(type).tokenHash(hashToken(rawToken)).expiresAt(Instant.now().plus(ttl)).build());
        return rawToken;
    }

    public AuthToken getActiveToken(String rawToken, AuthTokenType type) {
        return authTokenRepository.findFirstByTokenHashAndTypeAndUsedAtIsNullAndExpiresAtAfter(hashToken(rawToken), type, Instant.now()).orElseThrow(() -> new IllegalArgumentException("Invalid or expired token."));
    }

    public void markUsed(AuthToken token) {
        token.setUsedAt(Instant.now());
        authTokenRepository.save(token);
    }

    public void invalidateActiveTokens(User user, AuthTokenType type) {
        List<AuthToken> tokens = authTokenRepository.findByUserAndTypeAndUsedAtIsNull(user, type);
        Instant now = Instant.now();
        for (AuthToken token : tokens) {
            token.setUsedAt(now);
        }
        authTokenRepository.saveAll(tokens);
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash auth token", e);
        }
    }

    @java.lang.SuppressWarnings("all")
    public AuthTokenService(final AuthTokenRepository authTokenRepository) {
        this.authTokenRepository = authTokenRepository;
    }
}
