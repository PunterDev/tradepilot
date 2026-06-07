import { useEffect, useState } from "react";
import { getDailyCoach, generateDailyCoach } from "../api/coachApi";
import type { CoachSnapshot } from "../types/coachTypes";

export function useDailyCoach(accountId?: number, date?: string) {
  const [data, setData] = useState<CoachSnapshot | null>(null);
  const [loading, setLoading] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    if (!accountId || !date) return;

    setLoading(true);
    setError(null);

    try {
      const snapshot = await getDailyCoach(accountId, date);
      setData(snapshot);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load daily coach");
    } finally {
      setLoading(false);
    }
  }

  async function regenerate() {
    if (!accountId || !date) return;

    setGenerating(true);
    setError(null);

    try {
      const snapshot = await generateDailyCoach({
        accountId,
        periodStart: date,
        periodEnd: date,
        forceRefresh: true,
      });

      setData(snapshot);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to generate daily coach");
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
    reload: load,
    regenerate,
  };
}