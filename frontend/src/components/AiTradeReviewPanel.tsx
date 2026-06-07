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

    const hasReviewContent = Boolean(review?.summary || review?.grade || review?.tags?.length);

    return (
        <div style={aiReviewCardStyle}>
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
