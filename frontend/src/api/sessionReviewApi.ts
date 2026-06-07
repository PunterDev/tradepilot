import { api } from "./axios";

export type SessionReviewTrade = {
  id: number;
  accountId?: number;
  accountName?: string;
  symbol: string;
  tradeDate?: string;
  entryTime?: string;
  exitTime?: string;
  side?: "LONG" | "SHORT";
  setup?: string;
  session?: string;
  entryPrice?: number | string | null;
  exitPrice?: number | string | null;
  quantity?: number | null;
  pnlAmount?: number | string | null;
  pnlR?: number | string | null;
  mistakeTag?: string | null;
  confidence?: number | null;
  emotion?: string | null;
  notes?: string | null;
};

export type SessionTimelineTrade = {
  tradeId: number;
  symbol: string;
  side?: "LONG" | "SHORT";
  entryTime?: string;
  exitTime?: string;
  quantity?: number | null;
  pnl?: number | string | null;
  setup?: string | null;
  confidence?: number | null;
  emotion?: string | null;
  notes?: string | null;
  outcome?: string | null;
};

export type AiSessionReview = {
  id?: number;
  summary?: string;
  whatWentWell?: string;
  whatHurtPerformance?: string;
  biggestMistake?: string;
  bestSetup?: string;
  worstDecision?: string;
  emotionalPattern?: string;
  executionPattern?: string;
  riskManagementPattern?: string;
  oneRuleForTomorrow?: string;
  generatedAt?: string;
};

export type SessionMistake = {
  type: string;
  title: string;
  description: string;
  severity: string;
};

export type SessionReplayInsights = {
  firstTrade?: SessionTimelineTrade | null;
  lastTrade?: SessionTimelineTrade | null;
  largestWinner?: SessionTimelineTrade | null;
  largestLoser?: SessionTimelineTrade | null;
  longestHold?: SessionTimelineTrade | null;
  shortestHold?: SessionTimelineTrade | null;
  longestHoldMinutes?: number | null;
  shortestHoldMinutes?: number | null;
  bestTradingHour?: number | null;
  bestTradingHourPnl?: number | string | null;
  worstTradingHour?: number | null;
  worstTradingHourPnl?: number | string | null;
  firstTradeTime?: string | null;
  lastTradeTime?: string | null;
};

export type SessionReview = {
  accountId: number;
  date: string;
  sessionDate: string;
  account: {
    id: number;
    name: string;
    accountType?: string;
  };
  trades: SessionReviewTrade[];
  timelineTrades?: SessionTimelineTrade[];
  totalTrades?: number;
  winners?: number;
  losers?: number;
  breakevenTrades?: number;
  netPnl?: number | string;
  grossProfit?: number | string;
  grossLoss?: number | string;
  largestWinner?: number | string;
  largestLoser?: number | string;
  averageWinner?: number | string;
  averageLoser?: number | string;
  totalPnl: number | string;
  winRate: number | string;
  profitFactor?: number | string;
  sessionGrade?: string;
  sessionStory?: string;
  bestTradeWhy?: string;
  worstTradeWhy?: string;
  bestTrade?: SessionReviewTrade | null;
  worstTrade?: SessionReviewTrade | null;
  mistakeSummary: Record<string, number>;
  detectedMistakes?: SessionMistake[];
  replayInsights?: SessionReplayInsights | null;
  sessionDisciplineScore: number;
  aiSessionReview?: AiSessionReview | null;
  aiReviewCached?: boolean;
  previousTradingDay?: string | null;
  nextTradingDay?: string | null;
};

export async function getSessionReview(accountId: number, date: string) {
  const response = await api.get<SessionReview>("/sessions/review", {
    params: { accountId, date },
  });
  return response.data;
}

export async function generateSessionReview(params: {
  accountId: number;
  date: string;
  forceRefresh?: boolean;
}) {
  const response = await api.post<SessionReview>("/sessions/review/generate", params);
  return response.data;
}
