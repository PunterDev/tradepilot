export type Trade = {
  id: number;

  accountId?: number;

  accountName?: string;

  symbol: string;

  side: "LONG" | "SHORT" | "UNKNOWN";

  pnl: number;

  quantity?: number;

  entryPrice?: number;

  exitPrice?: number;

  entryTime?: string;

  exitTime?: string;

  notes?: string;

  strategy?: string;

  setup?: string;

  confidence?: number | null;

  emotion?: string | null;

  session?: string;

  mistakeTag?: string | null;

  pnlR?: number | null;

  tradeDate?: string;

  psychologyNotes?: string | null;

  lessonLearned?: string | null;
};
