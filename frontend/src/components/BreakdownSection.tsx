import type { MetricItem } from "../api/statsApi";
import {
  card,
  sectionTitle,
  table,
  td,
  th,
} from "../styles/dashboardStyles";
import { formatCurrency } from "../utils/dashboardUtils";

type BreakdownSectionProps = {
  title: string;
  items: MetricItem[];
  formatLabel?: (value: string) => string;
};

export default function BreakdownSection({
  title,
  items,
  formatLabel,
}: BreakdownSectionProps) {
  return (
    <div style={card}>
      <h2 style={sectionTitle}>{title}</h2>
      <div style={{ overflowX: "auto" }}>
        <table style={table}>
          <thead>
            <tr>
              <th style={th}>Group</th>
              <th style={th}>Trades</th>
              <th style={th}>Win Rate</th>
              <th style={th}>Avg R</th>
              <th style={th}>PnL</th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 ? (
              <tr>
                <td colSpan={5} style={td}>
                  No trades in this view yet.
                </td>
              </tr>
            ) : (
              items.map((item) => {
                const pnl = toNumber(item.totalPnl);
                const avgR = toNumber(item.averageR);

                return (
                  <tr key={item.label}>
                    <td style={td}>{formatLabel ? formatLabel(item.label) : formatMetricLabel(item.label)}</td>
                    <td style={td}>{item.totalTrades}</td>
                    <td style={td}>{toNumber(item.winRate).toFixed(2)}%</td>
                    <td style={td}>{avgR.toFixed(2)}R</td>
                    <td
                      style={{
                        ...td,
                        color: pnl >= 0 ? "#34d399" : "#fb7185",
                        fontWeight: 900,
                      }}
                    >
                      {formatCurrency(pnl)}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function toNumber(value: number | string | undefined): number {
  if (value === undefined || value === null) return 0;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function formatMetricLabel(value: string): string {
  return value
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join(" ");
}
