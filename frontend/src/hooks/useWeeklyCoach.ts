import { useEffect, useState } from "react";
import { getWeeklyCoach, generateWeeklyCoach, generateWeeklyCoachPreview } from "../api/coachApi";
import { getExecutionWeeklyFocus, type WeeklyExecutionFocus } from "../api/executionApi";
import type { CoachSnapshot } from "../types/coachTypes";
import { coachLimitMessage, isLimitReachedError, type CoachLimitState } from "./coachLimitHandling";

const WEEKLY_LIMIT_FALLBACK =
  "Weekly Coach limit reached. No cached coach snapshot is available for this week.";

function addDays(date: string, days: number) {
  const d = new Date(date);
  d.setDate(d.getDate() + days);
  return d.toISOString().slice(0, 10);
}

export function useWeeklyCoach(accountId?: number, weekStart?: string, previewAvailable = false, loadEnabled = true, executionEnabled = false) {
  const [data, setData] = useState<CoachSnapshot | null>(null);
  const [loading, setLoading] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [executionFocus, setExecutionFocus] = useState<WeeklyExecutionFocus | null>(null);
  const [limitState, setLimitState] = useState<CoachLimitState>({
    limitReached: false,
    message: "",
    hasCachedSnapshot: false,
  });
  const hasCurrentCachedSnapshot = Boolean(
    data && data.accountId === accountId && data.periodStart === weekStart
  );

  async function load() {
    if (!loadEnabled || !accountId || !weekStart) return;

    setLoading(true);
    setError(null);
    setLimitState({ limitReached: false, message: "", hasCachedSnapshot: false });

    try {
      const snapshot = await getWeeklyCoach(accountId, weekStart);
      setData(snapshot);
      if (executionEnabled) {
        getExecutionWeeklyFocus(accountId).then(setExecutionFocus).catch(() => setExecutionFocus(null));
      } else {
        setExecutionFocus(null);
      }
    } catch (e) {
      if (isLimitReachedError(e)) {
        setLimitState({
          limitReached: true,
          message: hasCurrentCachedSnapshot
            ? "Limit reached. Showing cached Weekly Coach snapshot."
            : coachLimitMessage(e, WEEKLY_LIMIT_FALLBACK),
          hasCachedSnapshot: hasCurrentCachedSnapshot,
        });
        console.info("Weekly Coach limit reached.", { accountId, weekStart, hasCachedSnapshot: hasCurrentCachedSnapshot });
        return;
      }
      console.error(e);
      setError("Unable to load Weekly Coach right now. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  async function regenerate() {
    if (!accountId || !weekStart) return;

    setGenerating(true);
    setError(null);
    setLimitState({ limitReached: false, message: "", hasCachedSnapshot: false });

    try {
      const snapshot = await generateWeeklyCoach({
        accountId,
        periodStart: weekStart,
        periodEnd: addDays(weekStart, 6),
        forceRefresh: true,
      });

      setData(snapshot);
      if (executionEnabled) {
        getExecutionWeeklyFocus(accountId).then(setExecutionFocus).catch(() => setExecutionFocus(null));
      } else {
        setExecutionFocus(null);
      }
    } catch (e) {
      if (isLimitReachedError(e)) {
        setLimitState({
          limitReached: true,
          message: hasCurrentCachedSnapshot
            ? "Limit reached. Showing cached Weekly Coach snapshot."
            : coachLimitMessage(e, WEEKLY_LIMIT_FALLBACK),
          hasCachedSnapshot: hasCurrentCachedSnapshot,
        });
        console.info("Weekly Coach limit reached.", { accountId, weekStart, hasCachedSnapshot: hasCurrentCachedSnapshot });
        return;
      }
      console.error(e);
      setError("Unable to load Weekly Coach right now. Please try again.");
    } finally {
      setGenerating(false);
    }
  }

  async function generatePreview() {
    if (!accountId || !weekStart || !previewAvailable) return;

    setGenerating(true);
    setError(null);
    setLimitState({ limitReached: false, message: "", hasCachedSnapshot: false });

    try {
      const snapshot = await generateWeeklyCoachPreview({
        accountId,
        periodStart: weekStart,
        periodEnd: addDays(weekStart, 6),
        forceRefresh: true,
      });

      setData(snapshot);
      if (executionEnabled) {
        getExecutionWeeklyFocus(accountId).then(setExecutionFocus).catch(() => setExecutionFocus(null));
      } else {
        setExecutionFocus(null);
      }
    } catch (e) {
      console.error(e);
      setError("Unable to start the Weekly Coach preview right now. Please try again.");
    } finally {
      setGenerating(false);
    }
  }

  useEffect(() => {
    if (!executionEnabled) {
      setExecutionFocus(null);
    }
  }, [executionEnabled]);

  useEffect(() => {
    load();
  }, [accountId, weekStart, loadEnabled, executionEnabled]);

  useEffect(() => {
    if (!data) return;

    if (data.status !== "PENDING" && data.status !== "PROCESSING") return;

    const interval = window.setInterval(() => {
      load();
    }, 2500);

    return () => window.clearInterval(interval);
  }, [data?.status, accountId, weekStart, loadEnabled]);

  return {
    data,
    loading,
    generating,
    error,
    limitReached: limitState.limitReached,
    limitMessage: limitState.message,
    hasCachedSnapshot: limitState.hasCachedSnapshot,
    executionFocus,
    reload: load,
    regenerate,
    generatePreview,
  };
}
