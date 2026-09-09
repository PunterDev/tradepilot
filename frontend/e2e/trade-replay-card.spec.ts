import { expect, test } from "@playwright/test";
import { isTradeReplayProcessingStatus } from "../src/components/trade-replay/tradeReplayStatus";
import type { TradeReplayStatus } from "../src/api/tradeReplayApi";

test("trade replay regenerate is disabled only for active processing statuses", () => {
  const activeStatuses: Array<TradeReplayStatus | null> = ["PENDING", "PROCESSING", "FETCHING_DATA", "ANALYZING"];
  const retryableStatuses: TradeReplayStatus[] = [
    "FAILED",
    "READY",
    "WAITING_FOR_MARKET_DATA",
    "PROVIDER_NOT_CONFIGURED",
    "CONTRACT_RESOLUTION_FAILED",
    "NOT_ENOUGH_DATA",
    "UNSUPPORTED_INSTRUMENT",
    "DEMO_REPLAY_UNAVAILABLE",
  ];

  for (const status of activeStatuses) {
    expect(isTradeReplayProcessingStatus(status)).toBe(true);
  }

  for (const status of retryableStatuses) {
    expect(isTradeReplayProcessingStatus(status)).toBe(false);
  }
});
