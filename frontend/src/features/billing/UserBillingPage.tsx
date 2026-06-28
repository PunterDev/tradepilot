import { useState, type CSSProperties } from "react";
import { useLocation } from "react-router-dom";
import { createCheckoutSession, createCreditPackCheckoutSession, createPortalSession, type CreditPackId } from "../../api/billingApi";
import type { AuthUser } from "../../api/authApi";
import BillingUsageCard from "../../components/BillingUsageCard";
import { creditPacks, formatCreditPackPrice } from "../../config/creditPacks";
import PageHeader from "../../layout/PageHeader";
import { button, card, mutedText, sectionTitle } from "../../styles/dashboardStyles";
import type { BillingUsage } from "../../types/billing";

type Props = {
  user: AuthUser;
  usage: BillingUsage | null;
  loading: boolean;
  error: string;
};

export default function UserBillingPage({ user, usage, loading, error }: Props) {
  const location = useLocation();
  const [checkoutLoading, setCheckoutLoading] = useState<"monthly" | "yearly" | null>(null);
  const [portalLoading, setPortalLoading] = useState(false);
  const [creditPackLoading, setCreditPackLoading] = useState<CreditPackId | null>(null);
  const [notice, setNotice] = useState("");
  const creditCheckoutResult = (location.state as { creditCheckoutResult?: string } | null)?.creditCheckoutResult;
  const isPro = usage?.plan === "PRO" || user.plan === "PRO";
  const isActivePro = isPro && (usage?.subscriptionStatus === "ACTIVE" || usage?.subscriptionStatus === "TRIALING");
  const isScheduledCancellation = isPro && usage?.subscriptionStatus === "CANCELED";

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

  async function manageBilling() {
    setPortalLoading(true);
    setNotice("");
    try {
      const { portalUrl } = await createPortalSession();
      window.location.href = portalUrl;
    } catch (err: any) {
      const message = err?.response?.data?.error || err?.response?.data?.message || "";
      setNotice(
        message.includes("not configured")
          ? "Stripe customer portal is not configured in this local environment."
          : "Unable to open billing portal. Please try again."
      );
    } finally {
      setPortalLoading(false);
    }
  }

  async function buyCreditPack(packId: CreditPackId) {
    setCreditPackLoading(packId);
    setNotice("");
    try {
      const { checkoutUrl } = await createCreditPackCheckoutSession(packId);
      window.location.href = checkoutUrl;
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.response?.data?.error || "";
      setNotice(
        message.includes("Upgrade to Pro")
          ? "Upgrade to Pro before buying AI credit packs."
          : message.includes("not configured")
            ? "Stripe checkout is not configured in this local environment."
            : "Unable to start credit pack checkout. Please try again."
      );
    } finally {
      setCreditPackLoading(null);
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
          {isPro ? (
            <div style={currentPlanPanel}>
              <div>
                <span style={currentPlanBadge}>{isActivePro ? "Current Plan" : "Pro Plan"}</span>
                <p style={{ ...mutedText, margin: "8px 0 0", fontSize: 13 }}>
                  {isScheduledCancellation
                    ? `Your Pro access remains available through ${usage?.resetDate ? formatDate(usage.resetDate) : "the end of the current billing period"}.`
                    : "Your Pro subscription is active."}
                </p>
              </div>
              <button
                type="button"
                onClick={manageBilling}
                disabled={portalLoading || loading || !usage}
                style={{ ...button, width: "auto", opacity: portalLoading ? 0.72 : 1 }}
              >
                {portalLoading ? "Opening..." : "Manage Billing"}
              </button>
            </div>
          ) : (
            <div style={ctaRow}>
              <button type="button" onClick={() => startCheckout("monthly")} disabled={!!checkoutLoading} style={{ ...button, width: "auto" }}>
                {checkoutLoading === "monthly" ? "Opening..." : "Start Monthly"}
              </button>
              <button type="button" onClick={() => startCheckout("yearly")} disabled={!!checkoutLoading} style={secondaryButton}>
                {checkoutLoading === "yearly" ? "Opening..." : "Start Yearly"}
              </button>
            </div>
          )}
          <p style={{ ...mutedText, fontSize: 12 }}>Cancel anytime. Taxes may apply. Stripe-secured checkout.</p>
          {notice && <div style={noticeBox}>{notice}</div>}
        </article>
      </section>

      {creditCheckoutResult === "success" && (
        <div style={successNotice}>
          AI credits purchased successfully. Your balance has been updated.
        </div>
      )}
      {creditCheckoutResult === "cancelled" && (
        <div style={noticeBox}>
          Credit pack checkout was cancelled. No credits were purchased.
        </div>
      )}

      <BillingUsageCard usage={usage} loading={loading} error={error} />

      {usage?.plan === "PRO" && (
        <section style={creditPackSection}>
          <div>
            <span style={eyebrow}>AI credit packs</span>
            <h2 style={{ ...sectionTitle, color: "#f8fafc", marginTop: 8 }}>Buy extra credits</h2>
            <p style={{ ...mutedText, maxWidth: 720, lineHeight: 1.6 }}>
              Purchased credits never reset and are used only after your monthly Pro credits are exhausted.
            </p>
          </div>
          <div style={packGrid}>
            {creditPacks.map((pack) => (
              <article key={pack.id} style={packCard}>
                <div>
                  <span style={packName}>{pack.name}</span>
                  <strong style={packCredits}>{pack.credits} credits</strong>
                  <p style={{ ...mutedText, marginTop: 6 }}>{pack.copy}</p>
                </div>
                <button
                  type="button"
                  onClick={() => buyCreditPack(pack.id)}
                  disabled={!!creditPackLoading}
                  style={{ ...button, width: "100%", opacity: creditPackLoading ? 0.72 : 1 }}
                >
                  {creditPackLoading === pack.id ? "Opening..." : `Buy ${pack.credits} — ${formatCreditPackPrice(pack.amountCents)}`}
                </button>
              </article>
            ))}
          </div>
        </section>
      )}
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
const successNotice: CSSProperties = { padding: 12, borderRadius: 14, color: "#bbf7d0", background: "rgba(16,185,129,0.12)", border: "1px solid rgba(52,211,153,0.32)", marginBottom: 18, fontWeight: 850 };
const currentPlanPanel: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 14, alignItems: "center", flexWrap: "wrap", padding: 14, borderRadius: 18, border: "1px solid rgba(52,211,153,0.24)", background: "rgba(2,6,23,0.45)" };
const currentPlanBadge: CSSProperties = { display: "inline-flex", padding: "7px 10px", borderRadius: 999, color: "#bbf7d0", background: "rgba(16,185,129,0.14)", border: "1px solid rgba(52,211,153,0.28)", fontSize: 12, fontWeight: 950, textTransform: "uppercase", letterSpacing: "0.08em" };
const creditPackSection: CSSProperties = { ...card, display: "grid", gap: 18, border: "1px solid rgba(52,211,153,0.24)", marginBottom: 24 };
const packGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: 14 };
const packCard: CSSProperties = { padding: 16, borderRadius: 18, border: "1px solid rgba(148,163,184,0.18)", background: "rgba(2,6,23,0.42)", display: "grid", gap: 16 };
const packName: CSSProperties = { color: "#6ee7b7", fontSize: 12, fontWeight: 950, textTransform: "uppercase", letterSpacing: "0.08em" };
const packCredits: CSSProperties = { display: "block", color: "#f8fafc", fontSize: 28, lineHeight: 1.05, marginTop: 8 };

function formatDate(value: string) {
  return new Date(`${value}T00:00:00`).toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}
