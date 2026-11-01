package com.tradepilot.survival;

import com.tradepilot.entity.*;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import com.tradepilot.service.TradingAccountService;
import com.tradepilot.survival.dto.*;
import com.tradepilot.survival.entity.*;
import com.tradepilot.survival.repository.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountSurvivalService {
    private static final Logger log = LoggerFactory.getLogger(AccountSurvivalService.class);
    public static final String CALCULATION_VERSION = "account-survival-v1";
    private static final Duration RAPID_REENTRY_WINDOW = Duration.ofMinutes(10);
    private static final int HISTORY_WINDOW_SESSIONS = 30;
    private final TradingAccountService accountService;
    private final TradingAccountRepository tradingAccountRepository;
    private final TradeRepository tradeRepository;
    private final AccountRiskProfileRepository profileRepository;
    private final NextSessionProtectionCommitmentRepository commitmentRepository;
    private final NextSessionProtectionEvaluationRepository evaluationRepository;
    private final NextSessionProtectionRuleResultRepository ruleResultRepository;
    private final ProtectionCommitmentEvaluator commitmentEvaluator;
    private final CrossAccountContinuationDetector crossAccountContinuationDetector;

    public AccountSurvivalService(TradingAccountService accountService, TradingAccountRepository tradingAccountRepository, TradeRepository tradeRepository,
            AccountRiskProfileRepository profileRepository, NextSessionProtectionCommitmentRepository commitmentRepository, NextSessionProtectionEvaluationRepository evaluationRepository, NextSessionProtectionRuleResultRepository ruleResultRepository, ProtectionCommitmentEvaluator commitmentEvaluator, CrossAccountContinuationDetector crossAccountContinuationDetector) {
        this.accountService = accountService; this.tradingAccountRepository = tradingAccountRepository; this.tradeRepository = tradeRepository;
        this.profileRepository = profileRepository; this.commitmentRepository = commitmentRepository; this.evaluationRepository=evaluationRepository; this.ruleResultRepository=ruleResultRepository; this.commitmentEvaluator=commitmentEvaluator; this.crossAccountContinuationDetector=crossAccountContinuationDetector;
    }

    @Transactional(readOnly = true)
    public AccountRiskProfileResponse profile(User user, Long accountId) {
        accountService.getAccountForImport(user, accountId);
        return profileRepository.findByAccount_IdAndUser(accountId, user).map(this::profileResponse).orElse(null);
    }

    @Transactional
    public AccountRiskProfileResponse saveProfile(User user, Long accountId, AccountRiskProfileRequest request) {
        TradingAccount account = accountService.getAccountForImport(user, accountId);
        validateProfile(request);
        AccountRiskProfile profile = profileRepository.findByAccount_IdAndUser(accountId, user).orElseGet(AccountRiskProfile::new);
        profile.setUser(user); profile.setAccount(account); profile.setCurrentBalance(request.currentBalance());
        profile.setFailureThreshold(request.failureThreshold()); profile.setDailyLossLimit(request.dailyLossLimit());
        profile.setPlannedRiskPerTrade(request.plannedRiskPerTrade()); profile.setPersonalMaxContracts(request.personalMaxContracts());
        profile.setFirmMaxContracts(request.firmMaxContracts()); profile.setPrimaryWindowStart(request.primaryWindowStart());
        profile.setPrimaryWindowEnd(request.primaryWindowEnd()); profile.setRenewalDate(request.renewalDate()); profile.setPayoutTarget(request.payoutTarget());
        profile.setPayoutDate(request.payoutDate()); profile.setTimezone(blankToNull(request.timezone())); profile.setAccountStage(request.accountStage());
        profile.setEffectiveDate(request.effectiveDate() == null ? LocalDate.now() : request.effectiveDate());
        return profileResponse(profileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public AccountSurvivalAssessmentResponse assessment(User user, Long accountId, LocalDate sessionDate) {
        accountService.getAccountForImport(user, accountId);
        AccountRiskProfile profile = profileRepository.findByAccount_IdAndUser(accountId, user).orElse(null);
        List<Trade> all = tradeRepository.findByUserAndAccount_IdOrderByTradeDateAscEntryTimeAscIdAsc(user, accountId);
        List<Trade> session = sessionDate == null ? List.of() : all.stream().filter(t -> sessionDate.equals(t.getTradeDate())).toList();
        List<Trade> userSession = sessionDate == null ? List.of() : tradeRepository.findByUserOrderByTradeDateAscEntryTimeAscIdAsc(user).stream()
                .filter(t -> sessionDate.equals(t.getTradeDate())).toList();
        List<String> missing = new ArrayList<>();
        if (profile == null) missing.add("Complete your account risk profile to calculate account survival.");
        BigDecimal balance = profile == null ? null : profile.getCurrentBalance();
        BigDecimal threshold = profile == null ? null : profile.getFailureThreshold();
        if (!AccountSurvivalCalculator.valid(balance)) missing.add("Current account balance is required.");
        if (threshold == null) missing.add("Failure or drawdown threshold is required.");
        BigDecimal buffer = balance != null && threshold != null ? balance.subtract(threshold) : null;
        List<BigDecimal> recentLosses = all.stream().filter(t -> pnl(t).signum() < 0).sorted(Comparator.comparing(Trade::getTradeDate).thenComparing(this::entryAt).reversed()).limit(AccountSurvivalCalculator.RECENT_LOSS_SAMPLE_SIZE).map(t -> pnl(t).abs()).toList();
        BigDecimal typical = AccountSurvivalCalculator.medianLoss(recentLosses);
        if (typical == null) missing.add("At least " + AccountSurvivalCalculator.MIN_LOSS_EVIDENCE + " realized losses are required to estimate typical loss.");
        BigDecimal planned = profile == null ? null : profile.getPlannedRiskPerTrade();
        BigDecimal plannedPct = AccountSurvivalCalculator.percent(planned, buffer);
        BigDecimal typicalPct = AccountSurvivalCalculator.percent(typical, buffer);
        BigDecimal capacity = AccountSurvivalCalculator.lossCapacity(buffer, typical);
        Integer sustainable = sustainableSize(buffer, typical, all, profile);
        AccountSurvivalState state = buffer != null && buffer.signum() <= 0 ? AccountSurvivalState.CRITICAL
                : missing.isEmpty() ? AccountSurvivalCalculator.state(buffer, typicalPct, capacity) : AccountSurvivalState.INSUFFICIENT_DATA;
        SessionTurningPointResponse turning = sessionDate == null ? noTurningPoint("No session date was selected.") : turningPoint(session, profile, userSession, accountId);
        String threat = primaryThreat(state, turning, typicalPct, missing);
        String evidence = evidenceStrength(recentLosses.size());
        log.info("Account survival calculated. accountId={}, state={}, lossEvidence={}, turningPoint={}", accountId, state, recentLosses.size(), turning.detected());
        return new AccountSurvivalAssessmentResponse(accountId, sessionDate, state, balance, threshold, buffer, planned, typical,
                recentLosses.size(), plannedPct, typicalPct, capacity, profile == null ? null : profile.getFirmMaxContracts(),
                profile == null ? null : profile.getPersonalMaxContracts(), sustainable, threat, evidence, List.copyOf(missing),
                turning, LocalDateTime.now(), CALCULATION_VERSION);
    }

    @Transactional(readOnly = true)
    public ProtectionCommitmentResponse commitment(User user, Long accountId, LocalDate date) {
        accountService.getAccountForImport(user, accountId);
        return commitmentRepository.findByAccount_IdAndUserAndEffectiveSessionDate(accountId, user, date).map(this::commitmentResponse).orElse(null);
    }

    @Transactional
    public ProtectionCommitmentResponse saveCommitment(User user, Long accountId, ProtectionCommitmentRequest request) {
        if (request.effectiveSessionDate() == null) throw new IllegalArgumentException("Effective session date is required");
        validateCommitment(request);
        AccountSurvivalAssessmentResponse assessment = assessment(user, accountId, request.effectiveSessionDate());
        // Lock the owned account as the stable parent key. This also serializes an initial
        // save with accept, when no commitment row exists yet and therefore cannot be locked.
        TradingAccount account = lockOwnedAccount(user, accountId);
        NextSessionProtectionCommitment commitment = commitmentRepository.findForUpdate(accountId, user, request.effectiveSessionDate()).orElseGet(NextSessionProtectionCommitment::new);
        // A protection rule becomes part of the historical accountability record once accepted.
        // Changes must be saved as a new commitment for a future session, never backdated.
        if (commitment.getId() != null && (commitment.getAcceptedAt() != null || commitment.getStatus() != ProtectionCommitmentStatus.DRAFT)) {
            throw new ProtectionCommitmentLifecycleException("Accepted protection commitments are locked. Create a new commitment for a future session.");
        }
        commitment.setUser(user); commitment.setAccount(account); commitment.setEffectiveSessionDate(request.effectiveSessionDate());
        commitment.setMaxNormalizedExposure(request.maxNormalizedExposure()); commitment.setMaxAttempts(request.maxAttempts());
        commitment.setMaxSessionLoss(request.maxSessionLoss()); commitment.setPostLossPauseMinutes(request.postLossPauseMinutes());
        commitment.setAllowedWindowStart(request.allowedWindowStart()); commitment.setAllowedWindowEnd(request.allowedWindowEnd());
        commitment.setRequireFreshSetupConfirmation(request.requireFreshSetupConfirmation());
        commitment.setSource(commitment.getId() == null ? ProtectionCommitmentSource.SYSTEM_RECOMMENDED : ProtectionCommitmentSource.USER_MODIFIED);
        commitment.setStatus(ProtectionCommitmentStatus.DRAFT);
        if (request.maxSessionLoss() == null && assessment.typicalLoss() != null) commitment.setMaxSessionLoss(assessment.typicalLoss().multiply(BigDecimal.valueOf(2)));
        return commitmentResponse(commitmentRepository.save(commitment));
    }

    @Transactional
    public ProtectionCommitmentResponse acceptCommitment(User user, Long accountId, LocalDate date) {
        lockOwnedAccount(user, accountId);
        NextSessionProtectionCommitment commitment = commitmentRepository.findForUpdate(accountId, user, date).orElseThrow(() -> new ResourceNotFoundException("Protection commitment not found"));
        // Repeated acceptance must not reset the original acceptance time or an existing evaluation.
        if (commitment.getStatus() != ProtectionCommitmentStatus.DRAFT) {
            if (commitment.getAcceptedAt() != null) return commitmentResponse(commitment);
            throw new ProtectionCommitmentLifecycleException("Only draft protection commitments can be accepted.");
        }
        commitment.setStatus(ProtectionCommitmentStatus.ACCEPTED); commitment.setAcceptedAt(LocalDateTime.now());
        return commitmentResponse(commitmentRepository.save(commitment));
    }
    @Transactional
    public CommitmentEvaluationResponse evaluateCommitment(User user, Long accountId, LocalDate date) {
        accountService.getAccountForImport(user, accountId);
        NextSessionProtectionCommitment c = commitmentRepository.findForEvaluation(accountId, user, date)
                .orElseThrow(() -> new ResourceNotFoundException("Protection commitment not found"));
        if (c.getStatus() == ProtectionCommitmentStatus.DRAFT || c.getAcceptedAt() == null) {
            throw new ProtectionCommitmentLifecycleException("Accept the commitment before evaluating it.");
        }
        LocalDate today = LocalDate.now(evaluationZone(user, accountId));
        if (c.getEffectiveSessionDate().isAfter(today)) {
            return new CommitmentEvaluationResponse(false, null, c.getStatus(), CALCULATION_VERSION, List.of(),
                    "This commitment's effective session has not started yet.");
        }
        List<Trade> all = tradeRepository.findByUserAndAccount_IdOrderByTradeDateAscEntryTimeAscIdAsc(user, accountId);
        LocalDate eligibilityStart = c.getEffectiveSessionDate().isAfter(c.getAcceptedAt().toLocalDate())
                ? c.getEffectiveSessionDate() : c.getAcceptedAt().toLocalDate();
        List<LocalDate> eligibleSessions = all.stream().map(Trade::getTradeDate).filter(Objects::nonNull)
                .filter(sessionDate -> !sessionDate.isAfter(today)).filter(sessionDate -> sessionDate.isAfter(eligibilityStart)).distinct().sorted().toList();
        if (eligibleSessions.isEmpty()) return new CommitmentEvaluationResponse(false, null, ProtectionCommitmentStatus.ACCEPTED, CALCULATION_VERSION, List.of(), "No later eligible imported session exists yet.");
        CommitmentEvaluationResponse latest = null;
        for (LocalDate eligible : eligibleSessions) {
            latest = evaluateSession(user, c, eligible, all.stream().filter(t -> eligible.equals(t.getTradeDate())).toList());
        }
        return latest;
    }

    private CommitmentEvaluationResponse evaluateSession(User user, NextSessionProtectionCommitment commitment, LocalDate eligible,
            List<Trade> trades) {
        String fingerprint = fingerprint(commitment, eligible, trades);
        Optional<NextSessionProtectionEvaluation> existing = evaluationRepository.findByCommitment_IdAndSourceFingerprint(commitment.getId(), fingerprint);
        if (existing.isPresent()) {
            NextSessionProtectionEvaluation stored = existing.get();
            if (!Boolean.TRUE.equals(stored.getIsCurrent())) {
                evaluationRepository.findByCommitment_IdAndEvaluatedSessionDateAndIsCurrentTrue(commitment.getId(), eligible)
                        .forEach(previous -> previous.setIsCurrent(false));
                evaluationRepository.flush();
                stored.setIsCurrent(true);
                evaluationRepository.saveAndFlush(stored);
            }
            return evaluationResponse(stored);
        }
        ZoneId zone = evaluationZone(user, commitment.getAccount().getId());
        ProtectionCommitmentEvaluator.Evaluation evaluation = commitmentEvaluator.evaluate(commitment, trades, zone);
        evaluationRepository.findByCommitment_IdAndEvaluatedSessionDateAndIsCurrentTrue(commitment.getId(), eligible)
                .forEach(previous -> previous.setIsCurrent(false));
        // PostgreSQL enforces one current version for this commitment/session. Flush the retiring version
        // before inserting its replacement so the partial unique index also protects corrected imports.
        evaluationRepository.flush();
        NextSessionProtectionEvaluation stored = new NextSessionProtectionEvaluation();
        stored.setUser(user); stored.setAccount(commitment.getAccount()); stored.setCommitment(commitment); stored.setEvaluatedSessionDate(eligible);
        stored.setOverallStatus(evaluation.overallStatus()); stored.setSourceFingerprint(fingerprint); stored.setEvaluationVersion(CALCULATION_VERSION);
        stored.setEvidenceCoverage(evaluation.rules().size()); stored.setIsCurrent(true); stored = evaluationRepository.saveAndFlush(stored);
        for (CommitmentEvaluationResponse.RuleResult result : evaluation.rules()) {
            NextSessionProtectionRuleResult row = new NextSessionProtectionRuleResult(); row.setEvaluation(stored); row.setRuleType(result.rule()); row.setResult(result.status());
            row.setConfiguredValue(result.configuredValue()); row.setObservedValue(result.observedValue()); row.setEvidence(result.message()); row.setEvidenceCount(trades.size()); row.setCalculationVersion(CALCULATION_VERSION); ruleResultRepository.save(row);
        }
        return evaluationResponse(stored);
    }
    @Transactional(readOnly = true)
    public CommitmentEvaluationResponse currentEvaluation(User user, Long accountId, LocalDate date) {
        accountService.getAccountForImport(user, accountId);
        NextSessionProtectionCommitment commitment = commitmentRepository.findByAccount_IdAndUserAndEffectiveSessionDate(accountId, user, date)
                .orElseThrow(() -> new ResourceNotFoundException("Protection commitment not found"));
        return evaluationRepository.findTopByCommitment_IdAndIsCurrentTrueOrderByEvaluatedSessionDateDescIdDesc(commitment.getId())
                .map(this::evaluationResponse)
                .orElseGet(() -> new CommitmentEvaluationResponse(false, null, commitment.getStatus(), CALCULATION_VERSION, List.of(), "No durable evaluation exists yet."));
    }
    @Transactional(readOnly = true)
    public CommitmentEvaluationHistoryResponse evaluationHistory(User user, Long accountId, LocalDate date) {
        accountService.getAccountForImport(user, accountId);
        NextSessionProtectionCommitment commitment = commitmentRepository.findByAccount_IdAndUserAndEffectiveSessionDate(accountId, user, date)
                .orElseThrow(() -> new ResourceNotFoundException("Protection commitment not found"));
        List<CommitmentEvaluationResponse> evaluations = evaluationRepository
                .findByCommitment_IdAndIsCurrentTrueOrderByEvaluatedSessionDateDescIdDesc(commitment.getId()).stream()
                .limit(HISTORY_WINDOW_SESSIONS).map(this::evaluationResponse).toList();
        int followed = (int) evaluations.stream().filter(e -> e.overallStatus() == ProtectionCommitmentStatus.COMPLETED).count();
        int broken = (int) evaluations.stream().filter(e -> e.overallStatus() == ProtectionCommitmentStatus.BROKEN).count();
        int partial = (int) evaluations.stream().filter(e -> e.overallStatus() == ProtectionCommitmentStatus.PARTIALLY_OBSERVED).count();
        int unavailable = (int) evaluations.stream().filter(e -> e.overallStatus() == ProtectionCommitmentStatus.NOT_EVALUABLE).count();
        int fullyEvaluable = followed + broken;
        Integer adherencePercent = fullyEvaluable == 0 ? null
                : BigDecimal.valueOf(followed).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(fullyEvaluable), 0, RoundingMode.HALF_UP).intValue();
        return new CommitmentEvaluationHistoryResponse(evaluations, HISTORY_WINDOW_SESSIONS, evaluations.size(), followed,
                broken, partial, unavailable, fullyEvaluable, adherencePercent);
    }
    @Transactional
    public void evaluateEligibleAfterImport(User user, Long accountId) {
        // Import callers run this after commit. We deliberately evaluate only commitments owned by this account/user.
        commitmentRepository.findByAccount_IdAndUserAndStatusNot(accountId, user, ProtectionCommitmentStatus.DRAFT).forEach(c -> {
                    try { evaluateCommitment(user, accountId, c.getEffectiveSessionDate()); }
                    catch (RuntimeException ex) { log.warn("Account survival post-import evaluation failed safely. accountId={}, commitmentId={}, reason={}", accountId, c.getId(), ex.getClass().getSimpleName()); }
                });
    }
    private CommitmentEvaluationResponse evaluationResponse(NextSessionProtectionEvaluation e){return new CommitmentEvaluationResponse(true,e.getEvaluatedSessionDate(),e.getOverallStatus(),e.getEvaluationVersion(),ruleResultRepository.findByEvaluation_IdOrderById(e.getId()).stream().map(r->new CommitmentEvaluationResponse.RuleResult(r.getRuleType(),r.getResult(),r.getConfiguredValue(),r.getObservedValue(),r.getEvidence())).toList(),null);}
    private ZoneId evaluationZone(User user, Long accountId) { String zone = profileRepository.findByAccount_IdAndUser(accountId, user).map(AccountRiskProfile::getTimezone).orElse(null); try { return zone == null || zone.isBlank() ? com.tradepilot.importing.TradingSessionPolicy.MARKET_ZONE : ZoneId.of(zone); } catch (DateTimeException ex) { return com.tradepilot.importing.TradingSessionPolicy.MARKET_ZONE; } }
    private String fingerprint(NextSessionProtectionCommitment c, LocalDate date, List<Trade> trades) { try { MessageDigest digest = MessageDigest.getInstance("SHA-256"); String payload = c.getId()+"|"+date+"|"+c.getMaxAttempts()+"|"+c.getMaxNormalizedExposure()+"|"+c.getMaxSessionLoss()+"|"+c.getPostLossPauseMinutes()+"|"+c.getAllowedWindowStart()+"|"+c.getAllowedWindowEnd()+"|"+c.getRequireFreshSetupConfirmation()+"|"+trades.stream().map(t -> t.getId()+":"+t.getEntryTime()+":"+t.getExitTime()+":"+t.getQuantity()+":"+t.getSymbol()+":"+pnl(t)).reduce("", (a,b)->a+"|"+b); return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8))); } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 is unavailable", ex); } }

    private SessionTurningPointResponse turningPoint(List<Trade> trades, AccountRiskProfile profile, List<Trade> userSession, Long accountId) {
        if (trades.isEmpty()) return noTurningPoint("No imported trades are available for this session.");
        List<Trade> ordered = trades.stream().sorted(Comparator.comparing(this::entryAt).thenComparing(Trade::getId, Comparator.nullsLast(Long::compareTo))).toList();
        BigDecimal running = BigDecimal.ZERO; BigDecimal peak = BigDecimal.ZERO;
        List<SessionTurningPointResponse> candidates = new ArrayList<>();
        for (int i=1; i<ordered.size(); i++) {
            Trade prior=ordered.get(i-1), current=ordered.get(i); BigDecimal before = running.add(pnl(prior));
            TurningPointType type=null; List<String> facts=new ArrayList<>(); long minutes=Duration.between(exitAt(prior), entryAt(current)).toMinutes();
            if (pnl(prior).signum()<0 && normalizedExposure(current).compareTo(normalizedExposure(prior))>0) {
                type=TurningPointType.POST_LOSS_SIZE_ESCALATION; facts.add("A realized loss was followed by a larger normalized exposure.");
            } else if (pnl(prior).signum()<0 && sameFamily(prior,current) && minutes>=0 && minutes<=RAPID_REENTRY_WINDOW.toMinutes()) {
                type=TurningPointType.RAPID_SAME_FAMILY_REENTRY; facts.add("A realized loss was followed by a same-family entry within " + minutes + " minutes.");
            } else if (profile != null && profile.getDailyLossLimit()!=null && before.compareTo(profile.getDailyLossLimit().negate())<=0) {
                type=TurningPointType.PERSONAL_LOSS_BOUNDARY_CONTINUED; facts.add("The configured session-loss boundary had been reached before this entry.");
            } else if (peak.signum()>0 && before.compareTo(peak)<0) {
                type=TurningPointType.PROFIT_GIVEBACK_CONTINUATION; facts.add("Trading continued after the session moved below an earlier realized P&L peak.");
            }
            if(type!=null) candidates.add(turning(type,current,prior,before,ordered,facts,minutes));
            running=before; if(running.compareTo(peak)>0)peak=running;
        }
        crossAccountContinuationDetector.detect(userSession, accountId).stream()
                .map(candidate -> crossAccountTurning(candidate, userSession))
                .forEach(candidates::add);
        return candidates.stream()
                .sorted(Comparator.comparing(SessionTurningPointResponse::timestamp)
                        .thenComparing(response -> response.type() == TurningPointType.CROSS_ACCOUNT_CONTINUATION ? 1 : 0)
                        .thenComparing(response -> response.tradeId(), Comparator.nullsLast(Long::compareTo)))
                .findFirst()
                .orElseGet(() -> noTurningPoint("No evidence-backed turning point was detected for this session."));
    }

    private SessionTurningPointResponse turning(TurningPointType type, Trade current, Trade prior, BigDecimal before, List<Trade> all, List<String> facts, long minutes) {
        BigDecimal finalPnl=all.stream().map(this::pnl).reduce(BigDecimal.ZERO,BigDecimal::add);
        List<Trade> later=all.stream().filter(t -> entryAt(t).isAfter(entryAt(current)) || (entryAt(t).equals(entryAt(current)) && !Objects.equals(t.getId(),current.getId()))).toList();
        BigDecimal laterPnl=later.stream().map(this::pnl).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal lossesAvoided=later.stream().map(this::pnl).filter(v->v.signum()<0).map(BigDecimal::abs).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal winnersForgone=later.stream().map(this::pnl).filter(v->v.signum()>0).reduce(BigDecimal.ZERO,BigDecimal::add);
        return new SessionTurningPointResponse(true,type,entryAt(current),current.getId(),current.getAccount()==null?null:current.getAccount().getName(),facts,
                normalizedExposure(prior),normalizedExposure(current),minutes,before,finalPnl,before,lossesAvoided,winnersForgone,laterPnl.negate(),"INSUFFICIENT",List.of("This is a single-session observed sequence; it does not infer intent or predict an outcome."),CALCULATION_VERSION,null);
    }
    private SessionTurningPointResponse crossAccountTurning(CrossAccountContinuationDetector.Candidate candidate, List<Trade> session) {
        var loss = candidate.loss(); var later = candidate.later();
        Set<Long> scopeAccountIds = new HashSet<>(loss.accountIds()); scopeAccountIds.addAll(later.accountIds());
        List<Trade> scope = session.stream().filter(trade -> trade.getAccount() != null && scopeAccountIds.contains(trade.getAccount().getId())).toList();
        LocalDateTime actionAt = later.entryAt();
        BigDecimal before = scope.stream().filter(trade -> exitAt(trade).isBefore(actionAt)).map(this::pnl).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal finalPnl = scope.stream().map(this::pnl).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Trade> after = scope.stream().filter(trade -> !entryAt(trade).isBefore(actionAt)).toList();
        BigDecimal laterPnl = after.stream().map(this::pnl).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal lossesAvoided = after.stream().map(this::pnl).filter(value -> value.signum() < 0).map(BigDecimal::abs).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal winnersForgone = after.stream().map(this::pnl).filter(value -> value.signum() > 0).reduce(BigDecimal.ZERO, BigDecimal::add);
        CrossAccountContinuationEvidence evidence = new CrossAccountContinuationEvidence(loss.accountNames(), later.accountNames(),
                loss.symbols(), later.symbols(), later.family(), later.direction(), candidate.elapsedSeconds(), loss.pnl(),
                loss.exposure(), later.exposure(), candidate.exposureDifference(), candidate.exposureMultiplier(), "SECOND", candidate.confidence(),
                candidate.limitations(), CALCULATION_VERSION);
        List<String> facts = List.of("Cross-account continuation detected.",
                "A losing " + later.direction().toLowerCase(Locale.ROOT) + " " + later.family() + " attempt was followed "
                        + candidate.elapsedSeconds() + " seconds later by a new attempt across another participating account.",
                "Normalized exposure changed from " + loss.exposure().stripTrailingZeros().toPlainString() + " to " + later.exposure().stripTrailingZeros().toPlainString() + " units.");
        Long tradeId = later.attempts().isEmpty() ? null : later.attempts().get(0).tradeId();
        return new SessionTurningPointResponse(true, TurningPointType.CROSS_ACCOUNT_CONTINUATION, actionAt, tradeId,
                String.join(", ", later.accountNames()), facts, loss.exposure(), later.exposure(), candidate.elapsedSeconds() / 60,
                before, finalPnl, before, lossesAvoided, winnersForgone, laterPnl.negate(), candidate.confidence(), candidate.limitations(),
                CALCULATION_VERSION, evidence);
    }
    private SessionTurningPointResponse noTurningPoint(String limitation) { return new SessionTurningPointResponse(false,null,null,null,null,List.of(),null,null,null,null,null,null,null,null,null,"INSUFFICIENT",List.of(limitation),CALCULATION_VERSION,null); }
    private Integer sustainableSize(BigDecimal buffer, BigDecimal typical, List<Trade> trades, AccountRiskProfile profile) {
        if(!AccountSurvivalCalculator.valid(buffer)||!AccountSurvivalCalculator.valid(typical)||trades.isEmpty()) return null;
        BigDecimal medianQty=trades.stream().map(Trade::getQuantity).filter(Objects::nonNull).map(BigDecimal::valueOf).sorted().skip(Math.max(0,trades.size()/2)).findFirst().orElse(null);
        if(!AccountSurvivalCalculator.valid(medianQty)) return null;
        int result=buffer.multiply(new BigDecimal("0.10")).divide(typical.divide(medianQty,6,RoundingMode.HALF_UP),0,RoundingMode.DOWN).intValue();
        if(profile.getPersonalMaxContracts()!=null) result=Math.min(result,profile.getPersonalMaxContracts()); if(profile.getFirmMaxContracts()!=null) result=Math.min(result,profile.getFirmMaxContracts()); return Math.max(0,result);
    }
    private BigDecimal normalizedExposure(Trade trade) { int qty=trade.getQuantity()==null?0:trade.getQuantity(); String s=trade.getSymbol()==null?"":trade.getSymbol().toUpperCase(Locale.ROOT); return BigDecimal.valueOf(qty*(s.startsWith("NQ")||s.startsWith("ES")?10:1)); }
    private boolean sameFamily(Trade a, Trade b){return family(a.getSymbol()).equals(family(b.getSymbol()));} private String family(String s){if(s==null)return"";s=s.toUpperCase(Locale.ROOT);return s.startsWith("MNQ")||s.startsWith("NQ")?"NQ":s.startsWith("MES")||s.startsWith("ES")?"ES":s;}
    private String primaryThreat(AccountSurvivalState state, SessionTurningPointResponse turning, BigDecimal lossPct,List<String> missing){if(!missing.isEmpty())return"Configuration or observed-loss evidence is incomplete.";if(turning.detected())return humanize(turning.type());if(lossPct!=null)return"Typical realized loss uses " + lossPct.setScale(1,RoundingMode.HALF_UP)+"% of the remaining buffer.";return state.name();}
    private String evidenceStrength(int n){return n<5?"INSUFFICIENT":n<10?"WEAK":n<20?"MODERATE":"STRONG";} private String humanize(Enum<?> e){return e.name().toLowerCase(Locale.ROOT).replace('_',' ');}
    private BigDecimal pnl(Trade t){return t.getPnlAmount()==null?BigDecimal.ZERO:t.getPnlAmount();} private LocalDateTime entryAt(Trade t){return t.getEntryTime()==null?t.getTradeDate().atStartOfDay():t.getEntryTime();} private LocalDateTime exitAt(Trade t){return t.getExitTime()==null?entryAt(t):t.getExitTime();}
    private AccountRiskProfileResponse profileResponse(AccountRiskProfile p){return new AccountRiskProfileResponse(p.getAccount().getId(),p.getCurrentBalance(),p.getFailureThreshold(),p.getDailyLossLimit(),p.getPlannedRiskPerTrade(),p.getPersonalMaxContracts(),p.getFirmMaxContracts(),p.getPrimaryWindowStart(),p.getPrimaryWindowEnd(),p.getRenewalDate(),p.getPayoutTarget(),p.getPayoutDate(),p.getTimezone(),p.getAccountStage(),p.getEffectiveDate(),p.getUpdatedAt());}
    private ProtectionCommitmentResponse commitmentResponse(NextSessionProtectionCommitment c){return new ProtectionCommitmentResponse(c.getId(),c.getEffectiveSessionDate()==null?null:c.getAccount().getId(),c.getEffectiveSessionDate(),c.getMaxNormalizedExposure(),c.getMaxAttempts(),c.getMaxSessionLoss(),c.getPostLossPauseMinutes(),c.getAllowedWindowStart(),c.getAllowedWindowEnd(),c.getRequireFreshSetupConfirmation(),c.getSource(),c.getStatus(),c.getAcceptedAt());}
    private void validateProfile(AccountRiskProfileRequest r){for(BigDecimal v:java.util.stream.Stream.of(r.currentBalance(),r.failureThreshold(),r.dailyLossLimit(),r.plannedRiskPerTrade(),r.payoutTarget()).filter(Objects::nonNull).toList())if(v.signum()<=0)throw new IllegalArgumentException("Risk-profile monetary values must be greater than zero");if(r.personalMaxContracts()!=null&&r.personalMaxContracts()<=0||r.firmMaxContracts()!=null&&r.firmMaxContracts()<=0)throw new IllegalArgumentException("Maximum contracts must be greater than zero");if(r.primaryWindowStart()!=null&&r.primaryWindowEnd()!=null&&!r.primaryWindowStart().isBefore(r.primaryWindowEnd()))throw new IllegalArgumentException("Primary trading window end must be after start");}
    private void validateCommitment(ProtectionCommitmentRequest r){if(r.maxNormalizedExposure()!=null&&r.maxNormalizedExposure().signum()<=0||r.maxSessionLoss()!=null&&r.maxSessionLoss().signum()<=0||r.maxAttempts()!=null&&r.maxAttempts()<=0||r.postLossPauseMinutes()!=null&&r.postLossPauseMinutes()<0)throw new IllegalArgumentException("Commitment limits must be positive");}
    private String blankToNull(String v){return v==null||v.isBlank()?null:v.trim();}
    private TradingAccount lockOwnedAccount(User user, Long accountId) {
        return tradingAccountRepository.findByIdAndUserForUpdate(accountId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Trading account not found"));
    }
}
