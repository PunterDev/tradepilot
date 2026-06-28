package com.tradepilot.service;

import com.tradepilot.coach.entity.CoachSnapshotType;
import com.tradepilot.coach.repository.AiCoachSnapshotRepository;
import com.tradepilot.dto.admin.AdminTradeCleanupResponse;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.leakdetector.repository.AiLeakDetectorSnapshotRepository;
import com.tradepilot.repository.AiSessionReviewRepository;
import com.tradepilot.repository.AiTradeAnalysisRepository;
import com.tradepilot.repository.AiTradeReviewRepository;
import com.tradepilot.repository.DailyDisciplineScoreRepository;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradeRuleViolationRepository;
import com.tradepilot.repository.TradingAccountRepository;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.tradingdna.repository.TradingDnaSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminTradeCleanupService {
    private final UserRepository userRepository;
    private final TradingAccountRepository tradingAccountRepository;
    private final TradeRepository tradeRepository;
    private final AiTradeReviewRepository aiTradeReviewRepository;
    private final AiTradeAnalysisRepository aiTradeAnalysisRepository;
    private final TradeRuleViolationRepository tradeRuleViolationRepository;
    private final DailyDisciplineScoreRepository dailyDisciplineScoreRepository;
    private final AiSessionReviewRepository aiSessionReviewRepository;
    private final AiLeakDetectorSnapshotRepository leakDetectorSnapshotRepository;
    private final TradingDnaSnapshotRepository tradingDnaSnapshotRepository;
    private final AiCoachSnapshotRepository coachSnapshotRepository;
    private final PerformanceCacheInvalidationService cacheInvalidationService;

    @Transactional
    public AdminTradeCleanupResponse cleanupRange(
            User admin,
            Long userId,
            Long accountId,
            LocalDate startDate,
            LocalDate endDate,
            boolean dryRun
    ) {
        if (admin == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (userId == null || accountId == null || startDate == null || endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId, accountId, startDate, and endDate are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate cannot be before startDate");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found"));
        TradingAccount account = tradingAccountRepository.findByIdAndUser_Id(accountId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found for target user"));

        List<Trade> trades = tradeRepository.findByUser_IdAndAccount_IdAndTradeDateBetweenOrderByTradeDateAscEntryTimeAscIdAsc(
                targetUser.getId(),
                account.getId(),
                startDate,
                endDate
        );
        List<Long> tradeIds = trades.stream().map(Trade::getId).toList();

        Counts counts = countAffectedRows(targetUser.getId(), account.getId(), startDate, endDate, tradeIds);
        long sessionCacheCount = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        if (!dryRun && !tradeIds.isEmpty()) {
            deleteAffectedRows(targetUser.getId(), account.getId(), startDate, endDate, tradeIds);
            tradeRepository.deleteAllInBatch(trades);
            cacheInvalidationService.invalidateUserTradeDerivedCaches(targetUser.getId());
        } else if (!dryRun) {
            cacheInvalidationService.invalidateUserTradeDerivedCaches(targetUser.getId());
        }

        log.warn("Admin trade cleanup completed. adminUserId={}, targetUserId={}, accountId={}, startDate={}, endDate={}, dryRun={}, tradesMatched={}, tradesDeleted={}",
                admin.getId(), targetUser.getId(), account.getId(), startDate, endDate, dryRun, trades.size(), dryRun ? 0 : trades.size());

        AdminTradeCleanupResponse response = new AdminTradeCleanupResponse();
        response.setUserId(targetUser.getId());
        response.setAccountId(account.getId());
        response.setStartDate(startDate);
        response.setEndDate(endDate);
        response.setDryRun(dryRun);
        response.setTradesMatched(trades.size());
        response.setTradesDeleted(dryRun ? 0 : trades.size());
        response.setTradeReviewsDeleted(counts.tradeReviews());
        response.setTradeAnalysesDeleted(counts.tradeAnalyses());
        response.setTradeRuleViolationsDeleted(counts.tradeRuleViolations());
        response.setDailyDisciplineScoresDeleted(counts.dailyDisciplineScores());
        response.setSessionReviewsDeleted(counts.sessionReviews());
        response.setSessionCachesInvalidated(dryRun ? 0 : sessionCacheCount);
        response.setLeakSnapshotsDeleted(counts.leakSnapshots());
        response.setTradingDnaSnapshotsDeleted(counts.tradingDnaSnapshots());
        response.setDailyCoachSnapshotsDeleted(counts.dailyCoachSnapshots());
        response.setWeeklyCoachSnapshotsDeleted(counts.weeklyCoachSnapshots());
        return response;
    }

    private Counts countAffectedRows(Long userId, Long accountId, LocalDate startDate, LocalDate endDate, List<Long> tradeIds) {
        long tradeReviews = tradeIds.isEmpty() ? 0 : aiTradeReviewRepository.countByUserIdAndTradeIdIn(userId, tradeIds);
        long tradeAnalyses = tradeIds.isEmpty() ? 0 : aiTradeAnalysisRepository.countByUserIdAndTradeIdIn(userId, tradeIds);
        long tradeRuleViolations = tradeIds.isEmpty() ? 0 : tradeRuleViolationRepository.countByUserIdAndTradeIdIn(userId, tradeIds);
        long dailyScores = dailyDisciplineScoreRepository.countByUserIdAndAccountIdAndTradeDateBetween(userId, accountId, startDate, endDate);
        long sessionReviews = aiSessionReviewRepository.countByUserIdAndAccountIdAndSessionDateBetween(userId, accountId, startDate, endDate);
        long leakSnapshots = leakDetectorSnapshotRepository.countByUserIdAndAccountId(userId, accountId);
        long tradingDnaSnapshots = tradingDnaSnapshotRepository.countAffectedByAccountHistory(userId, accountId);
        long dailyCoachSnapshots = coachSnapshotRepository.countByUserIdAndAccountIdAndSnapshotTypeAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
                userId,
                accountId,
                CoachSnapshotType.DAILY,
                endDate,
                startDate
        );
        long weeklyCoachSnapshots = coachSnapshotRepository.countByUserIdAndAccountIdAndSnapshotTypeAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
                userId,
                accountId,
                CoachSnapshotType.WEEKLY,
                endDate,
                startDate
        );
        return new Counts(tradeReviews, tradeAnalyses, tradeRuleViolations, dailyScores, sessionReviews, leakSnapshots, tradingDnaSnapshots, dailyCoachSnapshots, weeklyCoachSnapshots);
    }

    private void deleteAffectedRows(Long userId, Long accountId, LocalDate startDate, LocalDate endDate, List<Long> tradeIds) {
        if (!tradeIds.isEmpty()) {
            aiTradeReviewRepository.deleteByUserIdAndTradeIdIn(userId, tradeIds);
            aiTradeAnalysisRepository.deleteByUserIdAndTradeIdIn(userId, tradeIds);
            tradeRuleViolationRepository.deleteByUserIdAndTradeIdIn(userId, tradeIds);
        }
        dailyDisciplineScoreRepository.deleteByUserIdAndAccountIdAndTradeDateBetween(userId, accountId, startDate, endDate);
        aiSessionReviewRepository.deleteByUserIdAndAccountIdAndSessionDateBetween(userId, accountId, startDate, endDate);
        leakDetectorSnapshotRepository.deleteByUserIdAndAccountId(userId, accountId);
        tradingDnaSnapshotRepository.deleteAffectedByAccountHistory(userId, accountId);
        coachSnapshotRepository.deleteByUserIdAndAccountIdAndSnapshotTypeAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
                userId,
                accountId,
                CoachSnapshotType.DAILY,
                endDate,
                startDate
        );
        coachSnapshotRepository.deleteByUserIdAndAccountIdAndSnapshotTypeAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
                userId,
                accountId,
                CoachSnapshotType.WEEKLY,
                endDate,
                startDate
        );
    }

    private record Counts(
            long tradeReviews,
            long tradeAnalyses,
            long tradeRuleViolations,
            long dailyDisciplineScores,
            long sessionReviews,
            long leakSnapshots,
            long tradingDnaSnapshots,
            long dailyCoachSnapshots,
            long weeklyCoachSnapshots
    ) {
    }
}
