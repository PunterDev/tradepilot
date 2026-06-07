import { useState, type CSSProperties } from "react";
import { createCheckoutSession } from "../../api/billingApi";
import BillingUsageCard from "../../components/BillingUsageCard";
import PageHeader from "../../layout/PageHeader";
import { button, card, mutedText, sectionTitle } from "../../styles/dashboardStyles";
import type { BillingUsage } from "../../types/billing";

type Props = {
  usage: BillingUsage | null;
  loading: boolean;
  error: string;
};

export default function UserBillingPage({ usage, loading, error }: Props) {
  const [checkoutLoading, setCheckoutLoading] = useState<"monthly" | "yearly" | null>(null);
  const [notice, setNotice] = useState("");

  async function startCheckout(plan: "monthly" | "yearly") {
    setCheckoutLoading(plan);
    setNotice("");
    try {
      const { checkoutUrl } = await createCheckoutSession(plan);
      window.location.href = checkoutUrl;
    } catch (err: any) {
      const message = err?.response?.data?.error || err?.response?.data?.message || "";
      setNotice(
        message.includes("not configured")
          ? "Stripe checkout is not configured in this local environment."
          : "Unable to start checkout. Please try again."
      );
    } finally {
      setCheckoutLoading(null);
    }
  }

  return (
    <>
      <PageHeader
        label="Billing"
        title="Plan & Usage"
        description="Review plan limits, remaining AI generations, reset dates, and upgrade controls."
      />

      <section style={pricingGrid}>
        <article style={freeCard}>
          <span style={eyebrow}>Free</span>
          <h2 style={priceTitle}>$0</h2>
          <p style={mutedText}>For getting your journal out of spreadsheets.</p>
          <FeatureList items={["CSV import", "Limited AI reviews", "PnL calendar", "Basic analytics"]} />
        </article>

        <article style={proCard}>
          <div style={cardTop}>
            <span style={eyebrow}>Pro</span>
            <span style={popularBadge}>Most Popular</span>
          </div>
          <div style={priceRow}>
            <div>
              <h2 style={priceTitle}>$29.99/mo</h2>
              <p style={mutedText}>For traders who want feedback loops, not just records.</p>
            </div>
            <div style={yearlyBox}>
              <strong>$249/year</strong>
              <span>Save $110/year</span>
            </div>
          </div>
          <FeatureList items={["More AI reviews", "Daily and weekly coach", "Trading DNA", "Session review", "Advanced analytics", "Priority feature access"]} />
          <div style={ctaRow}>
            <button type="button" onClick={() => startCheckout("monthly")} disabled={!!checkoutLoading} style={{ ...button, width: "auto" }}>
              {checkoutLoading === "monthly" ? "Opening..." : "Start Monthly"}
            </button>
            <button type="button" onClick={() => startCheckout("yearly")} disabled={!!checkoutLoading} style={secondaryButton}>
              {checkoutLoading === "yearly" ? "Opening..." : "Start Yearly"}
            </button>
          </div>
          <p style={{ ...mutedText, fontSize: 12 }}>Cancel anytime. Taxes may apply. Stripe-secured checkout.</p>
          {notice && <div style={noticeBox}>{notice}</div>}
        </article>
      </section>

      <BillingUsageCard usage={usage} loading={loading} error={error} />
    </>
  );
}

function FeatureList({ items }: { items: string[] }) {
  return (
    <ul style={featureList}>
      {items.map((item) => (
        <li key={item}>{item}</li>
      ))}
    </ul>
  );
}

const pricingGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: 18, marginBottom: 24 };
const freeCard: CSSProperties = { ...card, display: "grid", gap: 12 };
const proCard: CSSProperties = { ...card, display: "grid", gap: 14, border: "1px solid rgba(52,211,153,0.36)", boxShadow: "0 28px 90px rgba(20,184,166,0.18)" };
const eyebrow: CSSProperties = { color: "#34d399", fontSize: 12, fontWeight: 950, textTransform: "uppercase", letterSpacing: "0.1em" };
const priceTitle: CSSProperties = { ...sectionTitle, color: "#f8fafc", fontSize: 34, margin: 0 };
const cardTop: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 12, alignItems: "center" };
const popularBadge: CSSProperties = { padding: "7px 10px", borderRadius: 999, color: "#022c22", background: "linear-gradient(135deg,#34d399,#22d3ee)", fontSize: 12, fontWeight: 950 };
const priceRow: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: 16, alignItems: "center" };
const yearlyBox: CSSProperties = { padding: 14, borderRadius: 18, background: "rgba(2,6,23,0.55)", border: "1px solid rgba(52,211,153,0.24)", display: "grid", gap: 4, color: "#bbf7d0", minWidth: 150 };
const featureList: CSSProperties = { margin: 0, paddingLeft: 18, color: "#cbd5e1", lineHeight: 1.8 };
const ctaRow: CSSProperties = { display: "flex", gap: 10, flexWrap: "wrap" };
const secondaryButton: CSSProperties = { ...button, width: "auto", background: "rgba(15,23,42,0.85)", border: "1px solid rgba(148,163,184,0.2)", boxShadow: "none" };
const noticeBox: CSSProperties = { padding: 12, borderRadius: 14, color: "#fde68a", background: "rgba(245,158,11,0.12)", border: "1px solid rgba(245,158,11,0.25)" };
