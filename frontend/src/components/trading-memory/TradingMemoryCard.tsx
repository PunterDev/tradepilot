import { useEffect, useState, type CSSProperties } from "react";
import {
  getTradingMemory,
  regenerateTradingMemory,
  type SimilarTrade,
  type TradingMemoryResponse,
} from "../../api/tradingMemoryApi";

type Props = {
  tradeId: number;
  enabled: boolean;
  onSelectTrade: (tradeId: number) => void;
};

export default function TradingMemoryCard({ tradeId, enabled, onSelectTrade }: Props) {
  const [data, setData] = useState<TradingMemoryResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [regenerating, setRegenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function fetchMemory() {
    const next = await getTradingMemory(tradeId);
    setData(next);
    return next;
  }

  async function load() {
    if (!enabled) return;
    setLoading(true);
    setError(null);
    try {
      await fetchMemory();
    } catch (err: any) {
      if (err?.response?.status === 403) {
        setError("Trading Memory is available on Pro with V2 features enabled.");
      } else {
        setError("Trading Memory is temporarily unavailable. Your trade review is still available.");
      }
    } finally {
      setLoading(false);
    }
  }

  async function pollUntilReadyOrFailed() {
    let latest: TradingMemoryResponse | null = null;
    for (let attempt = 0; attempt < TRADING_MEMORY_MAX_POLL_ATTEMPTS; attempt += 1) {
      latest = await fetchMemory();
      if (latest.status !== "PENDING") {
        return latest;
      }
      await wait(TRADING_MEMORY_POLL_INTERVAL_MS);
    }
    return latest;
  }

  useEffect(() => {
    setData(null);
    setError(null);
    void load();
  }, [tradeId, enabled]);

  if (!enabled) return null;

  async function regenerate() {
    setRegenerating(true);
    setError(null);
    try {
      await regenerateTradingMemory(tradeId, true);
      const latest = await pollUntilReadyOrFailed();
      if (latest?.status === "PENDING") {
        setError("Trading Memory is still being prepared. Refresh this trade in a moment.");
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || "Unable to regenerate Trading Memory right now.");
    } finally {
      setRegenerating(false);
    }
  }

  return (
    <section style={card} data-testid="trading-memory-card">
      <div style={header}>
        <div>
          <div style={eyebrow}>V2 Trading Memory</div>
          <h3 style={title}>Trading Memory</h3>
          <p style={muted}>Historical context from reviewed trades with similar behavior.</p>
        </div>
        <button
          type="button"
          data-testid="trading-memory-regenerate"
          onClick={() => void regenerate()}
          disabled={loading || regenerating}
          style={secondaryButton(loading || regenerating)}
        >
          {regenerating ? "Regenerating..." : "Regenerate"}
        </button>
      </div>

      {loading && <div style={stateBox} data-testid="trading-memory-status" data-status="LOADING">Loading Trading Memory...</div>}
      {error && <div style={errorBox} data-testid="trading-memory-status" data-status="FAILED">{error}</div>}

      {!loading && !error && data && data.status !== "READY" && (
        <div style={stateBox} data-testid="trading-memory-status" data-status={data.status}>
          <strong>{statusTitle(data.status)}</strong>
          <span>{data.message || statusMessage(data.status)}</span>
        </div>
      )}

      {!loading && !error && data?.status === "READY" && (
        <div style={{ display: "grid", gap: 16 }} data-testid="trading-memory-status" data-status="READY">
          <div style={metricGrid}>
            {metricItems(data).map((metric) => (
              <Metric key={metric.label} label={metric.label} value={metric.value} tone={metric.tone} />
            ))}
          </div>

          {data.recurringPattern?.detected && (
            <div style={patternCard}>
              <span style={accentBadge}>Recurring pattern</span>
              <strong style={{ color: "#f8fafc" }}>{data.recurringPattern.title}</strong>
              <p style={muted}>{data.recurringPattern.message}</p>
              {data.recurringPattern.averageOpportunityR != null && (
                <p style={smallText}>
                  Estimated opportunity based on MFE: {formatR(data.recurringPattern.averageOpportunityR)} per trade
                </p>
              )}
            </div>
          )}

          {data.aiInsight && data.aiInsight.source !== "NONE" && (
            <div style={insightCard}>
              <span style={insightBadge(data.aiInsight.source)}>{data.aiInsight.source === "AI" ? "AI Memory Insight" : "Pattern Summary"}</span>
              <strong style={{ color: "#f8fafc" }}>{data.aiInsight.headline}</strong>
              <p style={muted}>{data.aiInsight.explanation}</p>
              <p style={lessonText}>{data.aiInsight.lesson}</p>
            </div>
          )}

          <div>
            <div style={subHeader}>Supporting trades</div>
            <div style={tradeList}>
              {data.similarTrades.map((trade) => (
                <SimilarTradeRow key={trade.tradeId} trade={trade} onSelectTrade={onSelectTrade} />
              ))}
            </div>
          </div>
        </div>
      )}
    </section>
  );
}

const TRADING_MEMORY_POLL_INTERVAL_MS = 1500;
const TRADING_MEMORY_MAX_POLL_ATTEMPTS = 12;

function wait(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

function SimilarTradeRow({ trade, onSelectTrade }: { trade: SimilarTrade; onSelectTrade: (tradeId: number) => void }) {
  return (
    <button type="button" data-testid={`similar-trade-row-${trade.tradeId}`} style={tradeRow} onClick={() => onSelectTrade(trade.tradeId)}>
      <div>
        <strong style={{ color: "#f8fafc" }}>
          {trade.instrument || "Trade"} {trade.direction || ""}
        </strong>
        <div style={smallText}>
          {trade.tradeDate || "No date"} · {formatLabel(trade.setup)} · {formatLabel(trade.mistake)}
        </div>
      </div>
      <div style={{ textAlign: "right" }}>
        <strong style={{ color: tone(trade.resultR) === "positive" ? "#6ee7b7" : tone(trade.resultR) === "negative" ? "#fda4af" : "#e2e8f0" }}>
          {formatR(trade.resultR)}
        </strong>
        <div style={smallText}>{trade.similarity == null ? "Match unavailable" : `${Math.round(trade.similarity * 100)}% semantic match`}</div>
      </div>
    </button>
  );
}

function Metric({ label, value, tone }: { label: string; value: string; tone?: "positive" | "negative" | "neutral" }) {
  return (
    <div style={metricCard}>
      <span style={metricLabel}>{label}</span>
      <strong style={{ ...metricValue, color: tone === "positive" ? "#6ee7b7" : tone === "negative" ? "#fda4af" : "#f8fafc" }}>
        {value}
      </strong>
    </div>
  );
}

function metricItems(data: TradingMemoryResponse) {
  const metrics = data.metrics;
  const items: { label: string; value: string; tone?: "positive" | "negative" | "neutral" }[] = [
    { label: "Similar Trades", value: String(data.similarTradeCount) },
  ];
  if (metrics?.averageResultR !== null && metrics?.averageResultR !== undefined) {
    items.push({ label: "Average Result", value: formatR(metrics.averageResultR), tone: tone(metrics.averageResultR) });
  }
  if (metrics?.medianResultR !== null && metrics?.medianResultR !== undefined) {
    items.push({ label: "Median Result", value: formatR(metrics.medianResultR), tone: tone(metrics.medianResultR) });
  }
  if (metrics?.averageMaeR !== null && metrics?.averageMaeR !== undefined) {
    items.push({ label: "Average MAE", value: formatR(metrics.averageMaeR), tone: tone(metrics.averageMaeR) });
  }
  if (metrics?.averageMfeR !== null && metrics?.averageMfeR !== undefined) {
    items.push({ label: "Average MFE", value: formatR(metrics.averageMfeR), tone: tone(metrics.averageMfeR) });
  }
  if (metrics?.averageExitEfficiency !== null && metrics?.averageExitEfficiency !== undefined) {
    items.push({ label: "Exit Efficiency", value: `${metrics.averageExitEfficiency.toFixed(2)}x` });
  }
  if (data.commonBehavior?.mistakeLabel) {
    items.push({ label: "Common Mistake", value: formatLabel(data.commonBehavior.mistakeLabel) });
  }
  if (data.commonBehavior?.emotionLabel) {
    items.push({ label: "Common Emotion", value: formatLabel(data.commonBehavior.emotionLabel) });
  }
  return items;
}

function statusTitle(status: string) {
  if (status === "PENDING") return "Building this trade's memory";
  if (status === "NOT_ENOUGH_DATA") return "Not enough similar reviewed trades yet";
  if (status === "DISABLED") return "Trading Memory is disabled";
  return "Trading Memory unavailable";
}

function statusMessage(status: string) {
  if (status === "PENDING") return "TradePilot will add historical context after this reviewed trade is indexed.";
  if (status === "NOT_ENOUGH_DATA") return "TradePilot will identify patterns as your reviewed trade history grows.";
  return "Your AI trade review remains available.";
}

function formatR(value: number | null | undefined) {
  if (value == null || Number.isNaN(value)) return "-";
  const sign = value > 0 ? "+" : "";
  return `${sign}${value.toFixed(2)}R`;
}

function formatLabel(value: string | null | undefined) {
  if (!value) return "-";
  return value
    .replace(/_/g, " ")
    .toLowerCase()
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

function tone(value: number | null | undefined): "positive" | "negative" | "neutral" {
  if (value == null) return "neutral";
  if (value > 0) return "positive";
  if (value < 0) return "negative";
  return "neutral";
}

const card: CSSProperties = {
  border: "1px solid rgba(45, 212, 191, 0.18)",
  borderRadius: 20,
  padding: 20,
  background: "linear-gradient(135deg, rgba(15, 23, 42, 0.94), rgba(8, 13, 28, 0.96))",
  boxShadow: "0 18px 50px rgba(0,0,0,0.28)",
};

const header: CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  alignItems: "flex-start",
  gap: 16,
  marginBottom: 16,
};

const eyebrow: CSSProperties = {
  color: "#5eead4",
  fontSize: 11,
  fontWeight: 800,
  letterSpacing: 0,
  textTransform: "uppercase",
};

const title: CSSProperties = {
  margin: "4px 0",
  color: "#f8fafc",
  fontSize: 22,
};

const muted: CSSProperties = {
  margin: 0,
  color: "#94a3b8",
  lineHeight: 1.55,
};

const smallText: CSSProperties = {
  color: "#94a3b8",
  fontSize: 12,
  lineHeight: 1.45,
};

const lessonText: CSSProperties = {
  margin: "8px 0 0",
  color: "#ccfbf1",
  lineHeight: 1.55,
};

const metricGrid: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(130px, 1fr))",
  gap: 10,
};

const metricCard: CSSProperties = {
  minWidth: 0,
  border: "1px solid rgba(148, 163, 184, 0.16)",
  borderRadius: 14,
  padding: 12,
  background: "rgba(15, 23, 42, 0.74)",
};

const metricLabel: CSSProperties = {
  display: "block",
  color: "#94a3b8",
  fontSize: 11,
  textTransform: "uppercase",
  fontWeight: 800,
  letterSpacing: 0,
};

const metricValue: CSSProperties = {
  display: "block",
  marginTop: 6,
  fontSize: 18,
  lineHeight: 1.1,
};

const patternCard: CSSProperties = {
  display: "grid",
  gap: 8,
  border: "1px solid rgba(45, 212, 191, 0.22)",
  borderRadius: 16,
  padding: 14,
  background: "rgba(20, 184, 166, 0.08)",
};

const insightCard: CSSProperties = {
  display: "grid",
  gap: 8,
  border: "1px solid rgba(99, 102, 241, 0.22)",
  borderRadius: 16,
  padding: 14,
  background: "rgba(79, 70, 229, 0.09)",
};

const accentBadge: CSSProperties = {
  width: "fit-content",
  border: "1px solid rgba(45, 212, 191, 0.28)",
  borderRadius: 999,
  padding: "4px 9px",
  color: "#5eead4",
  fontSize: 11,
  fontWeight: 800,
  textTransform: "uppercase",
};

function insightBadge(source: string | undefined): CSSProperties {
  const ai = source === "AI";
  return {
    width: "fit-content",
    border: `1px solid ${ai ? "rgba(129, 140, 248, 0.32)" : "rgba(148, 163, 184, 0.22)"}`,
    borderRadius: 999,
    padding: "4px 9px",
    color: ai ? "#c4b5fd" : "#cbd5e1",
    fontSize: 11,
    fontWeight: 800,
    textTransform: "uppercase",
  };
}

const subHeader: CSSProperties = {
  color: "#f8fafc",
  fontWeight: 800,
  marginBottom: 10,
};

const tradeList: CSSProperties = {
  display: "grid",
  gap: 8,
};

const tradeRow: CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  gap: 14,
  width: "100%",
  textAlign: "left",
  border: "1px solid rgba(148, 163, 184, 0.14)",
  borderRadius: 14,
  padding: 12,
  background: "rgba(2, 6, 23, 0.42)",
  cursor: "pointer",
};

const stateBox: CSSProperties = {
  display: "grid",
  gap: 6,
  border: "1px solid rgba(148, 163, 184, 0.16)",
  borderRadius: 16,
  padding: 14,
  color: "#cbd5e1",
  background: "rgba(15, 23, 42, 0.7)",
};

const errorBox: CSSProperties = {
  ...stateBox,
  color: "#fecdd3",
  borderColor: "rgba(244, 63, 94, 0.24)",
  background: "rgba(127, 29, 29, 0.18)",
};

function secondaryButton(disabled: boolean): CSSProperties {
  return {
    border: "1px solid rgba(45, 212, 191, 0.26)",
    borderRadius: 999,
    padding: "9px 12px",
    color: "#ccfbf1",
    background: "rgba(20, 184, 166, 0.1)",
    cursor: disabled ? "not-allowed" : "pointer",
    opacity: disabled ? 0.6 : 1,
    fontWeight: 800,
    whiteSpace: "nowrap",
  };
}
