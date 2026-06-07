import { useEffect, useState } from "react";
import { getWeeklyCoach, generateWeeklyCoach } from "../api/coachApi";
import type { CoachSnapshot } from "../types/coachTypes";

function addDays(date: string, days: number) {
  const d = new Date(date);
  d.setDate(d.getDate() + days);
  return d.toISOString().slice(0, 10);
}

export function useWeeklyCoach(accountId?: number, weekStart?: string) {
  const [data, setData] = useState<CoachSnapshot | null>(null);
  const [loading, setLoading] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    if (!accountId || !weekStart) return;

    setLoading(true);
    setError(null);

    try {
      const snapshot = await getWeeklyCoach(accountId, weekStart);
      setData(snapshot);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load weekly coach");
    } finally {
      setLoading(false);
    }
  }

  async function regenerate() {
    if (!accountId || !weekStart) return;

    setGenerating(true);
    setError(null);

    try {
      const snapshot = await generateWeeklyCoach({
        accountId,
        periodStart: weekStart,
        periodEnd: addDays(weekStart, 6),
        forceRefresh: true,
      });

      setData(snapshot);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to generate weekly coach");
    } finally {
      setGenerating(false);
    }
  }

  useEffect(() => {
    load();
  }, [accountId, weekStart]);

  useEffect(() => {
    if (!data) return;

    if (data.status !== "PENDING" && data.status !== "PROCESSING") return;

    const interval = window.setInterval(() => {
      load();
    }, 2500);

    return () => window.clearInterval(interval);
  }, [data?.status, accountId, weekStart]);

  return {
    data,
    loading,
    generating,
    error,
    reload: load,
    regenerate,
  };
}