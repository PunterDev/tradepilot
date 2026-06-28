import { useEffect, useRef, useState } from "react";
import axios from "axios";
import { getTradingDna } from "../api/tradingDnaApi";
import type { TradingAccount } from "../api/accountApi";
import type { SelectedAccountId } from "../types/dashboard";
import type { TradingDnaResponse } from "../types/tradingDna";
import { clearSelectedAccount } from "../utils/accountSelection";

const inFlightTradingDnaRequests = new Set<string>();
const TRADING_DNA_LIMIT_FALLBACK_MESSAGE =
  "Trading DNA limit reached. No cached analysis is available for the current trade data.";

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
  const [limitReached, setLimitReached] = useState(false);
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
    setLimitReached(false);

    try {
      const requestParams = allAccounts
        ? { days, includeAllAccounts: true, forceRefresh }
        : { accountId, days, includeAllAccounts: false, forceRefresh };

      const next = await getTradingDna(
        requestParams
      );
      setData(next);
      setLimitReached(Boolean(next.limitReached));
      recoveredForbiddenRef.current = false;
    } catch (err) {
      const status = axios.isAxiosError(err) ? err.response?.status : undefined;
      if (status === 429) {
        const limitResponse = normalizeLimitReachedResponse(
          axios.isAxiosError(err) ? err.response?.data : undefined,
          accountId,
          days,
          allAccounts
        );
        setData(limitResponse);
        setLimitReached(true);
        setError("");
        console.info("Trading DNA limit reached.", {
          accountId,
          includeAllAccounts: allAccounts,
          hasCachedAnalysis: limitResponse.hasCachedAnalysis ?? limitResponse.cached,
        });
        return;
      }

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
        console.error(err);
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
    setLimitReached(false);

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
    limitReached,
    canLoad,
    generate: () => load(true),
    refresh: () => load(true),
  };
}

function normalizeLimitReachedResponse(
  responseData: unknown,
  accountId: number | undefined,
  days: number,
  includeAllAccounts: boolean
): TradingDnaResponse {
  const payload = isRecord(responseData) ? responseData : {};
  const message = stringValue(payload.message)
    ?? stringValue(payload.error)
    ?? TRADING_DNA_LIMIT_FALLBACK_MESSAGE;
  const cached = booleanValue(payload.cached) ?? booleanValue(payload.hasCachedAnalysis) ?? false;
  const status = stringValue(payload.status) ?? "LIMIT_REACHED";

  return {
    id: numberValue(payload.id),
    accountId: numberValue(payload.accountId) ?? accountId ?? null,
    days: numberValue(payload.days) ?? days,
    includeAllAccounts: booleanValue(payload.includeAllAccounts) ?? includeAllAccounts,
    status,
    cached,
    limitReached: true,
    aiGenerated: booleanValue(payload.aiGenerated) ?? false,
    noTrades: booleanValue(payload.noTrades) ?? false,
    limitedData: booleanValue(payload.limitedData) ?? false,
    creditsConsumed: numberValue(payload.creditsConsumed) ?? 0,
    code: stringValue(payload.code) ?? "LIMIT_REACHED",
    resetDate: stringValue(payload.resetDate),
    hasCachedAnalysis: booleanValue(payload.hasCachedAnalysis) ?? cached,
    message,
    totalTrades: numberValue(payload.totalTrades) ?? 0,
    lastGeneratedAt: stringValue(payload.lastGeneratedAt),
    strengths: stringArrayValue(payload.strengths),
    weaknesses: stringArrayValue(payload.weaknesses),
    emotionalPatterns: stringArrayValue(payload.emotionalPatterns),
    riskPatterns: stringArrayValue(payload.riskPatterns),
    bestConditions: stringArrayValue(payload.bestConditions),
    worstConditions: stringArrayValue(payload.worstConditions),
    recurringMistakes: stringArrayValue(payload.recurringMistakes),
    improvementTrend: stringValue(payload.improvementTrend) ?? "",
    consistencyScore: numberValue(payload.consistencyScore) ?? 0,
    disciplineScore: numberValue(payload.disciplineScore) ?? 0,
    executionScore: numberValue(payload.executionScore) ?? 0,
    patienceScore: numberValue(payload.patienceScore) ?? 0,
    confidenceSummary: stringValue(payload.confidenceSummary) ?? "",
    coachingSummary: stringValue(payload.coachingSummary) ?? message,
    timeline: Array.isArray(payload.timeline) ? payload.timeline as TradingDnaResponse["timeline"] : [],
  };
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function stringValue(value: unknown) {
  return typeof value === "string" && value.trim() ? value : undefined;
}

function numberValue(value: unknown) {
  return typeof value === "number" && Number.isFinite(value) ? value : undefined;
}

function booleanValue(value: unknown) {
  return typeof value === "boolean" ? value : undefined;
}

function stringArrayValue(value: unknown) {
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === "string") : [];
}
