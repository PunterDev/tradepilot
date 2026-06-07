import { NavLink } from "react-router-dom";
import type { CSSProperties } from "react";
import type { NavItem } from "./SidebarNav";

type Props = {
  items: NavItem[];
};

export default function MobileNav({ items }: Props) {
  return (
    <nav className="tradepilot-mobile-nav" style={mobileNav}>
      {items.slice(0, 6).map((item) => (
        <NavLink key={item.to} to={item.to} style={({ isActive }) => mobileLink(isActive)}>
          {item.label}
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
  gridTemplateColumns: "repeat(6, minmax(0, 1fr))",
  gap: 6,
  padding: 8,
  borderRadius: 18,
  border: "1px solid rgba(148,163,184,0.2)",
  background: "rgba(2,6,23,0.92)",
  backdropFilter: "blur(18px)",
};

function mobileLink(active: boolean): CSSProperties {
  return {
    padding: "9px 6px",
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
