import type { CSSProperties } from "react";
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { card, sectionTitle, tooltipStyle } from "../../styles/dashboardStyles";
import { formatCurrency, type DailyPnl } from "../../utils/dashboardUtils";

type Props = {
  dailyPnl: DailyPnl[];
};

export default function EquityCurveCard({ dailyPnl }: Props) {
  return (
    <section style={card}>
      <h2 style={sectionTitle}>Equity Curve</h2>
      {dailyPnl.length === 0 ? (
        <div style={empty}>Import trades to see your equity curve.</div>
      ) : (
        <div style={chartFrame}>
          <ResponsiveContainer width="100%" height={340}>
            <LineChart data={dailyPnl}>
              <CartesianGrid stroke="rgba(148,163,184,0.16)" strokeDasharray="4 4" />
              <XAxis dataKey="date" stroke="#64748b" tick={{ fill: "#94a3b8", fontSize: 12 }} />
              <YAxis stroke="#64748b" tick={{ fill: "#94a3b8", fontSize: 12 }} />
              <Tooltip formatter={(value) => formatCurrency(Number(value))} contentStyle={tooltipStyle} />
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
      )}
    </section>
  );
}

const chartFrame: CSSProperties = {
  width: "100%",
  minWidth: 0,
  height: 340,
};

const empty: CSSProperties = {
  width: "100%",
  minWidth: 0,
  height: 340,
  display: "grid",
  placeItems: "center",
  color: "#94a3b8",
  borderRadius: 18,
  border: "1px dashed rgba(148,163,184,0.18)",
  background: "rgba(2,6,23,0.28)",
};
