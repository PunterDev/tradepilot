package com.tradepilot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import io.jsonwebtoken.JwtException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String jwt = authHeader.substring(7);
        try {
            String email = jwtService.extractUsername(jwt);
            if (StringUtils.hasText(email) && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(token);
                }
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
            log.trace("JWT authentication failed. path={}, reason={}", request.getRequestURI(), ex.getClass().getSimpleName());
            securityErrorResponseWriter.write(response, HttpStatus.UNAUTHORIZED, "INVALID_OR_EXPIRED_TOKEN", "Invalid or expired token", request.getRequestURI());
            return;
        }
        filterChain.doFilter(request, response);
    }

    @java.lang.SuppressWarnings("all")
    public JwtAuthenticationFilter(final JwtService jwtService, final CustomUserDetailsService userDetailsService, final SecurityErrorResponseWriter securityErrorResponseWriter) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.securityErrorResponseWriter = securityErrorResponseWriter;
    }
}
