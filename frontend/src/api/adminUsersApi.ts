import { api } from "./axios";
import type { AdminContactMessage } from "./adminContactApi";
import type { AdminRecentUpgrade } from "./adminDashboardApi";
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
  contactMessages: AdminContactMessage[];
  auditLogs: AdminRecentUpgrade[];
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
