import { useEffect, useMemo, useState } from "react";
import {
  getAllTrades,
  getTradesByAccount,
  previewCsvImport,
  saveCsvImport,
  type TradeImportPreviewResponse,
} from "../api/tradeApi";
import { getDashboardStats, type DashboardStats } from "../api/statsApi";
import { getCalendarSummary, type CalendarSummaryItem } from "../api/analyticsApi";
import type { TradovateTrade } from "../utils/tradovateCsv";
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

export type ImportFeedback = {
  type: "success" | "error" | "warning";
  message: string;
  details?: string[];
};

export function useDashboardData({
  authReady,
  selectedAccountId,
  onDefaultCalendarMonthChange,
  onTradesSaved,
}: UseDashboardDataParams) {
  const [fileName, setFileName] = useState("");
  const [selectedCsvFile, setSelectedCsvFile] = useState<File | null>(null);
  const [importPreview, setImportPreview] = useState<TradeImportPreviewResponse | null>(null);
  const [parsedImportCount, setParsedImportCount] = useState(0);
  const [trades, setTrades] = useState<TradovateTrade[]>([]);
  const [dailyPnl, setDailyPnl] = useState<DailyPnl[]>([]);
  const [parsing, setParsing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [importFeedback, setImportFeedback] = useState<ImportFeedback | null>(null);
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
    setSelectedCsvFile(null);
    setImportPreview(null);
    setParsedImportCount(0);
    setImportFeedback(null);
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
    setImportFeedback(null);
    if (!selectedAccountId || selectedAccountId === "ALL") {
      event.target.value = "";
      setImportFeedback({
        type: "warning",
        message: "Select one trading account before importing CSV trades.",
      });
      return;
    }

    const file = event.target.files?.[0];
    if (!file) return;
    if (!file.name.toLowerCase().endsWith(".csv")) {
      event.target.value = "";
      setFileName("");
      setSelectedCsvFile(null);
      setParsedImportCount(0);
      setImportPreview(null);
      setImportFeedback({
        type: "error",
        message: "Upload a CSV file exported from Tradovate or Topstep.",
      });
      return;
    }

    setFileName(file.name);
    setSelectedCsvFile(file);
    setImportPreview(null);
    setParsing(true);

    try {
      const preview = await previewCsvImport(Number(selectedAccountId), file);
      setImportPreview(preview);
      setParsedImportCount(preview.completedTrades);
      const previewDailyPnl = buildDailyPnlFromImportPreview(preview);
      setTrades(mapPreviewTrades(preview));
      setDailyPnl(previewDailyPnl);
      onDefaultCalendarMonthChange(getDefaultCalendarMonth(previewDailyPnl));
      setImportFeedback({
        type: preview.completedTrades === preview.duplicateTrades ? "warning" : "success",
        message: importPreviewMessage(preview),
        details: preview.warnings,
      });
    } catch (error) {
      console.error(error);
      setParsedImportCount(0);
      setSelectedCsvFile(null);
      setImportPreview(null);
      setImportFeedback({
        type: "error",
        message: importErrorMessage(error),
        details: importErrorDetails(error),
      });
    } finally {
      setParsing(false);
      event.target.value = "";
    }
  }

  async function handleSaveToDatabase() {
    if (!selectedAccountId) {
      setImportFeedback({ type: "warning", message: "Select an account first." });
      return;
    }

    if (selectedAccountId === "ALL") {
      setImportFeedback({ type: "warning", message: "Select a specific account before importing CSV trades." });
      return;
    }

    if (!selectedCsvFile || !importPreview) {
      setImportFeedback({ type: "warning", message: "Upload and preview a CSV before saving imported trades." });
      return;
    }

    if (parsedImportCount === 0) {
      setImportFeedback({ type: "warning", message: "No completed trades are available to save from this CSV." });
      return;
    }

    if (!fileName) {
      setImportFeedback({ type: "warning", message: "Upload and preview a CSV before saving imported trades." });
      return;
    }

    setSaving(true);
    setImportFeedback(null);

    try {
      const saved = await saveCsvImport(Number(selectedAccountId), selectedCsvFile);
      const [dbTrades, calendarSummary] = await Promise.all([
        getTradesByAccount(Number(selectedAccountId)),
        getCalendarSummary(Number(selectedAccountId)),
      ]);

      applyDbTrades(dbTrades, calendarSummary);
      setFileName("");
      setSelectedCsvFile(null);
      setImportPreview(null);

      const savedCount = Array.isArray(saved) ? saved.length : saved.savedCount;
      setImportFeedback({
        type: savedCount === 0 ? "warning" : "success",
        message: savedCount === 0
          ? "All trades in this file already exist. No new trades were saved."
          : `Saved ${savedCount} new trades and synced from the database.`,
        details: saved.preview?.warnings,
      });
      await loadDashboardStats(selectedAccountId);
      onTradesSaved?.();
    } catch (error) {
      console.error(error);
      setImportFeedback({
        type: "error",
        message: importErrorMessage(error) || "Failed to save trades to database.",
        details: importErrorDetails(error),
      });
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
      averageR: toNullableNumber(summary?.averageR),
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
    importPreview,
    importFeedback,
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

function toNullableNumber(value: number | string | null | undefined): number | null {
  if (value === undefined || value === null) return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
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
        totalR: toNullableNumber(day.totalR),
        winRate: tradeCount > 0 ? Number(((winningTrades / tradeCount) * 100).toFixed(2)) : 0,
      };
    });
}

function buildDailyPnlFromImportPreview(preview: TradeImportPreviewResponse): DailyPnl[] {
  const dailyMap = new Map<string, DailyPnl>();
  preview.trades.forEach((trade) => {
    const date = trade.tradeDate;
    const pnl = toNumber(trade.pnlAmount);
    if (!dailyMap.has(date)) {
      dailyMap.set(date, {
        date,
        totalPnl: 0,
        cumulativePnl: 0,
        tradeCount: 0,
        winningTrades: 0,
        losingTrades: 0,
        breakevenTrades: 0,
        winRate: 0,
      });
    }
    const day = dailyMap.get(date)!;
    day.totalPnl += pnl;
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
        winRate: day.tradeCount > 0 ? Number(((day.winningTrades / day.tradeCount) * 100).toFixed(2)) : 0,
      };
    });
}

function mapPreviewTrades(preview: TradeImportPreviewResponse): TradovateTrade[] {
  return preview.trades.map((trade) => ({
    symbol: trade.contractSymbol || trade.originalBrokerSymbol || trade.symbol,
    qty: Number(trade.quantity ?? 0),
    buyPrice: trade.side === "SHORT" ? Number(trade.exitPrice ?? 0) : Number(trade.entryPrice ?? 0),
    sellPrice: trade.side === "SHORT" ? Number(trade.entryPrice ?? 0) : Number(trade.exitPrice ?? 0),
    pnl: toNumber(trade.pnlAmount),
    boughtTimestamp: trade.side === "SHORT" ? trade.exitTime : trade.entryTime,
    soldTimestamp: trade.side === "SHORT" ? trade.entryTime : trade.exitTime,
    duration: "",
    side: trade.side === "LONG" || trade.side === "SHORT" ? trade.side : "UNKNOWN",
    setup: "OTHER",
    session: "UNKNOWN",
    tradeDate: trade.tradeDate,
  }));
}

function importPreviewMessage(preview: TradeImportPreviewResponse): string {
  const format = preview.detectedFormat === "TOPSTEP" ? "Topstep order history" : "Tradovate trade export";
  if (preview.completedTrades > 0 && preview.completedTrades === preview.duplicateTrades) {
    return `${format} detected. All ${preview.completedTrades} completed trades already exist.`;
  }
  return `${format} detected. ${preview.completedTrades} completed trades reconstructed.`;
}

function importErrorMessage(error: any): string {
  return error?.response?.data?.message
    || error?.response?.data?.error
    || "Failed to parse CSV. Upload a supported Tradovate or Topstep export.";
}

function importErrorDetails(error: any): string[] | undefined {
  const requestId = error?.response?.data?.requestId;
  return requestId ? [`Request ID: ${requestId}`] : undefined;
}
