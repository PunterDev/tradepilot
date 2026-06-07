import { useTradeAnalytics } from "../../hooks/useTradeAnalytics";
import FridayOpexPanel from "./FridayOpexPanel";
import SessionAnalysisPanel from "./SessionAnalysisPanel";
import PerformanceHeatmap from "./PerformanceHeatmap";
import BehavioralInsightsPanel from "./BehavioralInsightsPanel";
import type { TradovateTrade } from "../../utils/tradovateCsv";

type AnalyticsDashboardProps = {
  trades?: TradovateTrade[];
  onTradeSelect?: (tradeId: number) => void;
};

export default function AnalyticsDashboard({
  trades = [],
  onTradeSelect,
}: AnalyticsDashboardProps) {
  const {
    fridayOpex,
    sessions,
    heatmap,
    insights,
    loading,
    error,
    reload,
  } = useTradeAnalytics();
  const hasAnalyticsData =
    (fridayOpex?.allFridays.tradeCount ?? 0) > 0 ||
    sessions.some((session) => session.tradeCount > 0) ||
    heatmap.some((cell) => cell.tradeCount > 0) ||
    insights.length > 0;

  return (
    <div style={{ marginTop: 32 }}>
      <div style={{ display: "flex", justifyContent: "space-between", gap: 16 }}>
        <div>
          <h2 style={{ color: "#e5e7eb", marginBottom: 4 }}>
            Trade Analytics Engine
          </h2>
          <p style={{ color: "#94a3b8", marginTop: 0 }}>
            Friday/OPEX behavior, session performance, heatmaps, and discipline insights.
          </p>
        </div>

        <button
          onClick={reload}
          disabled={loading}
          style={{
            height: 42,
            padding: "0 16px",
            borderRadius: 999,
            border: "1px solid rgba(148,163,184,0.25)",
            background: "rgba(15,23,42,0.9)",
            color: "#e5e7eb",
            fontWeight: 800,
            cursor: loading ? "not-allowed" : "pointer",
          }}
        >
          {loading ? "Loading..." : "Refresh"}
        </button>
      </div>

      {error && (
        <div
          style={{
            marginTop: 16,
            padding: 14,
            borderRadius: 14,
            background: "rgba(127,29,29,0.35)",
            border: "1px solid rgba(248,113,113,0.35)",
            color: "#fecaca",
          }}
        >
          {error}
        </div>
      )}

      {!loading && !error && !hasAnalyticsData && (
        <div
          style={{
            marginTop: 18,
            padding: 22,
            borderRadius: 22,
            border: "1px solid rgba(56,189,248,0.24)",
            background: "linear-gradient(180deg, rgba(8,47,73,0.28), rgba(15,23,42,0.82))",
            color: "#cbd5e1",
          }}
        >
          <h3 style={{ margin: "0 0 8px", color: "#f8fafc" }}>
            No analytics data yet
          </h3>
          <p style={{ margin: 0, color: "#94a3b8", lineHeight: 1.6 }}>
            Import trades into a real account to unlock Friday/OPEX behavior,
            heatmaps, session stats, and discipline insights.
          </p>
        </div>
      )}

      {fridayOpex && <FridayOpexPanel data={fridayOpex} />}

      <div
        style={{
          marginTop: 20,
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: 20,
        }}
      >
        <SessionAnalysisPanel sessions={sessions} />
        <BehavioralInsightsPanel insights={insights} />
      </div>

      <PerformanceHeatmap
        data={heatmap}
        trades={trades}
        onTradeSelect={onTradeSelect}
      />
    </div>
  );
}
