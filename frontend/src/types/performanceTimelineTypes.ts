export type TimelineDataStatus = "READY" | "LIMITED_DATA" | "NO_ACTIVITY";
export type PerformanceTrend = "IMPROVING" | "STABLE" | "DECLINING" | "LIMITED_DATA";
export type ScoreConfidence = "LOW" | "MEDIUM" | "HIGH";
export type ComparisonResult = "IMPROVED" | "DECLINED" | "UNCHANGED";

export type PerformanceCategoryScores = {
  discipline?: number | null;
  riskManagement?: number | null;
  execution?: number | null;
  consistency?: number | null;
  psychology?: number | null;
};

export type PerformanceTimelineMetrics = {
  totalTrades: number;
  netPnl?: number | null;
  totalR?: number | null;
  averageR?: number | null;
  winRate?: number | null;
  profitFactor?: number | null;
  averageWinnerR?: number | null;
  averageLoserR?: number | null;
  largestWinR?: number | null;
  largestLossR?: number | null;
  winningDays?: number;
  losingDays?: number;
  profitableTradingDayPercent?: number | null;
  longTradeCount?: number;
  shortTradeCount?: number;
  unknownDirectionCount?: number;
  bestPerformingSetup?: string | null;
  worstPerformingSetup?: string | null;
  overtradingDays?: number;
  fomoTradeCount?: number;
  revengeTradeCount?: number;
  averageTradesPerActiveDay?: number | null;
  largestDailyLossR?: number | null;
  emotionalTradeCount?: number;
  notesCompletionPercent?: number | null;
  metadataCompletionPercent?: number | null;
  greenWeekCount?: number;
  redWeekCount?: number;
  dailyRVolatility?: number | null;
};

export type PerformanceMetricComparison = {
  metric: string;
  previousValue?: number | null;
  currentValue?: number | null;
  absoluteChange?: number | null;
  percentageChange?: number | null;
  direction: ComparisonResult;
  higherIsBetter: boolean;
};

export type PerformanceTimelineNarrative = {
  theme?: string | null;
  summary?: string | null;
  biggestImprovement?: string | null;
  biggestLeak?: string | null;
  emergingRisk?: string | null;
  bestBehavior?: string | null;
  nextFocus?: string | null;
  encouragement?: string | null;
  supportingEvidence?: string[];
  confidenceExplanation?: string | null;
  aiGenerated?: boolean;
};

export type PerformanceMilestone = {
  milestoneType: string;
  title: string;
  earnedDate: string;
  periodStart?: string | null;
  periodEnd?: string | null;
  evidence?: string[];
};

export type PerformanceTimelineMonth = {
  month: string;
  year: number;
  periodStart: string;
  periodEnd: string;
  status: TimelineDataStatus;
  tradeCount: number;
  metrics?: PerformanceTimelineMetrics | null;
  comparison?: PerformanceMetricComparison[];
  categoryScores?: PerformanceCategoryScores | null;
  evolutionScore?: number | null;
  scoreChange?: number | null;
  scoreConfidence?: ScoreConfidence | null;
  narrative?: PerformanceTimelineNarrative | null;
  milestones?: PerformanceMilestone[];
  message?: string | null;
};

export type PerformanceTimelineResponse = {
  accountId: number;
  generatedAt: string;
  fromDate?: string | null;
  toDate?: string | null;
  overallTrend: PerformanceTrend;
  currentEvolutionScore?: number | null;
  previousEvolutionScore?: number | null;
  scoreChange?: number | null;
  highestEvolutionScore?: number | null;
  strongestCategory?: string | null;
  weakestCategory?: string | null;
  latestFocus?: string | null;
  months: PerformanceTimelineMonth[];
  milestones: PerformanceMilestone[];
  creditCost: number;
  cached: boolean;
};
