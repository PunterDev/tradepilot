import { api } from "./axios";
import type { PagedResponse, PageParams } from "./pagination";

export type AdminUser = {
  id: number;
  email: string;
  name: string;
  plan: "FREE" | "PRO";
  role: "USER" | "ADMIN";
  createdAt?: string;
  emailVerified: boolean;
  stripeCustomerId?: string | null;
  stripeSubscriptionId?: string | null;
  proUntil?: string | null;
  subscriptionStatus?: string;
  usageThisMonth?: Record<string, number>;
  limits?: Record<string, number>;
  remaining?: Record<string, number>;
};

export async function getAdminUsers(params: PageParams = {}): Promise<AdminUser[]> {
  const res = await api.get<PagedResponse<AdminUser>>("/admin/users", { params });
  return res.data.content;
}

export async function searchAdminUsers(query: string, params: PageParams = {}): Promise<AdminUser[]> {
  const res = await api.get<PagedResponse<AdminUser>>("/admin/users/search", {
    params: { query, ...params },
  });
  return res.data.content;
}

export async function upgradeUserToPro(
  userId: number,
  reason: string,
  proUntil?: string | null
): Promise<AdminUser> {
  const res = await api.post<AdminUser>(`/admin/users/${userId}/upgrade-pro`, {
    reason,
    proUntil: proUntil || null,
  });
  return res.data;
}

export async function downgradeUserToFree(
  userId: number,
  reason: string
): Promise<AdminUser> {
  const res = await api.post<AdminUser>(`/admin/users/${userId}/downgrade-free`, {
    reason,
  });
  return res.data;
}
