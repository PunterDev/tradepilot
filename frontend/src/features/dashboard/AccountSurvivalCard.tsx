import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getSurvivalAssessment, type SurvivalAssessment } from "../../api/accountSurvivalApi";
import { card, mutedText, sectionTitle } from "../../styles/dashboardStyles";

export default function AccountSurvivalCard({ accountId }: { accountId?: number }) {
  const [assessment, setAssessment] = useState<SurvivalAssessment | null>(null);
  useEffect(() => { let cancelled = false; if (!accountId) return; getSurvivalAssessment(accountId).then(value => !cancelled && setAssessment(value)).catch(() => !cancelled && setAssessment(null)); return () => { cancelled = true; }; }, [accountId]);
  if (!accountId) return null;
  const incomplete = !assessment || assessment.state === "INSUFFICIENT_DATA";
  return <section style={{ ...card, marginTop: 20 }} aria-labelledby="account-survival-title">
    <div style={{ display: "flex", justifyContent: "space-between", gap: 16, alignItems: "start", flexWrap: "wrap" }}>
      <div><p style={{ ...mutedText, margin: 0, fontSize: 12, textTransform: "uppercase" }}>Account Survival</p><h2 id="account-survival-title" style={sectionTitle}>{incomplete ? "Setup needed" : humanize(assessment.state)}</h2></div>
      <Link to="/session-review" style={linkStyle}>Review account</Link>
    </div>
    {incomplete ? <><p style={mutedText}>Complete your account risk profile to calculate account survival.</p><Link to="/session-review" style={linkStyle}>Complete risk profile</Link></> : <div style={grid}>
      <Metric label="Remaining buffer" value={money(assessment.remainingBuffer)} /><Metric label="Typical-loss use" value={`${number(assessment.typicalLossBufferPercent).toFixed(1)}%`} /><Metric label="Normal-loss capacity" value={`${number(assessment.normalLossCapacity).toFixed(1)} losses`} /><Metric label="Primary threat" value={assessment.primaryThreat} />
    </div>}
  </section>;
}
function Metric({ label, value }: { label: string; value: string }) { return <div style={{ minWidth: 0 }}><span style={{ ...mutedText, display: "block", fontSize: 12 }}>{label}</span><strong style={{ color: "#f8fafc", display: "block", marginTop: 4, overflowWrap: "anywhere" }}>{value}</strong></div>; }
const grid = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))", gap: 14 } as const;
const linkStyle = { color: "#5eead4", fontWeight: 700, textDecoration: "none" };
const number = (value: unknown) => { const parsed = Number(value ?? 0); return Number.isFinite(parsed) ? parsed : 0; };
const money = (value: unknown) => new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(number(value));
const humanize = (value: string) => value.toLowerCase().replace(/_/g, " ").replace(/\b\w/g, (letter: string) => letter.toUpperCase());
