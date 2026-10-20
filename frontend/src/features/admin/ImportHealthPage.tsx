import { useEffect, useState } from "react";
import { getImportHealthSummary, getRecentImportOperations, type ImportHealthRange, type ImportHealthSummary, type RecentImportOperation } from "../../api/importHealthApi";
import PageHeader from "../../layout/PageHeader";
import {
  card as dashboardCard,
  mutedText,
  sectionTitle,
  table as dashboardTable,
  tableCard,
  td as dashboardTd,
  th as dashboardTh,
} from "../../styles/dashboardStyles";

const ranges: [ImportHealthRange, string][] = [["TODAY", "Today"], ["LAST_7_DAYS", "Last 7 days"], ["LAST_30_DAYS", "Last 30 days"]];

export default function ImportHealthPage() {
  const [range, setRange] = useState<ImportHealthRange>("LAST_7_DAYS");
  const [summary, setSummary] = useState<ImportHealthSummary | null>(null);
  const [recent, setRecent] = useState<RecentImportOperation[]>([]);
  useEffect(() => { let active = true; Promise.all([getImportHealthSummary(range), getRecentImportOperations(range)]).then(([next, operations]) => { if (active) { setSummary(next); setRecent(operations); } }).catch(() => { if (active) { setSummary(null); setRecent([]); } }); return () => { active = false; }; }, [range]);
  return <div>
    <PageHeader label="Admin Analytics" title="Import Health" description="Operational CSV import outcomes. Preview failures are tracked separately from save attempts." />
    <div style={rangesStyle}>{ranges.map(([value, label]) => <button key={value} type="button" onClick={() => setRange(value)} aria-pressed={range === value} style={{ ...rangeButton, ...(range === value ? selectedRange : {}) }}>{label}</button>)}</div>
    {!summary ? <p style={muted}>No import health data is available for this range.</p> : <>
      <section style={kpiGrid}>{[["Import Attempts", summary.importAttempts], ["Completed", summary.completedImports], ["Failed", summary.failedImports], ["Success Rate", summary.successRate == null ? "—" : `${summary.successRate}%`], ["Preview Failures", summary.previewFailures], ["Trades Saved", summary.tradesSaved], ["Duplicates", summary.duplicates], ["First Imports", summary.firstImports]].map(([label, value]) => <div key={String(label)} style={kpi}><strong style={kpiValue}>{value}</strong><span style={muted}>{label}</span></div>)}</section>
      <section style={split}><DataTable title="Parser / Source" headers={["Source", "Operations"]} rows={summary.parserBreakdown.map(row => [row.key, row.count])} empty="No completed or failed imports yet." /><DataTable title="Failures" headers={["Stage", "Category", "Count"]} rows={summary.failureBreakdown.map(row => [row.stage, row.category, row.count])} empty="No import failures in this range." /></section>
      <section style={tableWrap}><h2 style={title}>Recent Imports</h2><div data-testid="recent-imports-scroll" style={scroll}><table style={table}><thead><tr>{["Time", "User", "Stage", "Status", "Source", "Parsed", "Saved", "Duplicates", "Failure"].map(value => <th key={value} style={th}>{value}</th>)}</tr></thead><tbody>{recent.length === 0 ? <tr><td colSpan={9} style={empty}>No recent import operations.</td></tr> : recent.map((operation, index) => <tr key={`${operation.occurredAt}-${index}`}><td style={td}>{new Date(operation.occurredAt).toLocaleString()}</td><td style={td}>{operation.user}</td><td style={td}>{operation.stage}</td><td style={td}>{operation.status}</td><td style={td}>{operation.parserSource}</td><td style={td}>{operation.parsedTrades ?? "—"}</td><td style={td}>{operation.savedRows ?? "—"}</td><td style={td}>{operation.duplicateRows ?? "—"}</td><td style={td}>{operation.failureCategory ?? "—"}</td></tr>)}</tbody></table></div></section>
    </>}
  </div>;
}

function DataTable({ title, headers, rows, empty: noRows }: { title: string; headers: string[]; rows: (string | number)[][]; empty: string }) { return <section style={tableWrap}><h2 style={titleStyle}>{title}</h2><div style={scroll}><table style={table}><thead><tr>{headers.map(header => <th key={header} style={th}>{header}</th>)}</tr></thead><tbody>{rows.length ? rows.map((row, index) => <tr key={index}>{row.map((cell, cellIndex) => <td key={cellIndex} style={td}>{cell}</td>)}</tr>) : <tr><td colSpan={headers.length} style={empty}>{noRows}</td></tr>}</tbody></table></div></section>; }
const muted = { ...mutedText, fontSize: 14 };
const rangesStyle = { display: "flex", gap: 8, flexWrap: "wrap" as const, marginBottom: 20 };
const rangeButton = { borderWidth: 1, borderStyle: "solid", borderColor: "rgba(148,163,184,0.24)", background: "rgba(15,23,42,0.72)", color: "#cbd5e1", padding: "9px 13px", borderRadius: 12, cursor: "pointer", fontWeight: 800 };
const selectedRange = { background: "rgba(20,184,166,0.18)", borderColor: "rgba(45,212,191,0.58)", color: "#99f6e4", boxShadow: "0 10px 24px rgba(20,184,166,0.14)" };
const kpiGrid = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(140px, 1fr))", gap: 12, marginBottom: 20 };
const kpi = { ...dashboardCard, borderRadius: 18, padding: 16, display: "grid", gap: 5, boxShadow: "0 14px 40px rgba(0,0,0,0.22)" };
const kpiValue = { color: "#f8fafc", fontSize: 24 };
const split = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: 16, marginBottom: 20 };
const tableWrap = { ...tableCard, padding: 20, minWidth: 0 };
const title = { ...sectionTitle, margin: "0 0 14px", fontSize: 18, color: "#e5eefc" };
const titleStyle = { ...title, fontSize: 16 };
const scroll = { overflowX: "auto" as const };
const table = { ...dashboardTable, minWidth: 560, fontSize: 13 };
const th = { ...dashboardTh, padding: "10px 12px", whiteSpace: "nowrap" as const };
const td = { ...dashboardTd, padding: "10px 12px", whiteSpace: "nowrap" as const };
const empty = { ...td, textAlign: "center" as const, color: "#94a3b8" };
