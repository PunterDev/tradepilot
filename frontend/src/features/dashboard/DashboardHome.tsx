import { useEffect, useState, type ChangeEvent, type CSSProperties } from "react";
import type { TradingAccount } from "../../api/accountApi";
import { getExecutionAccountSummary, type ExecutionAccountSummary } from "../../api/executionApi";
import type { BillingUsage } from "../../types/billing";
import type { ActivationStatus } from "../../types/activation";
import type { TradeImportPreviewResponse } from "../../api/tradeApi";
import type { ImportFeedback } from "../../hooks/useDashboardData";
import type { SelectedAccountId } from "../../types/dashboard";
import type { DailyPnl } from "../../utils/dashboardUtils";
import type { TradovateTrade } from "../../utils/tradovateCsv";
import PageHeader from "../../layout/PageHeader";
import CompactUsageWidget from "./CompactUsageWidget";
import DashboardKpiGrid from "./DashboardKpiGrid";
import DashboardPreviewCards from "./DashboardPreviewCards";
import DashboardQuickActions from "./DashboardQuickActions";
import EquityCurveCard from "./EquityCurveCard";
import { getTradeCalendarDate } from "../../utils/dashboardUtils";

type Metrics = {
  totalPnl: number;
  totalTrades: number;
  overallWinRate: number;
  bestWinStreak: number;
  worstDay: number;
};

type Props = {
  accounts: TradingAccount[];
  selectedAccountId: SelectedAccountId;
  billingUsage: BillingUsage | null;
  billingLoading: boolean;
  billingError: string;
  dailyPnl: DailyPnl[];
  trades: TradovateTrade[];
  metrics: Metrics;
  parsing: boolean;
  saving: boolean;
  fileName: string;
  parsedImportCount: number;
  importPreview: TradeImportPreviewResponse | null;
  importFeedback: ImportFeedback | null;
  activationStatus: ActivationStatus | null;
  showAccountForm: boolean;
  creatingAccount: boolean;
  newAccountName: string;
  newPropFirm: string;
  newAccountType: string;
  newStartingBalance: string;
  newDefaultRiskAmount: string;
  billingResult?: string | null;
  onSelectAccount: (accountId: SelectedAccountId) => void;
  onToggleAccountForm: () => void;
  onCreateAccount: () => void;
  onAccountNameChange: (value: string) => void;
  onPropFirmChange: (value: string) => void;
  onAccountTypeChange: (value: string) => void;
  onStartingBalanceChange: (value: string) => void;
  onDefaultRiskAmountChange: (value: string) => void;
  onCsvUpload: (event: ChangeEvent<HTMLInputElement>) => void;
  onSaveToDatabase: () => void;
  onTradeSelect: (tradeId: number) => void;
  showTradingDnaEntryPoint: boolean;
  v2FeaturesEnabled: boolean;
};

export default function DashboardHome({
  accounts,
  selectedAccountId,
  billingUsage,
  billingLoading,
  billingError,
  dailyPnl,
  trades,
  metrics,
  parsing,
  saving,
  fileName,
  parsedImportCount,
  importPreview,
  importFeedback,
  activationStatus,
  showAccountForm,
  creatingAccount,
  newAccountName,
  newPropFirm,
  newAccountType,
  newStartingBalance,
  newDefaultRiskAmount,
  billingResult,
  onSelectAccount,
  onToggleAccountForm,
  onCreateAccount,
  onAccountNameChange,
  onPropFirmChange,
  onAccountTypeChange,
  onStartingBalanceChange,
  onDefaultRiskAmountChange,
  onCsvUpload,
  onSaveToDatabase,
  onTradeSelect,
  showTradingDnaEntryPoint,
  v2FeaturesEnabled,
}: Props) {
  const latestTrade = trades[0];
  const latestDate = latestTrade ? getTradeCalendarDate(latestTrade) : null;
  const executionAccountId = typeof selectedAccountId === "number" ? selectedAccountId : accounts[0]?.id;
  const [executionSummary, setExecutionSummary] = useState<ExecutionAccountSummary | null>(null);

  useEffect(() => {
    let cancelled = false;
    setExecutionSummary(null);
    if (!v2FeaturesEnabled || !executionAccountId) return;
    getExecutionAccountSummary(executionAccountId)
      .then((summary) => {
        if (!cancelled) setExecutionSummary(summary);
      })
      .catch(() => {
        if (!cancelled) setExecutionSummary(null);
      });
    return () => {
      cancelled = true;
    };
  }, [executionAccountId, v2FeaturesEnabled]);

  return (
    <>
      <PageHeader
        label="TradePilot AI"
        title="Dashboard"
        titleTestId="dashboard-heading"
        description="A fast overview of account performance, import status, and the next places worth reviewing."
      />

      {billingResult === "success" && <div style={successBanner}>Upgrade successful. Pro limits will unlock after Stripe confirms the subscription.</div>}
      {billingResult === "cancelled" && <div style={warningBanner}>Checkout cancelled. Your Free plan is still active.</div>}

      <FirstInsightActivationPanel
        activationStatus={activationStatus}
        latestTradeId={latestTrade?.id}
        onCreateAccount={onToggleAccountForm}
        onReviewTrade={onTradeSelect}
      />

      <DashboardQuickActions
        accounts={accounts}
        selectedAccountId={selectedAccountId}
        parsing={parsing}
        saving={saving}
        fileName={fileName}
        tradeCount={parsedImportCount}
        importPreview={importPreview}
        importFeedback={importFeedback}
        showAccountForm={showAccountForm}
        creatingAccount={creatingAccount}
        newAccountName={newAccountName}
        newPropFirm={newPropFirm}
        newAccountType={newAccountType}
        newStartingBalance={newStartingBalance}
        newDefaultRiskAmount={newDefaultRiskAmount}
        onSelectAccount={onSelectAccount}
        onToggleAccountForm={onToggleAccountForm}
        onCreateAccount={onCreateAccount}
        onAccountNameChange={onAccountNameChange}
        onPropFirmChange={onPropFirmChange}
        onAccountTypeChange={onAccountTypeChange}
        onStartingBalanceChange={onStartingBalanceChange}
        onDefaultRiskAmountChange={onDefaultRiskAmountChange}
        onCsvUpload={onCsvUpload}
        onSaveToDatabase={onSaveToDatabase}
        showTradingDnaEntryPoint={showTradingDnaEntryPoint}
      />

      <CompactUsageWidget usage={billingUsage} loading={billingLoading} error={billingError} />

      <DashboardKpiGrid metrics={metrics} dailyPnl={dailyPnl} />

      {executionSummary && <DashboardExecutionSummary summary={executionSummary} />}

      <div style={mainGrid}>
        <EquityCurveCard dailyPnl={dailyPnl} />
      </div>

      <DashboardPreviewCards
        dailyPnl={dailyPnl}
        latestDate={latestDate}
        latestTrade={latestTrade}
        totalTrades={metrics.totalTrades}
        winRate={metrics.overallWinRate}
        worstDay={metrics.worstDay}
        showTradingDnaEntryPoint={showTradingDnaEntryPoint}
      />
    </>
  );
}

function DashboardExecutionSummary({ summary }: { summary: ExecutionAccountSummary }) {
  return (
    <section style={executionSummaryCard}>
      <div>
        <div style={activationEyebrow}>Execution</div>
        <h2 style={activationTitle}>Trade your plan. Not your P&L.</h2>
        <p style={activationText}>{summary.weeklyFocus?.title ?? "Review the process before the result."}</p>
      </div>
      <div style={executionSummaryMetrics}>
        <ExecutionPill label="Score" value={summary.executionScore == null ? "N/A" : `${summary.executionScore}/100`} />
        <ExecutionPill label="Rule Adherence" value={`${toNumber(summary.ruleAdherencePercent).toFixed(0)}%`} />
        <ExecutionPill label="Avoidable R" value={`${formatR(summary.avoidableImpactR)}R`} />
      </div>
    </section>
  );
}

function ExecutionPill({ label, value }: { label: string; value: string }) {
  return (
    <div style={executionPill}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function toNumber(value?: number | string | null) {
  const parsed = Number(value ?? 0);
  return Number.isFinite(parsed) ? parsed : 0;
}

function formatR(value?: number | string | null) {
  return toNumber(value).toFixed(2).replace(/\.00$/, "").replace(/(\.\d)0$/, "$1");
}

function FirstInsightActivationPanel({
  activationStatus,
  latestTradeId,
  onCreateAccount,
  onReviewTrade,
}: {
  activationStatus: ActivationStatus | null;
  latestTradeId?: number;
  onCreateAccount: () => void;
  onReviewTrade: (tradeId: number) => void;
}) {
  if (!activationStatus) return null;

  const previewAvailable = activationStatus.proPreview?.available;
  const firstInsight = activationStatus.firstInsight;
  const hasLeakReady =
    activationStatus.recommendedNextAction === "GENERATE_FIRST_LEAK" &&
    activationStatus.recommendedAccountId;
  const importNeeded = activationStatus.recommendedNextAction === "IMPORT_TRADES";

  return (
    <section style={activationPanel}>
      <div style={{ minWidth: 0 }}>
        <div style={activationEyebrow}>
          {activationStatus.activated ? "First Insight Ready" : "First Insight Activation"}
        </div>
        <h2 style={activationTitle}>
          {firstInsight?.title ??
            (activationStatus.hasTradingAccount
              ? "Import trades to unlock your first AI-backed pattern."
              : "Create your first trading account.")}
        </h2>
        <p style={activationText}>
          {firstInsight?.suggestedRule ??
            (importNeeded
              ? "Upload a Tradovate or Topstep CSV, then TradePilot will route you to the first useful review instead of a blank dashboard."
              : activationStatus.firstInsightAvailable
                ? "You have enough trade data for a first review. Start with an individual trade, then use the next AI action when ready."
                : "Set up an account and import your first trades to make the dashboard actionable.")}
        </p>
        <div style={activationMetaRow}>
          <span>{activationStatus.tradeCount} trades</span>
          <span>{activationStatus.totalAiCreditsAvailable} AI credits available</span>
          {activationStatus.additionalTradesNeededForLeakAnalysis > 0 && (
            <span>{activationStatus.additionalTradesNeededForLeakAnalysis} more for Leak Detector</span>
          )}
        </div>
      </div>
      <div style={activationActions}>
        {!activationStatus.hasTradingAccount && (
          <button type="button" style={primaryActivationButton} onClick={onCreateAccount}>
            Create account
          </button>
        )}
        {hasLeakReady && (
          <a href="/leak-detector" style={primaryActivationLink}>
            Run Leak Detector
          </a>
        )}
        {!firstInsight && latestTradeId && !hasLeakReady && (
          <button type="button" style={primaryActivationButton} onClick={() => onReviewTrade(latestTradeId)}>
            Review first trade
          </button>
        )}
        {previewAvailable && (
          <a href="/coach" style={secondaryActivationLink}>
            Use Pro preview
          </a>
        )}
        {activationStatus.aiAllowanceLow && (
          <a href="/billing" style={secondaryActivationLink}>
            Add credits
          </a>
        )}
      </div>
    </section>
  );
}

const mainGrid: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "minmax(0, 1fr)",
  gap: 18,
  marginTop: 18,
};

const successBanner: CSSProperties = {
  marginBottom: 18,
  padding: "14px 16px",
  borderRadius: 14,
  border: "1px solid rgba(52,211,153,0.35)",
  background: "rgba(16,185,129,0.12)",
  color: "#bbf7d0",
  fontWeight: 800,
};

const warningBanner: CSSProperties = {
  marginBottom: 18,
  padding: "14px 16px",
  borderRadius: 14,
  border: "1px solid rgba(251,191,36,0.35)",
  background: "rgba(245,158,11,0.11)",
  color: "#fde68a",
  fontWeight: 800,
};

const executionSummaryCard: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "minmax(0, 1fr) auto",
  gap: 16,
  alignItems: "center",
  margin: "18px 0",
  padding: 18,
  borderRadius: 8,
  border: "1px solid rgba(96,165,250,0.28)",
  background: "rgba(15,23,42,0.84)",
};

const executionSummaryMetrics: CSSProperties = {
  display: "flex",
  gap: 10,
  flexWrap: "wrap",
  justifyContent: "flex-end",
};

const executionPill: CSSProperties = {
  minWidth: 112,
  padding: "10px 12px",
  borderRadius: 8,
  border: "1px solid rgba(148,163,184,0.18)",
  background: "rgba(2,6,23,0.46)",
  color: "#e0f2fe",
  display: "grid",
  gap: 4,
};

const activationPanel: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "minmax(0, 1fr) auto",
  gap: 18,
  alignItems: "center",
  marginBottom: 18,
  padding: 18,
  borderRadius: 18,
  border: "1px solid rgba(45,212,191,0.28)",
  background: "linear-gradient(135deg, rgba(15,23,42,0.96), rgba(8,47,73,0.62))",
};

const activationEyebrow: CSSProperties = {
  color: "#67e8f9",
  fontSize: 12,
  fontWeight: 900,
  textTransform: "uppercase",
  letterSpacing: 1.2,
  marginBottom: 8,
};

const activationTitle: CSSProperties = {
  margin: 0,
  color: "#f8fafc",
  fontSize: 22,
  lineHeight: 1.2,
};

const activationText: CSSProperties = {
  margin: "8px 0 0",
  color: "#cbd5e1",
  lineHeight: 1.55,
};

const activationMetaRow: CSSProperties = {
  display: "flex",
  flexWrap: "wrap",
  gap: 8,
  marginTop: 12,
  color: "#a7f3d0",
  fontSize: 13,
  fontWeight: 800,
};

const activationActions: CSSProperties = {
  display: "flex",
  flexWrap: "wrap",
  justifyContent: "flex-end",
  gap: 10,
};

const primaryActivationButton: CSSProperties = {
  border: "none",
  borderRadius: 12,
  padding: "12px 15px",
  background: "#22c55e",
  color: "#020617",
  fontWeight: 900,
  cursor: "pointer",
};

const primaryActivationLink: CSSProperties = {
  ...primaryActivationButton,
  display: "inline-flex",
  textDecoration: "none",
};

const secondaryActivationLink: CSSProperties = {
  display: "inline-flex",
  alignItems: "center",
  borderRadius: 12,
  padding: "12px 15px",
  border: "1px solid rgba(148,163,184,0.28)",
  background: "rgba(15,23,42,0.72)",
  color: "#e0f2fe",
  fontWeight: 900,
  textDecoration: "none",
};
