package com.tradepilot.service;

import com.tradepilot.billing.UsageLimitService;
import com.tradepilot.dto.common.PagedResponse;
import com.tradepilot.dto.trade.*;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.User;
import com.tradepilot.enums.Market;
import com.tradepilot.enums.Side;
import com.tradepilot.enums.SetupType;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.repository.ImportBatchRepository;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.spec.TradeSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.tradepilot.entity.ImportBatch;
import com.tradepilot.entity.TradingAccount;


@Service
@RequiredArgsConstructor
@Slf4j
public class TradeService {
    private final TradeRepository tradeRepository;
    private final TradingAccountService tradingAccountService;
    private final ImportBatchRepository importBatchRepository;
    private final UsageLimitService usageLimitService;
    private final PerformanceCacheInvalidationService cacheInvalidationService;

    @Transactional
    public TradeResponse createTrade(User user, TradeRequest request) {
        requireUser(user);
        Trade saved = tradeRepository.save(mapToEntity(user, request));
        cacheInvalidationService.invalidateUserTradeDerivedCaches(user.getId());
        log.info("Trade created. userId={}, tradeId={}, symbol={}", user.getId(), saved.getId(), saved.getSymbol());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TradeResponse> getTrades(User user, TradeFilterRequest filter, int page, int size, String sortBy, String sortDir) {
        requireUser(user);
        validateOwnedAccountFilter(user, filter.getAccountId());

        if (filter.getStartDate() != null && filter.getEndDate() != null && filter.getStartDate().isAfter(filter.getEndDate()))
            throw new IllegalArgumentException("startDate cannot be after endDate");
        String safeSortBy = sanitizeSortBy(sortBy);
        String safeSortDir = "asc".equalsIgnoreCase(sortDir) ? "asc" : "desc";
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 25 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.fromString(safeSortDir), safeSortBy));
        log.trace("Trades requested. userId={}, accountId={}, page={}, size={}, sortBy={}, sortDir={}",
                user.getId(), filter.getAccountId(), safePage, safeSize, safeSortBy, safeSortDir);
        Page<Trade> tradePage = tradeRepository.findAll(TradeSpecification.withFilters(user, filter), pageable);
        List<TradeResponse> content = tradePage.getContent().stream().map(this::mapToResponse).toList();
        return PagedResponse.of(content, tradePage.getNumber(), tradePage.getSize(), tradePage.getTotalElements(), tradePage.getTotalPages(), tradePage.isFirst(), tradePage.isLast(), safeSortBy, safeSortDir);
    }

    @Transactional(readOnly = true)
    public TradeResponse getTradeById(User user, Long tradeId) {
        requireUser(user);
        log.trace("Trade requested. userId={}, tradeId={}", user.getId(), tradeId);
        return tradeRepository.findByIdAndUser(tradeId, user).map(this::mapToResponse).orElseThrow(() -> new ResourceNotFoundException("Trade not found"));
    }

    @Transactional
    public TradeResponse updateTrade(User user, Long tradeId, TradeRequest request) {
        requireUser(user);
        Trade t = tradeRepository.findByIdAndUser(tradeId, user).orElseThrow(() -> new ResourceNotFoundException("Trade not found"));
        applyRequest(t, request);
        Trade saved = tradeRepository.save(t);
        cacheInvalidationService.invalidateUserTradeDerivedCaches(user.getId());
        log.info("Trade updated. userId={}, tradeId={}", user.getId(), tradeId);
        return mapToResponse(saved);
    }

    @Transactional
    public TradeResponse updateTradeJournal(User user, Long tradeId, TradeJournalRequest request) {
        requireUser(user);
        Trade trade = tradeRepository.findByIdAndUser(tradeId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Trade not found"));
        trade.setSetup(parseSetup(request.setup()));
        trade.setConfidence(request.confidence());
        trade.setConfidenceRating(request.confidence());
        trade.setEmotion(cleanText(request.emotion()));
        trade.setNotes(cleanText(request.notes()));
        Trade saved = tradeRepository.save(trade);
        cacheInvalidationService.invalidateUserTradeDerivedCaches(user.getId());
        log.info("Trade journal updated. userId={}, tradeId={}", user.getId(), tradeId);
        return mapToResponse(saved);
    }

    @Transactional
    public List<TradeResponse> importTrades(User user, List<TradeImportRequest> requests) {
        requireUser(user);

        if (requests == null || requests.isEmpty()) {
            log.warn("Trade import skipped because request list is empty. userId={}", user.getId());
            return List.of();
        }

        Long accountId = requests.get(0).accountId();
        log.info("Trade import started. userId={}, accountId={}, requestedRows={}", user.getId(), accountId, requests.size());

        if (accountId == null) {
            throw new IllegalArgumentException("accountId is required for import");
        }

        usageLimitService.enforceTradeImport(user, requests.size());

        TradingAccount account = tradingAccountService.getAccountForImport(user, accountId);

        ImportBatch batch = ImportBatch.builder()
                .user(user)
                .account(account)
                .fileName(requests.get(0).fileName())
                .totalRows(requests.size())
                .importedRows(0)
                .skippedDuplicates(0)
                .build();

        batch = importBatchRepository.save(batch);

        List<Trade> tradesToSave = new java.util.ArrayList<>();
        java.util.Set<String> seenKeys = new java.util.HashSet<>();
        int skippedDuplicates = 0;

        for (TradeImportRequest request : requests) {
            String key = request.externalTradeKey();

            if (key != null && !seenKeys.add(key)) {
                skippedDuplicates++;
                continue;
            }

            if (key != null && tradeRepository.existsByUserAndAccount_IdAndExternalTradeKey(user, accountId, key)) {
                skippedDuplicates++;
                continue;
            }

            Trade trade = Trade.builder()
                    .user(user)
                    .account(account)
                    .importBatch(batch)
                    .externalTradeKey(request.externalTradeKey())
                    .symbol(request.symbol())
                    .market(request.market())
                    .tradeDate(request.tradeDate())
                    .entryTime(request.entryTime())
                    .exitTime(request.exitTime())
                    .side(request.side())
                    .setup(request.setup())
                    .session(request.session())
                    .entryPrice(request.entryPrice())
                    .exitPrice(request.exitPrice())
                    .quantity(request.quantity())
                    .pnlAmount(request.pnlAmount())
                    .pnlR(request.pnlR())
                    .outcome(request.outcome())
                    .build();

            tradesToSave.add(trade);
        }

        List<Trade> saved = tradeRepository.saveAll(tradesToSave);

        batch.setImportedRows(saved.size());
        batch.setSkippedDuplicates(skippedDuplicates);
        importBatchRepository.save(batch);
        cacheInvalidationService.invalidateUserTradeDerivedCaches(user.getId());
        log.info("Trade import completed. userId={}, accountId={}, batchId={}, importedRows={}, skippedDuplicates={}",
                user.getId(), accountId, batch.getId(), saved.size(), skippedDuplicates);

        return saved.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void deleteTrade(User user, Long tradeId) {
        requireUser(user);
        Trade t = tradeRepository.findByIdAndUser(tradeId, user).orElseThrow(() -> new ResourceNotFoundException("Trade not found"));
        tradeRepository.delete(t);
        cacheInvalidationService.invalidateUserTradeDerivedCaches(user.getId());
        log.info("Trade deleted. userId={}, tradeId={}", user.getId(), tradeId);
    }

    @Transactional(readOnly = true)
    public Trade getOwnedTrade(User user, Long tradeId) {
        requireUser(user);
        return tradeRepository.findByIdAndUser(tradeId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Trade not found"));
    }

    private String sanitizeSortBy(String sortBy) {
        return switch (sortBy) {
            case "tradeDate", "symbol", "setup", "session", "outcome", "pnlAmount", "pnlR", "createdAt" -> sortBy;
            default -> "tradeDate";
        };
    }

    private Trade mapToEntity(User user, TradeRequest r) {
        Trade t = new Trade();
        t.setUser(user);
        applyRequest(t, r);
        return t;
    }

    private void applyRequest(Trade t, TradeRequest r) {
        t.setSymbol(r.getSymbol() == null ? null : r.getSymbol().trim().toUpperCase());
        t.setMarket(r.getMarket());
        t.setTradeDate(r.getTradeDate());
        t.setEntryTime(r.getEntryTime());
        t.setExitTime(r.getExitTime());
        t.setSide(r.getSide());
        t.setSetup(r.getSetup());
        t.setSession(r.getSession());
        t.setEntryPrice(r.getEntryPrice());
        t.setStopPrice(r.getStopPrice());
        t.setTargetPrice(r.getTargetPrice());
        t.setExitPrice(r.getExitPrice());
        t.setQuantity(r.getQuantity());
        t.setRiskAmount(r.getRiskAmount());
        t.setPnlAmount(r.getPnlAmount());
        t.setPnlR(r.getPnlR());
        t.setOutcome(r.getOutcome());
        t.setConfidenceRating(r.getConfidenceRating());
        t.setExecutionRating(r.getExecutionRating());
        t.setFollowedPlan(r.getFollowedPlan());
        t.setMistakeTag(r.getMistakeTag());
        t.setNotes(r.getNotes());
        t.setPsychologyNotes(r.getPsychologyNotes());
        t.setLessonLearned(r.getLessonLearned());
        t.setScreenshotUrl(r.getScreenshotUrl());
    }

    public TradeResponse mapToResponse(Trade t) {
        TradingAccount account = t.getAccount();
        TradeResponse response = new TradeResponse();
        response.setId(t.getId());
        response.setAccountId(account == null ? null : account.getId());
        response.setAccountName(account == null ? null : account.getName());
        response.setAccountType(account == null || account.getAccountType() == null ? null : account.getAccountType().name());
        response.setSymbol(t.getSymbol());
        response.setMarket(parseMarket(t.getMarket()));
        response.setTradeDate(t.getTradeDate());
        response.setEntryTime(t.getEntryTime());
        response.setExitTime(t.getExitTime());
        response.setSide(Side.valueOf(t.getSide()));
        response.setSetup(t.getSetup());
        response.setSession(t.getSession());
        response.setEntryPrice(t.getEntryPrice());
        response.setStopPrice(t.getStopPrice());
        response.setTargetPrice(t.getTargetPrice());
        response.setExitPrice(t.getExitPrice());
        response.setQuantity(t.getQuantity());
        response.setRiskAmount(t.getRiskAmount());
        response.setPnlAmount(t.getPnlAmount());
        response.setPnlR(t.getPnlR());
        response.setOutcome(t.getOutcome());
        response.setConfidence(t.getConfidence() == null ? t.getConfidenceRating() : t.getConfidence());
        response.setConfidenceRating(t.getConfidenceRating() == null ? t.getConfidence() : t.getConfidenceRating());
        response.setEmotion(t.getEmotion());
        response.setExecutionRating(t.getExecutionRating());
        response.setFollowedPlan(t.getFollowedPlan());
        response.setMistakeTag(t.getMistakeTag());
        response.setNotes(t.getNotes());
        response.setPsychologyNotes(t.getPsychologyNotes());
        response.setLessonLearned(t.getLessonLearned());
        response.setScreenshotUrl(t.getScreenshotUrl());
        return response;
    }

    private Market parseMarket(String market) {
        if (market == null || market.isBlank()) {
            return Market.OTHER;
        }
        try {
            return Market.valueOf(market.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown trade market mapped to OTHER. market={}", market);
            return Market.OTHER;
        }
    }

    private SetupType parseSetup(String setup) {
        String clean = cleanText(setup);
        if (clean == null) {
            return null;
        }
        try {
            return SetupType.valueOf(clean.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported setup value");
        }
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }
        String clean = value.trim();
        return clean.isEmpty() ? null : clean;
    }

    private void validateOwnedAccountFilter(User user, Long accountId) {
        if (accountId != null) {
            tradingAccountService.getAccountForImport(user, accountId);
        }
    }

    private void requireUser(User user) {
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }
    }
}
