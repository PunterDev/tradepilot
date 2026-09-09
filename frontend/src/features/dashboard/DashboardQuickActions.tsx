import { Link } from "react-router-dom";
import { useRef, useState } from "react";
import type { ChangeEvent, CSSProperties } from "react";
import type { TradingAccount } from "../../api/accountApi";
import type { TradeImportPreviewResponse } from "../../api/tradeApi";
import type { ImportFeedback } from "../../hooks/useDashboardData";
import type { SelectedAccountId } from "../../types/dashboard";
import { button, card, input, label, mutedText, sectionTitle } from "../../styles/dashboardStyles";

type Props = {
  accounts: TradingAccount[];
  selectedAccountId: SelectedAccountId;
  parsing: boolean;
  saving: boolean;
  fileName: string;
  tradeCount: number;
  importPreview: TradeImportPreviewResponse | null;
  importFeedback: ImportFeedback | null;
  showAccountForm: boolean;
  creatingAccount: boolean;
  newAccountName: string;
  newPropFirm: string;
  newAccountType: string;
  newStartingBalance: string;
  newDefaultRiskAmount: string;
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

export default function DashboardQuickActions({
  accounts,
  selectedAccountId,
  parsing,
  saving,
  fileName,
  tradeCount,
  importPreview,
  importFeedback,
  showAccountForm,
  creatingAccount,
  newAccountName,
  newPropFirm,
  newAccountType,
  newStartingBalance,
  newDefaultRiskAmount,
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
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [isPickerHovered, setIsPickerHovered] = useState(false);
  const canImport = selectedAccountId !== "" && selectedAccountId !== "ALL";
  const canSave = canImport && tradeCount > 0 && Boolean(fileName) && Boolean(importPreview) && !parsing && !saving;
  const pickerDisabled = !canImport || parsing || saving;
  const importStatusLabel = tradeCount > 0
    ? `${tradeCount} parsed`
    : fileName && importFeedback?.type === "error"
      ? `${fileName} selected — import validation failed`
      : "No file parsed";

  return (
    <div style={grid}>
      <section style={topCard}>
        <div>
          <div style={accountHeader}>
            <h2 style={sectionTitle}>Account</h2>
            <button
              type="button"
              onClick={onToggleAccountForm}
              style={addAccountButton}
            >
              {showAccountForm ? "Cancel" : "+ Add Account"}
            </button>
          </div>

          <label style={label}>Viewing</label>
          <select
            data-testid="import-account-select"
            value={accounts.length > 0 ? selectedAccountId : ""}
            onChange={(event) => {
              const value = event.target.value;
              onSelectAccount(value === "ALL" ? "ALL" : value ? Number(value) : "");
            }}
            style={input}
            disabled={accounts.length === 0}
          >
            {accounts.length === 0 ? (
              <option value="">No accounts yet</option>
            ) : (
              <>
                <option value="ALL">All Accounts</option>
                {accounts.map((account) => (
                  <option key={account.id} value={account.id}>
                    {account.name}
                  </option>
                ))}
              </>
            )}
          </select>

          {accounts.length === 0 ? (
            <p style={mutedText}>Create your first trading account before importing trades.</p>
          ) : (
            <p style={mutedText}>All Accounts is analytics-only. Select one account before importing.</p>
          )}
        </div>

        {showAccountForm && (
          <div style={accountForm}>
            <div style={{ gridColumn: "1 / -1" }}>
              <label style={label}>Account Name</label>
              <input
                style={input}
                placeholder="Topstep 50K"
                value={newAccountName}
                onChange={(event) => onAccountNameChange(event.target.value)}
              />
            </div>

            <div>
              <label style={label}>Broker / Platform</label>
              <select
                style={input}
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
              <label style={label}>Account Type</label>
              <select
                style={input}
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
              <label style={label}>Starting Balance</label>
              <input
                style={input}
                type="number"
                placeholder="50000"
                value={newStartingBalance}
                onChange={(event) => onStartingBalanceChange(event.target.value)}
              />
            </div>

            <div>
              <label style={label}>Default 1R</label>
              <input
                style={input}
                type="number"
                min="0.01"
                step="0.01"
                placeholder="200"
                value={newDefaultRiskAmount}
                onChange={(event) => onDefaultRiskAmountChange(event.target.value)}
              />
            </div>

            <button
              type="button"
              onClick={onCreateAccount}
              disabled={creatingAccount || !newAccountName.trim()}
              style={{
                ...button,
                gridColumn: "1 / -1",
                opacity: creatingAccount || !newAccountName.trim() ? 0.55 : 1,
                cursor: creatingAccount || !newAccountName.trim() ? "not-allowed" : "pointer",
              }}
            >
              {creatingAccount ? "Creating..." : "Create Account"}
            </button>
          </div>
        )}
      </section>

      <section style={topCard}>
        <h2 style={sectionTitle}>Import CSV</h2>
        <p style={{ ...mutedText, marginTop: 0 }}>
          Supports Tradovate trade exports and Topstep order-history CSV exports.
        </p>
        <input
          ref={fileInputRef}
          type="file"
          data-testid="import-file-input"
          accept=".csv"
          disabled={pickerDisabled}
          onChange={onCsvUpload}
          hidden
          aria-label="Choose CSV file"
        />
        <div style={filePickerRow}>
          <button
            type="button"
            data-testid="import-file-button"
            disabled={pickerDisabled}
            onClick={() => fileInputRef.current?.click()}
            onMouseEnter={() => setIsPickerHovered(true)}
            onMouseLeave={() => setIsPickerHovered(false)}
            style={{
              ...filePickerButton,
              ...(isPickerHovered && !pickerDisabled ? filePickerButtonHover : {}),
              ...(pickerDisabled ? filePickerButtonDisabled : {}),
            }}
          >
            {parsing ? "Parsing..." : "Choose CSV File"}
          </button>
          <span aria-live="polite" title={fileName || "No file selected"} style={fileNameText}>
            {fileName || "No file selected"}
          </span>
        </div>
        <div style={{ display: "flex", gap: 10, alignItems: "center", marginTop: 12, flexWrap: "wrap" }}>
          <button
            type="button"
            data-testid="import-confirm-button"
            disabled={!canSave}
            onClick={onSaveToDatabase}
            style={{ ...button, width: 170, opacity: canSave ? 1 : 0.55 }}
          >
            {saving ? "Saving..." : "Save Trades"}
          </button>
          <span style={mutedText}>
            {importStatusLabel}
          </span>
        </div>
        {importFeedback && (
          <div style={feedbackBox(importFeedback.type)} role={importFeedback.type === "error" ? "alert" : "status"}>
            <strong>{importFeedback.message}</strong>
            {importFeedback.details && importFeedback.details.length > 0 && (
              <ul style={feedbackList}>
                {importFeedback.details.slice(0, 4).map((detail) => (
                  <li key={detail}>{detail}</li>
                ))}
              </ul>
            )}
          </div>
        )}
        {importPreview && (
          <div style={previewBox} data-testid="import-preview">
            <div style={previewHeader}>
              <span data-testid="import-broker">{importPreview.detectedFormat} detected</span>
              <span data-testid="import-trade-count">{importPreview.completedTrades} completed trades</span>
            </div>
            <div style={previewGrid}>
              <span>Filled: {importPreview.filledRows}</span>
              <span data-testid="import-cancelled-ignored">Cancelled ignored: {importPreview.cancelledRowsIgnored}</span>
              <span data-testid="import-duplicate-count">Duplicates: {importPreview.duplicateTrades}</span>
              <span>Open positions: {importPreview.incompletePositions}</span>
            </div>
            {importPreview.sourceAccountNames.length > 0 && (
              <p style={previewText}>Source account: {importPreview.sourceAccountNames.join(", ")}</p>
            )}
            {importPreview.warnings.length > 0 && (
              <p style={previewWarning}>{importPreview.warnings.slice(0, 2).join(" ")}</p>
            )}
          </div>
        )}
      </section>

      <section style={topCard}>
        <h2 style={sectionTitle}>Quick Actions</h2>
        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
          <Link to="/session-review" style={linkButton}>Session Review</Link>
          {showTradingDnaEntryPoint && <Link to="/trading-dna" style={linkButton}>Trading DNA</Link>}
          <Link to="/billing" style={linkButton}>Upgrade</Link>
        </div>
      </section>
    </div>
  );
}

const grid: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))",
  gap: 18,
};

const topCard: CSSProperties = {
  ...card,
  minHeight: 176,
  display: "flex",
  flexDirection: "column",
  justifyContent: "space-between",
};

const accountHeader: CSSProperties = {
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  gap: 12,
  marginBottom: 8,
};

const addAccountButton: CSSProperties = {
  width: "auto",
  whiteSpace: "nowrap",
  padding: "9px 12px",
  borderRadius: 999,
  border: "1px solid rgba(52,211,153,0.42)",
  background: "rgba(34,197,94,0.12)",
  color: "#86efac",
  fontWeight: 900,
  cursor: "pointer",
};

const accountForm: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))",
  gap: 12,
  marginTop: 16,
  paddingTop: 16,
  borderTop: "1px solid rgba(148,163,184,0.14)",
};

const linkButton: CSSProperties = {
  ...button,
  width: "auto",
  display: "inline-flex",
  textDecoration: "none",
};

const filePickerRow: CSSProperties = {
  minHeight: 46,
  display: "flex",
  alignItems: "center",
  gap: 14,
  flexWrap: "wrap",
};

const filePickerButton: CSSProperties = {
  minWidth: 158,
  padding: "13px 18px",
  border: "1px solid rgba(52,211,153,0.48)",
  borderRadius: 14,
  background: "linear-gradient(135deg, #22c55e, #16a34a)",
  color: "#ffffff",
  cursor: "pointer",
  fontWeight: 900,
  boxShadow: "0 14px 35px rgba(34,197,94,0.28)",
  transition: "transform 140ms ease, box-shadow 140ms ease, opacity 140ms ease",
};

const filePickerButtonHover: CSSProperties = {
  transform: "translateY(-1px)",
  boxShadow: "0 18px 44px rgba(34,197,94,0.34)",
};

const filePickerButtonDisabled: CSSProperties = {
  opacity: 0.55,
  cursor: "not-allowed",
  transform: "none",
  boxShadow: "none",
};

const fileNameText: CSSProperties = {
  minWidth: 0,
  flex: "1 1 160px",
  color: "#cbd5e1",
  fontSize: 14,
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
};

const feedbackList: CSSProperties = {
  margin: "8px 0 0",
  paddingLeft: 18,
  color: "inherit",
  fontSize: 12,
  lineHeight: 1.45,
};

function feedbackBox(type: ImportFeedback["type"]): CSSProperties {
  const palette = type === "success"
    ? { border: "rgba(52,211,153,0.34)", bg: "rgba(16,185,129,0.12)", color: "#bbf7d0" }
    : type === "warning"
      ? { border: "rgba(251,191,36,0.34)", bg: "rgba(245,158,11,0.11)", color: "#fde68a" }
      : { border: "rgba(251,113,133,0.36)", bg: "rgba(244,63,94,0.12)", color: "#fecdd3" };
  return {
    marginTop: 12,
    padding: "12px 14px",
    borderRadius: 14,
    border: `1px solid ${palette.border}`,
    background: palette.bg,
    color: palette.color,
    fontSize: 13,
    fontWeight: 800,
  };
}

const previewBox: CSSProperties = {
  marginTop: 12,
  border: "1px solid rgba(52,211,153,0.24)",
  borderRadius: 14,
  padding: 12,
  background: "rgba(15,23,42,0.72)",
};

const previewHeader: CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  gap: 10,
  color: "#ecfeff",
  fontWeight: 800,
  fontSize: 13,
  flexWrap: "wrap",
};

const previewGrid: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(2, minmax(0, 1fr))",
  gap: "6px 10px",
  marginTop: 10,
  color: "#94a3b8",
  fontSize: 12,
};

const previewText: CSSProperties = {
  margin: "10px 0 0",
  color: "#cbd5e1",
  fontSize: 12,
};

const previewWarning: CSSProperties = {
  margin: "8px 0 0",
  color: "#fbbf24",
  fontSize: 12,
  lineHeight: 1.45,
};
