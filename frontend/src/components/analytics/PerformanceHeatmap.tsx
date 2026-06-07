import { useMemo } from "react";
import type { HeatmapCellDto } from "../../types/analytics";
import type { TradovateTrade } from "../../utils/tradovateCsv";
import {
  DAY_ORDER,
  SESSION_ORDER,
  formatDayLabel,
  formatMoney,
  formatSessionLabel,
  getDayOrder,
  getSessionOrder,
  normalizeSessionBucket,
  normalizeWeekdayKey,
} from "../../utils/analyticsUtils";

type Props = {
  data: HeatmapCellDto[];
  trades?: TradovateTrade[];
  onTradeSelect?: (tradeId: number) => void;
};

function getCellBackground(pnl: number) {
  if (pnl >= 1000) return "rgba(34,197,94,0.42)";
  if (pnl > 0) return "rgba(34,197,94,0.22)";
  if (pnl <= -1000) return "rgba(239,68,68,0.42)";
  if (pnl < 0) return "rgba(239,68,68,0.22)";
  return "rgba(148,163,184,0.08)";
}

function numeric(value: number | string | undefined | null) {
  const parsed = Number(value ?? 0);
  return Number.isFinite(parsed) ? parsed : 0;
}

function cellKey(day: string, session: string) {
  return `${day}::${session}`;
}

function buildCellMap(data: HeatmapCellDto[]) {
  const map = new Map<string, HeatmapCellDto>();

  data.forEach((cell) => {
    const day = normalizeWeekdayKey(cell.dayOfWeek);
    const session = normalizeSessionBucket(cell.sessionBucket ?? cell.session);
    if (day === "UNKNOWN") return;

    const key = cellKey(day, session);
    const existing = map.get(key);
    const tradeCount = numeric(cell.tradeCount);
    const totalPnl = numeric(cell.totalPnl);
    const winRate = numeric(cell.winRate);

    if (!existing) {
      map.set(key, {
        dayOfWeek: day,
        session,
        tradeCount,
        totalPnl,
        winRate,
      });
      return;
    }

    const existingTrades = numeric(existing.tradeCount);
    const nextTrades = existingTrades + tradeCount;
    map.set(key, {
      dayOfWeek: day,
      session,
      tradeCount: nextTrades,
      totalPnl: numeric(existing.totalPnl) + totalPnl,
      winRate:
        nextTrades === 0
          ? 0
          : (numeric(existing.winRate) * existingTrades + winRate * tradeCount) /
            nextTrades,
    });
  });

  return map;
}

export default function PerformanceHeatmap({
  data,
  trades = [],
  onTradeSelect,
}: Props) {
  const sortedDays = [...DAY_ORDER].sort(
    (a, b) => getDayOrder(a) - getDayOrder(b)
  );

  const sortedSessions = [...SESSION_ORDER]
    .filter((session) => session !== "UNKNOWN")
    .sort((a, b) => getSessionOrder(a) - getSessionOrder(b));
  const cellMap = useMemo(() => buildCellMap(data), [data]);
  const hasAnyData = [...cellMap.values()].some(
    (cell) => numeric(cell.tradeCount) > 0
  );

  return (
    <div
      style={{
        marginTop: 24,
        padding: 20,
        borderRadius: 20,
        background: "rgba(15,23,42,0.92)",
        border: "1px solid rgba(148,163,184,0.15)",
        overflowX: "auto",
      }}
    >
      <h3 style={{ color: "#e5e7eb", marginTop: 0 }}>
        Performance Heatmap
      </h3>

      <p style={{ color: "#94a3b8", marginTop: 4 }}>
        PnL by day of week and trading session.
      </p>

      {!hasAnyData && (
        <div
          style={{
            marginTop: 18,
            padding: 18,
            borderRadius: 16,
            background: "rgba(2,6,23,0.55)",
            border: "1px dashed rgba(148,163,184,0.2)",
            color: "#94a3b8",
            fontWeight: 700,
          }}
        >
          No heatmap data yet. Import trades to see performance by weekday and session.
        </div>
      )}

      {hasAnyData && <div
        style={{
          marginTop: 18,
          display: "grid",
          gridTemplateColumns: `140px repeat(${sortedSessions.length}, minmax(145px, 1fr))`,
          gap: 10,
          minWidth: 980,
        }}
      >
        <div />

        {sortedSessions.map((session) => (
          <div
            key={session}
            style={{
              color: "#93c5fd",
              fontSize: 13,
              fontWeight: 800,
              textAlign: "center",
              padding: "8px 6px",
            }}
          >
            {formatSessionLabel(session)}
          </div>
        ))}

        {sortedDays.map((day) => (
          <div key={day} style={{ display: "contents" }}>
            <div
              style={{
                color: "#e5e7eb",
                fontWeight: 900,
                padding: "18px 10px",
                borderRadius: 12,
                background: "rgba(2,6,23,0.65)",
                border: "1px solid rgba(148,163,184,0.08)",
              }}
            >
              {formatDayLabel(day)}
            </div>

            {sortedSessions.map((session) => {
              const cell = cellMap.get(cellKey(day, session));
              const pnl = numeric(cell?.totalPnl);
              const tradeCount = numeric(cell?.tradeCount);
              const winRate = numeric(cell?.winRate);
              const hasData = tradeCount > 0;
              const matchingTrade = trades.find((trade) => {
                if (!trade.id || normalizeSessionBucket(trade.session) !== session) return false;
                const tradeDay = new Date(trade.tradeDate ?? trade.soldTimestamp).toLocaleDateString("en-US", { weekday: "long" }).toUpperCase();
                return normalizeWeekdayKey(tradeDay) === day;
              });

              return (
                <div
                  key={`${day}-${session}`}
                  onClick={() => matchingTrade?.id && onTradeSelect?.(matchingTrade.id)}
                  title={matchingTrade ? "Open a trade from this heatmap cell" : `${formatDayLabel(day)} · ${formatSessionLabel(session)}`}
                  style={{
                    minHeight: 92,
                    padding: 12,
                    borderRadius: 14,
                    background: hasData
                      ? getCellBackground(pnl)
                      : "rgba(15,23,42,0.55)",
                    border: hasData
                      ? "1px solid rgba(148,163,184,0.16)"
                      : "1px dashed rgba(148,163,184,0.12)",
                    opacity: hasData ? 1 : 0.45,
                    cursor: matchingTrade ? "pointer" : "default",
                  }}
                >
                  {hasData ? (
                    <>
                      <div
                        style={{
                          fontSize: 20,
                          fontWeight: 900,
                          color: pnl >= 0 ? "#bbf7d0" : "#fecaca",
                        }}
                      >
                        {formatMoney(pnl)}
                      </div>

                      <div
                        style={{
                          marginTop: 6,
                          color: "#cbd5e1",
                          fontSize: 13,
                        }}
                      >
                        {tradeCount} trades
                      </div>

                      <div
                        style={{
                          color: "#94a3b8",
                          fontSize: 12,
                        }}
                      >
                        WR {winRate.toFixed(1)}%
                      </div>
                    </>
                  ) : (
                    <div
                      style={{
                        color: "#64748b",
                        fontSize: 13,
                        fontWeight: 700,
                      }}
                    >
                      No trades
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        ))}
      </div>}
    </div>
  );
}
