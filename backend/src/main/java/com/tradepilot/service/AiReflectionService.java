package com.tradepilot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.ai.*;
import com.tradepilot.dto.ai.*;
import com.tradepilot.entity.*;
import com.tradepilot.repository.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AiReflectionService {
    private final TradeRepository tradeRepository;
    private final AiReflectionRepository aiReflectionRepository;
    private final ReflectionPromptBuilder promptBuilder;
    private final OpenAiReflectionService openAiReflectionService;
    private final ObjectMapper objectMapper;

    public WeeklyReflectionResponse generateWeeklyReflection(User user, WeeklyReflectionRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) throw new IllegalArgumentException("startDate cannot be after endDate");
        List<Trade> trades = tradeRepository.findByUserAndTradeDateBetweenOrderByTradeDateDesc(user, request.getStartDate(), request.getEndDate());
        if (trades.isEmpty()) throw new IllegalArgumentException("No trades found in the selected date range");
        String prompt = promptBuilder.buildWeeklyReflectionPrompt(trades);
        WeeklyReflectionStructuredResult result = openAiReflectionService.generateStructuredWeeklyReflection(prompt);
        String json = toJson(result);
        AiReflection saved = aiReflectionRepository.save(AiReflection.builder().user(user).startDate(request.getStartDate()).endDate(request.getEndDate()).promptText(prompt).responseText(json).build());
        return WeeklyReflectionResponse.builder().reflectionId(saved.getId()).summary(result.getSummary()).strengths(result.getStrengths()).mistakes(result.getMistakes()).psychologyPatterns(result.getPsychologyPatterns()).actionItems(result.getActionItems()).coachingScore(result.getCoachingScore()).rawResponseText(json).build();
    }

    private String toJson(WeeklyReflectionStructuredResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize AI reflection", e);
        }
    }

    @java.lang.SuppressWarnings("all")
    public AiReflectionService(final TradeRepository tradeRepository, final AiReflectionRepository aiReflectionRepository, final ReflectionPromptBuilder promptBuilder, final OpenAiReflectionService openAiReflectionService, final ObjectMapper objectMapper) {
        this.tradeRepository = tradeRepository;
        this.aiReflectionRepository = aiReflectionRepository;
        this.promptBuilder = promptBuilder;
        this.openAiReflectionService = openAiReflectionService;
        this.objectMapper = objectMapper;
    }
}
