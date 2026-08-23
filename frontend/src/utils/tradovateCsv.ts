/**
 * Raw trade parsed from Tradovate CSV
 */
export type TradovateTrade = {
  id?: number;
  accountId?: number;
  accountName?: string;
  symbol: string;
  qty: number;
  buyPrice: number;
  sellPrice: number;
  pnl: number;
  boughtTimestamp: string;
  soldTimestamp: string;
  duration: string;
  side?: "LONG" | "SHORT" | "UNKNOWN";
  setup?: string;
  session?: string;
  mistakeTag?: string | null;
  notes?: string | null;
  psychologyNotes?: string | null;
  lessonLearned?: string | null;
  pnlR?: number | null;
  tradeDate?: string;
};

/**
 * Aggregated daily stats
 */
export type DailyPnl = {
  date: string;
  totalPnl: number;
  totalR?: number | null;
  cumulativePnl: number;
  tradeCount: number;
  winningTrades: number;
  losingTrades: number;
  breakevenTrades: number;
  winRate: number;
};
