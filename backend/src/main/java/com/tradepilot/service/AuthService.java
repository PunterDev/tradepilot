package com.tradepilot.service;

import com.tradepilot.dto.auth.*;
import com.tradepilot.billing.BillingUsageResponse;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.UsageLimitService;
import com.tradepilot.entity.AuthToken;
import com.tradepilot.entity.User;
import com.tradepilot.enums.AuthTokenType;
import com.tradepilot.enums.UserRole;
import com.tradepilot.exception.EmailNotVerifiedException;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageAction;
import com.tradepilot.featureusage.FeatureUsageEventType;
import com.tradepilot.featureusage.FeatureUsageService;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.security.JwtService;
import com.tradepilot.seoanalytics.SeoAnalyticsService;
import com.tradepilot.util.LogSanitizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.Locale;

@Service
public class AuthService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);
    private static final String VERIFY_MESSAGE = "Account created. Please check your email to verify your account.";
    private static final String RESEND_MESSAGE = "If an account exists, a verification link has been sent.";
    private static final String FORGOT_MESSAGE = "If an account exists, a reset link has been sent.";
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuthTokenService authTokenService;
    private final EmailAsyncService emailAsyncService;
    private final UsageLimitService usageLimitService;
    private final FeatureUsageService featureUsageService;
    private final SeoAnalyticsService seoAnalyticsService;
    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public MessageResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        log.info("Registration started. email={}", LogSanitizer.maskEmail(email));
        if (userRepository.existsByEmail(email)) {
            log.warn("Registration rejected because email already exists. email={}", LogSanitizer.maskEmail(email));
            throw new IllegalArgumentException("Email is already registered");
        }
        User user = User.builder().name(request.getName().trim()).email(email).passwordHash(passwordEncoder.encode(request.getPassword())).emailVerified(false).build();
        User saved = userRepository.save(user);
        featureUsageService.track(saved, FeatureKey.ACTIVATION, FeatureUsageAction.USER_SIGNED_UP, FeatureUsageEventType.USER_ACTION, null, "USER", saved.getId().toString(), "auth", null);
        seoAnalyticsService.recordSignup(saved, request.getSeoAttribution());
        sendVerification(saved);
        log.info("Registration completed. userId={}", saved.getId());
        return new MessageResponse(VERIFY_MESSAGE);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        log.info("Login started. email={}", LogSanitizer.maskEmail(email));
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        } catch (BadCredentialsException ex) {
            log.warn("Login failed due to invalid credentials. email={}", LogSanitizer.maskEmail(email));
            throw ex;
        }
        User user = userRepository.findByEmail(email).orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            log.warn("Login blocked pending email verification. userId={}", user.getId());
            throw new EmailNotVerifiedException("Please verify your email before logging in.");
        }
        BillingUsageResponse usage = usageLimitService.getUsage(user);
        AuthResponse response = AuthResponse.of(jwtService.generateToken(user), user.getId(), user.getName(), user.getEmail(), planName(user), roleName(user), usage.getMonthlyAiCredits(), usage.getMonthlyAiCreditsUsed(), usage.getMonthlyAiCreditsRemaining(), usage.getPurchasedAiCreditsBalance(), usage.getTotalAiCreditsAvailable());
        log.info("Login successful. userId={}, role={}, plan={}", user.getId(), response.getRole(), response.getPlan());
        return response;
    }

    public UserMeResponse me(User user) {
        log.trace("Auth me requested. userId={}", user == null ? null : user.getId());
        BillingUsageResponse usage = usageLimitService.getUsage(user);
        return UserMeResponse.of(user.getId(), user.getName(), user.getEmail(), planName(user), roleName(user), usage.getMonthlyAiCredits(), usage.getMonthlyAiCreditsUsed(), usage.getMonthlyAiCreditsRemaining(), usage.getPurchasedAiCreditsBalance(), usage.getTotalAiCreditsAvailable());
    }

    @Transactional
    public MessageResponse verifyEmail(TokenRequest request) {
        AuthToken token = authTokenService.getActiveToken(request.token(), AuthTokenType.EMAIL_VERIFICATION);
        User user = token.getUser();
        log.info("Email verification started. userId={}", user.getId());
        user.setEmailVerified(true);
        userRepository.save(user);
        authTokenService.markUsed(token);
        authTokenService.invalidateActiveTokens(user, AuthTokenType.EMAIL_VERIFICATION);
        log.info("Email verification completed. userId={}", user.getId());
        return new MessageResponse("Email verified. You can now log in.");
    }

    public MessageResponse resendVerification(EmailRequest request) {
        String email = normalizeEmail(request.email());
        log.info("Verification resend requested. email={}", LogSanitizer.maskEmail(email));
        userRepository.findByEmail(email).filter(user -> !Boolean.TRUE.equals(user.getEmailVerified())).ifPresent(this::sendVerification);
        return new MessageResponse(RESEND_MESSAGE);
    }

    public MessageResponse forgotPassword(EmailRequest request) {
        String email = normalizeEmail(request.email());
        log.info("Password reset requested. email={}", LogSanitizer.maskEmail(email));
        userRepository.findByEmail(email).ifPresent(user -> {
            String rawToken = authTokenService.createToken(user, AuthTokenType.PASSWORD_RESET, Duration.ofMinutes(30));
            emailAsyncService.sendPasswordResetEmail(user.getId(), user.getEmail(), buildLink("/reset-password", rawToken));
            log.info("Password reset email queued. userId={}", user.getId());
        });
        return new MessageResponse(FORGOT_MESSAGE);
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        AuthToken token = authTokenService.getActiveToken(request.token(), AuthTokenType.PASSWORD_RESET);
        User user = token.getUser();
        log.info("Password reset started. userId={}", user.getId());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setTokenVersion(nextTokenVersion(user));
        userRepository.save(user);
        authTokenService.markUsed(token);
        authTokenService.invalidateActiveTokens(user, AuthTokenType.PASSWORD_RESET);
        log.info("Password reset completed. userId={}", user.getId());
        return new MessageResponse("Password reset. You can now log in.");
    }

    private void sendVerification(User user) {
        String rawToken = authTokenService.createToken(user, AuthTokenType.EMAIL_VERIFICATION, Duration.ofHours(24));
        emailAsyncService.sendVerificationEmail(user.getId(), user.getEmail(), buildLink("/verify-email", rawToken));
        log.info("Verification email queued. userId={}", user.getId());
    }

    private String buildLink(String path, String rawToken) {
        String normalizedFrontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        return normalizedFrontendUrl + path + "?token=" + rawToken;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String planName(User user) {
        return (user.getPlan() == null ? PlanType.FREE : user.getPlan()).name();
    }

    private String roleName(User user) {
        return (user.getRole() == null ? UserRole.USER : user.getRole()).name();
    }

    private int nextTokenVersion(User user) {
        Integer tokenVersion = user.getTokenVersion();
        return tokenVersion == null ? 1 : tokenVersion + 1;
    }

    @java.lang.SuppressWarnings("all")
    public AuthService(final UserRepository userRepository, final PasswordEncoder passwordEncoder, final JwtService jwtService, final AuthenticationManager authenticationManager, final AuthTokenService authTokenService, final EmailAsyncService emailAsyncService, final UsageLimitService usageLimitService, final FeatureUsageService featureUsageService, final SeoAnalyticsService seoAnalyticsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.authTokenService = authTokenService;
        this.emailAsyncService = emailAsyncService;
        this.usageLimitService = usageLimitService;
        this.featureUsageService = featureUsageService;
        this.seoAnalyticsService = seoAnalyticsService;
    }
}
