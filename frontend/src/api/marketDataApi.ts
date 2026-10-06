import { api } from "./axios";

export type MarketInstrument = "NQ" | "ES";

export type MarketDataRequest = {
  instrument: MarketInstrument;
  startDate: string;
  endDate: string;
};

export type MarketDataEstimate = MarketDataRequest & {
  continuousSymbol: string;
  estimatedCostUsd: number;
  requestedDays: number;
  missingChunks: number;
};

export type MarketDataCoverage = {
  instrument: MarketInstrument;
  continuousSymbol: string;
  timeframe: string;
  firstBar?: string | null;
  lastBar?: string | null;
  barCount: number;
};

export type MarketDataDownload = {
  instrument: MarketInstrument;
  continuousSymbol: string;
  requestedStartDate: string;
  requestedEndDate: string;
  downloadedBars: number;
  skippedExistingBars: number;
  insertedBars: number;
  coverageStart?: string | null;
  coverageEnd?: string | null;
  status: string;
};

export async function getMarketDataCoverage(): Promise<MarketDataCoverage[]> {
  const response = await api.get<MarketDataCoverage[]>("/admin/market-data/coverage");
  return response.data;
}

export async function estimateMarketData(request: MarketDataRequest): Promise<MarketDataEstimate> {
  const response = await api.post<MarketDataEstimate>("/admin/market-data/estimate", request);
  return response.data;
}

export async function downloadMarketData(request: MarketDataRequest): Promise<MarketDataDownload> {
  const response = await api.post<MarketDataDownload>("/admin/market-data/download", request);
  return response.data;
}
