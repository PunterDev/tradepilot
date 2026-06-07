import type React from "react";

export const authPageStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 380px), 1fr))",
  background:
    "linear-gradient(135deg, #03120c 0%, #0f172a 42%, #14130b 100%)",
  color: "#f8fafc",
  fontFamily:
    "Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, Arial",
};

export const authIntroStyle: React.CSSProperties = {
  minHeight: "100%",
  padding: "72px clamp(32px, 7vw, 96px)",
  display: "flex",
  flexDirection: "column",
  justifyContent: "space-between",
  background:
    "linear-gradient(rgba(2,6,23,0.2), rgba(2,6,23,0.62)), url('https://images.unsplash.com/photo-1642790106117-e829e14a795f?auto=format&fit=crop&w=1600&q=80') center / cover",
};

export const authFormShellStyle: React.CSSProperties = {
  minHeight: "100%",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  padding: "48px clamp(24px, 5vw, 72px)",
  background: "rgba(2,6,23,0.78)",
};

export const authPanelStyle: React.CSSProperties = {
  width: "100%",
  maxWidth: 460,
  border: "1px solid rgba(148,163,184,0.24)",
  borderRadius: 8,
  padding: 28,
  background: "rgba(15,23,42,0.92)",
  boxShadow: "0 24px 70px rgba(0,0,0,0.34)",
};

export const authLabelStyle: React.CSSProperties = {
  display: "block",
  color: "#94a3b8",
  fontSize: 12,
  fontWeight: 800,
  letterSpacing: "0.05em",
  textTransform: "uppercase",
  marginBottom: 8,
};

export const authInputStyle: React.CSSProperties = {
  width: "100%",
  boxSizing: "border-box",
  border: "1px solid rgba(148,163,184,0.28)",
  borderRadius: 8,
  background: "rgba(2,6,23,0.72)",
  color: "#f8fafc",
  padding: "13px 14px",
  outline: "none",
};

export const authButtonStyle: React.CSSProperties = {
  width: "100%",
  border: "none",
  borderRadius: 8,
  padding: "13px 18px",
  color: "#052e16",
  background: "linear-gradient(135deg, #6ee7b7, #22c55e)",
  fontWeight: 900,
  cursor: "pointer",
};

export const authLinkButtonStyle: React.CSSProperties = {
  border: "none",
  background: "transparent",
  color: "#6ee7b7",
  fontWeight: 900,
  cursor: "pointer",
  padding: 0,
};

export const authMessageStyle: React.CSSProperties = {
  border: "1px solid rgba(110,231,183,0.34)",
  borderRadius: 8,
  padding: "11px 12px",
  color: "#bbf7d0",
  background: "rgba(20,83,45,0.24)",
  fontSize: 14,
};

export const authErrorStyle: React.CSSProperties = {
  border: "1px solid rgba(248,113,113,0.34)",
  borderRadius: 8,
  padding: "11px 12px",
  color: "#fecaca",
  background: "rgba(127,29,29,0.28)",
  fontSize: 14,
};

export function AuthIntro() {
  return (
    <section style={authIntroStyle}>
      <div>
        <div
          style={{
            color: "#6ee7b7",
            fontSize: 13,
            fontWeight: 900,
            letterSpacing: "0.1em",
            textTransform: "uppercase",
          }}
        >
          TradePilot AI
        </div>
        <h1 style={{ margin: "14px 0", fontSize: 52, lineHeight: 1, maxWidth: 620 }}>
          Your trading cockpit, locked to your account.
        </h1>
        <p style={{ color: "#cbd5e1", fontSize: 18, lineHeight: 1.7, maxWidth: 620 }}>
          Import trades, track prop firm performance, review behavior, and keep coach
          snapshots tied to the trader who owns them.
        </p>
      </div>
      <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
        {["JWT secured", "Private accounts", "AI coach history"].map((item) => (
          <span
            key={item}
            style={{
              border: "1px solid rgba(110,231,183,0.34)",
              borderRadius: 999,
              padding: "10px 14px",
              color: "#d1fae5",
              background: "rgba(2,44,34,0.5)",
              fontWeight: 800,
            }}
          >
            {item}
          </span>
        ))}
      </div>
    </section>
  );
}
