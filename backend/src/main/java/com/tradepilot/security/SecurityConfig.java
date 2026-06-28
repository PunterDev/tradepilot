package com.tradepilot.security;

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
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.util.Map;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SecurityConfig.class);
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final Environment environment;
    @Value("${app.dev-auth.enabled:false}")
    private boolean devAuthEnabled;

    @Bean
    @Order(1)
    public SecurityFilterChain stripeWebhookSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Registering SecurityFilterChain bean=stripeWebhookSecurityFilterChain order=1 matcher=/api/stripe/webhook");
        return http.securityMatcher("/api/stripe/webhook").csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.POST, "/api/stripe/webhook").permitAll().anyRequest().denyAll()).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain appSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Registering SecurityFilterChain bean=appSecurityFilterChain order=2 matcher=application");
        http.csrf(csrf -> csrf.disable()).cors(cors -> {
        }).headers(headers -> {
            headers.contentTypeOptions(Customizer.withDefaults());
            headers.referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
            if (h2ConsoleAllowed()) {
                headers.frameOptions(frame -> frame.sameOrigin());
            } else {
                headers.frameOptions(frame -> frame.deny());
                headers.contentSecurityPolicy(csp -> csp.policyDirectives("frame-ancestors \'none\'"));
            }
        }).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, authException) -> response.sendError(401, "Authentication required"))).authorizeHttpRequests(auth -> {
            auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
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
        }).authenticationProvider(authenticationProvider()).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public ApplicationRunner securityFilterChainStartupLogger(Map<String, SecurityFilterChain> securityFilterChains) {
        return args -> securityFilterChains.forEach((name, chain) -> log.info("Active SecurityFilterChain bean={} instance={} expectedOrder={}", name, chain.getClass().getName(), "stripeWebhookSecurityFilterChain".equals(name) ? 1 : "appSecurityFilterChain".equals(name) ? 2 : "unknown"));
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

    @java.lang.SuppressWarnings("all")
    public SecurityConfig(final JwtAuthenticationFilter jwtAuthenticationFilter, final CustomUserDetailsService userDetailsService, final Environment environment) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.environment = environment;
    }
}
