import { useEffect, useState, type CSSProperties, type ReactNode } from "react";
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import {
  getObservabilitySummary,
  getObservabilityTrends,
  resolveObservabilityAlert,
  type ObservabilityFilter,
  type ObservabilityRange,
  type ObservabilitySummary,
  type ObservabilityTrends,
  type SystemAlert,
} from "../../../api/adminObservabilityApi";
import { card, mutedText, sectionTitle } from "../../../styles/dashboardStyles";
import PageHeader from "../../../layout/PageHeader";

export default function ObservabilityPage() {
  const [summary, setSummary] = useState<ObservabilitySummary | null>(null);
  const [trends, setTrends] = useState<ObservabilityTrends | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [resolvingId, setResolvingId] = useState<number | null>(null);
  const [filter, setFilter] = useState<ObservabilityFilter>({ range: "TODAY" });

  async function load() {
    setLoading(true);
    setError("");
    setSuccess("");
    try {
      const [summaryData, trendData] = await Promise.all([
        getObservabilitySummary(filter),
        getObservabilityTrends(filter),
      ]);
      setSummary(summaryData);
      setTrends(trendData);
    } catch {
      setError("Unable to load observability metrics.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, [filter.range, filter.startDate, filter.endDate]);

  async function handleResolveAlert(alertId: number) {
    setResolvingId(alertId);
    setError("");
    setSuccess("");
    try {
      await resolveObservabilityAlert(alertId);
      setSuccess("Alert resolved.");
      await load();
    } catch {
      setError("Unable to resolve alert.");
    } finally {
      setResolvingId(null);
    }
  }

  function updateRange(range: ObservabilityRange) {
    setFilter((current) => ({
      range,
      startDate: range === "CUSTOM" ? current.startDate : undefined,
      endDate: range === "CUSTOM" ? current.endDate : undefined,
    }));
  }

  return (
    <div>
      <PageHeader
        label="Admin Observability"
        title="Production Monitoring"
        description="API latency, AI spend, cache health, email delivery, Stripe events, and system alerts."
        actions={<button type="button" onClick={load} style={refreshButton}>Refresh</button>}
      />

      <section style={filterBar}>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          {(["TODAY", "LAST_7_DAYS", "LAST_30_DAYS", "CUSTOM"] as ObservabilityRange[]).map((range) => (
            <button
              key={range}
              type="button"
              onClick={() => updateRange(range)}
              style={{
                ...filterButton,
                ...(filter.range === range ? activeFilterButton : {}),
              }}
            >
              {rangeLabel(range)}
            </button>
          ))}
        </div>
        {filter.range === "CUSTOM" && (
          <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
            <input
              type="date"
              value={filter.startDate ?? ""}
              onChange={(event) => setFilter((current) => ({ ...current, startDate: event.target.value }))}
              style={dateInput}
            />
            <input
              type="date"
              value={filter.endDate ?? ""}
              onChange={(event) => setFilter((current) => ({ ...current, endDate: event.target.value }))}
              style={dateInput}
            />
          </div>
        )}
      </section>

      {error && <div style={errorCard}>{error}</div>}
      {success && <div style={successCard}>{success}</div>}

      <section style={grid4}>
        <Kpi label="Requests Today" value={summary?.requestsToday ?? 0} loading={loading} accent="#38bdf8" />
        <Kpi label="Avg API Response" value={latency(summary?.avgResponseTime, 1)} loading={loading} accent="#22c55e" />
        <Kpi label="P95 Response" value={latency(summary?.p95ResponseTime, 0)} loading={loading} accent="#f59e0b" />
        <Kpi label="Error Rate" value={percent(summary?.errorRate)} loading={loading} accent="#fb7185" />
        <Kpi label="AI Cost Today" value={money(summary?.aiCostToday)} loading={loading} accent="#a78bfa" />
        <Kpi label="AI Cost Month" value={money(summary?.aiCostMonth)} loading={loading} accent="#c084fc" />
        <Kpi label="Cache Hit Rate" value={percent(summary?.cacheHitRate)} loading={loading} accent="#2dd4bf" />
        <Kpi label="Email Success" value={percent(summary?.emailSuccessRate)} loading={loading} accent="#86efac" />
      </section>

      <section style={grid2}>
        <ChartCard title="API Performance" data={trends?.apiPerformance ?? []} dataKey="requests" stroke="#38bdf8" />
        <ChartCard title="AI Cost Trend" data={trends?.aiCosts ?? []} dataKey="estimatedCost" stroke="#a78bfa" />
      </section>

      <section style={grid3}>
        <InfoCard title="AI Costs">
          <Detail label="Total AI Calls Today" value={summary?.aiCallsToday ?? 0} />
          <Detail label="Tokens Today" value={summary?.aiTokensToday ?? 0} />
          <Detail label="Most Expensive Feature" value={summary?.mostExpensiveFeature ?? "No AI calls yet"} />
          <Detail label="Most Used Model" value={summary?.mostUsedModel ?? "No model usage yet"} />
        </InfoCard>
        <InfoCard title="Cache Health">
          <Detail label="Hits" value={summary?.cacheHits ?? 0} />
          <Detail label="Misses" value={summary?.cacheMisses ?? 0} />
          {(summary?.cacheHealth ?? []).slice(0, 4).map((item) => (
            <Detail key={item.cacheName} label={item.cacheName} value={percent(item.hitRate)} />
          ))}
        </InfoCard>
        <InfoCard title="Stripe Health">
          <Detail label="Successful Events" value={summary?.stripeSuccesses ?? 0} />
          <Detail label="Failures" value={summary?.stripeFailures ?? 0} />
          <Detail label="Success Rate" value={percent(summary?.stripeSuccessRate)} />
        </InfoCard>
      </section>

      <section style={grid2}>
        <AlertCard
          title="Open Alerts"
          empty="No open system alerts."
          alerts={summary?.alerts ?? []}
          resolvingId={resolvingId}
          onResolve={handleResolveAlert}
        />
        <ResolvedAlertCard alerts={summary?.resolvedAlerts ?? []} />
      </section>

      <section style={grid2}>
        <TableCard title="Slowest Endpoints" empty="No request traffic yet.">
          {(summary?.slowestEndpoints ?? []).map((row) => (
            <Row key={row.endpoint} left={row.endpoint} middle={`${formatNumber(row.avgDurationMs, 1)} ms avg`} right={`${formatNumber(row.maxDurationMs, 0)} ms max`} />
          ))}
        </TableCard>
        <TableCard title="Slow Requests" empty="No slow requests recorded.">
          {(summary?.slowRequests ?? []).map((row) => (
            <Row key={`${row.endpoint}-${row.createdAt}`} left={row.endpoint} middle={`${row.durationMs} ms`} right={row.severity} />
          ))}
        </TableCard>
      </section>

      <section style={grid2}>
        <TableCard title="AI Feature Spend" empty="No AI calls recorded.">
          {(summary?.aiFeatureCosts ?? []).map((row) => (
            <Row key={row.feature} left={row.feature} middle={`${row.tokens} tokens`} right={money(row.estimatedCost)} />
          ))}
        </TableCard>
        <TableCard title="Email Failures" empty="No email failures.">
          {(summary?.recentEmailFailures ?? []).map((row) => (
            <Row key={`${row.recipient}-${row.createdAt}`} left={row.recipient} middle={row.template} right={row.errorMessage ?? "Failed"} />
          ))}
        </TableCard>
      </section>

      <section style={grid2}>
        <TableCard title="Recent Stripe Events" empty="No Stripe events recorded.">
          {(summary?.recentStripeEvents ?? []).map((row) => (
            <Row key={`${row.eventType}-${row.createdAt}`} left={row.eventType} middle={row.success ? "Success" : "Failed"} right={`${row.durationMs} ms`} />
          ))}
        </TableCard>
      </section>
    </div>
  );
}

function Kpi({ label, value, loading, accent }: { label: string; value: string | number; loading: boolean; accent: string }) {
  return (
    <div style={kpiCard}>
      <div style={{ ...mutedText, fontSize: 12 }}>{label}</div>
      <div style={kpiValue}>{loading ? "..." : value}</div>
      <div style={{ height: 3, borderRadius: 999, background: accent, marginTop: 16, opacity: 0.85 }} />
    </div>
  );
}

function ChartCard({ title, data, dataKey, stroke }: { title: string; data: Record<string, unknown>[]; dataKey: string; stroke: string }) {
  return (
    <div style={card}>
      <h2 style={sectionTitle}>{title}</h2>
      {data.length === 0 ? (
        <p style={mutedText}>No trend data yet.</p>
      ) : (
        <div style={{ width: "100%", height: 260, minWidth: 0 }}>
          <ResponsiveContainer width="100%" height={260}>
            <AreaChart data={data}>
              <defs>
                <linearGradient id={`${dataKey}-gradient`} x1="0" x2="0" y1="0" y2="1">
                  <stop offset="0%" stopColor={stroke} stopOpacity={0.5} />
                  <stop offset="100%" stopColor={stroke} stopOpacity={0.04} />
                </linearGradient>
              </defs>
              <CartesianGrid stroke="rgba(148,163,184,0.12)" vertical={false} />
              <XAxis dataKey="date" stroke="#94a3b8" tick={{ fontSize: 11 }} />
              <YAxis stroke="#94a3b8" tick={{ fontSize: 11 }} />
              <Tooltip contentStyle={tooltipStyle} />
              <Area type="monotone" dataKey={dataKey} stroke={stroke} fill={`url(#${dataKey}-gradient)`} strokeWidth={2} />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  );
}

function InfoCard({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div style={card}>
      <h2 style={sectionTitle}>{title}</h2>
      <div style={{ display: "grid", gap: 10 }}>{children}</div>
    </div>
  );
}

function Detail({ label, value }: { label: string; value: string | number }) {
  return (
    <div style={detailRow}>
      <span style={mutedText}>{label}</span>
      <strong style={detailValue}>{value}</strong>
    </div>
  );
}

function TableCard({ title, empty, children }: { title: string; empty: string; children: ReactNode[] }) {
  return (
    <div style={card}>
      <h2 style={sectionTitle}>{title}</h2>
      <div style={{ display: "grid", gap: 8 }}>
        {children.length ? children : <p style={mutedText}>{empty}</p>}
      </div>
    </div>
  );
}

function Row({ left, middle, right }: { left: string; middle: string; right: string }) {
  return (
    <div style={rowStyle}>
      <span style={{ color: "#f8fafc", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{left}</span>
      <span style={mutedText}>{middle}</span>
      <span style={{ ...mutedText, textAlign: "right", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{right}</span>
    </div>
  );
}

function AlertCard({
  title,
  empty,
  alerts,
  resolvingId,
  onResolve,
}: {
  title: string;
  empty: string;
  alerts: SystemAlert[];
  resolvingId: number | null;
  onResolve: (id: number) => void;
}) {
  return (
    <div style={card}>
      <h2 style={sectionTitle}>{title}</h2>
      <div style={{ display: "grid", gap: 10 }}>
        {alerts.length ? alerts.map((alert) => (
          <div key={alert.id} style={alertRow}>
            <div style={{ minWidth: 0 }}>
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
                <span style={severityBadge(alert.severity)}>{alert.severity}</span>
                <strong style={{ color: "#f8fafc" }}>{alert.type}</strong>
              </div>
              <p style={{ ...mutedText, margin: "8px 0 0" }}>{alert.message}</p>
              <small style={mutedText}>{formatDate(alert.createdAt)}</small>
            </div>
            <button
              type="button"
              onClick={() => onResolve(alert.id)}
              disabled={resolvingId === alert.id}
              style={{ ...resolveButton, opacity: resolvingId === alert.id ? 0.65 : 1 }}
            >
              {resolvingId === alert.id ? "Resolving..." : "Resolve"}
            </button>
          </div>
        )) : <p style={mutedText}>{empty}</p>}
      </div>
    </div>
  );
}

function ResolvedAlertCard({ alerts }: { alerts: SystemAlert[] }) {
  return (
    <div style={card}>
      <h2 style={sectionTitle}>Resolved Alerts</h2>
      <div style={{ display: "grid", gap: 10 }}>
        {alerts.length ? alerts.map((alert) => (
          <div key={alert.id} style={alertRow}>
            <div style={{ minWidth: 0 }}>
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
                <span style={resolvedBadge}>RESOLVED</span>
                <strong style={{ color: "#f8fafc" }}>{alert.type}</strong>
              </div>
              <p style={{ ...mutedText, margin: "8px 0 0" }}>{alert.message}</p>
              <small style={mutedText}>
                Resolved by {alert.resolvedBy ?? "unknown"} at {formatDate(alert.resolvedAt)}
              </small>
            </div>
          </div>
        )) : <p style={mutedText}>No resolved alerts in this range.</p>}
      </div>
    </div>
  );
}

function rangeLabel(range: ObservabilityRange) {
  if (range === "LAST_7_DAYS") return "Last 7 Days";
  if (range === "LAST_30_DAYS") return "Last 30 Days";
  if (range === "CUSTOM") return "Custom";
  return "Today";
}

function formatDate(value?: string | null) {
  if (!value) return "unknown";
  return new Date(value).toLocaleString();
}

function money(value?: number | null) {
  return `$${Number(value ?? 0).toFixed(2)}`;
}

function latency(value?: number | null, fractionDigits = 0) {
  return `${formatNumber(value, fractionDigits)} ms`;
}

function percent(value?: number | null) {
  return `${formatNumber(value, 1)}%`;
}

function formatNumber(value?: number | null, maximumFractionDigits = 1) {
  return Number(value ?? 0).toLocaleString(undefined, {
    minimumFractionDigits: 0,
    maximumFractionDigits,
  });
}

const grid4: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(190px, 1fr))",
  gap: 16,
  marginBottom: 18,
};

const kpiCard: CSSProperties = {
  ...card,
  minHeight: 118,
  minWidth: 0,
  borderColor: "rgba(56,189,248,0.15)",
};

const kpiValue: CSSProperties = {
  color: "#f8fafc",
  fontSize: "clamp(22px, 2.2vw, 28px)",
  fontWeight: 800,
  lineHeight: 1.08,
  marginTop: 12,
  maxWidth: "100%",
  minWidth: 0,
  overflowWrap: "anywhere",
  wordBreak: "break-word",
};

const grid3: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))",
  gap: 18,
  marginBottom: 18,
};

const grid2: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))",
  gap: 18,
  marginBottom: 18,
};

const refreshButton: CSSProperties = {
  border: "1px solid rgba(45,212,191,0.4)",
  background: "linear-gradient(135deg, rgba(20,184,166,0.9), rgba(34,197,94,0.78))",
  color: "#022c22",
  borderRadius: 12,
  padding: "10px 14px",
  fontWeight: 800,
  cursor: "pointer",
};

const filterBar: CSSProperties = {
  ...card,
  marginBottom: 18,
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  gap: 14,
  flexWrap: "wrap",
};

const filterButton: CSSProperties = {
  border: "1px solid rgba(148,163,184,0.18)",
  background: "rgba(2,6,23,0.45)",
  color: "#cbd5e1",
  borderRadius: 999,
  padding: "9px 13px",
  fontWeight: 800,
  cursor: "pointer",
};

const activeFilterButton: CSSProperties = {
  borderColor: "rgba(45,212,191,0.55)",
  background: "rgba(20,184,166,0.18)",
  color: "#ccfbf1",
};

const dateInput: CSSProperties = {
  border: "1px solid rgba(148,163,184,0.2)",
  background: "rgba(2,6,23,0.6)",
  color: "#e5eefc",
  borderRadius: 12,
  padding: "9px 11px",
};

const errorCard: CSSProperties = {
  ...card,
  borderColor: "rgba(248,113,113,0.35)",
  color: "#fecaca",
  marginBottom: 18,
};

const successCard: CSSProperties = {
  ...card,
  borderColor: "rgba(45,212,191,0.35)",
  color: "#ccfbf1",
  marginBottom: 18,
};

const alertRow: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "minmax(0, 1fr) auto",
  gap: 14,
  alignItems: "center",
  padding: "13px 14px",
  borderRadius: 16,
  background: "rgba(2,6,23,0.42)",
  border: "1px solid rgba(148,163,184,0.12)",
  minWidth: 0,
};

const resolveButton: CSSProperties = {
  border: "1px solid rgba(45,212,191,0.4)",
  background: "rgba(20,184,166,0.16)",
  color: "#99f6e4",
  borderRadius: 12,
  padding: "9px 12px",
  fontWeight: 800,
  cursor: "pointer",
};

const resolvedBadge: CSSProperties = {
  borderRadius: 999,
  padding: "4px 8px",
  background: "rgba(34,197,94,0.14)",
  border: "1px solid rgba(34,197,94,0.35)",
  color: "#bbf7d0",
  fontSize: 11,
  fontWeight: 900,
};

function severityBadge(severity: string): CSSProperties {
  const critical = severity === "CRITICAL";
  return {
    borderRadius: 999,
    padding: "4px 8px",
    background: critical ? "rgba(239,68,68,0.16)" : "rgba(245,158,11,0.14)",
    border: critical ? "1px solid rgba(239,68,68,0.38)" : "1px solid rgba(245,158,11,0.35)",
    color: critical ? "#fecaca" : "#fde68a",
    fontSize: 11,
    fontWeight: 900,
  };
}

const detailRow: CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  gap: 14,
  padding: "10px 0",
  borderBottom: "1px solid rgba(148,163,184,0.1)",
  minWidth: 0,
};

const detailValue: CSSProperties = {
  color: "#e5eefc",
  textAlign: "right",
  maxWidth: "55%",
  overflowWrap: "anywhere",
  wordBreak: "break-word",
};

const rowStyle: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "minmax(160px, 1.5fr) minmax(100px, 1fr) minmax(110px, 1fr)",
  gap: 12,
  alignItems: "center",
  padding: "11px 12px",
  borderRadius: 12,
  background: "rgba(2,6,23,0.35)",
  border: "1px solid rgba(148,163,184,0.1)",
  minWidth: 0,
};

const tooltipStyle: CSSProperties = {
  background: "rgba(2,6,23,0.96)",
  border: "1px solid rgba(56,189,248,0.24)",
  borderRadius: 12,
  color: "#e5eefc",
};
