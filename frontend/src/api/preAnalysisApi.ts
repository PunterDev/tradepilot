import { api, API_PREFIX } from "./axios";

export type PreAnalysisMarket = "ES" | "NQ";
export type PreAnalysisTimeframe = "M15" | "M5" | "M1";
export type SetupStatus = "NO_TRADE" | "WATCHING" | "SETUP_FORMING" | "ARMED" | "WAITING_FOR_RETRACE" | "ENTRY_VALID" | "INVALIDATED";
export type Bias = "BULLISH" | "BEARISH" | "NEUTRAL";
export type ConditionStatus = "SATISFIED" | "PARTIAL" | "WAITING" | "FAILED" | "NOT_APPLICABLE";
export type EntryAssessmentStatus = "WAIT" | "VALID" | "INVALID" | "NO_TRADE";
export type FeedbackReasonCode = "MISREAD_STRUCTURE" | "WRONG_LEVEL" | "MISSED_FVG" | "WRONG_BIAS" | "DID_NOT_FOLLOW_PLAYBOOK" | "OTHER";
export type MarketBias = "STRONG_BULLISH" | "BULLISH" | "NEUTRAL_BULLISH" | "NEUTRAL" | "NEUTRAL_BEARISH" | "BEARISH" | "STRONG_BEARISH";
export type EntryPosture = "FAVORABLE" | "ACCEPTABLE" | "POOR_TO_CHASE" | "WAIT_FOR_RETRACEMENT" | "WAIT_FOR_CONFIRMATION" | "NO_TRADE";

export type PreAnalysisAccess = {
  allowed: boolean;
  entitlementKey: string;
  label: string;
  badge: string;
};

export type TradingPlaybook = {
  id: number;
  name: string;
  description?: string | null;
  markets: string[];
  contextTimeframes: string[];
  setupTimeframes: string[];
  executionTimeframes: string[];
  setupSequence: string[];
  entryRules: string[];
  invalidationRules: string[];
  targetRules: string[];
  avoidRules: string[];
  customNotes?: string | null;
  active: boolean;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type DirectionalContext = {
  bias: Bias;
  confidenceLabel?: string | null;
  reasoning?: string | null;
};

export type ConditionEvidence = {
  status: ConditionStatus;
  evidence?: string | null;
};

export type PreAnalysisAiResult = {
  market: PreAnalysisMarket;
  timeframe: string;
  playbookName: string;
  marketState?: string | null;
  structureQualityScore?: number | null;
  directionalContext: DirectionalContext;
  setupStatus: SetupStatus;
  conditions: {
    break: ConditionEvidence;
    acceptance: ConditionEvidence;
    displacement: ConditionEvidence;
    fvgIfvg: ConditionEvidence;
    retrace: ConditionEvidence;
    confirmation: ConditionEvidence;
  };
  setupAlignmentScore?: number | null;
  importantLevels: { label: string; price?: number | null; role?: string | null }[];
  entryAssessment: {
    status: EntryAssessmentStatus;
    summary?: string | null;
    whatMustHappenNext: string[];
  };
  invalidation: {
    summary?: string | null;
    levels: number[];
  };
  potentialTargets: { price?: number | null; reason?: string | null }[];
  ruleViolations: string[];
  warnings: string[];
  narrative?: string | null;
  marketRead?: {
    marketBias?: MarketBias | string | null;
    marketState?: string | null;
    summary?: string | null;
    entryPosture?: EntryPosture | string | null;
  } | null;
  contextScores?: {
    structure?: QualitativeScore | null;
    location?: QualitativeScore | null;
    confirmation?: QualitativeScore | null;
    relativeStrength?: QualitativeScore | null;
  } | null;
  keyLevels?: KeyLevel[];
  bullishScenario?: TradeScenario | null;
  bearishScenario?: TradeScenario | null;
  noTradeScenario?: {
    condition?: string | null;
    reason?: string | null;
  } | null;
  tradePlan?: {
    preferredScenario?: string | null;
    waitFor?: string | null;
    confirmationNeeded?: string[];
    avoid?: string[];
    invalidation?: string | null;
    targetSummary?: string | null;
  } | null;
  crossMarket?: {
    available?: boolean | null;
    instrument?: string | null;
    comparisonInstrument?: string | null;
    leader?: string | null;
    laggard?: string | null;
    relativeStrengthState?: string | null;
    smtStatus?: string | null;
    interpretation?: string | null;
  } | null;
};

export type QualitativeScore = {
  score?: number | null;
  label?: string | null;
  reason?: string | null;
};

export type KeyLevel = {
  price?: number | null;
  label?: string | null;
  category?: string | null;
  role?: string | null;
  importance?: string | null;
  reason?: string | null;
};

export type TradeScenario = {
  status?: string | null;
  thesis?: string | null;
  entryZoneLow?: number | null;
  entryZoneHigh?: number | null;
  trigger?: string | null;
  confirmations?: string[];
  invalidation?: string | null;
  targets?: { price?: number | null; reason?: string | null; order?: number | null }[];
  quality?: string | null;
  notes?: string[];
};

export type PreAnalysisFeedback = {
  id: number;
  rating: 1 | -1;
  reasonCode?: FeedbackReasonCode | null;
  comment?: string | null;
  createdAt?: string | null;
};

export type PreAnalysisResult = {
  id: number;
  market: PreAnalysisMarket;
  timeframe: PreAnalysisTimeframe;
  displayTimeframe: string;
  playbookId: number;
  playbookName: string;
  imageUrl: string;
  traderNote?: string | null;
  setupStatus: SetupStatus;
  bias: Bias;
  setupAlignmentScore?: number | null;
  structureQualityScore?: number | null;
  analysis: PreAnalysisAiResult;
  feedback?: PreAnalysisFeedback | null;
  createdAt?: string | null;
};

export type PreAnalysisSummary = {
  id: number;
  market: PreAnalysisMarket;
  timeframe: PreAnalysisTimeframe;
  displayTimeframe: string;
  setupStatus: SetupStatus;
  bias: Bias;
  setupAlignmentScore?: number | null;
  feedbackRating?: number | null;
  createdAt?: string | null;
};

export function preAnalysisImageUrl(path: string) {
  if (/^https?:\/\//.test(path)) return path;
  const base = api.defaults.baseURL ?? "";
  return `${base.replace(/\/+$/, "")}${path.startsWith(API_PREFIX) ? path.slice(API_PREFIX.length) : path}`;
}

export async function getPreAnalysisAccess(): Promise<PreAnalysisAccess> {
  const res = await api.get<PreAnalysisAccess>("/pre-analysis/access");
  return res.data;
}

export async function getPreAnalysisPlaybook(): Promise<TradingPlaybook> {
  const res = await api.get<TradingPlaybook>("/pre-analysis/playbook");
  return res.data;
}

export async function updatePreAnalysisPlaybook(payload: Partial<TradingPlaybook>): Promise<TradingPlaybook> {
  const res = await api.put<TradingPlaybook>("/pre-analysis/playbook", payload);
  return res.data;
}

export async function submitPreAnalysis(payload: {
  market: PreAnalysisMarket;
  timeframe: PreAnalysisTimeframe;
  traderNote: string;
  screenshot: File;
}): Promise<PreAnalysisResult> {
  const form = new FormData();
  form.append("market", payload.market);
  form.append("timeframe", payload.timeframe);
  form.append("traderNote", payload.traderNote);
  form.append("screenshot", payload.screenshot);
  const res = await api.post<PreAnalysisResult>("/pre-analysis", form, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data;
}

export async function getPreAnalysisHistory(): Promise<PreAnalysisSummary[]> {
  const res = await api.get<PreAnalysisSummary[]>("/pre-analysis/history");
  return res.data;
}

export async function getPreAnalysis(id: number): Promise<PreAnalysisResult> {
  const res = await api.get<PreAnalysisResult>(`/pre-analysis/${id}`);
  return res.data;
}

export async function submitPreAnalysisFeedback(
  id: number,
  payload: { rating: 1 | -1; reasonCode?: FeedbackReasonCode | null; comment?: string }
): Promise<PreAnalysisFeedback> {
  const res = await api.post<PreAnalysisFeedback>(`/pre-analysis/${id}/feedback`, payload);
  return res.data;
}
