import { api } from "./axios";
import type { AdminContactMessage } from "./adminContactApi";
import type { AdminRecentUpgrade } from "./adminDashboardApi";
import type { TradingAccount } from "./accountApi";
import type { PagedResponse, PageParams } from "./pagination";

export type AdminUserRecord = {
  id: number;
  email: string;
  name: string;
  role: "USER" | "ADMIN";
  plan: "FREE" | "PRO";
  emailVerified: boolean;
  createdAt?: string | null;
  tradingAccountCount: number;
  tradeCount: number;
  aiReviewCount: number;
  dailyCoachUsage: number;
  weeklyCoachUsage: number;
  tradingDnaUsage: number;
  stripeCustomerId?: string | null;
  stripeSubscriptionId?: string | null;
  proUntil?: string | null;
  subscriptionStatus?: string | null;
  planSource?: string | null;
  usageThisMonth?: Record<string, number>;
  limits?: Record<string, number>;
  remaining?: Record<string, number>;
};

export type AdminUserDetail = {
  user: AdminUserRecord;
  accounts: TradingAccount[];
  contactMessages: AdminContactMessage[];
  auditLogs: AdminRecentUpgrade[];
};

export type UserFeatureEntitlement = {
  entitlementKey: "AI_PRE_ANALYSIS_BETA" | "PLAYBOOK_BACKTEST_BETA" | "MANUAL_BACKTEST_BETA";
  enabled: boolean;
  reason?: string | null;
  updatedAt?: string | null;
};

export type AdminTradeCleanupRequest = {
  userId: number;
  accountId: number;
  startDate: string;
  endDate: string;
  dryRun: boolean;
};

export type AdminTradeCleanupResponse = {
  userId: number;
  accountId: number;
  startDate: string;
  endDate: string;
  dryRun: boolean;
  tradesMatched: number;
  tradesDeleted: number;
  tradeReviewsDeleted: number;
  tradeAnalysesDeleted: number;
  tradeRuleViolationsDeleted: number;
  dailyDisciplineScoresDeleted: number;
  sessionReviewsDeleted: number;
  sessionCachesInvalidated: number;
  leakSnapshotsDeleted: number;
  tradingDnaSnapshotsDeleted: number;
  dailyCoachSnapshotsDeleted: number;
  weeklyCoachSnapshotsDeleted: number;
};

export async function getAdminUsers(params: PageParams = {}): Promise<PagedResponse<AdminUserRecord>> {
  const res = await api.get<PagedResponse<AdminUserRecord>>("/admin/users", { params });
  return res.data;
}

export async function getAdminUser(id: number): Promise<AdminUserDetail> {
  const res = await api.get<AdminUserDetail>(`/admin/users/${id}`);
  return res.data;
}

export async function searchAdminUsers(
  query: string,
  params: PageParams = {}
): Promise<PagedResponse<AdminUserRecord>> {
  const res = await api.get<PagedResponse<AdminUserRecord>>("/admin/users/search", { params: { query, ...params } });
  return res.data;
}

export async function upgradeUserToPro(userId: number, reason: string): Promise<AdminUserRecord> {
  const res = await api.post<AdminUserRecord>(`/admin/users/${userId}/upgrade-pro`, { reason });
  return res.data;
}

export async function downgradeUserToFree(userId: number, reason: string): Promise<AdminUserRecord> {
  const res = await api.post<AdminUserRecord>(`/admin/users/${userId}/downgrade-free`, { reason });
  return res.data;
}

export async function makeUserAdmin(userId: number, reason: string): Promise<AdminUserRecord> {
  const res = await api.post<AdminUserRecord>(`/admin/users/${userId}/make-admin`, { reason });
  return res.data;
}

export async function removeUserAdmin(userId: number, reason: string): Promise<AdminUserRecord> {
  const res = await api.post<AdminUserRecord>(`/admin/users/${userId}/remove-admin`, { reason });
  return res.data;
}

export async function getUserFeatureEntitlements(userId: number): Promise<UserFeatureEntitlement[]> {
  const res = await api.get<UserFeatureEntitlement[]>(`/admin/users/${userId}/feature-entitlements`);
  return res.data;
}

export async function setUserFeatureEntitlement(
  userId: number,
  payload: { entitlementKey: UserFeatureEntitlement["entitlementKey"]; enabled: boolean; reason?: string }
): Promise<UserFeatureEntitlement> {
  const res = await api.put<UserFeatureEntitlement>(`/admin/users/${userId}/feature-entitlements`, payload);
  return res.data;
}

export async function cleanupTradesRange(
  payload: AdminTradeCleanupRequest
): Promise<AdminTradeCleanupResponse> {
  const res = await api.delete<AdminTradeCleanupResponse>("/admin/trades/cleanup-range", {
    params: payload,
  });
  return res.data;
}
