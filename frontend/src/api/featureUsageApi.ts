import { api } from "./axios";

export type FeatureKey =
  | "AI_REVIEW"
  | "DAILY_COACH"
  | "WEEKLY_COACH"
  | "TRADING_DNA"
  | "AI_LEAK_DETECTOR"
  | "PERFORMANCE_TIMELINE"
  | "PLAYBOOK"
  | "SESSION_REVIEW"
  | "TRADE_DETAIL"
  | "CALENDAR_VIEW"
  | "ANALYTICS_VIEW";

export type FeatureUsageCategory =
  | "JOURNALING"
  | "ANALYTICS"
  | "AI_COACHING"
  | "PERFORMANCE"
  | "PLAYBOOK"
  | "BILLING"
  | "ACCOUNT_MANAGEMENT"
  | "ADMIN"
  | "OTHER";

export type FeatureUsageEventType =
  | "PAGE_VIEW"
  | "ANALYTICS_VIEW"
  | "AI_GENERATION"
  | "AI_CACHED_READ"
  | "CREDIT_PURCHASE"
  | "IMPORT"
  | "EXPORT"
  | "TRADE_UPDATE"
  | "USER_ACTION";

export type FeatureUsageRange =
  | "LAST_7_DAYS"
  | "LAST_30_DAYS"
  | "LAST_90_DAYS"
  | "CURRENT_MONTH"
  | "PREVIOUS_MONTH"
  | "CUSTOM";

export type FeatureUsageActivity = "ALL" | "ACTIVE" | "PASSIVE" | "AI";

export type FeatureUsageFilters = {
  range?: FeatureUsageRange;
  startDate?: string;
  endDate?: string;
  plan?: "FREE" | "PRO";
  category?: FeatureUsageCategory;
  eventType?: FeatureUsageEventType;
  excludeAdmins?: boolean;
  activity?: FeatureUsageActivity;
};

export type FeatureUsageMetadata = {
  key: FeatureKey;
  displayName: string;
  shortName: string;
  description: string;
  category: FeatureUsageCategory;
  eventType: FeatureUsageEventType;
  coreFeature: boolean;
  aiFeature: boolean;
  paidAction: boolean;
  passiveEvent: boolean;
  sortOrder: number;
  iconKey: string;
  colorKey: string;
};

export type FeatureAdoption = {
  feature: FeatureKey;
  displayName: string;
  shortName: string;
  description: string;
  category: FeatureUsageCategory;
  eventType: FeatureUsageEventType;
  coreFeature: boolean;
  aiFeature: boolean;
  paidAction: boolean;
  passiveEvent: boolean;
  knownFeature: boolean;
  uniqueUsers: number;
  totalUsage: number;
  activeEventCount: number;
  passiveEventCount: number;
  aiGenerationCount: number;
  overallAdoptionRate: number;
  activeUserAdoptionRate: number;
  eventsPerUser: number;
  repeatUsageRate: number;
  previousPeriodUsers: number;
  previousPeriodEvents: number;
  userGrowthPercent: number;
  eventGrowthPercent: number;
  engagementScore: number;
  opportunityScore: number;
  status: string;
};

export type FeatureTrendPoint = {
  date: string;
  count: number;
};

export type FeatureUsageSummary = {
  totalUsers: number;
  activeUsers30d: number;
  activeUsers: number;
  totalEvents: number;
  uniqueFeaturesUsed: number;
  avgEventsPerActiveUser: number;
  avgUniqueFeaturesPerActiveUser: number;
  features: FeatureAdoption[];
  mostUsedFeature?: FeatureKey | null;
  leastUsedFeature?: FeatureKey | null;
  highestVolumeFeature?: FeatureKey | null;
  highestAdoptionFeature?: FeatureKey | null;
  lowestAdoptionFeature?: FeatureKey | null;
  insights: FeatureUsageInsight[];
  dataQuality: FeatureUsageDataQualityIssue[];
  metadata: FeatureUsageMetadata[];
  selectedStartDate: string;
  selectedEndDate: string;
  generatedAt: string;
};

export type FeatureUsageTrends = {
  dailyCoach: FeatureTrendPoint[];
  weeklyCoach: FeatureTrendPoint[];
  tradingDna: FeatureTrendPoint[];
  aiReview: FeatureTrendPoint[];
  sessionReview: FeatureTrendPoint[];
  series: Partial<Record<FeatureKey, FeatureTrendPoint[]>>;
  fastestGrowingFeature?: FeatureKey | null;
  largestDeclineFeature?: FeatureKey | null;
  mostStableFeature?: FeatureKey | null;
};

export type FeatureUsageInsight = {
  type: string;
  feature?: FeatureKey | null;
  title: string;
  evidence: string;
  whyItMatters: string;
  suggestedInvestigation: string;
  confidence: string;
  priority: string;
};

export type FeatureUsageDataQualityIssue = {
  issue: string;
  eventCount: number;
  earliestOccurrence?: string | null;
  latestOccurrence?: string | null;
  suggestedFix: string;
};

export type FeatureUsageUser = {
  userId: number;
  email: string;
  maskedEmail: string;
  plan: "FREE" | "PRO";
  trades: number;
  activeFeatures: number;
  activeActions: number;
  passiveEvents: number;
  aiGenerations: number;
  totalEvents: number;
  lastActiveAt?: string | null;
  engagementLevel: string;
  featureUsageCounts: Partial<Record<FeatureKey, number>>;
};

export async function trackFeatureUsage(feature: FeatureKey): Promise<void> {
  await api.post("/feature-usage/track", { feature });
}

function filterParams(filters?: FeatureUsageFilters) {
  const params: Record<string, string | boolean> = {};
  if (!filters) return params;
  if (filters.range) params.range = filters.range;
  if (filters.startDate) params.startDate = filters.startDate;
  if (filters.endDate) params.endDate = filters.endDate;
  if (filters.plan) params.plan = filters.plan;
  if (filters.category) params.category = filters.category;
  if (filters.eventType) params.eventType = filters.eventType;
  if (typeof filters.excludeAdmins === "boolean") params.excludeAdmins = filters.excludeAdmins;
  if (filters.activity && filters.activity !== "ALL") params.activity = filters.activity;
  return params;
}

export async function getFeatureUsageSummary(filters?: FeatureUsageFilters): Promise<FeatureUsageSummary> {
  const res = await api.get<FeatureUsageSummary>("/admin/feature-usage/summary", { params: filterParams(filters) });
  return res.data;
}

export async function getFeatureUsageTrends(filters?: FeatureUsageFilters): Promise<FeatureUsageTrends> {
  const res = await api.get<FeatureUsageTrends>("/admin/feature-usage/trends", { params: filterParams(filters) });
  return res.data;
}

export async function getFeatureUsageUsers(filters?: FeatureUsageFilters): Promise<FeatureUsageUser[]> {
  const res = await api.get<FeatureUsageUser[]>("/admin/feature-usage/users", { params: filterParams(filters) });
  return res.data;
}
