package com.tradepilot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.dto.ai.AiTradeAnalysisResponse;
import com.tradepilot.dto.discipline.RuleViolationDto;
import com.tradepilot.entity.AiTradeAnalysis;
import com.tradepilot.entity.TradeRuleViolation;
import com.tradepilot.repository.TradeRuleViolationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AiTradeAnalysisMapper {

    private final ObjectMapper objectMapper;
    private final TradeRuleViolationRepository violationRepository;

    public AiTradeAnalysisResponse toResponse(AiTradeAnalysis entity) {
        AiTradeAnalysisResponse response = new AiTradeAnalysisResponse();
        response.setId(entity.getId());
        response.setTradeId(entity.getTradeId());
        response.setStatus(entity.getStatus());
        response.setReviewVersion(entity.getReviewVersion());
        response.setReviewHash(entity.getReviewHash());
        response.setSummary(entity.getSummary());
        response.setGrade(entity.getGrade());
        response.setExecutionScore(entity.getExecutionScore());
        response.setDisciplineScore(entity.getDisciplineScore());
        response.setRiskScore(entity.getRiskScore());
        response.setMarketContext(entity.getMarketContext());
        response.setEntryAnalysis(entity.getEntryAnalysis());
        response.setExitAnalysis(entity.getExitAnalysis());
        response.setRiskAnalysis(entity.getRiskAnalysis());
        response.setPsychologyAnalysis(entity.getPsychologyAnalysis());
        response.setChartAnalysis(entity.getChartAnalysis());
        response.setMistakes(readList(entity.getMistakesJson()));
        response.setStrengths(readList(entity.getStrengthsJson()));
        response.setImprovementPlan(readList(entity.getImprovementPlanJson()));
        response.setTags(readList(entity.getTagsJson()));
        response.setViolations(violationRepository.findByUserIdAndTradeIdOrderByCreatedAtDesc(entity.getUserId(), entity.getTradeId()).stream().map(this::toDto).toList());
        response.setErrorMessage(entity.getErrorMessage());
        return response;
    }

    public void fillCompletedEntity(AiTradeAnalysis entity, AiTradeAnalysisResponse response) {
        entity.setStatus("COMPLETED");
        entity.setSummary(response.getSummary());
        entity.setGrade(response.getGrade());
        entity.setExecutionScore(response.getExecutionScore());
        entity.setDisciplineScore(response.getDisciplineScore());
        entity.setRiskScore(response.getRiskScore());
        entity.setMarketContext(response.getMarketContext());
        entity.setEntryAnalysis(response.getEntryAnalysis());
        entity.setExitAnalysis(response.getExitAnalysis());
        entity.setRiskAnalysis(response.getRiskAnalysis());
        entity.setPsychologyAnalysis(response.getPsychologyAnalysis());
        entity.setChartAnalysis(response.getChartAnalysis());
        entity.setMistakesJson(writeList(response.getMistakes()));
        entity.setStrengthsJson(writeList(response.getStrengths()));
        entity.setImprovementPlanJson(writeList(response.getImprovementPlan()));
        entity.setTagsJson(writeList(response.getTags()));
        entity.setErrorMessage(null);
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception e) {
            return "[]";
        }
    }

    private RuleViolationDto toDto(TradeRuleViolation entity) {
        return RuleViolationDto.builder()
                .id(entity.getId())
                .tradeId(entity.getTradeId())
                .ruleCode(entity.getRuleCode())
                .ruleTitle(entity.getRuleTitle())
                .category(entity.getCategory())
                .severity(entity.getSeverity())
                .reason(entity.getReason())
                .source(entity.getSource())
                .build();
    }
}
