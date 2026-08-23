import { api } from "./api";

export type TradeReplayStatus =
  | "PENDING"
  | "WAITING_FOR_MARKET_DATA"
  | "FETCHING_DATA"
  | "ANALYZING"
  | "READY"
  | "PROVIDER_NOT_CONFIGURED"
  | "UNSUPPORTED_INSTRUMENT"
  | "CONTRACT_RESOLUTION_FAILED"
  | "NOT_ENOUGH_DATA"
  | "DEMO_REPLAY_UNAVAILABLE"
  | "FAILED";

export type ReplayCandle = {
  timestamp: string;
  open: number | string;
  high: number | string;
  low: number | string;
  close: number | string;
  volume: number | string;
};

export type ReplayMarker = {
  type: "ENTRY" | "PARTIAL_EXIT" | "FINAL_EXIT" | string;
  timestamp: string;
  price: number | string | null;
  quantity: number | null;
  label: string;
};

export type ReplayInsight = {
  id: number;
  type: string;
  eventTimestamp: string | null;
  eventPrice: number | string | null;
  severity: string | null;
  title: string;
  explanation: string | null;
  supportingMetrics: Record<string, unknown>;
  aiExplanation: string | null;
  displayOrder: number;
  primary: boolean;
};

export type TradeReplayResponse = {
  tradeId: number;
  replayId: number | null;
  status: TradeReplayStatus | null;
  eligibleAt: string | null;
  originalBrokerSymbol: string | null;
  resolvedProviderSymbol: string | null;
  provider: string | null;
  candleInterval: string | null;
  windowStart: string | null;
  windowEnd: string | null;
  generatedAt: string | null;
  failureCode: string | null;
  message: string | null;
  markers: ReplayMarker[];
  candles: ReplayCandle[];
  metrics: Record<string, unknown>;
  insights: ReplayInsight[];
  aiSummary: string | null;
  reflectionQuestion: string | null;
  dataQualityWarnings: string[];
};

export type ContractCandidate = {
  label: string;
  rootInstrument: string;
  normalizedSymbol: string;
  monthCode: string;
  month: number;
  year: number;
  mostLikely: boolean;
};

export type ContractCandidatesResponse = {
  tradeId: number;
  rootInstrument: string | null;
  originalBrokerSymbol: string | null;
  candidates: ContractCandidate[];
};

export async function getTradeReplay(tradeId: number) {
  const response = await api.get<TradeReplayResponse>(`/v1/trades/${tradeId}/replay`);
  return response.data;
}

export async function generateTradeReplay(tradeId: number) {
  const response = await api.post<TradeReplayResponse>(`/v1/trades/${tradeId}/replay/generate`);
  return response.data;
}

export async function regenerateTradeReplay(tradeId: number) {
  const response = await api.post<TradeReplayResponse>(`/v1/trades/${tradeId}/replay/regenerate`);
  return response.data;
}

export async function getReplayContractCandidates(tradeId: number) {
  const response = await api.get<ContractCandidatesResponse>(`/v1/trades/${tradeId}/replay/contract-candidates`);
  return response.data;
}

export async function confirmReplayContract(tradeId: number, contractSymbol: string) {
  const response = await api.put<TradeReplayResponse>(`/v1/trades/${tradeId}/replay/contract`, { contractSymbol });
  return response.data;
}
