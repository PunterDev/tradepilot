import { useState } from "react";
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
  newDefaultRiskAmount: string;
  onToggleAccountForm: () => void;
  onCreateAccount: () => void;
  onDeleteAccount: (accountId: number) => void;
  onUpdateAccountRisk: (accountId: number, defaultRiskAmount: number) => Promise<void>;
  onSelectAccount: (accountId: SelectedAccountId) => void;
  onAccountNameChange: (value: string) => void;
  onPropFirmChange: (value: string) => void;
  onAccountNumberChange: (value: string) => void;
  onAccountTypeChange: (value: string) => void;
  onStartingBalanceChange: (value: string) => void;
  onDefaultRiskAmountChange: (value: string) => void;
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
  newDefaultRiskAmount,
  onToggleAccountForm,
  onCreateAccount,
  onDeleteAccount,
  onUpdateAccountRisk,
  onSelectAccount,
  onAccountNameChange,
  onPropFirmChange,
  onAccountNumberChange,
  onAccountTypeChange,
  onStartingBalanceChange,
  onDefaultRiskAmountChange,
}: AccountsSectionProps) {
  const [riskInputs, setRiskInputs] = useState<Record<number, string>>({});
  const [savingRiskAccountId, setSavingRiskAccountId] = useState<number | null>(null);
  const [riskMessages, setRiskMessages] = useState<Record<number, { type: "success" | "error"; text: string }>>({});

  async function saveRiskAmount(account: TradingAccount) {
    const rawValue = riskInputs[account.id] ?? String(account.defaultRiskAmount ?? "");
    const value = Number(rawValue);
    if (!Number.isFinite(value) || value <= 0) {
      setRiskMessages((prev) => ({
        ...prev,
        [account.id]: { type: "error", text: "Enter a default 1R amount greater than $0." },
      }));
      return;
    }

    setSavingRiskAccountId(account.id);
    setRiskMessages((prev) => {
      const next = { ...prev };
      delete next[account.id];
      return next;
    });
    try {
      await onUpdateAccountRisk(account.id, value);
      setRiskInputs((prev) => ({ ...prev, [account.id]: value.toString() }));
      setRiskMessages((prev) => ({
        ...prev,
        [account.id]: { type: "success", text: "Default 1R updated." },
      }));
    } catch (error) {
      setRiskMessages((prev) => ({
        ...prev,
        [account.id]: { type: "error", text: riskErrorMessage(error) },
      }));
    } finally {
      setSavingRiskAccountId(null);
    }
  }

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
            <label style={label}>Default Risk Per Trade / 1R</label>
            <input
              style={input}
              type="number"
              min="0.01"
              step="0.01"
              placeholder="200"
              value={newDefaultRiskAmount}
              onChange={(e) => onDefaultRiskAmountChange(e.target.value)}
            />
            <p style={{ ...mutedText, margin: "6px 0 0", fontSize: 12 }}>
              If 1R = $200 and a trade makes $400, that trade is +2R.
            </p>
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
                  <div
                    style={{
                      borderRadius: 18,
                      border: selected
                        ? "1px solid rgba(52,211,153,0.75)"
                        : "1px solid rgba(148,163,184,0.18)",
                      background: selected
                        ? "linear-gradient(135deg, rgba(16,185,129,0.18), rgba(34,197,94,0.1))"
                        : "rgba(15,23,42,0.52)",
                      padding: 12,
                      minWidth: 280,
                    }}
                  >
                    <button
                      type="button"
                      onClick={() => onSelectAccount(account.id)}
                      style={{
                        ...accountButtonStyle(selected),
                        width: "100%",
                        justifyContent: "flex-start",
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
                        top: 18,
                        right: 18,
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

                    <div style={{ marginTop: 10 }}>
                      <label style={{ ...label, fontSize: 12 }}>Default 1R</label>
                      <div style={{ display: "flex", gap: 8 }}>
                        <input
                          style={{ ...input, height: 38, fontSize: 13 }}
                          type="number"
                          min="0.01"
                          step="0.01"
                          placeholder="Set 1R"
                          value={riskInputs[account.id] ?? String(account.defaultRiskAmount ?? "")}
                          onChange={(event) =>
                            setRiskInputs((prev) => ({
                              ...prev,
                              [account.id]: event.target.value,
                            }))
                          }
                        />
                        <button
                          type="button"
                          onClick={() => saveRiskAmount(account)}
                          disabled={savingRiskAccountId === account.id}
                          style={{
                            ...button,
                            width: 92,
                            height: 38,
                            fontSize: 13,
                            opacity: savingRiskAccountId === account.id ? 0.6 : 1,
                          }}
                        >
                          {savingRiskAccountId === account.id ? "Saving" : "Set 1R"}
                        </button>
                      </div>
                      {riskMessages[account.id] ? (
                        <p
                          style={{
                            ...mutedText,
                            marginTop: 6,
                            color: riskMessages[account.id].type === "success" ? "#34d399" : "#fb7185",
                          }}
                        >
                          {riskMessages[account.id].text}
                        </p>
                      ) : (
                        <p style={{ ...mutedText, marginTop: 6 }}>
                          Set your planned dollar risk so analytics can calculate R-multiple.
                        </p>
                      )}
                    </div>
                  </div>
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

function riskErrorMessage(error: unknown) {
  const responseData = (error as { response?: { data?: unknown } })?.response?.data;

  if (typeof responseData === "string" && responseData.trim()) {
    return responseData;
  }

  if (responseData && typeof responseData === "object") {
    const data = responseData as { message?: unknown; error?: unknown };
    if (typeof data.message === "string" && data.message.trim()) {
      return data.message;
    }
    if (typeof data.error === "string" && data.error.trim()) {
      return data.error;
    }
  }

  return "Unable to update 1R. Please try again.";
}
