import { useEffect, useMemo, useState, type CSSProperties } from "react";
import { Link } from "react-router-dom";
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
  getRetentionCohorts,
  getRetentionSummary,
  getRetentionTrends,
  getRetentionUsers,
  type RetentionCohort,
  type RetentionSummary,
  type RetentionTrendPoint,
  type RetentionTrends,
  type RetentionUser,
} from "../../../api/adminRetentionApi";
import PageHeader from "../../../layout/PageHeader";
import { button, card, mutedText, sectionTitle, tooltipStyle } from "../../../styles/dashboardStyles";

export default function RetentionAnalyticsPage() {
  const [summary, setSummary] = useState<RetentionSummary | null>(null);
  const [trends, setTrends] = useState<RetentionTrends | null>(null);
  const [cohorts, setCohorts] = useState<RetentionCohort[]>([]);
  const [users, setUsers] = useState<RetentionUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function load() {
      setLoading(true);
      setError("");
      try {
        const [nextSummary, nextTrends, nextCohorts, nextUsers] = await Promise.all([
          getRetentionSummary(),
          getRetentionTrends(),
          getRetentionCohorts(),
          getRetentionUsers(),
        ]);
        setSummary(nextSummary);
        setTrends(nextTrends);
        setCohorts(nextCohorts);
        setUsers(nextUsers);
      } catch {
        setError("Unable to load retention analytics.");
      } finally {
        setLoading(false);
      }
    }

    void load();
  }, []);

  const powerUsers = useMemo(() => users.filter((user) => user.status === "POWER_USER").slice(0, 10), [users]);
  const atRiskUsers = useMemo(() => users.filter((user) => user.status === "AT_RISK" || user.status === "INACTIVE").slice(0, 10), [users]);
  const insights = useMemo(() => buildInsights(summary, users), [summary, users]);

  return (
    <div style={pageWrap}>
      <PageHeader
        label="Admin Analytics"
        title="Retention Analytics"
        description="Track who comes back, which traders are engaged, and whether Pro users retain better than Free users."
      />

      {error && <div style={errorBox}>{error}</div>}

      <section style={kpiGrid}>
        <Kpi label="Daily Active Users" value={summary?.dau ?? 0} loading={loading} accent="#86efac" />
        <Kpi label="Weekly Active Users" value={summary?.wau ?? 0} loading={loading} accent="#93c5fd" />
        <Kpi label="Monthly Active Users" value={summary?.mau ?? 0} loading={loading} accent="#67e8f9" />
        <Kpi label="DAU / MAU" value={`${summary?.dauMauRatio ?? 0}%`} loading={loading} accent="#c4b5fd" />
        <Kpi label="7-Day Retention" value={`${summary?.retention7d ?? 0}%`} loading={loading} accent="#fef08a" />
        <Kpi label="30-Day Retention" value={`${summary?.retention30d ?? 0}%`} loading={loading} accent="#fda4af" />
        <Kpi label="Power Users" value={summary?.powerUsers ?? 0} loading={loading} accent="#34d399" />
        <Kpi label="At-Risk Users" value={summary?.atRiskUsers ?? 0} loading={loading} accent="#fb7185" />
      </section>

      <section style={twoColumnGrid}>
        <article style={sectionCard}>
          <h2 style={sectionTitle}>User Activity Funnel</h2>
          <div style={funnel}>
            <FunnelRow label="Registered Users" value={summary?.totalUsers ?? 0} max={summary?.totalUsers ?? 0} />
            <FunnelRow label="Monthly Active Users" value={summary?.mau ?? 0} max={summary?.totalUsers ?? 0} />
            <FunnelRow label="Weekly Active Users" value={summary?.wau ?? 0} max={summary?.totalUsers ?? 0} />
            <FunnelRow label="Daily Active Users" value={summary?.dau ?? 0} max={summary?.totalUsers ?? 0} />
          </div>
        </article>

        <article style={sectionCard}>
          <h2 style={sectionTitle}>Plan Segmentation</h2>
          <div style={segmentGrid}>
            <Segment title="Free Users" segment={summary?.freeUsers} />
            <Segment title="Pro Users" segment={summary?.proUsers} accent="#34d399" />
          </div>
          <p style={{ ...mutedText, marginTop: 14 }}>
            Segments use the same DAU, WAU, and MAU activity windows as the top-level metrics.
          </p>
        </article>
      </section>

      <section style={sectionCard}>
        <h2 style={sectionTitle}>Activity Trends</h2>
        {trends ? (
          <div style={trendGrid}>
            <Trend title="DAU" data={trends.dailyActiveUsers} stroke="#34d399" />
            <Trend title="WAU" data={trends.weeklyActiveUsers} stroke="#38bdf8" />
            <Trend title="MAU" data={trends.monthlyActiveUsers} stroke="#a78bfa" />
          </div>
        ) : (
          <div style={emptyState}>No activity trend data yet.</div>
        )}
      </section>

      <section style={twoColumnGrid}>
        <article style={sectionCard}>
          <h2 style={sectionTitle}>Retention Cohorts</h2>
          <div style={cohortTable}>
            <div style={{ ...cohortRow, ...tableHeader }}>
              <span>Month</span>
              <span>Users</span>
              <span>7-Day</span>
              <span>30-Day</span>
            </div>
            {cohorts.length === 0 ? (
              <div style={emptyState}>No cohorts yet.</div>
            ) : cohorts.map((cohort) => (
              <div key={cohort.cohortMonth} style={cohortRow}>
                <strong>{formatCohort(cohort.cohortMonth)}</strong>
                <span>{cohort.users}</span>
                <span>{cohort.retained7d}%</span>
                <span>{cohort.retained30d}%</span>
              </div>
            ))}
          </div>
        </article>

        <article style={sectionCard}>
          <h2 style={sectionTitle}>Retention Insights</h2>
          <div style={insightList}>
            {insights.map((insight) => (
              <div key={insight} style={insightItem}>{insight}</div>
            ))}
          </div>
        </article>
      </section>

      <section style={twoColumnGrid}>
        <UserTable title="Power Users" users={powerUsers} empty="No power users yet." />
        <UserTable title="Users At Risk" users={atRiskUsers} empty="No at-risk users right now." showDays />
      </section>
    </div>
  );
}

function Kpi({ label, value, loading, accent = "#e5eefc" }: { label: string; value: string | number; loading: boolean; accent?: string }) {
  return (
    <article style={{ ...kpiCard, borderColor: `${accent}33` }}>
      <span>{label}</span>
      <strong style={{ color: accent }}>{loading ? "..." : value}</strong>
    </article>
  );
}

function FunnelRow({ label, value, max }: { label: string; value: number; max: number }) {
  const width = max <= 0 ? 0 : Math.max(6, Math.round((value / max) * 100));
  return (
    <div style={funnelRow}>
      <div style={funnelMeta}><span>{label}</span><strong>{value}</strong></div>
      <div style={progressTrack}><div style={{ ...progressFill, width: `${width}%` }} /></div>
    </div>
  );
}

function Segment({ title, segment, accent = "#38bdf8" }: { title: string; segment?: { dau: number; wau: number; mau: number }; accent?: string }) {
  return (
    <div style={{ ...segmentCard, borderColor: `${accent}33` }}>
      <h3>{title}</h3>
      <div style={segmentMetrics}>
        <span>DAU <strong>{segment?.dau ?? 0}</strong></span>
        <span>WAU <strong>{segment?.wau ?? 0}</strong></span>
        <span>MAU <strong>{segment?.mau ?? 0}</strong></span>
      </div>
    </div>
  );
}

function Trend({ title, data, stroke }: { title: string; data: RetentionTrendPoint[]; stroke: string }) {
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
              <Line type="monotone" dataKey="count" stroke={stroke} strokeWidth={2} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
    </article>
  );
}

function UserTable({ title, users, empty, showDays = false }: { title: string; users: RetentionUser[]; empty: string; showDays?: boolean }) {
  return (
    <article style={sectionCard}>
      <h2 style={sectionTitle}>{title}</h2>
      <div style={userTable}>
        <div style={{ ...userRow, ...tableHeader }}>
          <span>Email</span>
          <span>Plan</span>
          <span>Last Active</span>
          <span>{showDays ? "Days" : "Activity"}</span>
          <span>Action</span>
        </div>
        {users.length === 0 ? (
          <div style={emptyState}>{empty}</div>
        ) : users.map((user) => (
          <div key={user.userId} style={userRow}>
            <span style={truncateText} title={user.email}>{user.email}</span>
            <span style={user.plan === "PRO" ? proBadge : freeBadge}>{user.plan}</span>
            <span>{formatDate(user.lastActive)}</span>
            <strong>{showDays ? user.daysSinceActivity : user.activityCount30d}</strong>
            <Link to={`/admin/users`} style={viewLink}>View User</Link>
          </div>
        ))}
      </div>
    </article>
  );
}

function buildInsights(summary: RetentionSummary | null, users: RetentionUser[]) {
  if (!summary) return ["Retention data is still loading."];
  const insights: string[] = [];
  insights.push(`Only ${summary.retention30d}% of eligible users return after 30 days.`);
  if (summary.mau > 0) insights.push(`${summary.dauMauRatio}% of monthly active users are active daily.`);
  const freeActivity = summary.freeUsers?.mau ?? 0;
  const proActivity = summary.proUsers?.mau ?? 0;
  if (freeActivity > 0 && proActivity > 0) {
    insights.push(`Pro MAU is ${proActivity}; Free MAU is ${freeActivity}. Compare this with plan counts to judge Pro engagement lift.`);
  } else if (proActivity > freeActivity) {
    insights.push("Pro users are currently more active than Free users.");
  }
  if (summary.atRiskUsers > 0) insights.push(`${summary.atRiskUsers} users have gone 7+ days without activity.`);
  const weeklyDrop = users.filter((user) => user.status === "AT_RISK").length > users.filter((user) => user.status === "POWER_USER").length;
  if (weeklyDrop) insights.push("At-risk users outnumber power users; coaching activation may need attention.");
  return insights.length ? insights : ["Retention is sparse. Activity will become more useful after users generate feature events."];
}

function formatCohort(cohortMonth: string) {
  const [year, month] = cohortMonth.split("-");
  const date = new Date(Number(year), Number(month) - 1, 1);
  return date.toLocaleDateString(undefined, { month: "long", year: "numeric" });
}

function formatDate(value?: string | null) {
  if (!value) return "Never";
  return new Date(value).toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
}

const pageWrap: CSSProperties = { display: "grid", gap: 22 };
const kpiGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(170px, 1fr))", gap: 14 };
const kpiCard: CSSProperties = { ...card, minHeight: 108, display: "grid", gap: 8, alignContent: "center" };
const sectionCard: CSSProperties = { ...card, minWidth: 0 };
const twoColumnGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(340px, 1fr))", gap: 18 };
const funnel: CSSProperties = { display: "grid", gap: 15, marginTop: 16 };
const funnelRow: CSSProperties = { display: "grid", gap: 8 };
const funnelMeta: CSSProperties = { display: "flex", justifyContent: "space-between", color: "#cbd5e1" };
const progressTrack: CSSProperties = { height: 12, borderRadius: 999, background: "rgba(15,23,42,0.85)", overflow: "hidden", border: "1px solid rgba(148,163,184,0.14)" };
const progressFill: CSSProperties = { height: "100%", borderRadius: 999, background: "linear-gradient(135deg, #22c55e, #38bdf8)" };
const segmentGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: 14, marginTop: 16 };
const segmentCard: CSSProperties = { padding: 16, borderRadius: 18, background: "rgba(2,6,23,0.52)", border: "1px solid rgba(56,189,248,0.22)", color: "#e5eefc" };
const segmentMetrics: CSSProperties = { display: "grid", gap: 8, color: "#94a3b8" };
const trendGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(300px, 1fr))", gap: 16, marginTop: 16 };
const trendCard: CSSProperties = { padding: 14, borderRadius: 18, background: "rgba(2,6,23,0.48)", border: "1px solid rgba(148,163,184,0.13)" };
const trendTitle: CSSProperties = { margin: "0 0 12px", color: "#f8fafc", fontSize: 16 };
const chartWrap: CSSProperties = { width: "100%", height: 230, minWidth: 0 };
const chartEmpty: CSSProperties = { ...chartWrap, display: "grid", placeItems: "center", color: "#94a3b8", borderRadius: 14, border: "1px dashed rgba(148,163,184,0.16)", background: "rgba(2,6,23,0.28)" };
const cohortTable: CSSProperties = { overflowX: "auto", marginTop: 16 };
const cohortRow: CSSProperties = { minWidth: 520, display: "grid", gridTemplateColumns: "minmax(160px,1fr) 90px 130px 130px", gap: 12, alignItems: "center", padding: "12px 14px", borderBottom: "1px solid rgba(148,163,184,0.12)", color: "#cbd5e1" };
const userTable: CSSProperties = { overflowX: "auto", marginTop: 16 };
const userRow: CSSProperties = { minWidth: 680, display: "grid", gridTemplateColumns: "minmax(240px,1fr) 90px 140px 90px 100px", gap: 12, alignItems: "center", padding: "12px 14px", borderBottom: "1px solid rgba(148,163,184,0.12)", color: "#cbd5e1" };
const tableHeader: CSSProperties = { color: "#94a3b8", fontSize: 11, fontWeight: 950, textTransform: "uppercase", letterSpacing: "0.08em", background: "rgba(15,23,42,0.48)", borderRadius: 14 };
const insightList: CSSProperties = { display: "grid", gap: 10, marginTop: 16 };
const insightItem: CSSProperties = { padding: 14, borderRadius: 16, color: "#bbf7d0", background: "rgba(34,197,94,0.1)", border: "1px solid rgba(34,197,94,0.22)" };
const truncateText: CSSProperties = { minWidth: 0, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" };
const badgeBase: CSSProperties = { width: "fit-content", padding: "7px 9px", borderRadius: 999, fontSize: 12, fontWeight: 950 };
const freeBadge: CSSProperties = { ...badgeBase, color: "#bae6fd", background: "rgba(14,165,233,0.12)", border: "1px solid rgba(56,189,248,0.24)" };
const proBadge: CSSProperties = { ...badgeBase, color: "#bbf7d0", background: "rgba(34,197,94,0.14)", border: "1px solid rgba(34,197,94,0.28)" };
const viewLink: CSSProperties = { ...button, minHeight: 34, width: "fit-content", padding: "8px 12px", textDecoration: "none", fontSize: 12 };
const emptyState: CSSProperties = { color: "#94a3b8", padding: 16 };
const errorBox: CSSProperties = { padding: 14, borderRadius: 14, color: "#fecaca", background: "rgba(239,68,68,0.12)", border: "1px solid rgba(239,68,68,0.28)" };
