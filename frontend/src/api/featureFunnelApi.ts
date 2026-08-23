import { api } from "./axios";
import type { FeatureKey, FeatureUsageCategory, FeatureUsageEventType } from "./featureUsageApi";

export type FeatureFunnelKey =
  | "PLAYBOOK_ACTIVATION"
  | "JOURNEY_ENGAGEMENT"
  | "LEAK_DETECTOR"
  | "TRADE_REVIEW"
  | "SESSION_REVIEW"
  | "FREE_TO_PRO_UPGRADE"
  | "AI_CREDIT_PURCHASE";

export type FeatureFunnelConfidence = "INSUFFICIENT" | "LOW" | "MEDIUM" | "HIGH";
export type FeatureFunnelCalculationMode = "UNIQUE_USERS" | "FUNNEL_INSTANCES" | "SESSIONS";

export type FeatureFunnelFilters = {
  funnel?: FeatureFunnelKey;
  range?: "LAST_7_DAYS" | "LAST_30_DAYS" | "LAST_90_DAYS" | "CURRENT_MONTH" | "PREVIOUS_MONTH" | "CUSTOM";
  startDate?: string;
  endDate?: string;
  plan?: "FREE" | "PRO";
  category?: FeatureUsageCategory;
  excludeAdmins?: boolean;
  includeTestUsers?: boolean;
  calculationMode?: FeatureFunnelCalculationMode;
  conversionWindow?: number;
};

export type FeatureFunnelStep = {
  stepKey: string;
  displayName: string;
  description: string;
  feature: FeatureKey;
  action?: string | null;
  eventType?: FeatureUsageEventType | null;
  stepOrder: number;
  required: boolean;
  passive: boolean;
  usersReached: number;
  percentOfStarters: number;
  percentFromPreviousStep: number;
  dropoffCount: number;
  dropoffRate: number;
  medianTimeFromPreviousHours?: number | null;
  averageTimeFromPreviousHours?: number | null;
  planBreakdown: string;
  newReturningBreakdown: string;
  activePassiveClassification: string;
};

export type FeatureFunnelDropoff = {
  funnelKey: string;
  fromStepKey: string;
  toStepKey: string;
  transition: string;
  previousStepUsers: number;
  nextStepUsers: number;
  dropoffUsers: number;
  dropoffRate: number;
  medianDelayHours?: number | null;
  suggestedInvestigation: string;
  confidence: FeatureFunnelConfidence;
};

export type FeatureFunnelSummary = {
  funnelKey: FeatureFunnelKey;
  displayName: string;
  category: FeatureUsageCategory;
  description: string;
  eligibleUsers: number;
  enteredUsers: number;
  completedUsers: number;
  completionRate: number;
  eligibleCompletionRate: number;
  previousCompletionRate?: number | null;
  completionRateChange?: number | null;
  repeatRate7d: number;
  repeatRate14d: number;
  medianCompletionTimeHours?: number | null;
  averageCompletionTimeHours?: number | null;
  largestDropoffStep?: FeatureFunnelDropoff | null;
  confidence: FeatureFunnelConfidence;
  status: "ACTIVE" | "ARCHIVED";
  steps: FeatureFunnelStep[];
};

export type FeatureFunnelInsight = {
  type: string;
  title: string;
  funnelKey?: string | null;
  transition?: string | null;
  evidence: string;
  whyItMatters: string;
  suggestedInvestigation: string;
  priority: string;
  confidence: FeatureFunnelConfidence;
  caveat: string;
};

export type FeatureConversionAssociation = {
  funnelKey: string;
  behavior: string;
  exposedUsers: number;
  exposedConvertedUsers: number;
  exposedConversionRate: number;
  comparisonUsers: number;
  comparisonConversionRate: number;
  absoluteDifferencePoints?: number | null;
  relativeLift?: number | null;
  sampleSize: number;
  confidence: FeatureFunnelConfidence;
  caveat: string;
};

export type FeatureRetentionAssociation = {
  funnelKey: string;
  cohort: string;
  cohortSize: number;
  retained7dUsers: number;
  retention7dRate: number;
  retained14dUsers: number;
  retention14dRate: number;
  retained30dUsers: number;
  retention30dRate: number;
  absoluteDifferencePoints?: number | null;
  confidence: FeatureFunnelConfidence;
  caveat: string;
};

export type FeatureFunnelCohort = {
  cohortKey: string;
  cohortLabel: string;
  entrants: number;
  completed: number;
  completionRate: number;
  return7dRate: number;
  return14dRate: number;
  return30dRate: number;
};

export type FeatureFunnelUser = {
  userId: number;
  maskedEmail: string;
  plan: "FREE" | "PRO";
  stageReached: string;
  entryDate?: string | null;
  lastStageDate?: string | null;
  completionTimeHours?: number | null;
  lastActiveAt?: string | null;
  tradeBand: string;
  engagementStatus: string;
};

export type FeatureFunnelDashboard = {
  selectedStartDate: string;
  selectedEndDate: string;
  generatedAt: string;
  activeFilters: Record<string, string>;
  funnelSummaries: FeatureFunnelSummary[];
  topDropoff?: FeatureFunnelDropoff | null;
  highestCompletion?: FeatureFunnelSummary | null;
  fastestCompletion?: FeatureFunnelSummary | null;
  strongestRepeatUse?: FeatureFunnelSummary | null;
  conversionAssociations: FeatureConversionAssociation[];
  retentionAssociations: FeatureRetentionAssociation[];
  cohorts: FeatureFunnelCohort[];
  insights: FeatureFunnelInsight[];
  dataQualityWarnings: string[];
};

function filterParams(filters?: FeatureFunnelFilters) {
  const params: Record<string, string | boolean | number> = {};
  if (!filters) return params;
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== "" && value !== "ALL") params[key] = value as string | boolean | number;
  });
  return params;
}

export async function getFeatureFunnels(filters?: FeatureFunnelFilters): Promise<FeatureFunnelDashboard> {
  const res = await api.get<FeatureFunnelDashboard>("/admin/feature-funnels", { params: filterParams(filters) });
  return res.data;
}

export async function getFeatureFunnel(funnelKey: FeatureFunnelKey, filters?: FeatureFunnelFilters): Promise<FeatureFunnelSummary> {
  const res = await api.get<FeatureFunnelSummary>(`/admin/feature-funnels/${funnelKey}`, { params: filterParams(filters) });
  return res.data;
}

export async function getFeatureFunnelUsers(funnelKey: FeatureFunnelKey, filters?: FeatureFunnelFilters, page = 0, size = 25): Promise<FeatureFunnelUser[]> {
  const res = await api.get<FeatureFunnelUser[]>(`/admin/feature-funnels/${funnelKey}/users`, { params: { ...filterParams(filters), page, size } });
  return res.data;
}
