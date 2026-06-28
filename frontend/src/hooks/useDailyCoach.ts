import { useEffect, useState } from "react";
import { getDailyCoach, generateDailyCoach } from "../api/coachApi";
import type { CoachSnapshot } from "../types/coachTypes";
import { coachLimitMessage, isLimitReachedError, type CoachLimitState } from "./coachLimitHandling";

const DAILY_LIMIT_FALLBACK =
  "Daily Coach limit reached. No cached coach snapshot is available for this date.";

export function useDailyCoach(accountId?: number, date?: string) {
  const [data, setData] = useState<CoachSnapshot | null>(null);
  const [loading, setLoading] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [limitState, setLimitState] = useState<CoachLimitState>({
    limitReached: false,
    message: "",
    hasCachedSnapshot: false,
  });
  const hasCurrentCachedSnapshot = Boolean(
    data && data.accountId === accountId && data.periodStart === date
  );

  async function load() {
    if (!accountId || !date) return;

    setLoading(true);
    setError(null);
    setLimitState({ limitReached: false, message: "", hasCachedSnapshot: false });

    try {
      const snapshot = await getDailyCoach(accountId, date);
      setData(snapshot);
    } catch (e) {
      if (isLimitReachedError(e)) {
        setLimitState({
          limitReached: true,
          message: hasCurrentCachedSnapshot
            ? "Limit reached. Showing cached Daily Coach snapshot."
            : coachLimitMessage(e, DAILY_LIMIT_FALLBACK),
          hasCachedSnapshot: hasCurrentCachedSnapshot,
        });
        console.info("Daily Coach limit reached.", { accountId, date, hasCachedSnapshot: hasCurrentCachedSnapshot });
        return;
      }
      console.error(e);
      setError("Unable to load Daily Coach right now. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  async function regenerate() {
    if (!accountId || !date) return;

    setGenerating(true);
    setError(null);
    setLimitState({ limitReached: false, message: "", hasCachedSnapshot: false });

    try {
      const snapshot = await generateDailyCoach({
        accountId,
        periodStart: date,
        periodEnd: date,
        forceRefresh: true,
      });

      setData(snapshot);
    } catch (e) {
      if (isLimitReachedError(e)) {
        setLimitState({
          limitReached: true,
          message: hasCurrentCachedSnapshot
            ? "Limit reached. Showing cached Daily Coach snapshot."
            : coachLimitMessage(e, DAILY_LIMIT_FALLBACK),
          hasCachedSnapshot: hasCurrentCachedSnapshot,
        });
        console.info("Daily Coach limit reached.", { accountId, date, hasCachedSnapshot: hasCurrentCachedSnapshot });
        return;
      }
      console.error(e);
      setError("Unable to load Daily Coach right now. Please try again.");
    } finally {
      setGenerating(false);
    }
  }

  useEffect(() => {
    load();
  }, [accountId, date]);

  useEffect(() => {
    if (!data) return;

    if (data.status !== "PENDING" && data.status !== "PROCESSING") return;

    const interval = window.setInterval(() => {
      load();
    }, 2500);

    return () => window.clearInterval(interval);
  }, [data?.status, accountId, date]);

  return {
    data,
    loading,
    generating,
    error,
    limitReached: limitState.limitReached,
    limitMessage: limitState.message,
    hasCachedSnapshot: limitState.hasCachedSnapshot,
    reload: load,
    regenerate,
  };
}
