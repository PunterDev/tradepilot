package com.tradepilot;

import com.tradepilot.ai.OpenAiReflectionService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LegacyAiPlaceholderTests {
    @Test
    void legacyWeeklyReflectionDoesNotReturnPlaceholderAiOutput() {
        OpenAiReflectionService service = new OpenAiReflectionService();

        assertThatThrownBy(() -> service.generateStructuredWeeklyReflection("review these trades"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Legacy weekly AI reflection is unavailable")
                .hasMessageNotContaining("AI integration placeholder");
    }
}
