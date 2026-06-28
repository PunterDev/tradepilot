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
  const parsed = parseTradovateTimestamp(timestamp);
  if (parsed) return formatIsoDate(parsed);

  const [datePart] = timestamp.split(" ");
  const [month, day, year] = datePart.split("/");
  return `${year}-${month.padStart(2, "0")}-${day.padStart(2, "0")}`;
}

function toIsoDateTime(timestamp: string): string {
  const parsed = parseTradovateTimestamp(timestamp);
  if (!parsed) return timestamp;
  return `${formatIsoDate(parsed)}T${String(parsed.hour).padStart(2, "0")}:${String(parsed.minute).padStart(2, "0")}:${String(parsed.second).padStart(2, "0")}`;
}

function timestampMillis(timestamp: string): number | null {
  const parsed = parseTradovateTimestamp(timestamp);
  if (!parsed) return null;

  return new Date(parsed.year, parsed.month - 1, parsed.day, parsed.hour, parsed.minute, parsed.second).getTime();
}

type ParsedTradovateTimestamp = {
  year: number;
  month: number;
  day: number;
  hour: number;
  minute: number;
  second: number;
};

function parseTradovateTimestamp(timestamp: string): ParsedTradovateTimestamp | null {
  if (!timestamp) return null;
  const clean = timestamp.trim();
  const match = clean.match(/^(\d{1,2})\/(\d{1,2})\/(\d{2,4})\s+(\d{1,2}):(\d{2})(?::(\d{2}))?\s*([AP]M)$/i);
  if (match) {
    const [, monthText, dayText, yearText, hourText, minuteText, secondText, meridianText] = match;
    const month = Number(monthText);
    const day = Number(dayText);
    const year = normalizeYear(Number(yearText));
    let hour = Number(hourText);
    const minute = Number(minuteText);
    const second = secondText == null ? 0 : Number(secondText);
    const meridian = meridianText.toUpperCase();
    if (meridian === "PM" && hour !== 12) hour += 12;
    if (meridian === "AM" && hour === 12) hour = 0;
    if ([month, day, year, hour, minute, second].some((value) => Number.isNaN(value))) return null;
    return { year, month, day, hour, minute, second };
  }

  const parsedDate = new Date(clean);
  if (!Number.isNaN(parsedDate.getTime())) {
    return {
      year: parsedDate.getFullYear(),
      month: parsedDate.getMonth() + 1,
      day: parsedDate.getDate(),
      hour: parsedDate.getHours(),
      minute: parsedDate.getMinutes(),
      second: parsedDate.getSeconds(),
    };
  }

  return null;
}

function normalizeYear(year: number) {
  return year < 100 ? 2000 + year : year;
}

function formatIsoDate(parsed: ParsedTradovateTimestamp) {
  return `${parsed.year}-${String(parsed.month).padStart(2, "0")}-${String(parsed.day).padStart(2, "0")}`;
}

function inferTradovateSide(trade: TradovateTrade): "LONG" | "SHORT" | "UNKNOWN" {
  if (trade.side === "LONG" || trade.side === "SHORT") return trade.side;

  const boughtAt = timestampMillis(trade.boughtTimestamp);
  const soldAt = timestampMillis(trade.soldTimestamp);
  if (boughtAt == null || soldAt == null || boughtAt === soldAt) return "UNKNOWN";

  return boughtAt < soldAt ? "LONG" : "SHORT";
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
  const payload = trades.map((trade) => {
    const side = inferTradovateSide(trade);
    const isShort = side === "SHORT";
    const entryTimestamp = isShort ? trade.soldTimestamp : trade.boughtTimestamp;
    const exitTimestamp = isShort ? trade.boughtTimestamp : trade.soldTimestamp;

    return {
      accountId,
      fileName,
      externalTradeKey: buildExternalTradeKey(accountId, trade),
      symbol: trade.symbol,
      market: "FUTURES",
      tradeDate: toIsoDate(exitTimestamp),
      entryTime: toIsoDateTime(entryTimestamp),
      exitTime: toIsoDateTime(exitTimestamp),
      side,
      setup: "OTHER",
      session: "NY_AM",
      entryPrice: isShort ? trade.sellPrice : trade.buyPrice,
      exitPrice: isShort ? trade.buyPrice : trade.sellPrice,
      quantity: trade.qty,
      pnlAmount: trade.pnl,
      pnlR: null,
      outcome: trade.pnl > 0 ? "WIN" : trade.pnl < 0 ? "LOSS" : "BE",
    };
  });

  const res = await api.post("/trades/import", payload);
  return res.data;
}
