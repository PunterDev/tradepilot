package com.tradepilot.ai.memory.vector;

import java.util.List;

public interface TradeEmbeddingService {
    List<Double> createEmbedding(String content);
}
