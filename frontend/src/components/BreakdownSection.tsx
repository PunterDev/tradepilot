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
                const avgR = toNullableNumber(item.averageR);

                return (
                  <tr key={item.label}>
                    <td style={td}>{formatLabel ? formatLabel(item.label) : formatMetricLabel(item.label)}</td>
                    <td style={td}>{item.totalTrades}</td>
                    <td style={td}>{toNumber(item.winRate).toFixed(2)}%</td>
                    <td
                      style={td}
                      title={avgR == null ? "Set your account 1R amount to calculate R-multiple." : undefined}
                    >
                      {avgR == null ? "—" : `${avgR.toFixed(2)}R`}
                      {avgR == null && (
                        <span style={{ display: "block", color: "#64748b", fontSize: 11 }}>
                          Set 1R
                        </span>
                      )}
                    </td>
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

function toNullableNumber(value: number | string | null | undefined): number | null {
  if (value === undefined || value === null) return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function formatMetricLabel(value: string): string {
  return value
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join(" ");
}
