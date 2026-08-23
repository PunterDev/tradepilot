import { api } from "./api";

export type PlaybookStatus = "DRAFT" | "TESTING" | "VALIDATED" | "PAUSED" | "ARCHIVED";
export type PlaybookCategory = "CONTINUATION" | "REVERSAL" | "BREAKOUT" | "PULLBACK" | "LIQUIDITY_SWEEP" | "FVG" | "IFVG" | "OPENING_RANGE" | "TREND_FOLLOWING" | "MEAN_REVERSION" | "CUSTOM";
export type PlaybookDirection = "LONG" | "SHORT" | "BOTH";
export type ChecklistType = "CONTEXT" | "ENTRY" | "INVALIDATION" | "RISK" | "MANAGEMENT" | "POST_TRADE";
export type ChecklistResult = "MET" | "NOT_MET" | "NOT_APPLICABLE" | "UNKNOWN";
export type Grade = "A" | "B" | "C" | "D" | "F" | "UNGRADED";
export type PlaybookDetailState = "INCOMPLETE_SETUP" | "NO_LINKED_TRADES" | "EARLY_EVIDENCE" | "COMPARISON_ELIGIBLE" | "AI_REVIEW_ELIGIBLE" | "MATURE_SETUP";
export type PlaybookRequirement =
  | "MARKET_CONTEXT_REQUIRED"
  | "ENTRY_RULE_REQUIRED"
  | "INVALIDATION_RULE_REQUIRED"
  | "RISK_RULE_REQUIRED"
  | "MANAGEMENT_RULE_REQUIRED"
  | "POST_TRADE_RULE_REQUIRED"
  | "FIRST_LINKED_TRADE_REQUIRED"
  | "FIVE_REVIEWED_TRADES_REQUIRED"
  | "TEN_REVIEWED_TRADES_REQUIRED";

export type PlaybookChecklistItem = {
  id?: number | null;
  checklistType: ChecklistType;
  label: string;
  description?: string | null;
  required: boolean;
  weight: number;
  displayOrder: number;
  active: boolean;
  expectedValueType?: string | null;
  expectedValue?: unknown;
};

export type PlaybookSetupPayload = {
  accountId?: number | null;
  name: string;
  shortDescription?: string | null;
  description?: string | null;
  category: PlaybookCategory;
  status: PlaybookStatus;
  direction: PlaybookDirection;
  instruments: string[];
  sessions: string[];
  timeframes: string[];
  tags: string[];
  marketContext: string[];
  riskRules: string[];
  managementRules: string[];
  postTradeRules: string[];
  iconKey?: string | null;
  colorKey?: string | null;
  imageUrl?: string | null;
  checklistItems: PlaybookChecklistItem[];
};

export type PlaybookMetrics = {
  totalLinkedTrades: number;
  totalR?: number | null;
  netPnl?: number | null;
  winRate?: number | null;
  profitFactor?: number | null;
  averageR?: number | null;
  expectancy?: number | null;
  compliantTrades: number;
  noncompliantTrades: number;
  compliantAverageR?: number | null;
  noncompliantAverageR?: number | null;
  dataCompletenessScore?: number | null;
  lastUsedDate?: string | null;
};

export type PlaybookHealthScore = {
  score: number;
  confidence: string;
  definitionCompleteness: number;
  historicalEvidence: number;
  ruleAdherence: number;
  performanceConsistency: number;
  recentRelevance: number;
};

export type PlaybookBehaviorComparison = {
  checklistItemId: number;
  rule: string;
  metTradeCount: number;
  notMetTradeCount: number;
  metAverageR?: number | null;
  notMetAverageR?: number | null;
  metWinRate?: number | null;
  notMetWinRate?: number | null;
  confidence: string;
  directionallyMeaningful: boolean;
  interpretation: string;
};

export type PlaybookAiReview = {
  headline: string;
  summary: string;
  strongestBehavior: string;
  biggestSetupWeakness: string;
  biggestExecutionWeakness: string;
  mostBrokenRule: string;
  complianceInsight: string;
  performanceInsight: string;
  recentTrend: string;
  nextFocus: string;
  statusRecommendation: string;
  statusRecommendationReason: string;
  encouragement: string;
  confidenceExplanation: string;
  supportingEvidence: string[];
  limitations: string[];
};

export type PlaybookEligibility = {
  detailState: PlaybookDetailState;
  definitionComplete: boolean;
  completedDefinitionSections: number;
  totalDefinitionSections: number;
  linkedTradeCount: number;
  reviewedTradeCount: number;
  behaviorComparisonEligible: boolean;
  behaviorComparisonMinimumTrades: number;
  aiReviewEligible: boolean;
  aiReviewMinimumTrades: number;
  missingRequirements: PlaybookRequirement[];
  recommendedPrimaryAction: string;
};

export type PlaybookAnalysis = {
  setupId: number;
  metrics: PlaybookMetrics;
  healthScore: PlaybookHealthScore;
  behaviorComparisons: PlaybookBehaviorComparison[];
  aiReview?: PlaybookAiReview | null;
  cached: boolean;
  aiReviewStale: boolean;
  creditsConsumed: number;
  dataHash: string;
  generatedAt: string;
  eligibility: PlaybookEligibility;
};

export type PlaybookSetupSummary = {
  id: number;
  accountId?: number | null;
  accountName?: string | null;
  name: string;
  shortDescription?: string | null;
  category: PlaybookCategory;
  status: PlaybookStatus;
  direction: PlaybookDirection;
  instruments: string[];
  sessions: string[];
  tags: string[];
  linkedTradeCount: number;
  totalR?: number | null;
  netPnl?: number | null;
  winRate?: number | null;
  profitFactor?: number | null;
  complianceScore?: number | null;
  healthScore?: number | null;
  confidence?: string | null;
  lastUsedDate?: string | null;
  lastAnalyzedAt?: string | null;
  aiReviewStale: boolean;
};

export type PlaybookSetupDetail = PlaybookSetupSummary & {
  description?: string | null;
  timeframes: string[];
  marketContext: string[];
  riskRules: string[];
  managementRules: string[];
  postTradeRules: string[];
  checklistItems: PlaybookChecklistItem[];
  analysis: PlaybookAnalysis;
};

export type PlaybookOverview = {
  totalActiveSetups: number;
  draftSetups: number;
  testingSetups: number;
  validatedSetups: number;
  archivedSetups: number;
  recentSetups: PlaybookSetupSummary[];
};

export type PlaybookAssignmentPayload = {
  setupQualityGrade?: Grade;
  executionQualityGrade?: Grade;
  checklistResults?: Record<number, ChecklistResult>;
  ruleViolations?: string[];
  assignmentSource?: "MANUAL" | "BULK_MANUAL";
  reviewedByUser?: boolean;
  notes?: string;
};

export type PlaybookDraftResponse = {
  draft?: PlaybookSetupPayload | null;
  assumptions: string[];
  creditsConsumed: number;
  message: string;
};

export async function getPlaybookOverview(accountId?: number | null) {
  const response = await api.get<PlaybookOverview>("/playbook/overview", { params: { accountId: accountId ?? undefined } });
  return response.data;
}

export async function getPlaybookSetups(params: { accountId?: number | null; status?: PlaybookStatus | ""; search?: string } = {}) {
  const response = await api.get<PlaybookSetupSummary[]>("/playbook", { params });
  return response.data;
}

export async function getPlaybookSetup(setupId: number) {
  const response = await api.get<PlaybookSetupDetail>(`/playbook/${setupId}`);
  return response.data;
}

export async function createPlaybookSetup(payload: PlaybookSetupPayload) {
  const response = await api.post<PlaybookSetupDetail>("/playbook", payload);
  return response.data;
}

export async function updatePlaybookSetup(setupId: number, payload: PlaybookSetupPayload) {
  const response = await api.put<PlaybookSetupDetail>(`/playbook/${setupId}`, payload);
  return response.data;
}

export async function archivePlaybookSetup(setupId: number) {
  const response = await api.delete<PlaybookSetupDetail>(`/playbook/${setupId}`);
  return response.data;
}

export async function restorePlaybookSetup(setupId: number) {
  const response = await api.post<PlaybookSetupDetail>(`/playbook/${setupId}/restore`);
  return response.data;
}

export async function duplicatePlaybookSetup(setupId: number) {
  const response = await api.post<PlaybookSetupDetail>(`/playbook/${setupId}/duplicate`);
  return response.data;
}

export async function regeneratePlaybookAnalysis(setupId: number) {
  const response = await api.post<PlaybookAnalysis>(`/ai/playbook/${setupId}/regenerate`);
  return response.data;
}

export async function createPlaybookDraftFromDescription(description: string, accountId?: number | null) {
  const response = await api.post<PlaybookDraftResponse>("/ai/playbook/draft-from-description", { description, accountId });
  return response.data;
}
