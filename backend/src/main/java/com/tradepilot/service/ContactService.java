package com.tradepilot.service;

import com.tradepilot.dto.contact.ContactRequest;
import com.tradepilot.dto.contact.ContactResponse;
import com.tradepilot.entity.ContactMessage;
import com.tradepilot.enums.ContactMessageStatus;
import com.tradepilot.repository.ContactMessageRepository;
import com.tradepilot.util.LogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {
    private final EmailAsyncService emailAsyncService;
    private final ContactMessageRepository contactMessageRepository;

    @Value("${app.contact-email:support@tradepilot.app}")
    private String contactEmail;

    public ContactResponse submit(ContactRequest request, String remoteAddress, String userAgent) {
        String email = normalize(request.email());
        log.info("Contact form submission received. email={}, subjectLength={}, messageLength={}",
                LogSanitizer.maskEmail(email),
                request.subject().trim().length(),
                request.message().trim().length());

        ContactMessage message = contactMessageRepository.save(ContactMessage.builder()
                .name(request.name().trim())
                .email(email)
                .subject(request.subject().trim())
                .message(request.message().trim())
                .status(ContactMessageStatus.NEW)
                .emailSent(false)
                .remoteAddress(truncate(remoteAddress, 120))
                .userAgent(truncate(userAgent, 500))
                .leadSource("Contact form")
                .build());

        emailAsyncService.sendContactEmail(
                message.getId(),
                resolveDestination(),
                message.getName(),
                email,
                message.getSubject(),
                message.getMessage()
        );
        log.info("Contact form processed. contactMessageId={}, emailQueued=true", message.getId());

        return new ContactResponse(true, "Message sent successfully.");
    }

    private String resolveDestination() {
        return StringUtils.hasText(contactEmail) ? contactEmail : "support@tradepilot.app";
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
