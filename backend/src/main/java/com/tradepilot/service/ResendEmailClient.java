package com.tradepilot.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import com.tradepilot.util.LogSanitizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ResendEmailClient {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ResendEmailClient.class);
    @Value("${app.email.resend.api-key:}")
    private String resendApiKey;
    @Value("${app.email.from:}")
    private String emailFrom;

    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        if (isConfigured()) {
            log.info("EMAIL MODE: RESEND_ENABLED");
            return;
        }
        log.warn("EMAIL MODE: RESEND_NOT_CONFIGURED. Configure RESEND_API_KEY and EMAIL_FROM for production email delivery.");
    }

    public boolean isConfigured() {
        return StringUtils.hasText(resendApiKey) && StringUtils.hasText(emailFrom);
    }

    public String sendHtml(String to, String subject, String htmlBody) throws ResendException {
        return sendHtml(to, subject, htmlBody, null);
    }

    public String sendHtml(String to, String subject, String htmlBody, String replyTo) throws ResendException {
        var builder = CreateEmailOptions.builder().from(emailFrom).to(to).subject(subject).html(htmlBody);
        if (StringUtils.hasText(replyTo)) {
            builder.replyTo(replyTo);
        }
        CreateEmailResponse response = new Resend(resendApiKey).emails().send(builder.build());
        String emailId = response == null ? null : response.getId();
        log.debug("Resend email accepted. recipient={}, emailId={}", LogSanitizer.maskEmail(to), emailId);
        return emailId;
    }

    @java.lang.SuppressWarnings("all")
    public ResendEmailClient() {
    }
}
