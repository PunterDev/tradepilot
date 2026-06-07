import { api } from "./axios";
import type { TradovateTrade } from "../utils/tradovateCsv";
import type { PageParams, PagedResponse } from "./pagination";

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

function toIsoDate(timestamp: string): string {
  const [datePart] = timestamp.split(" ");
  const [month, day, year] = datePart.split("/");
  return `${year}-${month.padStart(2, "0")}-${day.padStart(2, "0")}`;
}

function toIsoDateTime(timestamp: string): string {
  const [datePart, timePart, meridian] = timestamp.split(" ");
  const [month, day, year] = datePart.split("/");
  let [hour, minute, second] = timePart.split(":").map(Number);

  if (meridian === "PM" && hour !== 12) hour += 12;
  if (meridian === "AM" && hour === 12) hour = 0;

  return `${year}-${month.padStart(2, "0")}-${day.padStart(2, "0")}T${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}:${String(second).padStart(2, "0")}`;
}

function buildExternalTradeKey(accountId: number, trade: TradovateTrade): string {
  return [
    accountId,
    trade.symbol,
    trade.boughtTimestamp,
    trade.soldTimestamp,
    trade.qty,
    trade.buyPrice,
    trade.sellPrice,
    trade.pnl,
  ].join("|");
}

export async function importTradesFromCsv(
  accountId: number,
  fileName: string,
  trades: TradovateTrade[]
) {
  const payload = trades.map((trade) => ({
    accountId,
    fileName,
    externalTradeKey: buildExternalTradeKey(accountId, trade),
    symbol: trade.symbol,
    market: "FUTURES",
    tradeDate: toIsoDate(trade.soldTimestamp),
    entryTime: toIsoDateTime(trade.boughtTimestamp),
    exitTime: toIsoDateTime(trade.soldTimestamp),
    side: trade.sellPrice >= trade.buyPrice ? "LONG" : "SHORT",
    setup: "OTHER",
    session: "NY_AM",
    entryPrice: trade.buyPrice,
    exitPrice: trade.sellPrice,
    quantity: trade.qty,
    pnlAmount: trade.pnl,
    pnlR: null,
    outcome: trade.pnl > 0 ? "WIN" : trade.pnl < 0 ? "LOSS" : "BE",
  }));

  const res = await api.post("/trades/import", payload);
  return res.data;
}
