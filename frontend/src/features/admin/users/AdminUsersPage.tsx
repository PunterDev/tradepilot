import { useEffect, useMemo, useState, type CSSProperties } from "react";
import {
  cleanupTradesRange,
  downgradeUserToFree,
  getAdminUser,
  getAdminUsers,
  makeUserAdmin,
  removeUserAdmin,
  searchAdminUsers,
  upgradeUserToPro,
  type AdminTradeCleanupResponse,
  type AdminUserDetail,
  type AdminUserRecord,
} from "../../../api/adminUsersApi";
import PageHeader from "../../../layout/PageHeader";
import { button, card, input, label, mutedText, sectionTitle } from "../../../styles/dashboardStyles";
import type { PagedResponse } from "../../../api/pagination";
import { extractApiError } from "../../../utils/apiError";

type Filter = "ALL" | "FREE" | "PRO" | "USER" | "ADMIN" | "YES" | "NO";
type ActionType = "upgrade" | "downgrade" | "make-admin" | "remove-admin";

type PendingAction = {
  type: ActionType;
  user: AdminUserRecord;
};

type CleanupModalState = {
  detail: AdminUserDetail;
};

export default function AdminUsersPage() {
  const [users, setUsers] = useState<AdminUserRecord[]>([]);
  const [pageInfo, setPageInfo] = useState<PagedResponse<AdminUserRecord> | null>(null);
  const [selected, setSelected] = useState<AdminUserDetail | null>(null);
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(25);
  const [planFilter, setPlanFilter] = useState<Filter>("ALL");
  const [roleFilter, setRoleFilter] = useState<Filter>("ALL");
  const [verifiedFilter, setVerifiedFilter] = useState<Filter>("ALL");
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [pendingAction, setPendingAction] = useState<PendingAction | null>(null);
  const [cleanupModal, setCleanupModal] = useState<CleanupModalState | null>(null);
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    void loadUsers(page);
  }, [page, pageSize]);

  async function loadUsers(nextPage = page) {
    setLoading(true);
    setError("");
    try {
      const next = query.trim()
        ? await searchAdminUsers(query.trim(), { page: nextPage, size: pageSize })
        : await getAdminUsers({ page: nextPage, size: pageSize });
      setPageInfo(next);
      setUsers(next.content);
      if (!selected && next.content[0]) {
        void openUser(next.content[0]);
      }
    } catch {
      setError("Unable to load users.");
    } finally {
      setLoading(false);
    }
  }

  async function handleSearch() {
    const nextPage = 0;
    setPage(nextPage);
    setLoading(true);
    setError("");
    try {
      const next = query.trim()
        ? await searchAdminUsers(query.trim(), { page: nextPage, size: pageSize })
        : await getAdminUsers({ page: nextPage, size: pageSize });
      setPageInfo(next);
      setUsers(next.content);
      if (next.content[0]) {
        void openUser(next.content[0]);
      } else {
        setSelected(null);
      }
    } catch {
      setError("Unable to search users.");
    } finally {
      setLoading(false);
    }
  }

  async function openUser(user: AdminUserRecord) {
    setDetailLoading(true);
    setError("");
    try {
      setSelected(await getAdminUser(user.id));
    } catch {
      setError("Unable to load user detail.");
    } finally {
      setDetailLoading(false);
    }
  }

  function openAction(type: ActionType, user: AdminUserRecord) {
    setPendingAction({ type, user });
    setReason("");
    setError("");
    setNotice("");
  }

  async function confirmAction() {
    if (!pendingAction || !reason.trim()) return;

    setSubmitting(true);
    setError("");
    setNotice("");
    try {
      const actionMap = {
        upgrade: upgradeUserToPro,
        downgrade: downgradeUserToFree,
        "make-admin": makeUserAdmin,
        "remove-admin": removeUserAdmin,
      };
      const updated = await actionMap[pendingAction.type](pendingAction.user.id, reason.trim());
      setUsers((current) => current.map((user) => user.id === updated.id ? updated : user));
      setPendingAction(null);
      setNotice(`${updated.email} updated successfully.`);
      await openUser(updated);
    } catch {
      setError("Unable to complete admin action.");
    } finally {
      setSubmitting(false);
    }
  }

  const filteredUsers = useMemo(() => {
    return users.filter((user) => {
      const planOk = planFilter === "ALL" || user.plan === planFilter;
      const roleOk = roleFilter === "ALL" || user.role === roleFilter;
      const verifiedOk =
        verifiedFilter === "ALL" ||
        (verifiedFilter === "YES" && user.emailVerified) ||
        (verifiedFilter === "NO" && !user.emailVerified);
      return planOk && roleOk && verifiedOk;
    });
  }, [users, planFilter, roleFilter, verifiedFilter]);

  const stats = useMemo(() => {
    return {
      total: pageInfo?.totalElements ?? users.length,
      free: users.filter((user) => user.plan === "FREE").length,
      pro: users.filter((user) => user.plan === "PRO").length,
      admins: users.filter((user) => user.role === "ADMIN").length,
      verified: users.filter((user) => user.emailVerified).length,
      unverified: users.filter((user) => !user.emailVerified).length,
    };
  }, [users, pageInfo?.totalElements]);

  return (
    <div style={pageWrap}>
      <PageHeader
        label="Admin CRM"
        title="Users"
        description="Search, inspect, and manage TradePilot users with plan, role, trading, AI, and audit context."
      />

      <section style={kpiGrid}>
        <Kpi label="Total Users" value={stats.total} />
        <Kpi label="Free Users" value={stats.free} />
        <Kpi label="Pro Users" value={stats.pro} accent="#5eead4" />
        <Kpi label="Admin Users" value={stats.admins} accent="#c4b5fd" />
        <Kpi label="Verified" value={stats.verified} accent="#86efac" />
        <Kpi label="Unverified" value={stats.unverified} accent="#fde68a" />
      </section>

      <section style={controlsCard}>
        <div style={{ flex: 1, minWidth: 240 }}>
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
        <FilterSelect labelText="Plan" value={planFilter} onChange={setPlanFilter} options={["ALL", "FREE", "PRO"]} />
        <FilterSelect labelText="Role" value={roleFilter} onChange={setRoleFilter} options={["ALL", "USER", "ADMIN"]} />
        <FilterSelect labelText="Verified" value={verifiedFilter} onChange={setVerifiedFilter} options={["ALL", "YES", "NO"]} />
        <button type="button" onClick={handleSearch} style={{ ...button, width: 140 }}>Search</button>
        <div style={{ minWidth: 132 }}>
          <label style={label}>Page size</label>
          <select
            style={input}
            value={pageSize}
            onChange={(event) => {
              setPageSize(Number(event.target.value));
              setPage(0);
            }}
          >
            {[25, 50, 100].map((size) => <option key={size} value={size}>{size}</option>)}
          </select>
        </div>
      </section>

      {notice && <div style={successBox}>{notice}</div>}
      {error && <div style={errorBox}>{error}</div>}

      <section style={contentGrid}>
        <article style={listPanel}>
          {loading ? (
            <div style={emptyState}>Loading users...</div>
          ) : filteredUsers.length === 0 ? (
            <div style={emptyState}>No users match those filters.</div>
          ) : (
            <div style={userTableWrap}>
              <div style={{ ...userGridRow, ...userHeaderRow }}>
                <span>Email</span>
                <span>Name</span>
                <span>Plan</span>
                <span>Role</span>
                <span>Verified</span>
                <span>Joined</span>
                <span>Accounts</span>
                <span>Trades</span>
                <span>AI Usage</span>
              </div>
              {filteredUsers.map((user) => (
                <button
                  key={user.id}
                  type="button"
                  onClick={() => void openUser(user)}
                  style={{
                    ...userGridRow,
                    ...userRowButton,
                    borderColor: selected?.user.id === user.id ? "rgba(45,212,191,0.48)" : "rgba(148,163,184,0.12)",
                    background: selected?.user.id === user.id ? "rgba(20,184,166,0.12)" : "rgba(2,6,23,0.36)",
                  }}
                >
                  <span style={truncateText} title={user.email}>{user.email}</span>
                  <span style={truncateText} title={user.name}>{user.name}</span>
                  <PlanBadge plan={user.plan} />
                  <RoleBadge role={user.role} />
                  <VerifiedBadge verified={user.emailVerified} />
                  <span style={mutedCell}>{formatDate(user.createdAt)}</span>
                  <span>{user.tradingAccountCount}</span>
                  <span>{user.tradeCount}</span>
                  <span style={mutedCell}>{user.aiReviewCount} reviews · {user.dailyCoachUsage + user.weeklyCoachUsage + user.tradingDnaUsage} coach/DNA</span>
                </button>
              ))}
              <PaginationControls
                page={pageInfo?.page ?? page}
                totalPages={pageInfo?.totalPages ?? 1}
                totalElements={pageInfo?.totalElements ?? filteredUsers.length}
                onPrevious={() => setPage((current) => Math.max(0, current - 1))}
                onNext={() => setPage((current) => Math.min((pageInfo?.totalPages ?? 1) - 1, current + 1))}
              />
            </div>
          )}
        </article>

        <aside style={detailPanel}>
          {detailLoading ? (
            <div style={emptyState}>Loading user detail...</div>
          ) : selected ? (
            <UserDetail
              detail={selected}
              onAction={(type) => openAction(type, selected.user)}
              onCleanup={() => {
                setCleanupModal({ detail: selected });
                setError("");
                setNotice("");
              }}
            />
          ) : (
            <div style={emptyState}>Select a user to inspect profile, billing, trading, and audit history.</div>
          )}
        </aside>
      </section>

      {pendingAction && (
        <div style={modalOverlay}>
          <section style={modalCard}>
            <h2 style={sectionTitle}>{actionTitle(pendingAction.type)}</h2>
            <p style={mutedText}>
              This action affects {pendingAction.user.email} and will be written to the admin audit log.
            </p>
            <label style={label}>Reason required</label>
            <textarea
              style={{ ...input, minHeight: 110, resize: "vertical" }}
              value={reason}
              onChange={(event) => setReason(event.target.value)}
              placeholder="Manual admin change requested by founder."
            />
            <div style={modalActions}>
              <button type="button" onClick={() => setPendingAction(null)} style={secondaryButton}>Cancel</button>
              <button
                type="button"
                onClick={confirmAction}
                disabled={!reason.trim() || submitting}
                style={{ ...(pendingAction.type.includes("remove") || pendingAction.type === "downgrade" ? dangerButton : button), width: 180, opacity: !reason.trim() || submitting ? 0.55 : 1 }}
              >
                {submitting ? "Saving..." : "Confirm"}
              </button>
            </div>
          </section>
        </div>
      )}

      {cleanupModal && (
        <TradeCleanupModal
          detail={cleanupModal.detail}
          onClose={() => setCleanupModal(null)}
          onCompleted={async (summary) => {
            setNotice(`Trade cleanup completed for ${cleanupModal.detail.user.email}. ${summary.tradesDeleted} trades deleted.`);
            await openUser(cleanupModal.detail.user);
            await loadUsers(page);
          }}
          onError={setError}
        />
      )}
    </div>
  );
}

function PaginationControls({
  page,
  totalPages,
  totalElements,
  onPrevious,
  onNext,
}: {
  page: number;
  totalPages: number;
  totalElements: number;
  onPrevious: () => void;
  onNext: () => void;
}) {
  return (
    <div style={paginationRow}>
      <span>{totalElements} users · Page {Math.min(page + 1, Math.max(totalPages, 1))} of {Math.max(totalPages, 1)}</span>
      <div style={{ display: "flex", gap: 8 }}>
        <button type="button" onClick={onPrevious} disabled={page <= 0} style={pagerButton(page <= 0)}>Previous</button>
        <button type="button" onClick={onNext} disabled={page >= totalPages - 1} style={pagerButton(page >= totalPages - 1)}>Next</button>
      </div>
    </div>
  );
}

function UserDetail({
  detail,
  onAction,
  onCleanup,
}: {
  detail: AdminUserDetail;
  onAction: (type: ActionType) => void;
  onCleanup: () => void;
}) {
  const user = detail.user;
  return (
    <div style={detailStack}>
      <div>
        <h2 style={{ ...sectionTitle, marginBottom: 4 }}>{user.name}</h2>
        <p style={{ ...mutedText, overflowWrap: "anywhere" }}>{user.email}</p>
      </div>

      <div style={badgeRow}>
        <PlanBadge plan={user.plan} />
        <RoleBadge role={user.role} />
        <VerifiedBadge verified={user.emailVerified} />
        <span style={sourceBadge}>{user.planSource || "UNKNOWN"}</span>
      </div>

      <div style={detailGrid}>
        <Detail label="Joined" value={formatDate(user.createdAt)} />
        <Detail label="Subscription" value={user.subscriptionStatus || "NONE"} />
        <Detail label="Accounts" value={user.tradingAccountCount} />
        <Detail label="Trades" value={user.tradeCount} />
        <Detail label="AI Reviews" value={user.aiReviewCount} />
        <Detail label="Coach / DNA Usage" value={`${user.dailyCoachUsage}/${user.weeklyCoachUsage}/${user.tradingDnaUsage}`} />
      </div>

      <section style={subCard}>
        <h3 style={smallTitle}>Stripe</h3>
        <Detail label="Customer" value={user.stripeCustomerId || "None"} />
        <Detail label="Subscription" value={user.stripeSubscriptionId || "None"} />
      </section>

      <section style={subCard}>
        <h3 style={smallTitle}>Actions</h3>
        <div style={actionRow}>
          <button type="button" onClick={() => onAction("upgrade")} disabled={user.plan === "PRO"} style={actionButton(user.plan === "PRO")}>Upgrade to Pro</button>
          <button type="button" onClick={() => onAction("downgrade")} disabled={user.plan === "FREE"} style={dangerActionButton(user.plan === "FREE")}>Downgrade</button>
          <button type="button" onClick={() => onAction("make-admin")} disabled={user.role === "ADMIN"} style={actionButton(user.role === "ADMIN")}>Make Admin</button>
          <button type="button" onClick={() => onAction("remove-admin")} disabled={user.role === "USER"} style={dangerActionButton(user.role === "USER")}>Remove Admin</button>
          <button type="button" onClick={onCleanup} style={dangerActionButton(false)}>Cleanup Trades</button>
        </div>
      </section>

      <section style={subCard}>
        <h3 style={smallTitle}>Contact History</h3>
        {detail.contactMessages.length === 0 ? (
          <p style={mutedText}>No contact messages from this email.</p>
        ) : detail.contactMessages.map((message) => (
          <div key={message.id} style={historyRow}>
            <strong>{message.subject}</strong>
            <span>{message.status} · {formatDate(message.createdAt)}</span>
          </div>
        ))}
      </section>

      <section style={subCard}>
        <h3 style={smallTitle}>Admin Audit History</h3>
        {detail.auditLogs.length === 0 ? (
          <p style={mutedText}>No admin actions recorded.</p>
        ) : detail.auditLogs.map((log) => (
          <div key={log.id} style={historyRow}>
            <strong>{log.action}</strong>
            <span>{log.reason || "No reason"} · {formatDate(log.createdAt)}</span>
          </div>
        ))}
      </section>
    </div>
  );
}

function TradeCleanupModal({
  detail,
  onClose,
  onCompleted,
  onError,
}: {
  detail: AdminUserDetail;
  onClose: () => void;
  onCompleted: (summary: AdminTradeCleanupResponse) => Promise<void>;
  onError: (message: string) => void;
}) {
  const user = detail.user;
  const [accountId, setAccountId] = useState<number | "">(detail.accounts[0]?.id ?? "");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [dryRunSummary, setDryRunSummary] = useState<AdminTradeCleanupResponse | null>(null);
  const [finalSummary, setFinalSummary] = useState<AdminTradeCleanupResponse | null>(null);
  const [localError, setLocalError] = useState("");
  const [loadingDryRun, setLoadingDryRun] = useState(false);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    setDryRunSummary(null);
    setFinalSummary(null);
    setConfirmation("");
    setLocalError("");
  }, [accountId, startDate, endDate]);

  const formReady = Boolean(accountId && startDate && endDate);
  const dryRunHasTrades = Boolean(dryRunSummary && dryRunSummary.tradesMatched > 0);
  const deleteReady = formReady && dryRunHasTrades && confirmation === "DELETE" && !loadingDryRun && !deleting;

  async function runCleanup(dryRun: boolean) {
    if (!accountId || !startDate || !endDate) {
      setLocalError("Select an account and date range first.");
      return;
    }

    if (endDate < startDate) {
      setLocalError("End date must be on or after start date.");
      return;
    }

    if (!dryRun && !deleteReady) return;

    setLocalError("");
    onError("");
    if (dryRun) {
      setLoadingDryRun(true);
    } else {
      setDeleting(true);
    }

    try {
      const summary = await cleanupTradesRange({
        userId: user.id,
        accountId: Number(accountId),
        startDate,
        endDate,
        dryRun,
      });

      if (dryRun) {
        setDryRunSummary(summary);
        if (summary.tradesMatched === 0) {
          setLocalError("Dry run found zero matching trades. Delete remains disabled.");
        }
      } else {
        setFinalSummary(summary);
        await onCompleted(summary);
      }
    } catch (error) {
      const message = extractApiError(error, dryRun ? "Dry run failed." : "Trade cleanup failed.");
      setLocalError(message);
      onError(message);
    } finally {
      setLoadingDryRun(false);
      setDeleting(false);
    }
  }

  return (
    <div style={modalOverlay}>
      <section style={{ ...modalCard, width: "min(760px,100%)" }}>
        <div style={modalHeaderRow}>
          <div>
            <h2 style={sectionTitle}>Cleanup Trades</h2>
            <p style={mutedText}>
              Delete imported trades and generated trade-derived data for a scoped user, account, and date range.
            </p>
          </div>
          <button type="button" onClick={onClose} disabled={loadingDryRun || deleting} style={secondaryButton}>Close</button>
        </div>

        <div style={warningBox}>
          This will permanently delete selected trades and generated AI/cache data for this user/account/date range.
          This cannot be undone. Billing, Stripe, user, and account records will not be deleted.
        </div>

        <div style={cleanupUserCard}>
          <Detail label="User ID" value={user.id} />
          <Detail label="User" value={`${user.name} · ${user.email}`} />
        </div>

        <div style={cleanupFormGrid}>
          <div>
            <label style={label}>Account</label>
            <select
              style={input}
              value={accountId}
              onChange={(event) => setAccountId(event.target.value ? Number(event.target.value) : "")}
              disabled={loadingDryRun || deleting}
            >
              <option value="">Select account</option>
              {detail.accounts.map((account) => (
                <option key={account.id} value={account.id}>
                  {account.name} · {account.propFirm}
                  {account.accountNumber ? ` · ${account.accountNumber}` : ""}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label style={label}>Start date</label>
            <input
              type="date"
              style={input}
              value={startDate}
              onChange={(event) => setStartDate(event.target.value)}
              disabled={loadingDryRun || deleting}
            />
          </div>
          <div>
            <label style={label}>End date</label>
            <input
              type="date"
              style={input}
              value={endDate}
              onChange={(event) => setEndDate(event.target.value)}
              disabled={loadingDryRun || deleting}
            />
          </div>
        </div>

        {detail.accounts.length === 0 && (
          <div style={errorBox}>This user has no trading accounts. Cleanup is unavailable.</div>
        )}

        {localError && <div style={errorBox}>{localError}</div>}

        <div style={modalActions}>
          <button
            type="button"
            onClick={() => void runCleanup(true)}
            disabled={!formReady || loadingDryRun || deleting}
            style={{ ...secondaryButton, opacity: !formReady || loadingDryRun || deleting ? 0.55 : 1 }}
          >
            {loadingDryRun ? "Checking..." : "Dry Run"}
          </button>
        </div>

        {dryRunSummary && (
          <CleanupSummary
            title={dryRunSummary.tradesMatched > 0 ? "Dry Run Summary" : "Dry Run Found No Trades"}
            summary={dryRunSummary}
            dryRun
          />
        )}

        {dryRunSummary && dryRunSummary.tradesMatched > 0 && !finalSummary && (
          <section style={confirmationCard}>
            <h3 style={smallTitle}>Confirm permanent delete</h3>
            <p style={mutedText}>Type <strong style={{ color: "#fecaca" }}>DELETE</strong> exactly to enable the final delete.</p>
            <input
              style={input}
              value={confirmation}
              onChange={(event) => setConfirmation(event.target.value)}
              placeholder="DELETE"
              disabled={deleting}
            />
            <div style={modalActions}>
              <button
                type="button"
                onClick={() => void runCleanup(false)}
                disabled={!deleteReady}
                style={{ ...dangerButton, opacity: deleteReady ? 1 : 0.5, cursor: deleteReady ? "pointer" : "not-allowed" }}
              >
                {deleting ? "Deleting..." : "Delete Selected Trades"}
              </button>
            </div>
          </section>
        )}

        {finalSummary && (
          <CleanupSummary title="Final Cleanup Summary" summary={finalSummary} />
        )}
      </section>
    </div>
  );
}

function CleanupSummary({
  title,
  summary,
  dryRun = false,
}: {
  title: string;
  summary: AdminTradeCleanupResponse;
  dryRun?: boolean;
}) {
  const rows = [
    ["Trades matched", summary.tradesMatched],
    [dryRun ? "Trades that would be deleted" : "Trades deleted", dryRun ? summary.tradesMatched : summary.tradesDeleted],
    ["AI trade reviews", summary.tradeReviewsDeleted],
    ["AI trade analyses", summary.tradeAnalysesDeleted],
    ["Rule violations", summary.tradeRuleViolationsDeleted],
    ["Discipline scores", summary.dailyDisciplineScoresDeleted],
    ["Session reviews", summary.sessionReviewsDeleted],
    ["Session caches invalidated", summary.sessionCachesInvalidated],
    ["Leak snapshots", summary.leakSnapshotsDeleted],
    ["Trading DNA snapshots", summary.tradingDnaSnapshotsDeleted],
    ["Daily Coach snapshots", summary.dailyCoachSnapshotsDeleted],
    ["Weekly Coach snapshots", summary.weeklyCoachSnapshotsDeleted],
  ] as const;

  return (
    <section style={summaryCard}>
      <div>
        <h3 style={smallTitle}>{title}</h3>
        <p style={mutedText}>
          User {summary.userId} · Account {summary.accountId} · {summary.startDate} through {summary.endDate}
        </p>
      </div>
      <div style={summaryGrid}>
        {rows.map(([name, value]) => (
          <div key={name} style={summaryItem}>
            <span>{name}</span>
            <strong>{value}</strong>
          </div>
        ))}
      </div>
    </section>
  );
}

function FilterSelect({ labelText, value, onChange, options }: { labelText: string; value: Filter; onChange: (value: Filter) => void; options: Filter[] }) {
  return (
    <div style={{ minWidth: 132 }}>
      <label style={label}>{labelText}</label>
      <select style={input} value={value} onChange={(event) => onChange(event.target.value as Filter)}>
        {options.map((option) => <option key={option} value={option}>{option}</option>)}
      </select>
    </div>
  );
}

function Kpi({ label, value, accent = "#e5eefc" }: { label: string; value: number; accent?: string }) {
  return <article style={{ ...kpiCard, borderColor: `${accent}33` }}><span>{label}</span><strong style={{ color: accent }}>{value}</strong></article>;
}

function Detail({ label, value }: { label: string; value: string | number }) {
  return <div style={detailBox}><span>{label}</span><strong>{value}</strong></div>;
}

function PlanBadge({ plan }: { plan: AdminUserRecord["plan"] }) {
  return <span style={plan === "PRO" ? proBadge : freeBadge}>{plan}</span>;
}

function RoleBadge({ role }: { role: AdminUserRecord["role"] }) {
  return <span style={role === "ADMIN" ? adminBadge : userBadge}>{role}</span>;
}

function VerifiedBadge({ verified }: { verified: boolean }) {
  return <span style={verified ? verifiedBadge : mutedBadge}>{verified ? "Yes" : "No"}</span>;
}

function formatDate(value?: string | null) {
  if (!value) return "Not yet";
  return new Date(value).toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
}

function actionTitle(type: ActionType) {
  return {
    upgrade: "Upgrade to Pro",
    downgrade: "Downgrade to Free",
    "make-admin": "Make Admin",
    "remove-admin": "Remove Admin",
  }[type];
}

const pageWrap: CSSProperties = { display: "grid", gap: 22 };
const kpiGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))", gap: 14 };
const kpiCard: CSSProperties = { ...card, minHeight: 96, display: "grid", gap: 7, alignContent: "center" };
const controlsCard: CSSProperties = { ...card, display: "flex", gap: 14, flexWrap: "wrap", alignItems: "end" };
const contentGrid: CSSProperties = { display: "grid", gridTemplateColumns: "minmax(0, 1.5fr) minmax(340px, 0.8fr)", gap: 18 };
const listPanel: CSSProperties = { ...card, minWidth: 0, overflow: "hidden" };
const detailPanel: CSSProperties = { ...card, minWidth: 0, alignSelf: "start" };
const userTableWrap: CSSProperties = { overflowX: "auto", maxWidth: "100%" };
const userGridTemplate = "minmax(220px,2fr) minmax(150px,1.2fr) 90px 90px 90px 130px 80px 80px minmax(180px,1.2fr)";
const userGridRow: CSSProperties = { minWidth: 1160, display: "grid", gridTemplateColumns: userGridTemplate, gap: 12, alignItems: "center", padding: "12px 14px", borderBottom: "1px solid rgba(148,163,184,0.12)", color: "#cbd5e1" };
const userHeaderRow: CSSProperties = { color: "#94a3b8", fontSize: 11, fontWeight: 950, textTransform: "uppercase", letterSpacing: "0.08em", background: "rgba(15,23,42,0.48)", borderRadius: 14 };
const userRowButton: CSSProperties = { width: "100%", textAlign: "left", border: "1px solid rgba(148,163,184,0.12)", cursor: "pointer" };
const truncateText: CSSProperties = { minWidth: 0, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" };
const mutedCell: CSSProperties = { ...truncateText, color: "#94a3b8", fontSize: 13 };
const badgeBase: CSSProperties = { width: "fit-content", padding: "7px 9px", borderRadius: 999, fontSize: 12, fontWeight: 950 };
const freeBadge: CSSProperties = { ...badgeBase, color: "#bae6fd", background: "rgba(14,165,233,0.12)", border: "1px solid rgba(56,189,248,0.24)" };
const proBadge: CSSProperties = { ...badgeBase, color: "#bbf7d0", background: "rgba(34,197,94,0.14)", border: "1px solid rgba(34,197,94,0.28)" };
const userBadge: CSSProperties = { ...badgeBase, color: "#cbd5e1", background: "rgba(71,85,105,0.22)", border: "1px solid rgba(148,163,184,0.18)" };
const adminBadge: CSSProperties = { ...badgeBase, color: "#ddd6fe", background: "rgba(139,92,246,0.14)", border: "1px solid rgba(139,92,246,0.28)" };
const verifiedBadge: CSSProperties = { ...badgeBase, color: "#bbf7d0", background: "rgba(34,197,94,0.14)", border: "1px solid rgba(34,197,94,0.28)" };
const mutedBadge: CSSProperties = { ...badgeBase, color: "#cbd5e1", background: "rgba(71,85,105,0.22)", border: "1px solid rgba(148,163,184,0.18)" };
const sourceBadge: CSSProperties = { ...badgeBase, color: "#fde68a", background: "rgba(245,158,11,0.14)", border: "1px solid rgba(245,158,11,0.24)" };
const detailStack: CSSProperties = { display: "grid", gap: 18 };
const badgeRow: CSSProperties = { display: "flex", gap: 8, flexWrap: "wrap" };
const detailGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(2, minmax(0,1fr))", gap: 10 };
const detailBox: CSSProperties = { padding: 12, borderRadius: 14, background: "rgba(2,6,23,0.5)", border: "1px solid rgba(148,163,184,0.13)", color: "#94a3b8", display: "grid", gap: 4, wordBreak: "break-word" };
const subCard: CSSProperties = { padding: 16, borderRadius: 18, background: "rgba(15,23,42,0.58)", border: "1px solid rgba(148,163,184,0.13)", display: "grid", gap: 10 };
const smallTitle: CSSProperties = { margin: 0, color: "#f8fafc", fontSize: 16 };
const actionRow: CSSProperties = { display: "flex", gap: 10, flexWrap: "wrap" };
const secondaryButton: CSSProperties = { ...button, width: "auto", background: "rgba(14,165,233,0.2)", border: "1px solid rgba(56,189,248,0.24)", boxShadow: "none" };
const dangerButton: CSSProperties = { ...button, width: "auto", background: "linear-gradient(135deg,#ef4444,#991b1b)", boxShadow: "0 14px 35px rgba(239,68,68,0.2)" };
const actionButton = (disabled: boolean): CSSProperties => ({ ...button, width: "auto", opacity: disabled ? 0.48 : 1, cursor: disabled ? "not-allowed" : "pointer" });
const dangerActionButton = (disabled: boolean): CSSProperties => ({ ...dangerButton, opacity: disabled ? 0.48 : 1, cursor: disabled ? "not-allowed" : "pointer" });
const historyRow: CSSProperties = { padding: 12, borderRadius: 14, background: "rgba(2,6,23,0.45)", border: "1px solid rgba(148,163,184,0.12)", display: "grid", gap: 4, color: "#cbd5e1" };
const emptyState: CSSProperties = { color: "#94a3b8", padding: 16 };
const successBox: CSSProperties = { padding: 14, borderRadius: 14, color: "#bbf7d0", background: "rgba(34,197,94,0.12)", border: "1px solid rgba(34,197,94,0.28)" };
const errorBox: CSSProperties = { padding: 14, borderRadius: 14, color: "#fecaca", background: "rgba(239,68,68,0.12)", border: "1px solid rgba(239,68,68,0.28)" };
const modalOverlay: CSSProperties = { position: "fixed", inset: 0, zIndex: 70, display: "grid", placeItems: "center", padding: 20, background: "rgba(2,6,23,0.78)", backdropFilter: "blur(10px)" };
const modalCard: CSSProperties = { ...card, width: "min(760px,100%)", maxHeight: "calc(100vh - 40px)", overflowY: "auto" };
const modalHeaderRow: CSSProperties = { display: "flex", alignItems: "flex-start", justifyContent: "space-between", gap: 14, marginBottom: 14, flexWrap: "wrap" };
const modalActions: CSSProperties = { display: "flex", justifyContent: "flex-end", gap: 12, marginTop: 18, flexWrap: "wrap" };
const warningBox: CSSProperties = { padding: 14, borderRadius: 14, color: "#fecaca", background: "rgba(239,68,68,0.12)", border: "1px solid rgba(239,68,68,0.28)", lineHeight: 1.5 };
const cleanupUserCard: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(170px, 1fr))", gap: 10, marginTop: 14 };
const cleanupFormGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: 12, marginTop: 14 };
const confirmationCard: CSSProperties = { padding: 16, borderRadius: 18, background: "rgba(127,29,29,0.16)", border: "1px solid rgba(248,113,113,0.22)", display: "grid", gap: 10, marginTop: 14 };
const summaryCard: CSSProperties = { padding: 16, borderRadius: 18, background: "rgba(15,23,42,0.7)", border: "1px solid rgba(45,212,191,0.18)", display: "grid", gap: 12, marginTop: 14 };
const summaryGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))", gap: 10 };
const summaryItem: CSSProperties = { padding: 12, borderRadius: 14, background: "rgba(2,6,23,0.48)", border: "1px solid rgba(148,163,184,0.12)", color: "#94a3b8", display: "grid", gap: 4 };
const paginationRow: CSSProperties = { minWidth: 1160, display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12, padding: "14px", color: "#94a3b8" };
const pagerButton = (disabled: boolean): CSSProperties => ({ ...secondaryButton, opacity: disabled ? 0.45 : 1, cursor: disabled ? "not-allowed" : "pointer" });
