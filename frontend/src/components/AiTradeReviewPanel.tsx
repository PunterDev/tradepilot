import { useEffect, useState, type CSSProperties } from "react";
import { getTradeExecution, type ExecutionScore, type PlanComparisonItem } from "../api/executionApi";
import type { Trade } from "../types/trade";
import { useAiReviewPolling } from "../hooks/useAiReviewPolling";
import {
    aiReviewButtonStyle,
    aiReviewCardStyle,
    aiReviewErrorStyle,
    aiReviewGradeStyle,
    aiReviewHeaderStyle,
    aiReviewResultStyle,
    aiReviewSectionStyle,
} from "../styles/dashboardStyles";

type Props = {
    trade: Trade;
    journalText?: string;
};

const subtleTextStyle = {
    color: "#94a3b8",
    fontSize: 13,
};

export default function AiTradeReviewPanel({ trade, journalText }: Props) {
    const [execution, setExecution] = useState<ExecutionScore | null>(null);
    const [executionError, setExecutionError] = useState<string | null>(null);
    const {
        review,
        error,
        loadingText,
        retryCount,
        hasCachedReview,
        cacheMessage,
        isLoading,
        isFailed,
        start,
        retry,
    } = useAiReviewPolling();

    const handleReview = () => {
        start({
            tradeId: trade.id,
            trade,
            journalText,
        });
    };

    useEffect(() => {
        let cancelled = false;
        setExecution(null);
        setExecutionError(null);
        getTradeExecution(trade.id)
            .then((data) => {
                if (!cancelled) setExecution(data);
            })
            .catch(() => {
                if (!cancelled) setExecutionError("Execution score is unavailable for this trade.");
            });
        return () => {
            cancelled = true;
        };
    }, [trade.id]);

    const hasReviewContent = Boolean(review?.summary || review?.grade || review?.tags?.length);

    return (
        <div style={aiReviewCardStyle}>
            <ExecutionCard execution={execution} error={executionError} />

            <div style={aiReviewHeaderStyle}>
                <div>
                    <h3>AI Trade Review</h3>
                    <p>Get feedback on execution, risk, and discipline.</p>
                </div>

                <button
                    style={{
                        ...aiReviewButtonStyle,
                        opacity: isLoading ? 0.6 : 1,
                        cursor: isLoading ? "not-allowed" : "pointer",
                    }}
                    onClick={handleReview}
                    disabled={isLoading}
                >
                    {isLoading
                        ? "Generating Review..."
                        : hasReviewContent
                            ? "Refresh Review"
                            : "Review Trade"}
                </button>
            </div>

            {isLoading && (
                <div
                    style={{
                        ...aiReviewResultStyle,
                        display: "flex",
                        flexDirection: "column",
                        gap: 8,
                    }}
                >
                    <strong>
                        {hasCachedReview ? "Refreshing AI review..." : "AI is reviewing your trade..."}
                    </strong>

                    <p style={{ margin: 0, color: "#93c5fd" }}>{loadingText}</p>

                    {cacheMessage && (
                        <small style={subtleTextStyle}>{cacheMessage}</small>
                    )}

                    {retryCount > 0 && (
                        <small style={{ color: "#fbbf24", fontSize: 13 }}>
                            Retry attempt: {retryCount}
                        </small>
                    )}

                    <small style={subtleTextStyle}>
                        This can take a few seconds. The review will appear automatically.
                    </small>
                </div>
            )}

            {isFailed && error && (
                <div style={aiReviewErrorStyle}>
                    <strong>AI review failed</strong>
                    <p style={{ marginTop: 8 }}>{error}</p>

                    <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
                        <button
                            style={aiReviewButtonStyle}
                            onClick={retry}
                            disabled={isLoading}
                        >
                            Try Again
                        </button>

                        <button
                            style={{
                                ...aiReviewButtonStyle,
                                background: "rgba(15,23,42,0.9)",
                                border: "1px solid rgba(148,163,184,0.35)",
                            }}
                            onClick={handleReview}
                            disabled={isLoading}
                        >
                            Start New Review
                        </button>
                    </div>
                </div>
            )}

            {hasReviewContent && review && (
                <div
                    style={{
                        ...aiReviewResultStyle,
                        opacity: isLoading && hasCachedReview ? 0.72 : 1,
                    }}
                >
                    {hasCachedReview && isLoading && (
                        <div
                            style={{
                                marginBottom: 14,
                                padding: "10px 12px",
                                borderRadius: 12,
                                background: "rgba(59,130,246,0.12)",
                                border: "1px solid rgba(59,130,246,0.25)",
                                color: "#bfdbfe",
                                fontSize: 13,
                                fontWeight: 700,
                            }}
                        >
                            Showing cached review while TradePilot generates the newest version.
                        </div>
                    )}

                    {review.grade && (
                        <div style={aiReviewGradeStyle}>
                            Grade: <strong>{review.grade}</strong>
                        </div>
                    )}

                    {review.tags && review.tags.length > 0 && (
                        <section style={aiReviewSectionStyle}>
                            <h4>AI Tags</h4>

                            <div
                                style={{
                                    display: "flex",
                                    flexWrap: "wrap",
                                    gap: 8,
                                    marginTop: 10,
                                }}
                            >
                                {review.tags.map((tag) => (
                                    <span
                                        key={tag}
                                        style={{
                                            padding: "6px 12px",
                                            borderRadius: 999,
                                            background: "rgba(59,130,246,0.16)",
                                            color: "#93c5fd",
                                            border: "1px solid rgba(59,130,246,0.35)",
                                            fontSize: 13,
                                            fontWeight: 800,
                                        }}
                                    >
                                        {tag}
                                    </span>
                                ))}
                            </div>
                        </section>
                    )}

                    <section style={aiReviewSectionStyle}>
                        <h4>Summary</h4>
                        <p>{review.summary || "No summary returned."}</p>
                    </section>

                    <section style={aiReviewSectionStyle}>
                        <h4>Mistakes</h4>
                        {(review.mistakes ?? []).length > 0 ? (
                            <ul>
                                {(review.mistakes ?? []).map((item, index) => (
                                    <li key={index}>{item}</li>
                                ))}
                            </ul>
                        ) : (
                            <p>No mistakes returned.</p>
                        )}
                    </section>

                    <section style={aiReviewSectionStyle}>
                        <h4>Strengths</h4>
                        {(review.strengths ?? []).length > 0 ? (
                            <ul>
                                {(review.strengths ?? []).map((item, index) => (
                                    <li key={index}>{item}</li>
                                ))}
                            </ul>
                        ) : (
                            <p>No strengths returned.</p>
                        )}
                    </section>

                    <section style={aiReviewSectionStyle}>
                        <h4>Improvement Plan</h4>
                        {(review.improvementPlan ?? []).length > 0 ? (
                            <ul>
                                {(review.improvementPlan ?? []).map((item, index) => (
                                    <li key={index}>{item}</li>
                                ))}
                            </ul>
                        ) : (
                            <p>No improvement plan returned.</p>
                        )}
                    </section>
                </div>
            )}
        </div>
    );
}

function ExecutionCard({ execution, error }: { execution: ExecutionScore | null; error: string | null }) {
    if (error) {
        return <div style={executionPanel}><strong>Execution Score</strong><p style={subtleTextStyle}>{error}</p></div>;
    }
    if (!execution) {
        return <div style={executionPanel}><strong>Execution Score</strong><p style={subtleTextStyle}>Loading execution context...</p></div>;
    }
    const components = Object.values(execution.components ?? {});
    return (
        <section style={executionPanel}>
            <div style={{ display: "flex", justifyContent: "space-between", gap: 16, alignItems: "flex-start", flexWrap: "wrap" }}>
                <div>
                    <div style={executionKicker}>Execution Score</div>
	                    <strong style={{ fontSize: 34, lineHeight: 1 }}>{execution.executionScore ?? "N/A"} / 100</strong>
	                    <p style={{ ...subtleTextStyle, marginTop: 8 }}>{evidenceLabel(execution)}</p>
	                </div>
                <div style={classificationBadge(execution.classification)}>{classificationLabel(execution.classification)}</div>
            </div>
	            <p style={{ margin: "14px 0 0", color: "#dbeafe", lineHeight: 1.5 }}>{execution.processOutcomeMessage}</p>
	            {execution.preAnalysisComparison?.planAvailable && (
	                <div style={planAlignmentBox}>
	                    <strong>Plan Alignment</strong>
	                    <div style={{ display: "grid", gap: 6, marginTop: 8 }}>
	                        {execution.preAnalysisComparison.comparisons
	                            .filter((item) => item.status !== "NOT_EVALUABLE")
	                            .slice(0, 3)
	                            .map((item) => <PlanAlignmentLine key={item.type} item={item} />)}
	                    </div>
	                </div>
	            )}
	            <div style={{ display: "grid", gap: 8, marginTop: 16 }}>
                {components.map((component) => (
                    <div key={component.key} style={componentRow}>
                        <span>{component.label}</span>
                        <strong>{component.evaluated ? `${component.score}/${component.maxScore}` : "Not evaluated"}</strong>
                    </div>
                ))}
            </div>
        </section>
    );
}

function PlanAlignmentLine({ item }: { item: PlanComparisonItem }) {
    return (
        <div style={{ display: "flex", justifyContent: "space-between", gap: 12, color: "#cbd5e1", fontSize: 13 }}>
            <span>{humanizeExecution(item.type)}</span>
            <strong style={{ color: item.status === "VIOLATED" ? "#fda4af" : item.status === "PARTIAL" ? "#fde68a" : "#bbf7d0" }}>
                {humanizeExecution(item.status)}
            </strong>
        </div>
    );
}

function evidenceLabel(execution: ExecutionScore) {
    if (execution.evidenceConfidence === "LOW" || execution.evidenceConfidence === "VERY_LOW") {
        return "Based on limited execution evidence";
    }
    return `Evidence coverage: ${execution.evidenceCoveragePercent}%`;
}

function classificationLabel(value: string) {
    return value.toLowerCase().split("_").map((part) => part.charAt(0).toUpperCase() + part.slice(1)).join(" ");
}

function humanizeExecution(value: string) {
    return value.toLowerCase().split("_").map((part) => part.charAt(0).toUpperCase() + part.slice(1)).join(" ");
}

function classificationBadge(classification: string) {
    const risky = classification === "UNDISCIPLINED_WIN" || classification === "UNDISCIPLINED_LOSS";
    const clean = classification === "CLEAN_WIN" || classification === "CLEAN_LOSS";
    return {
        borderRadius: 999,
        padding: "8px 12px",
        fontSize: 12,
        fontWeight: 900,
        letterSpacing: 0,
        color: clean ? "#bbf7d0" : risky ? "#fecdd3" : "#cbd5e1",
        background: clean ? "rgba(34,197,94,0.14)" : risky ? "rgba(244,63,94,0.14)" : "rgba(148,163,184,0.12)",
        border: clean ? "1px solid rgba(34,197,94,0.32)" : risky ? "1px solid rgba(244,63,94,0.32)" : "1px solid rgba(148,163,184,0.24)",
    };
}

const executionPanel = {
    marginBottom: 18,
    padding: 18,
    borderRadius: 8,
    background: "rgba(15,23,42,0.86)",
    border: "1px solid rgba(96,165,250,0.28)",
} satisfies CSSProperties;

const executionKicker = {
    color: "#93c5fd",
    fontSize: 12,
    textTransform: "uppercase",
    fontWeight: 900,
    marginBottom: 8,
} satisfies CSSProperties;

const componentRow = {
    display: "flex",
    justifyContent: "space-between",
    gap: 12,
    padding: "8px 0",
    borderTop: "1px solid rgba(148,163,184,0.14)",
    color: "#cbd5e1",
    fontSize: 13,
} satisfies CSSProperties;

const planAlignmentBox = {
    marginTop: 14,
    padding: 12,
    borderRadius: 8,
    border: "1px solid rgba(56,189,248,0.22)",
    background: "rgba(8,47,73,0.22)",
    color: "#e0f2fe",
} satisfies CSSProperties;
