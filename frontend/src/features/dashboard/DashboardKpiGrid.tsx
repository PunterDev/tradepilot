import StatCard from "../../components/StatCard";
import { formatCurrency, type DailyPnl } from "../../utils/dashboardUtils";
import { statsGrid } from "../../styles/dashboardStyles";

type Metrics = {
  totalPnl: number;
  totalTrades: number;
  overallWinRate: number;
  bestWinStreak: number;
  worstDay: number;
};

type Props = {
  metrics: Metrics;
  dailyPnl: DailyPnl[];
};

export default function DashboardKpiGrid({ metrics, dailyPnl }: Props) {
  return (
    <div style={{ ...statsGrid, gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))" }}>
      <StatCard
        title="Total P&L"
        value={formatCurrency(metrics.totalPnl)}
        positive={metrics.totalPnl >= 0}
        negative={metrics.totalPnl < 0}
      />
      <StatCard
        title="Win Rate"
        value={`${metrics.overallWinRate}%`}
        positive={metrics.overallWinRate >= 50}
      />
      <StatCard title="Total Trades" value={String(metrics.totalTrades)} />
      <StatCard title="Trading Days" value={String(dailyPnl.length)} />
      <StatCard title="Best Win Streak" value={`${metrics.bestWinStreak} days`} positive />
      <StatCard
        title="Worst Day"
        value={formatCurrency(metrics.worstDay)}
        negative={metrics.worstDay < 0}
      />
    </div>
  );
}
