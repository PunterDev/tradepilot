import { useEffect, useState, type CSSProperties } from "react";
import { getAdminBillingSummary, type AdminBillingSummary } from "../../api/adminBillingApi";
import PageHeader from "../../layout/PageHeader";
import { card, mutedText, sectionTitle } from "../../styles/dashboardStyles";

export default function AdminBillingDashboard() {
  const [summary, setSummary] = useState<AdminBillingSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function load() {
      setLoading(true);
      setError("");
      try {
        setSummary(await getAdminBillingSummary());
      } catch {
        setError("Unable to load admin billing summary.");
      } finally {
        setLoading(false);
      }
    }

    void load();
  }, []);

  return (
    <>
      <PageHeader
        label="Admin Billing"
        title="Revenue & Subscriptions"
        description="Track plan mix, estimated run-rate revenue, manual upgrades, and Stripe subscription readiness."
      />

      {error && <div style={errorBox}>{error}</div>}

      <section style={kpiGrid}>
        <Kpi label="Total Users" value={summary?.totalUsers ?? 0} loading={loading} />
        <Kpi label="Free Users" value={summary?.freeUsers ?? 0} loading={loading} />
        <Kpi label="Pro Users" value={summary?.proUsers ?? 0} loading={loading} accent="#5eead4" />
        <Kpi label="Paying Subscribers" value={summary?.stripePayingSubscribers ?? 0} loading={loading} accent="#86efac" />
        <Kpi label="MRR" value={formatCurrency(summary?.estimatedMrr ?? 0)} loading={loading} accent="#86efac" />
        <Kpi label="ARR" value={formatCurrency(summary?.estimatedArr ?? 0)} loading={loading} accent="#93c5fd" />
      </section>

      <section style={secondaryKpiGrid}>
        <Kpi label="Manual Pro Users" value={summary?.manualProUsers ?? 0} loading={loading} accent="#fde68a" compact />
        <Kpi label="Potential MRR" value={formatCurrency(summary?.potentialMrr ?? 0)} loading={loading} accent="#c4b5fd" compact />
        <Kpi label="Potential ARR" value={formatCurrency(summary?.potentialArr ?? 0)} loading={loading} accent="#c4b5fd" compact />
        <Kpi label="Paid Conversion" value={`${summary?.paidConversionRate ?? 0}%`} loading={loading} accent="#5eead4" compact />
      </section>

      <div style={accuracyNote}>
        MRR only counts Stripe paying subscribers. Manual admin upgrades are excluded from revenue.
      </div>

      <section style={mainGrid}>
        <article style={panel}>
          <h2 style={sectionTitle}>Plan Economics</h2>
          <div style={priceGrid}>
            <Detail label="Monthly price" value="$29.99" />
            <Detail label="Yearly price" value="$249" />
            <Detail label="Yearly savings" value="$110/year" />
            <Detail label="Manual admin upgrades" value={summary?.manualUpgrades ?? 0} />
            <Detail label="Stripe subscriptions" value={summary?.stripeSubscriptions ?? 0} />
          </div>
          <div style={placeholderBox}>
            <strong>Failed payments</strong>
            <span>Stripe payment failure reporting is not connected yet.</span>
          </div>
        </article>

        <article style={panel}>
          <h2 style={sectionTitle}>Subscription Readiness</h2>
          <div style={readinessGrid}>
            <Readiness label="Stripe Checkout" configured={!!summary?.stripeCheckoutConfigured} />
            <Readiness label="Stripe Webhook" configured={!!summary?.stripeWebhookConfigured} />
            <Readiness label="Monthly Price ID" configured={!!summary?.monthlyPriceIdConfigured} />
            <Readiness label="Yearly Price ID" configured={!!summary?.yearlyPriceIdConfigured} />
          </div>
        </article>

        <article style={panel}>
          <h2 style={sectionTitle}>Recent Plan Changes</h2>
          <div style={list}>
            {loading ? (
              <div style={emptyState}>Loading plan changes...</div>
            ) : (summary?.recentPlanChanges ?? []).length === 0 ? (
              <div style={emptyState}>No recent upgrades or downgrades.</div>
            ) : (
              (summary?.recentPlanChanges ?? []).map((change) => (
                <div key={change.id} style={changeRow}>
                  <div style={changeContent}>
                    <div style={changeTitleRow}>
                      <strong>Action: {change.action === "UPGRADE_TO_PRO" ? "Upgrade to Pro" : "Downgrade to Free"}</strong>
                      <span style={sourceBadge(change.source || "ADMIN")}>{change.source || "ADMIN"}</span>
                    </div>
                    <span>Target: {change.targetUserEmail || change.targetUserName || `User #${change.targetUserId}`}</span>
                    <span>Reason: {change.reason || "No reason provided"}</span>
                    <span>Date: {formatDate(change.createdAt)}</span>
                  </div>
                </div>
              ))
            )}
          </div>
        </article>
      </section>
    </>
  );
}

function Kpi({ label, value, loading, accent = "#e5eefc", compact = false }: { label: string; value: string | number; loading: boolean; accent?: string; compact?: boolean }) {
  return (
    <article style={{ ...(compact ? compactKpiCard : kpiCard), borderColor: `${accent}33` }}>
      <span>{label}</span>
      <strong style={{ color: accent }}>{loading ? "..." : value}</strong>
    </article>
  );
}

function Readiness({ label, configured }: { label: string; configured: boolean }) {
  return (
    <div style={readinessItem}>
      <span>{label}</span>
      <strong style={configured ? readyText : missingText}>{configured ? "Configured" : "Missing"}</strong>
    </div>
  );
}

function Detail({ label, value }: { label: string; value: string | number }) {
  return (
    <div style={detailBox}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat(undefined, { style: "currency", currency: "USD" }).format(value);
}

function formatDate(value?: string | null) {
  if (!value) return "Not yet";
  return new Date(value).toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
}

const kpiGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: 14, marginBottom: 22 };
const kpiCard: CSSProperties = { ...card, minHeight: 112, display: "grid", gap: 8, alignContent: "center" };
const secondaryKpiGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: 12, marginBottom: 18 };
const compactKpiCard: CSSProperties = { ...card, minHeight: 92, padding: 18, display: "grid", gap: 6, alignContent: "center" };
const accuracyNote: CSSProperties = { padding: 14, borderRadius: 16, color: "#bbf7d0", background: "rgba(34,197,94,0.1)", border: "1px solid rgba(34,197,94,0.24)", marginBottom: 22 };
const mainGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(300px, 1fr))", gap: 18 };
const panel: CSSProperties = { ...card, minWidth: 0 };
const priceGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))", gap: 12, marginTop: 16 };
const detailBox: CSSProperties = { padding: 16, borderRadius: 18, background: "rgba(2,6,23,0.55)", border: "1px solid rgba(148,163,184,0.14)", display: "grid", gap: 7, color: "#94a3b8", minHeight: 90, alignContent: "center" };
const placeholderBox: CSSProperties = { marginTop: 16, padding: 16, borderRadius: 18, background: "rgba(245,158,11,0.1)", border: "1px solid rgba(245,158,11,0.22)", color: "#fde68a", display: "grid", gap: 5 };
const list: CSSProperties = { display: "grid", gap: 10, marginTop: 16 };
const readinessGrid: CSSProperties = { display: "grid", gap: 12, marginTop: 16 };
const readinessItem: CSSProperties = { padding: 14, borderRadius: 16, background: "rgba(2,6,23,0.5)", border: "1px solid rgba(148,163,184,0.13)", color: "#94a3b8", display: "flex", justifyContent: "space-between", gap: 12 };
const readyText: CSSProperties = { color: "#86efac" };
const missingText: CSSProperties = { color: "#fde68a" };
const changeRow: CSSProperties = { padding: 16, borderRadius: 16, background: "rgba(2,6,23,0.5)", border: "1px solid rgba(148,163,184,0.13)", color: "#cbd5e1" };
const changeContent: CSSProperties = { display: "grid", gap: 7, lineHeight: 1.35 };
const changeTitleRow: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 12, alignItems: "center", flexWrap: "wrap" };
const emptyState: CSSProperties = { color: "#94a3b8", padding: 16 };
const errorBox: CSSProperties = { padding: 14, borderRadius: 14, color: "#fecaca", background: "rgba(239,68,68,0.12)", border: "1px solid rgba(239,68,68,0.28)", marginBottom: 18 };

function sourceBadge(source: string): CSSProperties {
  const isDev = source === "DEV_MOCK";
  const isStripe = source === "STRIPE";
  return {
    width: "fit-content",
    padding: "6px 9px",
    borderRadius: 999,
    fontSize: 12,
    fontWeight: 950,
    color: isStripe ? "#bbf7d0" : isDev ? "#fde68a" : "#bae6fd",
    background: isStripe ? "rgba(34,197,94,0.14)" : isDev ? "rgba(245,158,11,0.14)" : "rgba(14,165,233,0.14)",
    border: isStripe ? "1px solid rgba(34,197,94,0.28)" : isDev ? "1px solid rgba(245,158,11,0.24)" : "1px solid rgba(56,189,248,0.26)",
  };
}
