import { useState, type CSSProperties } from "react";
import { createCheckoutSession, createPortalSession } from "../api/billingApi";
import type { BillingUsage } from "../types/billing";
import { button, card, mutedText, sectionTitle } from "../styles/dashboardStyles";

type Props = {
  usage: BillingUsage | null;
  loading: boolean;
  error?: string;
};

const usageRows = [
  ["AI_TRADE_REVIEW", "AI Reviews"],
  ["DAILY_COACH", "Daily Coach"],
  ["WEEKLY_COACH", "Weekly Coach"],
  ["TRADING_DNA", "Trading DNA"],
  ["TRADING_ACCOUNTS", "Trading Accounts"],
  ["IMPORTED_TRADES", "Imported Trades"],
] as const;

const visibleRows = usageRows.slice(0, 4);

export default function BillingUsageCard({ usage, loading, error }: Props) {
  const [billingActionLoading, setBillingActionLoading] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [checkoutNotice, setCheckoutNotice] = useState<null | {
    title: string;
    message: string;
    eyebrow: string;
  }>(null);
  const isFree = usage?.plan !== "PRO";
  const anyLimitReached = usage
    ? visibleRows.some(([key]) => usage.remaining[key] === 0)
    : false;

  const handleBillingAction = async () => {
    setBillingActionLoading(true);

    try {
      if (usage?.plan === "PRO") {
        const { portalUrl } = await createPortalSession();
        window.location.href = portalUrl;
      } else {
        const { checkoutUrl } = await createCheckoutSession();
        window.location.href = checkoutUrl;
      }
    } catch (err: any) {
      const message =
        err?.response?.data?.error ||
        err?.response?.data?.message ||
        "";
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
      setBillingActionLoading(false);
    }
  };

  return (
    <section
      style={{
        ...card,
        marginBottom: 24,
        border: anyLimitReached
          ? "1px solid rgba(250,204,21,0.36)"
          : "1px solid rgba(52,211,153,0.22)",
      }}
    >
      <style>
        {`
          @keyframes tradepilot-subtle-pulse {
            0%, 100% { box-shadow: 0 14px 34px rgba(34,197,94,0.24); transform: translateY(0); }
            50% { box-shadow: 0 18px 46px rgba(34,197,94,0.42); transform: translateY(-1px); }
          }
          .billing-tooltip:hover .billing-tooltip-bubble,
          .billing-tooltip:focus .billing-tooltip-bubble {
            opacity: 1 !important;
            transform: translateY(0) !important;
          }
        `}
      </style>

      <div style={headerRow}>
        <button type="button" onClick={() => setDrawerOpen(true)} style={guardButton}>
          <span style={eyebrow}>Billing guard</span>
          <span style={planTitle}>
            {loading ? "Loading plan..." : `${usage?.plan === "PRO" ? "Pro" : "Free"} Plan`}
          </span>
        </button>

        <div style={headerActions}>
          {usage?.resetDate && (
            <div style={resetPill}>Usage resets on: {formatDate(usage.resetDate)}</div>
          )}
          <button
            type="button"
            style={{
              ...button,
              width: 178,
              opacity: loading || billingActionLoading ? 0.72 : 1,
              animation: anyLimitReached && isFree ? "tradepilot-subtle-pulse 2.4s ease-in-out infinite" : undefined,
              border: anyLimitReached && isFree ? "1px solid rgba(187,247,208,0.55)" : button.border,
            }}
            disabled={loading || billingActionLoading || !usage}
            onClick={handleBillingAction}
          >
            {billingActionLoading ? "Opening..." : isFree ? "Upgrade to Pro" : "Manage Billing"}
          </button>
        </div>
      </div>

      <p style={{ ...mutedText, maxWidth: 780, lineHeight: 1.6, marginTop: 12 }}>
        Monthly AI credits are tracked below.
        <span className="billing-tooltip" style={tooltipWrap} tabIndex={0}>
          Usage rules
          <span className="billing-tooltip-bubble" style={tooltipBubble}>
            Cached results do not count against usage. Only fresh AI generations consume credits.
          </span>
        </span>
      </p>

      {error && <p style={{ color: "#fecaca", marginTop: 8 }}>{error}</p>}
      {isFree && (
        <p style={{ ...mutedText, marginTop: 8, fontSize: 12 }}>
          Local dev: Stripe keys are required to test checkout.
        </p>
      )}

      {usage && (
        <div style={progressGrid}>
          {visibleRows.map(([key, label]) => (
            <UsageProgress
              key={key}
              label={label}
              used={usage.usageThisMonth[key] ?? 0}
              remaining={usage.remaining[key] ?? 0}
              limit={usage.limits[key] ?? 0}
            />
          ))}
        </div>
      )}

      <UsageDetailsDrawer
        open={drawerOpen}
        usage={usage}
        loading={loading}
        onClose={() => setDrawerOpen(false)}
      />
      <BillingNotConfiguredDialog
        notice={checkoutNotice}
        onClose={() => setCheckoutNotice(null)}
      />
    </section>
  );
}

function UsageProgress({
  label,
  used,
  remaining,
  limit,
}: {
  label: string;
  used: number;
  remaining: number;
  limit: number;
}) {
  const unlimited = limit === -1;
  const reached = !unlimited && remaining === 0;
  const percent = unlimited || limit <= 0 ? 0 : Math.min(100, Math.round((used / limit) * 100));

  return (
    <div style={progressCard(reached)}>
      <div style={progressTopRow}>
        <span style={{ color: "#e2e8f0", fontSize: 13, fontWeight: 900 }}>{label}</span>
        <span style={{ color: reached ? "#fde68a" : "#94a3b8", fontSize: 12, fontWeight: 800 }}>
          {unlimited ? "Unlimited" : `${used}/${limit} used`}
        </span>
      </div>
      <div style={barTrack}>
        <div style={{ ...barFill, width: `${percent}%`, background: reached ? "#f59e0b" : "#34d399" }} />
      </div>
      <div style={{ color: reached ? "#fde68a" : "#64748b", fontSize: 12, marginTop: 7 }}>
        {unlimited ? "No monthly cap" : `${remaining} remaining`}
      </div>
    </div>
  );
}

function UsageDetailsDrawer({
  open,
  usage,
  loading,
  onClose,
}: {
  open: boolean;
  usage: BillingUsage | null;
  loading: boolean;
  onClose: () => void;
}) {
  if (!open) return null;

  return (
    <div style={drawerOverlay}>
      <aside style={drawer}>
        <div style={drawerHeader}>
          <div>
            <div style={eyebrow}>Usage details</div>
            <h3 style={{ ...sectionTitle, marginTop: 8 }}>
              {loading ? "Loading..." : `${usage?.plan === "PRO" ? "Pro" : "Free"} Plan`}
            </h3>
          </div>
          <button type="button" onClick={onClose} style={closeButton} aria-label="Close usage details">
            ×
          </button>
        </div>

        {usage?.resetDate && <div style={drawerReset}>Reset Date: {formatDate(usage.resetDate)}</div>}

        <div style={{ display: "grid", gap: 12, marginTop: 18 }}>
          {usageRows.map(([key, label]) => (
            <DrawerUsageRow
              key={key}
              label={label}
              limit={usage?.limits[key] ?? 0}
              used={usage?.usageThisMonth[key] ?? 0}
              remaining={usage?.remaining[key] ?? 0}
            />
          ))}
        </div>
      </aside>
    </div>
  );
}

function DrawerUsageRow({
  label,
  limit,
  used,
  remaining,
}: {
  label: string;
  limit: number;
  used: number;
  remaining: number;
}) {
  const unlimited = limit === -1;
  return (
    <div style={drawerRow}>
      <div style={{ color: "#e2e8f0", fontWeight: 900 }}>{label}</div>
      <div style={drawerMeta}>
        <span>Limit: {unlimited ? "Unlimited" : limit}</span>
        <span>Used: {used}</span>
        <span>Remaining: {unlimited ? "Unlimited" : remaining}</span>
      </div>
    </div>
  );
}

function BillingNotConfiguredDialog({
  notice,
  onClose,
}: {
  notice: null | { title: string; message: string; eyebrow: string };
  onClose: () => void;
}) {
  if (!notice) return null;

  return (
    <div style={modalOverlay}>
      <section style={configModal} role="dialog" aria-modal="true" aria-labelledby="billing-notice-title">
        <div style={eyebrow}>{notice.eyebrow}</div>
        <h3 id="billing-notice-title" style={{ ...planTitle, fontSize: 28 }}>
          {notice.title}
        </h3>
        <p style={{ ...mutedText, lineHeight: 1.65, marginTop: 10 }}>
          {notice.message}
        </p>
        <div style={{ display: "flex", justifyContent: "flex-end", marginTop: 22 }}>
          <button type="button" onClick={onClose} style={{ ...button, width: 120 }}>
            Got it
          </button>
        </div>
      </section>
    </div>
  );
}

function formatDate(value: string) {
  return new Date(`${value}T00:00:00`).toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

const headerRow: CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  gap: 18,
  alignItems: "flex-start",
  flexWrap: "wrap",
};

const headerActions: CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 12,
  flexWrap: "wrap",
};

const guardButton: CSSProperties = {
  border: "none",
  padding: 0,
  background: "transparent",
  textAlign: "left",
  cursor: "pointer",
};

const eyebrow: CSSProperties = {
  color: "#6ee7b7",
  fontSize: 12,
  fontWeight: 900,
  textTransform: "uppercase",
  letterSpacing: "0.08em",
};

const planTitle: CSSProperties = {
  ...sectionTitle,
  display: "block",
  marginTop: 8,
  color: "#f8fafc",
  textShadow: "0 1px 18px rgba(15,23,42,0.5)",
};

const resetPill: CSSProperties = {
  padding: "10px 12px",
  borderRadius: 999,
  border: "1px solid rgba(148,163,184,0.2)",
  background: "rgba(15,23,42,0.72)",
  color: "#cbd5e1",
  fontSize: 13,
  fontWeight: 800,
};

const tooltipWrap: CSSProperties = {
  position: "relative",
  display: "inline-flex",
  marginLeft: 10,
  padding: "3px 8px",
  borderRadius: 999,
  border: "1px solid rgba(148,163,184,0.22)",
  color: "#bae6fd",
  cursor: "help",
};

const tooltipBubble: CSSProperties = {
  position: "absolute",
  left: 0,
  bottom: "calc(100% + 10px)",
  width: 280,
  padding: 12,
  borderRadius: 12,
  border: "1px solid rgba(148,163,184,0.22)",
  background: "#020617",
  color: "#e2e8f0",
  boxShadow: "0 18px 50px rgba(0,0,0,0.45)",
  opacity: 0,
  pointerEvents: "none",
  transform: "translateY(4px)",
};

const progressGrid: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
  gap: 14,
  marginTop: 18,
};

function progressCard(reached: boolean): CSSProperties {
  return {
    padding: 14,
    borderRadius: 16,
    border: reached
      ? "1px solid rgba(250,204,21,0.36)"
      : "1px solid rgba(148,163,184,0.18)",
    background: "rgba(2,6,23,0.45)",
  };
}

const progressTopRow: CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  gap: 10,
  alignItems: "center",
};

const barTrack: CSSProperties = {
  height: 10,
  borderRadius: 999,
  background: "rgba(51,65,85,0.72)",
  overflow: "hidden",
  marginTop: 10,
};

const barFill: CSSProperties = {
  height: "100%",
  borderRadius: 999,
  transition: "width 180ms ease",
};

const drawerOverlay: CSSProperties = {
  position: "fixed",
  inset: 0,
  zIndex: 70,
  display: "flex",
  justifyContent: "flex-end",
  background: "rgba(2,6,23,0.58)",
  backdropFilter: "blur(8px)",
};

const drawer: CSSProperties = {
  width: "min(460px, 100%)",
  height: "100%",
  padding: 24,
  overflowY: "auto",
  borderLeft: "1px solid rgba(148,163,184,0.22)",
  background: "linear-gradient(180deg, rgba(15,23,42,0.99), rgba(2,6,23,0.98))",
  boxShadow: "-24px 0 80px rgba(0,0,0,0.42)",
};

const drawerHeader: CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  gap: 16,
  alignItems: "flex-start",
};

const closeButton: CSSProperties = {
  width: 38,
  height: 38,
  borderRadius: 10,
  border: "1px solid rgba(148,163,184,0.24)",
  background: "rgba(15,23,42,0.82)",
  color: "#cbd5e1",
  cursor: "pointer",
  fontSize: 24,
};

const drawerReset: CSSProperties = {
  marginTop: 18,
  padding: 12,
  borderRadius: 14,
  border: "1px solid rgba(52,211,153,0.22)",
  background: "rgba(16,185,129,0.1)",
  color: "#bbf7d0",
  fontWeight: 900,
};

const drawerRow: CSSProperties = {
  padding: 14,
  borderRadius: 14,
  border: "1px solid rgba(148,163,184,0.16)",
  background: "rgba(15,23,42,0.72)",
};

const drawerMeta: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(110px, 1fr))",
  gap: 8,
  marginTop: 10,
  color: "#94a3b8",
  fontSize: 12,
  fontWeight: 800,
};

const modalOverlay: CSSProperties = {
  position: "fixed",
  inset: 0,
  zIndex: 90,
  display: "grid",
  placeItems: "center",
  padding: 20,
  background: "rgba(2,6,23,0.68)",
  backdropFilter: "blur(10px)",
};

const configModal: CSSProperties = {
  width: "min(520px, 100%)",
  padding: 28,
  borderRadius: 22,
  border: "1px solid rgba(148,163,184,0.24)",
  background: "linear-gradient(180deg, rgba(15,23,42,0.99), rgba(2,6,23,0.96))",
  boxShadow: "0 28px 90px rgba(0,0,0,0.5)",
};
