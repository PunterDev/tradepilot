import type { CSSProperties } from "react";

export const shell: CSSProperties = { display: "grid", gap: 20 };
export const card: CSSProperties = { padding: 18, borderRadius: 18, border: "1px solid rgba(148,163,184,0.16)", background: "rgba(15,23,42,0.78)" };
export const grid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: 14 };
export const compactGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))", gap: 10 };
export const label: CSSProperties = { color: "#94a3b8", fontSize: 12, fontWeight: 900, textTransform: "uppercase", letterSpacing: "0.06em" };
export const muted: CSSProperties = { color: "#94a3b8", margin: 0 };
export const input: CSSProperties = { width: "100%", boxSizing: "border-box", padding: "11px 12px", borderRadius: 12, border: "1px solid rgba(148,163,184,0.24)", background: "rgba(2,6,23,0.7)", color: "#e5e7eb" };
export const textarea: CSSProperties = { ...input, minHeight: 96, resize: "vertical" };
export const button: CSSProperties = { border: "1px solid rgba(52,211,153,0.28)", background: "linear-gradient(135deg, rgba(34,197,94,0.92), rgba(20,184,166,0.86))", color: "#04111d", borderRadius: 12, padding: "11px 14px", fontWeight: 900, cursor: "pointer" };
export const secondaryButton: CSSProperties = { border: "1px solid rgba(148,163,184,0.22)", background: "rgba(15,23,42,0.72)", color: "#e5e7eb", borderRadius: 12, padding: "10px 12px", fontWeight: 800, cursor: "pointer" };
export const dangerButton: CSSProperties = { ...secondaryButton, color: "#fecaca", borderColor: "rgba(248,113,113,0.25)" };
export const badge: CSSProperties = { display: "inline-flex", alignItems: "center", width: "fit-content", padding: "5px 9px", borderRadius: 999, border: "1px solid rgba(52,211,153,0.28)", color: "#86efac", background: "rgba(34,197,94,0.08)", fontSize: 12, fontWeight: 900 };
export const statValue: CSSProperties = { display: "block", marginTop: 6, fontSize: 24, fontWeight: 950, color: "#e5e7eb" };
export const sectionHeader: CSSProperties = { display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12, flexWrap: "wrap" };
