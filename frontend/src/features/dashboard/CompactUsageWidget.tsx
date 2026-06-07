import { Link } from "react-router-dom";
import type { CSSProperties } from "react";
import type { BillingUsage } from "../../types/billing";
import { card, mutedText, sectionTitle } from "../../styles/dashboardStyles";

type Props = {
  usage: BillingUsage | null;
  loading: boolean;
  error?: string;
};

export default function CompactUsageWidget({ usage, loading, error }: Props) {
  const aiReviews = usagePair(usage, "AI_TRADE_REVIEW");
  const dailyCoach = usagePair(usage, "DAILY_COACH");
  const weeklyCoach = usagePair(usage, "WEEKLY_COACH");
  const tradingDna = usagePair(usage, "TRADING_DNA");
  const coachUsed = dailyCoach.used + weeklyCoach.used;
  const coachLimit = dailyCoach.limit === -1 || weeklyCoach.limit === -1
    ? -1
    : dailyCoach.limit + weeklyCoach.limit;

  return (
    <section style={{ ...card, padding: 18, marginTop: 18 }}>
      <div style={header}>
        <div>
          <h2 style={{ ...sectionTitle, fontSize: 20 }}>
            {loading ? "Loading usage" : `${usage?.plan === "PRO" ? "Pro" : "Free"} Plan`}
          </h2>
          <p style={{ ...mutedText, margin: 0 }}>
            Reset: {usage?.resetDate ? formatDate(usage.resetDate) : "Monthly"}
          </p>
        </div>
        <Link to="/billing" style={viewButton}>View Billing</Link>
      </div>

      {error && <p style={{ color: "#fecaca", marginBottom: 0 }}>{error}</p>}

      <div style={usageGrid}>
        <MiniUsage label="AI Reviews" used={aiReviews.used} limit={aiReviews.limit} />
        <MiniUsage label="Coach" used={coachUsed} limit={coachLimit} />
        <MiniUsage label="Trading DNA" used={tradingDna.used} limit={tradingDna.limit} />
      </div>
    </section>
  );
}

function MiniUsage({ label, used, limit }: { label: string; used: number; limit: number }) {
  const unlimited = limit === -1;
  const percent = unlimited || limit <= 0 ? 0 : Math.min(100, Math.round((used / limit) * 100));

  return (
    <div>
      <div style={miniTop}>
        <span>{label}</span>
        <strong>{unlimited ? "Unlimited" : `${used}/${limit}`}</strong>
      </div>
      <div style={track}>
        <div style={{ ...fill, width: `${percent}%` }} />
      </div>
    </div>
  );
}

function usagePair(usage: BillingUsage | null, key: string) {
  return {
    used: usage?.usageThisMonth[key] ?? 0,
    limit: usage?.limits[key] ?? 0,
  };
}

function formatDate(value: string) {
  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString([], { month: "short", day: "numeric", year: "numeric" });
}

const header: CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  gap: 14,
  flexWrap: "wrap",
};

const usageGrid: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))",
  gap: 14,
  marginTop: 16,
};

const miniTop: CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  gap: 10,
  color: "#cbd5e1",
  fontSize: 13,
  fontWeight: 850,
};

const track: CSSProperties = {
  height: 7,
  borderRadius: 999,
  background: "rgba(51,65,85,0.82)",
  overflow: "hidden",
  marginTop: 8,
};

const fill: CSSProperties = {
  height: "100%",
  borderRadius: 999,
  background: "linear-gradient(90deg, #22c55e, #38bdf8)",
};

const viewButton: CSSProperties = {
  padding: "10px 12px",
  borderRadius: 12,
  border: "1px solid rgba(56,189,248,0.28)",
  background: "rgba(56,189,248,0.1)",
  color: "#bae6fd",
  textDecoration: "none",
  fontWeight: 900,
};
