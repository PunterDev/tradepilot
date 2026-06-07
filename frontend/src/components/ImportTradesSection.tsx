import { useRef, useState } from "react";
import type { TradingAccount } from "../api/accountApi";
import type { SelectedAccountId } from "../types/dashboard";
import {
  button,
  card,
  formGrid,
  input,
  label,
  mutedText,
  sectionTitle,
} from "../styles/dashboardStyles";

type ImportTradesSectionProps = {
  accounts: TradingAccount[];
  selectedAccountId: SelectedAccountId;
  loadingAccounts: boolean;
  parsing: boolean;
  saving: boolean;
  fileName: string;
  tradeCount: number;
  onSelectAccount: (accountId: SelectedAccountId) => void;
  onCsvUpload: (event: React.ChangeEvent<HTMLInputElement>) => void;
  onSaveToDatabase: () => void;
};

export default function ImportTradesSection({
  accounts,
  selectedAccountId,
  loadingAccounts,
  parsing,
  saving,
  fileName,
  tradeCount,
  onSelectAccount,
  onCsvUpload,
  onSaveToDatabase,
}: ImportTradesSectionProps) {
  const [showPreview, setShowPreview] = useState(false);
  const [isPickerHovered, setIsPickerHovered] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const hasAccounts = accounts.length > 0;
  const canImport = hasAccounts && selectedAccountId !== "" && selectedAccountId !== "ALL";
  const hasParsedFile = Boolean(fileName) && tradeCount > 0;
  const canSave = canImport && hasParsedFile && !parsing && !saving;
  const pickerDisabled = !canImport || parsing || saving;

  return (
    <div style={card}>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          gap: 16,
          alignItems: "center",
        }}
      >
        <h2 style={sectionTitle}>Import Trades</h2>

        {tradeCount > 0 && (
          <button
            type="button"
            onClick={() => setShowPreview((prev) => !prev)}
            style={{
              ...button,
              width: 145,
              background: "rgba(15,23,42,0.9)",
              border: "1px solid rgba(148,163,184,0.35)",
            }}
          >
            {showPreview ? "Hide Preview" : "Show Preview"}
          </button>
        )}
      </div>

      <div style={formGrid}>
        <div>
          <label style={label}>Trading Account</label>
          <select
            value={hasAccounts ? selectedAccountId : ""}
            onChange={(e) => {
              const value = e.target.value;
              onSelectAccount(value === "ALL" ? "ALL" : value ? Number(value) : "");
            }}
            style={input}
            disabled={loadingAccounts || !hasAccounts}
          >
            {hasAccounts && <option value="ALL">All Accounts</option>}
            <option value="">Select account</option>
            {accounts.map((account) => (
              <option key={account.id} value={account.id}>
                {account.name} — {account.propFirm}
                {account.accountNumber ? ` — ${account.accountNumber}` : ""}
              </option>
            ))}
          </select>

          {accounts.length === 0 && !loadingAccounts && (
            <p style={{ color: "#fb7185", fontSize: 13 }}>
              Create your first trading account before importing trades.
            </p>
          )}

          {hasAccounts && selectedAccountId === "ALL" && (
            <p style={mutedText}>
              All Accounts is for analytics only. Select one account to import trades.
            </p>
          )}
        </div>

        <div>
          <label style={label}>Tradovate CSV</label>
          <input
            ref={fileInputRef}
            type="file"
            accept=".csv"
            onChange={onCsvUpload}
            hidden
            aria-label="Choose Tradovate CSV file"
            disabled={pickerDisabled}
          />
          <div style={filePickerRow}>
            <button
              type="button"
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
            <span
              aria-live="polite"
              title={fileName || "No file selected"}
              style={fileNameText}
            >
              {fileName || "No file selected"}
            </span>
          </div>
        </div>

        <div style={{ display: "flex", alignItems: "end" }}>
          <button
            onClick={onSaveToDatabase}
            disabled={!canSave}
            style={{
              ...button,
              opacity: canSave ? 1 : 0.55,
              cursor: canSave ? "pointer" : "not-allowed",
            }}
          >
            {saving ? "Saving..." : "Save Imported Trades"}
          </button>
        </div>
      </div>

      {parsing && <p style={mutedText}>Parsing CSV...</p>}

      {hasParsedFile && (
        <p style={mutedText}>
          {tradeCount} trades parsed and ready to save.
        </p>
      )}

      {tradeCount > 0 && !showPreview && (
        <p style={mutedText}>
          Preview hidden. Click “Show Preview” to inspect imported rows.
        </p>
      )}
    </div>
  );
}

const filePickerRow: React.CSSProperties = {
  minHeight: 46,
  display: "flex",
  alignItems: "center",
  gap: 14,
  flexWrap: "wrap",
};

const filePickerButton: React.CSSProperties = {
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

const filePickerButtonHover: React.CSSProperties = {
  transform: "translateY(-1px)",
  boxShadow: "0 18px 44px rgba(34,197,94,0.34)",
};

const filePickerButtonDisabled: React.CSSProperties = {
  opacity: 0.55,
  cursor: "not-allowed",
  transform: "none",
  boxShadow: "none",
};

const fileNameText: React.CSSProperties = {
  minWidth: 0,
  flex: "1 1 180px",
  color: "#cbd5e1",
  fontSize: 14,
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
};
