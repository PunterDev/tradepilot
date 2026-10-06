import type { BacktestTrade } from "../../../api/backtestApi";

export function reviewQueue(trades: BacktestTrade[], limit = 16) {
  const selected: BacktestTrade[] = [], seen = new Set<string>();
  const candidate = (trade: BacktestTrade) => {
    const metadata = parse(trade.setupMetadataJson);
    return [trade.direction, weekday(trade.tradeDate), trade.tradeDate.slice(0, 7), outcome(trade.modelAR), outcome(trade.modelBR), String(metadata.runnerObjectiveType ?? "NONE"), String(metadata.targetR ?? "-")].join("|");
  };
  for (const trade of [...trades].sort((a, b) => a.entryTime.localeCompare(b.entryTime))) {
    const key = candidate(trade);
    if (!seen.has(key) || selected.length < 4) { selected.push(trade); seen.add(key); }
    if (selected.length === limit) return selected;
  }
  return selected;
}
function parse(value: string) { try { return JSON.parse(value) as Record<string, unknown>; } catch { return {}; } }
function outcome(value: number) { return value > 0 ? "WIN" : value < 0 ? "LOSS" : "BE"; }
function weekday(date: string) { return new Date(`${date}T12:00:00Z`).toLocaleDateString("en-US", { weekday: "long", timeZone: "UTC" }); }
