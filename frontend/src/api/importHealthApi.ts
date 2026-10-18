import { api } from "./axios";

export type ImportHealthRange = "TODAY" | "LAST_7_DAYS" | "LAST_30_DAYS";
export type ImportHealthSummary = {
  startDate: string; endDate: string; importAttempts: number; completedImports: number; failedImports: number;
  successRate: number | null; previewFailures: number; tradesSaved: number; duplicates: number; firstImports: number;
  parserBreakdown: { key: string; count: number }[];
  failureBreakdown: { stage: string; category: string; count: number }[];
  generatedAt: string;
};
export type RecentImportOperation = {
  occurredAt: string; user: string; accountId: number | null; stage: string; status: string; parserSource: string;
  parsedTrades: number | null; savedRows: number | null; duplicateRows: number | null; failureCategory: string | null; firstImport: boolean;
};

export async function getImportHealthSummary(range: ImportHealthRange) { return (await api.get<ImportHealthSummary>("/admin/import-health/summary", { params: { range } })).data; }
export async function getRecentImportOperations(range: ImportHealthRange) { return (await api.get<RecentImportOperation[]>("/admin/import-health/recent", { params: { range } })).data; }
