package com.tradepilot.survival;

import static org.assertj.core.api.Assertions.assertThat;

import com.tradepilot.entity.Trade;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.survival.entity.NextSessionProtectionCommitment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProtectionCommitmentEvaluatorTests {
    private final ProtectionCommitmentEvaluator evaluator = new ProtectionCommitmentEvaluator();

    @Test void completesWhenAllSupportedRulesAreFollowed() {
        NextSessionProtectionCommitment commitment = commitment(); commitment.setMaxAttempts(2); commitment.setMaxNormalizedExposure(new BigDecimal("20")); commitment.setMaxSessionLoss(new BigDecimal("100"));
        var result = evaluator.evaluate(commitment, List.of(trade("09:30", "09:35", "-50", 1), trade("10:00", "10:05", "60", 2)), ZoneId.of("America/New_York"));
        assertThat(result.overallStatus()).isEqualTo(ProtectionCommitmentStatus.COMPLETED);
    }

    @Test void detectsBrokenAttemptsExposureAndSessionLossUsingExactDecimals() {
        NextSessionProtectionCommitment commitment = commitment(); commitment.setMaxAttempts(1); commitment.setMaxNormalizedExposure(new BigDecimal("9.99")); commitment.setMaxSessionLoss(new BigDecimal("50"));
        var result = evaluator.evaluate(commitment, List.of(trade("09:30", "09:35", "-50.01", 1), trade("10:00", "10:05", "-1", 1)), ZoneId.of("America/New_York"));
        assertThat(result.overallStatus()).isEqualTo(ProtectionCommitmentStatus.BROKEN);
        assertThat(result.rules()).extracting(r -> r.rule() + ":" + r.status()).contains("MAX_ATTEMPTS:BROKEN", "MAX_NORMALIZED_EXPOSURE:BROKEN", "MAX_SESSION_LOSS:BROKEN");
    }

    @Test void breaksWhenCumulativeRealizedLossReachesBoundaryEvenWhenSessionFinishesPositive() {
        NextSessionProtectionCommitment commitment = commitment(); commitment.setMaxSessionLoss(new BigDecimal("200"));
        var result = evaluator.evaluate(commitment, List.of(
                trade("09:30", "09:35", "-150", 1),
                trade("09:40", "09:45", "-100", 1),
                trade("10:00", "10:05", "300", 1)), ZoneId.of("America/New_York"));
        var loss = result.rules().stream().filter(rule -> "MAX_SESSION_LOSS".equals(rule.rule())).findFirst().orElseThrow();
        assertThat(loss.status()).isEqualTo("BROKEN");
        assertThat(loss.observedValue()).isEqualTo("-250");
        assertThat(loss.message()).contains("trade", "1 completed trade(s) followed", "Equality counts as reaching the boundary");
    }

    @Test void treatsAnExactLossBoundaryAsBroken() {
        NextSessionProtectionCommitment commitment = commitment(); commitment.setMaxSessionLoss(new BigDecimal("200"));
        var result = evaluator.evaluate(commitment, List.of(trade("09:30", "09:35", "-200", 1)), ZoneId.of("America/New_York"));
        assertThat(result.rules()).anySatisfy(rule -> {
            assertThat(rule.rule()).isEqualTo("MAX_SESSION_LOSS");
            assertThat(rule.status()).isEqualTo("BROKEN");
        });
    }

    @Test void doesNotInventAChronologicalLossResultWhenTimestampsAreIncompleteOrOverlapping() {
        NextSessionProtectionCommitment commitment = commitment(); commitment.setMaxSessionLoss(new BigDecimal("200"));
        Trade missingExit = trade("09:30", "09:35", "-250", 1); missingExit.setExitTime(null);
        var incomplete = evaluator.evaluate(commitment, List.of(missingExit), ZoneId.of("America/New_York"));
        assertThat(incomplete.rules()).anySatisfy(rule -> {
            assertThat(rule.rule()).isEqualTo("MAX_SESSION_LOSS"); assertThat(rule.status()).isEqualTo("NOT_EVALUABLE");
        });
        var overlapping = evaluator.evaluate(commitment, List.of(
                trade("09:30", "09:45", "-150", 1), trade("09:40", "09:50", "-100", 1)), ZoneId.of("America/New_York"));
        assertThat(overlapping.rules()).anySatisfy(rule -> {
            assertThat(rule.rule()).isEqualTo("MAX_SESSION_LOSS"); assertThat(rule.status()).isEqualTo("NOT_EVALUABLE");
        });
    }

    @Test void postLossPauseChecksEveryRealizedLossAndDoesNotTreatOneUntestedLossAsSuccess() {
        NextSessionProtectionCommitment commitment = commitment(); commitment.setPostLossPauseMinutes(20);
        var untested = evaluator.evaluate(commitment, List.of(trade("09:30", "09:35", "-20", 1)), ZoneId.of("America/New_York"));
        assertThat(pauseRule(untested).status()).isEqualTo("NOT_EVALUABLE");

        var broken = evaluator.evaluate(commitment, List.of(
                trade("09:30", "09:35", "-20", 1), trade("09:41", "09:50", "10", 1)), ZoneId.of("America/New_York"));
        assertThat(pauseRule(broken).status()).isEqualTo("BROKEN");
        assertThat(pauseRule(broken).message()).contains("Every realized losing attempt");
    }

    @Test void postLossPauseChecksBothLossesInAConsecutiveLossSequence() {
        NextSessionProtectionCommitment commitment = commitment(); commitment.setPostLossPauseMinutes(20);
        var result = evaluator.evaluate(commitment, List.of(
                trade("09:30", "09:35", "-20", 1),
                trade("10:00", "10:05", "-10", 1),
                trade("10:10", "10:20", "15", 1)), ZoneId.of("America/New_York"));
        assertThat(pauseRule(result).status()).isEqualTo("BROKEN");
        assertThat(pauseRule(result).observedValue()).isEqualTo("5 minutes");
    }

    @Test void postLossPauseDoesNotRequireAConsecutiveLossStreak() {
        NextSessionProtectionCommitment commitment = commitment(); commitment.setPostLossPauseMinutes(20);
        var result = evaluator.evaluate(commitment, List.of(
                trade("09:30", "09:35", "-20", 1),
                trade("10:00", "10:05", "10", 1),
                trade("10:30", "10:35", "-15", 1),
                trade("11:00", "11:05", "10", 1)), ZoneId.of("America/New_York"));
        assertThat(pauseRule(result).status()).isEqualTo("FOLLOWED");
        assertThat(pauseRule(result).observedValue()).isEqualTo("25 minutes");
    }

    @Test void postLossPauseIsNotEvaluableForOverlappingOrAmbiguousTimestamps() {
        NextSessionProtectionCommitment commitment = commitment(); commitment.setPostLossPauseMinutes(20);
        var overlapping = evaluator.evaluate(commitment, List.of(
                trade("09:30", "09:45", "-20", 1), trade("09:40", "09:50", "10", 1)), ZoneId.of("America/New_York"));
        assertThat(pauseRule(overlapping).status()).isEqualTo("NOT_EVALUABLE");

        var tied = evaluator.evaluate(commitment, List.of(
                trade("09:30", "09:45", "-20", 1), trade("09:45", "09:50", "10", 1)), ZoneId.of("America/New_York"));
        assertThat(pauseRule(tied).status()).isEqualTo("NOT_EVALUABLE");
    }

    @Test void evaluatesSameDayAndOvernightWindowsWithInclusiveStartExclusiveEnd() {
        NextSessionProtectionCommitment commitment = commitment(); commitment.setAllowedWindowStart(LocalTime.of(9, 30)); commitment.setAllowedWindowEnd(LocalTime.of(10, 0));
        assertThat(evaluator.evaluate(commitment, List.of(trade("09:30", "09:35", "10", 1)), ZoneId.of("America/New_York")).overallStatus()).isEqualTo(ProtectionCommitmentStatus.COMPLETED);
        assertThat(evaluator.evaluate(commitment, List.of(trade("10:00", "10:05", "10", 1)), ZoneId.of("America/New_York")).overallStatus()).isEqualTo(ProtectionCommitmentStatus.BROKEN);
        commitment.setAllowedWindowStart(LocalTime.of(18, 0)); commitment.setAllowedWindowEnd(LocalTime.of(3, 0));
        assertThat(evaluator.evaluate(commitment, List.of(trade("02:59", "03:05", "10", 1)), ZoneId.of("America/New_York")).overallStatus()).isEqualTo(ProtectionCommitmentStatus.COMPLETED);
    }

    @Test void combinesObservedAndUnavailableRulesWithoutHidingBrokenResults() {
        NextSessionProtectionCommitment commitment = commitment(); commitment.setMaxAttempts(1); commitment.setRequireFreshSetupConfirmation(true);
        var partial = evaluator.evaluate(commitment, List.of(trade("09:30", "09:35", "10", 1)), ZoneId.of("America/New_York"));
        assertThat(partial.overallStatus()).isEqualTo(ProtectionCommitmentStatus.PARTIALLY_OBSERVED);
        var broken = evaluator.evaluate(commitment, List.of(trade("09:30", "09:35", "10", 1), trade("10:00", "10:05", "10", 1)), ZoneId.of("America/New_York"));
        assertThat(broken.overallStatus()).isEqualTo(ProtectionCommitmentStatus.BROKEN);
    }

    @Test void completedRowsAreSingleAttemptsAfterImporterHasReconstructedPartialFillsAndScaleIns() {
        NextSessionProtectionCommitment commitment = commitment(); commitment.setMaxAttempts(1);
        List<Trade> completedRoundTrips = List.of(trade("09:30", "09:50", "20", 3));
        assertThat(evaluator.evaluate(commitment, completedRoundTrips, ZoneId.of("America/New_York")).overallStatus()).isEqualTo(ProtectionCommitmentStatus.COMPLETED);
    }

    private NextSessionProtectionCommitment commitment() { NextSessionProtectionCommitment value = new NextSessionProtectionCommitment(); value.setEffectiveSessionDate(LocalDate.of(2026, 1, 1)); value.setStatus(ProtectionCommitmentStatus.ACCEPTED); return value; }
    private com.tradepilot.survival.dto.CommitmentEvaluationResponse.RuleResult pauseRule(ProtectionCommitmentEvaluator.Evaluation result) { return result.rules().stream().filter(rule -> "POST_LOSS_PAUSE".equals(rule.rule())).findFirst().orElseThrow(); }
    private Trade trade(String entry, String exit, String pnl, int quantity) { LocalDate date = LocalDate.of(2026, 3, 8); return Trade.builder().symbol("NQ").market("FUTURES").tradeDate(date).entryTime(LocalDateTime.of(date, LocalTime.parse(entry))).exitTime(LocalDateTime.of(date, LocalTime.parse(exit))).side("LONG").session(SessionType.NY_AM).quantity(quantity).pnlAmount(new BigDecimal(pnl)).outcome(new BigDecimal(pnl).signum() < 0 ? OutcomeType.LOSS : OutcomeType.WIN).build(); }
}
