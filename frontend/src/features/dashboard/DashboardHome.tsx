import type { ChangeEvent, CSSProperties } from "react";
import type { TradingAccount } from "../../api/accountApi";
import type { BillingUsage } from "../../types/billing";
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
  showTradingDnaEntryPoint: boolean;
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
  showTradingDnaEntryPoint,
}: Props) {
  const latestTrade = trades[0];
  const latestDate = latestTrade ? getTradeCalendarDate(latestTrade) : null;

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
