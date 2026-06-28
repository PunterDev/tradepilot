import { api } from "./axios";
import type { BillingUsage } from "../types/billing";

export async function getBillingUsage(): Promise<BillingUsage> {
  const response = await api.get<BillingUsage>("/billing/usage");
  return response.data;
}

export type BillingCycle = "monthly" | "yearly";
export type CreditPackId = "STARTER_25" | "POWER_75" | "ELITE_200";

export async function createCheckoutSession(billingCycle: BillingCycle = "monthly"): Promise<{ checkoutUrl: string }> {
  const response = await api.post<{ checkoutUrl: string }>("/billing/create-checkout-session", null, {
    params: { billingCycle },
  });
  return response.data;
}

export async function createPortalSession(): Promise<{ portalUrl: string }> {
  const response = await api.post<{ portalUrl: string }>("/billing/create-portal-session");
  return response.data;
}

export async function createCreditPackCheckoutSession(packId: CreditPackId): Promise<{ checkoutUrl: string }> {
  const response = await api.post<{ checkoutUrl: string }>("/billing/create-credit-pack-checkout-session", { packId });
  return response.data;
}
