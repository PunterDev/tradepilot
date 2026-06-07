import type { CSSProperties } from "react";
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
import { card, chartGrid, sectionTitle, tooltipStyle } from "../../styles/dashboardStyles";
import { formatCurrency, type DailyPnl } from "../../utils/dashboardUtils";

type Props = {
  dailyPnl: DailyPnl[];
};

export default function PerformanceCharts({ dailyPnl }: Props) {
  const empty = dailyPnl.length === 0;

  return (
    <>
      <div style={chartGrid}>
        <section style={card}>
          <h2 style={sectionTitle}>Daily P&L</h2>
          <ChartFrame empty={empty} emptyText="Import trades to see daily P&L.">
            <ResponsiveContainer width="100%" height={320}>
              <BarChart data={dailyPnl}>
                <CartesianGrid stroke="rgba(148,163,184,0.16)" strokeDasharray="4 4" />
                <XAxis dataKey="date" stroke="#64748b" tick={{ fill: "#94a3b8", fontSize: 12 }} />
                <YAxis stroke="#64748b" tick={{ fill: "#94a3b8", fontSize: 12 }} />
                <Tooltip formatter={(value) => formatCurrency(Number(value))} contentStyle={tooltipStyle} />
                <Bar dataKey="totalPnl" fill="#38bdf8" radius={[8, 8, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </ChartFrame>
        </section>

        <section style={card}>
          <h2 style={sectionTitle}>Equity Curve</h2>
          <ChartFrame empty={empty} emptyText="Import trades to see equity.">
            <ResponsiveContainer width="100%" height={320}>
              <LineChart data={dailyPnl}>
                <CartesianGrid stroke="rgba(148,163,184,0.16)" strokeDasharray="4 4" />
                <XAxis dataKey="date" stroke="#64748b" tick={{ fill: "#94a3b8", fontSize: 12 }} />
                <YAxis stroke="#64748b" tick={{ fill: "#94a3b8", fontSize: 12 }} />
                <Tooltip formatter={(value) => formatCurrency(Number(value))} contentStyle={tooltipStyle} />
                <Line type="monotone" dataKey="cumulativePnl" stroke="#34d399" strokeWidth={3} dot={{ r: 3 }} />
              </LineChart>
            </ResponsiveContainer>
          </ChartFrame>
        </section>
      </div>

      <section style={{ ...card, marginTop: 24 }}>
        <h2 style={sectionTitle}>Daily Win Rate</h2>
        <ChartFrame height={300} empty={empty} emptyText="Import trades to see daily win rate.">
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={dailyPnl}>
              <CartesianGrid stroke="rgba(148,163,184,0.16)" strokeDasharray="4 4" />
              <XAxis dataKey="date" stroke="#64748b" tick={{ fill: "#94a3b8", fontSize: 12 }} />
              <YAxis domain={[0, 100]} stroke="#64748b" tick={{ fill: "#94a3b8", fontSize: 12 }} />
              <Tooltip formatter={(value) => `${Number(value).toFixed(2)}%`} contentStyle={tooltipStyle} />
              <Line type="monotone" dataKey="winRate" stroke="#38bdf8" strokeWidth={3} dot={{ r: 3 }} />
            </LineChart>
          </ResponsiveContainer>
        </ChartFrame>
      </section>
    </>
  );
}

function ChartFrame({
  children,
  empty,
  emptyText,
  height = 320,
}: {
  children: React.ReactNode;
  empty: boolean;
  emptyText: string;
  height?: number;
}) {
  if (empty) {
    return <div style={{ ...frame, height }}>{emptyText}</div>;
  }

  return <div style={{ width: "100%", minWidth: 0, height }}>{children}</div>;
}

const frame: CSSProperties = {
  width: "100%",
  minWidth: 0,
  display: "grid",
  placeItems: "center",
  borderRadius: 18,
  border: "1px dashed rgba(148,163,184,0.18)",
  background: "rgba(2,6,23,0.28)",
  color: "#94a3b8",
  textAlign: "center",
  padding: 18,
};
