import type { RuleViolation } from "../types/discipline";
import { aiReviewSectionStyle } from "../styles/dashboardStyles";

type Props = {
  score?: number;
  violations?: RuleViolation[];
};

const severityColors: Record<string, { background: string; color: string; border: string }> = {
  CRITICAL: {
    background: "rgba(239,68,68,0.14)",
    color: "#fecaca",
    border: "1px solid rgba(248,113,113,0.32)",
  },
  MAJOR: {
    background: "rgba(249,115,22,0.14)",
    color: "#fed7aa",
    border: "1px solid rgba(251,146,60,0.32)",
  },
  MODERATE: {
    background: "rgba(234,179,8,0.14)",
    color: "#fef08a",
    border: "1px solid rgba(250,204,21,0.28)",
  },
};

export default function DisciplinePanel({ score, violations }: Props) {
  if (score == null && !violations?.length) return null;

  return (
    <section style={aiReviewSectionStyle}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12 }}>
        <h4 style={{ margin: 0 }}>Discipline</h4>
        {score != null && (
          <strong style={{ color: score >= 80 ? "#86efac" : score >= 60 ? "#fde68a" : "#fecaca" }}>
            {score}/100
          </strong>
        )}
      </div>

      {violations?.length ? (
        <div style={{ display: "grid", gap: 10, marginTop: 12 }}>
          {violations.map((violation, index) => {
            const severity = violation.severity?.toUpperCase() ?? "MODERATE";
            const severityStyle = severityColors[severity] ?? severityColors.MODERATE;

            return (
              <div
                key={`${violation.ruleCode ?? "violation"}-${index}`}
                style={{
                  padding: 12,
                  borderRadius: 14,
                  background: "rgba(2,6,23,0.36)",
                  border: "1px solid rgba(148,163,184,0.14)",
                }}
              >
                <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
                  <strong style={{ color: "#e5e7eb" }}>{violation.ruleTitle ?? violation.ruleCode ?? "Rule violation"}</strong>
                  <span style={{ ...severityStyle, borderRadius: 999, padding: "4px 8px", fontSize: 11, fontWeight: 900 }}>
                    {severity}
                  </span>
                  {violation.source && (
                    <span style={{ color: "#93c5fd", fontSize: 12, fontWeight: 800 }}>{violation.source}</span>
                  )}
                </div>
                {violation.reason && <p style={{ margin: "8px 0 0", color: "#cbd5e1" }}>{violation.reason}</p>}
              </div>
            );
          })}
        </div>
      ) : (
        <p style={{ marginBottom: 0 }}>No discipline violations returned.</p>
      )}
    </section>
  );
}
