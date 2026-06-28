export type LeakType =
  | "DAY_OF_WEEK"
  | "SESSION_TIME"
  | "SETUP"
  | "CONFIDENCE"
  | "EMOTION"
  | "OVERTRADING"
  | "LONG_SHORT_BIAS"
  | "LARGE_LOSS"
  | "BREAKEVEN_OR_EARLY_EXIT";

export type LeakSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type LeakResult = {
  leakType: LeakType;
  title: string;
  summary: string;
  description: string;
  tradeCount: number;
  winRate: number;
  totalPnl: number;
  avgPnl: number;
  estimatedMonthlyImpact: number;
  totalR?: number | null;
  avgR?: number | null;
  estimatedMonthlyImpactR?: number | null;
  hasValidRMultiple?: boolean;
  confidenceScore: number;
  severity: LeakSeverity;
  recommendedFix: string;
  relatedTradeCount?: number | null;
  relatedTradeIds: number[];
  generatedAt: string;
};

export type LeakDetectorResponse = {
  id?: number;
  accountId: number;
  cached: boolean;
  aiGenerated?: boolean;
  noTrades?: boolean;
  limitedData?: boolean;
  creditsConsumed?: number;
  totalTrades: number;
  sampleTradeCount?: number;
  dataHash: string;
  leaks: LeakResult[];
  message: string;
  biggestLeakTitle?: string | null;
  biggestLeakImpact?: number | null;
  biggestLeakFix?: string | null;
  totalEstimatedMonthlyImpact?: number | null;
  highestConfidence?: number | null;
  generatedAt: string;
};
