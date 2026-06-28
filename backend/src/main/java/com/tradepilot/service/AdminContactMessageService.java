package com.tradepilot.service;

import com.tradepilot.dto.admin.AdminContactMessageResponse;
import com.tradepilot.dto.admin.AdminContactMessageReplyRequest;
import com.tradepilot.dto.admin.AdminContactMessageSummaryResponse;
import com.tradepilot.dto.common.PageRequestParams;
import com.tradepilot.dto.common.PagedResponse;
import com.tradepilot.entity.ContactMessage;
import com.tradepilot.enums.ContactMessageStatus;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.repository.ContactMessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class AdminContactMessageService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AdminContactMessageService.class);
    private final ContactMessageRepository contactMessageRepository;
    private final EmailService emailService;
    private final TransactionTemplate transactionTemplate;

    @PreAuthorize("hasRole(\'ADMIN\')")
    @Transactional(readOnly = true)
    public PagedResponse<AdminContactMessageResponse> list(ContactMessageStatus status, Boolean emailFailed, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageRequestParams.pageable(page, size, sanitizeContactSort(sortBy), sortDir, "createdAt");
        log.info("Admin contact messages list requested. status={}, emailFailed={}, page={}, size={}", status, emailFailed, pageable.getPageNumber(), pageable.getPageSize());
        Page<ContactMessage> messages = Boolean.TRUE.equals(emailFailed) ? contactMessageRepository.findByEmailSentFalse(pageable) : status == null ? contactMessageRepository.findAll(pageable) : contactMessageRepository.findByStatus(status, pageable);
        return page(messages, sanitizeContactSort(sortBy), sortDir);
    }

    @PreAuthorize("hasRole(\'ADMIN\')")
    @Transactional
    public AdminContactMessageResponse get(Long id) {
        ContactMessage message = find(id);
        log.info("Admin contact message opened. contactMessageId={}", id);
        message.setLastViewedAt(LocalDateTime.now());
        if (message.getStatus() == ContactMessageStatus.NEW) {
            message.setStatus(ContactMessageStatus.OPEN);
        }
        return map(contactMessageRepository.save(message));
    }

    @PreAuthorize("hasRole(\'ADMIN\')")
    @Transactional
    public AdminContactMessageResponse updateStatus(Long id, ContactMessageStatus status) {
        ContactMessage message = find(id);
        message.setStatus(status);
        log.info("Admin contact message status updated. contactMessageId={}, status={}", id, status);
        return map(contactMessageRepository.save(message));
    }

    @PreAuthorize("hasRole(\'ADMIN\')")
    @Transactional
    public AdminContactMessageResponse updateNotes(Long id, String adminNotes) {
        ContactMessage message = find(id);
        message.setAdminNotes(adminNotes == null ? null : adminNotes.trim());
        log.info("Admin contact message notes updated. contactMessageId={}, notesLength={}", id, adminNotes == null ? 0 : adminNotes.trim().length());
        return map(contactMessageRepository.save(message));
    }

    @PreAuthorize("hasRole(\'ADMIN\')")
    public AdminContactMessageResponse reply(Long id, AdminContactMessageReplyRequest request) {
        log.info("Admin contact reply started. contactMessageId={}, subjectLength={}, messageLength={}", id, request.subject().trim().length(), request.message().trim().length());
        ContactMessage message = Objects.requireNonNull(transactionTemplate.execute(status -> {
            ContactMessage found = find(id);
            if (found.getStatus() == ContactMessageStatus.NEW) {
                found.setStatus(ContactMessageStatus.OPEN);
            }
            found.setReplySubject(request.subject().trim());
            found.setReplyBody(request.message().trim());
            return contactMessageRepository.save(found);
        }));
        EmailService.EmailSendResult result = emailService.sendContactReply(message.getEmail(), request.subject().trim(), request.message().trim());
        if (!result.sent()) {
            transactionTemplate.executeWithoutResult(status -> {
                ContactMessage failed = find(id);
                failed.setStatus(ContactMessageStatus.OPEN);
                failed.setEmailError(result.error());
                contactMessageRepository.save(failed);
            });
            log.warn("Admin contact reply failed. contactMessageId={}", id);
            throw new IllegalArgumentException("Reply email could not be sent. " + result.error());
        }
        ContactMessage saved = Objects.requireNonNull(transactionTemplate.execute(status -> {
            ContactMessage replied = find(id);
            replied.setStatus(ContactMessageStatus.REPLIED);
            replied.setEmailError(null);
            replied.setReplySubject(request.subject().trim());
            replied.setReplyBody(request.message().trim());
            replied.setRepliedAt(LocalDateTime.now());
            return contactMessageRepository.save(replied);
        }));
        log.info("Admin contact reply completed. contactMessageId={}", id);
        return map(saved);
    }

    @PreAuthorize("hasRole(\'ADMIN\')")
    @Transactional(readOnly = true)
    public AdminContactMessageSummaryResponse summary() {
        log.trace("Admin contact message summary requested.");
        return new AdminContactMessageSummaryResponse(contactMessageRepository.count(), contactMessageRepository.countByStatus(ContactMessageStatus.NEW), contactMessageRepository.countByStatus(ContactMessageStatus.OPEN), contactMessageRepository.countByStatus(ContactMessageStatus.REPLIED), contactMessageRepository.countByStatus(ContactMessageStatus.ARCHIVED), contactMessageRepository.countByEmailSentFalse());
    }

    private ContactMessage find(Long id) {
        return contactMessageRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Contact message not found"));
    }

    private PagedResponse<AdminContactMessageResponse> page(Page<ContactMessage> messages, String sortBy, String sortDir) {
        return PagedResponse.of(messages.getContent().stream().map(this::map).toList(), messages.getNumber(), messages.getSize(), messages.getTotalElements(), messages.getTotalPages(), messages.isFirst(), messages.isLast(), sanitizeContactSort(sortBy), "asc".equalsIgnoreCase(sortDir) ? "asc" : "desc");
    }

    private String sanitizeContactSort(String sortBy) {
        return switch (sortBy == null ? "" : sortBy) {
            case "createdAt", "updatedAt", "status", "emailSent" -> sortBy;
            default -> "createdAt";
        };
    }

    private AdminContactMessageResponse map(ContactMessage message) {
        return AdminContactMessageResponse.builder().id(message.getId()).name(message.getName()).email(message.getEmail()).subject(message.getSubject()).message(message.getMessage()).status(message.getStatus()).emailSent(message.isEmailSent()).emailError(message.getEmailError()).remoteAddress(message.getRemoteAddress()).userAgent(message.getUserAgent()).adminNotes(message.getAdminNotes()).leadSource(message.getLeadSource()).repliedAt(message.getRepliedAt()).lastViewedAt(message.getLastViewedAt()).replyBody(message.getReplyBody()).replySubject(message.getReplySubject()).createdAt(message.getCreatedAt()).updatedAt(message.getUpdatedAt()).build();
    }

    @java.lang.SuppressWarnings("all")
    public AdminContactMessageService(final ContactMessageRepository contactMessageRepository, final EmailService emailService, final TransactionTemplate transactionTemplate) {
        this.contactMessageRepository = contactMessageRepository;
        this.emailService = emailService;
        this.transactionTemplate = transactionTemplate;
    }
}
