package com.tradepilot.service;

import com.tradepilot.entity.User;
import com.tradepilot.enums.UserRole;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.util.LogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapService {
    private final UserRepository userRepository;

    @Value("${app.admin-email:}")
    private String adminEmail;

    @Value("${app.admin-bootstrap.enabled:false}")
    private boolean adminBootstrapEnabled;

    @Bean
    ApplicationRunner adminBootstrapRunner() {
        return args -> {
            if (!StringUtils.hasText(adminEmail)) {
                log.info("ADMIN BOOTSTRAP: no APP_ADMIN_EMAIL configured");
                return;
            }
            if (!adminBootstrapEnabled) {
                log.warn("ADMIN BOOTSTRAP: APP_ADMIN_EMAIL is configured but APP_ADMIN_BOOTSTRAP_ENABLED is not true; no user was promoted.");
                return;
            }

            String normalized = adminEmail.trim().toLowerCase(Locale.ROOT);
            String maskedEmail = LogSanitizer.maskEmail(normalized);
            userRepository.findByEmail(normalized).ifPresentOrElse(user -> {
                if (user.getRole() != UserRole.ADMIN) {
                    user.setRole(UserRole.ADMIN);
                    userRepository.save(user);
                    log.info("ADMIN BOOTSTRAP: promoted {}", maskedEmail);
                } else {
                    log.info("ADMIN BOOTSTRAP: {} already ADMIN", maskedEmail);
                }
            }, () -> log.info("ADMIN BOOTSTRAP: {} not found yet", maskedEmail));
        };
    }
}
