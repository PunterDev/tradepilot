import { api } from "./api";

export type TradingMemoryStatus =
  | "NOT_AVAILABLE"
  | "NOT_ENOUGH_DATA"
  | "PENDING"
  | "READY"
  | "FAILED"
  | "DISABLED";

export type TradingMemoryMetrics = {
  sampleSize: number;
  winningTrades: number;
  losingTrades: number;
  averageResultR: number | null;
  medianResultR: number | null;
  averageMaeR: number | null;
  averageMfeR: number | null;
  averageExitEfficiency: number | null;
  mostCommonMistake: string | null;
  mostCommonMistakeCount: number;
  mostCommonEmotion: string | null;
  mostCommonEmotionCount: number;
};

export type CommonBehavior = {
  mistake: string | null;
  mistakeLabel: string | null;
  mistakeOccurrences: number;
  emotion: string | null;
  emotionLabel: string | null;
  emotionOccurrences: number;
  sampleSize: number;
};

export type RecurringPattern = {
  detected: boolean;
  patternType: string | null;
  title: string | null;
  occurrences: number;
  sampleSize: number;
  occurrencePercentage: number | null;
  averageRealizedR: number | null;
  averageMfeR: number | null;
  averageOpportunityR: number | null;
  totalEstimatedOpportunityR: number | null;
  supportingTradeIds: number[];
  message: string | null;
};

export type SimilarTrade = {
  tradeId: number;
  tradeDate: string | null;
  instrument: string | null;
  direction: string | null;
  setup: string | null;
  resultR: number | null;
  maeR: number | null;
  mfeR: number | null;
  mistake: string | null;
  emotion: string | null;
  similarity: number | null;
  summary: string | null;
};

export type TradingMemoryAiInsight = {
  source?: "DETERMINISTIC" | "AI" | "NONE";
  headline: string;
  explanation: string;
  lesson: string;
};

export type TradingMemoryResponse = {
  tradeId: number;
  status: TradingMemoryStatus;
  indexedAt: string | null;
  similarTradeCount: number;
  metrics: TradingMemoryMetrics | null;
  commonBehavior: CommonBehavior | null;
  recurringPattern: RecurringPattern | null;
  similarTrades: SimilarTrade[];
  aiInsight: TradingMemoryAiInsight | null;
  message: string | null;
  ready: boolean;
};

export type TradeMemoryStatusResponse = {
  tradeId: number;
  status: TradingMemoryStatus;
  indexedAt: string | null;
  message: string | null;
};

export async function getTradingMemory(tradeId: number, limit = 5) {
  const response = await api.get<TradingMemoryResponse>(`/v1/trades/${tradeId}/trading-memory`, {
    params: { limit },
  });
  return response.data;
}

export async function regenerateTradingMemory(tradeId: number, force = true) {
  const response = await api.post<TradeMemoryStatusResponse>(
    `/v1/trades/${tradeId}/trading-memory/regenerate`,
    null,
    { params: { force } }
  );
  return response.data;
}
