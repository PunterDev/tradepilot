package com.tradepilot.ai.memory.service;

public record TradeMemoryDocument(
        int memoryVersion,
        String content,
        String contentHash,
        String setupNormalized,
        String emotionNormalized,
        String mistakeNormalized,
        String setupSource,
        String emotionSource,
        String mistakeSource,
        String summary
) {
}
