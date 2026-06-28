import Papa from "papaparse";

/**
 * Raw trade parsed from Tradovate CSV
 */
export type TradovateTrade = {
  id?: number;
  accountId?: number;
  accountName?: string;
  symbol: string;
  qty: number;
  buyPrice: number;
  sellPrice: number;
  pnl: number;
  boughtTimestamp: string;
  soldTimestamp: string;
  duration: string;
  side?: "LONG" | "SHORT" | "UNKNOWN";
  setup?: string;
  session?: string;
  mistakeTag?: string | null;
  notes?: string | null;
  psychologyNotes?: string | null;
  lessonLearned?: string | null;
  pnlR?: number | null;
  tradeDate?: string;
};

/**
 * Aggregated daily stats
 */
export type DailyPnl = {
  date: string;
  totalPnl: number;
  totalR?: number | null;
  cumulativePnl: number;
  tradeCount: number;
  winningTrades: number;
  losingTrades: number;
  breakevenTrades: number;
  winRate: number;
};

/**
 * CSV row structure (based on your Tradovate file)
 */
type TradovateCsvRow = {
  symbol: string;
  qty: string;
  buyPrice: string;
  sellPrice: string;
  pnl: string;
  boughtTimestamp: string;
  soldTimestamp: string;
  duration: string;
  side?: string;
  direction?: string;
  position?: string;
  openingAction?: string;
  openAction?: string;
  entryAction?: string;
  action?: string;
};

/**
 * Parse numbers safely
 */
function parseNumber(value: string): number {
  if (!value) return 0;
  return Number(value.replace(/,/g, "").trim()) || 0;
}

/**
 * Parse money (handles $(123.45) format)
 */
function parseMoney(value: string): number {
  if (!value) return 0;

  const cleaned = value
    .replace(/\$/g, "")
    .replace(/,/g, "")
    .trim();

  // Handle negative values like $(222.00)
  if (cleaned.startsWith("(") && cleaned.endsWith(")")) {
    return -Number(cleaned.replace("(", "").replace(")", ""));
  }

  return Number(cleaned) || 0;
}

/**
 * Extract YYYY-MM-DD from Tradovate timestamp
 */
function extractDate(timestamp: string): string {
  if (!timestamp) return "Unknown";

  const [datePart] = timestamp.split(" ");
  const [month, day, year] = datePart.split("/");

  return `${year}-${month.padStart(2, "0")}-${day.padStart(2, "0")}`;
}

function normalizeHeader(header: string): keyof TradovateCsvRow | string {
  const clean = header.trim().toLowerCase().replace(/[^a-z0-9]/g, "");
  const aliases: Record<string, keyof TradovateCsvRow> = {
    symbol: "symbol",
    contract: "symbol",
    instrument: "symbol",
    qty: "qty",
    quantity: "qty",
    buyprice: "buyPrice",
    boughtprice: "buyPrice",
    sellprice: "sellPrice",
    soldprice: "sellPrice",
    pnl: "pnl",
    profitloss: "pnl",
    boughttimestamp: "boughtTimestamp",
    buytimestamp: "boughtTimestamp",
    boughttime: "boughtTimestamp",
    buytime: "boughtTimestamp",
    soldtimestamp: "soldTimestamp",
    selltimestamp: "soldTimestamp",
    soldtime: "soldTimestamp",
    selltime: "soldTimestamp",
    duration: "duration",
    side: "side",
    direction: "direction",
    position: "position",
    openingaction: "openingAction",
    openaction: "openAction",
    entryaction: "entryAction",
    action: "action",
  };
  return aliases[clean] ?? header;
}

function sideFromText(value?: string): "LONG" | "SHORT" | "UNKNOWN" | undefined {
  if (!value) return undefined;
  const clean = value.trim().toUpperCase();
  if (["LONG", "BUY", "BOUGHT", "BOT", "B"].includes(clean)) return "LONG";
  if (["SHORT", "SELL", "SOLD", "SLD", "S"].includes(clean)) return "SHORT";
  if (clean === "UNKNOWN") return "UNKNOWN";
  return undefined;
}

function inferSideFromCsvRow(row: TradovateCsvRow): "LONG" | "SHORT" | "UNKNOWN" | undefined {
  return sideFromText(row.side)
    ?? sideFromText(row.direction)
    ?? sideFromText(row.position)
    ?? sideFromText(row.openingAction)
    ?? sideFromText(row.openAction)
    ?? sideFromText(row.entryAction)
    ?? sideFromText(row.action);
}

/**
 * Main CSV parser
 */
export function parseTradovateCsv(file: File): Promise<{
  trades: TradovateTrade[];
  dailyPnl: DailyPnl[];
}> {
  return new Promise((resolve, reject) => {
    Papa.parse<TradovateCsvRow>(file, {
      header: true,
      skipEmptyLines: true,
      transformHeader: normalizeHeader,
      complete: (results) => {
        // STEP 1: Convert CSV rows → trades
        const trades: TradovateTrade[] = results.data.map((row) => ({
          symbol: row.symbol,
          qty: parseNumber(row.qty),
          buyPrice: parseNumber(row.buyPrice),
          sellPrice: parseNumber(row.sellPrice),
          pnl: parseMoney(row.pnl),
          boughtTimestamp: row.boughtTimestamp,
          soldTimestamp: row.soldTimestamp,
          duration: row.duration,
          side: inferSideFromCsvRow(row),
        }));

        // STEP 2: Aggregate daily stats
        const dailyMap = new Map<string, DailyPnl>();

        trades.forEach((trade) => {
          const date = extractDate(trade.soldTimestamp);

          if (!dailyMap.has(date)) {
            dailyMap.set(date, {
              date,
              totalPnl: 0,
              cumulativePnl: 0,
              tradeCount: 0,
              winningTrades: 0,
              losingTrades: 0,
              breakevenTrades: 0,
              winRate: 0,
            });
          }

          const day = dailyMap.get(date)!;

          day.totalPnl += trade.pnl;
          day.tradeCount += 1;

          if (trade.pnl > 0) day.winningTrades += 1;
          else if (trade.pnl < 0) day.losingTrades += 1;
          else day.breakevenTrades += 1;
        });

        // STEP 3: Sort + compute cumulative + win rate
        let cumulativePnl = 0;

        const dailyPnl = Array.from(dailyMap.values())
          .sort((a, b) => a.date.localeCompare(b.date))
          .map((day) => {
            cumulativePnl += day.totalPnl;

            return {
              ...day,
              cumulativePnl,
              winRate:
                day.tradeCount > 0
                  ? Number(
                    ((day.winningTrades / day.tradeCount) * 100).toFixed(2)
                  )
                  : 0,
            };
          });

        resolve({ trades, dailyPnl });
      },
      error: reject,
    });
  });
}
