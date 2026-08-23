import { Link } from "react-router-dom";
import type { CSSProperties } from "react";
import type { DailyPnl } from "../../utils/dashboardUtils";
import { formatCurrency } from "../../utils/dashboardUtils";
import type { TradovateTrade } from "../../utils/tradovateCsv";
import { card, mutedText, sectionTitle } from "../../styles/dashboardStyles";

type Props = {
  dailyPnl: DailyPnl[];
  latestDate: string | null;
  latestTrade?: TradovateTrade;
  totalTrades: number;
  winRate: number;
  worstDay: number;
  showTradingDnaEntryPoint: boolean;
};

export default function DashboardPreviewCards({
  dailyPnl,
  latestDate,
  latestTrade,
  totalTrades,
  winRate,
  worstDay,
  showTradingDnaEntryPoint,
}: Props) {
  const latest = dailyPnl[dailyPnl.length - 1];
  const monthPnl = dailyPnl.reduce((sum, day) => sum + day.totalPnl, 0);

  return (
    <div style={grid}>
      <PreviewCard
        title="Latest Session"
        body={latestTrade ? `${latestDate ?? "Recent"}: ${latestTrade.symbol} ${latestTrade.side ?? ""} closed ${formatCurrency(latestTrade.pnl)}.` : "Import trades to populate the latest session."}
        meta="Replay the full day"
        to="/session-review"
      />
      {showTradingDnaEntryPoint && (
        <PreviewCard
          title="Trading DNA Snapshot"
          body={totalTrades >= 5 ? `${totalTrades} trades available for behavior analysis.` : "Import at least 5 trades for stronger DNA."}
          meta={`Win rate ${winRate}%`}
          to="/trading-dna"
        />
      )}
      <PreviewCard
        title="Calendar Preview"
        body={latest ? `Latest day: ${formatCurrency(latest.totalPnl)} across ${latest.tradeCount} trades.` : "No calendar data yet."}
        meta={`Month P&L ${formatCurrency(monthPnl)}`}
        to="/calendar"
      />
      <PreviewCard
        title="Coach Preview"
        body={worstDay < 0 ? `Worst day is ${formatCurrency(worstDay)}. Coach can turn that into tomorrow's rule.` : "Daily and weekly coaching snapshots live on the Coach page."}
        meta="Cached reviews stay viewable"
        to="/coach"
      />
    </div>
  );
}

function PreviewCard({ title, body, meta, to }: { title: string; body: string; meta: string; to: string }) {
  return (
    <section style={card}>
      <h3 style={{ ...sectionTitle, fontSize: 20 }}>{title}</h3>
      <p style={{ ...mutedText, minHeight: 52, lineHeight: 1.55 }}>{body}</p>
      <div style={metaStyle}>{meta}</div>
      <Link to={to} style={linkStyle}>View</Link>
    </section>
  );
}

const grid: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(230px, 1fr))",
  gap: 18,
  marginTop: 18,
};

const metaStyle: CSSProperties = {
  color: "#bae6fd",
  fontWeight: 850,
  fontSize: 13,
  marginBottom: 14,
};

const linkStyle: CSSProperties = {
  color: "#6ee7b7",
  fontWeight: 900,
  textDecoration: "none",
};
