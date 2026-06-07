import { api } from "./axios";

export type RetentionStatus = "POWER_USER" | "ACTIVE" | "AT_RISK" | "INACTIVE";

export type RetentionPlanSegment = {
  dau: number;
  wau: number;
  mau: number;
};

export type RetentionSummary = {
  totalUsers: number;
  dau: number;
  wau: number;
  mau: number;
  dauMauRatio: number;
  newUsers30d: number;
  retention7d: number;
  retention30d: number;
  powerUsers: number;
  atRiskUsers: number;
  inactiveUsers: number;
  freeUsers: RetentionPlanSegment;
  proUsers: RetentionPlanSegment;
};

export type RetentionTrendPoint = {
  date: string;
  count: number;
};

export type RetentionTrends = {
  dailyActiveUsers: RetentionTrendPoint[];
  weeklyActiveUsers: RetentionTrendPoint[];
  monthlyActiveUsers: RetentionTrendPoint[];
};

export type RetentionCohort = {
  cohortMonth: string;
  users: number;
  retained7d: number;
  retained30d: number;
};

export type RetentionUser = {
  userId: number;
  email: string;
  name?: string | null;
  plan: "FREE" | "PRO";
  lastActive?: string | null;
  activityCount30d: number;
  daysSinceActivity: number;
  status: RetentionStatus;
};

export async function getRetentionSummary(): Promise<RetentionSummary> {
  const res = await api.get<RetentionSummary>("/admin/retention/summary");
  return res.data;
}

export async function getRetentionTrends(): Promise<RetentionTrends> {
  const res = await api.get<RetentionTrends>("/admin/retention/trends");
  return res.data;
}

export async function getRetentionCohorts(): Promise<RetentionCohort[]> {
  const res = await api.get<RetentionCohort[]>("/admin/retention/cohorts");
  return res.data;
}

export async function getRetentionUsers(): Promise<RetentionUser[]> {
  const res = await api.get<RetentionUser[]>("/admin/retention/users");
  return res.data;
}
