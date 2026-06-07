import { api } from "./axios";
import type { TradingDnaResponse } from "../types/tradingDna";

export type TradingDnaParams = {
  accountId?: number;
  days?: number;
  includeAllAccounts?: boolean;
  forceRefresh?: boolean;
};

export async function getTradingDna(
  params: TradingDnaParams
): Promise<TradingDnaResponse> {
  const requestParams: TradingDnaParams = {
    days: params.days,
    forceRefresh: params.forceRefresh,
  };

  if (params.includeAllAccounts) {
    requestParams.includeAllAccounts = true;
  } else if (Number.isFinite(params.accountId)) {
    requestParams.accountId = params.accountId;
    requestParams.includeAllAccounts = false;
  }

  const response = await api.get<TradingDnaResponse>("/ai/trading-dna", {
    params: requestParams,
  });
  return response.data;
}
