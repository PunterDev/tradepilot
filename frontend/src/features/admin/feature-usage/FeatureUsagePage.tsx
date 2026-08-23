import { useEffect, useMemo, useState, type CSSProperties } from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  CartesianGrid,
  Legend,
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
  type FeatureUsageActivity,
  type FeatureUsageCategory,
  type FeatureUsageEventType,
  type FeatureUsageFilters,
  type FeatureUsageMetadata,
  type FeatureUsageRange,
  type FeatureUsageSummary,
  type FeatureUsageTrends,
  type FeatureUsageUser,
} from "../../../api/featureUsageApi";
import PageHeader from "../../../layout/PageHeader";
import { card, mutedText, sectionTitle, tooltipStyle } from "../../../styles/dashboardStyles";

const ranges: { value: FeatureUsageRange; label: string }[] = [
  { value: "LAST_7_DAYS", label: "Last 7 days" },
  { value: "LAST_30_DAYS", label: "Last 30 days" },
  { value: "LAST_90_DAYS", label: "Last 90 days" },
  { value: "CURRENT_MONTH", label: "Current month" },
  { value: "PREVIOUS_MONTH", label: "Previous month" },
  { value: "CUSTOM", label: "Custom" },
];

const categoryOptions: { value: FeatureUsageCategory | ""; label: string }[] = [
  { value: "", label: "All categories" },
  { value: "JOURNALING", label: "Journaling" },
  { value: "ANALYTICS", label: "Analytics" },
  { value: "AI_COACHING", label: "AI Coaching" },
  { value: "PERFORMANCE", label: "Performance" },
  { value: "PLAYBOOK", label: "Playbook" },
  { value: "OTHER", label: "Other" },
];

const eventTypeOptions: { value: FeatureUsageEventType | ""; label: string }[] = [
  { value: "", label: "All event types" },
  { value: "PAGE_VIEW", label: "Page views" },
  { value: "ANALYTICS_VIEW", label: "Analytics views" },
  { value: "AI_GENERATION", label: "AI generations" },
  { value: "USER_ACTION", label: "User actions" },
];

const activityOptions: { value: FeatureUsageActivity; label: string }[] = [
  { value: "ALL", label: "All activity" },
  { value: "ACTIVE", label: "Active actions" },
  { value: "PASSIVE", label: "Passive views" },
  { value: "AI", label: "AI features" },
];

const trendColors = ["#34d399", "#22d3ee", "#a78bfa", "#f472b6", "#fbbf24", "#60a5fa", "#fb7185", "#84cc16", "#c084fc", "#2dd4bf", "#f97316"];

export default function FeatureUsagePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const filters = useMemo(() => filtersFromParams(searchParams), [searchParams]);
  const [summary, setSummary] = useState<FeatureUsageSummary | null>(null);
  const [trends, setTrends] = useState<FeatureUsageTrends | null>(null);
  const [users, setUsers] = useState<FeatureUsageUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    async function load() {
      setLoading(true);
      setError("");
      try {
        const [nextSummary, nextTrends, nextUsers] = await Promise.all([
          getFeatureUsageSummary(filters),
          getFeatureUsageTrends(filters),
          getFeatureUsageUsers(filters),
        ]);
        if (!active) return;
        setSummary(nextSummary);
        setTrends(nextTrends);
        setUsers(nextUsers);
      } catch {
        if (active) setError("Unable to load feature usage analytics.");
      } finally {
        if (active) setLoading(false);
      }
    }

    void load();
    return () => {
      active = false;
    };
  }, [filters]);

  const metadata = useMemo(() => metadataMap(summary?.metadata), [summary?.metadata]);
  const sortedFeatures = useMemo(() => {
    return [...(summary?.features ?? [])].sort((a, b) => b.activeUserAdoptionRate - a.activeUserAdoptionRate || b.totalUsage - a.totalUsage);
  }, [summary]);
  const volumeFeatures = useMemo(() => [...(summary?.features ?? [])].sort((a, b) => b.totalUsage - a.totalUsage), [summary]);
  const trendData = useMemo(() => buildTrendData(trends, metadata), [trends, metadata]);
  const visibleTrendKeys = useMemo(() => {
    return Object.keys(trends?.series ?? {})
      .filter((key) => (trends?.series?.[key as FeatureKey] ?? []).some((point) => point.count > 0))
      .slice(0, 8) as FeatureKey[];
  }, [trends]);

  function updateFilter(name: keyof FeatureUsageFilters, value: string | boolean) {
    const next = new URLSearchParams(searchParams);
    if (value === "" || value === "ALL") next.delete(name);
    else next.set(name, String(value));
    if (name === "range" && value !== "CUSTOM") {
      next.delete("startDate");
      next.delete("endDate");
    }
    setSearchParams(next, { replace: true });
  }

  return (
    <div style={pageWrap}>
      <PageHeader
        label="Admin Analytics"
        title="Feature Usage"
        description="Separate adoption, active actions, passive views, and AI generation volume so product decisions are grounded in real behavior."
        actions={<Link to="/admin/feature-funnels" style={shortcutLink}>View Funnels</Link>}
      />

      <section style={filterBar}>
        <Select label="Range" value={filters.range ?? "LAST_30_DAYS"} onChange={(value) => updateFilter("range", value)} options={ranges} />
        {filters.range === "CUSTOM" && (
          <>
            <Field label="Start" type="date" value={filters.startDate ?? ""} onChange={(value) => updateFilter("startDate", value)} />
            <Field label="End" type="date" value={filters.endDate ?? ""} onChange={(value) => updateFilter("endDate", value)} />
          </>
        )}
        <Select label="Plan" value={filters.plan ?? ""} onChange={(value) => updateFilter("plan", value)} options={[{ value: "", label: "All plans" }, { value: "FREE", label: "Free" }, { value: "PRO", label: "Pro" }]} />
        <Select label="Category" value={filters.category ?? ""} onChange={(value) => updateFilter("category", value)} options={categoryOptions} />
        <Select label="Event Type" value={filters.eventType ?? ""} onChange={(value) => updateFilter("eventType", value)} options={eventTypeOptions} />
        <Select label="Activity" value={filters.activity ?? "ALL"} onChange={(value) => updateFilter("activity", value)} options={activityOptions} />
        <label style={toggleLabel}>
          <input
            type="checkbox"
            checked={filters.excludeAdmins !== false}
            onChange={(event) => updateFilter("excludeAdmins", event.target.checked)}
          />
          Exclude admins
        </label>
      </section>

      {error && <div style={errorBox}>{error}</div>}
      {!loading && summary && summary.totalEvents === 0 && (
        <div style={warningBox}>
          No feature events detected for {dateRange(summary)}. Widen the date range or verify telemetry emitters.
        </div>
      )}
      {!loading && (summary?.dataQuality?.length ?? 0) > 0 && (
        <section style={warningPanel}>
          <strong>Data quality warnings</strong>
          {(summary?.dataQuality ?? []).map((issue) => (
            <span key={issue.issue}>{issue.issue}: {issue.eventCount} events. {issue.suggestedFix}</span>
          ))}
        </section>
      )}

      <section style={kpiGrid}>
        <Kpi label="Active Users" value={summary?.activeUsers ?? 0} helper={dateRange(summary)} loading={loading} />
        <Kpi label="Total Events" value={summary?.totalEvents ?? 0} helper="All tracked events" loading={loading} accent="#93c5fd" />
        <Kpi label="Highest Adoption" value={featureLabel(summary?.highestAdoptionFeature, metadata)} helper={adoptionHelper(summary, summary?.highestAdoptionFeature)} loading={loading} accent="#67e8f9" />
        <Kpi label="Most Used by Volume" value={featureLabel(summary?.highestVolumeFeature ?? summary?.mostUsedFeature, metadata)} helper={volumeHelper(summary, summary?.highestVolumeFeature ?? summary?.mostUsedFeature)} loading={loading} accent="#86efac" />
        <Kpi label="Unique Features Used" value={summary?.uniqueFeaturesUsed ?? 0} helper={`${summary?.features?.length ?? 0} tracked features`} loading={loading} accent="#c4b5fd" />
        <Kpi label="Events / Active User" value={formatDecimal(summary?.avgEventsPerActiveUser)} helper="Usage volume, not adoption" loading={loading} accent="#fbbf24" />
      </section>

      <section style={sectionCard}>
        <div style={sectionHeader}>
          <div>
            <h2 style={sectionTitle}>Adoption vs. Usage Volume</h2>
            <p style={sectionCopy}>Adoption is distinct users. Usage volume is total events. Passive views are separated from active actions to avoid false product confidence.</p>
          </div>
          <span style={subtleBadge}>{summary ? dateRange(summary) : "Loading"}</span>
        </div>
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

      <section style={sectionCard}>
        <div style={sectionHeader}>
          <div>
            <h2 style={sectionTitle}>Usage Trend</h2>
            <p style={sectionCopy}>One timeline across the filtered feature set. Use category and activity filters to isolate active AI actions from page views.</p>
          </div>
          {trends?.fastestGrowingFeature && <span style={growthBadge}>Fastest growth: {featureLabel(trends.fastestGrowingFeature, metadata)}</span>}
        </div>
        {trendData.length > 0 && visibleTrendKeys.length > 0 ? (
          <div style={chartWrap}>
            <ResponsiveContainer width="100%" height={330}>
              <LineChart data={trendData}>
                <CartesianGrid stroke="rgba(148,163,184,0.12)" vertical={false} />
                <XAxis dataKey="date" tick={{ fill: "#94a3b8", fontSize: 11 }} tickFormatter={(value) => String(value).slice(5)} />
                <YAxis tick={{ fill: "#94a3b8", fontSize: 11 }} allowDecimals={false} />
                <Tooltip contentStyle={tooltipStyle} />
                <Legend />
                {visibleTrendKeys.map((feature, index) => (
                  <Line
                    key={feature}
                    type="monotone"
                    dataKey={feature}
                    name={featureLabel(feature, metadata)}
                    stroke={trendColors[index % trendColors.length]}
                    strokeWidth={2}
                    dot={false}
                  />
                ))}
              </LineChart>
            </ResponsiveContainer>
          </div>
        ) : (
          <div style={chartEmpty}>No trend data for this filter.</div>
        )}
      </section>

      <section style={twoColumnGrid}>
        <article style={sectionCard}>
          <h2 style={sectionTitle}>Feature Ranking</h2>
          <div style={rankingTable}>
            <div style={{ ...rankingRow, ...rankingHeader }}>
              <span>Rank</span>
              <span>Feature</span>
              <span>Category</span>
              <span>Users</span>
              <span>Events</span>
              <span>Events/User</span>
              <span>Status</span>
            </div>
            {volumeFeatures.map((feature, index) => (
              <div key={feature.feature} style={rankingRow}>
                <span>#{index + 1}</span>
                <strong>{feature.displayName || featureLabel(feature.feature, metadata)}</strong>
                <span>{pretty(feature.category)}</span>
                <span>{feature.uniqueUsers}</span>
                <span>{feature.totalUsage}</span>
                <span>{formatDecimal(feature.eventsPerUser)}</span>
                <span style={statusBadge(feature.status)}>{feature.status}</span>
              </div>
            ))}
          </div>
        </article>

        <article style={sectionCard}>
          <h2 style={sectionTitle}>Product Insights</h2>
          <div style={insightList}>
            {(summary?.insights ?? []).length === 0 ? (
              <div style={emptyState}>No insights yet.</div>
            ) : summary?.insights.map((insight) => (
              <div key={`${insight.type}-${insight.feature ?? "global"}`} style={insightItem}>
                <div style={insightTopline}>
                  <strong>{insight.title}</strong>
                  <span style={priorityBadge(insight.priority)}>{insight.priority}</span>
                </div>
                <p>{insight.evidence}</p>
                <span>{insight.whyItMatters}</span>
                <em>{insight.suggestedInvestigation}</em>
              </div>
            ))}
          </div>
        </article>
      </section>

      <section style={sectionCard}>
        <div style={sectionHeader}>
          <div>
            <h2 style={sectionTitle}>Power Users</h2>
            <p style={sectionCopy}>Emails are masked by default. Use this for support prioritization and cohort research, not public reporting.</p>
          </div>
          <span style={subtleBadge}>Top {users.length}</span>
        </div>
        <div style={powerTable}>
          <div style={{ ...powerRow, ...rankingHeader }}>
            <span>User</span>
            <span>Plan</span>
            <span>Trades</span>
            <span>Active Actions</span>
            <span>AI Generations</span>
            <span>Total Events</span>
            <span>Last Active</span>
            <span>Engagement</span>
          </div>
          {users.length === 0 ? (
            <div style={emptyState}>No power users for this filter.</div>
          ) : users.map((user) => (
            <div key={user.userId} style={powerRow}>
              <span style={truncateText} title={user.maskedEmail}>{user.maskedEmail}</span>
              <span style={user.plan === "PRO" ? proBadge : freeBadge}>{user.plan}</span>
              <span>{user.trades}</span>
              <span>{user.activeActions}</span>
              <span>{user.aiGenerations}</span>
              <strong>{user.totalEvents}</strong>
              <span>{formatDateTime(user.lastActiveAt)}</span>
              <span>{user.engagementLevel}</span>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

function Select({ label, value, onChange, options }: { label: string; value: string; onChange: (value: string) => void; options: { value: string; label: string }[] }) {
  return (
    <label style={fieldWrap}>
      <span>{label}</span>
      <select value={value} onChange={(event) => onChange(event.target.value)} style={inputStyle}>
        {options.map((option) => <option key={option.value || "all"} value={option.value}>{option.label}</option>)}
      </select>
    </label>
  );
}

function Field({ label, value, onChange, type = "text" }: { label: string; value: string; onChange: (value: string) => void; type?: string }) {
  return (
    <label style={fieldWrap}>
      <span>{label}</span>
      <input type={type} value={value} onChange={(event) => onChange(event.target.value)} style={inputStyle} />
    </label>
  );
}

function Kpi({ label, value, helper, loading, accent = "#e5eefc" }: { label: string; value: string | number; helper?: string; loading: boolean; accent?: string }) {
  return (
    <article style={{ ...kpiCard, borderColor: `${accent}33` }}>
      <span>{label}</span>
      <strong style={{ color: accent }}>{loading ? "..." : value}</strong>
      {helper && <small>{loading ? "" : helper}</small>}
    </article>
  );
}

function FeatureCard({ feature }: { feature: FeatureAdoption }) {
  return (
    <article style={featureCard}>
      <div style={featureHeader}>
        <h3>{feature.displayName}</h3>
        <span style={feature.passiveEvent ? passiveBadge : activeBadge}>{feature.passiveEvent ? "Passive" : feature.paidAction ? "Paid AI" : "Active"}</span>
      </div>
      <p>{feature.description}</p>
      <div style={metricPair}><span>Adoption</span><strong>{feature.activeUserAdoptionRate}%</strong></div>
      <div style={progressTrack}><div style={{ ...progressFill, width: `${Math.min(feature.activeUserAdoptionRate, 100)}%` }} /></div>
      <div style={featureMetricRow}>
        <span>{feature.uniqueUsers} users</span>
        <span>{feature.totalUsage} events</span>
        <span>{feature.eventsPerUser}/user</span>
      </div>
      <div style={featureMetricRow}>
        <span>{feature.activeEventCount} active</span>
        <span>{feature.passiveEventCount} passive</span>
        <span>{feature.aiGenerationCount} AI</span>
      </div>
      <div style={featureFooter}>
        <span>{pretty(feature.category)}</span>
        <strong>{feature.status}</strong>
      </div>
    </article>
  );
}

function filtersFromParams(params: URLSearchParams): FeatureUsageFilters {
  const range = (params.get("range") || "LAST_30_DAYS") as FeatureUsageRange;
  return {
    range,
    startDate: params.get("startDate") || undefined,
    endDate: params.get("endDate") || undefined,
    plan: (params.get("plan") as "FREE" | "PRO" | null) || undefined,
    category: (params.get("category") as FeatureUsageCategory | null) || undefined,
    eventType: (params.get("eventType") as FeatureUsageEventType | null) || undefined,
    excludeAdmins: params.get("excludeAdmins") === null ? true : params.get("excludeAdmins") === "true",
    activity: (params.get("activity") as FeatureUsageActivity | null) || "ALL",
  };
}

function metadataMap(items?: FeatureUsageMetadata[]) {
  return new Map((items ?? []).map((item) => [item.key, item]));
}

function featureLabel(feature: FeatureKey | null | undefined, metadata: Map<FeatureKey, FeatureUsageMetadata>) {
  if (!feature) return "None";
  return metadata.get(feature)?.displayName || `Unknown feature: ${feature}`;
}

function buildTrendData(trends: FeatureUsageTrends | null, metadata: Map<FeatureKey, FeatureUsageMetadata>) {
  if (!trends?.series) return [];
  const rows = new Map<string, Record<string, string | number>>();
  Object.entries(trends.series).forEach(([feature, points]) => {
    const key = feature as FeatureKey;
    if (!metadata.has(key)) return;
    (points ?? []).forEach((point) => {
      const row = rows.get(point.date) ?? { date: point.date };
      row[key] = point.count;
      rows.set(point.date, row);
    });
  });
  return [...rows.values()].sort((a, b) => String(a.date).localeCompare(String(b.date)));
}

function adoptionHelper(summary: FeatureUsageSummary | null, feature?: FeatureKey | null) {
  const item = summary?.features.find((entry) => entry.feature === feature);
  return item ? `${item.uniqueUsers} users · ${item.activeUserAdoptionRate}% active adoption` : "";
}

function volumeHelper(summary: FeatureUsageSummary | null, feature?: FeatureKey | null) {
  const item = summary?.features.find((entry) => entry.feature === feature);
  return item ? `${item.totalUsage} events · ${item.eventsPerUser}/user` : "";
}

function dateRange(summary: FeatureUsageSummary | null | undefined) {
  if (!summary?.selectedStartDate || !summary.selectedEndDate) return "";
  return `${summary.selectedStartDate} to ${summary.selectedEndDate}`;
}

function pretty(value?: string | null) {
  return value ? value.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (letter: string) => letter.toUpperCase()) : "None";
}

function formatDecimal(value?: number | null) {
  if (value == null) return "0.0";
  return Number(value).toFixed(1);
}

function formatDateTime(value?: string | null) {
  if (!value) return "Never";
  return new Date(value).toLocaleDateString(undefined, { month: "short", day: "numeric" });
}

function statusBadge(status: string): CSSProperties {
  const color = status.includes("High") ? "#86efac" : status.includes("Under") || status.includes("Low") ? "#fde68a" : "#cbd5e1";
  return { ...badgeBase, color, background: `${color}18`, border: `1px solid ${color}33` };
}

function priorityBadge(priority: string): CSSProperties {
  const color = priority === "High" ? "#fca5a5" : priority === "Medium" ? "#fde68a" : "#bae6fd";
  return { ...badgeBase, color, background: `${color}16`, border: `1px solid ${color}33` };
}

const pageWrap: CSSProperties = { display: "grid", gap: 22 };
const filterBar: CSSProperties = { ...card, display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))", gap: 12, alignItems: "end" };
const fieldWrap: CSSProperties = { display: "grid", gap: 7, color: "#94a3b8", fontSize: 12, fontWeight: 900, textTransform: "uppercase", letterSpacing: 0 };
const inputStyle: CSSProperties = { width: "100%", border: "1px solid rgba(148,163,184,0.2)", borderRadius: 12, background: "rgba(2,6,23,0.72)", color: "#e5eefc", padding: "10px 11px", outline: "none" };
const toggleLabel: CSSProperties = { minHeight: 42, display: "flex", alignItems: "center", gap: 9, color: "#cbd5e1", fontSize: 13, fontWeight: 800 };
const kpiGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: 14 };
const kpiCard: CSSProperties = { ...card, minHeight: 118, display: "grid", gap: 7, alignContent: "center" };
const sectionCard: CSSProperties = { ...card, minWidth: 0 };
const sectionHeader: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 16, alignItems: "flex-start", flexWrap: "wrap" };
const sectionCopy: CSSProperties = { ...mutedText, margin: "7px 0 0", maxWidth: 760 };
const featureGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))", gap: 14, marginTop: 16 };
const featureCard: CSSProperties = { padding: 16, borderRadius: 18, background: "rgba(2,6,23,0.52)", border: "1px solid rgba(148,163,184,0.14)", display: "grid", gap: 10, color: "#cbd5e1" };
const featureHeader: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 10, alignItems: "start" };
const featureMetricRow: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 10, color: "#94a3b8", fontSize: 12, flexWrap: "wrap" };
const featureFooter: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 12, alignItems: "center", color: "#94a3b8", fontSize: 12 };
const metricPair: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 12, color: "#94a3b8", fontSize: 13 };
const progressTrack: CSSProperties = { height: 7, borderRadius: 999, background: "rgba(148,163,184,0.14)", overflow: "hidden" };
const progressFill: CSSProperties = { height: "100%", borderRadius: 999, background: "linear-gradient(90deg,#14b8a6,#86efac)" };
const twoColumnGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(340px, 1fr))", gap: 18 };
const rankingTable: CSSProperties = { overflowX: "auto", marginTop: 16 };
const rankingRow: CSSProperties = { minWidth: 920, display: "grid", gridTemplateColumns: "64px minmax(210px,1fr) 130px 80px 90px 110px 160px", gap: 12, alignItems: "center", padding: "12px 14px", borderBottom: "1px solid rgba(148,163,184,0.12)", color: "#cbd5e1" };
const rankingHeader: CSSProperties = { color: "#94a3b8", fontSize: 11, fontWeight: 950, textTransform: "uppercase", letterSpacing: 0, background: "rgba(15,23,42,0.48)", borderRadius: 14 };
const chartWrap: CSSProperties = { width: "100%", height: 330, minWidth: 0, marginTop: 16 };
const chartEmpty: CSSProperties = { height: 240, display: "grid", placeItems: "center", color: "#94a3b8", borderRadius: 14, border: "1px dashed rgba(148,163,184,0.16)", background: "rgba(2,6,23,0.28)", marginTop: 16 };
const powerTable: CSSProperties = { overflowX: "auto", marginTop: 16 };
const powerRow: CSSProperties = { minWidth: 980, display: "grid", gridTemplateColumns: "minmax(180px,1fr) 80px 90px 120px 120px 100px 110px 140px", gap: 12, alignItems: "center", padding: "12px 14px", borderBottom: "1px solid rgba(148,163,184,0.12)", color: "#cbd5e1" };
const insightList: CSSProperties = { display: "grid", gap: 10, marginTop: 16 };
const insightItem: CSSProperties = { padding: 14, borderRadius: 16, color: "#cbd5e1", background: "rgba(15,23,42,0.56)", border: "1px solid rgba(148,163,184,0.15)", display: "grid", gap: 7 };
const insightTopline: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 10, alignItems: "center" };
const truncateText: CSSProperties = { minWidth: 0, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" };
const badgeBase: CSSProperties = { width: "fit-content", padding: "6px 9px", borderRadius: 999, fontSize: 11, fontWeight: 950 };
const subtleBadge: CSSProperties = { ...badgeBase, color: "#bae6fd", background: "rgba(14,165,233,0.12)", border: "1px solid rgba(56,189,248,0.24)" };
const growthBadge: CSSProperties = { ...badgeBase, color: "#bbf7d0", background: "rgba(34,197,94,0.14)", border: "1px solid rgba(34,197,94,0.28)" };
const activeBadge: CSSProperties = { ...badgeBase, color: "#bbf7d0", background: "rgba(34,197,94,0.12)", border: "1px solid rgba(34,197,94,0.24)" };
const passiveBadge: CSSProperties = { ...badgeBase, color: "#bae6fd", background: "rgba(14,165,233,0.11)", border: "1px solid rgba(56,189,248,0.22)" };
const freeBadge: CSSProperties = { ...badgeBase, color: "#bae6fd", background: "rgba(14,165,233,0.12)", border: "1px solid rgba(56,189,248,0.24)" };
const proBadge: CSSProperties = { ...badgeBase, color: "#bbf7d0", background: "rgba(34,197,94,0.14)", border: "1px solid rgba(34,197,94,0.28)" };
const emptyState: CSSProperties = { color: "#94a3b8", padding: 16 };
const errorBox: CSSProperties = { padding: 14, borderRadius: 14, color: "#fecaca", background: "rgba(239,68,68,0.12)", border: "1px solid rgba(239,68,68,0.28)" };
const warningBox: CSSProperties = { padding: 14, borderRadius: 14, color: "#fde68a", background: "rgba(251,191,36,0.11)", border: "1px solid rgba(251,191,36,0.28)" };
const warningPanel: CSSProperties = { ...warningBox, display: "grid", gap: 6 };
const shortcutLink: CSSProperties = { width: "fit-content", padding: "10px 13px", borderRadius: 999, color: "#bbf7d0", background: "rgba(34,197,94,0.12)", border: "1px solid rgba(34,197,94,0.28)", fontSize: 13, fontWeight: 900, textDecoration: "none" };
