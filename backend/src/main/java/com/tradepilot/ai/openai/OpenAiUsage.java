package com.tradepilot.ai.openai;

public record OpenAiUsage(int inputTokens, int cachedInputTokens, int outputTokens, int reasoningTokens, int totalTokens) {
    public static OpenAiUsage empty() {
        return new OpenAiUsage(0, 0, 0, 0, 0);
    }
}
