import type { ChecklistType, PlaybookSetupPayload } from "../../api/playbookApi";

export const checklistTypes: ChecklistType[] = ["CONTEXT", "ENTRY", "INVALIDATION", "RISK", "MANAGEMENT", "POST_TRADE"];

export function emptySetup(accountId?: number | null): PlaybookSetupPayload {
  return {
    accountId,
    name: "",
    shortDescription: "",
    description: "",
    category: "CUSTOM",
    status: "DRAFT",
    direction: "BOTH",
    instruments: [],
    sessions: [],
    timeframes: [],
    tags: [],
    marketContext: [],
    riskRules: [],
    managementRules: [],
    postTradeRules: [],
    iconKey: "book",
    colorKey: "teal",
    imageUrl: null,
    checklistItems: [
      { checklistType: "CONTEXT", label: "Market context matches the setup", required: true, weight: 1, displayOrder: 0, active: true },
      { checklistType: "ENTRY", label: "Entry condition is present", required: true, weight: 1, displayOrder: 1, active: true },
      { checklistType: "RISK", label: "Risk is within planned limits", required: true, weight: 1, displayOrder: 2, active: true },
      { checklistType: "POST_TRADE", label: "Post-trade review is completed", required: false, weight: 1, displayOrder: 3, active: true },
    ],
  };
}

export function splitCsv(value: string) {
  return value.split(",").map((part) => part.trim()).filter(Boolean);
}

export function joinCsv(value?: string[] | null) {
  return (value ?? []).join(", ");
}

export function formatR(value?: number | null) {
  if (value == null) return "—";
  return `${value > 0 ? "+" : ""}${Number(value).toFixed(2)}R`;
}

export function formatMoney(value?: number | null) {
  if (value == null) return "—";
  return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(value);
}

export function formatPct(value?: number | null) {
  if (value == null) return "—";
  return `${Number(value).toFixed(1)}%`;
}
