package com.tradepilot.security;

import com.tradepilot.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private static final int MIN_HMAC_SECRET_BYTES = 32;

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        byte[] decodedSecret = decodeAndValidateSecret(secret);
        if (expirationMs <= 0) {
            throw new IllegalStateException("JWT expiration must be positive. Configure JWT_EXPIRATION_MS.");
        }
        this.secretKey = Keys.hmacShaKeyFor(decodedSecret);
        this.expirationMs = expirationMs;
    }

    private byte[] decodeAndValidateSecret(String secret) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT secret is missing. Configure JWT_SECRET with at least 32 random bytes encoded as Base64.");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("JWT secret must be Base64 encoded. Configure JWT_SECRET with at least 32 random bytes encoded as Base64.", ex);
        }
        if (decoded.length < MIN_HMAC_SECRET_BYTES) {
            throw new IllegalStateException("JWT secret is too weak. Configure JWT_SECRET with at least 32 random bytes encoded as Base64.");
        }
        return decoded;
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
