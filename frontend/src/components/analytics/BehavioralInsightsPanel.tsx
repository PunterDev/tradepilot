import type { BehavioralInsightDto } from "../../types/analytics";
import { formatAnalyticsText } from "../../utils/analyticsUtils";

type Props = {
    insights: BehavioralInsightDto[];
};

function getSeverityColor(severity: string) {
    switch (severity) {
        case "HIGH":
            return "#f87171";
        case "MEDIUM":
            return "#facc15";
        default:
            return "#4ade80";
    }
}

export default function BehavioralInsightsPanel({ insights }: Props) {
    return (
        <div
            style={{
                padding: 20,
                borderRadius: 20,
                background: "rgba(15,23,42,0.92)",
                border: "1px solid rgba(148,163,184,0.15)",
            }}
        >
            <h3 style={{ color: "#e5e7eb", marginTop: 0 }}>
                Behavioral Insights
            </h3>

            <div style={{ marginTop: 18 }}>
                {insights.map((insight, index) => (
                    <div
                        key={`${insight.type}-${index}`}
                        style={{
                            padding: 14,
                            borderRadius: 14,
                            marginBottom: 12,
                            background: "rgba(2,6,23,0.9)",
                            border: "1px solid rgba(148,163,184,0.12)",
                        }}
                    >
                        <div
                            style={{
                                display: "inline-flex",
                                padding: "4px 10px",
                                borderRadius: 999,
                                background: "rgba(255,255,255,0.06)",
                                color: getSeverityColor(insight.severity),
                                fontWeight: 800,
                                fontSize: 12,
                            }}
                        >
                            {insight.severity}
                        </div>

                        <div
                            style={{
                                marginTop: 10,
                                color: "#e5e7eb",
                                fontWeight: 800,
                            }}
                        >
                            {formatAnalyticsText(insight.title)}
                        </div>

                        <div
                            style={{
                                marginTop: 6,
                                color: "#cbd5e1",
                                lineHeight: 1.6,
                            }}
                        >
                            {formatAnalyticsText(insight.message)}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}
