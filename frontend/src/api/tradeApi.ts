import { api } from "./axios";
import type { PageParams, PagedResponse } from "./pagination";

export type TradeImportFormat = "TRADOVATE" | "TOPSTEP";

export type TradeImportPreviewTrade = {
  symbol: string;
  side: string;
  tradeDate: string;
  entryTime: string;
  exitTime: string;
  quantity: number;
  entryPrice: number | string;
  exitPrice: number | string;
  pnlAmount: number | string;
  originalBrokerSymbol: string | null;
  contractSymbol: string | null;
  externalTradeKey: string;
  sourceExecutionIds: string[];
  duplicate: boolean;
};

export type TradeImportPreviewResponse = {
  detectedFormat: TradeImportFormat;
  sourceAccountNames: string[];
  totalRows: number;
  filledRows: number;
  cancelledRowsIgnored: number;
  invalidRows: number;
  completedTrades: number;
  duplicateTrades: number;
  incompletePositions: number;
  warnings: string[];
  trades: TradeImportPreviewTrade[];
};

export type TradeImportSaveResponse = {
  preview: TradeImportPreviewResponse;
  savedTrades: any[];
  savedCount: number;
};

export async function getTrades(params: PageParams = {}) {
  const res = await api.get("/trades", { params: { size: 25, ...params } });
  return res.data;
}

export async function getTradesByAccount(accountId: number, params: PageParams = {}) {
  const res = await api.get<PagedResponse<any>>("/trades", {
    params: {
      accountId,
      page: 0,
      size: 100,
      sortBy: "tradeDate",
      sortDir: "asc",
      ...params,
    },
  });

  return res.data.content;
}

export async function getAllTrades(params: PageParams = {}) {
  const res = await api.get<PagedResponse<any>>("/trades", {
    params: {
      page: 0,
      size: 100,
      sortBy: "tradeDate",
      sortDir: "asc",
      ...params,
    },
  });

  return res.data.content;
}

export async function previewCsvImport(accountId: number, file: File) {
  const formData = new FormData();
  formData.append("file", file);
  const res = await api.post<TradeImportPreviewResponse>("/trades/import/preview", formData, {
    params: { accountId },
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data;
}

export async function saveCsvImport(accountId: number, file: File) {
  const formData = new FormData();
  formData.append("file", file);
  const res = await api.post<TradeImportSaveResponse>("/trades/import/csv", formData, {
    params: { accountId },
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data;
}
