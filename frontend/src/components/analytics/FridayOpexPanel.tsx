import type { FridayOpexAnalyticsResponse } from "../../types/analytics";

type Props = {
    data: FridayOpexAnalyticsResponse;
};

function formatMoney(value?: number) {
    return `$${(value ?? 0).toFixed(2)}`;
}

export default function FridayOpexPanel({ data }: Props) {
    return (
        <div
            style={{
                marginTop: 24,
                padding: 20,
                borderRadius: 20,
                background: "rgba(15,23,42,0.92)",
                border: "1px solid rgba(148,163,184,0.15)",
            }}
        >
            <h3 style={{ color: "#e5e7eb", marginTop: 0 }}>
                Friday / OPEX Analytics
            </h3>

            <div
                style={{
                    display: "grid",
                    gridTemplateColumns: "1fr 1fr 1fr",
                    gap: 16,
                    marginTop: 18,
                }}
            >
                <MetricCard
                    title="All Fridays"
                    pnl={data.allFridays?.totalPnl}
                    winRate={data.allFridays?.winRate}
                    trades={data.allFridays?.tradeCount}
                />

                <MetricCard
                    title="OPEX Fridays"
                    pnl={data.opexFridays?.totalPnl}
                    winRate={data.opexFridays?.winRate}
                    trades={data.opexFridays?.tradeCount}
                />

                <MetricCard
                    title="Non-OPEX Fridays"
                    pnl={data.nonOpexFridays?.totalPnl}
                    winRate={data.nonOpexFridays?.winRate}
                    trades={data.nonOpexFridays?.tradeCount}
                />
            </div>

            <div
                style={{
                    marginTop: 18,
                    padding: 14,
                    borderRadius: 14,
                    background: "rgba(59,130,246,0.12)",
                    border: "1px solid rgba(59,130,246,0.25)",
                    color: "#cbd5e1",
                }}
            >
                {data.insight}
            </div>
        </div>
    );
}

function MetricCard({
    title,
    pnl,
    winRate,
    trades,
}: {
    title: string;
    pnl?: number;
    winRate?: number;
    trades?: number;
}) {
    return (
        <div
            style={{
                padding: 16,
                borderRadius: 16,
                background: "rgba(2,6,23,0.9)",
                border: "1px solid rgba(148,163,184,0.12)",
            }}
        >
            <div style={{ color: "#94a3b8", fontSize: 14 }}>{title}</div>

            <div
                style={{
                    marginTop: 8,
                    fontSize: 28,
                    fontWeight: 900,
                    color: (pnl ?? 0) >= 0 ? "#4ade80" : "#f87171",
                }}
            >
                {formatMoney(pnl)}
            </div>

            <div style={{ marginTop: 12, color: "#cbd5e1" }}>
                Win Rate: {(winRate ?? 0).toFixed(1)}%
            </div>

            <div style={{ color: "#94a3b8" }}>
                Trades: {trades ?? 0}
            </div>
        </div>
    );
}