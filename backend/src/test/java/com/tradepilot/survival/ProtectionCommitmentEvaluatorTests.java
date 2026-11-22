package com.tradepilot.survival;

import static org.assertj.core.api.Assertions.assertThat;

import com.tradepilot.entity.Trade;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.survival.dto.CommitmentEvaluationResponse;
import com.tradepilot.survival.entity.NextSessionProtectionCommitment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProtectionCommitmentEvaluatorTests {
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private final ProtectionCommitmentEvaluator evaluator = new ProtectionCommitmentEvaluator();

    @Test
    void completesWhenAllSupportedRulesAreFollowed() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setMaxAttempts(2);
        commitment.setMaxNormalizedExposure(new BigDecimal("20"));
        commitment.setMaxSessionLoss(new BigDecimal("100"));

        var result = evaluator.evaluate(commitment, List.of(
                trade("09:30", "09:35", "-50", 1),
                trade("10:00", "10:05", "60", 2)), NEW_YORK);

        assertThat(result.overallStatus()).isEqualTo(ProtectionCommitmentStatus.COMPLETED);
    }

    @Test
    void detectsBrokenAttemptsExposureAndSessionLossUsingExactDecimals() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setMaxAttempts(1);
        commitment.setMaxNormalizedExposure(new BigDecimal("9.99"));
        commitment.setMaxSessionLoss(new BigDecimal("50"));

        var result = evaluator.evaluate(commitment, List.of(
                trade("09:30", "09:35", "-50.01", 1),
                trade("10:00", "10:05", "-1", 1)), NEW_YORK);

        assertThat(result.overallStatus()).isEqualTo(ProtectionCommitmentStatus.BROKEN);
        assertThat(result.rules()).extracting(r -> r.rule() + ":" + r.status())
                .contains("MAX_ATTEMPTS:BROKEN", "MAX_NORMALIZED_EXPOSURE:BROKEN", "MAX_SESSION_LOSS:BROKEN");
    }

    @Test
    void missingRealizedPnlIsNotEvaluableRatherThanZero() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setMaxSessionLoss(new BigDecimal("200"));
        Trade missingPnl = trade("09:30", "09:35", "-250", 1);
        missingPnl.setPnlAmount(null);

        var result = evaluator.evaluate(commitment, List.of(missingPnl), NEW_YORK);

        assertThat(rule(result, "MAX_SESSION_LOSS").status()).isEqualTo("NOT_EVALUABLE");
        assertThat(rule(result, "MAX_SESSION_LOSS").message()).contains("missing realized P&L");
        assertThat(result.overallStatus()).isEqualTo(ProtectionCommitmentStatus.NOT_EVALUABLE);
    }

    @Test
    void genuineZeroRealizedPnlRemainsEvaluable() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setMaxSessionLoss(new BigDecimal("200"));

        var result = evaluator.evaluate(commitment,
                List.of(trade("09:30", "09:35", "0", 1)), NEW_YORK);

        assertThat(rule(result, "MAX_SESSION_LOSS").status()).isEqualTo("FOLLOWED");
        assertThat(rule(result, "MAX_SESSION_LOSS").observedValue()).isEqualTo("0");
        assertThat(result.overallStatus()).isEqualTo(ProtectionCommitmentStatus.COMPLETED);
    }

    @Test
    void emptySessionDoesNotReportConfiguredRulesAsFollowed() {
        NextSessionProtectionCommitment commitment = allHistoricalRules();

        var result = evaluator.evaluate(commitment, List.of(), NEW_YORK);

        assertThat(result.rules()).allSatisfy(resultRule ->
                assertThat(resultRule.status()).isEqualTo("NOT_EVALUABLE"));
        assertThat(result.overallStatus()).isEqualTo(ProtectionCommitmentStatus.NOT_EVALUABLE);
    }

    @Test
    void missingEntryTimestampIsNotEvaluable() {
        NextSessionProtectionCommitment commitment = allHistoricalRules();
        Trade missingEntry = trade("09:30", "09:35", "-50", 1);
        missingEntry.setEntryTime(null);

        var result = evaluator.evaluate(commitment, List.of(missingEntry), NEW_YORK);

        assertHistoricalRulesNotEvaluable(result);
    }

    @Test
    void missingExitTimestampIsNotEvaluable() {
        NextSessionProtectionCommitment commitment = allHistoricalRules();
        Trade missingExit = trade("09:30", "09:35", "-50", 1);
        missingExit.setExitTime(null);

        var result = evaluator.evaluate(commitment, List.of(missingExit), NEW_YORK);

        assertHistoricalRulesNotEvaluable(result);
    }

    @Test
    void reversedTimestampsAreNotEvaluable() {
        NextSessionProtectionCommitment commitment = allHistoricalRules();

        var result = evaluator.evaluate(commitment,
                List.of(trade("09:40", "09:35", "-50", 1)), NEW_YORK);

        assertHistoricalRulesNotEvaluable(result);
    }

    @Test
    void sharedEntryAndExitBoundaryOnlyLeavesPauseWithoutObservableReentry() {
        NextSessionProtectionCommitment commitment = allHistoricalRules();

        var result = evaluator.evaluate(commitment, List.of(
                trade("09:30", "09:45", "-50", 1),
                trade("09:45", "09:50", "25", 1)), NEW_YORK);

        assertThat(rule(result, "MAX_ATTEMPTS").status()).isEqualTo("FOLLOWED");
        assertThat(rule(result, "MAX_NORMALIZED_EXPOSURE").status()).isEqualTo("FOLLOWED");
        assertThat(rule(result, "MAX_SESSION_LOSS").status()).isEqualTo("FOLLOWED");
        assertThat(rule(result, "POST_LOSS_PAUSE").status()).isEqualTo("NOT_EVALUABLE");
        assertThat(rule(result, "TRADING_WINDOW").status()).isEqualTo("FOLLOWED");
        assertThat(result.overallStatus()).isEqualTo(ProtectionCommitmentStatus.PARTIALLY_OBSERVED);
    }

    @Test
    void overlappingTradesOnlyInvalidateExposureAndUseCloseOrderForRealizedLoss() {
        NextSessionProtectionCommitment commitment = allHistoricalRules();
        commitment.setMaxSessionLoss(new BigDecimal("75"));

        var result = evaluator.evaluate(commitment, List.of(
                trade("09:30", "09:50", "-50", 1),
                trade("09:40", "09:45", "-50", 1)), NEW_YORK);

        assertThat(rule(result, "MAX_ATTEMPTS").status()).isEqualTo("FOLLOWED");
        assertThat(rule(result, "TRADING_WINDOW").status()).isEqualTo("FOLLOWED");
        assertThat(rule(result, "MAX_NORMALIZED_EXPOSURE").status()).isEqualTo("NOT_EVALUABLE");
        assertThat(rule(result, "MAX_SESSION_LOSS").status()).isEqualTo("BROKEN");
        assertThat(rule(result, "MAX_SESSION_LOSS").observedValue()).isEqualTo("-100");
    }

    @Test
    void duplicateCloseOrderThatCanChangeBoundaryResultIsNotEvaluable() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setMaxSessionLoss(new BigDecimal("500"));

        var result = evaluator.evaluate(commitment, List.of(
                trade("09:30", "10:00", "-600", 1),
                trade("09:40", "10:00", "500", 1)), NEW_YORK);

        assertThat(rule(result, "MAX_SESSION_LOSS").status()).isEqualTo("NOT_EVALUABLE");
        assertThat(rule(result, "MAX_SESSION_LOSS").message()).contains("ambiguous within-timestamp");
    }

    @Test
    void laterCertainLossBreachTakesPrecedenceOverEarlierAmbiguousCloseOrder() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setMaxSessionLoss(new BigDecimal("500"));

        var result = evaluator.evaluate(commitment, List.of(
                trade("09:30", "10:00", "-600", 1),
                trade("09:40", "10:00", "500", 1),
                trade("10:05", "10:10", "-500", 1)), NEW_YORK);

        assertThat(rule(result, "MAX_SESSION_LOSS").status()).isEqualTo("BROKEN");
        assertThat(rule(result, "MAX_SESSION_LOSS").observedValue()).isEqualTo("-600");
    }

    @Test
    void cumulativeLossCrossingRemainsBrokenAfterRecovery() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setMaxSessionLoss(new BigDecimal("500"));

        var result = evaluator.evaluate(commitment, List.of(
                trade("09:30", "09:35", "-300", 1),
                trade("09:40", "09:45", "-250", 1),
                trade("10:00", "10:05", "400", 1)), NEW_YORK);

        assertThat(rule(result, "MAX_SESSION_LOSS").status()).isEqualTo("BROKEN");
        assertThat(rule(result, "MAX_SESSION_LOSS").observedValue()).isEqualTo("-550");
        assertThat(rule(result, "MAX_SESSION_LOSS").message()).contains("1 completed trade(s) followed");
    }

    @Test
    void exactLossBoundaryTouchIsBroken() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setMaxSessionLoss(new BigDecimal("500"));

        var result = evaluator.evaluate(commitment,
                List.of(trade("09:30", "09:35", "-500", 1)), NEW_YORK);

        assertThat(rule(result, "MAX_SESSION_LOSS").status()).isEqualTo("BROKEN");
        assertThat(rule(result, "MAX_SESSION_LOSS").message()).contains("Equality counts");
    }

    @Test
    void cumulativeLossRemainingAboveBoundaryIsFollowed() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setMaxSessionLoss(new BigDecimal("500"));

        var result = evaluator.evaluate(commitment, List.of(
                trade("09:30", "09:35", "-300", 1),
                trade("09:40", "09:45", "100", 1),
                trade("10:00", "10:05", "-100", 1)), NEW_YORK);

        assertThat(rule(result, "MAX_SESSION_LOSS").status()).isEqualTo("FOLLOWED");
        assertThat(rule(result, "MAX_SESSION_LOSS").observedValue()).isEqualTo("-300");
    }

    @Test
    void sessionLossOrderingIsDeterministicForUnorderedInput() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setMaxSessionLoss(new BigDecimal("500"));
        Trade first = trade("09:30", "09:35", "-300", 1);
        Trade second = trade("09:40", "09:45", "-250", 1);
        Trade third = trade("10:00", "10:05", "400", 1);

        var chronological = evaluator.evaluate(commitment, List.of(first, second, third), NEW_YORK);
        var reversedInput = evaluator.evaluate(commitment, List.of(third, second, first), NEW_YORK);

        assertThat(rule(reversedInput, "MAX_SESSION_LOSS"))
                .isEqualTo(rule(chronological, "MAX_SESSION_LOSS"));
    }

    @Test
    void postLossPauseChecksEveryLossInConsecutiveLossSequence() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setPostLossPauseMinutes(20);

        var result = evaluator.evaluate(commitment, List.of(
                trade("09:30", "09:35", "-20", 1),
                trade("10:00", "10:05", "-10", 1),
                trade("10:10", "10:20", "15", 1)), NEW_YORK);

        assertThat(rule(result, "POST_LOSS_PAUSE").status()).isEqualTo("BROKEN");
        assertThat(rule(result, "POST_LOSS_PAUSE").observedValue()).isEqualTo("5 minutes");
        assertThat(rule(result, "POST_LOSS_PAUSE").message()).contains("At least one observed re-entry");
    }

    @Test
    void postLossPauseIsFollowedWhenEveryLossHasACompliantObservedReentry() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setPostLossPauseMinutes(20);

        var result = evaluator.evaluate(commitment, List.of(
                trade("09:30", "09:35", "-20", 1),
                trade("10:00", "10:05", "10", 1)), NEW_YORK);

        assertThat(rule(result, "POST_LOSS_PAUSE").status()).isEqualTo("FOLLOWED");
        assertThat(rule(result, "POST_LOSS_PAUSE").observedValue()).isEqualTo("25 minutes");
        assertThat(rule(result, "POST_LOSS_PAUSE").message())
                .contains("Every realized losing attempt had a subsequent observable re-entry");
    }

    @Test
    void alreadyOpenPositionDoesNotCountAsPostLossReentry() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setPostLossPauseMinutes(20);

        var result = evaluator.evaluate(commitment, List.of(
                trade("09:30", "09:45", "-20", 1),
                trade("09:40", "10:00", "10", 1)), NEW_YORK);

        assertThat(rule(result, "POST_LOSS_PAUSE").status()).isEqualTo("NOT_EVALUABLE");
        assertThat(rule(result, "POST_LOSS_PAUSE").message())
                .contains("no subsequent observable re-entry");
    }

    @Test
    void postLossPauseWithNoReentryIsNotEvaluable() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setPostLossPauseMinutes(20);

        var result = evaluator.evaluate(commitment,
                List.of(trade("09:30", "09:35", "-20", 1)), NEW_YORK);

        assertThat(rule(result, "POST_LOSS_PAUSE").status()).isEqualTo("NOT_EVALUABLE");
    }

    @Test
    void compliantEarlierPauseFollowedByFinalLossWithoutReentryIsNotEvaluable() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setPostLossPauseMinutes(20);

        var result = evaluator.evaluate(commitment, List.of(
                trade("09:30", "09:35", "-20", 1),
                trade("10:05", "10:10", "10", 1),
                trade("10:30", "10:35", "-10", 1)), NEW_YORK);

        assertThat(rule(result, "POST_LOSS_PAUSE").status()).isEqualTo("NOT_EVALUABLE");
        assertThat(rule(result, "POST_LOSS_PAUSE").message()).contains("1 realized losing attempt(s)");
    }

    @Test
    void knownBrokenPauseRemainsBrokenWhenFinalLossHasNoReentry() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setPostLossPauseMinutes(20);

        var result = evaluator.evaluate(commitment, List.of(
                trade("09:30", "09:35", "-20", 1),
                trade("09:40", "09:45", "10", 1),
                trade("10:30", "10:35", "-10", 1)), NEW_YORK);

        assertThat(rule(result, "POST_LOSS_PAUSE").status()).isEqualTo("BROKEN");
        assertThat(rule(result, "POST_LOSS_PAUSE").observedValue()).isEqualTo("5 minutes");
    }

    @Test
    void zeroRealizedPnlDoesNotTriggerPostLossPause() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setPostLossPauseMinutes(20);

        var result = evaluator.evaluate(commitment, List.of(
                trade("09:30", "09:35", "0", 1),
                trade("09:40", "09:45", "10", 1)), NEW_YORK);

        assertThat(rule(result, "POST_LOSS_PAUSE").status()).isEqualTo("NOT_EVALUABLE");
        assertThat(rule(result, "POST_LOSS_PAUSE").message()).contains("No realized loss");
    }

    @Test
    void missingRealizedPnlMakesPostLossPauseNotEvaluable() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setPostLossPauseMinutes(20);
        Trade missingPnl = trade("09:30", "09:35", "-20", 1);
        missingPnl.setPnlAmount(null);

        var result = evaluator.evaluate(commitment, List.of(
                missingPnl,
                trade("10:00", "10:05", "10", 1)), NEW_YORK);

        assertThat(rule(result, "POST_LOSS_PAUSE").status()).isEqualTo("NOT_EVALUABLE");
        assertThat(rule(result, "POST_LOSS_PAUSE").message()).contains("missing realized P&L");
    }

    @Test
    void missingExposureQuantityIsNotEvaluable() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setMaxNormalizedExposure(new BigDecimal("20"));
        Trade missingQuantity = trade("09:30", "09:35", "10", 1);
        missingQuantity.setQuantity(null);

        var result = evaluator.evaluate(commitment, List.of(missingQuantity), NEW_YORK);

        assertThat(rule(result, "MAX_NORMALIZED_EXPOSURE").status()).isEqualTo("NOT_EVALUABLE");
    }

    @Test
    void supportedRootAndContractSymbolsUseEstablishedStandardAndMicroNormalization() {
        assertExposure("NQ", "10");
        assertExposure("MNQ", "1");
        assertExposure("ES", "10");
        assertExposure("MES", "1");
        assertExposure("NQH6", "10");
        assertExposure("MNQH6", "1");
        assertExposure("ESH6", "10");
        assertExposure("MESH6", "1");
        assertExposure(" NQ", "10");
        assertExposure("MNQ ", "1");
    }

    @Test
    void unsupportedAndMalformedSymbolsCannotProduceFabricatedExposureFindings() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setMaxNormalizedExposure(new BigDecimal("20"));

        for (String symbol : List.of("CL", "UNKNOWN", "NQ_NOT_A_CONTRACT", "ESX6")) {
            var result = evaluator.evaluate(commitment,
                    List.of(tradeWithSymbol("09:30", "09:35", "10", 1, symbol)), NEW_YORK);

            assertThat(rule(result, "MAX_NORMALIZED_EXPOSURE").status())
                    .as("unsupported symbol %s", symbol)
                    .isEqualTo("NOT_EVALUABLE");
            assertThat(rule(result, "MAX_NORMALIZED_EXPOSURE").message())
                    .contains("cannot be reliably normalized");
        }
    }

    @Test
    void unsupportedSymbolOnlyInvalidatesExposureDependentEvidence() {
        NextSessionProtectionCommitment commitment = allHistoricalRules();

        var result = evaluator.evaluate(commitment, List.of(
                tradeWithSymbol("09:30", "09:35", "-20", 1, "CL"),
                trade("10:00", "10:05", "10", 1)), NEW_YORK);

        assertThat(rule(result, "MAX_NORMALIZED_EXPOSURE").status()).isEqualTo("NOT_EVALUABLE");
        assertThat(rule(result, "MAX_ATTEMPTS").status()).isEqualTo("FOLLOWED");
        assertThat(rule(result, "TRADING_WINDOW").status()).isEqualTo("FOLLOWED");
        assertThat(rule(result, "MAX_SESSION_LOSS").status()).isEqualTo("FOLLOWED");
        assertThat(rule(result, "POST_LOSS_PAUSE").status()).isEqualTo("FOLLOWED");
        assertThat(result.overallStatus()).isEqualTo(ProtectionCommitmentStatus.PARTIALLY_OBSERVED);
    }

    @Test
    void missingInstrumentSymbolIsNotEvaluable() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setMaxNormalizedExposure(new BigDecimal("20"));
        Trade missingSymbol = trade("09:30", "09:35", "10", 1);
        missingSymbol.setSymbol(null);

        var result = evaluator.evaluate(commitment, List.of(missingSymbol), NEW_YORK);

        assertThat(rule(result, "MAX_NORMALIZED_EXPOSURE").status()).isEqualTo("NOT_EVALUABLE");
    }

    @Test
    void zeroExposureQuantityIsNotEvaluable() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setMaxNormalizedExposure(new BigDecimal("20"));

        var result = evaluator.evaluate(commitment,
                List.of(trade("09:30", "09:35", "10", 0)), NEW_YORK);

        assertThat(rule(result, "MAX_NORMALIZED_EXPOSURE").status()).isEqualTo("NOT_EVALUABLE");
    }

    @Test
    void negativeExposureQuantityIsNotEvaluable() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setMaxNormalizedExposure(new BigDecimal("20"));

        var result = evaluator.evaluate(commitment,
                List.of(trade("09:30", "09:35", "10", -1)), NEW_YORK);

        assertThat(rule(result, "MAX_NORMALIZED_EXPOSURE").status()).isEqualTo("NOT_EVALUABLE");
    }

    @Test
    void blankInstrumentSymbolIsNotEvaluable() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setMaxNormalizedExposure(new BigDecimal("20"));
        Trade blankSymbol = trade("09:30", "09:35", "10", 1);
        blankSymbol.setSymbol("  ");

        var result = evaluator.evaluate(commitment, List.of(blankSymbol), NEW_YORK);

        assertThat(rule(result, "MAX_NORMALIZED_EXPOSURE").status()).isEqualTo("NOT_EVALUABLE");
    }

    @Test
    void evaluatesSameDayAndOvernightWindowsWithInclusiveStartExclusiveEnd() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setAllowedWindowStart(LocalTime.of(9, 30));
        commitment.setAllowedWindowEnd(LocalTime.of(10, 0));
        assertThat(evaluator.evaluate(commitment,
                List.of(trade("09:30", "09:35", "10", 1)), NEW_YORK).overallStatus())
                .isEqualTo(ProtectionCommitmentStatus.COMPLETED);
        assertThat(evaluator.evaluate(commitment,
                List.of(trade("10:00", "10:05", "10", 1)), NEW_YORK).overallStatus())
                .isEqualTo(ProtectionCommitmentStatus.BROKEN);

        commitment.setAllowedWindowStart(LocalTime.of(18, 0));
        commitment.setAllowedWindowEnd(LocalTime.of(3, 0));
        assertThat(evaluator.evaluate(commitment,
                List.of(trade("02:59", "03:05", "10", 1)), NEW_YORK).overallStatus())
                .isEqualTo(ProtectionCommitmentStatus.COMPLETED);
    }

    @Test
    void combinesObservedAndUnavailableRulesWithoutHidingBrokenResults() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setMaxAttempts(1);
        commitment.setRequireFreshSetupConfirmation(true);

        var partial = evaluator.evaluate(commitment,
                List.of(trade("09:30", "09:35", "10", 1)), NEW_YORK);
        var broken = evaluator.evaluate(commitment, List.of(
                trade("09:30", "09:35", "10", 1),
                trade("10:00", "10:05", "10", 1)), NEW_YORK);

        assertThat(partial.overallStatus()).isEqualTo(ProtectionCommitmentStatus.PARTIALLY_OBSERVED);
        assertThat(broken.overallStatus()).isEqualTo(ProtectionCommitmentStatus.BROKEN);
    }

    @Test
    void completedRowsRemainSingleAttemptsAfterImporterReconstruction() {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setMaxAttempts(1);

        var result = evaluator.evaluate(commitment,
                List.of(trade("09:30", "09:50", "20", 3)), NEW_YORK);

        assertThat(result.overallStatus()).isEqualTo(ProtectionCommitmentStatus.COMPLETED);
    }

    private NextSessionProtectionCommitment allHistoricalRules() {
        NextSessionProtectionCommitment value = commitment();
        value.setMaxAttempts(2);
        value.setMaxNormalizedExposure(new BigDecimal("20"));
        value.setMaxSessionLoss(new BigDecimal("200"));
        value.setPostLossPauseMinutes(20);
        value.setAllowedWindowStart(LocalTime.of(9, 30));
        value.setAllowedWindowEnd(LocalTime.of(11, 0));
        return value;
    }

    private void assertHistoricalRulesNotEvaluable(ProtectionCommitmentEvaluator.Evaluation result) {
        assertThat(result.rules()).allSatisfy(resultRule ->
                assertThat(resultRule.status()).isEqualTo("NOT_EVALUABLE"));
        assertThat(result.overallStatus()).isEqualTo(ProtectionCommitmentStatus.NOT_EVALUABLE);
    }

    private CommitmentEvaluationResponse.RuleResult rule(
            ProtectionCommitmentEvaluator.Evaluation result, String name) {
        return result.rules().stream()
                .filter(resultRule -> name.equals(resultRule.rule()))
                .findFirst()
                .orElseThrow();
    }

    private NextSessionProtectionCommitment commitment() {
        NextSessionProtectionCommitment value = new NextSessionProtectionCommitment();
        value.setEffectiveSessionDate(LocalDate.of(2026, 1, 1));
        value.setStatus(ProtectionCommitmentStatus.ACCEPTED);
        return value;
    }

    private void assertExposure(String symbol, String expected) {
        NextSessionProtectionCommitment commitment = commitment();
        commitment.setMaxNormalizedExposure(new BigDecimal("100"));

        var result = evaluator.evaluate(commitment,
                List.of(tradeWithSymbol("09:30", "09:35", "10", 1, symbol)), NEW_YORK);

        assertThat(rule(result, "MAX_NORMALIZED_EXPOSURE").status())
                .as("supported symbol %s", symbol)
                .isEqualTo("FOLLOWED");
        assertThat(rule(result, "MAX_NORMALIZED_EXPOSURE").observedValue()).isEqualTo(expected);
    }

    private Trade trade(String entry, String exit, String pnl, int quantity) {
        return tradeWithSymbol(entry, exit, pnl, quantity, "NQ");
    }

    private Trade tradeWithSymbol(String entry, String exit, String pnl, int quantity, String symbol) {
        LocalDate date = LocalDate.of(2026, 3, 8);
        return Trade.builder()
                .symbol(symbol)
                .market("FUTURES")
                .tradeDate(date)
                .entryTime(LocalDateTime.of(date, LocalTime.parse(entry)))
                .exitTime(LocalDateTime.of(date, LocalTime.parse(exit)))
                .side("LONG")
                .session(SessionType.NY_AM)
                .quantity(quantity)
                .pnlAmount(new BigDecimal(pnl))
                .outcome(new BigDecimal(pnl).signum() < 0 ? OutcomeType.LOSS : OutcomeType.WIN)
                .build();
    }
}
