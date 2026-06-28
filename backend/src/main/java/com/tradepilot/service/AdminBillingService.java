package com.tradepilot.service;

import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.StripeProperties;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.dto.admin.AdminBillingSummaryResponse;
import com.tradepilot.dto.admin.AdminRecentUpgradeResponse;
import com.tradepilot.entity.AdminAuditLog;
import com.tradepilot.entity.User;
import com.tradepilot.enums.AdminAuditAction;
import com.tradepilot.repository.AdminAuditLogRepository;
import com.tradepilot.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

@Service
public class AdminBillingService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AdminBillingService.class);
    private static final BigDecimal MONTHLY_PRO_PRICE = new BigDecimal("29.99");
    private final UserRepository userRepository;
    private final AdminAuditLogRepository auditLogRepository;
    private final StripeProperties stripeProperties;
    private final AggregateCacheService aggregateCacheService;

    @PreAuthorize("hasRole(\'ADMIN\')")
    @Transactional(readOnly = true)
    public AdminBillingSummaryResponse summary() {
        log.info("Admin billing summary requested.");
        return aggregateCacheService.get("admin-billing-summary", Duration.ofSeconds(120), this::buildSummary);
    }

    private AdminBillingSummaryResponse buildSummary() {
        long totalUsers = userRepository.count();
        long freeUsers = userRepository.countByPlan(PlanType.FREE);
        long proUsers = userRepository.countByPlan(PlanType.PRO);
        long stripePayingSubscribers = userRepository.countByPlanAndSubscriptionStatusInAndStripeSubscriptionIdIsNotNull(PlanType.PRO, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING));
        long manualProUsers = Math.max(0, proUsers - stripePayingSubscribers);
        BigDecimal estimatedMrr = monthlyRevenue(stripePayingSubscribers);
        BigDecimal potentialMrr = monthlyRevenue(proUsers);
        log.trace("Admin billing aggregates loaded. totalUsers={}, proUsers={}, stripePayingSubscribers={}, manualProUsers={}", totalUsers, proUsers, stripePayingSubscribers, manualProUsers);
        return AdminBillingSummaryResponse.builder().totalUsers(totalUsers).freeUsers(freeUsers).proUsers(proUsers).stripePayingSubscribers(stripePayingSubscribers).manualProUsers(manualProUsers).conversionRate(totalUsers == 0 ? 0 : roundPercent((double) proUsers / totalUsers * 100)).paidConversionRate(totalUsers == 0 ? 0 : roundPercent((double) stripePayingSubscribers / totalUsers * 100)).estimatedMrr(estimatedMrr).estimatedArr(estimatedMrr.multiply(BigDecimal.valueOf(12)).setScale(2, RoundingMode.HALF_UP)).potentialMrr(potentialMrr).potentialArr(potentialMrr.multiply(BigDecimal.valueOf(12)).setScale(2, RoundingMode.HALF_UP)).manualUpgrades(auditLogRepository.countByAction(AdminAuditAction.UPGRADE_TO_PRO)).stripeSubscriptions(userRepository.countByStripeSubscriptionIdIsNotNull()).stripeCheckoutConfigured(stripeCheckoutConfigured()).stripeWebhookConfigured(StringUtils.hasText(stripeProperties.getWebhookSecret())).monthlyPriceIdConfigured(StringUtils.hasText(stripeProperties.getProPriceId())).yearlyPriceIdConfigured(StringUtils.hasText(stripeProperties.getProYearlyPriceId())).recentPlanChanges(recentPlanChanges()).build();
    }

    private List<AdminRecentUpgradeResponse> recentPlanChanges() {
        return auditLogRepository.findTop10ByActionInOrderByCreatedAtDesc(List.of(AdminAuditAction.UPGRADE_TO_PRO, AdminAuditAction.DOWNGRADE_TO_FREE)).stream().map(this::mapAuditLog).toList();
    }

    private AdminRecentUpgradeResponse mapAuditLog(AdminAuditLog log) {
        User target = userRepository.findById(log.getTargetUserId()).orElse(null);
        return AdminRecentUpgradeResponse.builder().id(log.getId()).adminUserId(log.getAdminUserId()).targetUserId(log.getTargetUserId()).targetUserEmail(target == null ? null : target.getEmail()).targetUserName(target == null ? null : target.getName()).action(log.getAction()).reason(log.getReason()).source(resolveSource(log)).beforeValue(log.getBeforeValue()).afterValue(log.getAfterValue()).createdAt(log.getCreatedAt()).build();
    }

    private BigDecimal monthlyRevenue(long subscribers) {
        return MONTHLY_PRO_PRICE.multiply(BigDecimal.valueOf(subscribers)).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean stripeCheckoutConfigured() {
        return StringUtils.hasText(stripeProperties.getSecretKey()) && StringUtils.hasText(stripeProperties.getProPriceId());
    }

    private String resolveSource(AdminAuditLog log) {
        String reason = log.getReason() == null ? "" : log.getReason().toLowerCase();
        return reason.contains("dev") || reason.contains("mock") ? "DEV_MOCK" : "ADMIN";
    }

    private double roundPercent(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    @java.lang.SuppressWarnings("all")
    public AdminBillingService(final UserRepository userRepository, final AdminAuditLogRepository auditLogRepository, final StripeProperties stripeProperties, final AggregateCacheService aggregateCacheService) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.stripeProperties = stripeProperties;
        this.aggregateCacheService = aggregateCacheService;
    }
}
