import { lazy, Suspense, useEffect, useRef, useState, type CSSProperties } from "react";
import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
  useLocation,
  useNavigate,
} from "react-router-dom";
import {
  createAccount,
  getAccounts,
  type TradingAccount,
} from "./api/accountApi";
import {
  getCurrentUser,
  getStoredSession,
  logout,
  persistSession,
  type AuthSession,
  type AuthUser,
} from "./api/authApi";
import { deleteAccount } from "./api/accountApi";
import { saveJournal } from "./api/journalApi";
import AccountsSection from "./components/AccountsSection";
import AuthPage from "./components/AuthPage";
import ForgotPasswordPage from "./components/ForgotPasswordPage";
import ResetPasswordPage from "./components/ResetPasswordPage";
import TradeDetailModal from "./components/TradeDetailModal";
import VerifyEmailPage from "./components/VerifyEmailPage";
import { useDashboardData } from "./hooks/useDashboardData";
import { useBillingUsage } from "./hooks/useBillingUsage";
import type { SelectedAccountId } from "./types/dashboard";
import AppShell from "./layout/AppShell";
import {
  page,
  hero,
  eyebrow,
  heroTitle,
  heroText,
  heroPill,
  button,
} from "./styles/dashboardStyles";
import {
  SELECTED_ACCOUNT_STORAGE_KEY,
  clearSelectedAccount,
  parseStoredSelectedAccount,
  persistSelectedAccount,
} from "./utils/accountSelection";

const AdminContactMessagesPage = lazy(() => import("./features/admin/AdminContactMessagesPage"));
const AdminDashboardPage = lazy(() => import("./features/admin/AdminDashboardPage"));
const FeatureUsagePage = lazy(() => import("./features/admin/feature-usage/FeatureUsagePage"));
const ObservabilityPage = lazy(() => import("./features/admin/observability/ObservabilityPage"));
const RetentionAnalyticsPage = lazy(() => import("./features/admin/retention/RetentionAnalyticsPage"));
const AdminUsersPage = lazy(() => import("./features/admin/users/AdminUsersPage"));
const AnalyticsPage = lazy(() => import("./features/analytics/AnalyticsPage"));
const BillingPage = lazy(() => import("./features/billing/BillingPage"));
const CalendarPage = lazy(() => import("./features/calendar/CalendarPage"));
const CoachPage = lazy(() => import("./features/coach/CoachPage"));
const DashboardHome = lazy(() => import("./features/dashboard/DashboardHome"));
const DevToolsPage = lazy(() => import("./features/dev-tools/DevToolsPage"));
const HomePage = lazy(() => import("./features/marketing/HomePage"));
const ContactPage = lazy(() => import("./features/marketing/ContactPage"));
const PrivacyPage = lazy(() => import("./features/marketing/PrivacyPage"));
const TermsPage = lazy(() => import("./features/marketing/TermsPage"));
const SessionReviewPage = lazy(() => import("./features/session-review/SessionReviewPage"));
const TradingDnaPage = lazy(() => import("./features/trading-dna/TradingDnaPage"));

const DEFAULT_IDLE_TIMEOUT_MINUTES = 30;
const DEFAULT_IDLE_WARNING_MINUTES = 2;
const SESSION_EXPIRED_MESSAGE = "Your session expired. Please log in again.";
const INACTIVITY_EXPIRED_MESSAGE = "Your session expired due to inactivity. Please log in again.";
const LOGGED_OUT_MESSAGE = "You have been logged out.";

function envMinutes(name: string, fallback: number) {
  const env = import.meta.env as Record<string, string | undefined>;
  const value = Number(env[name]);
  return Number.isFinite(value) && value > 0 ? value : fallback;
}

type DashboardAppProps = {
  user: AuthUser;
  onLogout: () => void;
};

function DashboardApp({ user, onLogout }: DashboardAppProps) {
  const location = useLocation();
  const isAdmin = user.role === "ADMIN";
  const [accounts, setAccounts] = useState<TradingAccount[]>([]);
  const [selectedAccountId, setSelectedAccountIdState] = useState<SelectedAccountId>(() =>
    parseStoredSelectedAccount(localStorage.getItem(SELECTED_ACCOUNT_STORAGE_KEY))
  );
  const [loadingAccounts, setLoadingAccounts] = useState(true);
  const [showAccountForm, setShowAccountForm] = useState(false);
  const [creatingAccount, setCreatingAccount] = useState(false);
  const [newAccountName, setNewAccountName] = useState("");
  const [newPropFirm, setNewPropFirm] = useState("TOPSTEP");
  const [newAccountNumber, setNewAccountNumber] = useState("");
  const [newAccountType, setNewAccountType] = useState("FUNDED");
  const [newStartingBalance, setNewStartingBalance] = useState("50000");
  const [calendarYear, setCalendarYear] = useState(new Date().getFullYear());
  const [calendarMonth, setCalendarMonth] = useState(new Date().getMonth());
  const [journalDate, setJournalDate] = useState("");
  const [journalMood, setJournalMood] = useState("Focused");
  const [journalSetupQuality, setJournalSetupQuality] = useState("Good");
  const [journalMistakes, setJournalMistakes] = useState("");
  const [journalLesson, setJournalLesson] = useState("");
  const [journalNotes, setJournalNotes] = useState("");
  const [selectedTradeId, setSelectedTradeId] = useState<number | null>(null);
  const billing = useBillingUsage(true);
  const billingResult = new URLSearchParams(location.search).get("billing");

  function setSelectedAccountId(next: SelectedAccountId | ((current: SelectedAccountId) => SelectedAccountId)) {
    setSelectedAccountIdState((current) => {
      const resolved = typeof next === "function" ? next(current) : next;
      persistSelectedAccount(resolved);
      return resolved;
    });
  }

  function resolveValidSelectedAccount(
    current: SelectedAccountId,
    loadedAccounts: TradingAccount[]
  ): SelectedAccountId {
    if (loadedAccounts.length === 0) {
      clearSelectedAccount();
      return "";
    }

    if (current === "ALL") {
      persistSelectedAccount("ALL");
      return "ALL";
    }

    const currentAccountId = typeof current === "number" ? current : Number(current);
    if (Number.isFinite(currentAccountId) && loadedAccounts.some((account) => account.id === currentAccountId)) {
      persistSelectedAccount(currentAccountId);
      return currentAccountId;
    }

    const fallback = loadedAccounts[0].id;
    persistSelectedAccount(fallback);
    return fallback;
  }

  function getValidatedSelectedAccountId(
    current: SelectedAccountId,
    loadedAccounts: TradingAccount[],
    loading: boolean
  ): SelectedAccountId {
    if (loading || loadedAccounts.length === 0) return "";
    if (current === "ALL") return "ALL";

    const currentAccountId = typeof current === "number" ? current : Number(current);
    return Number.isFinite(currentAccountId) && loadedAccounts.some((account) => account.id === currentAccountId)
      ? currentAccountId
      : "";
  }

  const validatedSelectedAccountId = getValidatedSelectedAccountId(
    selectedAccountId,
    accounts,
    loadingAccounts
  );

  useEffect(() => {
    if (loadingAccounts) return;

    const next = resolveValidSelectedAccount(selectedAccountId, accounts);
    if (next !== selectedAccountId) {
      setSelectedAccountId(next);
    }
  }, [loadingAccounts, accounts, selectedAccountId]);

  const {
    fileName,
    parsedImportCount,
    trades,
    dailyPnl,
    parsing,
    saving,
    dashboardStats,
    loadingStats,
    handleCsvUpload,
    handleSaveToDatabase,
    metrics,
  } = useDashboardData({
    authReady: !loadingAccounts && accounts.length > 0,
    selectedAccountId,
    onDefaultCalendarMonthChange: ({ year, month }) => {
      setCalendarYear(year);
      setCalendarMonth(month);
    },
    onTradesSaved: billing.reload,
  });

  async function handleSaveJournal() {
    if (!journalDate) {
      alert("Select a journal date first.");
      return;
    }

    try {
      const payload = {
        accountId: selectedAccountId === "ALL" ? null : Number(selectedAccountId),
        journalDate,
        mood: journalMood,
        setupQuality: journalSetupQuality,
        mistakes: journalMistakes,
        lessonLearned: journalLesson,
        notes: journalNotes,
      };

      await saveJournal(payload);

      alert("Journal saved to database ✅");
    } catch (error) {
      console.error(error);
      alert("Failed to save journal.");
    }
  }

  useEffect(() => {
    async function loadAccounts() {
      setLoadingAccounts(true);
      try {
        const data = await getAccounts();
        const after = resolveValidSelectedAccount(selectedAccountId, data);

        setAccounts(data);
        setSelectedAccountId(after);
      } catch (error) {
        console.error("Failed to load accounts", error);
      } finally {
        setLoadingAccounts(false);
      }
    }

    loadAccounts();
  }, []);

  async function handleCreateAccount() {
    if (!newAccountName.trim()) {
      alert("Account name is required.");
      return;
    }

    setCreatingAccount(true);

    try {
      const created = await createAccount({
        name: newAccountName.trim(),
        propFirm: newPropFirm,
        accountNumber: newAccountNumber.trim() || undefined,
        accountType: newAccountType || undefined,
        startingBalance: newStartingBalance ? Number(newStartingBalance) : undefined,
        status: "ACTIVE",
      });

      setAccounts((prev) => [created, ...prev]);
      setSelectedAccountId(created.id);
      setNewAccountName("");
      setNewPropFirm("TOPSTEP");
      setNewAccountNumber("");
      setNewAccountType("FUNDED");
      setNewStartingBalance("50000");
      setShowAccountForm(false);
      void billing.reload();
    } catch (error) {
      console.error(error);
      alert("Failed to create account.");
    } finally {
      setCreatingAccount(false);
    }
  }

  async function handleDeleteAccount(accountId: number) {
    const confirmDelete = window.confirm(
      "Are you sure? This will delete the account and all its trades."
    );

    if (!confirmDelete) return;

    try {
      await deleteAccount(accountId);

      setAccounts((prev) => prev.filter((a) => a.id !== accountId));

      setSelectedAccountId((current) => {
        const remainingAccounts = accounts.filter((account) => account.id !== accountId);

        if (remainingAccounts.length === 0) {
          clearSelectedAccount();
          return "";
        }

        return resolveValidSelectedAccount(current === accountId ? remainingAccounts[0].id : current, remainingAccounts);
      });

      alert("Account deleted successfully");
    } catch (error) {
      console.error(error);
      alert("Failed to delete account");
    }
  }

  async function reloadWorkspace() {
    const data = await getAccounts();
    setAccounts(data);
    setSelectedAccountId((current) => resolveValidSelectedAccount(current, data));
    await billing.reload();
  }

  function recoverInvalidSelectedAccount() {
    setSelectedAccountId((current) => resolveValidSelectedAccount(current, accounts));
  }

  const userHeader = (
    <div style={{ display: "flex", gap: 12, alignItems: "center", flexWrap: "wrap" }}>
      <a href="/session-review" style={{ ...heroPill, textDecoration: "none" }}>Session Review</a>
      <div style={heroPill}>{accounts.length} Accounts</div>
      <div
        style={{
          ...heroPill,
          background: "rgba(15,23,42,0.72)",
          border: "1px solid rgba(148,163,184,0.24)",
          color: "#cbd5e1",
        }}
      >
        {user.name}
      </div>
      <button
        type="button"
        onClick={onLogout}
        style={{ ...button, width: 120 }}
      >
        Logout
      </button>
    </div>
  );

  if (loadingAccounts) {
    return (
      <div style={page}>
        <section style={hero}>
          <div>
            <div style={eyebrow}>TradePilot AI</div>
            <h1 style={heroTitle}>Prop Firm Performance Dashboard</h1>
            <p style={heroText}>
              Preparing your private workspace.
            </p>
          </div>
          {userHeader}
        </section>

        <AccountsLoadingState />
      </div>
    );
  }

  if (accounts.length === 0 && isAdmin) {
    return (
      <AppShell
        user={user}
        accountCount={0}
        usage={billing.usage}
        onLogout={onLogout}
      >
        <Routes>
          <Route path="/dashboard" element={<AdminDetectedCard />} />
          <Route path="/admin" element={<AdminDashboardPage />} />
          <Route path="/admin/users" element={<AdminUsersPage />} />
          <Route path="/admin/contact-messages" element={<AdminContactMessagesPage />} />
          <Route path="/admin/feature-usage" element={<FeatureUsagePage />} />
          <Route path="/admin/retention" element={<RetentionAnalyticsPage />} />
          <Route path="/admin/observability" element={<ObservabilityPage />} />
          <Route
            path="/billing"
            element={<BillingPage user={user} usage={billing.usage} loading={billing.loading} error={billing.error} />}
          />
          <Route
            path="/dev-tools"
            element={
              import.meta.env.DEV ? (
                <DevToolsPage onChanged={reloadWorkspace} />
              ) : (
                <Navigate to="/admin" replace />
              )
            }
          />
          <Route path="*" element={<Navigate to="/admin" replace />} />
        </Routes>
      </AppShell>
    );
  }

  if (accounts.length === 0 && location.pathname === "/admin") {
    return (
      <div style={page}>
        <NotAuthorized />
      </div>
    );
  }

  if (accounts.length === 0) {
    return (
      <div style={page}>
        <section style={hero}>
          <div>
            <div style={eyebrow}>TradePilot AI</div>
            <h1 style={heroTitle}>Prop Firm Performance Dashboard</h1>
            <p style={heroText}>
              Set up one trading account, then import your Tradovate CSV.
            </p>
          </div>
          {userHeader}
        </section>

        <FirstUserOnboarding onCreateClick={() => setShowAccountForm(true)} />

        {showAccountForm && (
          <AccountCreationModal
            creatingAccount={creatingAccount}
            newAccountName={newAccountName}
            newPropFirm={newPropFirm}
            newAccountNumber={newAccountNumber}
            newAccountType={newAccountType}
            newStartingBalance={newStartingBalance}
            onCreateAccount={handleCreateAccount}
            onClose={() => setShowAccountForm(false)}
            onAccountNameChange={setNewAccountName}
            onPropFirmChange={setNewPropFirm}
            onAccountNumberChange={setNewAccountNumber}
            onAccountTypeChange={setNewAccountType}
            onStartingBalanceChange={setNewStartingBalance}
          />
        )}
      </div>
    );
  }

  return (
    <AppShell
      user={user}
      accountCount={accounts.length}
      usage={billing.usage}
      onLogout={onLogout}
    >
      <Routes>
        <Route
          path="/dashboard"
          element={
            isAdmin ? (
              <Navigate to="/admin" replace />
            ) : (
              <DashboardHome
                accounts={accounts}
                selectedAccountId={selectedAccountId}
                billingUsage={billing.usage}
                billingLoading={billing.loading}
                billingError={billing.error}
                dailyPnl={dailyPnl}
                trades={trades}
                metrics={metrics}
                parsing={parsing}
                saving={saving}
                fileName={fileName}
                parsedImportCount={parsedImportCount}
                billingResult={billingResult}
                onSelectAccount={setSelectedAccountId}
                onCsvUpload={handleCsvUpload}
                onSaveToDatabase={handleSaveToDatabase}
              />
            )
          }
        />
        <Route
          path="/analytics"
          element={
            <AnalyticsPage
              dailyPnl={dailyPnl}
              trades={trades}
              dashboardStats={dashboardStats}
              onTradeSelect={setSelectedTradeId}
            />
          }
        />
        <Route
          path="/calendar"
          element={
            <CalendarPage
              dailyPnl={dailyPnl}
              trades={trades}
              selectedAccountId={selectedAccountId}
              calendarYear={calendarYear}
              calendarMonth={calendarMonth}
              setCalendarYear={setCalendarYear}
              setCalendarMonth={setCalendarMonth}
              journalDate={journalDate}
              setJournalDate={setJournalDate}
              journalMood={journalMood}
              setJournalMood={setJournalMood}
              journalSetupQuality={journalSetupQuality}
              setJournalSetupQuality={setJournalSetupQuality}
              journalMistakes={journalMistakes}
              setJournalMistakes={setJournalMistakes}
              journalLesson={journalLesson}
              setJournalLesson={setJournalLesson}
              journalNotes={journalNotes}
              setJournalNotes={setJournalNotes}
              handleSaveJournal={handleSaveJournal}
              onTradeSelect={setSelectedTradeId}
            />
          }
        />
        <Route
          path="/session-review"
          element={
            <SessionReviewPage
              accounts={accounts}
              selectedAccountId={selectedAccountId}
              trades={trades}
              onTradeSelect={setSelectedTradeId}
            />
          }
        />
        <Route
          path="/sessions/review"
          element={<Navigate to="/session-review" replace />}
        />
        <Route
          path="/trading-dna"
          element={
            <TradingDnaPage
              selectedAccountId={validatedSelectedAccountId}
              accounts={accounts}
              accountsLoading={loadingAccounts}
              onInvalidAccount={recoverInvalidSelectedAccount}
              usage={billing.usage}
              onUsageChanged={billing.reload}
            />
          }
        />
        <Route
          path="/coach"
          element={
            <CoachPage
              selectedAccountId={selectedAccountId}
              usage={billing.usage}
              onUsageChanged={billing.reload}
            />
          }
        />
        <Route
          path="/billing"
          element={
            <>
              <BillingPage user={user} usage={billing.usage} loading={billing.loading} error={billing.error} />
              {!isAdmin && (
                <div style={{ marginTop: 24 }}>
                  <AccountsSection
                    accounts={accounts}
                    selectedAccountId={selectedAccountId}
                    showAccountForm={showAccountForm}
                    creatingAccount={creatingAccount}
                    newAccountName={newAccountName}
                    newPropFirm={newPropFirm}
                    newAccountNumber={newAccountNumber}
                    newAccountType={newAccountType}
                    newStartingBalance={newStartingBalance}
                    onToggleAccountForm={() => setShowAccountForm((prev) => !prev)}
                    onCreateAccount={handleCreateAccount}
                    onDeleteAccount={handleDeleteAccount}
                    onSelectAccount={setSelectedAccountId}
                    onAccountNameChange={setNewAccountName}
                    onPropFirmChange={setNewPropFirm}
                    onAccountNumberChange={setNewAccountNumber}
                    onAccountTypeChange={setNewAccountType}
                    onStartingBalanceChange={setNewStartingBalance}
                  />
                </div>
              )}
            </>
          }
        />
        <Route
          path="/dev-tools"
          element={
            import.meta.env.DEV ? (
              <DevToolsPage onChanged={reloadWorkspace} />
            ) : (
              <Navigate to="/dashboard" replace />
            )
          }
        />
        <Route
          path="/admin"
          element={
            user.role === "ADMIN" ? (
              <AdminDashboardPage />
            ) : (
              <NotAuthorized />
            )
          }
        />
        <Route
          path="/admin/contact-messages"
          element={
            user.role === "ADMIN" ? (
              <AdminContactMessagesPage />
            ) : (
              <NotAuthorized />
            )
          }
        />
        <Route
          path="/admin/users"
          element={
            user.role === "ADMIN" ? (
              <AdminUsersPage />
            ) : (
              <NotAuthorized />
            )
          }
        />
        <Route
          path="/admin/feature-usage"
          element={
            user.role === "ADMIN" ? (
              <FeatureUsagePage />
            ) : (
              <NotAuthorized />
            )
          }
        />
        <Route
          path="/admin/retention"
          element={
            user.role === "ADMIN" ? (
              <RetentionAnalyticsPage />
            ) : (
              <NotAuthorized />
            )
          }
        />
        <Route
          path="/admin/observability"
          element={
            user.role === "ADMIN" ? (
              <ObservabilityPage />
            ) : (
              <NotAuthorized />
            )
          }
        />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>

      {selectedTradeId && (
        <TradeDetailModal
          tradeId={selectedTradeId}
          onClose={() => setSelectedTradeId(null)}
          onSelectTrade={setSelectedTradeId}
          onUsageChanged={billing.reload}
        />
      )}
    </AppShell>
  );
}

function NotAuthorized() {
  return (
    <section style={notAuthorizedCard}>
      <div style={eyebrow}>Not Authorized</div>
      <h1 style={{ ...heroTitle, fontSize: 34, marginBottom: 10 }}>Admin access required</h1>
      <p style={heroText}>This area is only available to TradePilot admins.</p>
      <a href="/dashboard" style={{ ...button, display: "inline-flex", width: "auto", textDecoration: "none", marginTop: 16 }}>
        Back to Dashboard
      </a>
    </section>
  );
}

function AdminDetectedCard() {
  return (
    <section style={adminDetectedCard}>
      <div style={eyebrow}>Admin account detected</div>
      <h1 style={{ ...heroTitle, fontSize: 34, marginBottom: 10 }}>Open the admin dashboard</h1>
      <p style={heroText}>
        This admin workspace does not need a trading account. User management and manual plan controls are available now.
      </p>
      <a href="/admin" style={{ ...button, display: "inline-flex", width: "auto", textDecoration: "none", marginTop: 18 }}>
        Go to Admin Dashboard
      </a>
    </section>
  );
}

type AccountCreationModalProps = {
  creatingAccount: boolean;
  newAccountName: string;
  newPropFirm: string;
  newAccountNumber: string;
  newAccountType: string;
  newStartingBalance: string;
  onCreateAccount: () => void;
  onClose: () => void;
  onAccountNameChange: (value: string) => void;
  onPropFirmChange: (value: string) => void;
  onAccountNumberChange: (value: string) => void;
  onAccountTypeChange: (value: string) => void;
  onStartingBalanceChange: (value: string) => void;
};

function FirstUserOnboarding({ onCreateClick }: { onCreateClick: () => void }) {
  return (
    <main style={onboardingShell}>
      <section style={onboardingCard}>
        <div style={onboardingBadge}>Private trading workspace</div>
        <h2 style={onboardingTitle}>Welcome to TradePilot</h2>
        <p style={onboardingMessage}>
          Create your first trading account to start importing trades and unlocking analytics.
        </p>
        <button
          type="button"
          onClick={onCreateClick}
          style={{ ...button, width: "auto", minWidth: 220 }}
        >
          Create Trading Account
        </button>
        <p style={onboardingSecondary}>
          After creating an account, you can import your Tradovate CSV and review performance,
          mistakes, and AI coaching.
        </p>
      </section>
    </main>
  );
}

function AccountCreationModal({
  creatingAccount,
  newAccountName,
  newPropFirm,
  newAccountNumber,
  newAccountType,
  newStartingBalance,
  onCreateAccount,
  onClose,
  onAccountNameChange,
  onPropFirmChange,
  onAccountNumberChange,
  onAccountTypeChange,
  onStartingBalanceChange,
}: AccountCreationModalProps) {
  return (
    <div style={modalOverlay}>
      <section style={modalCard}>
        <div style={{ display: "flex", justifyContent: "space-between", gap: 16 }}>
          <div>
            <div style={eyebrow}>Account setup</div>
            <h2 style={{ ...heroTitle, fontSize: 28, marginBottom: 10 }}>
              Create Trading Account
            </h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            style={modalCloseButton}
            aria-label="Close account setup"
          >
            ×
          </button>
        </div>

        <div style={modalFormGrid}>
          <div>
            <label style={modalLabel}>Account name</label>
            <input
              style={modalInput}
              placeholder="Topstep 50K"
              value={newAccountName}
              onChange={(event) => onAccountNameChange(event.target.value)}
            />
          </div>

          <div>
            <label style={modalLabel}>Broker / platform optional</label>
            <select
              style={modalInput}
              value={newPropFirm}
              onChange={(event) => onPropFirmChange(event.target.value)}
            >
              <option value="TOPSTEP">Topstep</option>
              <option value="APEX">Apex</option>
              <option value="TAKE_PROFIT_TRADER">Take Profit Trader</option>
              <option value="TRADEIFY">Tradeify</option>
              <option value="OTHER">Tradovate / Other</option>
            </select>
          </div>

          <div>
            <label style={modalLabel}>Account type optional</label>
            <select
              style={modalInput}
              value={newAccountType}
              onChange={(event) => onAccountTypeChange(event.target.value)}
            >
              <option value="">Optional</option>
              <option value="FUNDED">Funded</option>
              <option value="EVALUATION">Evaluation</option>
              <option value="PERSONAL">Personal</option>
            </select>
          </div>

          <div>
            <label style={modalLabel}>Account number optional</label>
            <input
              style={modalInput}
              placeholder="TS-001"
              value={newAccountNumber}
              onChange={(event) => onAccountNumberChange(event.target.value)}
            />
          </div>

          <div>
            <label style={modalLabel}>Starting balance optional</label>
            <input
              style={modalInput}
              type="number"
              placeholder="50000"
              value={newStartingBalance}
              onChange={(event) => onStartingBalanceChange(event.target.value)}
            />
          </div>
        </div>

        <div style={{ display: "flex", justifyContent: "flex-end", gap: 12, marginTop: 24 }}>
          <button type="button" onClick={onClose} style={secondaryButton}>
            Cancel
          </button>
          <button
            type="button"
            onClick={onCreateAccount}
            disabled={creatingAccount || !newAccountName.trim()}
            style={{
              ...button,
              width: 180,
              opacity: creatingAccount || !newAccountName.trim() ? 0.55 : 1,
              cursor: creatingAccount || !newAccountName.trim() ? "not-allowed" : "pointer",
            }}
          >
            {creatingAccount ? "Creating..." : "Create Account"}
          </button>
        </div>
      </section>
    </div>
  );
}

function AccountsLoadingState() {
  return (
    <section style={loadingCard}>
      <div style={spinner} />
      <div>
        <h2 style={{ ...sectionTitleInline, marginBottom: 6 }}>Loading your workspace</h2>
        <p style={{ color: "#94a3b8", margin: 0 }}>
          Checking your accounts before opening the dashboard.
        </p>
      </div>
    </section>
  );
}

const onboardingShell: CSSProperties = {
  minHeight: "calc(100vh - 220px)",
  display: "grid",
  placeItems: "center",
};

const onboardingCard: CSSProperties = {
  width: "min(680px, 100%)",
  textAlign: "center",
  padding: "44px 36px",
  borderRadius: 24,
  border: "1px solid rgba(148,163,184,0.22)",
  background: "linear-gradient(180deg, rgba(15,23,42,0.98), rgba(2,6,23,0.92))",
  boxShadow: "0 28px 90px rgba(0,0,0,0.42)",
};

const onboardingBadge: CSSProperties = {
  display: "inline-flex",
  padding: "8px 12px",
  borderRadius: 999,
  border: "1px solid rgba(52,211,153,0.35)",
  background: "rgba(34,197,94,0.12)",
  color: "#6ee7b7",
  fontSize: 12,
  fontWeight: 900,
  textTransform: "uppercase",
  letterSpacing: "0.08em",
};

const onboardingTitle: CSSProperties = {
  margin: "18px 0 10px",
  color: "#f8fafc",
  fontSize: 42,
  lineHeight: 1.05,
  letterSpacing: "-0.03em",
};

const onboardingMessage: CSSProperties = {
  margin: "0 auto 24px",
  maxWidth: 560,
  color: "#cbd5e1",
  fontSize: 17,
  lineHeight: 1.65,
};

const onboardingSecondary: CSSProperties = {
  margin: "22px auto 0",
  maxWidth: 560,
  color: "#94a3b8",
  fontSize: 14,
  lineHeight: 1.7,
};

const modalOverlay: CSSProperties = {
  position: "fixed",
  inset: 0,
  zIndex: 30,
  display: "grid",
  placeItems: "center",
  padding: 24,
  background: "rgba(2,6,23,0.78)",
  backdropFilter: "blur(12px)",
};

const sessionTimeoutOverlay: CSSProperties = {
  position: "fixed",
  inset: 0,
  zIndex: 80,
  display: "grid",
  placeItems: "center",
  padding: 24,
  background: "rgba(2,6,23,0.74)",
  backdropFilter: "blur(14px)",
};

const sessionTimeoutCard: CSSProperties = {
  width: "min(520px, 100%)",
  padding: 28,
  borderRadius: 24,
  border: "1px solid rgba(56,189,248,0.26)",
  background: "linear-gradient(180deg, rgba(15,23,42,0.98), rgba(2,6,23,0.94))",
  boxShadow: "0 28px 100px rgba(0,0,0,0.52)",
};

const sessionCountdown: CSSProperties = {
  display: "inline-grid",
  placeItems: "center",
  minWidth: 92,
  padding: "10px 14px",
  borderRadius: 14,
  border: "1px solid rgba(251,191,36,0.32)",
  background: "rgba(113,63,18,0.24)",
  color: "#fde68a",
  fontSize: 24,
  fontWeight: 950,
};

const sessionLogoutButton: CSSProperties = {
  width: "auto",
  minWidth: 132,
  padding: "13px 18px",
  borderRadius: 14,
  color: "#fecaca",
  fontWeight: 800,
  cursor: "pointer",
  border: "1px solid rgba(248,113,113,0.32)",
  background: "rgba(127,29,29,0.24)",
};

const modalCard: CSSProperties = {
  width: "min(760px, 100%)",
  padding: 28,
  borderRadius: 24,
  border: "1px solid rgba(148,163,184,0.24)",
  background: "linear-gradient(180deg, rgba(15,23,42,0.99), rgba(15,23,42,0.94))",
  boxShadow: "0 28px 100px rgba(0,0,0,0.5)",
};

const modalFormGrid: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(2, minmax(0, 1fr))",
  gap: 16,
};

const modalLabel: CSSProperties = {
  display: "block",
  marginBottom: 8,
  color: "#94a3b8",
  fontSize: 12,
  fontWeight: 900,
  textTransform: "uppercase",
  letterSpacing: "0.06em",
};

const modalInput: CSSProperties = {
  width: "100%",
  padding: "13px 14px",
  borderRadius: 14,
  border: "1px solid rgba(148,163,184,0.28)",
  background: "rgba(2,6,23,0.72)",
  color: "#f8fafc",
  outline: "none",
  colorScheme: "dark",
};

const modalCloseButton: CSSProperties = {
  width: 38,
  height: 38,
  borderRadius: 10,
  border: "1px solid rgba(148,163,184,0.24)",
  background: "rgba(15,23,42,0.8)",
  color: "#cbd5e1",
  cursor: "pointer",
  fontSize: 24,
  lineHeight: 1,
};

const secondaryButton: CSSProperties = {
  width: 120,
  padding: "13px 18px",
  borderRadius: 14,
  border: "1px solid rgba(148,163,184,0.28)",
  background: "rgba(15,23,42,0.9)",
  color: "#e5e7eb",
  fontWeight: 800,
  cursor: "pointer",
};

const loadingCard: CSSProperties = {
  minHeight: 260,
  borderRadius: 24,
  border: "1px solid rgba(148,163,184,0.18)",
  background: "linear-gradient(180deg, rgba(15,23,42,0.94), rgba(15,23,42,0.72))",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  gap: 16,
};

const spinner: CSSProperties = {
  width: 28,
  height: 28,
  borderRadius: "50%",
  border: "3px solid rgba(148,163,184,0.25)",
  borderTopColor: "#34d399",
};

const sectionTitleInline: CSSProperties = {
  color: "#f8fafc",
  fontSize: 22,
  letterSpacing: "-0.02em",
};

const notAuthorizedCard: CSSProperties = {
  minHeight: 360,
  borderRadius: 24,
  border: "1px solid rgba(239,68,68,0.22)",
  background: "linear-gradient(180deg, rgba(15,23,42,0.96), rgba(2,6,23,0.88))",
  padding: 32,
  display: "grid",
  alignContent: "center",
  justifyItems: "start",
};

const adminDetectedCard: CSSProperties = {
  minHeight: 360,
  borderRadius: 24,
  border: "1px solid rgba(56,189,248,0.24)",
  background: "linear-gradient(180deg, rgba(15,23,42,0.96), rgba(2,6,23,0.88))",
  padding: 32,
  display: "grid",
  alignContent: "center",
  justifyItems: "start",
};

function billingBanner(type: "success" | "cancelled"): CSSProperties {
  const success = type === "success";
  return {
    marginBottom: 24,
    padding: "14px 16px",
    borderRadius: 14,
    border: success
      ? "1px solid rgba(52,211,153,0.35)"
      : "1px solid rgba(251,191,36,0.35)",
    background: success ? "rgba(16,185,129,0.12)" : "rgba(245,158,11,0.11)",
    color: success ? "#bbf7d0" : "#fde68a",
    fontWeight: 800,
  };
}

function AuthRoutes() {
  const navigate = useNavigate();
  const [session, setSession] = useState<AuthSession | null>(null);
  const [authNotice, setAuthNotice] = useState("");
  const [checkingAuth, setCheckingAuth] = useState(true);
  const [showIdleWarning, setShowIdleWarning] = useState(false);
  const [idleCountdownSeconds, setIdleCountdownSeconds] = useState(0);
  const warningTimerRef = useRef<number | null>(null);
  const logoutTimerRef = useRef<number | null>(null);
  const countdownTimerRef = useRef<number | null>(null);
  const idleDeadlineRef = useRef<number | null>(null);
  const showIdleWarningRef = useRef(false);

  const clearIdleTimers = () => {
    if (warningTimerRef.current !== null) {
      window.clearTimeout(warningTimerRef.current);
      warningTimerRef.current = null;
    }
    if (logoutTimerRef.current !== null) {
      window.clearTimeout(logoutTimerRef.current);
      logoutTimerRef.current = null;
    }
    if (countdownTimerRef.current !== null) {
      window.clearInterval(countdownTimerRef.current);
      countdownTimerRef.current = null;
    }
    idleDeadlineRef.current = null;
  };

  function finishSession(message: string) {
    logout();
    clearSelectedAccount();
    setSession(null);
    setAuthNotice(message);
    setShowIdleWarning(false);
    clearIdleTimers();
    navigate("/login", {
      replace: true,
      state: { sessionExpiredMessage: message },
    });
  }

  function expireSession(message = INACTIVITY_EXPIRED_MESSAGE) {
    finishSession(message);
  }

  function logoutNow() {
    finishSession(LOGGED_OUT_MESSAGE);
  }

  function startIdleCountdown() {
    setShowIdleWarning(true);
    updateIdleCountdown();
    countdownTimerRef.current = window.setInterval(updateIdleCountdown, 1000);
  }

  function updateIdleCountdown() {
    const deadline = idleDeadlineRef.current;
    if (!deadline) {
      setIdleCountdownSeconds(0);
      return;
    }
    setIdleCountdownSeconds(Math.max(0, Math.ceil((deadline - Date.now()) / 1000)));
  }

  function resetIdleTimers() {
    if (!session) return;

    const timeoutMinutes = envMinutes("VITE_IDLE_TIMEOUT_MINUTES", DEFAULT_IDLE_TIMEOUT_MINUTES);
    const warningMinutes = envMinutes("VITE_IDLE_WARNING_MINUTES", DEFAULT_IDLE_WARNING_MINUTES);
    const timeoutMs = timeoutMinutes * 60_000;
    const warningMs = Math.max(0, timeoutMs - warningMinutes * 60_000);

    clearIdleTimers();
    setShowIdleWarning(false);
    setIdleCountdownSeconds(Math.ceil((timeoutMs - warningMs) / 1000));
    idleDeadlineRef.current = Date.now() + timeoutMs;

    warningTimerRef.current = window.setTimeout(startIdleCountdown, warningMs);
    logoutTimerRef.current = window.setTimeout(() => {
      window.dispatchEvent(
        new CustomEvent("tradepilot:auth-expired", {
          detail: {
            reason: "idle",
            message: INACTIVITY_EXPIRED_MESSAGE,
          },
        })
      );
    }, timeoutMs);
  }

  useEffect(() => {
    let cancelled = false;

    async function verifyStoredSession() {
      const stored = getStoredSession();

      if (!stored) {
        setCheckingAuth(false);
        return;
      }

      try {
        const user = await getCurrentUser();
        const verifiedSession = { token: stored.token, user };
        persistSession(verifiedSession);

        if (!cancelled) {
          setSession(verifiedSession);
        }
      } catch (error) {
        console.error("Stored auth session is no longer valid", error);
        logout();
        clearSelectedAccount();
        if (!cancelled) {
          setSession(null);
          setAuthNotice(SESSION_EXPIRED_MESSAGE);
          navigate("/login", {
            replace: true,
            state: { sessionExpiredMessage: SESSION_EXPIRED_MESSAGE },
          });
        }
      } finally {
        if (!cancelled) {
          setCheckingAuth(false);
        }
      }
    }

    verifyStoredSession();

    return () => {
      cancelled = true;
    };
  }, [navigate]);

  useEffect(() => {
    function handleExpiredSession(event: Event) {
      const customEvent = event as CustomEvent<{ message?: string }>;
      expireSession(customEvent.detail?.message ?? SESSION_EXPIRED_MESSAGE);
    }

    window.addEventListener("tradepilot:auth-expired", handleExpiredSession);

    return () => {
      window.removeEventListener("tradepilot:auth-expired", handleExpiredSession);
    };
  }, [navigate]);

  useEffect(() => {
    showIdleWarningRef.current = showIdleWarning;
  }, [showIdleWarning]);

  useEffect(() => {
    if (!session) {
      clearIdleTimers();
      setShowIdleWarning(false);
      return;
    }

    const activityEvents = ["mousemove", "keydown", "click", "scroll", "touchstart"];
    const handleActivity = () => {
      if (showIdleWarningRef.current) return;
      resetIdleTimers();
    };

    resetIdleTimers();
    activityEvents.forEach((eventName) => {
      window.addEventListener(eventName, handleActivity, { passive: true });
    });

    return () => {
      activityEvents.forEach((eventName) => {
        window.removeEventListener(eventName, handleActivity);
      });
      clearIdleTimers();
    };
  }, [session]);

  function handleAuthenticated(nextSession: AuthSession) {
    setAuthNotice("");
    setSession(nextSession);
    navigate(nextSession.user.role === "ADMIN" ? "/admin" : "/dashboard", { replace: true });
  }

  function authenticatedPath(nextSession: AuthSession) {
    return nextSession.user.role === "ADMIN" ? "/admin" : "/dashboard";
  }

  function handleLogout() {
    logout();
    clearSelectedAccount();
    setSession(null);
    setAuthNotice("");
    clearIdleTimers();
    setShowIdleWarning(false);
    navigate("/login", { replace: true });
  }

  if (checkingAuth) {
    return (
      <div style={{ ...page, display: "grid", placeItems: "center" }}>
        <div style={{ ...heroPill, borderRadius: 8 }}>Checking session...</div>
      </div>
    );
  }

  return (
    <Routes>
      <Route
        path="/"
        element={
          session ? (
            <Navigate to={authenticatedPath(session)} replace />
          ) : (
            <HomePage />
          )
        }
      />
      <Route
        path="/login"
        element={
          session ? (
            <Navigate to={authenticatedPath(session)} replace />
          ) : (
            <AuthPage onAuthenticated={handleAuthenticated} sessionMessage={authNotice} />
          )
        }
      />
      <Route
        path="/register"
        element={
          session ? (
            <Navigate to={authenticatedPath(session)} replace />
          ) : (
            <AuthPage onAuthenticated={handleAuthenticated} initialMode="register" />
          )
        }
      />
      <Route path="/privacy" element={<PrivacyPage />} />
      <Route path="/terms" element={<TermsPage />} />
      <Route path="/contact" element={<ContactPage />} />
      <Route
        path="/verify-email"
        element={
          session ? <Navigate to="/dashboard" replace /> : <VerifyEmailPage />
        }
      />
      <Route
        path="/forgot-password"
        element={
          session ? <Navigate to="/dashboard" replace /> : <ForgotPasswordPage />
        }
      />
      <Route
        path="/reset-password"
        element={
          session ? <Navigate to="/dashboard" replace /> : <ResetPasswordPage />
        }
      />
      <Route
        path="/*"
        element={
          session ? (
            <>
              <DashboardApp user={session.user} onLogout={handleLogout} />
      {showIdleWarning && (
                <IdleWarningModal
                  countdownSeconds={idleCountdownSeconds}
                  onStaySignedIn={resetIdleTimers}
                  onLogoutNow={logoutNow}
                />
              )}
            </>
          ) : (
            <Navigate
              to="/login"
              replace
              state={authNotice ? { sessionExpiredMessage: authNotice } : undefined}
            />
          )
        }
      />
    </Routes>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<RouteLoadingState />}>
        <AuthRoutes />
      </Suspense>
    </BrowserRouter>
  );
}

function RouteLoadingState() {
  return (
    <div style={{ ...page, display: "grid", placeItems: "center" }}>
      <div style={{ ...heroPill, borderRadius: 8 }}>Loading TradePilot...</div>
    </div>
  );
}

function IdleWarningModal({
  countdownSeconds,
  onStaySignedIn,
  onLogoutNow,
}: {
  countdownSeconds: number;
  onStaySignedIn: () => void;
  onLogoutNow: () => void;
}) {
  const minutes = Math.floor(countdownSeconds / 60);
  const seconds = countdownSeconds % 60;
  const countdownLabel = `${minutes}:${String(seconds).padStart(2, "0")}`;

  return (
    <div style={sessionTimeoutOverlay}>
      <section style={sessionTimeoutCard} role="dialog" aria-modal="true" aria-labelledby="idle-warning-title">
        <div style={eyebrow}>Session timeout</div>
        <h2 id="idle-warning-title" style={{ ...heroTitle, fontSize: 28, marginBottom: 10 }}>
          You will be logged out soon due to inactivity.
        </h2>
        <p style={{ ...heroText, marginBottom: 22 }}>
          Stay signed in to keep working in TradePilot.
        </p>
        <div style={sessionCountdown} aria-live="polite">
          {countdownLabel}
        </div>
        <div style={{ display: "flex", gap: 12, flexWrap: "wrap", marginTop: 22 }}>
          <button type="button" onClick={onStaySignedIn} style={{ ...button, width: "auto", minWidth: 170 }}>
            Stay signed in
          </button>
          <button type="button" onClick={onLogoutNow} style={sessionLogoutButton}>
            Logout now
          </button>
        </div>
      </section>
    </div>
  );
}

export default App;
