import { useState, type CSSProperties } from "react";
import { createCheckoutSession } from "../api/billingApi";
import { button, mutedText, sectionTitle } from "../styles/dashboardStyles";

type Props = {
  open: boolean;
  onClose: () => void;
};

const proBenefits = [
  "More AI Reviews",
  "More Daily Coach generations",
  "More Weekly Coach generations",
  "More Trading DNA refreshes",
  "Unlimited accounts",
  "Unlimited imports",
];

export default function UpgradeModal({ open, onClose }: Props) {
  const [loading, setLoading] = useState(false);
  const [checkoutNotice, setCheckoutNotice] = useState<null | {
    title: string;
    message: string;
    eyebrow: string;
  }>(null);

  if (!open) return null;

  async function handleUpgrade() {
    setLoading(true);
    setCheckoutNotice(null);
    try {
      const { checkoutUrl } = await createCheckoutSession();
      window.location.href = checkoutUrl;
    } catch (err: any) {
      const message = err?.response?.data?.error || err?.response?.data?.message || "";
      if (message === "Stripe is not configured." || message.includes("not configured")) {
        setCheckoutNotice({
          eyebrow: "Local checkout",
          title: "Billing not configured",
          message: "Stripe checkout is not configured in this local environment. Add Stripe test keys to enable checkout.",
        });
      } else {
        setCheckoutNotice({
          eyebrow: "Checkout",
          title: "Unable to start checkout",
          message: "Unable to start checkout. Please try again.",
        });
      }
    } finally {
      setLoading(false);
    }
  }

  if (checkoutNotice) {
    return (
      <div style={overlay} role="dialog" aria-modal="true" aria-labelledby="billing-notice-title">
        <section style={modal}>
          <button type="button" onClick={onClose} style={closeButton} aria-label="Close billing notice">
            ×
          </button>
          <div style={badge}>{checkoutNotice.eyebrow}</div>
          <h2 id="billing-notice-title" style={{ ...sectionTitle, color: "#f8fafc", fontSize: 32, marginTop: 14 }}>
            {checkoutNotice.title}
          </h2>
          <p style={{ ...mutedText, lineHeight: 1.65, marginTop: 8 }}>
            {checkoutNotice.message}
          </p>
          <div style={{ display: "flex", justifyContent: "flex-end", marginTop: 24 }}>
            <button type="button" onClick={onClose} style={{ ...button, width: 120 }}>
              Got it
            </button>
          </div>
        </section>
      </div>
    );
  }

  return (
    <div style={overlay} role="dialog" aria-modal="true" aria-labelledby="upgrade-title">
      <section style={modal}>
        <button type="button" onClick={onClose} style={closeButton} aria-label="Close upgrade modal">
          ×
        </button>
        <div style={badge}>TradePilot Pro</div>
        <h2 id="upgrade-title" style={{ ...sectionTitle, color: "#f8fafc", fontSize: 32, marginTop: 14 }}>
          Upgrade to TradePilot Pro
        </h2>
        <p style={{ ...mutedText, lineHeight: 1.65, marginTop: 8 }}>
          Keep your AI workflow moving with higher monthly generation limits and room to scale every account.
        </p>

        <div style={benefitGrid}>
          {proBenefits.map((benefit) => (
            <div key={benefit} style={benefitItem}>
              <span style={check}>✓</span>
              {benefit}
            </div>
          ))}
        </div>
        <div style={{ display: "flex", gap: 12, justifyContent: "flex-end", marginTop: 24, flexWrap: "wrap" }}>
          <button type="button" onClick={onClose} style={secondaryButton}>
            Not now
          </button>
          <button type="button" onClick={handleUpgrade} disabled={loading} style={{ ...button, width: 190, opacity: loading ? 0.72 : 1 }}>
            {loading ? "Opening..." : "Upgrade to Pro"}
          </button>
        </div>
      </section>
    </div>
  );
}

const overlay: CSSProperties = {
  position: "fixed",
  inset: 0,
  zIndex: 80,
  display: "grid",
  placeItems: "center",
  padding: 20,
  background: "rgba(2,6,23,0.78)",
  backdropFilter: "blur(12px)",
};

const modal: CSSProperties = {
  position: "relative",
  width: "min(620px, 100%)",
  padding: 30,
  borderRadius: 24,
  border: "1px solid rgba(52,211,153,0.28)",
  background: "linear-gradient(180deg, rgba(15,23,42,0.99), rgba(2,6,23,0.96))",
  boxShadow: "0 30px 100px rgba(0,0,0,0.55)",
};

const closeButton: CSSProperties = {
  position: "absolute",
  top: 16,
  right: 16,
  width: 38,
  height: 38,
  borderRadius: 10,
  border: "1px solid rgba(148,163,184,0.22)",
  background: "rgba(15,23,42,0.82)",
  color: "#cbd5e1",
  cursor: "pointer",
  fontSize: 24,
};

const badge: CSSProperties = {
  display: "inline-flex",
  padding: "7px 11px",
  borderRadius: 999,
  border: "1px solid rgba(52,211,153,0.35)",
  color: "#6ee7b7",
  background: "rgba(16,185,129,0.12)",
  fontSize: 12,
  fontWeight: 900,
  textTransform: "uppercase",
  letterSpacing: "0.08em",
};

const benefitGrid: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
  gap: 12,
  marginTop: 22,
};

const benefitItem: CSSProperties = {
  display: "flex",
  gap: 10,
  alignItems: "center",
  padding: "12px 14px",
  borderRadius: 14,
  border: "1px solid rgba(148,163,184,0.16)",
  background: "rgba(15,23,42,0.72)",
  color: "#e2e8f0",
  fontWeight: 800,
};

const check: CSSProperties = {
  color: "#34d399",
  fontWeight: 950,
};

const secondaryButton: CSSProperties = {
  width: 120,
  padding: "13px 18px",
  borderRadius: 14,
  border: "1px solid rgba(148,163,184,0.28)",
  background: "rgba(15,23,42,0.9)",
  color: "#e5e7eb",
  fontWeight: 800,
  cursor: "pointer",
};
