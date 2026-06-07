package com.tradepilot.dto.admin;

import com.tradepilot.enums.ContactMessageStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminContactMessageResponse {
    private Long id;
    private String name;
    private String email;
    private String subject;
    private String message;
    private ContactMessageStatus status;
    private boolean emailSent;
    private String emailError;
    private String remoteAddress;
    private String userAgent;
    private String adminNotes;
    private String leadSource;
    private LocalDateTime repliedAt;
    private LocalDateTime lastViewedAt;
    private String replyBody;
    private String replySubject;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
