export interface AnalyticsMetricDto {
    tradeCount: number;
    totalPnl: number;
    winRate: number;
    avgPnl: number;
    avgWin: number;
    avgLoss: number;
  }
  
  export interface FridayOpexAnalyticsResponse {
    allFridays: AnalyticsMetricDto;
    opexFridays: AnalyticsMetricDto;
    nonOpexFridays: AnalyticsMetricDto;
    insight: string;
  }
  
  export interface SessionPerformanceDto {
    session: string;
    tradeCount: number;
    totalPnl: number;
    winRate: number;
    avgPnl: number;
  }
  
  export interface HeatmapCellDto {
    dayOfWeek: string;
    session: string;
    sessionBucket?: string;
    tradeCount: number;
    wins?: number;
    losses?: number;
    totalPnl: number;
    pnl?: number;
    winRate: number;
  }
  
  export interface BehavioralInsightDto {
    type: string;
    severity: "LOW" | "MEDIUM" | "HIGH" | string;
    title: string;
    message: string;
  }
