import { NavLink } from "react-router-dom";
import type { CSSProperties } from "react";

export type NavItem = {
  label: string;
  to: string;
  devOnly?: boolean;
};

type Props = {
  items: NavItem[];
};

export default function SidebarNav({ items }: Props) {
  return (
    <aside className="tradepilot-sidebar" style={sidebar}>
      <div>
        <div style={brandMark}>TP</div>
        <div style={brandName}>TradePilot</div>
      </div>

      <nav style={navList}>
        {items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            style={({ isActive }) => navLink(isActive)}
          >
            {item.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}

const sidebar: CSSProperties = {
  position: "sticky",
  top: 0,
  alignSelf: "start",
  minHeight: "100vh",
  height: "100vh",
  padding: "28px 18px",
  borderRight: "1px solid rgba(148,163,184,0.16)",
  background: "rgba(2,6,23,0.58)",
  backdropFilter: "blur(18px)",
};

const brandMark: CSSProperties = {
  width: 42,
  height: 42,
  borderRadius: 12,
  display: "grid",
  placeItems: "center",
  background: "linear-gradient(135deg, #22c55e, #38bdf8)",
  color: "#020617",
  fontWeight: 950,
};

const brandName: CSSProperties = {
  marginTop: 12,
  color: "#f8fafc",
  fontSize: 18,
  fontWeight: 950,
};

const navList: CSSProperties = {
  display: "grid",
  gap: 8,
  marginTop: 30,
};

function navLink(active: boolean): CSSProperties {
  return {
    display: "block",
    padding: "12px 13px",
    borderRadius: 12,
    color: active ? "#f8fafc" : "#94a3b8",
    textDecoration: "none",
    fontWeight: 850,
    background: active ? "rgba(56,189,248,0.15)" : "transparent",
    border: active ? "1px solid rgba(56,189,248,0.28)" : "1px solid transparent",
  };
}
