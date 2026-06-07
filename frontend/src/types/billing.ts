export type BillingUsage = {
  plan: "FREE" | "PRO";
  subscriptionStatus: "ACTIVE" | "TRIALING" | "PAST_DUE" | "CANCELED" | "NONE";
  limits: Record<string, number>;
  usageThisMonth: Record<string, number>;
  remaining: Record<string, number>;
  resetDate?: string;
};
