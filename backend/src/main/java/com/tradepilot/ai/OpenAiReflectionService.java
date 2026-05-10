package com.tradepilot.ai;

import com.tradepilot.dto.ai.WeeklyReflectionStructuredResult;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class OpenAiReflectionService {
    public WeeklyReflectionStructuredResult generateStructuredWeeklyReflection(String prompt) {
        // Compile-safe placeholder. Replace this method with the official OpenAI Java SDK call when ready.
        return WeeklyReflectionStructuredResult.builder()
                .summary("AI integration placeholder. Add OPENAI_API_KEY and wire the SDK call in OpenAiReflectionService.")
                .strengths(List.of("Backend AI workflow is wired end-to-end."))
                .mistakes(List.of("Live OpenAI call is intentionally not enabled in this compile-ready starter."))
                .psychologyPatterns(List.of("Add real model analysis after your CRUD and stats flow is stable."))
                .actionItems(List.of("Replace the placeholder service with a typed OpenAI structured-output call."))
                .coachingScore(5)
                .build();
    }
}
