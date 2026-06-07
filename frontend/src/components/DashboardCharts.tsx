import { useState } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { DashboardStats } from "../api/statsApi";
import BreakdownSection from "./BreakdownSection";
import StatCard from "./StatCard";
import type { DailyPnl } from "../utils/dashboardUtils";
import { formatCurrency } from "../utils/dashboardUtils";
import type { TradovateTrade } from "../utils/tradovateCsv";
import {
  button,
  card,
  chartGrid,
  sectionTitle,
  statsGrid,
  statsGridSmall,
  table,
  tableCard,
  td,
  th,
  tooltipStyle,
} from "../styles/dashboardStyles";

type DashboardMetrics = {
  totalPnl: number;
  totalTrades: number;
  winningTrades: number;
  losingTrades: number;
  breakevenTrades: number;
  averageR: number;
  overallWinRate: number;
  winningDays: number;
  losingDays: number;
  worstDay: number;
  currentWinStreak: number;
  currentLossStreak: number;
  bestWinStreak: number;
  worstLossStreak: number;
};

type DashboardChartsProps = {
  dailyPnl: DailyPnl[];
  trades: TradovateTrade[];
  dashboardStats: DashboardStats | null;
  loadingStats: boolean;
  metrics: DashboardMetrics;
};

export default function DashboardCharts({
  dailyPnl,
  trades,
  dashboardStats,
  loadingStats,
  metrics,
}: DashboardChartsProps) {
  const [showTradesPreview, setShowTradesPreview] = useState(false);

  return (
    <>
      <div style={statsGrid}>
        <StatCard
          title={loadingStats ? "Loading PnL" : "Total PnL"}
          value={formatCurrency(metrics.totalPnl)}
          positive={metrics.totalPnl >= 0}
          negative={metrics.totalPnl < 0}
        />

        <StatCard
          title="Win Rate"
          value={`${metrics.overallWinRate}%`}
          positive={metrics.overallWinRate >= 50}
        />

        <StatCard
          title="Total Trades"
          value={String(metrics.totalTrades)}
        />

        <StatCard
          title="Trading Days"
          value={String(dailyPnl.length)}
        />

        <StatCard
          title="Winning Days"
          value={String(metrics.winningDays)}
          positive
        />

        <StatCard
          title="Losing Days"
          value={String(metrics.losingDays)}
          negative
        />
      </div>

      <div style={statsGridSmall}>
        <StatCard
          title="Winning Trades"
          value={String(metrics.winningTrades)}
          positive
        />

        <StatCard
          title="Losing Trades"
          value={String(metrics.losingTrades)}
          negative
        />

        <StatCard
          title="Breakeven Trades"
          value={String(metrics.breakevenTrades)}
        />

        <StatCard
          title="Average R"
          value={`${metrics.averageR.toFixed(2)}R`}
          positive={metrics.averageR > 0}
          negative={metrics.averageR < 0}
        />
      </div>

      <div
        style={{
          ...statsGridSmall,
          gridTemplateColumns: "repeat(5, minmax(0, 1fr))",
        }}
      >
        <StatCard
          title="Worst Day"
          value={formatCurrency(metrics.worstDay)}
          negative={metrics.worstDay < 0}
        />

        <StatCard
          title="Current Win Streak"
          value={`${metrics.currentWinStreak} days`}
          positive
        />

        <StatCard
          title="Current Loss Streak"
          value={`${metrics.currentLossStreak} days`}
          negative
        />

        <StatCard
          title="Best Win Streak"
          value={`${metrics.bestWinStreak} days`}
          positive
        />

        <StatCard
          title="Worst Loss Streak"
          value={`${metrics.worstLossStreak} days`}
          negative
        />
      </div>

      {!loadingStats && metrics.totalTrades === 0 && (
        <div
          style={{
            ...card,
            marginTop: 28,
            border: "1px solid rgba(56,189,248,0.28)",
            background:
              "linear-gradient(180deg, rgba(8,47,73,0.36), rgba(15,23,42,0.84))",
          }}
        >
          <h2 style={sectionTitle}>No trades imported yet</h2>
          <p style={{ color: "#94a3b8", margin: 0, lineHeight: 1.6 }}>
            Select one trading account and import a Tradovate CSV to populate performance,
            drawdown, win rate, and calendar analytics.
          </p>
        </div>
      )}

      <div style={chartGrid}>
        <BreakdownSection
          title="Setup Breakdown"
          items={dashboardStats?.bySetup ?? []}
        />

        <BreakdownSection
          title="Session Breakdown"
          items={dashboardStats?.bySession ?? []}
        />

        <BreakdownSection
          title="Symbol Breakdown"
          items={dashboardStats?.bySymbol ?? []}
        />

        <BreakdownSection
          title="Weekday Breakdown"
          items={dashboardStats?.byWeekday ?? []}
        />
      </div>

      <div style={chartGrid}>
        <div style={card}>
          <h2 style={sectionTitle}>Daily PnL</h2>

          <div style={{ width: "100%", height: 320 }}>
            <ResponsiveContainer>
              <BarChart data={dailyPnl}>
                <CartesianGrid
                  stroke="rgba(148,163,184,0.16)"
                  strokeDasharray="4 4"
                />

                <XAxis
                  dataKey="date"
                  stroke="#64748b"
                  tick={{ fill: "#94a3b8", fontSize: 12 }}
                />

                <YAxis
                  stroke="#64748b"
                  tick={{ fill: "#94a3b8", fontSize: 12 }}
                />

                <Tooltip
                  formatter={(value) =>
                    formatCurrency(Number(value))
                  }
                  contentStyle={tooltipStyle}
                />

                <Bar
                  dataKey="totalPnl"
                  fill="#38bdf8"
                  radius={[8, 8, 0, 0]}
                />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div style={card}>
          <h2 style={sectionTitle}>Equity Curve</h2>

          <div style={{ width: "100%", height: 320 }}>
            <ResponsiveContainer>
              <LineChart data={dailyPnl}>
                <CartesianGrid
                  stroke="rgba(148,163,184,0.16)"
                  strokeDasharray="4 4"
                />

                <XAxis
                  dataKey="date"
                  stroke="#64748b"
                  tick={{ fill: "#94a3b8", fontSize: 12 }}
                />

                <YAxis
                  stroke="#64748b"
                  tick={{ fill: "#94a3b8", fontSize: 12 }}
                />

                <Tooltip
                  formatter={(value) =>
                    formatCurrency(Number(value))
                  }
                  contentStyle={tooltipStyle}
                />

                <Line
                  type="monotone"
                  dataKey="cumulativePnl"
                  stroke="#34d399"
                  strokeWidth={3}
                  dot={{ r: 3 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      <div style={{ ...card, marginTop: 32 }}>
        <h2 style={sectionTitle}>Daily Win Rate</h2>

        <div style={{ width: "100%", height: 300 }}>
          <ResponsiveContainer>
            <LineChart data={dailyPnl}>
              <CartesianGrid
                stroke="rgba(148,163,184,0.16)"
                strokeDasharray="4 4"
              />

              <XAxis
                dataKey="date"
                stroke="#64748b"
                tick={{ fill: "#94a3b8", fontSize: 12 }}
              />

              <YAxis
                domain={[0, 100]}
                stroke="#64748b"
                tick={{ fill: "#94a3b8", fontSize: 12 }}
              />

              <Tooltip
                formatter={(value) =>
                  `${Number(value).toFixed(2)}%`
                }
                contentStyle={tooltipStyle}
              />

              <Line
                type="monotone"
                dataKey="winRate"
                stroke="#38bdf8"
                strokeWidth={3}
                dot={{ r: 3 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      <h2 style={{ marginTop: 40 }}>Daily PnL Table</h2>

      <div style={tableCard}>
        <table style={table}>
          <thead>
            <tr>
              <th style={th}>Date</th>
              <th style={th}>Trades</th>
              <th style={th}>Wins</th>
              <th style={th}>Losses</th>
              <th style={th}>BE</th>
              <th style={th}>Win Rate</th>
              <th style={th}>Daily PnL</th>
              <th style={th}>Equity</th>
            </tr>
          </thead>

          <tbody>
            {dailyPnl.length === 0 ? (
              <tr>
                <td colSpan={8} style={td}>
                  Select an account and upload your Tradovate CSV.
                </td>
              </tr>
            ) : (
              dailyPnl.map((day) => (
                <tr key={day.date}>
                  <td style={td}>{day.date}</td>
                  <td style={td}>{day.tradeCount}</td>
                  <td style={td}>{day.winningTrades}</td>
                  <td style={td}>{day.losingTrades}</td>
                  <td style={td}>{day.breakevenTrades}</td>
                  <td style={td}>{day.winRate}%</td>

                  <td
                    style={{
                      ...td,
                      color:
                        day.totalPnl >= 0
                          ? "#34d399"
                          : "#fb7185",
                      fontWeight: "bold",
                    }}
                  >
                    {formatCurrency(day.totalPnl)}
                  </td>

                  <td
                    style={{
                      ...td,
                      color:
                        day.cumulativePnl >= 0
                          ? "#34d399"
                          : "#fb7185",
                      fontWeight: "bold",
                    }}
                  >
                    {formatCurrency(day.cumulativePnl)}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div
        style={{
          marginTop: 40,
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
        }}
      >
        <h2 style={{ margin: 0 }}>Loaded Trades Preview</h2>

        <button
          type="button"
          onClick={() =>
            setShowTradesPreview((prev) => !prev)
          }
          style={{
            ...button,
            width: 150,
            background: "rgba(15,23,42,0.9)",
            border:
              "1px solid rgba(148,163,184,0.35)",
          }}
        >
          {showTradesPreview
            ? "Hide Preview"
            : "Show Preview"}
        </button>
      </div>

      {!showTradesPreview && (
        <p
          style={{
            color: "#94a3b8",
            marginTop: 12,
          }}
        >
          Preview hidden to keep dashboard clean.
        </p>
      )}

      {showTradesPreview && (
        <div style={tableCard}>
          <table style={table}>
            <thead>
              <tr>
                <th style={th}>Sold Time</th>
                <th style={th}>Symbol</th>
                <th style={th}>Qty</th>
                <th style={th}>Buy</th>
                <th style={th}>Sell</th>
                <th style={th}>PnL</th>
                <th style={th}>Duration</th>
              </tr>
            </thead>

            <tbody>
              {trades.length === 0 ? (
                <tr>
                  <td colSpan={7} style={td}>
                    No trades loaded. Select an account
                    or import a CSV.
                  </td>
                </tr>
              ) : (
                trades
                  .slice(0, 250)
                  .map((trade, index) => (
                    <tr
                      key={`${trade.soldTimestamp}-${index}`}
                    >
                      <td style={td}>
                        {trade.soldTimestamp}
                      </td>

                      <td style={td}>
                        {trade.symbol}
                      </td>

                      <td style={td}>
                        {trade.qty}
                      </td>

                      <td style={td}>
                        {trade.buyPrice}
                      </td>

                      <td style={td}>
                        {trade.sellPrice}
                      </td>

                      <td
                        style={{
                          ...td,
                          color:
                            trade.pnl >= 0
                              ? "#34d399"
                              : "#fb7185",
                          fontWeight: 900,
                        }}
                      >
                        {formatCurrency(trade.pnl)}
                      </td>

                      <td style={td}>
                        {trade.duration || "-"}
                      </td>
                    </tr>
                  ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
