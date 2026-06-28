package com.tradepilot.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    private static final int MIN_HMAC_SECRET_BYTES = 32;

    private final Key signingKey;
    private final long jwtExpirationMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiration-minutes:30}") long jwtExpirationMinutes
    ) {
        if (jwtExpirationMinutes <= 0) {
            throw new IllegalStateException("JWT expiration must be positive. Configure JWT_EXPIRATION_MINUTES.");
        }
        this.signingKey = Keys.hmacShaKeyFor(decodeAndValidateSecret(jwtSecret));
        this.jwtExpirationMinutes = jwtExpirationMinutes;
    }

    public String extractUsername(String token){ return extractClaim(token, Claims::getSubject); }
    public <T> T extractClaim(String token, Function<Claims,T> resolver){ return resolver.apply(extractAllClaims(token)); }
    public String generateToken(UserDetails userDetails){ return generateToken(new HashMap<>(), userDetails); }
    public String generateToken(Map<String,Object> extraClaims, UserDetails userDetails){
        Date now = new Date(); Date expiry = new Date(now.getTime() + jwtExpirationMinutes * 60_000L);
        return Jwts.builder().setClaims(extraClaims).setSubject(userDetails.getUsername()).setIssuedAt(now).setExpiration(expiry).signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
    }
    public boolean isTokenValid(String token, UserDetails userDetails){ return extractUsername(token).equals(userDetails.getUsername()) && !isTokenExpired(token); }
    private boolean isTokenExpired(String token){ return extractClaim(token, Claims::getExpiration).before(new Date()); }
    private Claims extractAllClaims(String token){ return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody(); }
    private Key getSigningKey(){ return signingKey; }

    private byte[] decodeAndValidateSecret(String jwtSecret) {
        if (!StringUtils.hasText(jwtSecret)) {
            throw new IllegalStateException("JWT secret is missing. Configure JWT_SECRET with at least 32 random bytes encoded as Base64.");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(jwtSecret);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("JWT secret must be Base64 encoded. Configure JWT_SECRET with at least 32 random bytes encoded as Base64.", ex);
        }
        if (decoded.length < MIN_HMAC_SECRET_BYTES) {
            throw new IllegalStateException("JWT secret is too weak. Configure JWT_SECRET with at least 32 random bytes encoded as Base64.");
        }
        return decoded;
    }
}
