import type { CSSProperties, ReactNode } from "react";
import type { AuthUser } from "../api/authApi";
import type { BillingUsage } from "../types/billing";
import { button, heroPill } from "../styles/dashboardStyles";
import MobileNav from "./MobileNav";
import SidebarNav, { type NavItem } from "./SidebarNav";
import { UserPill } from "./PageHeader";
import { hasFeature } from "../utils/featureAccess";

type Props = {
  user: AuthUser;
  accountCount: number;
  usage?: BillingUsage | null;
  onLogout: () => void;
  children: ReactNode;
};

const baseNav: NavItem[] = [
  { label: "Dashboard", to: "/dashboard" },
  { label: "Analytics", to: "/analytics" },
  { label: "Calendar", to: "/calendar" },
  { label: "Session", to: "/session-review" },
  { label: "Trading DNA", to: "/trading-dna" },
  { label: "Leak Detector", to: "/leak-detector" },
  { label: "Coach", to: "/coach" },
  { label: "Billing", to: "/billing" },
];

export default function AppShell({ user, accountCount, usage, onLogout, children }: Props) {
  const userNav = baseNav.map((item) => {
    if (item.to === "/analytics") return { ...item, locked: !hasFeature("ADVANCED_ANALYTICS", user, usage) };
    if (item.to === "/session-review") return { ...item, locked: !hasFeature("SESSION_REVIEW", user, usage) };
    if (item.to === "/trading-dna") return { ...item, locked: !hasFeature("TRADING_DNA", user, usage) };
    if (item.to === "/leak-detector") return { ...item, locked: !hasFeature("AI_LEAK_DETECTOR", user, usage) };
    if (item.to === "/coach") return { ...item, locked: !hasFeature("DAILY_COACH", user, usage) };
    return item;
  });
  const adminBaseNav: NavItem[] = accountCount > 0
    ? [...userNav, { label: "Admin", to: "/admin" }, { label: "Users", to: "/admin/users" }, { label: "Contact Messages", to: "/admin/contact-messages" }, { label: "Feature Usage", to: "/admin/feature-usage" }, { label: "Retention Analytics", to: "/admin/retention" }, { label: "Observability", to: "/admin/observability" }]
    : [
        { label: "Admin", to: "/admin" },
        { label: "Users", to: "/admin/users" },
        { label: "Contact Messages", to: "/admin/contact-messages" },
        { label: "Billing", to: "/billing" },
        { label: "Feature Usage", to: "/admin/feature-usage" },
        { label: "Retention Analytics", to: "/admin/retention" },
        { label: "Observability", to: "/admin/observability" },
      ];

  const navItems = import.meta.env.DEV
    ? [
        ...(user.role === "ADMIN" ? adminBaseNav : userNav),
        { label: "Dev Tools", to: "/dev-tools", devOnly: true },
      ]
    : user.role === "ADMIN"
      ? adminBaseNav
      : userNav;
  return (
    <div className="tradepilot-shell" style={shell}>
      <style>
        {`
          @media (max-width: 860px) {
            .tradepilot-shell {
              grid-template-columns: minmax(0, 1fr) !important;
            }
            .tradepilot-sidebar {
              display: none !important;
            }
            .tradepilot-mobile-nav {
              display: grid !important;
            }
            .tradepilot-content {
              padding: 18px !important;
              padding-bottom: 96px !important;
            }
          }
        `}
      </style>
      <SidebarNav items={navItems} />
      <main className="tradepilot-content" style={content}>
        <div style={topBar}>
          <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
            <div style={heroPill}>{accountCount} Accounts</div>
            <div style={planPill}>{usage?.plan === "PRO" ? "Pro Plan" : "Free Plan"}</div>
            <UserPill name={user.name} />
          </div>
          <button type="button" onClick={onLogout} style={{ ...button, width: 118 }}>
            Logout
          </button>
        </div>
        {children}
      </main>
      <MobileNav items={navItems} />
    </div>
  );
}

const shell: CSSProperties = {
  minHeight: "100vh",
  height: "100vh",
  display: "grid",
  gridTemplateColumns: "244px minmax(0, 1fr)",
  background:
    "radial-gradient(circle at top left, rgba(34,197,94,0.14), transparent 32%), linear-gradient(135deg, #020617 0%, #0b1120 45%, #111827 100%)",
  color: "#e5e7eb",
  fontFamily:
    "Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, Arial",
};

const content: CSSProperties = {
  minWidth: 0,
  minHeight: "100vh",
  height: "100vh",
  overflowY: "auto",
  overflowX: "hidden",
  background:
    "radial-gradient(circle at top left, rgba(34,197,94,0.08), transparent 28%), linear-gradient(135deg, rgba(2,6,23,0.1), rgba(15,23,42,0.12))",
  padding: 28,
  paddingBottom: 90,
};

const topBar: CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  gap: 16,
  marginBottom: 24,
};

const planPill: CSSProperties = {
  ...heroPill,
  background: "rgba(56,189,248,0.11)",
  border: "1px solid rgba(56,189,248,0.28)",
  color: "#bae6fd",
};
