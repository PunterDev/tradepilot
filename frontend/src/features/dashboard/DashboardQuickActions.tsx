import { Link } from "react-router-dom";
import { useRef, useState } from "react";
import type { ChangeEvent, CSSProperties } from "react";
import type { TradingAccount } from "../../api/accountApi";
import type { SelectedAccountId } from "../../types/dashboard";
import { button, card, input, label, mutedText, sectionTitle } from "../../styles/dashboardStyles";

type Props = {
  accounts: TradingAccount[];
  selectedAccountId: SelectedAccountId;
  parsing: boolean;
  saving: boolean;
  fileName: string;
  tradeCount: number;
  onSelectAccount: (accountId: SelectedAccountId) => void;
  onCsvUpload: (event: ChangeEvent<HTMLInputElement>) => void;
  onSaveToDatabase: () => void;
};

export default function DashboardQuickActions({
  accounts,
  selectedAccountId,
  parsing,
  saving,
  fileName,
  tradeCount,
  onSelectAccount,
  onCsvUpload,
  onSaveToDatabase,
}: Props) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [isPickerHovered, setIsPickerHovered] = useState(false);
  const canImport = selectedAccountId !== "" && selectedAccountId !== "ALL";
  const canSave = canImport && tradeCount > 0 && Boolean(fileName) && !saving;
  const pickerDisabled = !canImport || parsing || saving;

  return (
    <div style={grid}>
      <section style={topCard}>
        <h2 style={sectionTitle}>Account</h2>
        <label style={label}>Viewing</label>
        <select
          value={selectedAccountId}
          onChange={(event) => {
            const value = event.target.value;
            onSelectAccount(value === "ALL" ? "ALL" : value ? Number(value) : "");
          }}
          style={input}
        >
          <option value="ALL">All Accounts</option>
          {accounts.map((account) => (
            <option key={account.id} value={account.id}>
              {account.name}
            </option>
          ))}
        </select>
        <p style={mutedText}>All Accounts is analytics-only. Select one account before importing.</p>
      </section>

      <section style={topCard}>
        <h2 style={sectionTitle}>Import CSV</h2>
        <input
          ref={fileInputRef}
          type="file"
          accept=".csv"
          disabled={pickerDisabled}
          onChange={onCsvUpload}
          hidden
          aria-label="Choose CSV file"
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
          <span aria-live="polite" title={fileName || "No file selected"} style={fileNameText}>
            {fileName || "No file selected"}
          </span>
        </div>
        <div style={{ display: "flex", gap: 10, alignItems: "center", marginTop: 12, flexWrap: "wrap" }}>
          <button
            type="button"
            disabled={!canSave}
            onClick={onSaveToDatabase}
            style={{ ...button, width: 170, opacity: canSave ? 1 : 0.55 }}
          >
            {saving ? "Saving..." : "Save Trades"}
          </button>
          <span style={mutedText}>
            {tradeCount > 0 ? `${tradeCount} parsed` : "No file parsed"}
          </span>
        </div>
      </section>

      <section style={topCard}>
        <h2 style={sectionTitle}>Quick Actions</h2>
        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
          <Link to="/session-review" style={linkButton}>Session Review</Link>
          <Link to="/trading-dna" style={linkButton}>Trading DNA</Link>
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
