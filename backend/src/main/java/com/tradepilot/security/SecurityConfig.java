package com.tradepilot.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Map;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final Environment environment;

    @Value("${app.dev-auth.enabled:false}")
    private boolean devAuthEnabled;

    @Bean
    @Order(1)
    public SecurityFilterChain stripeWebhookSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Registering SecurityFilterChain bean=stripeWebhookSecurityFilterChain order=1 matcher=/api/stripe/webhook");
        return http
                .securityMatcher("/api/stripe/webhook")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/stripe/webhook").permitAll()
                        .anyRequest().denyAll()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain appSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Registering SecurityFilterChain bean=appSecurityFilterChain order=2 matcher=application");
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {
                })
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, authException) ->
                        response.sendError(401, "Authentication required")
                ))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/auth/**").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/contact").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/stripe/webhook").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll();
                    auth.requestMatchers("/api/admin/**").hasRole("ADMIN");
                    if (h2ConsoleAllowed()) {
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }
                    if (devAuthEnabled) {
                        auth.requestMatchers(HttpMethod.GET, "/api/dev/token").permitAll();
                    }

                    auth.anyRequest().authenticated();
                })
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        if (h2ConsoleAllowed()) {
            http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
        }

        return http.build();
    }

    @Bean
    public ApplicationRunner securityFilterChainStartupLogger(Map<String, SecurityFilterChain> securityFilterChains) {
        return args -> securityFilterChains.forEach((name, chain) ->
                log.info("Active SecurityFilterChain bean={} instance={} expectedOrder={}",
                        name,
                        chain.getClass().getName(),
                        "stripeWebhookSecurityFilterChain".equals(name) ? 1 : "appSecurityFilterChain".equals(name) ? 2 : "unknown")
        );
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    private boolean h2ConsoleAllowed() {
        return environment.acceptsProfiles(Profiles.of("dev", "test"));
    }
}
