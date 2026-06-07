package com.tradepilot.dto.admin;

import com.tradepilot.enums.AdminAuditAction;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminRecentUpgradeResponse {
    private Long id;
    private Long adminUserId;
    private Long targetUserId;
    private String targetUserEmail;
    private String targetUserName;
    private AdminAuditAction action;
    private String reason;
    private String source;
    private String beforeValue;
    private String afterValue;
    private LocalDateTime createdAt;
}
