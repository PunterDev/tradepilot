import type { DashboardStats } from "../../api/statsApi";
import BreakdownSection from "../../components/BreakdownSection";
import { chartGrid } from "../../styles/dashboardStyles";
import { formatSessionLabel } from "../../utils/analyticsUtils";

type Props = {
  dashboardStats: DashboardStats | null;
};

export default function BreakdownTables({ dashboardStats }: Props) {
  return (
    <div style={chartGrid}>
      <BreakdownSection title="Setup Breakdown" items={dashboardStats?.bySetup ?? []} />
      <BreakdownSection title="Session Breakdown" items={dashboardStats?.bySession ?? []} formatLabel={formatSessionLabel} />
      <BreakdownSection title="Symbol Breakdown" items={dashboardStats?.bySymbol ?? []} />
      <BreakdownSection title="Weekday Breakdown" items={dashboardStats?.byWeekday ?? []} />
    </div>
  );
}
