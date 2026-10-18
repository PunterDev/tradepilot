import type { CSSProperties, ReactNode } from "react";
import { useLocation } from "react-router-dom";
import type { AuthUser } from "../api/authApi";
import type { BillingUsage } from "../types/billing";
import { button, heroPill } from "../styles/dashboardStyles";
import MobileNav from "./MobileNav";
import SidebarNav, { type NavItem } from "./SidebarNav";
import { UserPill } from "./PageHeader";
import { hasFeature } from "../utils/featureAccess";
import { isFeatureEnabled, type FeatureAvailability, type ReleaseFeatureKey } from "../config/featureFlags";
import Seo from "../components/Seo";

type Props = {
  user: AuthUser;
  accountCount: number;
  usage?: BillingUsage | null;
  featureAvailability?: FeatureAvailability | null;
  preAnalysisAllowed?: boolean;
  backtestAllowed?: boolean;
  manualBacktestAllowed?: boolean;
  onLogout: () => void;
  children: ReactNode;
};

type AppNavItem = NavItem & { feature?: ReleaseFeatureKey; requiresPreAnalysis?: boolean; requiresBacktest?: boolean; requiresManualBacktest?: boolean };

const baseNav: AppNavItem[] = [
  { label: "Dashboard", to: "/dashboard" },
  { label: "Analytics", to: "/analytics" },
  { label: "Calendar", to: "/calendar" },
  { label: "Session", to: "/session-review" },
  { label: "Trading DNA", to: "/trading-dna", feature: "TRADING_DNA" },
  { label: "Leak Detector", to: "/leak-detector" },
  { label: "Journey", to: "/journey", feature: "JOURNEY" },
  { label: "Playbook", to: "/playbook", feature: "PLAYBOOK" },
  { label: "Coach", to: "/coach" },
  { label: "AI Pre-Analysis", to: "/coach/pre-analysis", requiresPreAnalysis: true },
  { label: "Playbook Backtest", to: "/backtest", requiresBacktest: true },
  { label: "Manual Backtest", to: "/manual-backtest", requiresManualBacktest: true },
  { label: "Billing", to: "/billing" },
];

export default function AppShell({ user, accountCount, usage, featureAvailability, preAnalysisAllowed, backtestAllowed, manualBacktestAllowed, onLogout, children }: Props) {
  const location = useLocation();
  const isAdminRoute = location.pathname.startsWith("/admin");
  const userNav: NavItem[] = baseNav.filter((item) => {
    if (item.requiresPreAnalysis && (!featureAvailability?.v2FeaturesEnabled || !preAnalysisAllowed)) return false;
    if (item.requiresBacktest && !backtestAllowed) return false;
    if (item.requiresManualBacktest && !manualBacktestAllowed) return false;
    return !item.feature || isFeatureEnabled(item.feature, featureAvailability);
  }).map((item) => {
    const { feature: _feature, requiresPreAnalysis: _requiresPreAnalysis, requiresBacktest: _requiresBacktest, requiresManualBacktest: _requiresManualBacktest, ...navItem } = item;
    if (navItem.to === "/analytics") return { ...navItem, locked: !hasFeature("ADVANCED_ANALYTICS", user, usage) };
    if (navItem.to === "/session-review") return { ...navItem, locked: !hasFeature("SESSION_REVIEW", user, usage) };
    if (navItem.to === "/trading-dna") return { ...navItem, locked: !hasFeature("TRADING_DNA", user, usage) };
    if (navItem.to === "/leak-detector") return { ...navItem, locked: !hasFeature("AI_LEAK_DETECTOR", user, usage) };
    if (navItem.to === "/journey") return { ...navItem, locked: !hasFeature("PERFORMANCE_TIMELINE", user, usage) };
    if (navItem.to === "/playbook") return { ...navItem, locked: !hasFeature("PLAYBOOK", user, usage) };
    if (navItem.to === "/coach") return { ...navItem, locked: !hasFeature("DAILY_COACH", user, usage) };
    return navItem;
  });
  const adminBaseNav: NavItem[] = accountCount > 0
    ? [...userNav, { label: "Admin", to: "/admin" }, { label: "Users", to: "/admin/users" }, { label: "Contact Messages", to: "/admin/contact-messages" }, { label: "Feature Usage", to: "/admin/feature-usage" }, { label: "Funnels", to: "/admin/feature-funnels" }, { label: "SEO Analytics", to: "/admin/analytics/seo" }, { label: "Data Trust", to: "/admin/data-trust" }, { label: "Import Health", to: "/admin/import-health" }, { label: "Market Data", to: "/admin/market-data" }, { label: "Retention Analytics", to: "/admin/retention" }, { label: "Observability", to: "/admin/observability" }]
    : [
        { label: "Admin", to: "/admin" },
        { label: "Users", to: "/admin/users" },
        { label: "Contact Messages", to: "/admin/contact-messages" },
        { label: "Billing", to: "/billing" },
        { label: "Feature Usage", to: "/admin/feature-usage" },
        { label: "Funnels", to: "/admin/feature-funnels" },
        { label: "SEO Analytics", to: "/admin/analytics/seo" },
        { label: "Data Trust", to: "/admin/data-trust" },
        { label: "Import Health", to: "/admin/import-health" },
        { label: "Market Data", to: "/admin/market-data" },
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
  const aiCreditsAvailable = usage?.totalAiCreditsAvailable ?? usage?.remaining?.AI_CREDITS;
  const monthlyAllowance = usage?.monthlyAiCredits ?? usage?.limits?.AI_CREDITS;
  const monthlyUsed = usage?.monthlyAiCreditsUsed ?? usage?.usageThisMonth?.AI_CREDITS;
  return (
    <div className="tradepilot-shell" style={shell}>
      <Seo
        title="TradePilot Private Workspace"
        description="Private TradePilot workspace."
        path={location.pathname}
        robots="noindex,nofollow"
      />
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
          <div style={topBarPills}>
            {isAdminRoute ? (
              <>
                <div style={adminPill}>Admin Workspace</div>
                <div style={planPill}>Product Analytics</div>
                <div style={aiCreditPill}>{import.meta.env.DEV ? "Dev Runtime" : "Production Runtime"}</div>
              </>
            ) : (
              <>
                <div style={compactHeroPill}>{accountCount} Accounts</div>
                <div style={planPill}>{usage?.plan === "PRO" ? "Pro Plan" : "Free Plan"}</div>
                {aiCreditsAvailable !== undefined && (
                  <div style={aiCreditPill}>
                    {aiCreditsAvailable} AI credits
                    {monthlyAllowance !== undefined && monthlyUsed !== undefined ? ` · ${monthlyUsed}/${monthlyAllowance} monthly` : ""}
                  </div>
                )}
                <UserPill name={user.name} />
              </>
            )}
          </div>
          <button type="button" data-testid="app-logout-button" onClick={onLogout} style={topLogoutButton}>
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
  marginBottom: 18,
  flexWrap: "wrap",
};

const topBarPills: CSSProperties = { display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" };

const compactHeroPill: CSSProperties = {
  ...heroPill,
  padding: "6px 9px",
  fontSize: 11,
  boxShadow: "none",
};

const planPill: CSSProperties = {
  ...compactHeroPill,
  background: "rgba(56,189,248,0.11)",
  border: "1px solid rgba(56,189,248,0.28)",
  color: "#bae6fd",
};

const adminPill: CSSProperties = {
  ...compactHeroPill,
  background: "rgba(34,197,94,0.14)",
  border: "1px solid rgba(34,197,94,0.3)",
  color: "#bbf7d0",
};

const aiCreditPill: CSSProperties = {
  ...compactHeroPill,
  background: "rgba(20,184,166,0.12)",
  border: "1px solid rgba(45,212,191,0.32)",
  color: "#ccfbf1",
};

const topLogoutButton: CSSProperties = { ...button, width: "auto", minWidth: 88, padding: "8px 12px", boxShadow: "none" };
