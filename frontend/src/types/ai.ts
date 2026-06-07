import type { Trade } from "./trade";
import type { RuleViolation } from "./discipline";

export type AiJobStatus = "PENDING" | "COMPLETED" | "FAILED";

export type AiTradeAnalysisStatus =
  | "IDLE"
  | "STARTING"
  | "POLLING"
  | "COMPLETED"
  | "FAILED";

export interface AiTradeAnalysisRequest {
  tradeId: number;
  trade: Trade;
  journalText?: string;
  reviewVersion?: string;
  forceRefresh?: boolean;
}

export interface AiTradeAnalysisResponse {
  id?: number;
  tradeId?: number;
  status?: AiJobStatus;
  reviewVersion?: string;
  reviewHash?: string;

  summary?: string;
  grade?: string;

  executionScore?: number;
  disciplineScore?: number;
  riskScore?: number;

  marketContext?: string;
  entryAnalysis?: string;
  exitAnalysis?: string;
  riskAnalysis?: string;
  psychologyAnalysis?: string;
  chartAnalysis?: string;

  mistakes?: string[];
  strengths?: string[];
  improvementPlan?: string[];
  tags?: string[];
  violations?: RuleViolation[];

  errorMessage?: string;
}

export interface AiTradeAnalysisJobResponse {
  analysisId?: number;
  reviewId?: number;
  id?: number;

  tradeId?: number;
  status?: AiJobStatus;

  analysis?: AiTradeAnalysisResponse | null;
  review?: AiTradeAnalysisResponse | null;

  message?: string;
  errorMessage?: string;
}

export type AiTradeReviewRequest = AiTradeAnalysisRequest;
export type AiTradeReviewResponse = AiTradeAnalysisResponse;

export interface AiTradeReviewJobResponse {
  reviewId?: number;
  tradeId?: number;
  status?: AiJobStatus | string;
  review?: AiTradeReviewResponse | null;
  message?: string;
  errorMessage?: string;
}
