import { useEffect, useMemo, useState } from "react";
import {
  getAllTrades,
  getTradesByAccount,
  importTradesFromCsv,
} from "../api/tradeApi";
import { getDashboardStats, type DashboardStats } from "../api/statsApi";
import { getCalendarSummary, type CalendarSummaryItem } from "../api/analyticsApi";
import {
  parseTradovateCsv,
  type TradovateTrade,
} from "../utils/tradovateCsv";
import {
  buildDailyPnlFromDbTrades,
  calculateBestStreak,
  calculateCurrentStreak,
  getDefaultCalendarMonth,
  type DailyPnl,
} from "../utils/dashboardUtils";
import type { SelectedAccountId } from "../types/dashboard";

type CalendarMonth = {
  year: number;
  month: number;
};

type UseDashboardDataParams = {
  authReady: boolean;
  selectedAccountId: SelectedAccountId;
  onDefaultCalendarMonthChange: (month: CalendarMonth) => void;
  onTradesSaved?: () => void;
};

export function useDashboardData({
  authReady,
  selectedAccountId,
  onDefaultCalendarMonthChange,
  onTradesSaved,
}: UseDashboardDataParams) {
  const [fileName, setFileName] = useState("");
  const [parsedImportCount, setParsedImportCount] = useState(0);
  const [trades, setTrades] = useState<TradovateTrade[]>([]);
  const [dailyPnl, setDailyPnl] = useState<DailyPnl[]>([]);
  const [parsing, setParsing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [dashboardStats, setDashboardStats] = useState<DashboardStats | null>(null);
  const [loadingStats, setLoadingStats] = useState(false);

  async function loadDashboardStats(accountId: SelectedAccountId) {
    if (!accountId) return;

    setLoadingStats(true);
    try {
      const stats = await getDashboardStats(
        accountId === "ALL" ? {} : { accountId: Number(accountId) }
      );
      setDashboardStats(stats);
    } catch (error) {
      console.error("Failed to load dashboard stats", error);
      setDashboardStats(null);
    } finally {
      setLoadingStats(false);
    }
  }

  function applyDbTrades(dbTrades: any[], calendarSummary?: CalendarSummaryItem[]) {
    const mappedTrades: TradovateTrade[] = dbTrades.map((trade: any) => ({
      id: trade.id,
      accountId: trade.accountId,
      accountName: trade.accountName,
      symbol: trade.symbol,
      qty: trade.quantity,
      buyPrice: Number(trade.entryPrice ?? 0),
      sellPrice: Number(trade.exitPrice ?? 0),
      pnl: Number(trade.pnlAmount ?? 0),
      boughtTimestamp: trade.entryTime ?? "",
      soldTimestamp: trade.exitTime ?? trade.tradeDate,
      duration: "",
      side: trade.side,
      setup: trade.setup,
      session: trade.session,
      mistakeTag: trade.mistakeTag,
      notes: trade.notes,
      psychologyNotes: trade.psychologyNotes,
      lessonLearned: trade.lessonLearned,
      pnlR: trade.pnlR == null ? null : Number(trade.pnlR),
      tradeDate: trade.tradeDate,
    }));

    const rebuiltDailyPnl = calendarSummary
      ? buildDailyPnlFromCalendarSummary(calendarSummary)
      : buildDailyPnlFromDbTrades(dbTrades);

    setTrades(mappedTrades);
    setParsedImportCount(0);
    setDailyPnl(rebuiltDailyPnl);
    onDefaultCalendarMonthChange(getDefaultCalendarMonth(rebuiltDailyPnl));
  }

  useEffect(() => {
    if (!authReady || !selectedAccountId) return;

    async function loadSavedTrades() {
      try {
        const [dbTrades, calendarSummary] = await Promise.all([
          selectedAccountId === "ALL"
            ? getAllTrades()
            : getTradesByAccount(Number(selectedAccountId)),
          getCalendarSummary(selectedAccountId === "ALL" ? undefined : Number(selectedAccountId)),
          loadDashboardStats(selectedAccountId),
        ]);

        applyDbTrades(dbTrades, calendarSummary);
      } catch (error) {
        console.error("Failed to load saved trades", error);
      }
    }

    loadSavedTrades();
  }, [authReady, selectedAccountId]);

  async function handleCsvUpload(event: React.ChangeEvent<HTMLInputElement>) {
    if (!selectedAccountId || selectedAccountId === "ALL") {
      event.target.value = "";
      alert("Select one trading account before importing CSV trades.");
      return;
    }

    const file = event.target.files?.[0];
    if (!file) return;

    setFileName(file.name);
    setParsing(true);

    try {
      const parsed = await parseTradovateCsv(file);
      setTrades(parsed.trades);
      setParsedImportCount(parsed.trades.length);
      setDailyPnl(parsed.dailyPnl);
      onDefaultCalendarMonthChange(getDefaultCalendarMonth(parsed.dailyPnl));
    } catch (error) {
      console.error(error);
      setParsedImportCount(0);
      alert("Failed to parse CSV");
    } finally {
      setParsing(false);
    }
  }

  async function handleSaveToDatabase() {
    if (!selectedAccountId) {
      alert("Select an account first.");
      return;
    }

    if (selectedAccountId === "ALL") {
      alert("Select a specific account before importing CSV trades.");
      return;
    }

    if (trades.length === 0) {
      alert("Upload and parse a CSV first.");
      return;
    }

    if (parsedImportCount === 0) {
      alert("Upload and parse a CSV before saving imported trades.");
      return;
    }

    if (!fileName) {
      alert("Upload and parse a CSV before saving imported trades.");
      return;
    }

    setSaving(true);

    try {
      const saved = await importTradesFromCsv(
        selectedAccountId,
        fileName,
        trades
      );
      const [dbTrades, calendarSummary] = await Promise.all([
        getTradesByAccount(Number(selectedAccountId)),
        getCalendarSummary(Number(selectedAccountId)),
      ]);

      applyDbTrades(dbTrades, calendarSummary);
      setFileName("");

      alert(`Saved ${saved.length} new trades and synced from database ✅`);
      await loadDashboardStats(selectedAccountId);
      onTradesSaved?.();
    } catch (error) {
      console.error(error);
      alert("Failed to save trades to database.");
    } finally {
      setSaving(false);
    }
  }

  const metrics = useMemo(() => {
    const summary = dashboardStats?.summary;
    const winningDays = dailyPnl.filter((day) => day.totalPnl > 0).length;
    const losingDays = dailyPnl.filter((day) => day.totalPnl < 0).length;
    const worstDay = dailyPnl.reduce(
      (worst, day) => (day.totalPnl < worst ? day.totalPnl : worst),
      0
    );

    return {
      totalPnl: toNumber(summary?.totalPnl),
      totalTrades: summary?.totalTrades ?? 0,
      winningTrades: summary?.wins ?? 0,
      losingTrades: summary?.losses ?? 0,
      breakevenTrades: summary?.breakevens ?? 0,
      averageR: toNumber(summary?.averageR),
      overallWinRate: toNumber(summary?.winRate),
      winningDays,
      losingDays,
      worstDay,
      currentWinStreak: calculateCurrentStreak(dailyPnl, "WIN"),
      currentLossStreak: calculateCurrentStreak(dailyPnl, "LOSS"),
      bestWinStreak: calculateBestStreak(dailyPnl, "WIN"),
      worstLossStreak: calculateBestStreak(dailyPnl, "LOSS"),
    };
  }, [dailyPnl, dashboardStats]);

  return {
    fileName,
    parsedImportCount,
    trades,
    dailyPnl,
    parsing,
    saving,
    dashboardStats,
    loadingStats,
    handleCsvUpload,
    handleSaveToDatabase,
    metrics,
  };
}

function toNumber(value: number | string | undefined): number {
  if (value === undefined || value === null) return 0;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function buildDailyPnlFromCalendarSummary(summary: CalendarSummaryItem[]): DailyPnl[] {
  let cumulativePnl = 0;
  return summary
    .slice()
    .sort((a, b) => a.date.localeCompare(b.date))
    .map((day) => {
      const totalPnl = toNumber(day.netPnl);
      const tradeCount = Number(day.tradeCount ?? 0);
      const winningTrades = Number(day.winningTrades ?? 0);
      cumulativePnl += totalPnl;
      return {
        date: day.date,
        totalPnl,
        cumulativePnl,
        tradeCount,
        winningTrades,
        losingTrades: Number(day.losingTrades ?? 0),
        breakevenTrades: Number(day.breakevenTrades ?? 0),
        winRate: tradeCount > 0 ? Number(((winningTrades / tradeCount) * 100).toFixed(2)) : 0,
      };
    });
}
