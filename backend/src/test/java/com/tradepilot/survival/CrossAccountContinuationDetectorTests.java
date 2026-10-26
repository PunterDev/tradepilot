package com.tradepilot.survival;

import static org.junit.jupiter.api.Assertions.*;

import com.tradepilot.entity.ImportBatch;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.SessionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CrossAccountContinuationDetectorTests {
    private final CrossAccountContinuationDetector detector = new CrossAccountContinuationDetector();

    @Test void detectsSameFamilySameDirectionContinuationAcrossAccounts() {
        var candidates = detector.detect(List.of(loss(1, 11, "Apex 50K #1", "NQ", "SHORT", "10:00:05", "10:04:05", "-240", 2),
                trade(2, 12, "Topstep 50K #2", "MNQ", "SHORT", "10:12:05", "10:18:05", "-540", 20)), 11L);

        var candidate = assertSingle(candidates);
        assertEquals("NASDAQ", candidate.loss().family());
        assertEquals("SHORT", candidate.loss().direction());
        assertEquals(480, candidate.elapsedSeconds());
        assertEquals(0, new BigDecimal("20").compareTo(candidate.loss().exposure()));
        assertEquals(0, new BigDecimal("20").compareTo(candidate.later().exposure()));
        assertEquals("MODERATE", candidate.confidence());
    }

    @Test void normalizesEsAndMesAndAllowsIncreasedExposure() {
        var candidate = assertSingle(detector.detect(List.of(loss(1, 1, "A", "ES", "LONG", "10:00:05", "10:04:05", "-100", 1),
                trade(2, 2, "B", "MES", "LONG", "10:08:05", "10:12:05", "-100", 15)), null));
        assertEquals("S&P", candidate.later().family());
        assertEquals(0, new BigDecimal("10").compareTo(candidate.loss().exposure()));
        assertEquals(0, new BigDecimal("15").compareTo(candidate.later().exposure()));
        assertEquals(0, new BigDecimal("5").compareTo(candidate.exposureDifference()));
    }

    @Test void rejectsOutsideWindowDifferentFamilyDirectionAndLowerExposure() {
        Trade loss = loss(1, 1, "A", "NQ", "LONG", "10:00:05", "10:04:05", "-100", 2);
        assertTrue(detector.detect(List.of(loss, trade(2, 2, "B", "MNQ", "LONG", "10:24:06", "10:30:06", "-100", 20)), null).isEmpty());
        assertTrue(detector.detect(List.of(loss, trade(2, 2, "B", "ES", "LONG", "10:08:05", "10:12:05", "-100", 2)), null).isEmpty());
        assertTrue(detector.detect(List.of(loss, trade(2, 2, "B", "MNQ", "SHORT", "10:08:05", "10:12:05", "-100", 20)), null).isEmpty());
        assertTrue(detector.detect(List.of(loss, trade(2, 2, "B", "MNQ", "LONG", "10:08:05", "10:12:05", "-100", 19)), null).isEmpty());
    }

    @Test void rejectsLaterAttemptAlreadyOpenWhenLossClosed() {
        assertTrue(detector.detect(List.of(loss(1, 1, "A", "NQ", "LONG", "10:00:05", "10:10:05", "-100", 1),
                trade(2, 2, "B", "MNQ", "LONG", "10:05:05", "10:15:05", "-100", 10)), null).isEmpty());
    }

    @Test void rejectsMissingOrMinuteOnlyTimestampPrecision() {
        Trade missing = loss(1, 1, "A", "NQ", "LONG", "10:00:05", "10:04:05", "-100", 1); missing.setExitTime(null);
        assertTrue(detector.detect(List.of(missing, trade(2, 2, "B", "MNQ", "LONG", "10:08:05", "10:12:05", "-100", 10)), null).isEmpty());
        assertTrue(detector.detect(List.of(loss(1, 1, "A", "NQ", "LONG", "10:00:00", "10:04:00", "-100", 1),
                trade(2, 2, "B", "MNQ", "LONG", "10:08:00", "10:12:00", "-100", 10)), null).isEmpty());
    }

    @Test void clustersSynchronizedLossesAndThreeLaterAccountsIntoOneFinding() {
        List<CrossAccountContinuationDetector.Candidate> candidates = detector.detect(List.of(
                loss(1, 1, "A", "NQ", "SHORT", "10:00:05", "10:04:05", "-100", 1),
                loss(2, 2, "B", "MNQ", "SHORT", "10:00:06", "10:04:06", "-100", 10),
                trade(3, 3, "C", "MNQ", "SHORT", "10:08:05", "10:12:05", "-100", 10),
                trade(4, 4, "D", "MNQ", "SHORT", "10:08:06", "10:12:06", "-100", 10),
                trade(5, 5, "E", "MNQ", "SHORT", "10:08:07", "10:12:07", "-100", 10)), null);
        var candidate = assertSingle(candidates);
        assertEquals(2, candidate.loss().attempts().size());
        assertEquals(3, candidate.later().attempts().size());
        assertTrue(candidate.limitations().stream().anyMatch(value -> value.contains("no copier relationship is inferred")));
    }

    @Test void filtersEvidenceToParticipatingAccountAndKeepsAllAccountsViewCombined() {
        List<Trade> trades = List.of(loss(1, 1, "A", "NQ", "LONG", "10:00:05", "10:04:05", "-100", 1),
                trade(2, 2, "B", "MNQ", "LONG", "10:08:05", "10:12:05", "-100", 10));
        assertEquals(1, detector.detect(trades, 1L).size());
        assertEquals(1, detector.detect(trades, 2L).size());
        assertTrue(detector.detect(trades, 99L).isEmpty());
        assertEquals(1, detector.detect(trades, null).size());
    }

    @Test void completeImportCoverageProducesStrongEvidenceAndOrderingIsStable() {
        Trade loss = loss(1, 1, "A", "NQ", "LONG", "10:00:05", "10:04:05", "-100", 1);
        Trade later = trade(2, 2, "B", "MNQ", "LONG", "10:08:05", "10:12:05", "90", 10);
        ImportBatch batch = ImportBatch.builder().totalRows(2).importedRows(2).build(); loss.setImportBatch(batch); later.setImportBatch(batch);
        var first = assertSingle(detector.detect(List.of(later, loss), null));
        var second = assertSingle(detector.detect(List.of(loss, later), null));
        assertEquals("STRONG", first.confidence());
        assertEquals(first.loss().stableId(), second.loss().stableId());
        assertEquals(0, new BigDecimal("90").compareTo(first.later().pnl()));
    }

    @Test void rejectsSameFamilySequenceWhenTradeOwnershipDiffers() {
        Trade loss = loss(1, 1, "A", "NQ", "LONG", "10:00:05", "10:04:05", "-100", 1);
        Trade later = trade(2, 2, "B", "MNQ", "LONG", "10:08:05", "10:12:05", "-100", 10);
        User anotherUser = new User(); anotherUser.setId(2L); later.setUser(anotherUser);
        assertTrue(detector.detect(List.of(loss, later), null).isEmpty());
    }

    private CrossAccountContinuationDetector.Candidate assertSingle(List<CrossAccountContinuationDetector.Candidate> candidates) {
        assertEquals(1, candidates.size()); return candidates.get(0);
    }

    private Trade loss(long id, long accountId, String accountName, String symbol, String side, String entry, String exit, String pnl, int quantity) {
        return trade(id, accountId, accountName, symbol, side, entry, exit, pnl, quantity);
    }

    private Trade trade(long id, long accountId, String accountName, String symbol, String side, String entry, String exit, String pnl, int quantity) {
        TradingAccount account = new TradingAccount(); account.setId(accountId); account.setName(accountName);
        User user = new User(); user.setId(1L);
        return Trade.builder().id(id).user(user).account(account).symbol(symbol).market("FUTURES").side(side).quantity(quantity)
                .pnlAmount(new BigDecimal(pnl)).tradeDate(LocalDate.of(2026, 10, 1)).entryTime(time(entry)).exitTime(time(exit))
                .session(SessionType.NY_AM).outcome(new BigDecimal(pnl).signum() < 0 ? OutcomeType.LOSS : OutcomeType.WIN).build();
    }

    private LocalDateTime time(String value) { return LocalDateTime.parse("2026-10-01T" + value); }
}
