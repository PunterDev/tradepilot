import { useEffect, type CSSProperties } from "react";
import { trackLockedPreviewViewed } from "../api/featureUsageApi";
import { button } from "../styles/dashboardStyles";

export type LockedFeaturePreview = {
  title: string;
  description: string;
  icon?: string;
};

type LockedFeaturePageProps = {
  eyebrow: string;
  title: string;
  subtitle: string;
  features: LockedFeaturePreview[];
  ctaTitle: string;
  ctaText: string;
  creditsRemaining?: number;
  creditsLimit?: number;
  resetDate?: string;
  onUpgrade: () => void;
};

export default function LockedFeaturePage({
  eyebrow,
  title,
  subtitle,
  features,
  ctaTitle,
  ctaText,
  creditsRemaining,
  creditsLimit = 10,
  resetDate,
  onUpgrade,
}: LockedFeaturePageProps) {
  const visibleCredits = creditsRemaining ?? creditsLimit;

  useEffect(() => {
    void trackLockedPreviewViewed().catch(() => undefined);
  }, []);

  return (
    <section style={pageShell}>
      <style>
        {`
          @media (max-width: 760px) {
            .locked-feature-header {
              grid-template-columns: minmax(0, 1fr) !important;
            }
            .locked-feature-title {
              font-size: 34px !important;
            }
            .locked-feature-grid {
              grid-template-columns: minmax(0, 1fr) !important;
            }
            .locked-feature-cta {
              grid-template-columns: minmax(0, 1fr) !important;
              text-align: left !important;
            }
            .locked-feature-checklist {
              grid-template-columns: minmax(0, 1fr) !important;
            }
            .locked-feature-cta-button {
              width: 100% !important;
            }
          }
        `}
      </style>

      <div className="locked-feature-header" style={headerGrid}>
        <div>
          <div style={eyebrowStyle}>{eyebrow}</div>
          <h1 className="locked-feature-title" style={titleStyle}>{title}</h1>
          <p style={subtitleStyle}>{subtitle}</p>
        </div>

        <div style={planBadge}>
          <div style={planStatus}>⚡ Free Plan</div>
          <div style={planCredits}>{visibleCredits}/{creditsLimit} AI credits available</div>
          {resetDate && <div style={planReset}>Resets {resetDate}</div>}
        </div>
      </div>

      <div className="locked-feature-grid" style={featureGrid}>
        {features.map((feature) => (
          <article key={feature.title} style={previewCard}>
            <div style={previewIcon}>{feature.icon ?? "✦"}</div>
            <h2 style={previewTitle}>{feature.title}</h2>
            <p style={previewText}>{feature.description}</p>
          </article>
        ))}
      </div>

      <div className="locked-feature-cta" data-testid="pro-feature-upgrade-prompt" style={ctaCard}>
        <div>
          <div style={popularBadge}>⭐ Most traders choose Pro</div>
          <h2 style={ctaTitleStyle}>{ctaTitle}</h2>
          <p style={ctaTextStyle}>{ctaText}</p>
          <div className="locked-feature-checklist" style={checklist}>
            {proChecklist.map((item) => (
              <div key={item} style={checkItem}>
                <span style={checkMark}>✓</span>
                <span>{item}</span>
              </div>
            ))}
          </div>
        </div>
        <div style={ctaAction}>
          <button className="locked-feature-cta-button" type="button" onClick={onUpgrade} style={ctaButton}>
            Start Pro — $29.99/month
          </button>
          <div style={secureText}>Cancel anytime • Secure Stripe checkout</div>
        </div>
      </div>
    </section>
  );
}

const proChecklist = [
  "Trading DNA",
  "Daily AI Coach",
  "Weekly AI Coach",
  "Session Review",
  "Advanced Analytics",
  "100 AI credits per month",
];

const pageShell: CSSProperties = {
  display: "grid",
  gap: 20,
  maxWidth: 1180,
  margin: "0 auto",
};

const headerGrid: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "minmax(0, 1fr) auto",
  alignItems: "start",
  gap: 20,
  padding: 26,
  borderRadius: 8,
  border: "1px solid rgba(52,211,153,0.22)",
  background:
    "linear-gradient(135deg, rgba(15,23,42,0.96), rgba(15,23,42,0.78)), radial-gradient(circle at top right, rgba(52,211,153,0.18), transparent 34%)",
  boxShadow: "0 24px 70px rgba(0,0,0,0.28)",
};

const eyebrowStyle: CSSProperties = {
  color: "#6ee7b7",
  fontSize: 12,
  fontWeight: 900,
  textTransform: "uppercase",
  letterSpacing: "0.08em",
};

const titleStyle: CSSProperties = {
  margin: "10px 0 10px",
  color: "#f8fafc",
  fontSize: 44,
  lineHeight: 1.02,
  letterSpacing: 0,
};

const subtitleStyle: CSSProperties = {
  margin: 0,
  maxWidth: 760,
  color: "#b6c2d1",
  fontSize: 16,
  lineHeight: 1.65,
};

const planBadge: CSSProperties = {
  minWidth: 178,
  padding: "10px 12px",
  borderRadius: 8,
  border: "1px solid rgba(148,163,184,0.18)",
  background: "rgba(2,6,23,0.42)",
  boxShadow: "inset 0 1px 0 rgba(255,255,255,0.04)",
};

const planStatus: CSSProperties = {
  color: "#d1fae5",
  fontSize: 12,
  fontWeight: 950,
};

const planCredits: CSSProperties = {
  marginTop: 6,
  color: "#94a3b8",
  fontSize: 12,
  fontWeight: 850,
};

const planReset: CSSProperties = {
  marginTop: 4,
  color: "#64748b",
  fontSize: 11,
  fontWeight: 750,
};

const featureGrid: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(4, minmax(0, 1fr))",
  gap: 14,
};

const previewCard: CSSProperties = {
  minHeight: 172,
  padding: 18,
  borderRadius: 8,
  border: "1px solid rgba(148,163,184,0.16)",
  background: "linear-gradient(180deg, rgba(15,23,42,0.92), rgba(2,6,23,0.72))",
  boxShadow: "inset 0 1px 0 rgba(255,255,255,0.04)",
};

const previewIcon: CSSProperties = {
  width: 34,
  height: 34,
  display: "grid",
  placeItems: "center",
  marginBottom: 14,
  borderRadius: 8,
  border: "1px solid rgba(52,211,153,0.22)",
  background: "rgba(20,83,45,0.26)",
  color: "#86efac",
  fontSize: 16,
  fontWeight: 950,
};

const previewTitle: CSSProperties = {
  margin: "0 0 8px",
  color: "#f8fafc",
  fontSize: 16,
  lineHeight: 1.2,
};

const previewText: CSSProperties = {
  margin: 0,
  color: "#94a3b8",
  fontSize: 14,
  lineHeight: 1.55,
};

const ctaCard: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "minmax(0, 1fr) 240px",
  alignItems: "end",
  gap: 24,
  padding: 24,
  borderRadius: 8,
  border: "1px solid rgba(34,197,94,0.28)",
  background:
    "linear-gradient(135deg, rgba(20,83,45,0.38), rgba(15,23,42,0.94)), radial-gradient(circle at bottom right, rgba(56,189,248,0.16), transparent 36%)",
};

const popularBadge: CSSProperties = {
  display: "inline-flex",
  marginBottom: 12,
  padding: "7px 10px",
  borderRadius: 999,
  border: "1px solid rgba(250,204,21,0.24)",
  background: "rgba(113,63,18,0.22)",
  color: "#fde68a",
  fontSize: 12,
  fontWeight: 900,
};

const ctaTitleStyle: CSSProperties = {
  margin: "0 0 8px",
  color: "#f8fafc",
  fontSize: 24,
  lineHeight: 1.2,
};

const ctaTextStyle: CSSProperties = {
  margin: 0,
  color: "#cbd5e1",
  lineHeight: 1.55,
};

const checklist: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(3, minmax(0, 1fr))",
  gap: "10px 14px",
  marginTop: 18,
};

const checkItem: CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 8,
  color: "#dbeafe",
  fontSize: 13,
  fontWeight: 800,
};

const checkMark: CSSProperties = {
  display: "grid",
  placeItems: "center",
  width: 18,
  height: 18,
  flex: "0 0 auto",
  borderRadius: 999,
  background: "rgba(34,197,94,0.18)",
  color: "#86efac",
  fontSize: 12,
  fontWeight: 950,
};

const ctaAction: CSSProperties = {
  display: "grid",
  gap: 10,
  justifyItems: "stretch",
};

const ctaButton: CSSProperties = {
  ...button,
  width: "100%",
  minHeight: 48,
  borderRadius: 8,
};

const secureText: CSSProperties = {
  color: "#94a3b8",
  fontSize: 12,
  textAlign: "center",
  lineHeight: 1.4,
};
