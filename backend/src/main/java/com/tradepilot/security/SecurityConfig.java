package com.tradepilot.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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
import org.springframework.security.web.header.writers.StaticHeadersWriter;
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
    private final SecurityErrorResponseWriter securityErrorResponseWriter;
    @Value("${app.dev-auth.enabled:false}")
    private boolean devAuthEnabled;

    @Bean
    @Order(1)
    public SecurityFilterChain stripeWebhookSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Registering SecurityFilterChain bean=stripeWebhookSecurityFilterChain order=1 matcher=/api/stripe/webhook");
        return http.securityMatcher("/api/stripe/webhook", "/api/v1/stripe/webhook").csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.POST, "/api/stripe/webhook", "/api/v1/stripe/webhook").permitAll().anyRequest().denyAll()).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain appSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Registering SecurityFilterChain bean=appSecurityFilterChain order=2 matcher=application");
        http.csrf(csrf -> csrf.disable()).cors(cors -> {
        }).headers(headers -> {
            headers.contentTypeOptions(Customizer.withDefaults());
            headers.referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
            headers.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000));
            headers.addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()"));
            if (h2ConsoleAllowed()) {
                headers.frameOptions(frame -> frame.sameOrigin());
            } else {
                headers.frameOptions(frame -> frame.deny());
                headers.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'"));
            }
        }).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, authException) -> securityErrorResponseWriter.write(response, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication required", request.getRequestURI())).accessDeniedHandler((request, response, accessDeniedException) -> securityErrorResponseWriter.write(response, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied", request.getRequestURI()))).authorizeHttpRequests(auth -> {
            auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
            auth.requestMatchers(HttpMethod.GET, "/api/auth/me", "/api/v1/auth/me").authenticated();
            auth.requestMatchers(HttpMethod.POST,
                    "/api/auth/register", "/api/v1/auth/register",
                    "/api/auth/login", "/api/v1/auth/login",
                    "/api/auth/verify-email", "/api/v1/auth/verify-email",
                    "/api/auth/resend-verification", "/api/v1/auth/resend-verification",
                    "/api/auth/forgot-password", "/api/v1/auth/forgot-password",
                    "/api/auth/reset-password", "/api/v1/auth/reset-password"
            ).permitAll();
            auth.requestMatchers(HttpMethod.GET, "/api/features", "/api/v1/features").permitAll();
            auth.requestMatchers(HttpMethod.POST, "/api/contact", "/api/v1/contact").permitAll();
            auth.requestMatchers(HttpMethod.POST, "/api/seo/events", "/api/v1/seo/events").permitAll();
            auth.requestMatchers(HttpMethod.POST, "/api/stripe/webhook", "/api/v1/stripe/webhook").permitAll();
            auth.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll();
            auth.requestMatchers("/api/admin/**", "/api/v1/admin/**").hasRole("ADMIN");
            if (h2ConsoleAllowed()) {
                auth.requestMatchers("/h2-console/**").permitAll();
            }
            if (devAuthEnabled) {
                auth.requestMatchers(HttpMethod.GET, "/api/dev/token", "/api/v1/dev/token").permitAll();
                auth.requestMatchers(HttpMethod.POST, "/api/dev/e2e/prepare", "/api/v1/dev/e2e/prepare").permitAll();
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
    public SecurityConfig(final JwtAuthenticationFilter jwtAuthenticationFilter, final CustomUserDetailsService userDetailsService, final Environment environment, final SecurityErrorResponseWriter securityErrorResponseWriter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.environment = environment;
        this.securityErrorResponseWriter = securityErrorResponseWriter;
    }
}
