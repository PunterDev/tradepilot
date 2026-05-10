import type React from "react";

export const page: React.CSSProperties = {
  minHeight: "100vh",
  padding: 36,
  fontFamily:
    "Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, Arial",
  background:
    "radial-gradient(circle at top left, rgba(34,197,94,0.14), transparent 32%), linear-gradient(135deg, #020617 0%, #0b1120 45%, #111827 100%)",
  color: "#e5e7eb",
};

export const hero: React.CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  marginBottom: 28,
};

export const eyebrow: React.CSSProperties = {
  color: "#34d399",
  fontSize: 13,
  fontWeight: 900,
  textTransform: "uppercase",
  letterSpacing: "0.1em",
};

export const heroTitle: React.CSSProperties = {
  margin: "8px 0 8px",
  fontSize: 42,
  lineHeight: 1,
  letterSpacing: "-0.04em",
};

export const heroText: React.CSSProperties = {
  color: "#94a3b8",
  maxWidth: 760,
  fontSize: 16,
};

export const heroPill: React.CSSProperties = {
  padding: "12px 16px",
  borderRadius: 999,
  background: "rgba(34,197,94,0.12)",
  border: "1px solid rgba(52,211,153,0.35)",
  color: "#6ee7b7",
  fontWeight: 800,
};

export const card: React.CSSProperties = {
  background:
    "linear-gradient(180deg, rgba(15,23,42,0.96), rgba(15,23,42,0.82))",
  padding: 24,
  border: "1px solid rgba(148,163,184,0.18)",
  borderRadius: 24,
  boxShadow: "0 24px 80px rgba(0,0,0,0.35)",
  backdropFilter: "blur(18px)",
};

export const tableCard: React.CSSProperties = {
  background:
    "linear-gradient(180deg, rgba(15,23,42,0.96), rgba(15,23,42,0.82))",
  border: "1px solid rgba(148,163,184,0.18)",
  borderRadius: 24,
  overflow: "hidden",
  boxShadow: "0 24px 80px rgba(0,0,0,0.35)",
};

export const sectionHeader: React.CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  gap: 18,
};

export const sectionTitle: React.CSSProperties = {
  marginTop: 0,
  marginBottom: 8,
  fontSize: 24,
  letterSpacing: "-0.03em",
};

export const mutedText: React.CSSProperties = {
  color: "#94a3b8",
  marginTop: 4,
};

export const formGrid: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "2fr 2fr 1fr",
  gap: 16,
  alignItems: "end",
};

export const statsGrid: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(6, minmax(0, 1fr))",
  gap: 18,
  marginTop: 24,
};

export const statsGridSmall: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(4, minmax(0, 1fr))",
  gap: 18,
  marginTop: 18,
};

export const chartGrid: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "1fr 1fr",
  gap: 24,
  marginTop: 32,
};

export const table: React.CSSProperties = {
  width: "100%",
  borderCollapse: "collapse",
};

export const th: React.CSSProperties = {
  padding: 15,
  textAlign: "left",
  borderBottom: "1px solid rgba(148,163,184,0.16)",
  color: "#94a3b8",
  background: "rgba(2,6,23,0.55)",
  fontSize: 12,
  textTransform: "uppercase",
  letterSpacing: "0.06em",
};

export const td: React.CSSProperties = {
  padding: 15,
  borderBottom: "1px solid rgba(148,163,184,0.10)",
  color: "#e5e7eb",
};

export const label: React.CSSProperties = {
  display: "block",
  fontSize: 12,
  fontWeight: 800,
  marginBottom: 8,
  color: "#94a3b8",
  textTransform: "uppercase",
  letterSpacing: "0.05em",
};

export const input: React.CSSProperties = {
  width: "100%",
  padding: "13px 14px",
  border: "1px solid rgba(148,163,184,0.28)",
  borderRadius: 14,
  background: "rgba(2,6,23,0.65)",
  color: "#f8fafc",
  outline: "none",
};

export const button: React.CSSProperties = {
  width: "100%",
  padding: "13px 18px",
  border: "none",
  borderRadius: 14,
  background: "linear-gradient(135deg, #22c55e, #16a34a)",
  color: "white",
  fontWeight: 800,
  cursor: "pointer",
  boxShadow: "0 14px 35px rgba(34,197,94,0.28)",
};

export const accountChip: React.CSSProperties = {
  padding: "10px 14px",
  borderRadius: 999,
  cursor: "pointer",
  fontWeight: 800,
};

export const tooltipStyle: React.CSSProperties = {
  background: "#020617",
  border: "1px solid rgba(148,163,184,0.24)",
  borderRadius: 12,
  color: "#e5e7eb",
};

export const monthLabel: React.CSSProperties = {
  minWidth: 170,
  textAlign: "center",
  fontWeight: 900,
  color: "#e5e7eb",
};

export const calendarGrid: React.CSSProperties = {
  marginTop: 22,
  display: "grid",
  gridTemplateColumns: "repeat(7, minmax(0, 1fr))",
  gap: 10,
};

export const calendarHeaderCell: React.CSSProperties = {
  color: "#64748b",
  textAlign: "center",
  fontSize: 12,
  fontWeight: 900,
  textTransform: "uppercase",
  letterSpacing: "0.08em",
  paddingBottom: 8,
};

export const calendarCellBase: React.CSSProperties = {
  position: "relative",
  minHeight: 118,
  borderRadius: 18,
  padding: 14,
  background: "rgba(30,41,59,0.72)",
  border: "1px solid rgba(148,163,184,0.14)",
  boxShadow: "inset 0 1px 0 rgba(255,255,255,0.04)",
};

export const calendarDayNumber: React.CSSProperties = {
  position: "absolute",
  top: 12,
  left: 14,
  fontSize: 15,
  fontWeight: 900,
  color: "#e2e8f0",
};

export const calendarPnlWrap: React.CSSProperties = {
  height: "100%",
  display: "flex",
  flexDirection: "column",
  justifyContent: "center",
  alignItems: "flex-start",
  paddingTop: 16,
};

export const calendarPnlAmount: React.CSSProperties = {
  fontSize: 22,
  fontWeight: 950,
  letterSpacing: "-0.03em",
};

export const calendarTradeCount: React.CSSProperties = {
  marginTop: 6,
  color: "#94a3b8",
  fontSize: 13,
  lineHeight: 1.35,
};

export const calendarBadge: React.CSSProperties = {
  position: "absolute",
  top: 12,
  right: 12,
  fontSize: 11,
  fontWeight: 900,
  textTransform: "uppercase",
};

export const calendarMonthlyFooter: React.CSSProperties = {
  marginTop: 20,
  display: "flex",
  justifyContent: "space-between",
  padding: 18,
  borderRadius: 18,
  background: "rgba(2,6,23,0.45)",
  border: "1px solid rgba(148,163,184,0.14)",
};

export const hoverPopup: React.CSSProperties = {
  position: "absolute",
  zIndex: 20,
  left: 12,
  right: 12,
  bottom: 12,
  padding: 12,
  borderRadius: 14,
  background: "rgba(2,6,23,0.96)",
  border: "1px solid rgba(148,163,184,0.24)",
  color: "#e5e7eb",
  fontSize: 12,
  boxShadow: "0 18px 50px rgba(0,0,0,0.45)",
  pointerEvents: "none",
};

export const modalOverlay: React.CSSProperties = {
  position: "fixed",
  inset: 0,
  zIndex: 100,
  background: "rgba(2,6,23,0.78)",
  backdropFilter: "blur(8px)",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  padding: 24,
};

export const modalCard: React.CSSProperties = {
  width: "min(1100px, 96vw)",
  maxHeight: "86vh",
  overflow: "auto",
  background:
    "linear-gradient(180deg, rgba(15,23,42,0.98), rgba(15,23,42,0.92))",
  border: "1px solid rgba(148,163,184,0.24)",
  borderRadius: 24,
  padding: 24,
  boxShadow: "0 30px 120px rgba(0,0,0,0.55)",
};

export const journalGrid: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "1fr 1fr 1fr",
  gap: 16,
  marginTop: 20,
};

export const textarea: React.CSSProperties = {
  ...input,
  minHeight: 95,
  resize: "vertical",
  fontFamily: "inherit",
  lineHeight: 1.5,
};
