import type { SessionPerformanceDto } from "../../types/analytics";
import {
  formatMoney,
  formatSessionLabel,
  getPnlColor,
  getSessionOrder,
} from "../../utils/analyticsUtils";

type Props = {
  sessions: SessionPerformanceDto[];
};

export default function SessionAnalysisPanel({ sessions }: Props) {
  const sortedSessions = [...sessions].sort(
    (a, b) => getSessionOrder(a.session) - getSessionOrder(b.session)
  );

  return (
    <div
      style={{
        padding: 20,
        borderRadius: 20,
        background: "rgba(15,23,42,0.92)",
        border: "1px solid rgba(148,163,184,0.15)",
      }}
    >
      <h3 style={{ color: "#e5e7eb", marginTop: 0 }}>Session Analysis</h3>

      <div style={{ marginTop: 18 }}>
        {sortedSessions.map((session) => (
          <div
            key={session.session}
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              padding: "12px 0",
              borderBottom: "1px solid rgba(148,163,184,0.08)",
            }}
          >
            <div>
              <div style={{ color: "#e5e7eb", fontWeight: 700 }}>
                {formatSessionLabel(session.session)}
              </div>

              <div style={{ color: "#94a3b8", fontSize: 13 }}>
                {session.tradeCount} trades
              </div>
            </div>

            <div style={{ textAlign: "right" }}>
              <div
                style={{
                  color: getPnlColor(session.totalPnl),
                  fontWeight: 800,
                }}
              >
                {formatMoney(session.totalPnl)}
              </div>

              <div style={{ color: "#94a3b8", fontSize: 13 }}>
                WR {(session.winRate ?? 0).toFixed(1)}%
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}