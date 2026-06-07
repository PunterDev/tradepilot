package com.tradepilot.service;

import com.tradepilot.discipline.*;
import com.tradepilot.dto.ai.AiTradeAnalysisResponse;
import com.tradepilot.dto.ai.TradeSnapshot;
import com.tradepilot.dto.discipline.*;
import com.tradepilot.entity.*;
import com.tradepilot.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DisciplineEngineService {
    private final DeterministicRuleDetector deterministicRuleDetector;
    private final DisciplineScoreCalculator scoreCalculator;
    private final TradeRuleViolationRepository violationRepository;
    private final DailyDisciplineScoreRepository dailyScoreRepository;
    private final TradeService tradeService;

    @Transactional
    public DisciplineEvaluationResponse evaluateAndPersist(Long userId, Long tradeId, TradeSnapshot trade, AiTradeAnalysisResponse aiAnalysis, int dailyTradeCount, double dailyPnl) {
        LocalDate tradeDate = extractTradeDate(trade);

        violationRepository.deleteByUserIdAndTradeIdAndSource(userId, tradeId, "SYSTEM");
        violationRepository.deleteByUserIdAndTradeIdAndSource(userId, tradeId, "AI");

        List<RuleViolationDto> violations = new ArrayList<>();
        violations.addAll(deterministicRuleDetector.detect(tradeId, trade, dailyTradeCount, dailyPnl));

        if (aiAnalysis != null && aiAnalysis.getViolations() != null) {
            violations.addAll(aiAnalysis.getViolations());
        }

        for (RuleViolationDto v : violations) {
            violationRepository.save(toEntity(v, userId, tradeId, tradeDate));
        }

        int score = scoreCalculator.calculate(violations);

        return DisciplineEvaluationResponse.builder()
                .tradeId(tradeId)
                .tradeDate(tradeDate.toString())
                .disciplineScore(score)
                .cleanTrade(scoreCalculator.isClean(violations))
                .violationCount(violations.size())
                .violations(violations)
                .build();
    }

    public List<RuleViolationDto> getViolationsForTrade(User user, Long tradeId) {
        tradeService.getOwnedTrade(user, tradeId);
        return violationRepository.findByUserIdAndTradeIdOrderByCreatedAtDesc(user.getId(), tradeId).stream().map(this::toDto).toList();
    }

    private TradeRuleViolation toEntity(RuleViolationDto dto, Long userId, Long tradeId, LocalDate tradeDate) {
        return TradeRuleViolation.builder()
                .userId(userId)
                .tradeId(tradeId)
                .ruleCode(dto.getRuleCode())
                .ruleTitle(dto.getRuleTitle())
                .category(dto.getCategory())
                .severity(dto.getSeverity())
                .reason(dto.getReason())
                .source(dto.getSource() == null ? "AI" : dto.getSource())
                .tradeDate(tradeDate)
                .build();
    }

    private RuleViolationDto toDto(TradeRuleViolation e) {
        return RuleViolationDto.builder()
                .id(e.getId()).tradeId(e.getTradeId()).ruleCode(e.getRuleCode()).ruleTitle(e.getRuleTitle())
                .category(e.getCategory()).severity(e.getSeverity()).reason(e.getReason()).source(e.getSource())
                .build();
    }

    private LocalDate extractTradeDate(TradeSnapshot trade) {
        if (trade == null || trade.getEntryTime() == null) return LocalDate.now();
        String value = trade.getEntryTime();
        try {
            return OffsetDateTime.parse(value).toLocalDate();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(value).toLocalDate();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (Exception ignored) {
        }
        return LocalDate.now();
    }
}
