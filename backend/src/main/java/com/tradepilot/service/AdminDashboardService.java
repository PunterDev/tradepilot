package com.tradepilot.service;

import com.tradepilot.billing.AiUsageEventRepository;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.billing.UsageType;
import com.tradepilot.dto.admin.AdminContactMessageResponse;
import com.tradepilot.dto.admin.AdminDashboardSummaryResponse;
import com.tradepilot.dto.admin.AdminRecentUpgradeResponse;
import com.tradepilot.dto.admin.AdminUsageSummaryResponse;
import com.tradepilot.dto.admin.AdminUserResponse;
import com.tradepilot.entity.AdminAuditLog;
import com.tradepilot.entity.ContactMessage;
import com.tradepilot.entity.User;
import com.tradepilot.enums.AdminAuditAction;
import com.tradepilot.enums.ContactMessageStatus;
import com.tradepilot.enums.UserRole;
import com.tradepilot.repository.AdminAuditLogRepository;
import com.tradepilot.repository.AiTradeReviewRepository;
import com.tradepilot.repository.ContactMessageRepository;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import com.tradepilot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardService {
    private final UserRepository userRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final TradingAccountRepository tradingAccountRepository;
    private final TradeRepository tradeRepository;
    private final AiTradeReviewRepository aiTradeReviewRepository;
    private final AiUsageEventRepository usageEventRepository;
    private final AdminAuditLogRepository auditLogRepository;
    private final AggregateCacheService aggregateCacheService;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse summary() {
        log.info("Admin dashboard summary requested.");
        return aggregateCacheService.get("admin-dashboard-summary", Duration.ofSeconds(120), this::buildSummary);
    }

    private AdminDashboardSummaryResponse buildSummary() {
        AdminUsageSummaryResponse usage = usage();
        Object[] userCounts = (Object[]) userRepository.adminUserCounts();
        Object[] contactCounts = (Object[]) contactMessageRepository.adminContactCounts();
        log.trace("Admin dashboard summary aggregates loaded. totalUsers={}, totalContacts={}",
                longValue(userCounts, 0), longValue(contactCounts, 0));
        AdminDashboardSummaryResponse response = new AdminDashboardSummaryResponse();
        response.setTotalUsers(longValue(userCounts, 0));
        response.setFreeUsers(longValue(userCounts, 1));
        response.setProUsers(longValue(userCounts, 2));
        response.setAdminUsers(longValue(userCounts, 3));
        response.setVerifiedUsers(longValue(userCounts, 4));
        response.setUnverifiedUsers(longValue(userCounts, 5));
        response.setTotalContactMessages(longValue(contactCounts, 0));
        response.setNewContactMessages(longValue(contactCounts, 1));
        response.setOpenContactMessages(longValue(contactCounts, 2));
        response.setRepliedContactMessages(longValue(contactCounts, 3));
        response.setFailedEmailMessages(longValue(contactCounts, 4));
        response.setTotalTradingAccounts(tradingAccountRepository.count());
        response.setTotalTrades(tradeRepository.count());
        response.setTotalAiReviews(aiTradeReviewRepository.count());
        response.setDailyCoachUsage(usage.getDailyCoachUsage());
        response.setWeeklyCoachUsage(usage.getWeeklyCoachUsage());
        response.setTradingDnaUsage(usage.getTradingDnaUsage());
        response.setRecentUsers(recentUsers());
        response.setRecentContactMessages(recentContactMessages());
        response.setRecentUpgrades(recentUpgrades());
        return response;
    }

    private long longValue(Object[] row, int index) {
        Object value = row[index];
        return value instanceof Number number ? number.longValue() : 0L;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<AdminUserResponse> recentUsers() {
        return userRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(this::mapUser)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<AdminContactMessageResponse> recentContactMessages() {
        return contactMessageRepository.findTop5ByOrderByCreatedAtDesc().stream()
                .map(this::mapContactMessage)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AdminUsageSummaryResponse usage() {
        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);
        Map<UsageType, Long> counts = usageEventRepository.countByUsageTypeBetween(start, end).stream()
                .collect(Collectors.toMap(row -> (UsageType) row[0], row -> (Long) row[1]));
        long aiReviews = counts.getOrDefault(UsageType.AI_TRADE_REVIEW, 0L);
        long dailyCoach = counts.getOrDefault(UsageType.DAILY_COACH, 0L);
        long weeklyCoach = counts.getOrDefault(UsageType.WEEKLY_COACH, 0L);
        long tradingDna = counts.getOrDefault(UsageType.TRADING_DNA, 0L);

        AdminUsageSummaryResponse response = new AdminUsageSummaryResponse();
        response.setTotalAiReviews(aiReviews);
        response.setDailyCoachUsage(dailyCoach);
        response.setWeeklyCoachUsage(weeklyCoach);
        response.setTradingDnaUsage(tradingDna);
        response.setTotalMonthlyAiUsage(aiReviews + dailyCoach + weeklyCoach + tradingDna);
        return response;
    }

    private List<AdminRecentUpgradeResponse> recentUpgrades() {
        return auditLogRepository.findTop10ByActionInOrderByCreatedAtDesc(List.of(
                        AdminAuditAction.UPGRADE_TO_PRO,
                        AdminAuditAction.DOWNGRADE_TO_FREE
                ))
                .stream()
                .map(this::mapAuditLog)
                .toList();
    }

    private AdminUserResponse mapUser(User user) {
        AdminUserResponse response = new AdminUserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setName(user.getName());
        response.setPlan(user.getPlan());
        response.setRole(user.getRole());
        response.setCreatedAt(user.getCreatedAt());
        response.setEmailVerified(user.getEmailVerified());
        response.setStripeCustomerId(user.getStripeCustomerId());
        response.setStripeSubscriptionId(user.getStripeSubscriptionId());
        response.setProUntil(user.getProUntil());
        response.setSubscriptionStatus(user.getSubscriptionStatus() == null ? SubscriptionStatus.NONE : user.getSubscriptionStatus());
        return response;
    }

    private AdminContactMessageResponse mapContactMessage(ContactMessage message) {
        return AdminContactMessageResponse.builder()
                .id(message.getId())
                .name(message.getName())
                .email(message.getEmail())
                .subject(message.getSubject())
                .message(message.getMessage())
                .status(message.getStatus())
                .emailSent(message.isEmailSent())
                .emailError(message.getEmailError())
                .remoteAddress(message.getRemoteAddress())
                .userAgent(message.getUserAgent())
                .adminNotes(message.getAdminNotes())
                .leadSource(message.getLeadSource())
                .repliedAt(message.getRepliedAt())
                .lastViewedAt(message.getLastViewedAt())
                .replyBody(message.getReplyBody())
                .replySubject(message.getReplySubject())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }

    private AdminRecentUpgradeResponse mapAuditLog(AdminAuditLog log) {
        return AdminRecentUpgradeResponse.builder()
                .id(log.getId())
                .adminUserId(log.getAdminUserId())
                .targetUserId(log.getTargetUserId())
                .action(log.getAction())
                .reason(log.getReason())
                .source("ADMIN")
                .beforeValue(log.getBeforeValue())
                .afterValue(log.getAfterValue())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
