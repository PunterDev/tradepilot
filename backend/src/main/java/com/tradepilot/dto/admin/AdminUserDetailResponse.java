package com.tradepilot.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminUserDetailResponse {
    private AdminUserResponse user;
    private List<AdminContactMessageResponse> contactMessages;
    private List<AdminRecentUpgradeResponse> auditLogs;
}
