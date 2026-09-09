export type ProPreviewStatus = {
  feature: "WEEKLY_COACH";
  available: boolean;
  consumed: boolean;
  reserved: boolean;
  eligible: boolean;
  grantedAt?: string | null;
  consumedAt?: string | null;
};

export type FirstInsightSummary = {
  sourceFeature: string;
  insightId: number;
  accountId?: number | null;
  title: string;
  historicalImpact?: number | null;
  tradeCount?: number | null;
  averageImpact?: number | null;
  suggestedRule?: string | null;
  generatedAt: string;
};

export type ActivationStatus = {
  hasTradingAccount: boolean;
  hasImportedTrades: boolean;
  accountCount: number;
  tradeCount: number;
  firstImportCompleted: boolean;
  firstImportAt?: string | null;
  firstInsightAvailable: boolean;
  firstInsightGenerated: boolean;
  firstInsightAt?: string | null;
  activated: boolean;
  recommendedAccountId?: number | null;
  recommendedNextAction: string;
  minimumTradesForLeakAnalysis: number;
  additionalTradesNeededForLeakAnalysis: number;
  aiCreditsRemaining: number;
  aiCreditsMonthlyAllowance: number;
  aiCreditsMonthlyUsed: number;
  purchasedAiCreditsBalance: number;
  totalAiCreditsAvailable: number;
  aiAllowanceLow: boolean;
  featureCosts: Record<string, number>;
  proPreview: ProPreviewStatus;
  firstInsight?: FirstInsightSummary | null;
};
