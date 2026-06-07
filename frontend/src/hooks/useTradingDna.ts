import { useEffect, useRef, useState } from "react";
import { getTradingDna } from "../api/tradingDnaApi";
import type { TradingAccount } from "../api/accountApi";
import type { SelectedAccountId } from "../types/dashboard";
import type { TradingDnaResponse } from "../types/tradingDna";
import { clearSelectedAccount } from "../utils/accountSelection";

const inFlightTradingDnaRequests = new Set<string>();

type UseTradingDnaParams = {
  selectedAccountId: SelectedAccountId;
  accounts: TradingAccount[];
  accountsLoading: boolean;
  days?: number;
  onInvalidAccount?: () => void;
};

export function useTradingDna({
  selectedAccountId,
  accounts,
  accountsLoading,
  days = 30,
  onInvalidAccount,
}: UseTradingDnaParams) {
  const [data, setData] = useState<TradingDnaResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");
  const recoveredForbiddenRef = useRef(false);
  const blockedAccountRef = useRef<number | null>(null);

  const normalizedSelected =
    typeof selectedAccountId === "string" && selectedAccountId.toUpperCase() === "ALL"
      ? "ALL"
      : selectedAccountId;
  const allAccounts = normalizedSelected === "ALL";
  const accountId =
    normalizedSelected && normalizedSelected !== "ALL"
      ? Number(normalizedSelected)
      : undefined;
  const accountIds = accounts.map((account) => account.id);
  const hasValidAccount =
    allAccounts || (accountId !== undefined && Number.isFinite(accountId) && accountIds.includes(accountId));
  const blockedByPreviousForbidden =
    !allAccounts && accountId !== undefined && blockedAccountRef.current === accountId;
  const canLoad =
    !accountsLoading &&
    Boolean(normalizedSelected) &&
    hasValidAccount &&
    !blockedByPreviousForbidden;
  const requestKey = allAccounts
    ? `ALL:${days}`
    : accountId !== undefined
      ? `${accountId}:${days}`
      : `NONE:${days}`;

  async function load(forceRefresh = false) {
    if (!canLoad) {
      setLoading(false);
      setRefreshing(false);
      return;
    }

    if (!allAccounts && (accountId === undefined || !accountIds.includes(accountId))) {
      return;
    }

    const inFlightKey = `${requestKey}:${forceRefresh}`;
    if (inFlightTradingDnaRequests.has(inFlightKey)) {
      return;
    }

    inFlightTradingDnaRequests.add(inFlightKey);

    if (forceRefresh) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }
    setError("");

    try {
      const requestParams = allAccounts
        ? { days, includeAllAccounts: true, forceRefresh }
        : { accountId, days, includeAllAccounts: false, forceRefresh };

      const next = await getTradingDna(
        requestParams
      );
      setData(next);
      recoveredForbiddenRef.current = false;
    } catch (err) {
      console.error(err);
      const status = (err as any)?.response?.status;
      if (status === 403) {
        if (!allAccounts && accountId !== undefined) {
          blockedAccountRef.current = accountId;
        }
        clearSelectedAccount();
        if (!recoveredForbiddenRef.current) {
          recoveredForbiddenRef.current = true;
          onInvalidAccount?.();
        }
        setError("Trading DNA could not access that account. Select one of your current accounts and try again.");
      } else {
        setError("Failed to load Trading DNA.");
      }
    } finally {
      inFlightTradingDnaRequests.delete(inFlightKey);
      setLoading(false);
      setRefreshing(false);
    }
  }

  useEffect(() => {
    setData(null);
    setError("");

    if (hasValidAccount && !blockedByPreviousForbidden) {
      blockedAccountRef.current = null;
    }

    if (!accountsLoading && normalizedSelected && !hasValidAccount) {
      clearSelectedAccount();
      onInvalidAccount?.();
      return;
    }

    void load(false);
  }, [normalizedSelected, days, accountsLoading, accountIds.join(",")]);

  return {
    data,
    loading,
    refreshing,
    error,
    canLoad,
    generate: () => load(true),
    refresh: () => load(true),
  };
}
