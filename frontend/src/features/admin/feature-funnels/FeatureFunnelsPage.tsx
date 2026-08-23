import { useEffect, useMemo, useState, type CSSProperties } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import {
  getFeatureFunnelUsers,
  getFeatureFunnels,
  type FeatureFunnelDashboard,
  type FeatureFunnelFilters,
  type FeatureFunnelKey,
  type FeatureFunnelSummary,
  type FeatureFunnelUser,
} from "../../../api/featureFunnelApi";
import type { FeatureUsageCategory } from "../../../api/featureUsageApi";
import PageHeader from "../../../layout/PageHeader";
import { button, card, mutedText, sectionTitle } from "../../../styles/dashboardStyles";

const funnels: { value: FeatureFunnelKey | ""; label: string }[] = [
  { value: "", label: "All funnels" },
  { value: "PLAYBOOK_ACTIVATION", label: "Playbook Activation" },
  { value: "JOURNEY_ENGAGEMENT", label: "Journey Engagement" },
  { value: "LEAK_DETECTOR", label: "Leak Detector" },
  { value: "TRADE_REVIEW", label: "Trade Review" },
  { value: "SESSION_REVIEW", label: "Session Review" },
  { value: "FREE_TO_PRO_UPGRADE", label: "Free-to-Pro Upgrade" },
  { value: "AI_CREDIT_PURCHASE", label: "AI Credit Purchase" },
];

const ranges = [
  { value: "LAST_7_DAYS", label: "Last 7 days" },
  { value: "LAST_30_DAYS", label: "Last 30 days" },
  { value: "LAST_90_DAYS", label: "Last 90 days" },
  { value: "CURRENT_MONTH", label: "Current month" },
  { value: "PREVIOUS_MONTH", label: "Previous month" },
  { value: "CUSTOM", label: "Custom" },
];

const categories: { value: FeatureUsageCategory | ""; label: string }[] = [
  { value: "", label: "All categories" },
  { value: "PLAYBOOK", label: "Playbook" },
  { value: "PERFORMANCE", label: "Performance" },
  { value: "AI_COACHING", label: "AI Coaching" },
  { value: "BILLING", label: "Billing" },
];

export default function FeatureFunnelsPage() {
  const { funnelKey } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const routeFunnel = funnelKey as FeatureFunnelKey | undefined;
  const filters = useMemo(() => filtersFromParams(searchParams, routeFunnel), [searchParams, routeFunnel]);
  const [dashboard, setDashboard] = useState<FeatureFunnelDashboard | null>(null);
  const [users, setUsers] = useState<FeatureFunnelUser[]>([]);
  const [selectedFunnel, setSelectedFunnel] = useState<FeatureFunnelKey | null>(routeFunnel ?? null);
  const [loading, setLoading] = useState(true);
  const [userLoading, setUserLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    async function load() {
      setLoading(true);
      setError("");
      try {
        const next = await getFeatureFunnels(filters);
        if (!active) return;
        setDashboard(next);
        setSelectedFunnel(routeFunnel ?? filters.funnel ?? next.funnelSummaries[0]?.funnelKey ?? null);
      } catch {
        if (active) setError("Unable to load feature funnel analytics.");
      } finally {
        if (active) setLoading(false);
      }
    }
    void load();
    return () => { active = false; };
  }, [filters, routeFunnel]);

  const selected = useMemo(() => dashboard?.funnelSummaries.find((item) => item.funnelKey === selectedFunnel) ?? dashboard?.funnelSummaries[0] ?? null, [dashboard, selectedFunnel]);

  useEffect(() => {
    let active = true;
    async function loadUsers() {
      if (!selected) return;
      setUserLoading(true);
      try {
        const nextUsers = await getFeatureFunnelUsers(selected.funnelKey, filters, 0, 10);
        if (active) setUsers(nextUsers);
      } catch {
        if (active) setUsers([]);
      } finally {
        if (active) setUserLoading(false);
      }
    }
    void loadUsers();
    return () => { active = false; };
  }, [selected?.funnelKey, filters]);

  function updateFilter(name: keyof FeatureFunnelFilters, value: string | boolean | number) {
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
        title="Feature Funnels & Conversion Analytics"
        description="See where users complete, abandon, repeat, and convert across important TradePilot workflows."
        actions={<span style={adminBadge}>Admin Analytics</span>}
      />

      <section style={filterBar}>
        <Select label="Range" value={filters.range ?? "LAST_30_DAYS"} options={ranges} onChange={(value) => updateFilter("range", value)} />
        {filters.range === "CUSTOM" && (
          <>
            <Field label="Start" type="date" value={filters.startDate ?? ""} onChange={(value) => updateFilter("startDate", value)} />
            <Field label="End" type="date" value={filters.endDate ?? ""} onChange={(value) => updateFilter("endDate", value)} />
          </>
        )}
        <Select label="Funnel" value={filters.funnel ?? ""} options={funnels} onChange={(value) => updateFilter("funnel", value)} />
        <Select label="Plan" value={filters.plan ?? ""} options={[{ value: "", label: "All plans" }, { value: "FREE", label: "Free" }, { value: "PRO", label: "Pro" }]} onChange={(value) => updateFilter("plan", value)} />
        <Select label="Category" value={filters.category ?? ""} options={categories} onChange={(value) => updateFilter("category", value)} />
        <Select label="Mode" value={filters.calculationMode ?? "UNIQUE_USERS"} options={[{ value: "UNIQUE_USERS", label: "Unique users" }, { value: "FUNNEL_INSTANCES", label: "Instances" }, { value: "SESSIONS", label: "Sessions" }]} onChange={(value) => updateFilter("calculationMode", value)} />
        <label style={toggleLabel}><input type="checkbox" checked={filters.excludeAdmins !== false} onChange={(event) => updateFilter("excludeAdmins", event.target.checked)} /> Exclude admins</label>
      </section>

      {error && <div style={errorBox}>{error}</div>}
      {!loading && dashboard?.dataQualityWarnings?.map((warning) => <div key={warning} style={warningBox}>{warning}</div>)}

      <section style={kpiGrid}>
        <Kpi label="Funnel Entrants" value={sum(dashboard?.funnelSummaries, "enteredUsers")} loading={loading} />
        <Kpi label="Completions" value={sum(dashboard?.funnelSummaries, "completedUsers")} loading={loading} accent="#86efac" />
        <Kpi label="Largest Drop-off" value={dashboard?.topDropoff ? `${dashboard.topDropoff.dropoffRate}%` : "None"} helper={dashboard?.topDropoff?.transition} loading={loading} accent="#fca5a5" />
        <Kpi label="Strongest Repeat Use" value={dashboard?.strongestRepeatUse ? `${dashboard.strongestRepeatUse.repeatRate14d}%` : "None"} helper={dashboard?.strongestRepeatUse?.displayName} loading={loading} accent="#67e8f9" />
        <Kpi label="Generated" value={dashboard ? formatDate(dashboard.generatedAt) : "..."} helper={dashboard ? `${dashboard.selectedStartDate} to ${dashboard.selectedEndDate}` : ""} loading={loading} accent="#c4b5fd" />
      </section>

      <section style={overviewGrid}>
        {(dashboard?.funnelSummaries ?? []).map((funnel) => (
          <button key={funnel.funnelKey} type="button" onClick={() => setSelectedFunnel(funnel.funnelKey)} style={{ ...funnelCard, borderColor: selected?.funnelKey === funnel.funnelKey ? "rgba(45,212,191,0.55)" : "rgba(148,163,184,0.15)" }}>
            <span style={smallBadge}>{funnel.category}</span>
            <strong>{funnel.displayName}</strong>
            <p>{funnel.description}</p>
            <div style={miniGrid}>
              <Metric label="Entered" value={funnel.enteredUsers} />
              <Metric label="Completed" value={funnel.completedUsers} />
              <Metric label="Completion" value={`${funnel.completionRate}%`} />
              <Metric label="Repeat 14d" value={`${funnel.repeatRate14d}%`} />
            </div>
            <span style={confidenceBadge(funnel.confidence)}>{funnel.confidence}</span>
          </button>
        ))}
      </section>

      {selected && (
        <>
          <section style={sectionCard}>
            <div style={sectionHeader}>
              <div>
                <h2 style={sectionTitle}>{selected.displayName}</h2>
                <p style={sectionCopy}>{selected.description}</p>
              </div>
              <Link to={`/admin/feature-funnels/${selected.funnelKey}`} style={{ ...button, width: "auto", textDecoration: "none" }}>View funnel</Link>
            </div>
            <div style={stepGrid}>
              {selected.steps.map((step) => (
                <article key={step.stepKey} style={stepCard}>
                  <div style={stepTop}>
                    <span style={stepNumber}>{step.stepOrder}</span>
                    <span style={step.passive ? passiveBadge : activeBadge}>{step.activePassiveClassification}</span>
                  </div>
                  <strong>{step.displayName}</strong>
                  <div style={barTrack} aria-label={`${step.percentOfStarters}% of starters reached ${step.displayName}`}>
                    <div style={{ ...barFill, width: `${Math.min(100, step.percentOfStarters)}%` }} />
                  </div>
                  <div style={miniGrid}>
                    <Metric label="Users" value={step.usersReached} />
                    <Metric label="Of starters" value={`${step.percentOfStarters}%`} />
                    <Metric label="From prior" value={`${step.percentFromPreviousStep}%`} />
                    <Metric label="Drop-off" value={`${step.dropoffRate}%`} />
                  </div>
                </article>
              ))}
            </div>
          </section>

          <section style={twoColumn}>
            <article style={sectionCard}>
              <h2 style={sectionTitle}>Drop-off Analysis</h2>
              {selected.largestDropoffStep ? (
                <div style={callout}>
                  <strong>{selected.largestDropoffStep.transition}</strong>
                  <span>{selected.largestDropoffStep.dropoffUsers} users dropped off ({selected.largestDropoffStep.dropoffRate}%).</span>
                  <em>{selected.largestDropoffStep.suggestedInvestigation}</em>
                </div>
              ) : <div style={emptyState}>No drop-off detected yet.</div>}
            </article>

            <article style={sectionCard}>
              <h2 style={sectionTitle}>Deterministic Insights</h2>
              <div style={insightList}>
                {(dashboard?.insights ?? []).map((insight) => (
                  <div key={`${insight.type}-${insight.funnelKey ?? "global"}`} style={insightItem}>
                    <strong>{insight.title}</strong>
                    <span>{insight.evidence}</span>
                    <em>{insight.caveat}</em>
                  </div>
                ))}
              </div>
            </article>
          </section>

          <section style={twoColumn}>
            <AssociationCard title="Conversion Association" rows={dashboard?.conversionAssociations.filter((row) => row.funnelKey === selected.funnelKey) ?? []} />
            <RetentionCard rows={dashboard?.retentionAssociations.filter((row) => row.funnelKey === selected.funnelKey) ?? []} />
          </section>

          <section style={sectionCard}>
            <h2 style={sectionTitle}>Cohorts</h2>
            <div style={tableWrap}>
              <div style={{ ...cohortRow, ...tableHeader }}><span>Cohort</span><span>Entrants</span><span>Completion</span><span>7d Return</span><span>14d Return</span><span>30d Return</span></div>
              {(dashboard?.cohorts ?? []).filter((cohort) => cohort.cohortKey.startsWith(selected.funnelKey)).map((cohort) => (
                <div key={cohort.cohortKey} style={cohortRow}><strong>{cohort.cohortLabel}</strong><span>{cohort.entrants}</span><span>{cohort.completionRate}%</span><span>{cohort.return7dRate}%</span><span>{cohort.return14dRate}%</span><span>{cohort.return30dRate}%</span></div>
              ))}
            </div>
          </section>

          <section style={sectionCard}>
            <h2 style={sectionTitle}>Masked User Drill-down</h2>
            {userLoading ? <div style={emptyState}>Loading users...</div> : (
              <div style={tableWrap}>
                <div style={{ ...userRow, ...tableHeader }}><span>User</span><span>Plan</span><span>Stage</span><span>Entry</span><span>Last Active</span><span>Trades</span><span>Status</span></div>
                {users.map((user) => (
                  <div key={user.userId} style={userRow}><strong>{user.maskedEmail}</strong><span>{user.plan}</span><span>{user.stageReached}</span><span>{formatDate(user.entryDate)}</span><span>{formatDate(user.lastActiveAt)}</span><span>{user.tradeBand}</span><span>{user.engagementStatus}</span></div>
                ))}
                {users.length === 0 && <div style={emptyState}>No users reached this funnel in the selected period.</div>}
              </div>
            )}
          </section>
        </>
      )}
    </div>
  );
}

function Select({ label, value, options, onChange }: { label: string; value: string; options: { value: string; label: string }[]; onChange: (value: string) => void }) {
  return <label style={fieldWrap}><span>{label}</span><select style={inputStyle} value={value} onChange={(event) => onChange(event.target.value)}>{options.map((option) => <option key={option.value || "all"} value={option.value}>{option.label}</option>)}</select></label>;
}

function Field({ label, value, type, onChange }: { label: string; value: string; type: string; onChange: (value: string) => void }) {
  return <label style={fieldWrap}><span>{label}</span><input style={inputStyle} type={type} value={value} onChange={(event) => onChange(event.target.value)} /></label>;
}

function Kpi({ label, value, helper, loading, accent = "#e5eefc" }: { label: string; value: string | number; helper?: string; loading: boolean; accent?: string }) {
  return <article style={{ ...kpiCard, borderColor: `${accent}33` }}><span>{label}</span><strong style={{ color: accent }}>{loading ? "..." : value}</strong>{helper && <small>{helper}</small>}</article>;
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return <div style={metric}><span>{label}</span><strong>{value}</strong></div>;
}

function AssociationCard({ title, rows }: { title: string; rows: FeatureFunnelDashboard["conversionAssociations"] }) {
  return <article style={sectionCard}><h2 style={sectionTitle}>{title}</h2>{rows.map((row) => <div key={row.funnelKey} style={callout}><strong>{row.behavior}</strong><span>{row.exposedConversionRate}% exposed conversion vs {row.comparisonConversionRate}% comparison.</span><em>{row.caveat}</em></div>)}{rows.length === 0 && <div style={emptyState}>No association data yet.</div>}</article>;
}

function RetentionCard({ rows }: { rows: FeatureFunnelDashboard["retentionAssociations"] }) {
  return <article style={sectionCard}><h2 style={sectionTitle}>Retention Association</h2>{rows.map((row) => <div key={row.funnelKey} style={callout}><strong>{row.cohort}</strong><span>{row.retention7dRate}% 7d · {row.retention14dRate}% 14d · {row.retention30dRate}% 30d</span><em>{row.caveat}</em></div>)}{rows.length === 0 && <div style={emptyState}>No retention data yet.</div>}</article>;
}

function filtersFromParams(params: URLSearchParams, routeFunnel?: FeatureFunnelKey): FeatureFunnelFilters {
  return {
    funnel: routeFunnel ?? (params.get("funnel") as FeatureFunnelKey | null) ?? undefined,
    range: (params.get("range") as FeatureFunnelFilters["range"] | null) ?? "LAST_30_DAYS",
    startDate: params.get("startDate") ?? undefined,
    endDate: params.get("endDate") ?? undefined,
    plan: (params.get("plan") as "FREE" | "PRO" | null) ?? undefined,
    category: (params.get("category") as FeatureUsageCategory | null) ?? undefined,
    excludeAdmins: params.get("excludeAdmins") === null ? true : params.get("excludeAdmins") === "true",
    includeTestUsers: params.get("includeTestUsers") === "true",
    calculationMode: (params.get("calculationMode") as FeatureFunnelFilters["calculationMode"] | null) ?? "UNIQUE_USERS",
    conversionWindow: params.get("conversionWindow") ? Number(params.get("conversionWindow")) : undefined,
  };
}

function sum(items: FeatureFunnelSummary[] | undefined, key: "enteredUsers" | "completedUsers") {
  return (items ?? []).reduce((total, item) => total + item[key], 0);
}

function formatDate(value?: string | null) {
  if (!value) return "None";
  return new Date(value).toLocaleDateString(undefined, { month: "short", day: "numeric" });
}

function confidenceBadge(confidence: string): CSSProperties {
  const color = confidence === "HIGH" ? "#86efac" : confidence === "MEDIUM" ? "#67e8f9" : confidence === "LOW" ? "#fde68a" : "#cbd5e1";
  return { ...smallBadge, color, background: `${color}16`, borderColor: `${color}33` };
}

const pageWrap: CSSProperties = { display: "grid", gap: 22 };
const filterBar: CSSProperties = { ...card, display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))", gap: 12, alignItems: "end" };
const fieldWrap: CSSProperties = { display: "grid", gap: 7, color: "#94a3b8", fontSize: 12, fontWeight: 900, textTransform: "uppercase" };
const inputStyle: CSSProperties = { width: "100%", border: "1px solid rgba(148,163,184,0.2)", borderRadius: 12, background: "rgba(2,6,23,0.72)", color: "#e5eefc", padding: "10px 11px", outline: "none" };
const toggleLabel: CSSProperties = { minHeight: 42, display: "flex", alignItems: "center", gap: 9, color: "#cbd5e1", fontSize: 13, fontWeight: 800 };
const adminBadge: CSSProperties = { padding: "8px 11px", borderRadius: 999, color: "#bbf7d0", background: "rgba(34,197,94,0.12)", border: "1px solid rgba(34,197,94,0.28)", fontSize: 12, fontWeight: 950 };
const kpiGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(190px, 1fr))", gap: 14 };
const kpiCard: CSSProperties = { ...card, minHeight: 116, display: "grid", gap: 7, alignContent: "center" };
const overviewGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: 14 };
const funnelCard: CSSProperties = { ...card, textAlign: "left", color: "#cbd5e1", cursor: "pointer", display: "grid", gap: 10 };
const miniGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(2, minmax(0, 1fr))", gap: 9 };
const metric: CSSProperties = { display: "grid", gap: 2, color: "#94a3b8", fontSize: 12 };
const sectionCard: CSSProperties = { ...card, minWidth: 0 };
const sectionHeader: CSSProperties = { display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: 16, flexWrap: "wrap" };
const sectionCopy: CSSProperties = { ...mutedText, margin: "7px 0 0", maxWidth: 760 };
const stepGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: 14, marginTop: 18 };
const stepCard: CSSProperties = { padding: 16, borderRadius: 18, background: "rgba(2,6,23,0.52)", border: "1px solid rgba(148,163,184,0.14)", display: "grid", gap: 11 };
const stepTop: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 10, alignItems: "center" };
const stepNumber: CSSProperties = { width: 28, height: 28, borderRadius: 999, display: "grid", placeItems: "center", color: "#020617", background: "linear-gradient(135deg,#2dd4bf,#86efac)", fontWeight: 950 };
const barTrack: CSSProperties = { height: 8, borderRadius: 999, background: "rgba(148,163,184,0.16)", overflow: "hidden" };
const barFill: CSSProperties = { height: "100%", borderRadius: 999, background: "linear-gradient(90deg,#14b8a6,#86efac)" };
const twoColumn: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))", gap: 18 };
const callout: CSSProperties = { display: "grid", gap: 8, padding: 14, borderRadius: 16, color: "#cbd5e1", background: "rgba(15,23,42,0.56)", border: "1px solid rgba(148,163,184,0.15)", marginTop: 14 };
const insightList: CSSProperties = { display: "grid", gap: 10, marginTop: 14 };
const insightItem: CSSProperties = { ...callout, marginTop: 0 };
const tableWrap: CSSProperties = { overflowX: "auto", marginTop: 14 };
const tableHeader: CSSProperties = { color: "#94a3b8", fontSize: 11, fontWeight: 950, textTransform: "uppercase", background: "rgba(15,23,42,0.48)", borderRadius: 14 };
const cohortRow: CSSProperties = { minWidth: 780, display: "grid", gridTemplateColumns: "minmax(160px,1fr) repeat(5, 110px)", gap: 12, padding: "12px 14px", borderBottom: "1px solid rgba(148,163,184,0.12)", color: "#cbd5e1" };
const userRow: CSSProperties = { minWidth: 900, display: "grid", gridTemplateColumns: "minmax(190px,1fr) 80px 120px 110px 110px 90px 130px", gap: 12, padding: "12px 14px", borderBottom: "1px solid rgba(148,163,184,0.12)", color: "#cbd5e1" };
const smallBadge: CSSProperties = { width: "fit-content", padding: "6px 9px", borderRadius: 999, color: "#bae6fd", background: "rgba(14,165,233,0.12)", border: "1px solid rgba(56,189,248,0.24)", fontSize: 11, fontWeight: 950 };
const passiveBadge: CSSProperties = { ...smallBadge, color: "#bae6fd" };
const activeBadge: CSSProperties = { ...smallBadge, color: "#bbf7d0", background: "rgba(34,197,94,0.14)", border: "1px solid rgba(34,197,94,0.28)" };
const errorBox: CSSProperties = { padding: 14, borderRadius: 14, color: "#fecaca", background: "rgba(239,68,68,0.12)", border: "1px solid rgba(239,68,68,0.28)" };
const warningBox: CSSProperties = { padding: 14, borderRadius: 14, color: "#fde68a", background: "rgba(251,191,36,0.11)", border: "1px solid rgba(251,191,36,0.28)" };
const emptyState: CSSProperties = { color: "#94a3b8", padding: 16 };
