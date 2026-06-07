import { api } from "./axios";

export type FeatureKey =
  | "AI_REVIEW"
  | "DAILY_COACH"
  | "WEEKLY_COACH"
  | "TRADING_DNA"
  | "SESSION_REVIEW"
  | "TRADE_DETAIL"
  | "CALENDAR_VIEW"
  | "ANALYTICS_VIEW";

export type FeatureAdoption = {
  feature: FeatureKey;
  uniqueUsers: number;
  totalUsage: number;
  overallAdoptionRate: number;
  activeUserAdoptionRate: number;
};

export type FeatureTrendPoint = {
  date: string;
  count: number;
};

export type FeatureUsageSummary = {
  totalUsers: number;
  activeUsers30d: number;
  features: FeatureAdoption[];
  mostUsedFeature?: FeatureKey | null;
  leastUsedFeature?: FeatureKey | null;
};

export type FeatureUsageTrends = {
  dailyCoach: FeatureTrendPoint[];
  weeklyCoach: FeatureTrendPoint[];
  tradingDna: FeatureTrendPoint[];
  aiReview: FeatureTrendPoint[];
  sessionReview: FeatureTrendPoint[];
};

export type FeatureUsageUser = {
  userId: number;
  email: string;
  plan: "FREE" | "PRO";
  trades: number;
  featureUsageCounts: Partial<Record<FeatureKey, number>>;
};

export async function trackFeatureUsage(feature: FeatureKey): Promise<void> {
  await api.post("/feature-usage/track", { feature });
}

export async function getFeatureUsageSummary(): Promise<FeatureUsageSummary> {
  const res = await api.get<FeatureUsageSummary>("/admin/feature-usage/summary");
  return res.data;
}

export async function getFeatureUsageTrends(): Promise<FeatureUsageTrends> {
  const res = await api.get<FeatureUsageTrends>("/admin/feature-usage/trends");
  return res.data;
}

export async function getFeatureUsageUsers(): Promise<FeatureUsageUser[]> {
  const res = await api.get<FeatureUsageUser[]>("/admin/feature-usage/users");
  return res.data;
}
