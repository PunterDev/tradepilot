import { useEffect, useMemo, useState, type CSSProperties } from "react";
import { Link } from "react-router-dom";
import {
  downgradeUserToFree,
  getAdminUsers,
  searchAdminUsers,
  upgradeUserToPro,
  type AdminUser,
} from "../../api/adminApi";
import type { AdminContactMessage } from "../../api/adminContactApi";
import { getAdminDashboardSummary, type AdminDashboardSummary } from "../../api/adminDashboardApi";
import { useCachedRequest } from "../../hooks/useCachedRequest";
import { button, card, input, label, mutedText, sectionTitle } from "../../styles/dashboardStyles";

type PendingAction = {
  type: "upgrade" | "downgrade";
  user: AdminUser;
};

export default function AdminDashboardPage() {
  const [summary, setSummary] = useState<AdminDashboardSummary | null>(null);
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [usersLoading, setUsersLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [pendingAction, setPendingAction] = useState<PendingAction | null>(null);
  const [reason, setReason] = useState("");
  const [proUntil, setProUntil] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const cachedSummary = useCachedRequest("admin-dashboard-summary", getAdminDashboardSummary, 45_000);

  useEffect(() => {
    setSummary(cachedSummary.data);
    setUsers(cachedSummary.data?.recentUsers ?? []);
    setLoading(cachedSummary.loading);
    setError(cachedSummary.error === "Unable to load data." ? "Unable to load admin dashboard." : cachedSummary.error);
  }, [cachedSummary.data, cachedSummary.error, cachedSummary.loading]);

  async function loadDashboard() {
    setLoading(true);
    setError("");
    try {
      const nextSummary = await cachedSummary.refresh(true);
      setSummary(nextSummary);
      setUsers(nextSummary.recentUsers ?? []);
    } catch {
      setError("Unable to load admin dashboard.");
    } finally {
      setLoading(false);
    }
  }

  async function handleSearch() {
    setUsersLoading(true);
    setError("");
    try {
      setUsers(query.trim() ? await searchAdminUsers(query.trim()) : await getAdminUsers());
    } catch {
      setError("Unable to search users.");
    } finally {
      setUsersLoading(false);
    }
  }

  function openAction(type: PendingAction["type"], user: AdminUser) {
    setPendingAction({ type, user });
    setReason("");
    setProUntil("");
    setMessage("");
    setError("");
  }

  async function confirmAction() {
    if (!pendingAction || !reason.trim()) return;

    setSubmitting(true);
    setError("");
    setMessage("");
    try {
      if (pendingAction.type === "upgrade") {
        await upgradeUserToPro(pendingAction.user.id, reason.trim(), proUntil || null);
        setMessage(`${pendingAction.user.email} is now on Pro.`);
      } else {
        await downgradeUserToFree(pendingAction.user.id, reason.trim());
        setMessage(`${pendingAction.user.email} is now on Free.`);
      }

      setPendingAction(null);
      await loadDashboard();
    } catch {
      setError("Admin action failed. Please verify permissions and try again.");
    } finally {
      setSubmitting(false);
    }
  }

  const conversionRate = useMemo(() => {
    if (!summary?.totalUsers) return "0%";
    return `${Math.round((summary.proUsers / summary.totalUsers) * 100)}%`;
  }, [summary]);

  const aiUsageThisMonth =
    (summary?.totalAiReviews ?? 0) +
    (summary?.dailyCoachUsage ?? 0) +
    (summary?.weeklyCoachUsage ?? 0) +
    (summary?.tradingDnaUsage ?? 0);

  return (
    <div style={pageWrap}>
      <section style={header}>
        <div>
          <p style={eyebrow}>Admin Command Center</p>
          <h1 style={title}>TradePilot Business Dashboard</h1>
          <p style={mutedText}>
            Monitor growth, contact demand, plan movement, and AI usage from one executive view.
          </p>
        </div>
        <div style={quickActions}>
          <button type="button" onClick={() => void loadDashboard()} style={secondaryLinkButton}>Refresh</button>
          <Link to="/admin/contact-messages" style={linkButton}>View Contact Messages</Link>
          <Link to="/billing" style={secondaryLinkButton}>View Billing</Link>
          <Link to="/dev-tools" style={secondaryLinkButton}>Dev Tools</Link>
        </div>
      </section>

      {message && <div style={successBox}>{message}</div>}
      {error && <div style={errorBox}>{error}</div>}

      <section style={kpiGrid}>
        <Kpi label="Total Users" value={summary?.totalUsers ?? 0} loading={loading} />
        <Kpi label="Pro Users" value={summary?.proUsers ?? 0} loading={loading} accent="teal" />
        <Kpi label="Free Users" value={summary?.freeUsers ?? 0} loading={loading} />
        <Kpi label="Conversion Rate" value={conversionRate} loading={loading} accent="purple" />
        <Kpi label="New Contacts" value={summary?.newContactMessages ?? 0} loading={loading} accent="green" />
        <Kpi label="Failed Emails" value={summary?.failedEmailMessages ?? 0} loading={loading} accent="amber" />
        <Kpi label="Total Trades" value={summary?.totalTrades ?? 0} loading={loading} />
        <Kpi label="AI Usage This Month" value={aiUsageThisMonth} loading={loading} accent="blue" />
      </section>

      <section style={mainGrid}>
        <article style={panel}>
          <div style={panelHeader}>
            <div>
              <h2 style={sectionTitle}>Recent Signups</h2>
              <p style={mutedText}>Newest user accounts and verification state.</p>
            </div>
          </div>
          <div style={signupTableWrap}>
            {loading ? (
              <div style={emptyState}>Loading recent users...</div>
            ) : (summary?.recentUsers ?? []).length === 0 ? (
              <div style={emptyState}>No recent signups yet.</div>
            ) : (
              <div style={signupTable}>
                <div style={{ ...signupGridRow, ...signupHeaderRow }}>
                  <span>Email</span>
                  <span>Name</span>
                  <span>Plan</span>
                  <span>Role</span>
                  <span>Verified</span>
                  <span>Joined</span>
                </div>
                {(summary?.recentUsers ?? []).map((user) => (
                  <div key={user.id} style={signupGridRow}>
                    <span style={truncateText} title={user.email}>{user.email}</span>
                    <span style={truncateText} title={user.name}>{user.name}</span>
                    <PlanBadge plan={user.plan} />
                    <RoleBadge role={user.role} />
                    <VerifiedBadge verified={user.emailVerified} />
                    <span style={dateCell}>{formatDate(user.createdAt)}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </article>

        <article style={panel}>
          <div style={panelHeader}>
            <div>
              <h2 style={sectionTitle}>Contact Inbox</h2>
              <p style={mutedText}>Latest leads and support requests.</p>
            </div>
            <Link to="/admin/contact-messages" style={smallLink}>View Messages</Link>
          </div>
          <div style={messageList}>
            {loading ? (
              <div style={emptyState}>Loading messages...</div>
            ) : (summary?.recentContactMessages ?? []).length === 0 ? (
              <div style={emptyState}>No contact messages yet.</div>
            ) : (
              (summary?.recentContactMessages ?? []).map((contact) => (
                <ContactPreview key={contact.id} contact={contact} />
              ))
            )}
          </div>
        </article>
      </section>

      <section style={overviewGrid}>
        <article style={panel}>
          <h2 style={sectionTitle}>Billing & Plans</h2>
          <div style={planBars}>
            <PlanRow label="Free users" value={summary?.freeUsers ?? 0} total={summary?.totalUsers ?? 0} />
            <PlanRow label="Pro users" value={summary?.proUsers ?? 0} total={summary?.totalUsers ?? 0} accent="#34d399" />
            <PlanRow label="Admin users" value={summary?.adminUsers ?? 0} total={summary?.totalUsers ?? 0} accent="#a78bfa" />
          </div>
          <div style={billingGrid}>
            <Detail label="Verified" value={summary?.verifiedUsers ?? 0} />
            <Detail label="Unverified" value={summary?.unverifiedUsers ?? 0} />
            <Detail label="Manual plan changes" value={summary?.recentUpgrades?.length ?? 0} />
            <Detail label="Trading accounts" value={summary?.totalTradingAccounts ?? 0} />
          </div>
        </article>

        <article style={panel}>
          <h2 style={sectionTitle}>AI Usage Overview</h2>
          <div style={usageGrid}>
            <UsageMetric label="AI Reviews" value={summary?.totalAiReviews ?? 0} />
            <UsageMetric label="Daily Coach" value={summary?.dailyCoachUsage ?? 0} />
            <UsageMetric label="Weekly Coach" value={summary?.weeklyCoachUsage ?? 0} />
            <UsageMetric label="Trading DNA" value={summary?.tradingDnaUsage ?? 0} />
          </div>
        </article>

        <article style={panel}>
          <h2 style={sectionTitle}>Recent Plan Movement</h2>
          <div style={messageList}>
            {loading ? (
              <div style={emptyState}>Loading plan movement...</div>
            ) : (summary?.recentUpgrades ?? []).length === 0 ? (
              <div style={emptyState}>No manual upgrades or downgrades yet.</div>
            ) : (
              (summary?.recentUpgrades ?? []).map((upgrade) => (
                <div key={upgrade.id} style={upgradeRow}>
                  <strong>{upgrade.action === "UPGRADE_TO_PRO" ? "Upgrade to Pro" : "Downgrade to Free"}</strong>
                  <span>{upgrade.reason || "No reason provided"}</span>
                  <small>{formatDate(upgrade.createdAt)}</small>
                </div>
              ))
            )}
          </div>
        </article>
      </section>

      <section style={panel}>
        <div style={panelHeader}>
          <div>
            <h2 style={sectionTitle}>User Management</h2>
            <p style={mutedText}>Search users and apply audited manual plan changes.</p>
          </div>
          <button type="button" onClick={loadDashboard} style={secondaryButton}>Refresh</button>
        </div>

        <div style={searchRow}>
          <div style={{ flex: 1, minWidth: 220 }}>
            <label style={label}>Search users</label>
            <input
              style={input}
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") void handleSearch();
              }}
              placeholder="email or name"
            />
          </div>
          <button type="button" onClick={handleSearch} style={{ ...button, width: 150 }}>
            Search
          </button>
        </div>

        <div style={userGrid}>
          {usersLoading ? (
            <div style={emptyState}>Loading users...</div>
          ) : users.length === 0 ? (
            <div style={emptyState}>No users found.</div>
          ) : (
            users.map((user) => (
              <article key={user.id} style={userCard}>
                <div style={userCardTop}>
                  <div>
                    <h3 style={userName}>{user.name}</h3>
                    <p style={userEmail}>{user.email}</p>
                  </div>
                  <PlanBadge plan={user.plan} />
                </div>
                <div style={compactDetails}>
                  <Detail label="Role" value={user.role} />
                  <Detail label="Verified" value={user.emailVerified ? "Yes" : "No"} />
                  <Detail label="Status" value={user.subscriptionStatus || "NONE"} />
                  <Detail label="Joined" value={formatDate(user.createdAt)} />
                </div>
                <div style={actionRow}>
                  <button
                    type="button"
                    onClick={() => openAction("upgrade", user)}
                    disabled={user.plan === "PRO"}
                    style={{ ...button, width: "auto", opacity: user.plan === "PRO" ? 0.48 : 1 }}
                  >
                    Upgrade to Pro
                  </button>
                  <button
                    type="button"
                    onClick={() => openAction("downgrade", user)}
                    disabled={user.plan === "FREE"}
                    style={{ ...dangerButton, opacity: user.plan === "FREE" ? 0.48 : 1 }}
                  >
                    Downgrade
                  </button>
                </div>
              </article>
            ))
          )}
        </div>
      </section>

      {pendingAction && (
        <div style={modalOverlay}>
          <section style={modalCard}>
            <h2 style={sectionTitle}>
              {pendingAction.type === "upgrade" ? "Upgrade to Pro" : "Downgrade to Free"}
            </h2>
            <p style={mutedText}>
              This action affects {pendingAction.user.email} immediately and will be written to the admin audit log.
            </p>

            <div style={{ marginTop: 18 }}>
              <label style={label}>Reason required</label>
              <textarea
                style={{ ...input, minHeight: 100, resize: "vertical" }}
                value={reason}
                onChange={(event) => setReason(event.target.value)}
                placeholder="Manual admin upgrade for beta tester"
              />
            </div>

            {pendingAction.type === "upgrade" && (
              <div style={{ marginTop: 16 }}>
                <label style={label}>Pro until optional</label>
                <input
                  style={input}
                  type="datetime-local"
                  value={proUntil}
                  onChange={(event) => setProUntil(event.target.value)}
                />
              </div>
            )}

            <div style={modalActions}>
              <button type="button" onClick={() => setPendingAction(null)} style={secondaryButton}>
                Cancel
              </button>
              <button
                type="button"
                onClick={confirmAction}
                disabled={!reason.trim() || submitting}
                style={{
                  ...(pendingAction.type === "upgrade" ? button : dangerButton),
                  width: 180,
                  opacity: !reason.trim() || submitting ? 0.55 : 1,
                }}
              >
                {submitting ? "Saving..." : "Confirm"}
              </button>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}

function Kpi({ label, value, loading, accent = "slate" }: { label: string; value: string | number; loading: boolean; accent?: "slate" | "teal" | "purple" | "green" | "amber" | "blue" }) {
  const color = {
    slate: "#e5eefc",
    teal: "#5eead4",
    purple: "#c4b5fd",
    green: "#86efac",
    amber: "#fde68a",
    blue: "#93c5fd",
  }[accent];
  return (
    <article style={{ ...kpiCard, borderColor: `${color}33` }}>
      <span>{label}</span>
      <strong style={{ color }}>{loading ? "..." : value}</strong>
    </article>
  );
}

function ContactPreview({ contact }: { contact: AdminContactMessage }) {
  return (
    <div style={contactRow}>
      <div>
        <strong>{contact.subject}</strong>
        <span>{contact.name} · {contact.email}</span>
      </div>
      <div style={{ display: "grid", gap: 6, justifyItems: "end" }}>
        <StatusBadge status={contact.status} />
        {!contact.emailSent && <span style={warningBadge}>Failed Email</span>}
      </div>
    </div>
  );
}

function PlanRow({ label, value, total, accent = "#38bdf8" }: { label: string; value: number; total: number; accent?: string }) {
  const percent = total > 0 ? Math.min(100, Math.round((value / total) * 100)) : 0;
  return (
    <div style={planRow}>
      <div style={rowTop}>
        <span>{label}</span>
        <strong>{value}</strong>
      </div>
      <div style={barTrack}>
        <div style={{ ...barFill, width: `${percent}%`, background: accent }} />
      </div>
    </div>
  );
}

function UsageMetric({ label, value }: { label: string; value: number }) {
  return (
    <div style={usageMetric}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function Detail({ label, value }: { label: string; value: string | number }) {
  return (
    <div style={detailBox}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function PlanBadge({ plan }: { plan: AdminUser["plan"] }) {
  return <span style={plan === "PRO" ? proBadge : freeBadge}>{plan}</span>;
}

function RoleBadge({ role }: { role: AdminUser["role"] }) {
  return <span style={role === "ADMIN" ? adminRoleBadge : userRoleBadge}>{role}</span>;
}

function VerifiedBadge({ verified }: { verified: boolean }) {
  return <span style={verified ? verifiedBadge : unverifiedBadge}>{verified ? "Yes" : "No"}</span>;
}

function StatusBadge({ status }: { status: AdminContactMessage["status"] }) {
  const colors = {
    NEW: ["#bbf7d0", "rgba(34,197,94,0.14)", "rgba(34,197,94,0.3)"],
    OPEN: ["#bae6fd", "rgba(14,165,233,0.14)", "rgba(56,189,248,0.3)"],
    REPLIED: ["#ddd6fe", "rgba(139,92,246,0.14)", "rgba(139,92,246,0.3)"],
    ARCHIVED: ["#cbd5e1", "rgba(71,85,105,0.26)", "rgba(148,163,184,0.2)"],
  } satisfies Record<AdminContactMessage["status"], string[]>;
  const [color, background, border] = colors[status];
  return <span style={{ ...badgeBase, color, background, border: `1px solid ${border}` }}>{status}</span>;
}

function formatDate(value?: string | null) {
  if (!value) return "Not yet";
  return new Date(value).toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

const pageWrap: CSSProperties = { display: "grid", gap: 22 };
const header: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 20, flexWrap: "wrap", alignItems: "start" };
const eyebrow: CSSProperties = { margin: 0, color: "#34d399", fontSize: 12, fontWeight: 900, textTransform: "uppercase", letterSpacing: "0.12em" };
const title: CSSProperties = { margin: "8px 0", color: "#f8fafc", fontSize: 40, letterSpacing: "-0.04em" };
const quickActions: CSSProperties = { display: "flex", gap: 10, flexWrap: "wrap", justifyContent: "flex-end" };
const linkButton: CSSProperties = { ...button, width: "auto", textDecoration: "none", display: "inline-flex", alignItems: "center", justifyContent: "center" };
const secondaryLinkButton: CSSProperties = { ...linkButton, background: "rgba(15,23,42,0.85)", border: "1px solid rgba(148,163,184,0.18)", boxShadow: "none" };
const smallLink: CSSProperties = { color: "#67e8f9", fontWeight: 900, textDecoration: "none" };
const kpiGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(190px, 1fr))", gap: 14 };
const kpiCard: CSSProperties = { ...card, padding: 18, minHeight: 112, display: "grid", gap: 8, alignContent: "center" };
const mainGrid: CSSProperties = { display: "grid", gridTemplateColumns: "minmax(0, 1.25fr) minmax(320px, 0.75fr)", gap: 18 };
const overviewGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: 18 };
const panel: CSSProperties = { ...card, minWidth: 0 };
const panelHeader: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 14, alignItems: "start", flexWrap: "wrap", marginBottom: 16 };
const signupTableWrap: CSSProperties = { overflowX: "auto", maxWidth: "100%" };
const signupTable: CSSProperties = { minWidth: 790, display: "grid", color: "#cbd5e1", fontSize: 14 };
const signupGridTemplate = "minmax(220px, 2fr) minmax(160px, 1.3fr) 90px 90px 90px 130px";
const signupGridRow: CSSProperties = {
  display: "grid",
  gridTemplateColumns: signupGridTemplate,
  gap: 12,
  alignItems: "center",
  padding: "12px 14px",
  borderBottom: "1px solid rgba(148,163,184,0.12)",
  minWidth: 0,
};
const signupHeaderRow: CSSProperties = {
  color: "#94a3b8",
  fontSize: 11,
  fontWeight: 950,
  textTransform: "uppercase",
  letterSpacing: "0.08em",
  background: "rgba(15,23,42,0.48)",
  borderRadius: 14,
};
const truncateText: CSSProperties = { minWidth: 0, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" };
const dateCell: CSSProperties = { ...truncateText, color: "#94a3b8", fontSize: 13 };
const messageList: CSSProperties = { display: "grid", gap: 10 };
const contactRow: CSSProperties = { padding: 14, borderRadius: 16, border: "1px solid rgba(148,163,184,0.13)", background: "rgba(2,6,23,0.42)", color: "#cbd5e1", display: "flex", justifyContent: "space-between", gap: 12 };
const planBars: CSSProperties = { display: "grid", gap: 14, marginTop: 16 };
const planRow: CSSProperties = { display: "grid", gap: 8, color: "#cbd5e1" };
const rowTop: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 12, alignItems: "center" };
const barTrack: CSSProperties = { height: 9, borderRadius: 999, background: "rgba(15,23,42,0.95)", overflow: "hidden" };
const barFill: CSSProperties = { height: "100%", borderRadius: 999 };
const billingGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(2, minmax(0, 1fr))", gap: 10, marginTop: 18 };
const usageGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(2, minmax(0, 1fr))", gap: 12, marginTop: 16 };
const usageMetric: CSSProperties = { padding: 16, borderRadius: 18, background: "rgba(2,6,23,0.55)", border: "1px solid rgba(148,163,184,0.14)", display: "grid", gap: 8, color: "#94a3b8" };
const upgradeRow: CSSProperties = { padding: 14, borderRadius: 16, background: "rgba(15,23,42,0.62)", border: "1px solid rgba(148,163,184,0.13)", display: "grid", gap: 5, color: "#cbd5e1" };
const searchRow: CSSProperties = { display: "flex", gap: 14, alignItems: "end", flexWrap: "wrap", marginBottom: 16 };
const userGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: 14 };
const userCard: CSSProperties = { padding: 16, borderRadius: 18, background: "rgba(2,6,23,0.42)", border: "1px solid rgba(148,163,184,0.13)", display: "grid", gap: 14 };
const userCardTop: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 12, alignItems: "start" };
const userName: CSSProperties = { margin: 0, color: "#f8fafc", fontSize: 17 };
const userEmail: CSSProperties = { margin: "4px 0 0", color: "#94a3b8", fontSize: 13, overflowWrap: "anywhere" };
const compactDetails: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(2, minmax(0, 1fr))", gap: 8 };
const detailBox: CSSProperties = { padding: 11, borderRadius: 14, background: "rgba(15,23,42,0.7)", border: "1px solid rgba(148,163,184,0.12)", display: "grid", gap: 4, color: "#94a3b8", fontSize: 12, wordBreak: "break-word" };
const actionRow: CSSProperties = { display: "flex", gap: 10, flexWrap: "wrap" };
const secondaryButton: CSSProperties = { ...button, width: "auto", background: "rgba(14,165,233,0.2)", border: "1px solid rgba(56,189,248,0.24)", boxShadow: "none" };
const dangerButton: CSSProperties = { ...button, width: "auto", background: "linear-gradient(135deg,#ef4444,#991b1b)", boxShadow: "0 14px 35px rgba(239,68,68,0.2)" };
const badgeBase: CSSProperties = { width: "fit-content", padding: "7px 9px", borderRadius: 999, fontSize: 12, fontWeight: 950 };
const freeBadge: CSSProperties = { ...badgeBase, color: "#bae6fd", background: "rgba(14,165,233,0.12)", border: "1px solid rgba(56,189,248,0.24)" };
const proBadge: CSSProperties = { ...badgeBase, color: "#bbf7d0", background: "rgba(34,197,94,0.14)", border: "1px solid rgba(34,197,94,0.28)" };
const userRoleBadge: CSSProperties = { ...badgeBase, color: "#cbd5e1", background: "rgba(71,85,105,0.22)", border: "1px solid rgba(148,163,184,0.18)" };
const adminRoleBadge: CSSProperties = { ...badgeBase, color: "#ddd6fe", background: "rgba(139,92,246,0.14)", border: "1px solid rgba(139,92,246,0.28)" };
const verifiedBadge: CSSProperties = { ...badgeBase, color: "#bbf7d0", background: "rgba(34,197,94,0.14)", border: "1px solid rgba(34,197,94,0.28)" };
const unverifiedBadge: CSSProperties = { ...badgeBase, color: "#cbd5e1", background: "rgba(71,85,105,0.22)", border: "1px solid rgba(148,163,184,0.18)" };
const warningBadge: CSSProperties = { ...badgeBase, color: "#fde68a", background: "rgba(245,158,11,0.14)", border: "1px solid rgba(245,158,11,0.24)" };
const emptyState: CSSProperties = { color: "#94a3b8", padding: 16 };
const errorBox: CSSProperties = { padding: 14, borderRadius: 14, color: "#fecaca", background: "rgba(239,68,68,0.12)", border: "1px solid rgba(239,68,68,0.28)" };
const successBox: CSSProperties = { padding: 14, borderRadius: 14, color: "#bbf7d0", background: "rgba(34,197,94,0.12)", border: "1px solid rgba(34,197,94,0.28)" };
const modalOverlay: CSSProperties = { position: "fixed", inset: 0, zIndex: 60, display: "grid", placeItems: "center", padding: 20, background: "rgba(2,6,23,0.78)", backdropFilter: "blur(10px)" };
const modalCard: CSSProperties = { ...card, width: "min(620px,100%)" };
const modalActions: CSSProperties = { display: "flex", justifyContent: "flex-end", gap: 12, marginTop: 20, flexWrap: "wrap" };
