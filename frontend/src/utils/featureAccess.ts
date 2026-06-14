import type { BillingUsage } from "../types/billing";
import type { AuthUser } from "../api/authApi";

export type Feature =
  | "CSV_IMPORT"
  | "PNL_CALENDAR"
  | "BASIC_ANALYTICS"
  | "AI_TRADE_REVIEW"
  | "SESSION_REVIEW"
  | "DAILY_COACH"
  | "WEEKLY_COACH"
  | "TRADING_DNA"
  | "AI_LEAK_DETECTOR"
  | "ADVANCED_ANALYTICS";

const freeFeatures = new Set<Feature>([
  "CSV_IMPORT",
  "PNL_CALENDAR",
  "BASIC_ANALYTICS",
  "AI_TRADE_REVIEW",
]);

export function isPro(user?: AuthUser | null, usage?: BillingUsage | null) {
  return usage?.plan === "PRO" || user?.plan === "PRO";
}

export function hasFeature(feature: Feature, user?: AuthUser | null, usage?: BillingUsage | null) {
  return isPro(user, usage) || freeFeatures.has(feature);
}

export function creditsRemaining(usage?: BillingUsage | null) {
  return usage?.remaining?.AI_CREDITS ?? 0;
}

export function featureLabel(feature: Feature) {
  return feature
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}
