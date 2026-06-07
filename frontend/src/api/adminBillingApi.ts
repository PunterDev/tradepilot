import { api } from "./axios";
import type { AdminRecentUpgrade } from "./adminDashboardApi";

export type AdminBillingSummary = {
  totalUsers: number;
  freeUsers: number;
  proUsers: number;
  stripePayingSubscribers: number;
  manualProUsers: number;
  conversionRate: number;
  paidConversionRate: number;
  estimatedMrr: number;
  estimatedArr: number;
  potentialMrr: number;
  potentialArr: number;
  manualUpgrades: number;
  stripeSubscriptions: number;
  stripeCheckoutConfigured: boolean;
  stripeWebhookConfigured: boolean;
  monthlyPriceIdConfigured: boolean;
  yearlyPriceIdConfigured: boolean;
  recentPlanChanges: AdminRecentUpgrade[];
};

export async function getAdminBillingSummary(): Promise<AdminBillingSummary> {
  const res = await api.get<AdminBillingSummary>("/admin/billing/summary");
  return res.data;
}
