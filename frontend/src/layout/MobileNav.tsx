import { NavLink } from "react-router-dom";
import type { CSSProperties } from "react";
import type { NavItem } from "./SidebarNav";

type Props = {
  items: NavItem[];
};

export default function MobileNav({ items }: Props) {
  return (
    <nav className="tradepilot-mobile-nav" style={mobileNav}>
      {items.slice(0, 7).map((item) => (
        <NavLink key={item.to} to={item.to} end style={({ isActive }) => mobileLink(isActive)}>
          <span style={mobileLabel}>{item.label}</span>
          {item.locked && <span style={mobileBadge}>PRO</span>}
        </NavLink>
      ))}
    </nav>
  );
}

const mobileNav: CSSProperties = {
  position: "fixed",
  left: 12,
  right: 12,
  bottom: 12,
  zIndex: 40,
  display: "none",
  gridTemplateColumns: "repeat(7, minmax(0, 1fr))",
  gap: 6,
  padding: 8,
  borderRadius: 18,
  border: "1px solid rgba(148,163,184,0.2)",
  background: "rgba(2,6,23,0.92)",
  backdropFilter: "blur(18px)",
};

function mobileLink(active: boolean): CSSProperties {
  return {
    position: "relative",
    padding: "9px 4px",
    borderRadius: 12,
    color: active ? "#f8fafc" : "#94a3b8",
    background: active ? "rgba(52,211,153,0.16)" : "transparent",
    textAlign: "center",
    textDecoration: "none",
    fontSize: 11,
    fontWeight: 900,
    whiteSpace: "nowrap",
    overflow: "hidden",
    textOverflow: "ellipsis",
  };
}

const mobileLabel: CSSProperties = {
  display: "block",
  minWidth: 0,
  overflow: "hidden",
  textOverflow: "ellipsis",
};

const mobileBadge: CSSProperties = {
  position: "absolute",
  top: 2,
  right: 3,
  padding: "1px 2px",
  borderRadius: 4,
  background: "rgba(15,23,42,0.86)",
  color: "#6ee7b7",
  fontSize: 6,
  fontWeight: 950,
  lineHeight: 1.2,
};
