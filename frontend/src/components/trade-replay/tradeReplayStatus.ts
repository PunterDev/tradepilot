import type { TradeReplayStatus } from "../../api/tradeReplayApi";

export const processingStatuses: Array<TradeReplayStatus | null> = ["PENDING", "PROCESSING", "FETCHING_DATA", "ANALYZING"];

export function isTradeReplayProcessingStatus(status: TradeReplayStatus | null | undefined) {
  return processingStatuses.includes(status ?? null);
}
