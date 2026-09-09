import { api } from "./axios";

export type ExecutionClassification =
  | "CLEAN_WIN"
  | "CLEAN_LOSS"
  | "UNDISCIPLINED_WIN"
  | "UNDISCIPLINED_LOSS"
  | "NOT_CLASSIFIED";

export type EvidenceConfidence = "VERY_LOW" | "LOW" | "MODERATE" | "HIGH";
export type PlanComparisonStatus = "ALIGNED" | "PARTIAL" | "VIOLATED" | "NOT_EVALUABLE";
export type PlanAlignmentStatus = "FULL_ALIGNMENT" | "PARTIAL_ALIGNMENT" | "PLAN_VIOLATED" | "NOT_EVALUABLE";
export type NoTradeEvaluationStatus = "SUCCESSFUL_NO_TRADE" | "PLAN_VIOLATED_BY_TRADE" | "INSUFFICIENT_EVIDENCE" | "NOT_APPLICABLE";
export type ProcessBreakpointStatus = "DETECTED" | "NOT_DETECTED" | "INSUFFICIENT_EVIDENCE";

export type ExecutionScoreComponent = {
  key: string;
  label: string;
  score: number;
  maxScore: number;
  evaluated: boolean;
  availablePoints: number;
  reasons: string[];
};

export type ExecutionScore = {
  tradeId: number;
  executionScore?: number | null;
  grade: string;
  classification: ExecutionClassification;
  availablePoints: number;
  maxPoints: number;
  evidenceCoveragePercent: number;
  evidenceConfidence: EvidenceConfidence;
  components: Record<string, ExecutionScoreComponent>;
  reasons: string[];
  detectedViolations: string[];
  processOutcomeMessage: string;
  preAnalysisComparison?: PreAnalysisComparison | null;
};

export type PlanComparisonItem = {
  type: string;
  status: PlanComparisonStatus;
  message: string;
};

export type PreAnalysisComparison = {
  status: PlanAlignmentStatus;
  planAvailable: boolean;
  preAnalysisId?: number | null;
  preAnalysisCreatedAt?: string | null;
  market?: string | null;
  comparisons: PlanComparisonItem[];
};

export type NoTradeEvaluation = {
  status: NoTradeEvaluationStatus;
  preAnalysisId?: number | null;
  message: string;
};

export type ProcessBreakpoint = {
  status: ProcessBreakpointStatus;
  previousTradeId?: number | null;
  currentTradeId?: number | null;
  timeToNextTradeMinutes?: number | null;
  baselineTimeBetweenTradesMinutes?: number | null;
  previousExecutionScore?: number | null;
  currentExecutionScore?: number | null;
  message: string;
};

export type LossResponse = {
  losingTradeId?: number | null;
  nextTradeId?: number | null;
  score?: number | null;
  classification: string;
  timeToNextTradeMinutes?: number | null;
  baselineTimeBetweenTradesMinutes?: number | null;
  riskChangePercent?: number | string | null;
  sizeChangePercent?: number | string | null;
  nextTradeExecutionScore?: number | null;
  flags: string[];
  message: string;
};

export type BehaviorPattern = {
  patternKey: string;
  label: string;
  occurrences: number;
  netR: number | string;
  winRate?: number | string | null;
  averageExecutionScore?: number | string | null;
  averageTimeBetweenTradesMinutes?: number | string | null;
  averageRiskChangePercent?: number | string | null;
  insight: string;
};

export type WeeklyExecutionFocus = {
  title: string;
  reason: string;
  impactR: number | string;
  goal: string;
  previousFrequency?: number | null;
  currentFrequency?: number | null;
  previousExecutionScore?: number | null;
  currentExecutionScore?: number | null;
  status: string;
  confidence: string;
};

export type ExecutionSessionSummary = {
  accountId: number;
  date: string;
  sessionExecutionScore?: number | null;
  evidenceCoveragePercent?: number | null;
  evidenceConfidence?: EvidenceConfidence | null;
  cleanWins: number;
  cleanLosses: number;
  undisciplinedWins: number;
  undisciplinedLosses: number;
  ruleAdherencePercent?: number | string | null;
  avoidableImpactR?: number | string | null;
  lossResponse?: LossResponse | null;
  processBreakPoint: string;
  processBreakpoint?: ProcessBreakpoint | null;
  preAnalysisComparison?: PreAnalysisComparison | null;
  noTradeEvaluation?: NoTradeEvaluation | null;
  trades: ExecutionScore[];
};

export type ExecutionAccountSummary = {
  accountId: number;
  executionScore?: number | null;
  evidenceCoveragePercent?: number | null;
  evidenceConfidence?: EvidenceConfidence | null;
  ruleAdherencePercent?: number | string | null;
  avoidableImpactR?: number | string | null;
  cleanWins: number;
  cleanLosses: number;
  undisciplinedWins: number;
  undisciplinedLosses: number;
  weeklyFocus?: WeeklyExecutionFocus | null;
  patterns: BehaviorPattern[];
};

export async function getTradeExecution(tradeId: number) {
  const response = await api.get<ExecutionScore>(`/execution/trades/${tradeId}`);
  return response.data;
}

export async function getExecutionSession(accountId: number, date: string) {
  const response = await api.get<ExecutionSessionSummary>(`/execution/accounts/${accountId}/sessions/${date}`);
  return response.data;
}

export async function getExecutionAccountSummary(accountId: number) {
  const response = await api.get<ExecutionAccountSummary>(`/execution/accounts/${accountId}/summary`);
  return response.data;
}

export async function getExecutionWeeklyFocus(accountId: number) {
  const response = await api.get<WeeklyExecutionFocus>(`/execution/accounts/${accountId}/weekly-focus`);
  return response.data;
}
