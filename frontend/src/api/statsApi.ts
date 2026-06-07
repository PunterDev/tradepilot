import { api } from "./axios";

export type StatsParams = {
  accountId?: number;
  startDate?: string;
  endDate?: string;
};

export type DashboardSummary = {
  totalTrades: number;
  wins: number;
  losses: number;
  breakevens: number;
  totalPnl: number | string;
  averageR: number | string;
  winRate: number | string;
};

export type MetricItem = {
  label: string;
  totalTrades: number;
  wins: number;
  losses: number;
  breakevens: number;
  totalPnl: number | string;
  averageR: number | string;
  winRate: number | string;
};

export type DashboardStats = {
  summary: DashboardSummary;
  bySetup: MetricItem[];
  bySession: MetricItem[];
  bySymbol: MetricItem[];
  byWeekday: MetricItem[];
};

export async function getDashboardStats(params: StatsParams = {}): Promise<DashboardStats> {
  const [summary, bySetup, bySession, bySymbol, byWeekday] = await Promise.all([
    api.get<DashboardSummary>("/stats/summary", { params }),
    api.get<MetricItem[]>("/stats/by-setup", { params }),
    api.get<MetricItem[]>("/stats/by-session", { params }),
    api.get<MetricItem[]>("/stats/by-symbol", { params }),
    api.get<MetricItem[]>("/stats/by-weekday", { params }),
  ]);

  return {
    summary: summary.data,
    bySetup: bySetup.data,
    bySession: bySession.data,
    bySymbol: bySymbol.data,
    byWeekday: byWeekday.data,
  };
}
