import { formatCurrency, type DailyPnl } from "../../utils/dashboardUtils";
import { table, tableCard, td, th } from "../../styles/dashboardStyles";

type Props = {
  dailyPnl: DailyPnl[];
};

export default function DailyPnlTable({ dailyPnl }: Props) {
  return (
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
            <th style={th}>Daily P&L</th>
            <th style={th}>Equity</th>
          </tr>
        </thead>
        <tbody>
          {dailyPnl.length === 0 ? (
            <tr>
              <td colSpan={8} style={td}>Select an account and import trades.</td>
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
                <td style={{ ...td, color: day.totalPnl >= 0 ? "#34d399" : "#fb7185", fontWeight: 900 }}>
                  {formatCurrency(day.totalPnl)}
                </td>
                <td style={{ ...td, color: day.cumulativePnl >= 0 ? "#34d399" : "#fb7185", fontWeight: 900 }}>
                  {formatCurrency(day.cumulativePnl)}
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
