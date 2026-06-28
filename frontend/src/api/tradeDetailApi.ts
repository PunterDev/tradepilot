import { api } from "./axios";
import type { AiTradeReviewResponse } from "../types/ai";

export type TradeDetailTrade = {
  id: number;
  accountId?: number;
  accountName?: string;
  accountType?: string;
  symbol: string;
  market?: string;
  tradeDate?: string;
  entryTime?: string;
  exitTime?: string;
  side?: "LONG" | "SHORT" | "UNKNOWN";
  setup?: string;
  session?: string;
  entryPrice?: number | string | null;
  stopPrice?: number | string | null;
  targetPrice?: number | string | null;
  exitPrice?: number | string | null;
  quantity?: number | null;
  riskAmount?: number | string | null;
  pnlAmount?: number | string | null;
  pnlR?: number | string | null;
  outcome?: string;
  confidence?: number | null;
  confidenceRating?: number | null;
  emotion?: string | null;
  executionRating?: number | null;
  followedPlan?: boolean | null;
  mistakeTag?: string | null;
  notes?: string | null;
  psychologyNotes?: string | null;
  lessonLearned?: string | null;
  screenshotUrl?: string | null;
};

export type TradeDisciplineViolation = {
  id: number;
  ruleCode: string;
  ruleTitle: string;
  category: string;
  severity: string;
  reason?: string | null;
  source?: string;
  tradeDate?: string;
  createdAt?: string;
};

export type TradeDetail = {
  trade: TradeDetailTrade;
  previousTradeId?: number | null;
  nextTradeId?: number | null;
  aiReview?: AiTradeReviewResponse | null;
  aiReviewCached?: boolean;
  relatedBehavioralInsights: string[];
  disciplineViolations: TradeDisciplineViolation[];
};

export async function getTradeDetail(tradeId: number): Promise<TradeDetail> {
  const response = await api.get<TradeDetail>(`/trades/${tradeId}/detail`);
  return response.data;
}

export type TradeJournalPayload = {
  setup?: string | null;
  confidence?: number | null;
  emotion?: string | null;
  notes?: string | null;
};

export async function updateTradeJournal(
  tradeId: number,
  payload: TradeJournalPayload
): Promise<TradeDetailTrade> {
  const response = await api.patch<TradeDetailTrade>(`/trades/${tradeId}/journal`, payload);
  return response.data;
}
