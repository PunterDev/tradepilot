import { useEffect, useMemo, useState } from "react";
import type { Trade } from "../types/trade";
import { useAiTradeAnalysisPolling } from "../hooks/useAiTradeAnalysisPolling";
import { useBillingUsage } from "../hooks/useBillingUsage";
import DisciplinePanel from "./DisciplinePanel";
import UpgradeModal from "./UpgradeModal";
import {
  aiReviewButtonStyle,
  aiReviewCardStyle,
  aiReviewErrorStyle,
  aiReviewGradeStyle,
  aiReviewHeaderStyle,
  aiReviewResultStyle,
  aiReviewSectionStyle,
} from "../styles/dashboardStyles";

const subtleTextStyle = {
  color: "#94a3b8",
  fontSize: 13,
};

type Props = {
  trade: Trade;
  journalText?: string;
};

export default function AiTradeAnalysisPanel({ trade, journalText }: Props) {
  const [chartImage, setChartImage] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState("");
  const [upgradeOpen, setUpgradeOpen] = useState(false);

  const {
    analysis,
    error,
    loadingText,
    retryCount,
    hasCachedReview,
    cacheMessage,
    isLoading,
    isFailed,
    start,
    retry,
  } = useAiTradeAnalysisPolling();
  const billing = useBillingUsage(true);
  const aiReviewsRemaining = billing.usage?.remaining.AI_TRADE_REVIEW ?? 1;
  const reviewLimitReached = aiReviewsRemaining === 0;

  const hasAnalysisContent = Boolean(
    analysis?.summary || analysis?.grade || analysis?.tags?.length
  );

  const request = useMemo(
    () => ({
      tradeId: trade.id,
      trade,
      journalText,
      reviewVersion: "v2-unified-trade-analysis",
    }),
    [trade, journalText]
  );

  useEffect(() => {
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  function handleImageChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith("image/")) {
      return;
    }

    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setChartImage(file);
    setPreviewUrl(URL.createObjectURL(file));
  }

  function clearImage() {
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setChartImage(null);
    setPreviewUrl("");
  }

  function handleAnalyze() {
    if (reviewLimitReached) {
      setUpgradeOpen(true);
      return;
    }

    void start(request, chartImage).then(() => billing.reload());
  }

  return (
    <div style={aiReviewCardStyle}>
      <UpgradeModal open={upgradeOpen} onClose={() => setUpgradeOpen(false)} />
      <div style={aiReviewHeaderStyle}>
        <div>
          <h3>AI Trade Analysis</h3>
          <p>Analyze execution, discipline, journal context, and optional chart screenshot.</p>
        </div>

        <button
          style={{
            ...aiReviewButtonStyle,
            opacity: isLoading || reviewLimitReached ? 0.6 : 1,
            cursor: isLoading || reviewLimitReached ? "not-allowed" : "pointer",
          }}
          onClick={handleAnalyze}
          disabled={isLoading}
        >
          {isLoading
            ? "Analyzing..."
            : reviewLimitReached
              ? "Limit reached"
              : hasAnalysisContent
              ? "Refresh Analysis"
              : "Analyze Trade"}
        </button>
      </div>

      {reviewLimitReached && (
        <div
          style={{
            marginTop: 12,
            padding: 12,
            borderRadius: 12,
            background: "rgba(113,63,18,0.22)",
            border: "1px solid rgba(250,204,21,0.32)",
            color: "#fde68a",
            fontSize: 13,
          }}
        >
          You have reached this month’s AI trade review limit. Cached analysis remains viewable.
        </div>
      )}

      <div
        style={{
          marginTop: 14,
          padding: 14,
          borderRadius: 14,
          background: "rgba(15,23,42,0.72)",
          border: "1px solid rgba(148,163,184,0.18)",
        }}
      >
        <strong style={{ color: "#e5e7eb" }}>Chart screenshot</strong>
        <p style={{ ...subtleTextStyle, marginTop: 6 }}>
          Optional. Add a TradingView or Tradovate screenshot for deeper visual analysis.
        </p>

        <input
          type="file"
          accept="image/*"
          onChange={handleImageChange}
          disabled={isLoading}
          style={{ marginTop: 10, color: "#cbd5e1" }}
        />

        {previewUrl && (
          <div style={{ marginTop: 14 }}>
            <img
              src={previewUrl}
              alt="Chart screenshot preview"
              style={{
                width: "100%",
                maxHeight: 340,
                objectFit: "contain",
                borderRadius: 14,
                border: "1px solid rgba(148,163,184,0.24)",
              }}
            />

            <button
              type="button"
              onClick={clearImage}
              disabled={isLoading}
              style={{
                ...aiReviewButtonStyle,
                marginTop: 10,
                width: 140,
                background: "rgba(15,23,42,0.9)",
                border: "1px solid rgba(148,163,184,0.35)",
              }}
            >
              Remove Image
            </button>
          </div>
        )}
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
          <strong>{hasCachedReview ? "Refreshing AI analysis..." : "AI is analyzing your trade..."}</strong>
          <p style={{ margin: 0, color: "#93c5fd" }}>{loadingText}</p>
          {chartImage && <small style={subtleTextStyle}>Chart image included in this analysis.</small>}
          {cacheMessage && <small style={subtleTextStyle}>{cacheMessage}</small>}
          {retryCount > 0 && <small style={{ color: "#fbbf24", fontSize: 13 }}>Retry attempt: {retryCount}</small>}
          <small style={subtleTextStyle}>The analysis will appear automatically.</small>
        </div>
      )}

      {isFailed && error && (
        <div style={aiReviewErrorStyle}>
          <strong>AI analysis failed</strong>
          <p style={{ marginTop: 8 }}>{error}</p>

          <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
            <button style={aiReviewButtonStyle} onClick={retry} disabled={isLoading}>
              Try Again
            </button>
            <button style={{ ...aiReviewButtonStyle, background: "#334155" }} onClick={handleAnalyze} disabled={isLoading}>
              Start New Analysis
            </button>
          </div>
        </div>
      )}

      {hasAnalysisContent && analysis && (
        <div style={{ ...aiReviewResultStyle, opacity: isLoading && hasCachedReview ? 0.72 : 1 }}>
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
              Showing previous analysis while TradePilot generates the newest version.
            </div>
          )}

          {analysis.grade && (
            <div style={aiReviewGradeStyle}>
              Grade: <strong>{analysis.grade}</strong>
            </div>
          )}

          <ScoreRow
            executionScore={analysis.executionScore}
            disciplineScore={analysis.disciplineScore}
            riskScore={analysis.riskScore}
          />
          <DisciplinePanel
            score={analysis.disciplineScore}
            violations={analysis.violations}
          />

          <Tags tags={analysis.tags} />
          <Section title="Summary" value={analysis.summary} />
          <Section title="Market Context" value={analysis.marketContext} />
          <Section title="Entry Analysis" value={analysis.entryAnalysis} />
          <Section title="Exit Analysis" value={analysis.exitAnalysis} />
          <Section title="Risk Analysis" value={analysis.riskAnalysis} />
          <Section title="Psychology Analysis" value={analysis.psychologyAnalysis} />
          <Section title="Chart Analysis" value={analysis.chartAnalysis} />
          <ListSection title="Mistakes" items={analysis.mistakes} emptyText="No mistakes returned." />
          <ListSection title="Strengths" items={analysis.strengths} emptyText="No strengths returned." />
          <ListSection title="Improvement Plan" items={analysis.improvementPlan} emptyText="No improvement plan returned." />
        </div>
      )}
    </div>
  );
}

function ScoreRow({
  executionScore,
  disciplineScore,
  riskScore,
}: {
  executionScore?: number;
  disciplineScore?: number;
  riskScore?: number;
}) {
  const scores = [
    ["Execution", executionScore],
    ["Discipline", disciplineScore],
    ["Risk", riskScore],
  ] as const;

  if (scores.every(([, value]) => value == null)) return null;

  return (
    <div style={{ display: "grid", gridTemplateColumns: "repeat(3, minmax(0, 1fr))", gap: 10, marginTop: 14 }}>
      {scores.map(([label, value]) => (
        <div key={label} style={{ padding: 12, borderRadius: 14, background: "rgba(15,23,42,0.74)", border: "1px solid rgba(148,163,184,0.18)" }}>
          <div style={{ color: "#94a3b8", fontSize: 12 }}>{label}</div>
          <strong style={{ color: "#e5e7eb", fontSize: 18 }}>{value ?? "-"}/100</strong>
        </div>
      ))}
    </div>
  );
}

function Tags({ tags }: { tags?: string[] }) {
  if (!tags?.length) return null;

  return (
    <section style={aiReviewSectionStyle}>
      <h4>AI Tags</h4>
      <div style={{ display: "flex", flexWrap: "wrap", gap: 8, marginTop: 10 }}>
        {tags.map((tag) => (
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
  );
}

function Section({ title, value }: { title: string; value?: string }) {
  if (!value) return null;

  return (
    <section style={aiReviewSectionStyle}>
      <h4>{title}</h4>
      <p>{value}</p>
    </section>
  );
}

function ListSection({ title, items, emptyText }: { title: string; items?: string[]; emptyText: string }) {
  return (
    <section style={aiReviewSectionStyle}>
      <h4>{title}</h4>
      {items?.length ? (
        <ul>
          {items.map((item, index) => (
            <li key={index}>{item}</li>
          ))}
        </ul>
      ) : (
        <p>{emptyText}</p>
      )}
    </section>
  );
}
