package com.tradepilot.service;

import com.tradepilot.billing.*;
import com.tradepilot.dto.account.TradingAccountResponse;
import com.tradepilot.dto.admin.AdminPlanChangeRequest;
import com.tradepilot.dto.admin.AdminContactMessageResponse;
import com.tradepilot.dto.admin.AdminRecentUpgradeResponse;
import com.tradepilot.dto.admin.AdminUserDetailResponse;
import com.tradepilot.dto.admin.AdminUserResponse;
import com.tradepilot.dto.common.PageRequestParams;
import com.tradepilot.dto.common.PagedResponse;
import com.tradepilot.entity.AdminAuditLog;
import com.tradepilot.entity.ContactMessage;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.enums.AdminAuditAction;
import com.tradepilot.enums.UserRole;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.repository.AiTradeReviewRepository;
import com.tradepilot.repository.AdminAuditLogRepository;
import com.tradepilot.repository.ContactMessageRepository;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import com.tradepilot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {
    private final UserRepository userRepository;
    private final AdminAuditLogRepository auditLogRepository;
    private final UsageLimitService usageLimitService;
    private final AiUsageEventRepository aiUsageEventRepository;
    private final TradingAccountRepository tradingAccountRepository;
    private final TradeRepository tradeRepository;
    private final AiTradeReviewRepository aiTradeReviewRepository;
    private final ContactMessageRepository contactMessageRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PagedResponse<AdminUserResponse> listUsers(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageRequestParams.pageable(page, size, sanitizeUserSort(sortBy), sortDir, "createdAt");
        log.info("Admin users list requested. page={}, size={}, sortBy={}, sortDir={}",
                pageable.getPageNumber(), pageable.getPageSize(), sanitizeUserSort(sortBy), sortDir);
        return page(userRepository.findAll(pageable), sanitizeUserSort(sortBy), sortDir);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PagedResponse<AdminUserResponse> searchUsers(String query, int page, int size, String sortBy, String sortDir) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.isEmpty()) {
            return listUsers(page, size, sortBy, sortDir);
        }

        Pageable pageable = PageRequestParams.pageable(page, size, sanitizeUserSort(sortBy), sortDir, "createdAt");
        log.info("Admin users search requested. queryLength={}, page={}, size={}",
                normalized.length(), pageable.getPageNumber(), pageable.getPageSize());
        return page(
                userRepository.findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(normalized, normalized, pageable),
                sanitizeUserSort(sortBy),
                sortDir
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUser(Long userId) {
        User target = getTarget(userId);
        log.info("Admin user detail requested. targetUserId={}", userId);
        return AdminUserDetailResponse.builder()
                .user(mapUser(target))
                .accounts(tradingAccountRepository.findByUserOrderByCreatedAtDesc(target)
                        .stream()
                        .map(this::mapTradingAccount)
                        .toList())
                .contactMessages(contactMessageRepository.findTop10ByEmailIgnoreCaseOrderByCreatedAtDesc(target.getEmail())
                        .stream()
                        .map(this::mapContactMessage)
                        .toList())
                .auditLogs(auditLogRepository.findByTargetUserIdOrderByCreatedAtDesc(
                                target.getId(),
                                PageRequestParams.pageable(0, 10, "createdAt", "desc", "createdAt")
                        )
                        .stream()
                        .map(this::mapAuditLog)
                        .toList())
                .build();
    }

    private TradingAccountResponse mapTradingAccount(TradingAccount account) {
        return TradingAccountResponse.of(
                account.getId(),
                account.getName(),
                account.getPropFirm(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getStartingBalance(),
                account.getDefaultRiskAmount(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AdminUserResponse upgradeToPro(User admin, Long targetUserId, AdminPlanChangeRequest request) {
        User target = getTarget(targetUserId);
        String before = snapshot(target);

        target.setPlan(PlanType.PRO);
        target.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        target.setProUntil(request.proUntil());
        clearStripeManagedState(target);
        User saved = userRepository.save(target);

        audit(admin, saved, AdminAuditAction.UPGRADE_TO_PRO, request.reason(), before, snapshot(saved));
        log.info("Admin upgraded user to PRO. adminUserId={}, targetUserId={}", admin.getId(), saved.getId());
        return mapUser(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AdminUserResponse downgradeToFree(User admin, Long targetUserId, AdminPlanChangeRequest request) {
        User target = getTarget(targetUserId);
        String before = snapshot(target);

        target.setPlan(PlanType.FREE);
        target.setSubscriptionStatus(SubscriptionStatus.NONE);
        target.setProUntil(null);
        clearStripeManagedState(target);
        User saved = userRepository.save(target);

        audit(admin, saved, AdminAuditAction.DOWNGRADE_TO_FREE, request.reason(), before, snapshot(saved));
        log.info("Admin downgraded user to FREE. adminUserId={}, targetUserId={}", admin.getId(), saved.getId());
        return mapUser(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AdminUserResponse makeAdmin(User admin, Long targetUserId, AdminPlanChangeRequest request) {
        User target = getTarget(targetUserId);
        String before = snapshot(target);

        target.setRole(UserRole.ADMIN);
        User saved = userRepository.save(target);

        audit(admin, saved, AdminAuditAction.ROLE_CHANGE, request.reason(), before, snapshot(saved));
        log.info("Admin granted ADMIN role. adminUserId={}, targetUserId={}", admin.getId(), saved.getId());
        return mapUser(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AdminUserResponse removeAdmin(User admin, Long targetUserId, AdminPlanChangeRequest request) {
        User target = getTarget(targetUserId);
        String before = snapshot(target);

        target.setRole(UserRole.USER);
        User saved = userRepository.save(target);

        audit(admin, saved, AdminAuditAction.ROLE_CHANGE, request.reason(), before, snapshot(saved));
        log.info("Admin removed ADMIN role. adminUserId={}, targetUserId={}", admin.getId(), saved.getId());
        return mapUser(saved);
    }

    private User getTarget(Long targetUserId) {
        return userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void clearStripeManagedState(User target) {
        target.setStripeCustomerId(null);
        target.setStripeSubscriptionId(null);
        target.setStripeCancelAtPeriodEnd(false);
        target.setStripeCancelAt(null);
        target.setStripeCanceledAt(null);
        target.setCurrentPeriodStart(null);
        target.setCurrentPeriodEnd(null);
    }

    private PagedResponse<AdminUserResponse> page(Page<User> users, String sortBy, String sortDir) {
        Map<Long, UserMetrics> metricsByUserId = batchMetrics(users.getContent());
        return PagedResponse.of(
                users.getContent().stream()
                        .map(user -> mapUser(user, metricsByUserId.getOrDefault(user.getId(), UserMetrics.empty())))
                        .toList(),
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages(),
                users.isFirst(),
                users.isLast(),
                sanitizeUserSort(sortBy),
                "asc".equalsIgnoreCase(sortDir) ? "asc" : "desc"
        );
    }

    private String sanitizeUserSort(String sortBy) {
        return switch (sortBy == null ? "" : sortBy) {
            case "email", "name", "plan", "role", "emailVerified", "createdAt" -> sortBy;
            default -> "createdAt";
        };
    }

    private AdminUserResponse mapUser(User user) {
        return mapUser(user, singleUserMetrics(user));
    }

    private AdminUserResponse mapUser(User user, UserMetrics metrics) {
        Map<String, Integer> monthlyUsage = new HashMap<>();
        monthlyUsage.put("TRADING_ACCOUNTS", Math.toIntExact(Math.min(Integer.MAX_VALUE, metrics.tradingAccountCount())));
        monthlyUsage.put("IMPORTED_TRADES", Math.toIntExact(Math.min(Integer.MAX_VALUE, metrics.tradeCount())));
        monthlyUsage.put(UsageType.DAILY_COACH.name(), metrics.dailyCoachUsage());
        monthlyUsage.put(UsageType.WEEKLY_COACH.name(), metrics.weeklyCoachUsage());
        monthlyUsage.put(UsageType.TRADING_DNA.name(), metrics.tradingDnaUsage());
        monthlyUsage.put(UsageType.AI_LEAK_DETECTOR.name(), metrics.leakDetectorUsage());
        monthlyUsage.put(UsageType.AI_TRADE_REVIEW.name(), metrics.aiReviewUsage());
        monthlyUsage.put(UsageType.SESSION_REVIEW.name(), metrics.sessionReviewUsage());
        monthlyUsage.put("AI_CREDITS", metrics.totalAiCredits());
        Map<String, Integer> limits = limits(user.getPlan());
        Map<String, Integer> remaining = remaining(limits, monthlyUsage);
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
        response.setSubscriptionStatus(user.getSubscriptionStatus());
        response.setTradingAccountCount(metrics.tradingAccountCount());
        response.setTradeCount(metrics.tradeCount());
        response.setAiReviewCount(metrics.aiReviewCount());
        response.setDailyCoachUsage(metrics.dailyCoachUsage());
        response.setWeeklyCoachUsage(metrics.weeklyCoachUsage());
        response.setTradingDnaUsage(metrics.tradingDnaUsage());
        response.setPlanSource(resolvePlanSource(user));
        response.setUsageThisMonth(monthlyUsage);
        response.setLimits(limits);
        response.setRemaining(remaining);
        return response;
    }

    private Map<Long, UserMetrics> batchMetrics(List<User> users) {
        Map<Long, UserMetricsBuilder> builders = new HashMap<>();
        List<Long> userIds = users.stream().map(User::getId).toList();
        userIds.forEach(id -> builders.put(id, new UserMetricsBuilder()));
        if (userIds.isEmpty()) {
            return Map.of();
        }

        tradingAccountRepository.countByUserIds(userIds).forEach(row ->
                builders.get((Long) row[0]).tradingAccountCount = longValue(row[1]));
        tradeRepository.countByUserIds(userIds).forEach(row ->
                builders.get((Long) row[0]).tradeCount = longValue(row[1]));
        aiTradeReviewRepository.countByUserIds(userIds).forEach(row ->
                builders.get((Long) row[0]).aiReviewCount = longValue(row[1]));

        PeriodWindow period = monthlyPeriod();
        aiUsageEventRepository.countByUserIdsAndUsageTypeBetween(userIds, period.start(), period.end()).forEach(row -> {
            UserMetricsBuilder builder = builders.get((Long) row[0]);
            if (builder == null) {
                return;
            }
            UsageType type = (UsageType) row[1];
            int count = Math.toIntExact(Math.min(Integer.MAX_VALUE, longValue(row[2])));
            applyUsageCount(builder, type, count * creditsFor(type));
        });

        Map<Long, UserMetrics> metrics = new HashMap<>();
        builders.forEach((userId, builder) -> metrics.put(userId, builder.build()));
        log.trace("Admin user metrics batched. userCount={}", userIds.size());
        return metrics;
    }

    private UserMetrics singleUserMetrics(User user) {
        Map<Long, UserMetrics> metrics = batchMetrics(List.of(user));
        return metrics.getOrDefault(user.getId(), UserMetrics.empty());
    }

    private void applyUsageCount(UserMetricsBuilder builder, UsageType type, int count) {
        if (type == UsageType.AI_TRADE_REVIEW) {
            builder.aiReviewUsage = count;
        } else if (type == UsageType.DAILY_COACH) {
            builder.dailyCoachUsage = count;
        } else if (type == UsageType.WEEKLY_COACH) {
            builder.weeklyCoachUsage = count;
        } else if (type == UsageType.TRADING_DNA) {
            builder.tradingDnaUsage = count;
        } else if (type == UsageType.AI_LEAK_DETECTOR) {
            builder.leakDetectorUsage = count;
        } else if (type == UsageType.SESSION_REVIEW) {
            builder.sessionReviewUsage = count;
        }
    }

    private PeriodWindow monthlyPeriod() {
        YearMonth current = YearMonth.now();
        return new PeriodWindow(current.atDay(1).atStartOfDay(), current.plusMonths(1).atDay(1).atStartOfDay());
    }

    private Map<String, Integer> limits(PlanType plan) {
        boolean pro = plan == PlanType.PRO;
        Map<String, Integer> limits = new HashMap<>();
        limits.put("TRADING_ACCOUNTS", pro ? -1 : 1);
        limits.put("IMPORTED_TRADES", pro ? -1 : 100);
        int monthlyCredits = pro ? 100 : 10;
        limits.put("AI_CREDITS", monthlyCredits);
        limits.put(UsageType.AI_TRADE_REVIEW.name(), monthlyCredits);
        limits.put(UsageType.DAILY_COACH.name(), monthlyCredits);
        limits.put(UsageType.WEEKLY_COACH.name(), monthlyCredits);
        limits.put(UsageType.TRADING_DNA.name(), monthlyCredits);
        limits.put(UsageType.AI_LEAK_DETECTOR.name(), monthlyCredits);
        limits.put(UsageType.SESSION_REVIEW.name(), monthlyCredits);
        return limits;
    }

    private int creditsFor(UsageType type) {
        return switch (type) {
            case AI_TRADE_REVIEW -> 1;
            case DAILY_COACH, SESSION_REVIEW -> 2;
            case TRADING_DNA, AI_LEAK_DETECTOR -> 3;
            case WEEKLY_COACH -> 4;
        };
    }

    private Map<String, Integer> remaining(Map<String, Integer> limits, Map<String, Integer> used) {
        Map<String, Integer> remaining = new HashMap<>();
        limits.forEach((key, limit) -> {
            int usage = used.getOrDefault(key, 0);
            remaining.put(key, limit < 0 ? -1 : Math.max(0, limit - usage));
        });
        return remaining;
    }

    private long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private record PeriodWindow(LocalDateTime start, LocalDateTime end) {
    }

    private record UserMetrics(
            long tradingAccountCount,
            long tradeCount,
            long aiReviewCount,
            int aiReviewUsage,
            int dailyCoachUsage,
            int weeklyCoachUsage,
            int tradingDnaUsage,
            int leakDetectorUsage,
            int sessionReviewUsage
    ) {
        private int totalAiCredits() {
            return aiReviewUsage + dailyCoachUsage + weeklyCoachUsage + tradingDnaUsage + leakDetectorUsage + sessionReviewUsage;
        }

        private static UserMetrics empty() {
            return new UserMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    private static class UserMetricsBuilder {
        private long tradingAccountCount;
        private long tradeCount;
        private long aiReviewCount;
        private int aiReviewUsage;
        private int dailyCoachUsage;
        private int weeklyCoachUsage;
        private int tradingDnaUsage;
        private int leakDetectorUsage;
        private int sessionReviewUsage;

        private UserMetrics build() {
            return new UserMetrics(
                    tradingAccountCount,
                    tradeCount,
                    aiReviewCount,
                    aiReviewUsage,
                    dailyCoachUsage,
                    weeklyCoachUsage,
                    tradingDnaUsage,
                    leakDetectorUsage,
                    sessionReviewUsage
            );
        }
    }

    private void audit(User admin, User target, AdminAuditAction action, String reason, String before, String after) {
        auditLogRepository.save(AdminAuditLog.builder()
                .adminUserId(admin.getId())
                .targetUserId(target.getId())
                .action(action)
                .reason(reason)
                .beforeValue(before)
                .afterValue(after)
                .build());
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

    private String resolvePlanSource(User user) {
        if (user.getPlan() != PlanType.PRO) {
            return "FREE";
        }
        if ("mock_subscription".equals(user.getStripeSubscriptionId()) || "mock_customer".equals(user.getStripeCustomerId())) {
            return "DEV_MOCK";
        }
        if (StringUtils.hasText(user.getStripeSubscriptionId())) {
            return "STRIPE";
        }
        return "ADMIN";
    }

    private String snapshot(User user) {
        return "plan=%s,role=%s,subscriptionStatus=%s,proUntil=%s,stripeCustomerId=%s,stripeSubscriptionId=%s"
                .formatted(
                        user.getPlan(),
                        user.getRole(),
                        user.getSubscriptionStatus(),
                        user.getProUntil(),
                        user.getStripeCustomerId(),
                        user.getStripeSubscriptionId()
                );
    }
}
