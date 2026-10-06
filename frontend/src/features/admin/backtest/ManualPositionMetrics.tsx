import type { CSSProperties } from "react";
import type { BacktestTradeChart } from "../../../api/backtestApi";
import type { ManualPosition, ManualPositionCalculation } from "./manualPositionCalculator";

type Props = { position: ManualPosition | null; calculation: ManualPositionCalculation; chart: BacktestTradeChart };
const price = (value: number | string | null | undefined) => value == null ? "-" : Number(value).toFixed(3).replace(/\.000$/, "");
const number = (value: number | undefined) => value == null ? "-" : value.toFixed(3).replace(/\.000$/, "");

export default function ManualPositionMetrics({ position, calculation, chart }: Props) {
  if (!position) return null;
  const engine = chart.facts;
  return <aside style={panel} aria-label="Manual position metrics"><h3 style={title}>Manual {position.direction === "LONG" ? "Long" : "Short"} Position</h3>{!calculation.valid ? <p style={invalid}>{calculation.validationMessage}</p> : <><Metric label="Entry" value={price(position.entry)} /><Metric label="Stop" value={price(position.stop)} /><Metric label="Target" value={price(position.target)} /><Metric label="Risk" value={`${number(calculation.riskPoints)} pts`} /><Metric label="Risk" value={`${number(calculation.riskTicks)} ticks`} /><Metric label="Reward" value={`${number(calculation.rewardPoints)} pts`} /><Metric label="Reward" value={`${number(calculation.rewardTicks)} ticks`} /><Metric label="R:R" value={`1 : ${number(calculation.rewardRiskRatio)}`} /><Metric label="Potential" value={`+${number(calculation.potentialR)}R`} /></>}
    <div style={comparison}><strong>Engine vs Manual</strong><div style={comparisonGrid}><span /> <b>Engine</b><b>Manual</b><span>Entry</span><span>{price(engine.entryPrice)}</span><span>{price(position.entry)}</span><span>Stop</span><span>{price(engine.stopPrice)}</span><span>{price(position.stop)}</span><span>Target</span><span>{price(engine.tp1Price)}</span><span>{price(position.target)}</span><span>Target R</span><span>{price(engine.tp1R)}R</span><span>{calculation.valid ? `${number(calculation.potentialR)}R` : "-"}</span></div></div>
  </aside>;
}
function Metric({ label, value }: { label: string; value: string }) { return <div style={metric}><span>{label}</span><strong>{value}</strong></div>; }
const panel: CSSProperties = { display: "grid", alignContent: "start", gap: 8, padding: 12, border: "1px solid rgba(45,212,191,.35)", borderRadius: 10, background: "rgba(6,78,59,.14)" };
const title: CSSProperties = { margin: 0, color: "#ccfbf1", fontSize: 15 };
const metric: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 10, color: "#d1fae5", fontSize: 13 };
const invalid: CSSProperties = { margin: 0, color: "#fecaca", lineHeight: 1.45 };
const comparison: CSSProperties = { display: "grid", gap: 8, marginTop: 6, paddingTop: 10, borderTop: "1px solid rgba(148,163,184,.22)", color: "#cbd5e1", fontSize: 12 };
const comparisonGrid: CSSProperties = { display: "grid", gridTemplateColumns: "minmax(0,1fr) minmax(0,1fr) minmax(0,1fr)", gap: "5px 8px" };
