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
  getSearchConsoleAnalytics,
  getSeoAnalytics,
  type SearchConsoleAnalyticsResponse,
  type SearchConsolePageRow,
  type SeoAnalyticsLandingPageRow,
  type SeoAnalyticsResponse,
} from "../../../api/seoAnalyticsApi";
import PageHeader from "../../../layout/PageHeader";
import { button, card, mutedText, sectionTitle, tooltipStyle } from "../../../styles/dashboardStyles";

const ranges = [
  { value: "7", label: "Last 7 days" },
  { value: "30", label: "Last 30 days" },
  { value: "90", label: "Last 90 days" },
  { value: "custom", label: "Custom" },
];

function isoDate(date: Date) {
  return date.toISOString().slice(0, 10);
}

function defaultStart(days: number) {
  const date = new Date();
  date.setDate(date.getDate() - (days - 1));
  return isoDate(date);
}

export default function SeoAnalyticsPage() {
  const [range, setRange] = useState("30");
  const [startDate, setStartDate] = useState(defaultStart(30));
  const [endDate, setEndDate] = useState(isoDate(new Date()));
  const [data, setData] = useState<SeoAnalyticsResponse | null>(null);
  const [searchConsole, setSearchConsole] = useState<SearchConsoleAnalyticsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [gscLoading, setGscLoading] = useState(true);
  const [error, setError] = useState("");
  const [gscError, setGscError] = useState("");

  useEffect(() => {
    if (range === "custom") return;
    const days = Number(range);
    setStartDate(defaultStart(days));
    setEndDate(isoDate(new Date()));
  }, [range]);

  useEffect(() => {
    let active = true;
    async function load() {
      setLoading(true);
      setError("");
      try {
        const next = await getSeoAnalytics({ startDate, endDate });
        if (active) setData(next);
      } catch {
        if (active) setError("Unable to load SEO acquisition analytics.");
      } finally {
        if (active) setLoading(false);
      }
    }
    void load();
    return () => { active = false; };
  }, [startDate, endDate]);

  useEffect(() => {
    let active = true;
    async function loadSearchConsole() {
      setGscLoading(true);
      setGscError("");
      try {
        const next = await getSearchConsoleAnalytics({ startDate, endDate });
        if (active) setSearchConsole(next);
      } catch {
        if (active) setGscError("Search Console data is temporarily unavailable.");
      } finally {
        if (active) setGscLoading(false);
      }
    }
    void loadSearchConsole();
    return () => { active = false; };
  }, [startDate, endDate]);

  const chartData = useMemo(() => data?.activationTrend ?? [], [data]);
  const gscPageMap = useMemo(() => {
    const entries = searchConsole?.pages.map((row) => [row.landingPage, row] as const) ?? [];
    return new Map(entries);
  }, [searchConsole]);
  const landingColumnsWithGsc = useMemo(() => landingColumns(gscPageMap, Boolean(searchConsole?.available)), [gscPageMap, searchConsole?.available]);

  return (
    <div style={pageWrap}>
      <PageHeader
        label="Admin Analytics"
        title="SEO Acquisition & Activation"
        description="Connect SEO landing pages and free tools to signups, first imports, first personalized insights, and Pro conversions."
        actions={<span style={adminBadge}>Admin only</span>}
      />

      <section style={filterBar}>
        <label style={fieldWrap}>
          Range
          <select value={range} onChange={(event) => setRange(event.target.value)} style={inputStyle}>
            {ranges.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
          </select>
        </label>
        <label style={fieldWrap}>
          Start
          <input type="date" value={startDate} disabled={range !== "custom"} onChange={(event) => setStartDate(event.target.value)} style={inputStyle} />
        </label>
        <label style={fieldWrap}>
          End
          <input type="date" value={endDate} disabled={range !== "custom"} onChange={(event) => setEndDate(event.target.value)} style={inputStyle} />
        </label>
        <div style={generatedBox}>{data ? `Generated ${formatDateTime(data.generatedAt)}` : "Loading..."}</div>
      </section>

      {error && <div style={errorBox}>{error}</div>}

      <section style={kpiGrid}>
        {(data?.kpis ?? Array.from({ length: 5 })).map((kpi, index) => (
          <article key={kpi?.key ?? index} style={kpiCard}>
            <span style={kpiLabel}>{kpi?.label ?? "Loading"}</span>
            <strong style={kpiValue}>{loading ? "..." : number(kpi?.value ?? 0)}</strong>
            <span style={mutedText}>{kpi?.rate ? `${kpi.rate}% rate` : kpi?.helper ?? ""}</span>
          </article>
        ))}
      </section>

      <section style={gscSection}>
        <div style={sectionHeader}>
          <div>
            <h2 style={sectionTitle}>Google Search Console</h2>
            <p style={mutedParagraph}>Search Console data may be delayed and can differ from real-time TradePilot analytics.</p>
          </div>
          <div style={statusPill(searchConsole?.available)}>{gscLoading ? "Loading" : searchConsole?.status ?? "Not configured"}</div>
        </div>
        {searchConsole?.property && <div style={propertyText}>Property: {searchConsole.property}</div>}
        {searchConsole?.lastRefreshedAt && <div style={propertyText}>Last refreshed: {formatDateTime(searchConsole.lastRefreshedAt)}</div>}
        {(gscError || searchConsole?.error) && <div style={gscNotice}>{gscError || searchConsole?.error}</div>}
        {gscLoading && <div style={emptyState}>Loading Google Search Console data...</div>}
        {searchConsole?.available && (
          <>
            <section style={kpiGrid}>
              <article style={kpiCard}><span style={kpiLabel}>Google Impressions</span><strong style={kpiValue}>{number(searchConsole.summary.impressions)}</strong><span style={mutedText}>Web search visibility</span></article>
              <article style={kpiCard}><span style={kpiLabel}>Google Clicks</span><strong style={kpiValue}>{number(searchConsole.summary.clicks)}</strong><span style={mutedText}>Search Console clicks</span></article>
              <article style={kpiCard}><span style={kpiLabel}>Google CTR</span><strong style={kpiValue}>{percent(searchConsole.summary.ctr)}</strong><span style={mutedText}>{number(searchConsole.summary.nonBrandedClicks)} non-branded clicks</span></article>
              <article style={kpiCard}><span style={kpiLabel}>Average Position</span><strong style={kpiValue}>{decimal(searchConsole.summary.averagePosition)}</strong><span style={mutedText}>{number(searchConsole.summary.brandedClicks)} branded clicks</span></article>
            </section>

            <section style={twoColumnGrid}>
              <article style={innerPanel}>
                <h3 style={subsectionTitle}>Google Search Performance</h3>
                {searchConsole.trend.length ? (
                  <div style={chartWrap}>
                    <ResponsiveContainer width="100%" height="100%">
                      <LineChart data={searchConsole.trend}>
                        <CartesianGrid strokeDasharray="3 3" stroke="rgba(148,163,184,0.14)" />
                        <XAxis dataKey="date" stroke="#94a3b8" tick={{ fontSize: 12 }} />
                        <YAxis stroke="#94a3b8" tick={{ fontSize: 12 }} allowDecimals={false} />
                        <Tooltip contentStyle={tooltipStyle} />
                        <Line type="monotone" dataKey="clicks" stroke="#38bdf8" strokeWidth={2} dot={false} />
                        <Line type="monotone" dataKey="impressions" stroke="#f59e0b" strokeWidth={2} dot={false} />
                      </LineChart>
                    </ResponsiveContainer>
                  </div>
                ) : <div style={chartEmpty}>No Search Console trend data yet.</div>}
              </article>
              <article style={innerPanel}>
                <h3 style={subsectionTitle}>Branded vs Non-Branded</h3>
                <div style={latencyGrid}>
                  <Metric label="Branded clicks" value={number(searchConsole.summary.brandedClicks)} />
                  <Metric label="Non-branded clicks" value={number(searchConsole.summary.nonBrandedClicks)} />
                  <Metric label="Branded impressions" value={number(searchConsole.summary.brandedImpressions)} />
                  <Metric label="Non-branded impressions" value={number(searchConsole.summary.nonBrandedImpressions)} />
                </div>
              </article>
            </section>

            <Table title="Google Search Queries" rows={searchConsole.queries} columns={queryColumns} rowKey={(row) => `${row.query}-${row.landingPage}`} />
            <Table title="SEO Opportunities" rows={searchConsole.opportunities} columns={opportunityColumns} rowKey={(row) => `${row.category}-${row.query}-${row.landingPage}`} />
          </>
        )}
      </section>

      <section style={twoColumnGrid}>
        <article style={sectionCard}>
          <h2 style={sectionTitle}>SEO Funnel</h2>
          <div style={funnelWrap}>
            {(data?.funnel ?? []).map((step) => (
              <div key={step.key} style={funnelRow}>
                <div style={funnelMeta}><strong>{step.label}</strong><span>{number(step.count)}</span></div>
                <div style={progressTrack}><div style={{ ...progressFill, width: `${Math.min(100, step.conversionFromInitial)}%` }} /></div>
                <span style={smallMeta}>{step.conversionFromPrevious}% from prior · {step.conversionFromInitial}% from landing views</span>
              </div>
            ))}
            {!loading && !data?.funnel.length && <div style={emptyState}>No funnel events in this range.</div>}
          </div>
        </article>

        <article style={sectionCard}>
          <h2 style={sectionTitle}>SEO Activated Users Over Time</h2>
          {chartData.length ? (
            <div style={chartWrap}>
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(148,163,184,0.14)" />
                  <XAxis dataKey="date" stroke="#94a3b8" tick={{ fontSize: 12 }} />
                  <YAxis stroke="#94a3b8" tick={{ fontSize: 12 }} allowDecimals={false} />
                  <Tooltip contentStyle={tooltipStyle} />
                  <Line type="monotone" dataKey="signups" stroke="#38bdf8" strokeWidth={2} dot={false} />
                  <Line type="monotone" dataKey="firstInsights" stroke="#34d399" strokeWidth={2} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          ) : <div style={chartEmpty}>No trend data yet.</div>}
        </article>
      </section>

      <section style={twoColumnGrid}>
        <article style={sectionCard}>
          <h2 style={sectionTitle}>Activation Latency</h2>
          <div style={latencyGrid}>
            <Metric label="Signup to first import" value={minutes(data?.latency.medianSignupToFirstImportMinutes)} />
            <Metric label="Signup to first insight" value={minutes(data?.latency.medianSignupToFirstInsightMinutes)} />
          </div>
        </article>
        <article style={sectionCard}>
          <h2 style={sectionTitle}>Data Notes</h2>
          <div style={noteList}>
            {(data?.notes ?? []).map((note) => <div key={note} style={noteItem}>{note}</div>)}
          </div>
        </article>
      </section>

      <Table title="Landing Page Performance" rows={data?.landingPages ?? []} columns={landingColumnsWithGsc} rowKey={(row) => row.landingPage} />
      <Table title="Free Tool Performance" rows={data?.tools ?? []} columns={toolColumns} rowKey={(row) => row.tool} />
      <Table title="Source / UTM Performance" rows={data?.sources ?? []} columns={sourceColumns} rowKey={(row) => `${row.source}-${row.medium}`} />
      <Table title="Campaign Performance" rows={data?.campaigns ?? []} columns={campaignColumns} rowKey={(row) => row.campaign} />
    </div>
  );
}

type Column<T> = { label: string; value: (row: T) => string | number };

function Table<T>({ title, rows, columns, rowKey }: { title: string; rows: T[]; columns: Column<T>[]; rowKey: (row: T) => string }) {
  return (
    <section style={sectionCard}>
      <h2 style={sectionTitle}>{title}</h2>
      <div style={tableWrap}>
        <div style={{ ...tableRow(columns.length), ...tableHeader }}>{columns.map((column) => <span key={column.label}>{column.label}</span>)}</div>
        {rows.map((row) => (
          <div key={rowKey(row)} style={tableRow(columns.length)}>{columns.map((column) => <span key={column.label}>{column.value(row)}</span>)}</div>
        ))}
        {rows.length === 0 && <div style={emptyState}>No rows in this range.</div>}
      </div>
    </section>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return <div style={metricBox}><span>{label}</span><strong>{value}</strong></div>;
}

function landingColumns(gscPageMap: Map<string, SearchConsolePageRow>, gscAvailable: boolean): Column<SeoAnalyticsLandingPageRow>[] {
  const columns: Column<SeoAnalyticsLandingPageRow>[] = [
  { label: "Landing Page", value: (row: any) => row.landingPage },
  { label: "Views", value: (row: any) => number(row.views) },
  { label: "CTA Clicks", value: (row: any) => number(row.ctaClicks) },
  { label: "Signups", value: (row: any) => number(row.signups) },
  { label: "First Imports", value: (row: any) => number(row.firstImports) },
  { label: "First Insights", value: (row: any) => number(row.firstInsights) },
  { label: "Pro", value: (row: any) => number(row.proConversions) },
  { label: "Signup Rate", value: (row: any) => percent(row.signupRate) },
  { label: "Activation", value: (row: any) => percent(row.activationRate) },
  { label: "Paid", value: (row: any) => percent(row.paidConversionRate) },
  ];
  if (!gscAvailable) return columns;
  return [
    ...columns,
    { label: "Impressions", value: (row) => number(gscPageMap.get(row.landingPage)?.impressions ?? 0) },
    { label: "GSC Clicks", value: (row) => number(gscPageMap.get(row.landingPage)?.clicks ?? 0) },
    { label: "CTR", value: (row) => percent(gscPageMap.get(row.landingPage)?.ctr ?? 0) },
    { label: "Avg Pos", value: (row) => decimal(gscPageMap.get(row.landingPage)?.averagePosition ?? 0) },
    { label: "Signup / GSC Click", value: (row) => percent(gscPageMap.get(row.landingPage)?.signupPerGscClick ?? 0) },
    { label: "Activation / GSC Click", value: (row) => percent(gscPageMap.get(row.landingPage)?.activationPerGscClick ?? 0) },
    { label: "Pro / GSC Click", value: (row) => percent(gscPageMap.get(row.landingPage)?.proPerGscClick ?? 0) },
  ];
}

const queryColumns = [
  { label: "Query", value: (row: any) => row.query },
  { label: "Landing Page", value: (row: any) => row.landingPage },
  { label: "Intent", value: (row: any) => row.searchIntent },
  { label: "Impressions", value: (row: any) => number(row.impressions) },
  { label: "Clicks", value: (row: any) => number(row.clicks) },
  { label: "CTR", value: (row: any) => percent(row.ctr) },
  { label: "Avg Position", value: (row: any) => decimal(row.averagePosition) },
  { label: "Landing Page Signups", value: (row: any) => number(row.landingPageSignups) },
  { label: "Landing Page First Insights", value: (row: any) => number(row.landingPageFirstInsights) },
  { label: "Landing Page Pro", value: (row: any) => number(row.landingPageProConversions) },
  { label: "Activated / 100 Clicks", value: (row: any) => decimal(row.activatedPer100Clicks) },
  { label: "Pro / 100 Clicks", value: (row: any) => decimal(row.proPer100Clicks) },
];

const opportunityColumns = [
  { label: "Category", value: (row: any) => row.category },
  { label: "Query", value: (row: any) => row.query },
  { label: "Landing Page", value: (row: any) => row.landingPage },
  { label: "Impressions", value: (row: any) => number(row.impressions) },
  { label: "Clicks", value: (row: any) => number(row.clicks) },
  { label: "CTR", value: (row: any) => percent(row.ctr) },
  { label: "Avg Position", value: (row: any) => decimal(row.averagePosition) },
  { label: "First Insights", value: (row: any) => number(row.landingPageFirstInsights) },
  { label: "Activated / 100", value: (row: any) => decimal(row.activatedPer100Clicks) },
  { label: "Reason", value: (row: any) => row.reason },
];

const toolColumns = [
  { label: "Tool", value: (row: any) => toolLabel(row.tool) },
  { label: "Views", value: (row: any) => number(row.views) },
  { label: "Started", value: (row: any) => number(row.started) },
  { label: "Completed", value: (row: any) => number(row.completed) },
  { label: "CTA Clicks", value: (row: any) => number(row.ctaClicks) },
  { label: "Signups", value: (row: any) => number(row.signups) },
  { label: "First Imports", value: (row: any) => number(row.firstImports) },
  { label: "First Insights", value: (row: any) => number(row.firstInsights) },
  { label: "Pro", value: (row: any) => number(row.proConversions) },
  { label: "Complete", value: (row: any) => percent(row.completionRate) },
  { label: "Activation", value: (row: any) => percent(row.activationRate) },
];

const sourceColumns = [
  { label: "Source", value: (row: any) => row.source },
  { label: "Medium", value: (row: any) => row.medium },
  { label: "Views", value: (row: any) => number(row.views) },
  { label: "CTA Clicks", value: (row: any) => number(row.ctaClicks) },
  { label: "Signups", value: (row: any) => number(row.signups) },
  { label: "First Insights", value: (row: any) => number(row.firstInsights) },
  { label: "Pro", value: (row: any) => number(row.proConversions) },
  { label: "Signup Rate", value: (row: any) => percent(row.signupRate) },
  { label: "Activation", value: (row: any) => percent(row.activationRate) },
  { label: "Paid", value: (row: any) => percent(row.paidConversionRate) },
];

const campaignColumns = [
  { label: "Campaign", value: (row: any) => row.campaign },
  { label: "Views", value: (row: any) => number(row.views) },
  { label: "Signups", value: (row: any) => number(row.signups) },
  { label: "First Insights", value: (row: any) => number(row.firstInsights) },
  { label: "Pro", value: (row: any) => number(row.proConversions) },
  { label: "Signup Rate", value: (row: any) => percent(row.signupRate) },
  { label: "Activation", value: (row: any) => percent(row.activationRate) },
  { label: "Paid", value: (row: any) => percent(row.paidConversionRate) },
];

function number(value: number) { return new Intl.NumberFormat().format(value); }
function percent(value: number) { return `${value}%`; }
function decimal(value: number) { return new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(value); }
function minutes(value?: number | null) { return value == null ? "Not enough data" : `${number(Math.round(value))} min`; }
function formatDateTime(value: string) { return new Date(value).toLocaleString(); }
function toolLabel(value: string) {
  return ({ trading_expectancy: "Trading Expectancy", overtrading: "Overtrading", revenge_trading: "Revenge Trading" } as Record<string, string>)[value] ?? value;
}

const pageWrap: CSSProperties = { display: "grid", gap: 22 };
const filterBar: CSSProperties = { ...card, display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))", gap: 12, alignItems: "end" };
const fieldWrap: CSSProperties = { display: "grid", gap: 7, color: "#94a3b8", fontSize: 12, fontWeight: 900, textTransform: "uppercase", letterSpacing: 0 };
const inputStyle: CSSProperties = { width: "100%", border: "1px solid rgba(148,163,184,0.2)", borderRadius: 8, background: "rgba(2,6,23,0.72)", color: "#e5eefc", padding: "10px 11px", outline: "none" };
const generatedBox: CSSProperties = { minHeight: 39, display: "flex", alignItems: "center", color: "#94a3b8", fontSize: 13 };
const kpiGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(170px, 1fr))", gap: 14 };
const kpiCard: CSSProperties = { ...card, minHeight: 108, display: "grid", gap: 7, alignContent: "center", borderRadius: 8 };
const kpiLabel: CSSProperties = { color: "#94a3b8", fontSize: 12, fontWeight: 900, textTransform: "uppercase", letterSpacing: 0 };
const kpiValue: CSSProperties = { color: "#f8fafc", fontSize: 30, lineHeight: 1 };
const twoColumnGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(340px, 1fr))", gap: 18 };
const sectionCard: CSSProperties = { ...card, minWidth: 0, borderRadius: 8 };
const gscSection: CSSProperties = { minWidth: 0, display: "grid", gap: 16 };
const sectionHeader: CSSProperties = { display: "flex", gap: 16, justifyContent: "space-between", alignItems: "flex-start", marginBottom: 16 };
const subsectionTitle: CSSProperties = { ...sectionTitle, fontSize: 16 };
const mutedParagraph: CSSProperties = { margin: "6px 0 0", color: "#94a3b8", fontSize: 13, maxWidth: 720 };
const propertyText: CSSProperties = { color: "#94a3b8", fontSize: 13, marginTop: 6 };
const gscNotice: CSSProperties = { marginTop: 14, padding: 14, borderRadius: 8, color: "#fde68a", background: "rgba(245,158,11,0.12)", border: "1px solid rgba(245,158,11,0.26)" };
const innerPanel: CSSProperties = { minWidth: 0, borderRadius: 8, padding: 16, border: "1px solid rgba(148,163,184,0.14)", background: "rgba(2,6,23,0.3)" };
const statusPill = (available?: boolean): CSSProperties => ({ flexShrink: 0, borderRadius: 8, padding: "9px 12px", color: available ? "#bbf7d0" : "#fde68a", background: available ? "rgba(34,197,94,0.12)" : "rgba(245,158,11,0.12)", border: `1px solid ${available ? "rgba(34,197,94,0.26)" : "rgba(245,158,11,0.26)"}`, fontSize: 12, fontWeight: 900, textTransform: "uppercase", letterSpacing: 0 });
const funnelWrap: CSSProperties = { display: "grid", gap: 15, marginTop: 16 };
const funnelRow: CSSProperties = { display: "grid", gap: 8 };
const funnelMeta: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 12, color: "#cbd5e1" };
const progressTrack: CSSProperties = { height: 10, borderRadius: 999, background: "rgba(15,23,42,0.85)", overflow: "hidden", border: "1px solid rgba(148,163,184,0.14)" };
const progressFill: CSSProperties = { height: "100%", borderRadius: 999, background: "linear-gradient(90deg,#14b8a6,#38bdf8,#86efac)" };
const smallMeta: CSSProperties = { color: "#94a3b8", fontSize: 12 };
const chartWrap: CSSProperties = { width: "100%", height: 300, minWidth: 0, marginTop: 16 };
const chartEmpty: CSSProperties = { height: 240, display: "grid", placeItems: "center", color: "#94a3b8", borderRadius: 8, border: "1px dashed rgba(148,163,184,0.16)", background: "rgba(2,6,23,0.28)", marginTop: 16 };
const latencyGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: 12, marginTop: 16 };
const metricBox: CSSProperties = { padding: 16, borderRadius: 8, background: "rgba(2,6,23,0.52)", border: "1px solid rgba(148,163,184,0.14)", display: "grid", gap: 8, color: "#94a3b8" };
const noteList: CSSProperties = { display: "grid", gap: 10, marginTop: 16 };
const noteItem: CSSProperties = { padding: 14, borderRadius: 8, color: "#cbd5e1", background: "rgba(15,23,42,0.56)", border: "1px solid rgba(148,163,184,0.15)" };
const tableWrap: CSSProperties = { overflowX: "auto", marginTop: 16 };
const tableHeader: CSSProperties = { color: "#94a3b8", fontSize: 11, fontWeight: 950, textTransform: "uppercase", letterSpacing: 0, background: "rgba(15,23,42,0.48)", borderRadius: 8 };
const tableRow = (columns: number): CSSProperties => ({ minWidth: Math.max(860, columns * 118), display: "grid", gridTemplateColumns: `repeat(${columns}, minmax(90px, 1fr))`, gap: 12, alignItems: "center", padding: "12px 14px", borderBottom: "1px solid rgba(148,163,184,0.12)", color: "#cbd5e1" });
const emptyState: CSSProperties = { color: "#94a3b8", padding: 16 };
const errorBox: CSSProperties = { padding: 14, borderRadius: 8, color: "#fecaca", background: "rgba(239,68,68,0.12)", border: "1px solid rgba(239,68,68,0.28)" };
const adminBadge: CSSProperties = { ...button, width: "fit-content", borderRadius: 8, padding: "10px 13px", boxShadow: "none" };
