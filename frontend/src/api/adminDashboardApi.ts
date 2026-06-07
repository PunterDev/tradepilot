import { api } from "./axios";
import type { AdminContactMessage } from "./adminContactApi";
import type { AdminUser } from "./adminApi";

export type AdminRecentUpgrade = {
  id: number;
  adminUserId: number;
  targetUserId: number;
  targetUserEmail?: string | null;
  targetUserName?: string | null;
  action: "UPGRADE_TO_PRO" | "DOWNGRADE_TO_FREE" | "ROLE_CHANGE";
  reason?: string | null;
  source?: "STRIPE" | "ADMIN" | "DEV_MOCK" | string | null;
  beforeValue?: string | null;
  afterValue?: string | null;
  createdAt?: string | null;
};

export type AdminUsageSummary = {
  totalAiReviews: number;
  dailyCoachUsage: number;
  weeklyCoachUsage: number;
  tradingDnaUsage: number;
  totalMonthlyAiUsage: number;
};

export type AdminDashboardSummary = {
  totalUsers: number;
  freeUsers: number;
  proUsers: number;
  adminUsers: number;
  verifiedUsers: number;
  unverifiedUsers: number;
  totalContactMessages: number;
  newContactMessages: number;
  openContactMessages: number;
  repliedContactMessages: number;
  failedEmailMessages: number;
  totalTradingAccounts: number;
  totalTrades: number;
  totalAiReviews: number;
  dailyCoachUsage: number;
  weeklyCoachUsage: number;
  tradingDnaUsage: number;
  recentUsers: AdminUser[];
  recentContactMessages: AdminContactMessage[];
  recentUpgrades: AdminRecentUpgrade[];
};

export async function getAdminDashboardSummary(): Promise<AdminDashboardSummary> {
  const res = await api.get<AdminDashboardSummary>("/admin/dashboard/summary");
  return res.data;
}

export async function getRecentAdminUsers(): Promise<AdminUser[]> {
  const res = await api.get<AdminUser[]>("/admin/dashboard/users/recent");
  return res.data;
}

export async function getRecentAdminContactMessages(): Promise<AdminContactMessage[]> {
  const res = await api.get<AdminContactMessage[]>("/admin/dashboard/contact-messages/recent");
  return res.data;
}

export async function getAdminUsageSummary(): Promise<AdminUsageSummary> {
  const res = await api.get<AdminUsageSummary>("/admin/dashboard/usage");
  return res.data;
}
