import { api } from "./axios";

export type ReconciliationStatus = "NOT_REVIEWED" | "MATCHED" | "MISMATCH_REPORTED";
export type ImportMismatchReason = "PNL_MISMATCH" | "TRADE_COUNT_MISMATCH" | "FEES_MISMATCH" | "DUPLICATE_TRADES" | "TIMESTAMP_OR_SESSION_MISMATCH" | "MISSING_TRADES" | "OTHER";
export type InsightFeedbackReason = "WRONG_TRADES" | "WRONG_CONCLUSION" | "MISSING_CONTEXT" | "PNL_LOOKS_WRONG" | "OTHER";
export type ImportReconciliation = { importId: number; source: string | null; tradeCount: number; netPnl: number | string | null; status: ReconciliationStatus; reviewedAt: string | null; mismatchReasons: ImportMismatchReason[]; comment: string | null };
export type InsightFeedback = { snapshotId: number; findingType: string; helpful: boolean; reason: InsightFeedbackReason | null; updatedAt: string };
export type QualityRow = { key: string; rated: number; positive: number; negative: number; rate: number | null };
export type DataTrustQuality = { importTrust: { total: number; reviewed: number; matched: number; mismatched: number; notReviewed: number; userConfirmedMatchRate: number | null; bySource: QualityRow[]; mismatchReasons: Record<string, number> }; insightTrust: { rated: number; positive: number; negative: number; positiveRate: number | null; byFindingType: QualityRow[]; byEvidenceStrength: QualityRow[]; byVersion: QualityRow[]; negativeReasons: Record<string, number> } };

export async function getImportReconciliation(importId: number) { return (await api.get<ImportReconciliation>(`/data-trust/imports/${importId}/reconciliation`)).data; }
export async function saveImportReconciliation(importId: number, status: ReconciliationStatus, mismatchReasons: ImportMismatchReason[] = [], comment?: string) { return (await api.put<ImportReconciliation>(`/data-trust/imports/${importId}/reconciliation`, { status, mismatchReasons, comment })).data; }
export async function getInsightFeedback(snapshotId: number, findingType: string) { return (await api.get<InsightFeedback | null>(`/data-trust/insights/${snapshotId}/feedback`, { params: { findingType } })).data; }
export async function saveInsightFeedback(snapshotId: number, findingType: string, helpful: boolean, reason?: InsightFeedbackReason, comment?: string) { return (await api.put<InsightFeedback>(`/data-trust/insights/${snapshotId}/feedback`, { helpful, reason, comment }, { params: { findingType } })).data; }
export async function getDataTrustQuality() { return (await api.get<DataTrustQuality>("/admin/data-trust/quality")).data; }
