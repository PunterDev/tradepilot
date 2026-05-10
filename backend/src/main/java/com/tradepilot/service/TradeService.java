package com.tradepilot.service;

import com.tradepilot.dto.common.PagedResponse;
import com.tradepilot.dto.trade.*;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.User;
import com.tradepilot.enums.Market;
import com.tradepilot.enums.Side;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.repository.ImportBatchRepository;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.spec.TradeSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

import com.tradepilot.entity.ImportBatch;
import com.tradepilot.entity.TradingAccount;


@Service
@RequiredArgsConstructor
public class TradeService {
    private final TradeRepository tradeRepository;
    private final TradingAccountService tradingAccountService;
    private final ImportBatchRepository importBatchRepository;

    public TradeResponse createTrade(User user, TradeRequest request) {
        return mapToResponse(tradeRepository.save(mapToEntity(user, request)));
    }

    public PagedResponse<TradeResponse> getTrades(User user, TradeFilterRequest filter, int page, int size, String sortBy, String sortDir) {
        if (filter.getStartDate() != null && filter.getEndDate() != null && filter.getStartDate().isAfter(filter.getEndDate()))
            throw new IllegalArgumentException("startDate cannot be after endDate");
        String safeSortBy = sanitizeSortBy(sortBy);
        String safeSortDir = "asc".equalsIgnoreCase(sortDir) ? "asc" : "desc";
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 10 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.fromString(safeSortDir), safeSortBy));
        Page<Trade> tradePage = tradeRepository.findAll(TradeSpecification.withFilters(user, filter), pageable);
        List<TradeResponse> content = tradePage.getContent().stream().map(this::mapToResponse).toList();
        return PagedResponse.<TradeResponse>builder().content(content).page(tradePage.getNumber()).size(tradePage.getSize()).totalElements(tradePage.getTotalElements()).totalPages(tradePage.getTotalPages()).first(tradePage.isFirst()).last(tradePage.isLast()).sortBy(safeSortBy).sortDir(safeSortDir).build();
    }

    public TradeResponse getTradeById(User user, Long tradeId) {
        return tradeRepository.findByIdAndUser(tradeId, user).map(this::mapToResponse).orElseThrow(() -> new ResourceNotFoundException("Trade not found"));
    }

    public TradeResponse updateTrade(User user, Long tradeId, TradeRequest request) {
        Trade t = tradeRepository.findByIdAndUser(tradeId, user).orElseThrow(() -> new ResourceNotFoundException("Trade not found"));
        applyRequest(t, request);
        return mapToResponse(tradeRepository.save(t));
    }

    public List<TradeResponse> importTrades(User user, List<TradeImportRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        Long accountId = requests.get(0).accountId();

        if (accountId == null) {
            throw new IllegalArgumentException("accountId is required for import");
        }

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

            if (key != null && tradeRepository.existsByExternalTradeKey(key)) {
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

        return saved.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public void deleteTrade(User user, Long tradeId) {
        Trade t = tradeRepository.findByIdAndUser(tradeId, user).orElseThrow(() -> new ResourceNotFoundException("Trade not found"));
        tradeRepository.delete(t);
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

    private TradeResponse mapToResponse(Trade t) {
        return TradeResponse.builder().id(t.getId()).symbol(t.getSymbol()).market(Market.valueOf(t.getMarket())).tradeDate(t.getTradeDate()).entryTime(t.getEntryTime()).exitTime(t.getExitTime()).side(Side.valueOf(t.getSide())).setup(t.getSetup()).session(t.getSession()).entryPrice(t.getEntryPrice()).stopPrice(t.getStopPrice()).targetPrice(t.getTargetPrice()).exitPrice(t.getExitPrice()).quantity(t.getQuantity()).riskAmount(t.getRiskAmount()).pnlAmount(t.getPnlAmount()).pnlR(t.getPnlR()).outcome(t.getOutcome()).confidenceRating(t.getConfidenceRating()).executionRating(t.getExecutionRating()).followedPlan(t.getFollowedPlan()).mistakeTag(t.getMistakeTag()).notes(t.getNotes()).psychologyNotes(t.getPsychologyNotes()).lessonLearned(t.getLessonLearned()).screenshotUrl(t.getScreenshotUrl()).build();
    }
}
