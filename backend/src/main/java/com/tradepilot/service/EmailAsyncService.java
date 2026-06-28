package com.tradepilot.service;

import com.tradepilot.entity.ContactMessage;
import com.tradepilot.repository.ContactMessageRepository;
import com.tradepilot.util.LogSanitizer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailAsyncService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmailAsyncService.class);
    private final EmailService emailService;
    private final ContactMessageRepository contactMessageRepository;

    @Async("emailTaskExecutor")
    public void sendVerificationEmail(Long userId, String email, String link) {
        try {
            emailService.sendVerificationEmail(email, link);
            log.info("Verification email processed. userId={}", userId);
        } catch (Exception ex) {
            log.warn("Verification email async processing failed. userId={}, recipient={}, error={}", userId, LogSanitizer.maskEmail(email), LogSanitizer.summarizeError(ex.getMessage()));
        }
    }

    @Async("emailTaskExecutor")
    public void sendPasswordResetEmail(Long userId, String email, String link) {
        try {
            emailService.sendPasswordResetEmail(email, link);
            log.info("Password reset email processed. userId={}", userId);
        } catch (Exception ex) {
            log.warn("Password reset email async processing failed. userId={}, recipient={}, error={}", userId, LogSanitizer.maskEmail(email), LogSanitizer.summarizeError(ex.getMessage()));
        }
    }

    @Async("emailTaskExecutor")
    @Transactional
    public void sendContactEmail(Long contactMessageId, String to, String name, String fromEmail, String subject, String message) {
        EmailService.EmailSendResult result;
        try {
            result = emailService.sendContactEmail(to, name, fromEmail, subject, message);
        } catch (Exception ex) {
            result = new EmailService.EmailSendResult(false, LogSanitizer.summarizeError(ex.getMessage()));
        }
        ContactMessage contactMessage = contactMessageRepository.findById(contactMessageId).orElse(null);
        if (contactMessage == null) {
            log.warn("Contact email result could not be recorded because message is missing. contactMessageId={}", contactMessageId);
            return;
        }
        contactMessage.setEmailSent(result.sent());
        contactMessage.setEmailError(result.sent() ? null : truncate(LogSanitizer.summarizeError(result.error()), 1000));
        contactMessageRepository.save(contactMessage);
        if (result.sent()) {
            log.info("Contact form email processed. contactMessageId={}, emailSent=true", contactMessageId);
        } else {
            log.warn("Contact form saved but async email delivery failed. contactMessageId={}, recipient={}, error={}", contactMessageId, LogSanitizer.maskEmail(to), LogSanitizer.summarizeError(result.error()));
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    @java.lang.SuppressWarnings("all")
    public EmailAsyncService(final EmailService emailService, final ContactMessageRepository contactMessageRepository) {
        this.emailService = emailService;
        this.contactMessageRepository = contactMessageRepository;
    }
}
