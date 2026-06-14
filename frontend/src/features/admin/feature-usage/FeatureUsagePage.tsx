import { useEffect, useMemo, useState, type CSSProperties } from "react";
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  getFeatureUsageSummary,
  getFeatureUsageTrends,
  getFeatureUsageUsers,
  type FeatureAdoption,
  type FeatureKey,
  type FeatureUsageSummary,
  type FeatureUsageTrends,
  type FeatureUsageUser,
} from "../../../api/featureUsageApi";
import PageHeader from "../../../layout/PageHeader";
import { card, mutedText, sectionTitle, tooltipStyle } from "../../../styles/dashboardStyles";

const featureLabels: Record<FeatureKey, string> = {
  AI_REVIEW: "AI Reviews",
  DAILY_COACH: "Daily Coach",
  WEEKLY_COACH: "Weekly Coach",
  TRADING_DNA: "Trading DNA",
  AI_LEAK_DETECTOR: "AI Leak Detector",
  SESSION_REVIEW: "Session Review",
  TRADE_DETAIL: "Trade Detail",
  CALENDAR_VIEW: "PnL Calendar",
  ANALYTICS_VIEW: "Analytics",
};

export default function FeatureUsagePage() {
  const [summary, setSummary] = useState<FeatureUsageSummary | null>(null);
  const [trends, setTrends] = useState<FeatureUsageTrends | null>(null);
  const [users, setUsers] = useState<FeatureUsageUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function load() {
      setLoading(true);
      setError("");
      try {
        const [nextSummary, nextTrends, nextUsers] = await Promise.all([
          getFeatureUsageSummary(),
          getFeatureUsageTrends(),
          getFeatureUsageUsers(),
        ]);
        setSummary(nextSummary);
        setTrends(nextTrends);
        setUsers(nextUsers);
      } catch {
        setError("Unable to load feature usage analytics.");
      } finally {
        setLoading(false);
      }
    }

    void load();
  }, []);

  const sortedFeatures = useMemo(() => {
    return [...(summary?.features ?? [])].sort((a, b) => b.totalUsage - a.totalUsage);
  }, [summary]);

  const totalEvents = sortedFeatures.reduce((sum, feature) => sum + feature.totalUsage, 0);
  const averageFeaturesUsed = summary?.totalUsers ? (totalEvents / summary.totalUsers).toFixed(1) : "0";
  const activeAdoptionLeader = useMemo(() => {
    return sortedFeatures
      .filter((feature) => feature.totalUsage > 0)
      .sort((a, b) => b.activeUserAdoptionRate - a.activeUserAdoptionRate || b.totalUsage - a.totalUsage)[0] ?? null;
  }, [sortedFeatures]);
  const powerUsers = users.slice(0, 10).map((user) => ({
    ...user,
    totalUsage: Object.values(user.featureUsageCounts ?? {}).reduce((sum, value) => sum + (value ?? 0), 0),
  }));
  const insights = buildInsights(summary, trends);

  return (
    <div style={pageWrap}>
      <PageHeader
        label="Admin Analytics"
        title="Feature Usage"
        description="See what traders actually use, where adoption is lagging, and which workflows deserve product focus."
      />

      {error && <div style={errorBox}>{error}</div>}
      {!loading && (summary?.totalUsers ?? 0) > 0 && totalEvents === 0 && (
        <div style={warningBox}>
          No feature events detected. Tracking may not be configured, or existing activity needs a dev backfill.
        </div>
      )}

      <section style={kpiGrid}>
        <Kpi label="Active Users (30d)" value={summary?.activeUsers30d ?? 0} loading={loading} />
        <Kpi label="Most Used Feature" value={label(summary?.mostUsedFeature)} loading={loading} accent="#86efac" />
        <Kpi
          label="Active Adoption Leader"
          value={activeAdoptionLeader ? `${label(activeAdoptionLeader.feature)} ${activeAdoptionLeader.activeUserAdoptionRate}%` : "None"}
          loading={loading}
          accent="#67e8f9"
        />
        <Kpi label="Least Used Feature" value={label(summary?.leastUsedFeature)} loading={loading} accent="#fde68a" />
        <Kpi label="Total Feature Events" value={totalEvents} loading={loading} accent="#93c5fd" />
        <Kpi label="Avg Features / User" value={averageFeaturesUsed} loading={loading} accent="#c4b5fd" />
      </section>

      <section style={sectionCard}>
        <h2 style={sectionTitle}>Feature Adoption</h2>
        <div style={featureGrid}>
          {loading ? (
            <div style={emptyState}>Loading feature adoption...</div>
          ) : sortedFeatures.length === 0 ? (
            <div style={emptyState}>No feature events tracked yet.</div>
          ) : sortedFeatures.map((feature) => (
            <FeatureCard key={feature.feature} feature={feature} />
          ))}
        </div>
      </section>

      <section style={twoColumnGrid}>
        <article style={sectionCard}>
          <h2 style={sectionTitle}>Feature Ranking</h2>
          <div style={rankingTable}>
            <div style={{ ...rankingRow, ...rankingHeader }}>
              <span>Rank</span>
              <span>Feature</span>
              <span>Users</span>
              <span>Usage</span>
              <span>Active Adoption</span>
              <span>Overall Adoption</span>
            </div>
            {sortedFeatures.map((feature, index) => (
              <div key={feature.feature} style={rankingRow}>
                <span>#{index + 1}</span>
                <strong>{label(feature.feature)}</strong>
                <span>{feature.uniqueUsers}</span>
                <span>{feature.totalUsage}</span>
                <span>{feature.activeUserAdoptionRate}%</span>
                <span>{feature.overallAdoptionRate}%</span>
              </div>
            ))}
          </div>
        </article>

        <article style={sectionCard}>
          <h2 style={sectionTitle}>Feature Opportunity Insights</h2>
          <div style={insightList}>
            {insights.map((insight) => (
              <div key={insight} style={insightItem}>{insight}</div>
            ))}
          </div>
        </article>
      </section>

      <section style={sectionCard}>
        <h2 style={sectionTitle}>Usage Trends</h2>
        {trends ? (
          <div style={trendGrid}>
            <Trend title="Daily Coach" data={trends.dailyCoach} />
            <Trend title="Weekly Coach" data={trends.weeklyCoach} />
            <Trend title="Trading DNA" data={trends.tradingDna} />
            <Trend title="AI Review" data={trends.aiReview} />
            <Trend title="Session Review" data={trends.sessionReview} />
          </div>
        ) : (
          <div style={emptyState}>No trend data yet.</div>
        )}
      </section>

      <section style={sectionCard}>
        <h2 style={sectionTitle}>Power Users</h2>
        <div style={powerTable}>
          <div style={{ ...powerRow, ...rankingHeader }}>
            <span>Email</span>
            <span>Plan</span>
            <span>Trades</span>
            <span>Feature Usage</span>
          </div>
          {powerUsers.length === 0 ? (
            <div style={emptyState}>No power users yet.</div>
          ) : powerUsers.map((user) => (
            <div key={user.userId} style={powerRow}>
              <span style={truncateText} title={user.email}>{user.email}</span>
              <span style={user.plan === "PRO" ? proBadge : freeBadge}>{user.plan}</span>
              <span>{user.trades}</span>
              <strong>{user.totalUsage}</strong>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

function Kpi({ label, value, loading, accent = "#e5eefc" }: { label: string; value: string | number; loading: boolean; accent?: string }) {
  return <article style={{ ...kpiCard, borderColor: `${accent}33` }}><span>{label}</span><strong style={{ color: accent }}>{loading ? "..." : value}</strong></article>;
}

function FeatureCard({ feature }: { feature: FeatureAdoption }) {
  return (
    <article style={featureCard}>
      <h3>{label(feature.feature)}</h3>
      <div style={featureMetricRow}><span>{feature.uniqueUsers} users</span><span>{feature.totalUsage} uses</span></div>
      <div style={adoptionStack}>
        <span>Active Adoption</span>
        <strong>{feature.activeUserAdoptionRate}%</strong>
      </div>
      <div style={adoptionStack}>
        <span>Overall Adoption</span>
        <strong>{feature.overallAdoptionRate}%</strong>
      </div>
    </article>
  );
}

function Trend({ title, data }: { title: string; data: { date: string; count: number }[] }) {
  return (
    <article style={trendCard}>
      <h3 style={trendTitle}>{title}</h3>
      {data.length === 0 ? (
        <div style={chartEmpty}>No data available</div>
      ) : (
        <div style={chartWrap}>
          <ResponsiveContainer width="100%" height={230}>
            <LineChart data={data}>
              <CartesianGrid stroke="rgba(148,163,184,0.12)" vertical={false} />
              <XAxis dataKey="date" tick={{ fill: "#94a3b8", fontSize: 10 }} tickFormatter={(value) => String(value).slice(5)} />
              <YAxis tick={{ fill: "#94a3b8", fontSize: 11 }} allowDecimals={false} />
              <Tooltip contentStyle={tooltipStyle} />
              <Line type="monotone" dataKey="count" stroke="#34d399" strokeWidth={2} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
    </article>
  );
}

function buildInsights(summary: FeatureUsageSummary | null, trends: FeatureUsageTrends | null) {
  if (!summary) return ["Feature data is still loading."];
  const insights: string[] = [];
  const tradingDna = summary.features.find((feature) => feature.feature === "TRADING_DNA");
  const weeklyCoach = summary.features.find((feature) => feature.feature === "WEEKLY_COACH");
  if (tradingDna && tradingDna.totalUsage > 0 && tradingDna.activeUserAdoptionRate < 20) {
    insights.push(`Trading DNA is used by only ${tradingDna.activeUserAdoptionRate}% of active users.`);
  }
  const mostUsed = summary.features.find((feature) => feature.feature === summary.mostUsedFeature);
  if (mostUsed) {
    insights.push(`${label(mostUsed.feature)} is used by ${mostUsed.activeUserAdoptionRate}% of active users.`);
    insights.push(`${label(mostUsed.feature)} is the most-used feature.`);
  }
  if (summary.leastUsedFeature) insights.push(`${label(summary.leastUsedFeature)} is the least-used feature.`);
  if (weeklyCoach && weeklyCoach.totalUsage <= weeklyCoach.uniqueUsers) insights.push("Weekly Coach usage appears to drop after first use.");
  if (trends?.sessionReview && trendIsGrowing(trends.sessionReview)) insights.push("Session Review usage is growing.");
  return insights.length ? insights : ["Usage is still sparse. Seed demo data or wait for more user activity."];
}

function trendIsGrowing(data: { count: number }[]) {
  const first = data.slice(0, 15).reduce((sum, point) => sum + point.count, 0);
  const second = data.slice(15).reduce((sum, point) => sum + point.count, 0);
  return second > first;
}

function label(feature?: FeatureKey | null) {
  return feature ? featureLabels[feature] : "None";
}

const pageWrap: CSSProperties = { display: "grid", gap: 22 };
const kpiGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: 14 };
const kpiCard: CSSProperties = { ...card, minHeight: 108, display: "grid", gap: 8, alignContent: "center" };
const sectionCard: CSSProperties = { ...card, minWidth: 0 };
const featureGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(190px, 1fr))", gap: 14, marginTop: 16 };
const featureCard: CSSProperties = { padding: 16, borderRadius: 18, background: "rgba(2,6,23,0.52)", border: "1px solid rgba(148,163,184,0.14)", display: "grid", gap: 10, color: "#cbd5e1" };
const featureMetricRow: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 10, color: "#94a3b8", fontSize: 13 };
const adoptionStack: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 10, alignItems: "center", color: "#94a3b8", fontSize: 13 };
const twoColumnGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))", gap: 18 };
const rankingTable: CSSProperties = { overflowX: "auto", marginTop: 16 };
const rankingRow: CSSProperties = { minWidth: 760, display: "grid", gridTemplateColumns: "70px minmax(180px,1fr) 90px 100px 140px 140px", gap: 12, alignItems: "center", padding: "12px 14px", borderBottom: "1px solid rgba(148,163,184,0.12)", color: "#cbd5e1" };
const rankingHeader: CSSProperties = { color: "#94a3b8", fontSize: 11, fontWeight: 950, textTransform: "uppercase", letterSpacing: "0.08em", background: "rgba(15,23,42,0.48)", borderRadius: 14 };
const trendGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(300px, 1fr))", gap: 16, marginTop: 16 };
const trendCard: CSSProperties = { padding: 14, borderRadius: 18, background: "rgba(2,6,23,0.48)", border: "1px solid rgba(148,163,184,0.13)" };
const trendTitle: CSSProperties = { margin: "0 0 12px", color: "#f8fafc", fontSize: 16 };
const chartWrap: CSSProperties = { width: "100%", height: 230, minWidth: 0 };
const chartEmpty: CSSProperties = { ...chartWrap, display: "grid", placeItems: "center", color: "#94a3b8", borderRadius: 14, border: "1px dashed rgba(148,163,184,0.16)", background: "rgba(2,6,23,0.28)" };
const powerTable: CSSProperties = { overflowX: "auto", marginTop: 16 };
const powerRow: CSSProperties = { minWidth: 720, display: "grid", gridTemplateColumns: "minmax(260px,1fr) 90px 100px 140px", gap: 12, alignItems: "center", padding: "12px 14px", borderBottom: "1px solid rgba(148,163,184,0.12)", color: "#cbd5e1" };
const insightList: CSSProperties = { display: "grid", gap: 10, marginTop: 16 };
const insightItem: CSSProperties = { padding: 14, borderRadius: 16, color: "#bbf7d0", background: "rgba(34,197,94,0.1)", border: "1px solid rgba(34,197,94,0.22)" };
const truncateText: CSSProperties = { minWidth: 0, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" };
const badgeBase: CSSProperties = { width: "fit-content", padding: "7px 9px", borderRadius: 999, fontSize: 12, fontWeight: 950 };
const freeBadge: CSSProperties = { ...badgeBase, color: "#bae6fd", background: "rgba(14,165,233,0.12)", border: "1px solid rgba(56,189,248,0.24)" };
const proBadge: CSSProperties = { ...badgeBase, color: "#bbf7d0", background: "rgba(34,197,94,0.14)", border: "1px solid rgba(34,197,94,0.28)" };
const emptyState: CSSProperties = { color: "#94a3b8", padding: 16 };
const errorBox: CSSProperties = { padding: 14, borderRadius: 14, color: "#fecaca", background: "rgba(239,68,68,0.12)", border: "1px solid rgba(239,68,68,0.28)" };
const warningBox: CSSProperties = { padding: 14, borderRadius: 14, color: "#fde68a", background: "rgba(251,191,36,0.11)", border: "1px solid rgba(251,191,36,0.28)" };
