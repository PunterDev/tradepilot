import { useEffect } from "react";
import { trackFeatureUsage } from "../../api/featureUsageApi";
import type { DashboardStats } from "../../api/statsApi";
import AnalyticsDashboard from "../../components/analytics/AnalyticsDashboard";
import PageHeader from "../../layout/PageHeader";
import type { DailyPnl } from "../../utils/dashboardUtils";
import type { TradovateTrade } from "../../utils/tradovateCsv";
import BreakdownTables from "./BreakdownTables";
import DailyPnlTable from "./DailyPnlTable";
import PerformanceCharts from "./PerformanceCharts";

type Props = {
  dailyPnl: DailyPnl[];
  trades: TradovateTrade[];
  dashboardStats: DashboardStats | null;
  onTradeSelect: (tradeId: number) => void;
};

export default function AnalyticsPage({ dailyPnl, trades, dashboardStats, onTradeSelect }: Props) {
  useEffect(() => {
    void trackFeatureUsage("ANALYTICS_VIEW").catch(() => undefined);
  }, []);

  return (
    <>
      <PageHeader
        label="Analytics"
        title="Performance Analytics"
        description="Breakdowns, tables, charts, Friday/OPEX behavior, and heatmaps live here."
      />
      <BreakdownTables dashboardStats={dashboardStats} />
      <PerformanceCharts dailyPnl={dailyPnl} />
      <div style={{ marginTop: 28 }}>
        <DailyPnlTable dailyPnl={dailyPnl} />
      </div>
      <AnalyticsDashboard trades={trades} onTradeSelect={onTradeSelect} />
    </>
  );
}
