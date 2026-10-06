export type LeakType =
  | "DAY_OF_WEEK"
  | "SESSION_TIME"
  | "SETUP"
  | "CONFIDENCE"
  | "EMOTION"
  | "OVERTRADING"
  | "POST_LOSS_OVERTRADING"
  | "SIZE_ESCALATION_AFTER_LOSS"
  | "LONG_SHORT_BIAS"
  | "LARGE_LOSS"
  | "BREAKEVEN_OR_EARLY_EXIT";

export type LeakSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type EvidenceStrength = "INSUFFICIENT" | "WEAK" | "MODERATE" | "STRONG";
export type BehavioralMetrics = { sampleSize: number; winRate: number; expectancyR?: number | null; averagePnl: number };
export type BehavioralCounterfactual = { applicable: boolean; supported: boolean; skippedTradeCount: number; skippedWinnerCount: number; skippedLoserCount: number; lossesAvoided: number; winnersForgone: number; actualPnl: number; counterfactualPnl: number; netHistoricalImpact: number };
export type BehavioralFinding = { qualified: boolean; reasonNotQualified?: string | null; occurrenceCount: number; tradeSampleSize: number; sessionSampleSize: number; evidenceStrength: EvidenceStrength; baseline: BehavioralMetrics; triggered: BehavioralMetrics; counterfactual: BehavioralCounterfactual; engineVersion: string; ruleVersion: string; threshold?: number | null };

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
  behavioralFinding?: BehavioralFinding | null;
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
