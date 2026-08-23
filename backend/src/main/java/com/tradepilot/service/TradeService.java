package com.tradepilot.service;

import com.tradepilot.billing.UsageLimitService;
import com.tradepilot.ai.memory.service.TradeMemoryIndexService;
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
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.tradepilot.entity.ImportBatch;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.tradereplay.service.ContractSymbolNormalizationResult;
import com.tradepilot.tradereplay.service.ContractSymbolSource;
import com.tradepilot.tradereplay.service.FuturesContractSymbolNormalizer;

@Service
public class TradeService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TradeService.class);
    private static final int MAX_IMPORT_ROWS = 5000;
    private static final List<DateTimeFormatter> TRADOVATE_TIMESTAMP_FORMATTERS = List.of(new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("M/d/yyyy h:mm:ss a").toFormatter(Locale.US), new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("M/d/yyyy h:mm a").toFormatter(Locale.US), new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("M/d/yy h:mm:ss a").toFormatter(Locale.US), new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("M/d/yy h:mm a").toFormatter(Locale.US));
    private final TradeRepository tradeRepository;
    private final TradingAccountService tradingAccountService;
    private final ImportBatchRepository importBatchRepository;
    private final UsageLimitService usageLimitService;
    private final PerformanceCacheInvalidationService cacheInvalidationService;
    private final TradeMemoryIndexService tradeMemoryIndexService;
    private final FuturesContractSymbolNormalizer contractSymbolNormalizer;

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
        if (filter.getStartDate() != null && filter.getEndDate() != null && filter.getStartDate().isAfter(filter.getEndDate())) throw new IllegalArgumentException("startDate cannot be after endDate");
        String safeSortBy = sanitizeSortBy(sortBy);
        String safeSortDir = "asc".equalsIgnoreCase(sortDir) ? "asc" : "desc";
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 25 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.fromString(safeSortDir), safeSortBy));
        log.trace("Trades requested. userId={}, accountId={}, page={}, size={}, sortBy={}, sortDir={}", user.getId(), filter.getAccountId(), safePage, safeSize, safeSortBy, safeSortDir);
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
        tradeMemoryIndexService.markStale(user.getId(), tradeId);
        log.info("Trade updated. userId={}, tradeId={}", user.getId(), tradeId);
        return mapToResponse(saved);
    }

    @Transactional
    public TradeResponse updateTradeJournal(User user, Long tradeId, TradeJournalRequest request) {
        requireUser(user);
        Trade trade = tradeRepository.findByIdAndUser(tradeId, user).orElseThrow(() -> new ResourceNotFoundException("Trade not found"));
        trade.setSetup(parseSetup(request.setup()));
        trade.setConfidence(request.confidence());
        trade.setConfidenceRating(request.confidence());
        trade.setEmotion(cleanText(request.emotion()));
        trade.setNotes(cleanText(request.notes()));
        Trade saved = tradeRepository.save(trade);
        cacheInvalidationService.invalidateUserTradeDerivedCaches(user.getId());
        tradeMemoryIndexService.markStale(user.getId(), tradeId);
        log.info("Trade journal updated. userId={}, tradeId={}", user.getId(), tradeId);
        return mapToResponse(saved);
    }

    @Transactional
    public List<TradeResponse> importTrades(User user, List<TradeImportRequest> requests) {
        return importTrades(user, requests, false);
    }

    @Transactional
    public List<TradeResponse> importBrokerTrades(User user, List<TradeImportRequest> requests) {
        return importTrades(user, requests, true);
    }

    private List<TradeResponse> importTrades(User user, List<TradeImportRequest> requests, boolean trustContractSource) {
        requireUser(user);
        if (requests == null || requests.isEmpty()) {
            log.warn("Trade import skipped because request list is empty. userId={}", user.getId());
            return List.of();
        }
        long startedAt = System.currentTimeMillis();
        if (requests.size() > MAX_IMPORT_ROWS) {
            log.warn("Trade import rejected because row count exceeds limit. userId={}, requestedRows={}, maxRows={}", user.getId(), requests.size(), MAX_IMPORT_ROWS);
            throw new IllegalArgumentException("CSV import is limited to " + MAX_IMPORT_ROWS + " rows per upload.");
        }
        Long accountId = requests.get(0).accountId();
        log.info("Trade import started. userId={}, accountId={}, requestedRows={}", user.getId(), accountId, requests.size());
        if (accountId == null) {
            throw new IllegalArgumentException("accountId is required for import");
        }
        usageLimitService.enforceTradeImport(user, requests.size());
        TradingAccount account = tradingAccountService.getAccountForImport(user, accountId);
        ImportBatch batch = ImportBatch.builder().user(user).account(account).fileName(requests.get(0).fileName()).totalRows(requests.size()).importedRows(0).skippedDuplicates(0).build();
        batch = importBatchRepository.save(batch);
        List<Trade> tradesToSave = new java.util.ArrayList<>();
        Set<String> importKeys = requests.stream()
                .map(TradeImportRequest::externalTradeKey)
                .filter(key -> key != null)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        Set<String> existingKeys = importKeys.isEmpty()
                ? Set.of()
                : new java.util.HashSet<>(tradeRepository.findExistingExternalTradeKeys(user, accountId, List.copyOf(importKeys)));
        java.util.Set<String> seenKeys = new java.util.HashSet<>();
        int skippedDuplicates = 0;
        for (TradeImportRequest request : requests) {
            String key = request.externalTradeKey();
            if (key != null && !seenKeys.add(key)) {
                skippedDuplicates++;
                continue;
            }
            if (key != null && existingKeys.contains(key)) {
                skippedDuplicates++;
                continue;
            }
            Side requestedSide = parseSide(request.side());
            RepairData repairData = repairDataFromExternalKey(request.externalTradeKey());
            Side finalSide = requestedSide;
            if (requestedSide == Side.UNKNOWN && repairData != null && repairData.side() != Side.UNKNOWN) {
                finalSide = repairData.side();
                log.info("Tradovate import side inferred from execution timestamps. userId={}, accountId={}, symbol={}, externalTradeKey={}, side={}", user.getId(), accountId, request.symbol(), request.externalTradeKey(), finalSide);
            } else if (requestedSide == Side.UNKNOWN) {
                log.warn("Tradovate import side unresolved. userId={}, accountId={}, symbol={}, tradeDate={}, entryTime={}, exitTime={}, externalTradeKey={}", user.getId(), accountId, request.symbol(), request.tradeDate(), request.entryTime(), request.exitTime(), request.externalTradeKey());
            }
            LocalDate tradeDate = request.tradeDate();
            LocalDateTime entryTime = request.entryTime();
            LocalDateTime exitTime = request.exitTime();
            BigDecimal entryPrice = request.entryPrice();
            BigDecimal exitPrice = request.exitPrice();
            if (requestedSide == Side.UNKNOWN && repairData != null && repairData.side() != Side.UNKNOWN) {
                tradeDate = repairData.exitTime().toLocalDate();
                entryTime = repairData.entryTime();
                exitTime = repairData.exitTime();
                entryPrice = repairData.entryPrice() == null ? request.entryPrice() : repairData.entryPrice();
                exitPrice = repairData.exitPrice() == null ? request.exitPrice() : repairData.exitPrice();
            }
            Trade trade = Trade.builder().user(user).account(account).importBatch(batch).externalTradeKey(request.externalTradeKey()).symbol(request.symbol()).market(request.market()).tradeDate(tradeDate).entryTime(entryTime).exitTime(exitTime).side(finalSide.name()).setup(request.setup()).session(request.session()).entryPrice(entryPrice).exitPrice(exitPrice).quantity(request.quantity()).pnlAmount(request.pnlAmount()).pnlR(request.pnlR()).outcome(request.outcome()).build();
            applyImportedContractFields(trade, request, entryTime, trustContractSource);
            tradesToSave.add(trade);
        }
        List<Trade> saved = tradeRepository.saveAll(tradesToSave);
        batch.setImportedRows(saved.size());
        batch.setSkippedDuplicates(skippedDuplicates);
        importBatchRepository.save(batch);
        cacheInvalidationService.invalidateUserTradeDerivedCaches(user.getId());
        log.info("Trade import completed. userId={}, accountId={}, batchId={}, requestedRows={}, importedRows={}, skippedDuplicates={}, existingKeyCount={}, durationMs={}", user.getId(), accountId, batch.getId(), requests.size(), saved.size(), skippedDuplicates, existingKeys.size(), System.currentTimeMillis() - startedAt);
        return saved.stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public Map<String, Object> repairTradovateDirections(User user, Long accountId, LocalDate tradeDate) {
        requireUser(user);
        validateOwnedAccountFilter(user, accountId);
        List<Trade> candidates = tradeRepository.findImportedTradesForDirectionRepair(user, accountId, tradeDate);
        int updated = 0;
        int skipped = 0;
        int invalidKeys = 0;
        for (Trade trade : candidates) {
            RepairData repairData = repairDataFromExternalKey(trade.getExternalTradeKey());
            if (repairData == null) {
                invalidKeys++;
                continue;
            }
            if (repairData.side() == Side.UNKNOWN) {
                skipped++;
                continue;
            }
            boolean changed = false;
            if (!repairData.side().name().equals(trade.getSide())) {
                trade.setSide(repairData.side().name());
                changed = true;
            }
            if (!repairData.entryTime().equals(trade.getEntryTime())) {
                trade.setEntryTime(repairData.entryTime());
                changed = true;
            }
            if (!repairData.exitTime().equals(trade.getExitTime())) {
                trade.setExitTime(repairData.exitTime());
                changed = true;
            }
            if (repairData.entryPrice() != null && repairData.entryPrice().compareTo(nullSafe(trade.getEntryPrice())) != 0) {
                trade.setEntryPrice(repairData.entryPrice());
                changed = true;
            }
            if (repairData.exitPrice() != null && repairData.exitPrice().compareTo(nullSafe(trade.getExitPrice())) != 0) {
                trade.setExitPrice(repairData.exitPrice());
                changed = true;
            }
            if (!repairData.exitTime().toLocalDate().equals(trade.getTradeDate())) {
                trade.setTradeDate(repairData.exitTime().toLocalDate());
                changed = true;
            }
            if (changed) {
                updated++;
            } else {
                skipped++;
            }
        }
        if (updated > 0) {
            tradeRepository.saveAll(candidates);
            cacheInvalidationService.invalidateUserTradeDerivedCaches(user.getId());
        }
        log.info("Tradovate direction repair completed. userId={}, accountId={}, date={}, scanned={}, updated={}, skipped={}, invalidKeys={}", user.getId(), accountId, tradeDate, candidates.size(), updated, skipped, invalidKeys);
        return Map.of("scanned", candidates.size(), "updated", updated, "skipped", skipped, "invalidKeys", invalidKeys);
    }

    @Transactional
    public void deleteTrade(User user, Long tradeId) {
        requireUser(user);
        Trade t = tradeRepository.findByIdAndUser(tradeId, user).orElseThrow(() -> new ResourceNotFoundException("Trade not found"));
        tradeMemoryIndexService.deleteMemory(user.getId(), tradeId);
        tradeRepository.delete(t);
        cacheInvalidationService.invalidateUserTradeDerivedCaches(user.getId());
        log.info("Trade deleted. userId={}, tradeId={}", user.getId(), tradeId);
    }

    @Transactional(readOnly = true)
    public Trade getOwnedTrade(User user, Long tradeId) {
        requireUser(user);
        return tradeRepository.findByIdAndUser(tradeId, user).orElseThrow(() -> new ResourceNotFoundException("Trade not found"));
    }

    @Transactional(readOnly = true)
    public List<Trade> getImportedTradesForReplayInitialization(User user, List<Long> tradeIds) {
        requireUser(user);
        if (tradeIds == null || tradeIds.isEmpty()) return List.of();
        return tradeRepository.findByUser_IdAndIdIn(user.getId(), tradeIds);
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
        applyContractFields(t, r.getSymbol(), null, false);
        t.setMarket(r.getMarket());
        t.setTradeDate(r.getTradeDate());
        t.setEntryTime(r.getEntryTime());
        t.setExitTime(r.getExitTime());
        t.setSide(normalizeSideValue(r.getSide()));
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
        response.setOriginalBrokerSymbol(t.getOriginalBrokerSymbol());
        response.setContractSymbol(t.getContractSymbol());
        response.setContractSymbolSource(t.getContractSymbolSource());
        response.setContractSymbolConfirmedAt(t.getContractSymbolConfirmedAt());
        response.setMarket(parseMarket(t.getMarket()));
        response.setTradeDate(t.getTradeDate());
        response.setEntryTime(t.getEntryTime());
        response.setExitTime(t.getExitTime());
        response.setSide(parseSide(t.getSide()));
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

    private String normalizeSideValue(String side) {
        return parseSide(side).name();
    }

    private Side parseSide(String side) {
        if (side == null || side.isBlank()) {
            return Side.UNKNOWN;
        }
        try {
            return Side.valueOf(side.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown trade side mapped to UNKNOWN. side={}", side);
            return Side.UNKNOWN;
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

    private RepairData repairDataFromExternalKey(String externalTradeKey) {
        if (externalTradeKey == null || externalTradeKey.isBlank()) {
            return null;
        }
        String[] parts = externalTradeKey.split("\\|", -1);
        if (parts.length < 8) {
            return null;
        }
        LocalDateTime boughtAt = parseTradovateTimestamp(parts[2]);
        LocalDateTime soldAt = parseTradovateTimestamp(parts[3]);
        BigDecimal buyPrice = parseDecimal(parts[5]);
        BigDecimal sellPrice = parseDecimal(parts[6]);
        if (boughtAt == null || soldAt == null || boughtAt.equals(soldAt)) {
            return null;
        }
        if (boughtAt.isBefore(soldAt)) {
            return new RepairData(Side.LONG, boughtAt, soldAt, buyPrice, sellPrice);
        }
        return new RepairData(Side.SHORT, soldAt, boughtAt, sellPrice, buyPrice);
    }

    private LocalDateTime parseTradovateTimestamp(String value) {
        String clean = cleanText(value);
        if (clean == null) {
            return null;
        }
        for (DateTimeFormatter formatter : TRADOVATE_TIMESTAMP_FORMATTERS) {
            try {
                return LocalDateTime.parse(clean, formatter);
            } catch (Exception ignored) {
            }
            // Try the next known Tradovate export shape.
        }
        try {
            return LocalDateTime.parse(clean);
        } catch (Exception ignored) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String value) {
        String clean = cleanText(value);
        if (clean == null) {
            return null;
        }
        try {
            return new BigDecimal(clean.replace(",", ""));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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


    private record RepairData(Side side, LocalDateTime entryTime, LocalDateTime exitTime, BigDecimal entryPrice, BigDecimal exitPrice) {
    }

    @java.lang.SuppressWarnings("all")
    private void applyContractFields(Trade trade, String submittedSymbol, LocalDateTime executionTime, boolean imported) {
        if (submittedSymbol == null || submittedSymbol.isBlank()) return;
        ContractSymbolNormalizationResult normalized = contractSymbolNormalizer.normalize(submittedSymbol, executionTime == null ? trade.getEntryTime() : executionTime);
        trade.setOriginalBrokerSymbol(submittedSymbol.trim());
        if (normalized.valid()) {
            trade.setSymbol(normalized.rootInstrument());
            trade.setContractSymbol(normalized.normalizedContractSymbol());
            trade.setContractSymbolSource(imported ? ContractSymbolSource.IMPORTED.name() : ContractSymbolSource.NORMALIZED.name());
        } else {
            String root = contractSymbolNormalizer.rootInstrument(submittedSymbol);
            if (root != null) {
                trade.setSymbol(root);
            }
        }
    }

    private void applyImportedContractFields(Trade trade, TradeImportRequest request, LocalDateTime executionTime, boolean trustContractSource) {
        String submittedSymbol = cleanText(request.symbol());
        String originalBrokerSymbol = cleanText(request.originalBrokerSymbol());
        String submittedContract = cleanText(request.contractSymbol());
        if (originalBrokerSymbol == null) {
            originalBrokerSymbol = submittedSymbol;
        }
        trade.setOriginalBrokerSymbol(originalBrokerSymbol);
        ContractSymbolNormalizationResult explicitContract = contractSymbolNormalizer.normalize(submittedContract, executionTime);
        ContractSymbolNormalizationResult brokerContract = contractSymbolNormalizer.normalize(originalBrokerSymbol, executionTime);
        ContractSymbolNormalizationResult normalized = explicitContract.valid() ? explicitContract : brokerContract;
        if (normalized.valid()) {
            trade.setSymbol(normalized.rootInstrument());
            trade.setContractSymbol(normalized.normalizedContractSymbol());
            trade.setContractSymbolSource(contractSymbolSourceForImport(request, trustContractSource));
            return;
        }
        String root = contractSymbolNormalizer.rootInstrument(submittedSymbol);
        if (root == null) {
            root = contractSymbolNormalizer.rootInstrument(originalBrokerSymbol);
        }
        if (root != null) {
            trade.setSymbol(root);
        }
    }

    private String contractSymbolSourceForImport(TradeImportRequest request, boolean trustContractSource) {
        if (!trustContractSource) {
            return ContractSymbolSource.NORMALIZED.name();
        }
        String source = cleanText(request.contractSymbolSource());
        return source == null ? ContractSymbolSource.IMPORTED.name() : source;
    }

    public TradeService(final TradeRepository tradeRepository, final TradingAccountService tradingAccountService, final ImportBatchRepository importBatchRepository, final UsageLimitService usageLimitService, final PerformanceCacheInvalidationService cacheInvalidationService, final TradeMemoryIndexService tradeMemoryIndexService, final FuturesContractSymbolNormalizer contractSymbolNormalizer) {
        this.tradeRepository = tradeRepository;
        this.tradingAccountService = tradingAccountService;
        this.importBatchRepository = importBatchRepository;
        this.usageLimitService = usageLimitService;
        this.cacheInvalidationService = cacheInvalidationService;
        this.tradeMemoryIndexService = tradeMemoryIndexService;
        this.contractSymbolNormalizer = contractSymbolNormalizer;
    }
}
