package com.tradepilot.ai;

import org.springframework.stereotype.Service;

@Service
public class OpenAiReflectionService {
    public com.tradepilot.dto.ai.WeeklyReflectionStructuredResult generateStructuredWeeklyReflection(String prompt) {
        throw new IllegalStateException("Legacy weekly AI reflection is unavailable. Use Weekly Coach for production AI coaching.");
    }
}
