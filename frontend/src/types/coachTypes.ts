import type { LeakResult } from "./leakDetectorTypes";

export type CoachSnapshotType = "DAILY" | "WEEKLY";
export type CoachSnapshotStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";

export type CoachSnapshot = {
  id: number;
  accountId: number;
  snapshotType: CoachSnapshotType;
  status: CoachSnapshotStatus;
  periodStart: string;
  periodEnd: string;

  totalTrades: number;
  netPnl: number;
  winRate: number;
  profitFactor: number;

  bestSetup: string;
  worstSetup: string;
  bestSession: string;
  worstSession: string;
  biggestMistake: string;

  deterministicSummaryJson?: string;
  aiResponseJson?: string;
  errorMessage?: string;
  topLeakToWatch?: LeakResult;
  selectedDaySummary?: string;
  primaryIssueToday?: string;
  setupDirectionReview?: string;
  longTradeCount?: number;
  shortTradeCount?: number;
  unknownTradeCount?: number;
  longPnl?: number;
  shortPnl?: number;
  worstTradeSide?: string;
  historicalPatternToWatch?: string;
  historicalPatternAppliesToday?: boolean;
  dataQuality?: string;
  dataQualityReason?: string;
  nextSessionRule?: string;
  aiGenerated?: boolean;
  noTradeDay?: boolean;
  noTradeWeek?: boolean;
  creditsConsumed?: number;

  createdAt: string;
  updatedAt: string;
};

export type CoachAiResponse = {
  headline: string;
  emotionalPattern: string;
  bestBehavior: string;
  biggestMistake: string;
  bestSetup: string;
  avoidNextSession: string;
  nextSessionFocus: string;
  riskNote: string;
  disciplineNote: string;
  oneRuleForNextSession: string;
};

export type CoachGenerateRequest = {
  accountId: number;
  periodStart: string;
  periodEnd: string;
  forceRefresh?: boolean;
};
