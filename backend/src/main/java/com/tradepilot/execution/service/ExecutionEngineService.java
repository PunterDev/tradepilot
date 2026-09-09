package com.tradepilot.execution.service;

import com.tradepilot.billing.Feature;
import com.tradepilot.billing.FeatureAccessService;
import com.tradepilot.config.FeatureFlagService;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradeRuleViolation;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.execution.config.ExecutionEngineProperties;
import com.tradepilot.execution.dto.*;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradeRuleViolationRepository;
import com.tradepilot.repository.TradingAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExecutionEngineService {
    private static final Logger log = LoggerFactory.getLogger(ExecutionEngineService.class);
    private static final int SETUP_POINTS = 30;
    private static final int RISK_POINTS = 25;
    private static final int PATIENCE_POINTS = 20;
    private static final int MANAGEMENT_POINTS = 15;
    private static final int SESSION_POINTS = 10;

    private final ExecutionEngineProperties properties;
    private final FeatureFlagService featureFlagService;
    private final FeatureAccessService featureAccessService;
    private final TradeRepository tradeRepository;
    private final TradeRuleViolationRepository violationRepository;
    private final TradingAccountRepository accountRepository;
    private final PreAnalysisExecutionComparisonService preAnalysisComparisonService;

    public ExecutionEngineService(ExecutionEngineProperties properties, FeatureFlagService featureFlagService, FeatureAccessService featureAccessService, TradeRepository tradeRepository, TradeRuleViolationRepository violationRepository, TradingAccountRepository accountRepository, PreAnalysisExecutionComparisonService preAnalysisComparisonService) {
        this.properties = properties;
        this.featureFlagService = featureFlagService;
        this.featureAccessService = featureAccessService;
        this.tradeRepository = tradeRepository;
        this.violationRepository = violationRepository;
        this.accountRepository = accountRepository;
        this.preAnalysisComparisonService = preAnalysisComparisonService;
    }

    @Transactional(readOnly = true)
    public ExecutionScoreResponse tradeExecution(User user, Long tradeId) {
        requireExecutionAccess(user);
        Trade trade = tradeRepository.findByIdAndUser_Id(tradeId, user.getId()).orElseThrow(() -> new ResourceNotFoundException("Trade not found"));
        List<TradeRuleViolation> violations = violationRepository.findByUserIdAndTradeIdOrderByCreatedAtDesc(user.getId(), tradeId);
        return scoreTrade(trade, violations, preAnalysisComparisonService.compareTrade(user, trade));
    }

    @Transactional(readOnly = true)
    public ExecutionSessionSummaryResponse session(User user, Long accountId, LocalDate date) {
        requireExecutionAccess(user);
        requireAccount(user, accountId);
        List<Trade> trades = tradeRepository.findByUserAndAccount_IdAndTradeDateOrderByEntryTimeAscIdAsc(user, accountId, date);
        List<TradeRuleViolation> violations = violationRepository.findByUserIdAndTradeDateOrderByCreatedAtDesc(user.getId(), date).stream()
                .filter(v -> trades.stream().anyMatch(t -> Objects.equals(t.getId(), v.getTradeId())))
                .toList();
        return sessionSummary(user, accountId, date, trades, groupViolations(violations), loadAccountTrades(user, accountId));
    }

    @Transactional(readOnly = true)
    public ExecutionAccountSummaryResponse accountSummary(User user, Long accountId) {
        requireExecutionAccess(user);
        requireAccount(user, accountId);
        List<Trade> trades = loadAccountTrades(user, accountId);
        Map<Long, List<TradeRuleViolation>> violations = loadViolations(user.getId(), accountId, trades);
        List<ExecutionScoreResponse> scores = trades.stream().map(t -> scoreTrade(t, violations.getOrDefault(t.getId(), List.of()))).toList();
        long cleanWins = count(scores, ExecutionClassification.CLEAN_WIN);
        long cleanLosses = count(scores, ExecutionClassification.CLEAN_LOSS);
        long undisciplinedWins = count(scores, ExecutionClassification.UNDISCIPLINED_WIN);
        long undisciplinedLosses = count(scores, ExecutionClassification.UNDISCIPLINED_LOSS);
        BigDecimal ruleAdherence = ruleAdherence(scores);
        BigDecimal avoidableImpactR = avoidableImpactR(trades, scores);
        List<BehaviorPatternResponse> patterns = behaviorPatterns(trades, violations);
        WeeklyExecutionFocusResponse weeklyFocus = weeklyFocus(patterns, scores);
        Integer averageScore = averageScore(scores);
        Integer coverage = averageCoverage(scores);
        return new ExecutionAccountSummaryResponse(accountId, averageScore, coverage, confidence(coverage), ruleAdherence, avoidableImpactR, cleanWins, cleanLosses, undisciplinedWins, undisciplinedLosses, weeklyFocus, patterns);
    }

    @Transactional(readOnly = true)
    public List<LossResponseResponse> lossResponses(User user, Long accountId) {
        requireExecutionAccess(user);
        requireAccount(user, accountId);
        List<Trade> trades = loadAccountTrades(user, accountId);
        Map<Long, List<TradeRuleViolation>> violations = loadViolations(user.getId(), accountId, trades);
        return trades.stream()
                .filter(this::isLosing)
                .map(trade -> lossResponse(trade, trades, violations))
                .filter(response -> response.classification() != LossResponseClassification.NOT_EVALUATED)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BehaviorPatternResponse> patterns(User user, Long accountId) {
        requireExecutionAccess(user);
        requireAccount(user, accountId);
        List<Trade> trades = loadAccountTrades(user, accountId);
        return behaviorPatterns(trades, loadViolations(user.getId(), accountId, trades));
    }

    @Transactional(readOnly = true)
    public PressurePointResponse pressurePoint(User user, Long accountId) {
        requireExecutionAccess(user);
        requireAccount(user, accountId);
        List<Trade> trades = loadAccountTrades(user, accountId);
        Map<Long, List<TradeRuleViolation>> violations = loadViolations(user.getId(), accountId, trades);
        return pressurePoint(trades, violations);
    }

    @Transactional(readOnly = true)
    public List<SetupExecutionStatsResponse> setupStats(User user, Long accountId) {
        requireExecutionAccess(user);
        requireAccount(user, accountId);
        List<Trade> trades = loadAccountTrades(user, accountId);
        Map<Long, List<TradeRuleViolation>> violations = loadViolations(user.getId(), accountId, trades);
        return setupStats(trades, violations);
    }

    @Transactional(readOnly = true)
    public WeeklyExecutionFocusResponse weeklyFocus(User user, Long accountId) {
        return accountSummary(user, accountId).weeklyFocus();
    }

    public ExecutionScoreResponse scoreTrade(Trade trade, List<TradeRuleViolation> violations) {
        return scoreTrade(trade, violations, null);
    }

    public ExecutionScoreResponse scoreTrade(Trade trade, List<TradeRuleViolation> violations, PreAnalysisComparisonResponse comparison) {
        Map<String, ExecutionScoreComponentResponse> components = new LinkedHashMap<>();
        components.put("setupAdherence", setupComponent(trade, violations));
        components.put("riskAdherence", riskComponent(trade, violations));
        components.put("patience", patienceComponent(trade, violations));
        components.put("tradeManagement", managementComponent(trade, violations));
        components.put("sessionAdherence", sessionComponent(trade, violations));
        int available = components.values().stream().mapToInt(ExecutionScoreComponentResponse::availablePoints).sum();
        int raw = components.values().stream().mapToInt(ExecutionScoreComponentResponse::score).sum();
        Integer normalized = available == 0 ? null : (int) Math.round(raw * 100.0 / available);
        int evidenceCoverage = (int) Math.round(available * 100.0 / 100);
        ExecutionGrade grade = grade(normalized);
        ExecutionClassification classification = classify(trade, normalized);
        List<String> reasons = components.values().stream().flatMap(c -> c.reasons().stream()).toList();
        List<String> detected = violations.stream().map(v -> v.getRuleTitle() == null ? v.getRuleCode() : v.getRuleTitle()).distinct().toList();
        return new ExecutionScoreResponse(trade.getId(), normalized, grade, classification, available, 100, evidenceCoverage, confidence(evidenceCoverage), components, reasons, detected, processMessage(classification), comparison);
    }

    public LossResponseResponse lossResponse(Trade losingTrade, List<Trade> sortedAccountTrades, Map<Long, List<TradeRuleViolation>> violations) {
        Trade next = nextTradeSameSession(losingTrade, sortedAccountTrades);
        if (next == null) {
            return new LossResponseResponse(losingTrade.getId(), null, null, LossResponseClassification.NOT_EVALUATED, null, baselineMinutes(sortedAccountTrades), null, null, null, List.of(), "No later eligible trade in this account session.");
        }
        Long minutes = minutesBetween(losingTrade.getExitTime(), next.getEntryTime());
        Long baseline = baselineMinutes(sortedAccountTrades);
        BigDecimal riskChange = percentChange(riskBasis(losingTrade), riskBasis(next));
        BigDecimal sizeChange = percentChange(number(losingTrade.getQuantity()), number(next.getQuantity()));
        int currentScore = Optional.ofNullable(scoreTrade(losingTrade, violations.getOrDefault(losingTrade.getId(), List.of())).executionScore()).orElse(0);
        int nextScore = Optional.ofNullable(scoreTrade(next, violations.getOrDefault(next.getId(), List.of())).executionScore()).orElse(0);
        List<String> flags = new ArrayList<>();
        if (isFastReentry(minutes, baseline)) flags.add("FAST_REENTRY");
        if (riskChange != null && riskChange.compareTo(properties.getRiskIncreasePercent()) >= 0) flags.add("RISK_INCREASE");
        if (sizeChange != null && sizeChange.compareTo(properties.getSizeIncreasePercent()) >= 0) flags.add("SIZE_INCREASE");
        if (currentScore - nextScore >= properties.getExecutionDropPoints()) flags.add("LOWER_EXECUTION_SCORE");
        if (!violations.getOrDefault(next.getId(), List.of()).isEmpty()) flags.add("NEW_RULE_VIOLATION");
        int score = Math.max(0, 100 - flags.size() * 18);
        LossResponseClassification classification = flags.size() >= 3 ? LossResponseClassification.UNSTABLE
                : flags.size() >= 2 ? LossResponseClassification.ELEVATED_RISK
                : flags.size() == 1 ? LossResponseClassification.STABLE
                : LossResponseClassification.EXCELLENT;
        String message = flags.isEmpty()
                ? "Post-loss behavior stayed stable based on the available timing, risk, size, and rule data."
                : "Post-loss behavior showed measurable process pressure: " + String.join(", ", flags) + ".";
        return new LossResponseResponse(losingTrade.getId(), next.getId(), score, classification, minutes, baseline, riskChange, sizeChange, nextScore, flags, message);
    }

    private ExecutionScoreComponentResponse setupComponent(Trade trade, List<TradeRuleViolation> violations) {
        if (trade.getFollowedPlan() == null && trade.getSetup() == null && trade.getExecutionRating() == null && none(violations, "SETUP", "PLAN", "PROCESS")) {
            return notEvaluated("setupAdherence", "Setup / playbook adherence", SETUP_POINTS, "No setup or playbook adherence evidence was recorded.");
        }
        int penalty = penalty(violations, "SETUP", "PLAN", "PROCESS");
        int score = SETUP_POINTS - penalty;
        if (Boolean.FALSE.equals(trade.getFollowedPlan())) score -= 12;
        if (trade.getSetup() == null) score -= 4;
        score = clamp(score, 0, SETUP_POINTS);
        return evaluated("setupAdherence", "Setup / playbook adherence", score, SETUP_POINTS, reason(score, SETUP_POINTS, "Setup and plan evidence were mostly aligned.", "Setup or playbook adherence needs review."));
    }

    private ExecutionScoreComponentResponse riskComponent(Trade trade, List<TradeRuleViolation> violations) {
        if (trade.getRiskAmount() == null && trade.getPnlR() == null && none(violations, "RISK", "SIZE")) {
            return notEvaluated("riskAdherence", "Risk adherence", RISK_POINTS, "No risk amount, R value, or risk-rule evidence was available.");
        }
        int penalty = penalty(violations, "RISK", "SIZE");
        int score = clamp(RISK_POINTS - penalty, 0, RISK_POINTS);
        return evaluated("riskAdherence", "Risk adherence", score, RISK_POINTS, reason(score, RISK_POINTS, "Risk stayed within recorded process evidence.", "Risk or sizing discipline needs review."));
    }

    private ExecutionScoreComponentResponse patienceComponent(Trade trade, List<TradeRuleViolation> violations) {
        if (trade.getConfidence() == null && trade.getConfidenceRating() == null && trade.getMistakeTag() == null && none(violations, "ENTRY", "PATIENCE")) {
            return notEvaluated("patience", "Patience / entry discipline", PATIENCE_POINTS, "No confidence, entry, or patience evidence was recorded.");
        }
        int confidence = trade.getConfidence() != null ? trade.getConfidence() : trade.getConfidenceRating() == null ? 3 : trade.getConfidenceRating();
        int score = Math.min(PATIENCE_POINTS, Math.max(0, confidence) * 4);
        if (trade.getMistakeTag() != null && ("EARLY_ENTRY".equals(trade.getMistakeTag().name()) || "FOMO".equals(trade.getMistakeTag().name()) || "NO_CONFIRMATION".equals(trade.getMistakeTag().name()))) {
            score -= 8;
        }
        score -= penalty(violations, "ENTRY", "PATIENCE");
        return evaluated("patience", "Patience / entry discipline", clamp(score, 0, PATIENCE_POINTS), PATIENCE_POINTS, reason(score, PATIENCE_POINTS, "Entry patience was supported by the recorded data.", "Entry timing or confirmation discipline needs review."));
    }

    private ExecutionScoreComponentResponse managementComponent(Trade trade, List<TradeRuleViolation> violations) {
        if (trade.getExecutionRating() == null && none(violations, "MANAGEMENT", "EXIT")) {
            return notEvaluated("tradeManagement", "Trade management adherence", MANAGEMENT_POINTS, "No management or exit-rule evidence was available.");
        }
        int rating = trade.getExecutionRating() == null ? 5 : trade.getExecutionRating();
        int score = Math.min(MANAGEMENT_POINTS, Math.max(0, rating) * 3) - penalty(violations, "MANAGEMENT", "EXIT");
        return evaluated("tradeManagement", "Trade management adherence", clamp(score, 0, MANAGEMENT_POINTS), MANAGEMENT_POINTS, reason(score, MANAGEMENT_POINTS, "Trade management matched the available process evidence.", "Trade management or exit discipline needs review."));
    }

    private ExecutionScoreComponentResponse sessionComponent(Trade trade, List<TradeRuleViolation> violations) {
        if ((trade.getSession() == null || trade.getSession() == SessionType.UNKNOWN) && none(violations, "SESSION", "DAILY_LIMIT", "OVERTRADING")) {
            return notEvaluated("sessionAdherence", "Session / rule adherence", SESSION_POINTS, "No reliable session-rule evidence was available.");
        }
        int score = SESSION_POINTS - penalty(violations, "SESSION", "DAILY_LIMIT", "OVERTRADING");
        if (trade.getSession() == SessionType.UNKNOWN) score -= 3;
        return evaluated("sessionAdherence", "Session / rule adherence", clamp(score, 0, SESSION_POINTS), SESSION_POINTS, reason(score, SESSION_POINTS, "Session behavior matched available rules.", "Session or rule adherence needs review."));
    }

    private ExecutionSessionSummaryResponse sessionSummary(User user, Long accountId, LocalDate date, List<Trade> trades, Map<Long, List<TradeRuleViolation>> violations, List<Trade> accountTrades) {
        List<ExecutionScoreResponse> scores = trades.stream().map(t -> scoreTrade(t, violations.getOrDefault(t.getId(), List.of()), preAnalysisComparisonService.compareTrade(user, t))).toList();
        LossResponseResponse lossResponse = trades.stream().filter(this::isLosing).map(t -> lossResponse(t, accountTrades, violations)).filter(r -> r.classification() != LossResponseClassification.NOT_EVALUATED).findFirst().orElse(null);
        ProcessBreakpointResponse breakPoint = processBreakpoint(scores, accountTrades, violations);
        PreAnalysisComparisonResponse comparison = preAnalysisComparisonService.compareSession(user, date, trades);
        NoTradeEvaluationResponse noTradeEvaluation = preAnalysisComparisonService.evaluateNoTrade(user, date, trades);
        Integer coverage = averageCoverage(scores);
        return new ExecutionSessionSummaryResponse(
                accountId,
                date.toString(),
                averageScore(scores),
                coverage,
                confidence(coverage),
                count(scores, ExecutionClassification.CLEAN_WIN),
                count(scores, ExecutionClassification.CLEAN_LOSS),
                count(scores, ExecutionClassification.UNDISCIPLINED_WIN),
                count(scores, ExecutionClassification.UNDISCIPLINED_LOSS),
                ruleAdherence(scores),
                avoidableImpactR(trades, scores),
                lossResponse,
                breakPoint.message(),
                breakPoint,
                comparison,
                noTradeEvaluation,
                scores
        );
    }

    private List<BehaviorPatternResponse> behaviorPatterns(List<Trade> trades, Map<Long, List<TradeRuleViolation>> violations) {
        List<LossResponseResponse> responses = trades.stream().filter(this::isLosing).map(t -> lossResponse(t, trades, violations)).toList();
        List<BehaviorPatternResponse> patterns = new ArrayList<>();
        addPattern(patterns, "LOSS_FAST_REENTRY_LOWER_EXECUTION", "Loss -> Fast re-entry -> Lower execution", responses, r -> r.flags().contains("FAST_REENTRY") && r.flags().contains("LOWER_EXECUTION_SCORE"), trades);
        addPattern(patterns, "LOSS_SIZE_INCREASE_RULE_VIOLATION", "Loss -> Size increase -> Rule violation", responses, r -> r.flags().contains("SIZE_INCREASE") && r.flags().contains("NEW_RULE_VIOLATION"), trades);
        addPattern(patterns, "LOSS_RISK_INCREASE", "Loss -> Risk increase", responses, r -> r.flags().contains("RISK_INCREASE"), trades);
        return patterns.stream().sorted(Comparator.comparing(BehaviorPatternResponse::occurrences).reversed()).toList();
    }

    private void addPattern(List<BehaviorPatternResponse> patterns, String key, String label, List<LossResponseResponse> responses, java.util.function.Predicate<LossResponseResponse> predicate, List<Trade> trades) {
        List<LossResponseResponse> matched = responses.stream().filter(predicate).toList();
        if (matched.size() < properties.getMinimumPatternOccurrences()) return;
        Map<Long, Trade> tradesById = trades.stream().collect(Collectors.toMap(Trade::getId, Function.identity()));
        List<Trade> nextTrades = matched.stream().map(r -> tradesById.get(r.nextTradeId())).filter(Objects::nonNull).toList();
        BigDecimal netR = sumR(nextTrades);
        BigDecimal winRate = percent(nextTrades.stream().filter(this::isProfitable).count(), nextTrades.size());
        BigDecimal avgScore = average(matched.stream().map(LossResponseResponse::nextTradeExecutionScore).filter(Objects::nonNull).map(BigDecimal::valueOf).toList());
        BigDecimal avgMinutes = average(matched.stream().map(LossResponseResponse::timeToNextTradeMinutes).filter(Objects::nonNull).map(BigDecimal::valueOf).toList());
        BigDecimal avgRiskChange = average(matched.stream().map(LossResponseResponse::riskChangePercent).filter(Objects::nonNull).toList());
        patterns.add(new BehaviorPatternResponse(key, label, matched.size(), netR, winRate, avgScore, avgMinutes, avgRiskChange,
                "Historically associated with " + formatR(netR) + " across " + matched.size() + " occurrences."));
    }

    private PressurePointResponse pressurePoint(List<Trade> trades, Map<Long, List<TradeRuleViolation>> violations) {
        if (trades.size() < properties.getMinimumPressurePointSamples()) {
            return new PressurePointResponse(false, null, null, null, null, null, null, trades.size(), "Insufficient sample size for pressure-point analysis.");
        }
        BigDecimal threshold = BigDecimal.valueOf(-1.5);
        List<Integer> before = new ArrayList<>();
        List<Integer> after = new ArrayList<>();
        List<BigDecimal> beforeRisk = new ArrayList<>();
        List<BigDecimal> afterRisk = new ArrayList<>();
        BigDecimal runningR = BigDecimal.ZERO;
        for (Trade trade : trades) {
            Integer score = scoreTrade(trade, violations.getOrDefault(trade.getId(), List.of())).executionScore();
            if (score != null) {
                if (runningR.compareTo(threshold) <= 0) after.add(score); else before.add(score);
            }
            BigDecimal risk = riskBasis(trade);
            if (risk != null) {
                if (runningR.compareTo(threshold) <= 0) afterRisk.add(risk); else beforeRisk.add(risk);
            }
            runningR = runningR.add(rValue(trade));
        }
        if (before.size() < 3 || after.size() < 3) {
            return new PressurePointResponse(false, threshold, avgInt(before), avgInt(after), average(beforeRisk), average(afterRisk), null, trades.size(), "Insufficient before/after sample size for pressure-point analysis.");
        }
        int beforeAvg = avgInt(before);
        int afterAvg = avgInt(after);
        boolean detected = beforeAvg - afterAvg >= properties.getExecutionDropPoints();
        String message = detected
                ? "Execution tends to deteriorate after approximately " + formatR(threshold) + "."
                : "No defensible execution pressure point was detected from the current sample.";
        return new PressurePointResponse(detected, threshold, beforeAvg, afterAvg, average(beforeRisk), average(afterRisk), null, trades.size(), message);
    }

    private List<SetupExecutionStatsResponse> setupStats(List<Trade> trades, Map<Long, List<TradeRuleViolation>> violations) {
        return trades.stream()
                .filter(t -> t.getSetup() != null)
                .collect(Collectors.groupingBy(t -> t.getSetup().name(), LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> setupStat(entry.getKey(), entry.getValue(), violations))
                .sorted(Comparator.comparing(SetupExecutionStatsResponse::samples).reversed())
                .toList();
    }

    private SetupExecutionStatsResponse setupStat(String setup, List<Trade> trades, Map<Long, List<TradeRuleViolation>> violations) {
        List<ExecutionScoreResponse> scores = trades.stream().map(t -> scoreTrade(t, violations.getOrDefault(t.getId(), List.of()))).toList();
        long compliant = scores.stream().filter(s -> s.executionScore() != null && s.executionScore() >= properties.getCleanExecutionThreshold()).count();
        BigDecimal winRate = percent(trades.stream().filter(this::isProfitable).count(), trades.size());
        BigDecimal avgR = average(trades.stream().map(this::rValue).toList());
        BigDecimal avgWin = average(trades.stream().filter(this::isProfitable).map(this::rValue).toList());
        BigDecimal avgLoss = average(trades.stream().filter(this::isLosing).map(this::rValue).toList());
        BigDecimal expectancy = avgR;
        BigDecimal avgScore = average(scores.stream().map(ExecutionScoreResponse::executionScore).filter(Objects::nonNull).map(BigDecimal::valueOf).toList());
        return new SetupExecutionStatsResponse(setup, trades.size(), compliant, trades.size() - compliant, winRate, avgR, avgWin, avgLoss, expectancy, avgScore, trend(scores), sampleConfidence(trades.size()));
    }

    private WeeklyExecutionFocusResponse weeklyFocus(List<BehaviorPatternResponse> patterns, List<ExecutionScoreResponse> scores) {
        if (!patterns.isEmpty()) {
            BehaviorPatternResponse pattern = patterns.get(0);
            return new WeeklyExecutionFocusResponse(pattern.label(), "Detected " + pattern.occurrences() + " recent occurrences.", pattern.netR(), "Pause until normal setup criteria are fully restored before the next trade.", null, pattern.occurrences(), null, averageScore(scores), WeeklyExecutionFocusStatus.NEW, "DEVELOPING");
        }
        long undisciplinedWins = count(scores, ExecutionClassification.UNDISCIPLINED_WIN);
        if (undisciplinedWins > 0) {
            return new WeeklyExecutionFocusResponse("Stop rewarding undisciplined wins", "Profitable outcomes occurred with weak execution evidence.", BigDecimal.ZERO, "Review profitable trades for process quality before increasing risk.", null, undisciplinedWins, null, averageScore(scores), WeeklyExecutionFocusStatus.NEW, "EARLY");
        }
        return new WeeklyExecutionFocusResponse("Keep scoring execution before outcome", "No repeated high-impact execution pattern met the minimum evidence threshold.", BigDecimal.ZERO, "Review whether each trade followed the plan before reviewing P&L.", null, 0L, null, averageScore(scores), WeeklyExecutionFocusStatus.MASTERED, "INSUFFICIENT_DATA");
    }

    private List<Trade> loadAccountTrades(User user, Long accountId) {
        LocalDate end = LocalDate.now().plusDays(1);
        LocalDate start = end.minusDays(properties.getLookbackDays());
        return tradeRepository.findByUser_IdAndAccount_IdAndTradeDateBetweenOrderByTradeDateAscEntryTimeAscIdAsc(user.getId(), accountId, start, end);
    }

    private Map<Long, List<TradeRuleViolation>> loadViolations(Long userId, Long accountId, List<Trade> trades) {
        if (trades.isEmpty()) return Map.of();
        LocalDate start = trades.stream().map(Trade::getTradeDate).min(LocalDate::compareTo).orElse(LocalDate.now());
        LocalDate end = trades.stream().map(Trade::getTradeDate).max(LocalDate::compareTo).orElse(LocalDate.now());
        return groupViolations(violationRepository.findForTradingDna(userId, accountId, start, end));
    }

    private Map<Long, List<TradeRuleViolation>> groupViolations(List<TradeRuleViolation> violations) {
        return violations.stream().collect(Collectors.groupingBy(TradeRuleViolation::getTradeId));
    }

    private void requireExecutionAccess(User user) {
        featureFlagService.requireEnabled(Feature.PERFORMANCE_TIMELINE);
        featureAccessService.requireFeature(user, Feature.PERFORMANCE_TIMELINE);
    }

    private TradingAccount requireAccount(User user, Long accountId) {
        return accountRepository.findByIdAndUser_Id(accountId, user.getId()).orElseThrow(() -> new ResourceNotFoundException("Trading account not found"));
    }

    private ExecutionScoreComponentResponse evaluated(String key, String label, int score, int maxScore, String reason) {
        return new ExecutionScoreComponentResponse(key, label, score, maxScore, true, maxScore, List.of(reason));
    }

    private ExecutionScoreComponentResponse notEvaluated(String key, String label, int maxScore, String reason) {
        return new ExecutionScoreComponentResponse(key, label, 0, maxScore, false, 0, List.of(reason));
    }

    private boolean none(List<TradeRuleViolation> violations, String... categories) {
        return violations.stream().noneMatch(v -> matches(v, categories));
    }

    private int penalty(List<TradeRuleViolation> violations, String... categories) {
        return violations.stream().filter(v -> matches(v, categories)).mapToInt(this::severityPenalty).sum();
    }

    private boolean matches(TradeRuleViolation violation, String... categories) {
        String haystack = ((violation.getCategory() == null ? "" : violation.getCategory()) + " " + (violation.getRuleCode() == null ? "" : violation.getRuleCode()) + " " + (violation.getRuleTitle() == null ? "" : violation.getRuleTitle())).toUpperCase(Locale.ROOT);
        return Arrays.stream(categories).anyMatch(haystack::contains);
    }

    private int severityPenalty(TradeRuleViolation violation) {
        String severity = violation.getSeverity() == null ? "" : violation.getSeverity().toUpperCase(Locale.ROOT);
        if (severity.contains("HIGH") || severity.contains("CRITICAL")) return 12;
        if (severity.contains("MEDIUM")) return 8;
        return 5;
    }

    private ExecutionGrade grade(Integer score) {
        if (score == null) return ExecutionGrade.INSUFFICIENT_DATA;
        if (score >= 90) return ExecutionGrade.EXCELLENT;
        if (score >= properties.getCleanExecutionThreshold()) return ExecutionGrade.GOOD;
        if (score >= 55) return ExecutionGrade.NEEDS_WORK;
        return ExecutionGrade.POOR;
    }

    private ExecutionClassification classify(Trade trade, Integer score) {
        if (score == null || (!isProfitable(trade) && !isLosing(trade))) return ExecutionClassification.NOT_CLASSIFIED;
        boolean clean = score >= properties.getCleanExecutionThreshold();
        if (isProfitable(trade)) return clean ? ExecutionClassification.CLEAN_WIN : ExecutionClassification.UNDISCIPLINED_WIN;
        return clean ? ExecutionClassification.CLEAN_LOSS : ExecutionClassification.UNDISCIPLINED_LOSS;
    }

    private String processMessage(ExecutionClassification classification) {
        return switch (classification) {
            case CLEAN_WIN -> "Profitable outcome with strong process execution.";
            case CLEAN_LOSS -> "You followed your process. This was a normal losing outcome.";
            case UNDISCIPLINED_WIN -> "The outcome was profitable, but the execution violated your process.";
            case UNDISCIPLINED_LOSS -> "The losing outcome also showed process issues worth reviewing.";
            case NOT_CLASSIFIED -> "Insufficient outcome or process evidence to classify this trade.";
        };
    }

    private String reason(int score, int max, String good, String weak) {
        return score >= Math.round(max * 0.75) ? good : weak;
    }

    private boolean isProfitable(Trade trade) {
        if (trade.getPnlR() != null) return trade.getPnlR().compareTo(BigDecimal.ZERO) > 0;
        if (trade.getPnlAmount() != null) return trade.getPnlAmount().compareTo(BigDecimal.ZERO) > 0;
        return trade.getOutcome() == OutcomeType.WIN;
    }

    private boolean isLosing(Trade trade) {
        if (trade.getPnlR() != null) return trade.getPnlR().compareTo(BigDecimal.ZERO) < 0;
        if (trade.getPnlAmount() != null) return trade.getPnlAmount().compareTo(BigDecimal.ZERO) < 0;
        return trade.getOutcome() == OutcomeType.LOSS;
    }

    private Trade nextTradeSameSession(Trade trade, List<Trade> trades) {
        return trades.stream()
                .filter(candidate -> !Objects.equals(candidate.getId(), trade.getId()))
                .filter(candidate -> Objects.equals(candidate.getTradeDate(), trade.getTradeDate()))
                .filter(candidate -> candidate.getAccount() != null && trade.getAccount() != null && Objects.equals(candidate.getAccount().getId(), trade.getAccount().getId()))
                .filter(candidate -> trade.getEntryTime() == null || candidate.getEntryTime() == null || candidate.getEntryTime().isAfter(trade.getEntryTime()))
                .min(Comparator.comparing(t -> Optional.ofNullable(t.getEntryTime()).orElse(t.getTradeDate().atStartOfDay())))
                .orElse(null);
    }

    private Long baselineMinutes(List<Trade> trades) {
        List<Long> gaps = new ArrayList<>();
        for (int i = 1; i < trades.size(); i++) {
            Trade previous = trades.get(i - 1);
            Trade current = trades.get(i);
            if (!Objects.equals(previous.getTradeDate(), current.getTradeDate())) continue;
            Long gap = minutesBetween(previous.getExitTime(), current.getEntryTime());
            if (gap != null && gap >= 0) gaps.add(gap);
        }
        if (gaps.isEmpty()) return null;
        return Math.round(gaps.stream().mapToLong(Long::longValue).average().orElse(0));
    }

    private boolean isFastReentry(Long minutes, Long baseline) {
        if (minutes == null) return false;
        if (minutes <= properties.getFastReentryMinutes()) return true;
        if (baseline == null || baseline <= 0) return false;
        BigDecimal ratio = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(baseline), 4, RoundingMode.HALF_UP);
        return ratio.compareTo(properties.getFastReentryBaselineRatio()) <= 0;
    }

    private Long minutesBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) return null;
        return Duration.between(from, to).toMinutes();
    }

    private BigDecimal riskBasis(Trade trade) {
        if (trade.getRiskAmount() != null && trade.getRiskAmount().compareTo(BigDecimal.ZERO) > 0) return trade.getRiskAmount();
        return number(trade.getQuantity());
    }

    private BigDecimal number(Integer value) {
        return value == null ? null : BigDecimal.valueOf(value.longValue());
    }

    private BigDecimal percentChange(BigDecimal before, BigDecimal after) {
        if (before == null || after == null || before.compareTo(BigDecimal.ZERO) == 0) return null;
        return after.subtract(before).multiply(BigDecimal.valueOf(100)).divide(before.abs(), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal rValue(Trade trade) {
        if (trade.getPnlR() != null) return trade.getPnlR();
        if (trade.getPnlAmount() != null && trade.getRiskAmount() != null && trade.getRiskAmount().compareTo(BigDecimal.ZERO) > 0) {
            return trade.getPnlAmount().divide(trade.getRiskAmount(), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal sumR(List<Trade> trades) {
        return trades.stream().map(this::rValue).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> clean = values.stream().filter(Objects::nonNull).toList();
        if (clean.isEmpty()) return null;
        return clean.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(clean.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(long numerator, long denominator) {
        if (denominator == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private Integer averageScore(List<ExecutionScoreResponse> scores) {
        List<Integer> available = scores.stream().map(ExecutionScoreResponse::executionScore).filter(Objects::nonNull).toList();
        if (available.isEmpty()) return null;
        return (int) Math.round(available.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private Integer averageCoverage(List<ExecutionScoreResponse> scores) {
        if (scores.isEmpty()) return null;
        return (int) Math.round(scores.stream().mapToInt(ExecutionScoreResponse::evidenceCoveragePercent).average().orElse(0));
    }

    private EvidenceConfidence confidence(Integer evidenceCoveragePercent) {
        if (evidenceCoveragePercent == null) return EvidenceConfidence.VERY_LOW;
        return confidence(evidenceCoveragePercent.intValue());
    }

    private EvidenceConfidence confidence(int evidenceCoveragePercent) {
        if (evidenceCoveragePercent >= properties.getHighEvidenceCoverageThreshold()) return EvidenceConfidence.HIGH;
        if (evidenceCoveragePercent >= properties.getModerateEvidenceCoverageThreshold()) return EvidenceConfidence.MODERATE;
        if (evidenceCoveragePercent >= properties.getLowEvidenceCoverageThreshold()) return EvidenceConfidence.LOW;
        return EvidenceConfidence.VERY_LOW;
    }

    private int avgInt(List<Integer> values) {
        if (values.isEmpty()) return 0;
        return (int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private long count(List<ExecutionScoreResponse> scores, ExecutionClassification classification) {
        return scores.stream().filter(s -> s.classification() == classification).count();
    }

    private BigDecimal ruleAdherence(List<ExecutionScoreResponse> scores) {
        long classified = scores.stream().filter(s -> s.classification() != ExecutionClassification.NOT_CLASSIFIED).count();
        long clean = scores.stream().filter(s -> s.classification() == ExecutionClassification.CLEAN_WIN || s.classification() == ExecutionClassification.CLEAN_LOSS).count();
        return percent(clean, classified);
    }

    private BigDecimal avoidableImpactR(List<Trade> trades, List<ExecutionScoreResponse> scores) {
        Map<Long, ExecutionScoreResponse> byId = scores.stream().collect(Collectors.toMap(ExecutionScoreResponse::tradeId, Function.identity()));
        return trades.stream()
                .filter(t -> {
                    ExecutionScoreResponse score = byId.get(t.getId());
                    return score != null && (score.classification() == ExecutionClassification.UNDISCIPLINED_LOSS || score.classification() == ExecutionClassification.UNDISCIPLINED_WIN);
                })
                .map(this::rValue)
                .filter(r -> r.compareTo(BigDecimal.ZERO) < 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private ProcessBreakpointResponse processBreakpoint(List<ExecutionScoreResponse> scores, List<Trade> accountTrades, Map<Long, List<TradeRuleViolation>> violations) {
        if (scores.size() < 2) {
            return new ProcessBreakpointResponse(ProcessBreakpointStatus.INSUFFICIENT_EVIDENCE, null, null, null, baselineMinutes(accountTrades), null, null, "Insufficient trade sequence evidence for a process-breakpoint.");
        }
        for (int i = 1; i < scores.size(); i++) {
            Integer previous = scores.get(i - 1).executionScore();
            Integer current = scores.get(i).executionScore();
            if (previous != null && current != null && previous - current >= properties.getExecutionDropPoints()) {
                ExecutionScoreResponse previousScore = scores.get(i - 1);
                ExecutionScoreResponse currentScore = scores.get(i);
                Trade previousTrade = accountTrades.stream().filter(t -> Objects.equals(t.getId(), previousScore.tradeId())).findFirst().orElse(null);
                Trade currentTrade = accountTrades.stream().filter(t -> Objects.equals(t.getId(), currentScore.tradeId())).findFirst().orElse(null);
                Long minutes = previousTrade == null || currentTrade == null ? null : minutesBetween(previousTrade.getExitTime(), currentTrade.getEntryTime());
                Long baseline = baselineMinutes(accountTrades);
                String message = "Execution deteriorated after Trade #" + i + ".";
                if (minutes != null || baseline != null) {
                    message += " Time to next trade was " + (minutes == null ? "unknown" : minutes + " min") + " versus a " + (baseline == null ? "unknown" : baseline + " min") + " baseline.";
                }
                return new ProcessBreakpointResponse(ProcessBreakpointStatus.DETECTED, previousScore.tradeId(), currentScore.tradeId(), minutes, baseline, previous, current, message);
            }
        }
        return new ProcessBreakpointResponse(ProcessBreakpointStatus.NOT_DETECTED, null, null, null, baselineMinutes(accountTrades), null, null, "No clear process-break point detected.");
    }

    private String trend(List<ExecutionScoreResponse> scores) {
        if (scores.size() < 4) return "INSUFFICIENT_DATA";
        int midpoint = scores.size() / 2;
        Integer first = averageScore(scores.subList(0, midpoint));
        Integer second = averageScore(scores.subList(midpoint, scores.size()));
        if (first == null || second == null) return "INSUFFICIENT_DATA";
        if (second - first >= 5) return "IMPROVING";
        if (first - second >= 5) return "DETERIORATING";
        return "STABLE";
    }

    private SampleConfidenceLabel sampleConfidence(long samples) {
        if (samples < 10) return SampleConfidenceLabel.INSUFFICIENT_DATA;
        if (samples < 25) return SampleConfidenceLabel.EARLY;
        if (samples < 60) return SampleConfidenceLabel.DEVELOPING;
        return SampleConfidenceLabel.ESTABLISHED;
    }

    private String formatR(BigDecimal value) {
        return value == null ? "0R" : value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "R";
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
