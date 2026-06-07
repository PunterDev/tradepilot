import type { TradingAccount } from "../api/accountApi";
import type { SelectedAccountId } from "../types/dashboard";
import {
  accountChip,
  button,
  card,
  formGrid,
  input,
  label,
  mutedText,
  sectionHeader,
  sectionTitle,
} from "../styles/dashboardStyles";

type AccountsSectionProps = {
  accounts: TradingAccount[];
  selectedAccountId: SelectedAccountId;
  showAccountForm: boolean;
  creatingAccount: boolean;
  newAccountName: string;
  newPropFirm: string;
  newAccountNumber: string;
  newAccountType: string;
  newStartingBalance: string;
  onToggleAccountForm: () => void;
  onCreateAccount: () => void;
  onDeleteAccount: (accountId: number) => void;
  onSelectAccount: (accountId: SelectedAccountId) => void;
  onAccountNameChange: (value: string) => void;
  onPropFirmChange: (value: string) => void;
  onAccountNumberChange: (value: string) => void;
  onAccountTypeChange: (value: string) => void;
  onStartingBalanceChange: (value: string) => void;
};

export default function AccountsSection({
  accounts,
  selectedAccountId,
  showAccountForm,
  creatingAccount,
  newAccountName,
  newPropFirm,
  newAccountNumber,
  newAccountType,
  newStartingBalance,
  onToggleAccountForm,
  onCreateAccount,
  onDeleteAccount,
  onSelectAccount,
  onAccountNameChange,
  onPropFirmChange,
  onAccountNumberChange,
  onAccountTypeChange,
  onStartingBalanceChange,
}: AccountsSectionProps) {
  return (
    <div style={{ ...card, marginBottom: 24 }}>
      <div style={sectionHeader}>
        <div>
          <h2 style={sectionTitle}>Trading Accounts</h2>
          <p style={mutedText}>
            Create accounts for Topstep, Apex, TPT, Tradovate, or any prop firm before importing CSV files.
          </p>
        </div>

        <button
          type="button"
          onClick={onToggleAccountForm}
          style={{ ...button, width: 180 }}
        >
          {showAccountForm ? "Cancel" : "Add Account"}
        </button>
      </div>

      {showAccountForm && (
        <div style={{ ...formGrid, marginTop: 22 }}>
          <div>
            <label style={label}>Account Name</label>
            <input
              style={input}
              placeholder="Topstep 50K"
              value={newAccountName}
              onChange={(e) => onAccountNameChange(e.target.value)}
            />
          </div>

          <div>
            <label style={label}>Broker / Platform</label>
            <select
              style={input}
              value={newPropFirm}
              onChange={(e) => onPropFirmChange(e.target.value)}
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
              onChange={(e) => onAccountTypeChange(e.target.value)}
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
              onChange={(e) => onStartingBalanceChange(e.target.value)}
            />
          </div>

          <div>
            <label style={label}>Account Number</label>
            <input
              style={input}
              placeholder="TS-001"
              value={newAccountNumber}
              onChange={(e) => onAccountNumberChange(e.target.value)}
            />
          </div>

          <div style={{ display: "flex", alignItems: "end" }}>
            <button
              type="button"
              onClick={onCreateAccount}
              disabled={creatingAccount}
              style={button}
            >
              {creatingAccount ? "Creating..." : "Create Account"}
            </button>
          </div>
        </div>
      )}

      {accounts.length > 0 && (
        <div style={{ marginTop: 22 }}>
          <h3 style={{ marginBottom: 12 }}>Existing Accounts</h3>
          <div style={{ display: "flex", flexWrap: "wrap", gap: 12 }}>
            <button
              type="button"
              onClick={() => onSelectAccount("ALL")}
              style={accountButtonStyle(selectedAccountId === "ALL")}
            >
              All Accounts
            </button>
            {accounts.map((account) => {
              const selected = selectedAccountId === account.id;

              return (
                <div key={account.id} style={{ position: "relative" }}>
                  <button
                    type="button"
                    onClick={() => onSelectAccount(account.id)}
                    style={{
                      ...accountButtonStyle(selected),
                      paddingRight: 36,
                    }}
                  >
                    {account.name} · {account.propFirm}
                    {account.accountType ? ` · ${account.accountType}` : ""}
                  </button>

                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onDeleteAccount(account.id);
                    }}
                    style={{
                      position: "absolute",
                      top: 6,
                      right: 6,
                      background: "transparent",
                      border: "none",
                      color: "#fb7185",
                      cursor: "pointer",
                      fontSize: 14,
                      fontWeight: 900,
                    }}
                  >
                    ✕
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}

function accountButtonStyle(selected: boolean) {
  return {
    ...accountChip,
    border: selected
      ? "1px solid rgba(52,211,153,0.75)"
      : "1px solid rgba(148,163,184,0.22)",
    background: selected
      ? "linear-gradient(135deg, rgba(16,185,129,0.22), rgba(34,197,94,0.12))"
      : "rgba(15,23,42,0.6)",
    color: selected ? "#6ee7b7" : "#cbd5e1",
  };
}
