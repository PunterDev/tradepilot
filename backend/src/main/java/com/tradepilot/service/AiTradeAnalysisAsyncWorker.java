package com.tradepilot.service;

import com.tradepilot.dto.ai.AiTradeAnalysisRequest;
import com.tradepilot.dto.ai.AiTradeAnalysisResponse;
import com.tradepilot.dto.ai.TradeSnapshot;
import com.tradepilot.dto.discipline.DisciplineEvaluationResponse;
import com.tradepilot.entity.AiTradeAnalysis;
import com.tradepilot.entity.Trade;
import com.tradepilot.repository.AiTradeAnalysisRepository;
import com.tradepilot.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiTradeAnalysisAsyncWorker {

    private final AiTradeAnalysisRepository repository;
    private final TradeRepository tradeRepository;
    private final AiTradeAnalysisGeneratorService generatorService;
    private final AiTradeAnalysisMapper mapper;
    private final DisciplineEngineService disciplineEngineService;

    @Async("aiTaskExecutor")
    public void generateAsync(Long analysisId, AiTradeAnalysisRequest request, byte[] imageBytes, String mimeType) {
        try {
            log.info("Async AI trade analysis started. analysisId={}, tradeId={}", analysisId, request.getTradeId());

            AiTradeAnalysis entity = repository.findById(analysisId)
                    .orElseThrow(() -> new IllegalStateException("AI analysis not found: " + analysisId));

            AiTradeAnalysisResponse response = generatorService.generate(request, imageBytes, mimeType);
            DailyAggregate dailyAggregate = getDailyAggregate(entity.getUserId(), request);
            DisciplineEvaluationResponse discipline = disciplineEngineService.evaluateAndPersist(
                    entity.getUserId(),
                    request.getTradeId(),
                    request.getTrade(),
                    response,
                    dailyAggregate.tradeCount(),
                    dailyAggregate.pnl()
            );

            response.setDisciplineScore(discipline.getDisciplineScore());
            response.setViolations(discipline.getViolations());

            mapper.fillCompletedEntity(entity, response);
            repository.save(entity);

            log.info(
                    "Async AI trade analysis completed. analysisId={}, tradeId={}, grade={}",
                    analysisId,
                    request.getTradeId(),
                    response.getGrade()
            );
        } catch (Exception e) {
            log.error("Async AI trade analysis failed. analysisId={}, tradeId={}", analysisId, request.getTradeId(), e);
            repository.findById(analysisId).ifPresent(entity -> {
                entity.setStatus("FAILED");
                entity.setErrorMessage(e.getMessage());
                repository.save(entity);
            });
        }
    }

    private DailyAggregate getDailyAggregate(Long userId, AiTradeAnalysisRequest request) {
        if (request == null || request.getTradeId() == null) {
            return fromSnapshot(request == null ? null : request.getTrade());
        }

        return tradeRepository.findById(request.getTradeId())
                .filter(trade -> trade.getUser() != null && userId.equals(trade.getUser().getId()))
                .map(this::getPersistedDailyAggregate)
                .orElseGet(() -> fromSnapshot(request.getTrade()));
    }

    private DailyAggregate getPersistedDailyAggregate(Trade trade) {
        List<Trade> sameDayTrades = tradeRepository.findByUserAndTradeDateBetweenOrderByTradeDateDesc(
                trade.getUser(),
                trade.getTradeDate(),
                trade.getTradeDate()
        );

        if (trade.getAccount() != null) {
            Long accountId = trade.getAccount().getId();
            sameDayTrades = sameDayTrades.stream()
                    .filter(dayTrade -> dayTrade.getAccount() != null && accountId.equals(dayTrade.getAccount().getId()))
                    .toList();
        }

        double dailyPnl = sameDayTrades.stream()
                .map(Trade::getPnlAmount)
                .map(value -> value == null ? BigDecimal.ZERO : value)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doubleValue();

        return new DailyAggregate(sameDayTrades.size(), dailyPnl);
    }

    private DailyAggregate fromSnapshot(TradeSnapshot trade) {
        double pnl = trade == null || trade.getPnl() == null ? 0.0 : trade.getPnl();
        return new DailyAggregate(trade == null ? 0 : 1, pnl);
    }

    private record DailyAggregate(int tradeCount, double pnl) {
    }
}
