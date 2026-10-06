import { lazy, Suspense, useEffect, useRef, useState, type CSSProperties, type ReactNode } from "react";
import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
  useLocation,
  useNavigate,
  useParams,
} from "react-router-dom";
import {
  createAccount,
  getAccounts,
  updateAccountRiskAmount,
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
import { getFeatureAvailability } from "./api/featureAvailabilityApi";
import { getPreAnalysisAccess } from "./api/preAnalysisApi";
import { deleteAccount } from "./api/accountApi";
import { saveJournal } from "./api/journalApi";
import AccountsSection from "./components/AccountsSection";
import AuthPage from "./components/AuthPage";
import ForgotPasswordPage from "./components/ForgotPasswordPage";
import ResetPasswordPage from "./components/ResetPasswordPage";
import TradeDetailModal from "./components/TradeDetailModal";
import VerifyEmailPage from "./components/VerifyEmailPage";
import LockedFeaturePage, { type LockedFeaturePreview } from "./components/LockedFeaturePage";
import { useDashboardData } from "./hooks/useDashboardData";
import { useBillingUsage } from "./hooks/useBillingUsage";
import { useActivationStatus } from "./hooks/useActivationStatus";
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
import { creditsRemaining, hasFeature, type Feature } from "./utils/featureAccess";
import { isFeatureEnabled, type FeatureAvailability, type ReleaseFeatureKey } from "./config/featureFlags";
import { SeoLandingPage, seoLandingPages } from "./features/marketing/SeoLandingPage";
import GuidePage from "./features/marketing/GuidePage";
import { guides } from "./features/marketing/guideContent";
import ResourcesPage from "./features/marketing/ResourcesPage";
import {
  ACTIVATION_IMPORT_DESTINATION,
  activationSourceFromSearch,
  clearPendingActivationSource,
  readPendingActivationSource,
} from "./utils/activationRedirect";

const disabledFeatureAvailability: FeatureAvailability = {
  v2FeaturesEnabled: false,
  v3FeaturesEnabled: false,
  features: {
    playbook: false,
    tradingDna: false,
    journey: false,
    tradingMemory: false,
    tradeReplay: false,
    coach: true,
  },
};

const AdminContactMessagesPage = lazy(() => import("./features/admin/AdminContactMessagesPage"));
const AdminDashboardPage = lazy(() => import("./features/admin/AdminDashboardPage"));
const FeatureFunnelsPage = lazy(() => import("./features/admin/feature-funnels/FeatureFunnelsPage"));
const FeatureUsagePage = lazy(() => import("./features/admin/feature-usage/FeatureUsagePage"));
const ObservabilityPage = lazy(() => import("./features/admin/observability/ObservabilityPage"));
const RetentionAnalyticsPage = lazy(() => import("./features/admin/retention/RetentionAnalyticsPage"));
const SeoAnalyticsPage = lazy(() => import("./features/admin/seo/SeoAnalyticsPage"));
const AdminUsersPage = lazy(() => import("./features/admin/users/AdminUsersPage"));
const AnalyticsPage = lazy(() => import("./features/analytics/AnalyticsPage"));
const BillingPage = lazy(() => import("./features/billing/BillingPage"));
const CalendarPage = lazy(() => import("./features/calendar/CalendarPage"));
const CoachPage = lazy(() => import("./features/coach/CoachPage"));
const DashboardHome = lazy(() => import("./features/dashboard/DashboardHome"));
const DevToolsPage = lazy(() => import("./features/dev-tools/DevToolsPage"));
const LeakDetectorPage = lazy(() => import("./features/leak-detector/LeakDetectorPage"));
const JourneyPage = lazy(() => import("./features/performance-timeline/JourneyPage"));
const PlaybookPage = lazy(() => import("./features/playbook/PlaybookPage"));
const PlaybookFormPage = lazy(() => import("./features/playbook/PlaybookFormPage"));
const PlaybookDetailPage = lazy(() => import("./features/playbook/PlaybookDetailPage"));
const PreAnalysisPage = lazy(() => import("./features/pre-analysis/PreAnalysisPage"));
const HomePage = lazy(() => import("./features/marketing/HomePage"));
const SampleDemoPage = lazy(() => import("./features/marketing/SampleDemoPage"));
const HowTradePilotWorksPage = lazy(() => import("./features/marketing/HowTradePilotWorksPage"));
const ContactPage = lazy(() => import("./features/marketing/ContactPage"));
const PrivacyPage = lazy(() => import("./features/marketing/PrivacyPage"));
const TermsPage = lazy(() => import("./features/marketing/TermsPage"));
const NotFoundPage = lazy(() => import("./features/marketing/NotFoundPage"));
const TradingExpectancyCalculatorPage = lazy(() => import("./features/marketing/TradingExpectancyCalculatorPage"));
const OvertradingCalculatorPage = lazy(() => import("./features/marketing/OvertradingCalculatorPage"));
const RevengeTradingCalculatorPage = lazy(() => import("./features/marketing/RevengeTradingCalculatorPage"));
const SessionReviewPage = lazy(() => import("./features/session-review/SessionReviewPage"));
const TradingDnaPage = lazy(() => import("./features/trading-dna/TradingDnaPage"));

type LockedFeatureContent = {
  eyebrow: string;
  title: string;
  subtitle: string;
  features: LockedFeaturePreview[];
  ctaTitle: string;
  ctaText: string;
};

const lockedFeatureContent: Record<"ADVANCED_ANALYTICS" | "SESSION_REVIEW" | "TRADING_DNA" | "AI_LEAK_DETECTOR" | "PERFORMANCE_TIMELINE" | "PLAYBOOK" | "DAILY_COACH", LockedFeatureContent> = {
  ADVANCED_ANALYTICS: {
    eyebrow: "Advanced Analytics",
    title: "Unlock the data behind your best trades.",
    subtitle:
      "Discover your highest-performing setups, avoid costly habits, and get AI-powered insights that help you improve faster.",
    features: [
      {
        title: "Session Heatmap",
        icon: "◷",
        description: "Discover which times of day produce your best trades.",
      },
      {
        title: "Setup Performance",
        icon: "R",
        description: "Measure win rate, expectancy, and average R by strategy.",
      },
      {
        title: "Behavioral Insights",
        icon: "!",
        description: "Detect revenge trading, hesitation, overtrading, and broken rules.",
      },
      {
        title: "Friday / OPEX Analysis",
        icon: "F",
        description: "Identify difficult market conditions and avoid low-probability trades.",
      },
    ],
    ctaTitle: "Your journal records trades. TradePilot Pro turns them into improvement.",
    ctaText:
      "Pro includes Advanced Analytics, Daily Coach, Weekly Coach, Trading DNA, Session Review, and 100 AI credits/month.",
  },
  SESSION_REVIEW: {
    eyebrow: "Session Review",
    title: "Unlock the data behind your best trades.",
    subtitle:
      "Discover your highest-performing setups, avoid costly habits, and get AI-powered insights that help you improve faster.",
    features: [
      {
        title: "AI Coach Notes",
        icon: "AI",
        description: "Get a concise read on what drove the session and where discipline slipped.",
      },
      {
        title: "Rule for Tomorrow",
        icon: "→",
        description: "Leave every session with one practical guardrail for the next trading day.",
      },
      {
        title: "Mistake Patterns",
        icon: "!",
        description: "Spot repeated errors across entries, exits, timing, and risk decisions.",
      },
      {
        title: "Replay Insights",
        icon: "↺",
        description: "Review best and worst moments without digging through raw trades.",
      },
    ],
    ctaTitle: "Your journal records trades. TradePilot Pro turns them into improvement.",
    ctaText:
      "Pro adds Session Review, Daily Coach, Weekly Coach, Trading DNA, Advanced Analytics, and 100 AI credits/month.",
  },
  TRADING_DNA: {
    eyebrow: "Trading DNA",
    title: "Unlock the data behind your best trades.",
    subtitle:
      "Discover your highest-performing setups, avoid costly habits, and get AI-powered insights that help you improve faster.",
    features: [
      {
        title: "Strength Profile",
        icon: "S",
        description: "Identify which setups, sessions, and conditions consistently suit you.",
      },
      {
        title: "Risk Fingerprint",
        icon: "R",
        description: "Understand how sizing, timing, and drawdown behavior affect outcomes.",
      },
      {
        title: "Emotion Signals",
        icon: "E",
        description: "Use journal metadata to connect confidence and emotion to performance.",
      },
      {
        title: "Action Plan",
        icon: "✓",
        description: "Convert your data into focused adjustments for the next review cycle.",
      },
    ],
    ctaTitle: "Your journal records trades. TradePilot Pro turns them into improvement.",
    ctaText:
      "Pro unlocks Trading DNA plus coaching, Session Review, Advanced Analytics, and 100 AI credits/month.",
  },
  AI_LEAK_DETECTOR: {
    eyebrow: "AI Leak Detector",
    title: "Unlock the data behind your best trades.",
    subtitle:
      "Find the costly habits, weak time windows, and behavior patterns quietly draining your trading account.",
    features: [
      {
        title: "PnL Leak Impact",
        icon: "R",
        description: "See total R, average R, and estimated monthly impact for each leak.",
      },
      {
        title: "Confidence Scores",
        icon: "%",
        description: "Separate strong signals from patterns that still need more sample size.",
      },
      {
        title: "Behavioral Fixes",
        icon: "✓",
        description: "Turn detected leaks into direct rules for your next session.",
      },
      {
        title: "Related Trades",
        icon: "↗",
        description: "Jump back to the trades behind each leak and review the context.",
      },
    ],
    ctaTitle: "Your journal records trades. TradePilot Pro turns them into improvement.",
    ctaText:
      "Pro unlocks AI Leak Detector, Trading DNA, Daily Coach, Weekly Coach, Session Review, Advanced Analytics, and 100 AI credits/month.",
  },
  PERFORMANCE_TIMELINE: {
    eyebrow: "AI Performance Timeline",
    title: "Unlock the story behind your progress.",
    subtitle:
      "See how your discipline, risk management, execution, psychology, and consistency evolve month by month.",
    features: [
      {
        title: "Evolution Score",
        icon: "↗",
        description: "Track whether your process is actually improving over time.",
      },
      {
        title: "Monthly Chapters",
        icon: "M",
        description: "Turn each eligible month into a concise coaching narrative.",
      },
      {
        title: "Behavior Milestones",
        icon: "✓",
        description: "Celebrate process milestones without confusing inactivity for progress.",
      },
      {
        title: "Next Focus",
        icon: "→",
        description: "Leave each month with one focused adjustment for the next period.",
      },
    ],
    ctaTitle: "Your journal records trades. TradePilot Pro shows whether you are improving.",
    ctaText:
      "Pro unlocks Journey, AI Leak Detector, Trading DNA, Daily Coach, Weekly Coach, Session Review, Advanced Analytics, and 100 AI credits/month.",
  },
  PLAYBOOK: {
    eyebrow: "AI Trading Playbook",
    title: "Turn repeatable setups into a measurable process.",
    subtitle:
      "Document setup rules, link historical trades, and compare your playbook against actual execution behavior.",
    features: [
      {
        title: "Structured Rules",
        icon: "B",
        description: "Define context, entry, invalidation, risk, management, and review rules.",
      },
      {
        title: "Playbook vs. Actual",
        icon: "↔",
        description: "Compare compliant and noncompliant trades using calculated metrics.",
      },
      {
        title: "Health Score",
        icon: "%",
        description: "Measure definition quality, evidence, adherence, consistency, and relevance.",
      },
      {
        title: "AI Drafts",
        icon: "AI",
        description: "Turn a strategy description into a draft setup you review before saving.",
      },
    ],
    ctaTitle: "Build your trading playbook.",
    ctaText:
      "Pro unlocks Playbook, Journey, AI Leak Detector, Trading DNA, Daily Coach, Weekly Coach, and 100 AI credits/month.",
  },
  DAILY_COACH: {
    eyebrow: "AI Coach",
    title: "Unlock the data behind your best trades.",
    subtitle:
      "Discover your highest-performing setups, avoid costly habits, and get AI-powered insights that help you improve faster.",
    features: [
      {
        title: "Daily Coach",
        icon: "D",
        description: "Summarize the day, surface discipline issues, and capture what to repeat.",
      },
      {
        title: "Weekly Coach",
        icon: "W",
        description: "Review your week by behavior, setup quality, risk, and process consistency.",
      },
      {
        title: "Priority AI Credits",
        icon: "AI",
        description: "Use 100 monthly AI credits across reviews, coaching, DNA, and sessions.",
      },
      {
        title: "Progress Memory",
        icon: "↗",
        description: "Build a coaching trail that shows how your trading habits are changing.",
      },
    ],
    ctaTitle: "Your journal records trades. TradePilot Pro turns them into improvement.",
    ctaText:
      "Pro includes Daily Coach, Weekly Coach, Trading DNA, Session Review, Advanced Analytics, and 100 AI credits/month.",
  },
};

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
  onUserRefresh: () => Promise<void>;
};

function DashboardApp({ user, onLogout, onUserRefresh }: DashboardAppProps) {
  const location = useLocation();
  const navigate = useNavigate();
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
  const [newDefaultRiskAmount, setNewDefaultRiskAmount] = useState("");
  const [calendarYear, setCalendarYear] = useState(new Date().getFullYear());
  const [calendarMonth, setCalendarMonth] = useState(new Date().getMonth());
  const [journalDate, setJournalDate] = useState("");
  const [journalMood, setJournalMood] = useState("Focused");
  const [journalSetupQuality, setJournalSetupQuality] = useState("Good");
  const [journalMistakes, setJournalMistakes] = useState("");
  const [journalLesson, setJournalLesson] = useState("");
  const [journalNotes, setJournalNotes] = useState("");
  const [selectedTradeId, setSelectedTradeId] = useState<number | null>(null);
  const [featureAvailability, setFeatureAvailability] = useState<FeatureAvailability>(disabledFeatureAvailability);
  const [preAnalysisAllowed, setPreAnalysisAllowed] = useState(false);
  const preAnalysisEnabled = featureAvailability.v2FeaturesEnabled && preAnalysisAllowed;
  const billing = useBillingUsage(true);
  const activation = useActivationStatus(!isAdmin);
  const billingResult = new URLSearchParams(location.search).get("billing");
  const creditCheckoutResult = new URLSearchParams(location.search).get("credits");
  const canAccess = (feature: Feature) => hasFeature(feature, user, billing.usage);
  const canAccessReleasedFeature = (feature: Feature, releaseFeature: ReleaseFeatureKey) =>
    isFeatureEnabled(releaseFeature, featureAvailability) && canAccess(feature);

  function featureRoute(feature: ReleaseFeatureKey, element: ReactNode) {
    return isFeatureEnabled(feature, featureAvailability) ? element : <Navigate to="/dashboard" replace />;
  }

  useEffect(() => {
    let active = true;
    getFeatureAvailability()
      .then((availability) => {
        if (active) {
          setFeatureAvailability(availability);
        }
      })
      .catch(() => {
        if (active) {
          setFeatureAvailability(disabledFeatureAvailability);
        }
      });
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;
    getPreAnalysisAccess()
      .then((access) => {
        if (active) setPreAnalysisAllowed(access.allowed);
      })
      .catch(() => {
        if (active) setPreAnalysisAllowed(false);
      });
    return () => {
      active = false;
    };
  }, []);

  function renderLockedFeature(feature: keyof typeof lockedFeatureContent) {
    const content = lockedFeatureContent[feature];
    return (
      <LockedFeaturePage
        {...content}
        creditsRemaining={billing.usage ? creditsRemaining(billing.usage) : undefined}
        creditsLimit={billing.usage?.limits?.AI_CREDITS ?? 10}
        resetDate={billing.usage?.resetDate}
        onUpgrade={() => navigate("/billing")}
      />
    );
  }

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

  useEffect(() => {
    if (location.pathname !== "/billing" || !creditCheckoutResult) return;

    if (creditCheckoutResult === "success") {
      void Promise.all([billing.reload(), onUserRefresh()]).finally(() => {
        navigate("/billing", { replace: true, state: { creditCheckoutResult: "success" } });
      });
      return;
    }

    if (creditCheckoutResult === "cancelled") {
      navigate("/billing", { replace: true, state: { creditCheckoutResult: "cancelled" } });
    }
  }, [location.pathname, creditCheckoutResult, navigate, onUserRefresh]);

  const {
    fileName,
    parsedImportCount,
    trades,
    dailyPnl,
    parsing,
    saving,
    importPreview,
    importFeedback,
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
    onTradesSaved: () => {
      void billing.reload();
      void activation.reload();
    },
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
        defaultRiskAmount: newDefaultRiskAmount ? Number(newDefaultRiskAmount) : undefined,
        status: "ACTIVE",
      });
      const refreshedAccounts = await getAccounts();

      setAccounts(refreshedAccounts);
      setSelectedAccountId(created.id);
      setNewAccountName("");
      setNewPropFirm("TOPSTEP");
      setNewAccountNumber("");
      setNewAccountType("FUNDED");
      setNewStartingBalance("50000");
      setNewDefaultRiskAmount("");
      setShowAccountForm(false);
      void billing.reload();
      void activation.reload();
      alert("Account created. You can import CSV into it now.");
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

  async function handleUpdateAccountRisk(accountId: number, defaultRiskAmount: number) {
    try {
      const updated = await updateAccountRiskAmount(accountId, defaultRiskAmount);
      setAccounts((prev) => prev.map((account) => account.id === accountId ? updated : account));
    } catch (error) {
      console.error(error);
      throw error;
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
        preAnalysisAllowed={preAnalysisEnabled}
        onLogout={onLogout}
      >
        <Routes>
          <Route path="/dashboard" element={<AdminDetectedCard />} />
          <Route path="/admin" element={<AdminDashboardPage />} />
          <Route path="/admin/users" element={<AdminUsersPage />} />
          <Route path="/admin/contact-messages" element={<AdminContactMessagesPage />} />
          <Route path="/admin/feature-usage" element={<FeatureUsagePage />} />
          <Route path="/admin/feature-funnels" element={<FeatureFunnelsPage />} />
          <Route path="/admin/feature-funnels/:funnelKey" element={<FeatureFunnelsPage />} />
          <Route path="/admin/analytics/seo" element={<SeoAnalyticsPage />} />
          <Route path="/admin/retention" element={<RetentionAnalyticsPage />} />
          <Route path="/admin/observability" element={<ObservabilityPage />} />
          <Route
            path="/billing"
            element={
              <BillingPage
                user={user}
                usage={billing.usage}
                loading={billing.loading}
                error={billing.error}
                featureAvailability={featureAvailability}
              />
            }
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
              Set up one trading account, then import your Tradovate or Topstep CSV.
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
            newDefaultRiskAmount={newDefaultRiskAmount}
            onCreateAccount={handleCreateAccount}
            onClose={() => setShowAccountForm(false)}
            onAccountNameChange={setNewAccountName}
            onPropFirmChange={setNewPropFirm}
            onAccountNumberChange={setNewAccountNumber}
            onAccountTypeChange={setNewAccountType}
            onStartingBalanceChange={setNewStartingBalance}
            onDefaultRiskAmountChange={setNewDefaultRiskAmount}
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
      featureAvailability={featureAvailability}
      preAnalysisAllowed={preAnalysisEnabled}
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
                importPreview={importPreview}
                importFeedback={importFeedback}
                activationStatus={activation.status}
                showAccountForm={showAccountForm}
                creatingAccount={creatingAccount}
                newAccountName={newAccountName}
                newPropFirm={newPropFirm}
                newAccountType={newAccountType}
                newStartingBalance={newStartingBalance}
                newDefaultRiskAmount={newDefaultRiskAmount}
                billingResult={billingResult}
                onSelectAccount={setSelectedAccountId}
                onToggleAccountForm={() => setShowAccountForm((prev) => !prev)}
                onCreateAccount={handleCreateAccount}
                onAccountNameChange={setNewAccountName}
                onPropFirmChange={setNewPropFirm}
                onAccountTypeChange={setNewAccountType}
                onStartingBalanceChange={setNewStartingBalance}
                onDefaultRiskAmountChange={setNewDefaultRiskAmount}
                onCsvUpload={handleCsvUpload}
                onSaveToDatabase={handleSaveToDatabase}
                onTradeSelect={setSelectedTradeId}
                showTradingDnaEntryPoint={isFeatureEnabled("TRADING_DNA", featureAvailability)}
                v2FeaturesEnabled={featureAvailability.v2FeaturesEnabled}
              />
            )
          }
        />
        <Route
          path="/analytics"
          element={
            canAccess("ADVANCED_ANALYTICS") ? (
              <AnalyticsPage
                dailyPnl={dailyPnl}
                trades={trades}
                dashboardStats={dashboardStats}
                onTradeSelect={setSelectedTradeId}
              />
            ) : (
              renderLockedFeature("ADVANCED_ANALYTICS")
            )
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
            canAccess("SESSION_REVIEW") ? (
              <SessionReviewPage
                accounts={accounts}
                selectedAccountId={selectedAccountId}
                trades={trades}
                onTradeSelect={setSelectedTradeId}
                v2FeaturesEnabled={featureAvailability.v2FeaturesEnabled}
              />
            ) : (
              renderLockedFeature("SESSION_REVIEW")
            )
          }
        />
        <Route
          path="/sessions/review"
          element={<Navigate to="/session-review" replace />}
        />
        <Route
          path="/trading-dna"
          element={featureRoute(
            "TRADING_DNA",
            canAccessReleasedFeature("TRADING_DNA", "TRADING_DNA") ? (
                <TradingDnaPage
                  selectedAccountId={validatedSelectedAccountId}
                  accounts={accounts}
                  accountsLoading={loadingAccounts}
                  onInvalidAccount={recoverInvalidSelectedAccount}
                  usage={billing.usage}
                  onUsageChanged={billing.reload}
                />
              ) : (
                renderLockedFeature("TRADING_DNA")
              )
          )}
        />
        <Route
          path="/leak-detector"
          element={
            canAccess("AI_LEAK_DETECTOR") ? (
              <LeakDetectorPage
                accounts={accounts}
                selectedAccountId={selectedAccountId}
                usage={billing.usage}
                onSelectAccount={setSelectedAccountId}
                onUsageChanged={billing.reload}
                onTradeSelect={setSelectedTradeId}
              />
            ) : (
              renderLockedFeature("AI_LEAK_DETECTOR")
            )
          }
        />
        <Route
          path="/journey"
          element={featureRoute(
            "JOURNEY",
            canAccessReleasedFeature("PERFORMANCE_TIMELINE", "JOURNEY") ? (
                <JourneyPage
                  accounts={accounts}
                  selectedAccountId={selectedAccountId}
                  onSelectAccount={setSelectedAccountId}
                  onUsageChanged={billing.reload}
                />
              ) : (
                renderLockedFeature("PERFORMANCE_TIMELINE")
              )
          )}
        />
        <Route
          path="/playbook"
          element={featureRoute(
            "PLAYBOOK",
            canAccessReleasedFeature("PLAYBOOK", "PLAYBOOK") ? (
                <PlaybookPage
                  accounts={accounts}
                  selectedAccountId={selectedAccountId}
                  onSelectAccount={setSelectedAccountId}
                />
              ) : (
                renderLockedFeature("PLAYBOOK")
              )
          )}
        />
        <Route
          path="/playbook/new"
          element={featureRoute(
            "PLAYBOOK",
            canAccessReleasedFeature("PLAYBOOK", "PLAYBOOK") ? (
                <PlaybookFormPage
                  accounts={accounts}
                  selectedAccountId={selectedAccountId}
                  onUsageChanged={billing.reload}
                />
              ) : (
                renderLockedFeature("PLAYBOOK")
              )
          )}
        />
        <Route
          path="/playbook/:setupId"
          element={featureRoute(
            "PLAYBOOK",
            canAccessReleasedFeature("PLAYBOOK", "PLAYBOOK") ? (
                <PlaybookDetailPage onUsageChanged={billing.reload} />
              ) : (
                renderLockedFeature("PLAYBOOK")
              )
          )}
        />
        <Route
          path="/playbook/:setupId/edit"
          element={featureRoute(
            "PLAYBOOK",
            canAccessReleasedFeature("PLAYBOOK", "PLAYBOOK") ? (
                <PlaybookFormPage
                  accounts={accounts}
                  selectedAccountId={selectedAccountId}
                  onUsageChanged={billing.reload}
                />
              ) : (
                renderLockedFeature("PLAYBOOK")
              )
          )}
        />
        <Route
          path="/coach"
          element={
            canAccess("DAILY_COACH") || activation.status?.proPreview?.available ? (
              <CoachPage
                selectedAccountId={selectedAccountId}
                usage={billing.usage}
                activationStatus={activation.status}
                onUsageChanged={billing.reload}
                v2FeaturesEnabled={featureAvailability.v2FeaturesEnabled}
              />
            ) : (
              renderLockedFeature("DAILY_COACH")
            )
          }
        />
        <Route
          path="/coach/pre-analysis"
          element={preAnalysisEnabled ? <PreAnalysisPage /> : <Navigate to="/dashboard" replace />}
        />
        <Route
          path="/billing"
          element={
            <>
              <BillingPage
                user={user}
                usage={billing.usage}
                loading={billing.loading}
                error={billing.error}
                featureAvailability={featureAvailability}
              />
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
                    newDefaultRiskAmount={newDefaultRiskAmount}
                    onToggleAccountForm={() => setShowAccountForm((prev) => !prev)}
                    onCreateAccount={handleCreateAccount}
                    onDeleteAccount={handleDeleteAccount}
                    onUpdateAccountRisk={handleUpdateAccountRisk}
                    onSelectAccount={setSelectedAccountId}
                    onAccountNameChange={setNewAccountName}
                    onPropFirmChange={setNewPropFirm}
                    onAccountNumberChange={setNewAccountNumber}
                    onAccountTypeChange={setNewAccountType}
                    onStartingBalanceChange={setNewStartingBalance}
                    onDefaultRiskAmountChange={setNewDefaultRiskAmount}
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
          path="/admin/feature-funnels"
          element={
            user.role === "ADMIN" ? (
              <FeatureFunnelsPage />
            ) : (
              <NotAuthorized />
            )
          }
        />
        <Route
          path="/admin/feature-funnels/:funnelKey"
          element={
            user.role === "ADMIN" ? (
              <FeatureFunnelsPage />
            ) : (
              <NotAuthorized />
            )
          }
        />
        <Route
          path="/admin/analytics/seo"
          element={
            user.role === "ADMIN" ? (
              <SeoAnalyticsPage />
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
          path="/trades/:tradeId"
          element={<TradeDetailRoute onOpenTrade={setSelectedTradeId} />}
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
          tradingMemoryEnabled={isFeatureEnabled("TRADING_MEMORY", featureAvailability)}
          tradeReplayEnabled={isFeatureEnabled("TRADE_REPLAY", featureAvailability)}
        />
      )}
    </AppShell>
  );
}

function TradeDetailRoute({ onOpenTrade }: { onOpenTrade: (tradeId: number) => void }) {
  const { tradeId } = useParams();

  useEffect(() => {
    const parsedTradeId = Number(tradeId);
    if (Number.isFinite(parsedTradeId) && parsedTradeId > 0) {
      onOpenTrade(parsedTradeId);
    }
  }, [tradeId, onOpenTrade]);

  return null;
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
  newDefaultRiskAmount: string;
  onCreateAccount: () => void;
  onClose: () => void;
  onAccountNameChange: (value: string) => void;
  onPropFirmChange: (value: string) => void;
  onAccountNumberChange: (value: string) => void;
  onAccountTypeChange: (value: string) => void;
  onStartingBalanceChange: (value: string) => void;
  onDefaultRiskAmountChange: (value: string) => void;
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
          After creating an account, you can import your Tradovate or Topstep CSV and review performance,
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
  newDefaultRiskAmount,
  onCreateAccount,
  onClose,
  onAccountNameChange,
  onPropFirmChange,
  onAccountNumberChange,
  onAccountTypeChange,
  onStartingBalanceChange,
  onDefaultRiskAmountChange,
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

          <div>
            <label style={modalLabel}>Default Risk Per Trade / 1R optional</label>
            <input
              style={modalInput}
              type="number"
              min="0.01"
              step="0.01"
              placeholder="200"
              value={newDefaultRiskAmount}
              onChange={(event) => onDefaultRiskAmountChange(event.target.value)}
            />
            <p style={{ color: "#94a3b8", fontSize: 12, lineHeight: 1.5, margin: "6px 0 0" }}>
              TradePilot uses this to calculate R-multiple analytics. If 1R = $200 and a trade makes $400, that trade is +2R.
            </p>
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
  const location = useLocation();
  const prerenderingPublicRoute = typeof window !== "undefined" && new URLSearchParams(window.location.search).has("__prerender");
  const [session, setSession] = useState<AuthSession | null>(null);
  const [authNotice, setAuthNotice] = useState("");
  const [checkingAuth, setCheckingAuth] = useState(!prerenderingPublicRoute);
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
      if (prerenderingPublicRoute) {
        setCheckingAuth(false);
        return;
      }

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
  }, [navigate, prerenderingPublicRoute]);

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
    const activationSource = activationSourceFromSearch(location.search) ?? readPendingActivationSource();
    if (activationSource) {
      clearPendingActivationSource();
    }
    navigate(nextSession.user.role === "ADMIN" ? "/admin" : activationSource ? ACTIVATION_IMPORT_DESTINATION : "/dashboard", { replace: true });
  }

  function authenticatedPath(nextSession: AuthSession) {
    const activationSource = activationSourceFromSearch(location.search) ?? readPendingActivationSource();
    return nextSession.user.role === "ADMIN" ? "/admin" : activationSource ? ACTIVATION_IMPORT_DESTINATION : "/dashboard";
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

  async function refreshCurrentUser() {
    const user = await getCurrentUser();
    setSession((current) => current ? { ...current, user } : current);
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
      <Route path="/resources" element={<ResourcesPage />} />
      <Route path="/demo" element={<SampleDemoPage />} />
      <Route path="/how-tradepilot-works" element={<HowTradePilotWorksPage />} />
      <Route path="/tools/trading-expectancy-calculator" element={<TradingExpectancyCalculatorPage />} />
      <Route path="/tools/overtrading-calculator" element={<OvertradingCalculatorPage />} />
      <Route path="/tools/revenge-trading-calculator" element={<RevengeTradingCalculatorPage />} />
      {Object.values(guides).map((guide) => <Route key={guide.path} path={guide.path} element={<GuidePage guide={guide} />} />)}
      {Object.values(seoLandingPages).map((content) => (
        <Route
          key={content.path}
          path={content.path}
          element={<SeoLandingPage content={content} />}
        />
      ))}
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
              <DashboardApp user={session.user} onLogout={handleLogout} onUserRefresh={refreshCurrentUser} />
      {showIdleWarning && (
                <IdleWarningModal
                  countdownSeconds={idleCountdownSeconds}
                  onStaySignedIn={resetIdleTimers}
                  onLogoutNow={logoutNow}
                />
              )}
            </>
          ) : (
            <UnauthenticatedFallback authNotice={authNotice} />
          )
        }
      />
    </Routes>
  );
}

const privateRoutePrefixes = [
  "/admin",
  "/analytics",
  "/billing",
  "/calendar",
  "/coach",
  "/dashboard",
  "/dev-tools",
  "/journey",
  "/leak-detector",
  "/playbook",
  "/session-review",
  "/sessions",
  "/trades",
  "/trading-dna",
];

function UnauthenticatedFallback({ authNotice }: { authNotice: string }) {
  const location = useLocation();
  const isPrivateRoute = privateRoutePrefixes.some(
    (prefix) => location.pathname === prefix || location.pathname.startsWith(`${prefix}/`)
  );

  if (isPrivateRoute) {
    return (
      <Navigate
        to="/login"
        replace
        state={authNotice ? { sessionExpiredMessage: authNotice } : undefined}
      />
    );
  }

  return <NotFoundPage />;
}

function App() {
  return (
    <BrowserRouter>
      <ScrollToTop />
      <Suspense fallback={<RouteLoadingState />}>
        <AuthRoutes />
      </Suspense>
    </BrowserRouter>
  );
}

function ScrollToTop() {
  const { hash, pathname } = useLocation();

  useEffect(() => {
    if (hash) {
      let cancelled = false;
      let attempts = 0;
      let timeout: number | null = null;

      const scrollToHashTarget = () => {
        if (cancelled) return;

        const target = document.getElementById(hash.slice(1));
        if (target) {
          target.scrollIntoView({ block: "start", behavior: "auto" });
          return;
        }

        attempts += 1;
        if (attempts < 20) {
          timeout = window.setTimeout(scrollToHashTarget, 25);
        }
      };

      scrollToHashTarget();

      return () => {
        cancelled = true;
        if (timeout !== null) {
          window.clearTimeout(timeout);
        }
      };
    }

    const scrollToRouteTop = () => {
      window.scrollTo({ top: 0, left: 0, behavior: "auto" });
    };

    scrollToRouteTop();
    const frame = window.requestAnimationFrame(scrollToRouteTop);
    const timeout = window.setTimeout(scrollToRouteTop, 0);

    return () => {
      window.cancelAnimationFrame(frame);
      window.clearTimeout(timeout);
    };
  }, [hash, pathname]);

  return null;
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
