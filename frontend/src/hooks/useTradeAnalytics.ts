import { useEffect, useState } from "react";
import {
    getBehavioralInsights,
    getFridayOpexAnalytics,
    getHeatmapAnalytics,
    getSessionAnalytics,
} from "../api/analyticsApi";
import type {
    BehavioralInsightDto,
    FridayOpexAnalyticsResponse,
    HeatmapCellDto,
    SessionPerformanceDto,
} from "../types/analytics";

export function useTradeAnalytics() {
    const [fridayOpex, setFridayOpex] =
        useState<FridayOpexAnalyticsResponse | null>(null);
    const [sessions, setSessions] = useState<SessionPerformanceDto[]>([]);
    const [heatmap, setHeatmap] = useState<HeatmapCellDto[]>([]);
    const [insights, setInsights] = useState<BehavioralInsightDto[]>([]);

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    async function load() {
        setLoading(true);
        setError("");

        try {
            const [fridayRes, sessionRes, heatmapRes, insightRes] =
                await Promise.all([
                    getFridayOpexAnalytics(),
                    getSessionAnalytics(),
                    getHeatmapAnalytics(),
                    getBehavioralInsights(),
                ]);

            setFridayOpex(fridayRes);
            setSessions(sessionRes);
            setHeatmap(heatmapRes);
            setInsights(insightRes);
        } catch (err) {
            console.error(err);
            setError("Failed to load trade analytics.");
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        void load();
    }, []);

    return {
        fridayOpex,
        sessions,
        heatmap,
        insights,
        loading,
        error,
        reload: load,
    };
}