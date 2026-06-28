import { api } from "./api";
import type {
  BehavioralInsightDto,
  FridayOpexAnalyticsResponse,
  HeatmapCellDto,
  SessionPerformanceDto,
} from "../types/analytics";

export async function getFridayOpexAnalytics() {
  const res = await api.get<FridayOpexAnalyticsResponse>(
    "/analytics/friday-opex"
  );
  return res.data;
}

export async function getSessionAnalytics() {
  const res = await api.get<SessionPerformanceDto[]>(
    "/analytics/sessions"
  );
  return res.data;
}

export async function getHeatmapAnalytics() {
  const res = await api.get<HeatmapCellDto[]>(
    "/analytics/heatmap"
  );
  return res.data;
}

export async function getBehavioralInsights() {
  const res = await api.get<BehavioralInsightDto[]>(
    "/analytics/behavioral-insights"
  );
  return res.data;
}

export type CalendarSummaryItem = {
  date: string;
  tradeCount: number;
  netPnl: number | string;
  totalR?: number | string | null;
  winningTrades: number;
  losingTrades: number;
  breakevenTrades: number;
};

export async function getCalendarSummary(accountId?: number) {
  const res = await api.get<CalendarSummaryItem[]>("/analytics/calendar-summary", {
    params: accountId ? { accountId } : {},
  });
  return res.data;
}
