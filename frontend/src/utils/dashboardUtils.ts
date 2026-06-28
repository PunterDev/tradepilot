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

type DbTrade = {
  tradeDate?: string;
  exitTime?: string;
  entryTime?: string;
  pnlAmount?: number | string;
  pnlR?: number | string | null;
};

export function formatCurrency(value: number) {
  return `$${value.toFixed(2)}`;
}

export function formatCompactCurrency(value: number) {
  const abs = Math.abs(value);
  const sign = value < 0 ? "-" : "";

  if (abs >= 1000) {
    return `${sign}$${(abs / 1000).toFixed(abs >= 10000 ? 0 : 2)}K`;
  }

  return `${sign}$${abs.toFixed(0)}`;
}

export function getMonthCalendarDays(year: number, month: number) {
  const firstDay = new Date(year, month, 1);
  const startDay = firstDay.getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();

  const days: Array<{ date: string; dayNumber: number | null }> = [];

  for (let i = 0; i < startDay; i++) {
    days.push({ date: "", dayNumber: null });
  }

  for (let day = 1; day <= daysInMonth; day++) {
    const date = `${year}-${String(month + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
    days.push({ date, dayNumber: day });
  }

  return days;
}

export function getDefaultCalendarMonth(dailyPnl: DailyPnl[]) {
  if (dailyPnl.length > 0) {
    const [year, month] = dailyPnl[dailyPnl.length - 1].date
      .split("-")
      .map(Number);
    return { year, month: month - 1 };
  }

  const now = new Date();
  return { year: now.getFullYear(), month: now.getMonth() };
}

export function getTradeDate(timestamp: unknown): string {
  if (!timestamp) return "Unknown";

  const value =
    timestamp instanceof Date ? timestamp.toISOString() : String(timestamp);

  if (/^\d{4}-\d{2}-\d{2}/.test(value)) {
    return value.slice(0, 10);
  }

  const [datePart] = value.split(" ");
  const parts = datePart.split("/");

  if (parts.length !== 3) return "Unknown";

  const [month, day, year] = parts;

  if (!month || !day || !year) return "Unknown";

  return `${year}-${month.padStart(2, "0")}-${day.padStart(2, "0")}`;
}

export function normalizeCalendarDate(value: unknown): string | null {
  if (!value) return null;

  if (value instanceof Date) {
    const year = value.getFullYear();
    const month = String(value.getMonth() + 1).padStart(2, "0");
    const day = String(value.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  }

  const raw = String(value).trim();
  if (!raw) return null;

  const isoMatch = raw.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (isoMatch) {
    return `${isoMatch[1]}-${isoMatch[2]}-${isoMatch[3]}`;
  }

  const slashMatch = raw.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})/);
  if (slashMatch) {
    const [, month, day, year] = slashMatch;
    return `${year}-${month.padStart(2, "0")}-${day.padStart(2, "0")}`;
  }

  return null;
}

export function getTradeCalendarDate(trade: {
  tradeDate?: unknown;
  date?: unknown;
  soldTimestamp?: unknown;
  exitTime?: unknown;
  boughtTimestamp?: unknown;
  entryTime?: unknown;
}): string | null {
  return (
    normalizeCalendarDate(trade.tradeDate) ||
    normalizeCalendarDate(trade.date) ||
    normalizeCalendarDate(trade.soldTimestamp) ||
    normalizeCalendarDate(trade.exitTime) ||
    normalizeCalendarDate(trade.boughtTimestamp) ||
    normalizeCalendarDate(trade.entryTime)
  );
}

export function calculateCurrentStreak(
  dailyPnl: DailyPnl[],
  type: "WIN" | "LOSS"
): number {
  let streak = 0;

  for (let i = dailyPnl.length - 1; i >= 0; i--) {
    const day = dailyPnl[i];

    if (type === "WIN" && day.totalPnl > 0) {
      streak++;
    } else if (type === "LOSS" && day.totalPnl < 0) {
      streak++;
    } else {
      break;
    }
  }

  return streak;
}

export function calculateBestStreak(
  dailyPnl: DailyPnl[],
  type: "WIN" | "LOSS"
): number {
  let best = 0;
  let current = 0;

  dailyPnl.forEach((day) => {
    const matches = type === "WIN" ? day.totalPnl > 0 : day.totalPnl < 0;

    if (matches) {
      current++;
      best = Math.max(best, current);
    } else {
      current = 0;
    }
  });

  return best;
}

export function buildDailyPnlFromDbTrades(dbTrades: DbTrade[]): DailyPnl[] {
  const dailyMap = new Map<string, DailyPnl>();

  dbTrades.forEach((trade) => {
    const date = getTradeCalendarDate(trade);
    const pnl = safeNumber(trade.pnlAmount);

    if (!date) return;

    if (!dailyMap.has(date)) {
      dailyMap.set(date, {
        date,
        totalPnl: 0,
        totalR: null,
        cumulativePnl: 0,
        tradeCount: 0,
        winningTrades: 0,
        losingTrades: 0,
        breakevenTrades: 0,
        winRate: 0,
      });
    }

    const day = dailyMap.get(date)!;
    const pnlR = safeNullableNumber(trade.pnlR);
    day.totalPnl += pnl;
    if (pnlR !== null) {
      day.totalR = (day.totalR ?? 0) + pnlR;
    }
    day.tradeCount += 1;

    if (pnl > 0) day.winningTrades += 1;
    else if (pnl < 0) day.losingTrades += 1;
    else day.breakevenTrades += 1;
  });

  let cumulativePnl = 0;

  return Array.from(dailyMap.values())
    .sort((a, b) => a.date.localeCompare(b.date))
    .map((day) => {
      cumulativePnl += day.totalPnl;

      return {
        ...day,
        cumulativePnl,
        winRate:
          day.tradeCount > 0
            ? Number(((day.winningTrades / day.tradeCount) * 100).toFixed(2))
            : 0,
      };
    });
}

export function buildDailyPnlFromCalendarTrades(
  trades: Array<{
    tradeDate?: unknown;
    date?: unknown;
    soldTimestamp?: unknown;
    exitTime?: unknown;
    boughtTimestamp?: unknown;
    entryTime?: unknown;
    pnl?: number | string;
    pnlAmount?: number | string;
    pnlR?: number | string | null;
  }>
): DailyPnl[] {
  return buildDailyPnlFromDbTrades(
    trades.map((trade) => ({
      tradeDate: getTradeCalendarDate(trade) ?? undefined,
      pnlAmount: trade.pnlAmount ?? trade.pnl ?? 0,
      pnlR: trade.pnlR,
    }))
  );
}

function safeNumber(value: number | string | null | undefined): number {
  if (value === undefined || value === null || value === "") return 0;
  if (typeof value === "number") return Number.isFinite(value) ? value : 0;
  const parsed = Number(value.replace(/[$,]/g, ""));
  return Number.isFinite(parsed) ? parsed : 0;
}

function safeNullableNumber(value: number | string | null | undefined): number | null {
  if (value === undefined || value === null || value === "") return null;
  if (typeof value === "number") return Number.isFinite(value) ? value : null;
  const parsed = Number(value.replace(/[$,]/g, ""));
  return Number.isFinite(parsed) ? parsed : null;
}
