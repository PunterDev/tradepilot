package com.tradepilot;

import com.tradepilot.activation.ActivationService;
import com.tradepilot.activation.ActivationStatusResponse;
import com.tradepilot.activation.UserFeaturePreview;
import com.tradepilot.activation.UserFeaturePreviewRepository;
import com.tradepilot.activation.UserFeaturePreviewService;
import com.tradepilot.activation.UserFeaturePreviewStatus;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.dto.account.TradingAccountRequest;
import com.tradepilot.dto.account.TradingAccountResponse;
import com.tradepilot.dto.trade.TradeImportRequest;
import com.tradepilot.dto.trade.TradeResponse;
import com.tradepilot.entity.AiTradeReview;
import com.tradepilot.entity.User;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.PropFirmType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.enums.SetupType;
import com.tradepilot.enums.TradingAccountStatus;
import com.tradepilot.enums.UserRole;
import com.tradepilot.featureusage.FeatureUsageAction;
import com.tradepilot.featureusage.FeatureUsageRepository;
import com.tradepilot.repository.AiTradeReviewRepository;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.service.TradeService;
import com.tradepilot.service.TradingAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ActivationServiceTests {
    @Autowired ActivationService activationService;
    @Autowired UserFeaturePreviewService previewService;
    @Autowired UserFeaturePreviewRepository previewRepository;
    @Autowired UserRepository userRepository;
    @Autowired TradingAccountService tradingAccountService;
    @Autowired TradeService tradeService;
    @Autowired AiTradeReviewRepository aiTradeReviewRepository;
    @Autowired FeatureUsageRepository featureUsageRepository;

    @Test
    void statusGuidesNewUserFromAccountCreationToImport() {
        User user = createUser("activation-new-user@example.com", PlanType.FREE);

        ActivationStatusResponse initial = activationService.status(user);
        assertThat(initial.hasTradingAccount()).isFalse();
        assertThat(initial.recommendedNextAction()).isEqualTo("CREATE_TRADING_ACCOUNT");

        createAccount(user, "Activation");

        ActivationStatusResponse afterAccount = activationService.status(user);
        assertThat(afterAccount.hasTradingAccount()).isTrue();
        assertThat(afterAccount.firstImportCompleted()).isFalse();
        assertThat(afterAccount.recommendedNextAction()).isEqualTo("IMPORT_TRADES");
    }

    @Test
    void completedFirstInsightUnlocksSingleWeeklyCoachPreviewForFreeUser() {
        User user = createUser("activation-first-insight@example.com", PlanType.FREE);
        TradingAccountResponse account = createAccount(user, "Insight");
        TradeResponse trade = tradeService.importTrades(user, List.of(trade(account.getId(), "activation-first-insight-trade"))).get(0);
        aiTradeReviewRepository.save(AiTradeReview.builder()
                .userId(user.getId())
                .tradeId(trade.getId())
                .accountId(account.getId())
                .reviewDate("2026-07-15")
                .symbol("NQ")
                .pnl(125.0)
                .reviewVersion("test")
                .reviewHash("activation-first-insight")
                .summary("Your strongest improvement is waiting for clean execution review.")
                .createdAt(Instant.parse("2026-07-15T15:30:00Z"))
                .status("COMPLETED")
                .build());

        ActivationStatusResponse status = activationService.status(user);
        ActivationStatusResponse repeated = activationService.status(user);

        assertThat(status.activated()).isTrue();
        assertThat(status.firstInsightGenerated()).isTrue();
        assertThat(status.proPreview().available()).isTrue();
        assertThat(repeated.proPreview().available()).isTrue();
        assertThat(previewRepository.findAll().stream()
                .filter(row -> row.getUserId().equals(user.getId()))
                .filter(row -> UserFeaturePreviewService.WEEKLY_COACH_PREVIEW.equals(row.getFeatureKey())))
                .hasSize(1);
        assertThat(featureUsageRepository.existsByUserIdAndAction(user.getId(), FeatureUsageAction.PRO_PREVIEW_UNLOCKED)).isTrue();
    }

    @Test
    void weeklyCoachPreviewCanBeReservedReleasedAndConsumed() {
        User user = createUser("activation-preview-lifecycle@example.com", PlanType.FREE);
        UserFeaturePreview unlocked = previewService.ensureWeeklyCoachPreviewUnlocked(user, true);

        UserFeaturePreview reserved = previewService.reserveWeeklyCoachPreview(user);
        previewService.attachReservedResource(reserved.getId(), 1234L);
        previewService.releaseReservedWeeklyCoachPreview(user.getId(), 1234L);

        UserFeaturePreview released = previewRepository.findById(unlocked.getId()).orElseThrow();
        assertThat(released.getStatus()).isEqualTo(UserFeaturePreviewStatus.AVAILABLE);
        assertThat(released.getReservedResourceId()).isNull();

        UserFeaturePreview reservedAgain = previewService.reserveWeeklyCoachPreview(user);
        previewService.attachReservedResource(reservedAgain.getId(), 5678L);
        previewService.consumeReservedWeeklyCoachPreview(user.getId(), 5678L);

        UserFeaturePreview consumed = previewRepository.findById(unlocked.getId()).orElseThrow();
        assertThat(consumed.getStatus()).isEqualTo(UserFeaturePreviewStatus.CONSUMED);
        assertThat(consumed.getConsumedAt()).isNotNull();
    }

    @Test
    void proUsersDoNotReceiveFreeProPreview() {
        User user = createUser("activation-pro-no-preview@example.com", PlanType.PRO);

        assertThat(previewService.ensureWeeklyCoachPreviewUnlocked(user, true)).isNull();
        assertThat(previewRepository.findByUserIdAndFeatureKey(user.getId(), UserFeaturePreviewService.WEEKLY_COACH_PREVIEW)).isEmpty();
    }

    private User createUser(String email, PlanType plan) {
        return userRepository.save(User.builder()
                .name(email)
                .email(email)
                .passwordHash("test-password-hash")
                .emailVerified(true)
                .role(UserRole.USER)
                .plan(plan)
                .subscriptionStatus(plan == PlanType.PRO ? SubscriptionStatus.ACTIVE : SubscriptionStatus.NONE)
                .build());
    }

    private TradingAccountResponse createAccount(User user, String name) {
        return tradingAccountService.create(
                user,
                new TradingAccountRequest(
                        name,
                        PropFirmType.TOPSTEP,
                        null,
                        null,
                        BigDecimal.valueOf(50_000),
                        null,
                        TradingAccountStatus.ACTIVE
                )
        );
    }

    private TradeImportRequest trade(Long accountId, String externalKey) {
        LocalDate date = LocalDate.of(2026, 7, 15);
        return new TradeImportRequest(
                accountId,
                "tradovate.csv",
                externalKey,
                "NQ",
                "FUTURES",
                date,
                date.atTime(9, 30),
                date.atTime(9, 45),
                "LONG",
                SetupType.ORB,
                SessionType.NY_AM,
                BigDecimal.valueOf(18_000),
                BigDecimal.valueOf(18_010),
                1,
                BigDecimal.valueOf(125),
                null,
                OutcomeType.WIN,
                null,
                null,
                null
        );
    }
}
