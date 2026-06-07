import type { SelectedAccountId } from "../types/dashboard";

export const SELECTED_ACCOUNT_STORAGE_KEY = "tradepilot_selected_account_id";

export function parseStoredSelectedAccount(value: string | null): SelectedAccountId {
  if (!value) return "";
  if (value.toUpperCase() === "ALL") return "ALL";

  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : "";
}

export function persistSelectedAccount(accountId: SelectedAccountId) {
  if (!accountId) {
    localStorage.removeItem(SELECTED_ACCOUNT_STORAGE_KEY);
    return;
  }

  localStorage.setItem(SELECTED_ACCOUNT_STORAGE_KEY, String(accountId));
}

export function clearSelectedAccount() {
  localStorage.removeItem(SELECTED_ACCOUNT_STORAGE_KEY);
}
