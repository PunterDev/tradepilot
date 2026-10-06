import type { CSSProperties } from "react";
import type { BacktestTrade } from "../../../api/backtestApi";
import { card, mutedText, sectionTitle } from "../../../styles/dashboardStyles";
import { formatEntryTime, formatPrice, formatR, humanize } from "./backtestDisplay";

export default function BacktestReviewQueue({ trades, onView }: { trades: BacktestTrade[]; onView: (trade: BacktestTrade, trigger?: HTMLButtonElement) => void }) {
  if (!trades.length) return null;
  return <section style={{ ...card, borderRadius: 10, padding: 22, boxShadow: "0 14px 36px rgba(0,0,0,.16)" }}><h2 style={{ ...sectionTitle, fontSize: 20 }}>Manual Review Queue</h2><p style={mutedText}>Representative trades selected for visual validation. Review status remains local to this browser session.</p><div style={scroll}><table style={table}><thead><tr>{["Date", "Entry ET", "Direction", "Entry", "Stop", "TP1", "Runner", "Model A R", "Model B R", "Runner Type", "Review Status", "Review"].map(label => <th key={label}>{label}</th>)}</tr></thead><tbody>{trades.map(trade => { const metadata = parse(trade.setupMetadataJson); return <tr key={trade.id}><td>{trade.tradeDate}</td><td>{formatEntryTime(trade.entryTime)} ET</td><td><span style={badge(trade.direction)}>{trade.direction}</span></td><td>{formatPrice(trade.entryPrice)}</td><td>{formatPrice(trade.stopPrice)}</td><td>{formatPrice(trade.tp1Price)}</td><td>{formatPrice(trade.runnerTargetPrice)}</td><td>{formatR(trade.modelAR)}</td><td>{formatR(trade.modelBR)}</td><td>{humanize(String(metadata.runnerObjectiveType ?? ""))}</td><td>Unreviewed</td><td><button style={viewButton} onClick={event => onView(trade, event.currentTarget)}>Review Trade</button></td></tr>; })}</tbody></table></div><p style={mutedText}>{trades.length} representative trade{trades.length === 1 ? "" : "s"} queued. No outcome ranking is applied.</p></section>;
}
function parse(value: string) { try { return JSON.parse(value) as Record<string, unknown>; } catch { return {}; } }
const scroll: CSSProperties = { overflowX: "auto" };
const table: CSSProperties = { width: "100%", borderCollapse: "collapse", fontSize: 12 };
const viewButton: CSSProperties = { border: "1px solid rgba(45,212,191,.5)", borderRadius: 7, padding: "6px 8px", background: "rgba(13,148,136,.15)", color: "#ccfbf1", fontWeight: 800, cursor: "pointer" };
const badge = (direction: string): CSSProperties => ({ display: "inline-block", borderRadius: 4, padding: "3px 6px", fontSize: 11, fontWeight: 800, color: direction === "LONG" ? "#99f6e4" : "#fbcfe8", background: direction === "LONG" ? "rgba(13,148,136,.16)" : "rgba(190,24,93,.16)" });
