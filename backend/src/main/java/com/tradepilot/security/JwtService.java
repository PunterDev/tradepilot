package com.tradepilot.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    @Value("${app.jwt.secret}") private String jwtSecret;
    @Value("${app.jwt.expiration-ms}") private long jwtExpirationMs;

    public String extractUsername(String token){ return extractClaim(token, Claims::getSubject); }
    public <T> T extractClaim(String token, Function<Claims,T> resolver){ return resolver.apply(extractAllClaims(token)); }
    public String generateToken(UserDetails userDetails){ return generateToken(new HashMap<>(), userDetails); }
    public String generateToken(Map<String,Object> extraClaims, UserDetails userDetails){
        Date now = new Date(); Date expiry = new Date(now.getTime()+jwtExpirationMs);
        return Jwts.builder().setClaims(extraClaims).setSubject(userDetails.getUsername()).setIssuedAt(now).setExpiration(expiry).signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
    }
    public boolean isTokenValid(String token, UserDetails userDetails){ return extractUsername(token).equals(userDetails.getUsername()) && !isTokenExpired(token); }
    private boolean isTokenExpired(String token){ return extractClaim(token, Claims::getExpiration).before(new Date()); }
    private Claims extractAllClaims(String token){ return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody(); }
    private Key getSigningKey(){ return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)); }
}
