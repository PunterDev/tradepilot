import { useEffect, useMemo, useRef, useState, type CSSProperties } from "react";
import {
  confirmReplayContract,
  generateTradeReplay,
  getReplayContractCandidates,
  getTradeReplay,
  regenerateTradeReplay,
  type ContractCandidate,
  type ReplayCandle,
  type ReplayInsight,
  type ReplayMarker,
  type TradeReplayResponse,
  type TradeReplayStatus,
} from "../../api/tradeReplayApi";
import { isTradeReplayProcessingStatus } from "./tradeReplayStatus";

type Props = {
  tradeId: number;
  enabled: boolean;
};

const POLL_INTERVAL_MS = 2500;
const MAX_POLL_ATTEMPTS = 36;

export default function AITradeReplayCard({ tradeId, enabled }: Props) {
  const [data, setData] = useState<TradeReplayResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showAllInsights, setShowAllInsights] = useState(false);
  const [playbackIndex, setPlaybackIndex] = useState<number | null>(null);
  const [activeInsightIndex, setActiveInsightIndex] = useState(0);
  const [speed, setSpeed] = useState(1);
  const [playing, setPlaying] = useState(false);
  const [contractCandidates, setContractCandidates] = useState<ContractCandidate[]>([]);
  const [selectedContract, setSelectedContract] = useState("");
  const [contractLoading, setContractLoading] = useState(false);
  const pollRef = useRef<number | null>(null);
  const attemptsRef = useRef(0);

  useEffect(() => {
    if (!enabled) return;
    let cancelled = false;
    setLoading(true);
    setError(null);
    setPlaybackIndex(null);
    getTradeReplay(tradeId)
      .then((next) => {
        if (!cancelled) {
          setData(next);
          if (isTradeReplayProcessingStatus(next.status)) startPolling();
        }
      })
      .catch(() => {
        if (!cancelled) setError("Unable to load trade replay.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
      stopPolling();
    };
  }, [enabled, tradeId]);

  useEffect(() => {
    if (!playing || !data?.candles?.length) return;
    const interval = window.setInterval(() => {
      setPlaybackIndex((current) => {
        const next = current === null ? 1 : current + 1;
        if (next >= data.candles.length) {
          setPlaying(false);
          return data.candles.length;
        }
        return next;
      });
    }, Math.max(80, 420 / speed));
    return () => window.clearInterval(interval);
  }, [playing, speed, data?.candles?.length]);

  useEffect(() => {
    setActiveInsightIndex(0);
  }, [data?.replayId, data?.generatedAt]);

  useEffect(() => {
    if (!enabled || data?.status !== "CONTRACT_RESOLUTION_FAILED") {
      setContractCandidates([]);
      setSelectedContract("");
      return;
    }
    let cancelled = false;
    setContractLoading(true);
    getReplayContractCandidates(tradeId)
      .then((response) => {
        if (cancelled) return;
        setContractCandidates(response.candidates);
        setSelectedContract(response.candidates.find((candidate) => candidate.mostLikely)?.normalizedSymbol ?? response.candidates[0]?.normalizedSymbol ?? "");
      })
      .catch(() => {
        if (!cancelled) setError("Unable to load contract choices.");
      })
      .finally(() => {
        if (!cancelled) setContractLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [enabled, tradeId, data?.status]);

  if (!enabled) return null;

  const visibleCandles = data?.candles && playbackIndex !== null ? data.candles.slice(0, playbackIndex) : data?.candles ?? [];
  const primaryInsights = (data?.insights ?? []).filter((insight) => insight.primary);
  const insights = showAllInsights ? data?.insights ?? [] : primaryInsights;
  const activeInsight = primaryInsights[activeInsightIndex] ?? primaryInsights[0] ?? null;
  const status = data?.status ?? null;
  const ready = status === "READY";
  const processing = isTradeReplayProcessingStatus(status);
  const demoReplayUnavailable = status === "DEMO_REPLAY_UNAVAILABLE";

  async function requestReplay(force = false) {
    setLoading(true);
    setError(null);
    try {
      const next = force ? await regenerateTradeReplay(tradeId) : await generateTradeReplay(tradeId);
      setData(next);
      if (isTradeReplayProcessingStatus(next.status)) startPolling();
    } catch (err: any) {
      setError(err?.response?.data?.message || "Unable to request trade replay.");
    } finally {
      setLoading(false);
    }
  }

  async function confirmContract() {
    if (!selectedContract) return;
    setLoading(true);
    setError(null);
    try {
      const next = await confirmReplayContract(tradeId, selectedContract);
      setData(next);
      if (isTradeReplayProcessingStatus(next.status)) startPolling();
    } catch (err: any) {
      setError(err?.response?.data?.message || "Unable to confirm contract.");
    } finally {
      setLoading(false);
    }
  }

  function startPolling() {
    stopPolling();
    attemptsRef.current = 0;
    pollRef.current = window.setInterval(async () => {
      attemptsRef.current += 1;
      if (attemptsRef.current > MAX_POLL_ATTEMPTS) {
        stopPolling();
        setError("Replay is still processing. Refresh or try again in a moment.");
        return;
      }
      try {
        const latest = await getTradeReplay(tradeId);
        setData(latest);
        if (!isTradeReplayProcessingStatus(latest.status)) stopPolling();
      } catch {
        stopPolling();
        setError("Replay status could not be refreshed.");
      }
    }, POLL_INTERVAL_MS);
  }

  function stopPolling() {
    if (pollRef.current) window.clearInterval(pollRef.current);
    pollRef.current = null;
  }

  function jumpToNextInsight() {
    if (!data?.candles?.length || !data.insights.length) return;
    const nextIndex = primaryInsights.length ? (activeInsightIndex + 1) % primaryInsights.length : 0;
    const nextInsight = primaryInsights[nextIndex];
    if (!nextInsight?.eventTimestamp) return;
    setActiveInsightIndex(nextIndex);
    const index = data.candles.findIndex((candle) => new Date(candle.timestamp).getTime() >= new Date(nextInsight.eventTimestamp!).getTime());
    if (index >= 0) setPlaybackIndex(index + 1);
  }

  return (
    <section style={card} data-testid="ai-trade-replay">
      <div style={header}>
        <div>
          <h3 style={title}>AI Trade Replay</h3>
          <p style={muted}>{subtitle(data, loading)}</p>
        </div>
        <div style={buttonRow}>
          {!demoReplayUnavailable && !data?.replayId && (
            <button type="button" style={primaryButton(loading)} disabled={loading} onClick={() => void requestReplay(false)}>
              Generate Replay
            </button>
          )}
          {!demoReplayUnavailable && data?.replayId && (
            <button type="button" style={secondaryButton(loading)} disabled={loading || processing} onClick={() => void requestReplay(true)}>
              {status === "FAILED" ? "Try Again" : "Regenerate"}
            </button>
          )}
        </div>
      </div>

      {error && <div style={errorBox}>{error}</div>}
      {loading && <div style={skeleton}>Loading replay workspace...</div>}
      {!loading && status === "CONTRACT_RESOLUTION_FAILED" && (
        <ContractRecovery
          candidates={contractCandidates}
          selectedContract={selectedContract}
          loading={contractLoading || loading}
          onSelect={setSelectedContract}
          onConfirm={() => void confirmContract()}
        />
      )}
      {!loading && !ready && <StatusBlock data={data} />}

      {ready && data && (
        <div style={readyGrid}>
          <div style={chartPanel}>
            <ReplayChart candles={visibleCandles} allCandles={data.candles} markers={data.markers} insights={primaryInsights} activeInsight={activeInsight} />
            <div style={controls}>
              <button type="button" style={controlButton} onClick={() => setPlaying(true)} aria-label="Play replay">Play</button>
              <button type="button" style={controlButton} onClick={() => setPlaying(false)} aria-label="Pause replay">Pause</button>
              <button type="button" style={controlButton} onClick={() => { setPlaybackIndex(null); setPlaying(false); }} aria-label="Restart replay">Restart</button>
              {[1, 2, 4].map((value) => (
                <button key={value} type="button" style={speedButton(speed === value)} onClick={() => setSpeed(value)}>
                  {value}x
                </button>
              ))}
              <button type="button" style={controlButton} onClick={jumpToNextInsight}>Next Insight</button>
              {primaryInsights.length > 0 && <span style={insightCounter}>Insight {activeInsightIndex + 1} of {primaryInsights.length}</span>}
            </div>
          </div>

          <div style={sidePanel}>
            {data.aiSummary && <p style={summary}>{data.aiSummary}</p>}
            <MetricGrid metrics={data.metrics} />
            {data.reflectionQuestion && <div style={reflection}>{data.reflectionQuestion}</div>}
            <div style={smallMeta}>
              {data.generatedAt ? `Generated ${formatDateTime(data.generatedAt)}` : "One-minute historical replay"} · T+1 historical data
            </div>
          </div>
        </div>
      )}

      {ready && insights.length > 0 && (
        <div style={insightGrid}>
          {insights.map((insight) => (
            <InsightCard key={insight.id} insight={insight} active={activeInsight?.id === insight.id} />
          ))}
          {(data?.insights?.length ?? 0) > primaryInsights.length && (
            <button type="button" style={linkButton} onClick={() => setShowAllInsights((value) => !value)}>
              {showAllInsights ? "Show primary pins" : "Reveal all events"}
            </button>
          )}
        </div>
      )}

      {data?.dataQualityWarnings?.map((warning) => (
        <div key={warning} style={warningBox}>{warning}</div>
      ))}
    </section>
  );
}

function StatusBlock({ data }: { data: TradeReplayResponse | null }) {
  const status = data?.status;
  const text = statusText(status, data?.eligibleAt, data?.message);
  return (
    <div style={statusBox}>
      <strong>{text.title}</strong>
      <span>{text.body}</span>
    </div>
  );
}

function ContractRecovery({ candidates, selectedContract, loading, onSelect, onConfirm }: { candidates: ContractCandidate[]; selectedContract: string; loading: boolean; onSelect: (value: string) => void; onConfirm: () => void }) {
  if (!candidates.length) {
    return (
      <div style={recoveryBox}>
        <strong>Contract required</strong>
        <span>Enter the exact futures contract on the trade, then regenerate the replay.</span>
      </div>
    );
  }
  return (
    <div style={recoveryBox}>
      <label style={selectLabel} htmlFor="replay-contract-select">Contract</label>
      <div style={recoveryControls}>
        <select id="replay-contract-select" style={contractSelect} value={selectedContract} disabled={loading} onChange={(event) => onSelect(event.target.value)}>
          {candidates.map((candidate) => (
            <option key={candidate.normalizedSymbol} value={candidate.normalizedSymbol}>
              {candidate.label}{candidate.mostLikely ? " (likely)" : ""}
            </option>
          ))}
        </select>
        <button type="button" style={primaryButton(loading || !selectedContract)} disabled={loading || !selectedContract} onClick={onConfirm}>
          Confirm Contract
        </button>
      </div>
    </div>
  );
}

function ReplayChart({ candles, allCandles, markers, insights, activeInsight }: { candles: ReplayCandle[]; allCandles: ReplayCandle[]; markers: ReplayMarker[]; insights: ReplayInsight[]; activeInsight: ReplayInsight | null }) {
  const bounds = useMemo(() => chartBounds(allCandles), [allCandles]);
  if (!allCandles.length) return <div style={emptyChart}>No candles available.</div>;
  const width = 900;
  const height = 320;
  const pad = { top: 18, right: 18, bottom: 30, left: 46 };
  const plotW = width - pad.left - pad.right;
  const plotH = height - pad.top - pad.bottom;
  const step = plotW / Math.max(1, allCandles.length);
  const candleW = Math.max(3, Math.min(10, step * 0.62));
  const xFor = (timestamp: string) => {
    const index = allCandles.findIndex((candle) => candle.timestamp === timestamp);
    return pad.left + (index < 0 ? 0 : index * step + step / 2);
  };
  const yFor = (price: number) => pad.top + ((bounds.max - price) / Math.max(0.01, bounds.max - bounds.min)) * plotH;
  return (
    <svg viewBox={`0 0 ${width} ${height}`} style={svgChart} role="img" aria-label="AI Trade Replay candlestick chart">
      <rect x={0} y={0} width={width} height={height} fill="rgba(2,6,23,0.45)" rx={12} />
      {[0, 0.25, 0.5, 0.75, 1].map((ratio) => (
        <line key={ratio} x1={pad.left} x2={width - pad.right} y1={pad.top + plotH * ratio} y2={pad.top + plotH * ratio} stroke="rgba(148,163,184,0.16)" />
      ))}
      {candles.map((candle) => {
        const open = number(candle.open);
        const close = number(candle.close);
        const high = number(candle.high);
        const low = number(candle.low);
        const x = xFor(candle.timestamp);
        const up = close >= open;
        const top = yFor(Math.max(open, close));
        const bottom = yFor(Math.min(open, close));
        return (
          <g key={candle.timestamp}>
            <line x1={x} x2={x} y1={yFor(high)} y2={yFor(low)} stroke={up ? "#6ee7b7" : "#fda4af"} strokeWidth={1.2} />
            <rect x={x - candleW / 2} y={top} width={candleW} height={Math.max(2, bottom - top)} fill={up ? "#22c55e" : "#f43f5e"} rx={1.5} />
          </g>
        );
      })}
      {markers.map((marker) => marker.price == null ? null : (
        <g key={`${marker.type}-${marker.timestamp}`}>
          <circle cx={xFor(nearestCandle(allCandles, marker.timestamp)?.timestamp ?? marker.timestamp)} cy={yFor(number(marker.price))} r={marker.type === "ENTRY" ? 5 : 6} fill={marker.type === "ENTRY" ? "#38bdf8" : "#f59e0b"} stroke="#020617" strokeWidth={2} />
          <text x={xFor(nearestCandle(allCandles, marker.timestamp)?.timestamp ?? marker.timestamp) + 9} y={yFor(number(marker.price)) + (marker.type === "ENTRY" ? -10 : 16)} fill="#e2e8f0" fontSize={12} fontWeight={800}>{marker.label}</text>
          <title>{marker.label} · {formatDateTime(marker.timestamp)} · {marker.price}</title>
        </g>
      ))}
      {insights.map((insight) => insight.eventTimestamp && insight.eventPrice != null ? (
        <g key={insight.id}>
          {activeInsight?.id === insight.id && (
            <circle cx={xFor(nearestCandle(allCandles, insight.eventTimestamp)?.timestamp ?? insight.eventTimestamp)} cy={yFor(number(insight.eventPrice))} r={15} fill="none" stroke="#fef08a" strokeWidth={2.5} opacity={0.9} />
          )}
          <path d={`M ${xFor(nearestCandle(allCandles, insight.eventTimestamp)?.timestamp ?? insight.eventTimestamp)} ${yFor(number(insight.eventPrice)) - 10} l 7 14 l -14 0 z`} fill={activeInsight?.id === insight.id ? "#fef08a" : "#a78bfa"} />
          <title>{insight.title}</title>
        </g>
      ) : null)}
      <text x={pad.left} y={height - 8} fill="#64748b" fontSize={11}>{formatTime(allCandles[0]?.timestamp)}</text>
      <text x={width - pad.right - 120} y={height - 8} fill="#64748b" fontSize={11}>{formatTime(allCandles[allCandles.length - 1]?.timestamp)}</text>
    </svg>
  );
}

function MetricGrid({ metrics }: { metrics: Record<string, unknown> }) {
  const items = [
    ["MFE", valueWithUnit(metrics.mfePoints, "pts"), "Maximum favorable excursion while the position was open."],
    ["MAE", valueWithUnit(metrics.maePoints, "pts"), "Maximum adverse excursion while the position was open."],
    ["Realized", valueWithUnit(metrics.realizedPoints, "pts")],
    ["Capture", captureValue(metrics.excursionCapturePct), "Capture = realized favorable movement divided by maximum favorable excursion."],
    ["MFE $", currency(metrics.mfeDollars)],
    ["MAE $", currency(metrics.maeDollars)],
  ];
  return (
    <div style={metricGrid}>
      {items.map(([label, value, tooltip]) => (
        <div key={label} style={metric} title={tooltip}>
          <span>{label}</span>
          <strong>{value}</strong>
        </div>
      ))}
    </div>
  );
}

function InsightCard({ insight, active }: { insight: ReplayInsight; active: boolean }) {
  return (
    <article style={insightCard(active)}>
      <div style={insightHeader}>
        <strong>{insight.title}</strong>
        <span style={severityPill(insight.severity)}>{insight.severity ?? "INFO"}</span>
      </div>
      <p>{insight.aiExplanation || insight.explanation}</p>
      {typeof insight.supportingMetrics?.category === "string" && <span style={categoryPill}>{String(insight.supportingMetrics.category)}</span>}
      {insight.eventTimestamp && <span style={smallMeta}>{formatDateTime(insight.eventTimestamp)}</span>}
    </article>
  );
}

function statusText(status: TradeReplayStatus | null | undefined, eligibleAt?: string | null, message?: string | null) {
  if (!status) return { title: "Replay not requested", body: "Generate a replay to reconstruct one-minute futures candles around this trade." };
  if (status === "WAITING_FOR_MARKET_DATA") return { title: "Waiting for T+1 data", body: `Eligible ${eligibleAt ? formatDateTime(eligibleAt) : "after the replay delay"}.` };
  if (status === "PROVIDER_NOT_CONFIGURED") return { title: "Provider unavailable", body: message ?? "Historical market data is not configured." };
  if (status === "UNSUPPORTED_INSTRUMENT") return { title: "Unsupported instrument", body: message ?? "Replay currently supports MES, MNQ, ES, and NQ futures." };
  if (status === "CONTRACT_RESOLUTION_FAILED") return { title: "Contract resolution failed", body: message ?? "The imported symbol could not be safely resolved." };
  if (status === "NOT_ENOUGH_DATA") return { title: "Not enough candle data", body: message ?? "The provider did not return enough one-minute candles." };
  if (status === "DEMO_REPLAY_UNAVAILABLE") return { title: "Replay not available for this demo trade", body: "This sample trade uses synthetic market prices and isn't connected to historical replay data." };
  if (status === "FAILED") return { title: "Recoverable failure", body: message ?? "Replay generation failed. You can retry." };
  return { title: "Processing replay", body: "TradePilot is preparing one-minute candles and deterministic insights." };
}

function subtitle(data: TradeReplayResponse | null, loading: boolean) {
  if (loading) return "Preparing one-minute historical replay...";
  if (data?.resolvedProviderSymbol) return `${data.resolvedProviderSymbol} · ${data.candleInterval ?? "1m"} candles`;
  return "MES, MNQ, ES, and NQ futures · T+1 historical data";
}

function chartBounds(candles: ReplayCandle[]) {
  const lows = candles.map((candle) => number(candle.low));
  const highs = candles.map((candle) => number(candle.high));
  const min = Math.min(...lows);
  const max = Math.max(...highs);
  const padding = Math.max(0.25, (max - min) * 0.08);
  return { min: min - padding, max: max + padding };
}

function nearestCandle(candles: ReplayCandle[], timestamp: string) {
  const target = new Date(timestamp).getTime();
  return candles.reduce<ReplayCandle | null>((best, candle) => {
    if (!best) return candle;
    return Math.abs(new Date(candle.timestamp).getTime() - target) < Math.abs(new Date(best.timestamp).getTime() - target) ? candle : best;
  }, null);
}

function number(value: number | string | null | undefined) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function valueWithUnit(value: unknown, unit: string) {
  if (value === null || value === undefined || value === "") return "-";
  return `${Number(value).toFixed(2)} ${unit}`;
}

function captureValue(value: unknown) {
  if (value === null || value === undefined || value === "" || Number(value) <= 0) return "Unavailable";
  return `${Number(value).toFixed(2)} %`;
}

function currency(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  return Number(value).toLocaleString([], { style: "currency", currency: "USD" });
}

function formatDateTime(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString([], { month: "short", day: "numeric", hour: "numeric", minute: "2-digit" });
}

function formatTime(value?: string) {
  if (!value) return "";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
}

const card: CSSProperties = { marginTop: 16, padding: 20, borderRadius: 18, border: "1px solid rgba(56,189,248,0.22)", background: "rgba(8,47,73,0.18)" };
const header: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 16, alignItems: "flex-start", flexWrap: "wrap", marginBottom: 14 };
const title: CSSProperties = { margin: "0 0 6px", color: "#f8fafc", fontSize: 18 };
const muted: CSSProperties = { margin: 0, color: "#94a3b8", lineHeight: 1.55 };
const buttonRow: CSSProperties = { display: "flex", gap: 10, flexWrap: "wrap" };
const readyGrid: CSSProperties = { display: "grid", gridTemplateColumns: "minmax(0, 1.7fr) minmax(260px, 0.8fr)", gap: 16 };
const chartPanel: CSSProperties = { minWidth: 0 };
const sidePanel: CSSProperties = { display: "grid", alignContent: "start", gap: 12 };
const svgChart: CSSProperties = { width: "100%", maxHeight: 360, display: "block", borderRadius: 14, overflow: "hidden" };
const emptyChart: CSSProperties = { minHeight: 280, display: "grid", placeItems: "center", color: "#94a3b8" };
const controls: CSSProperties = { display: "flex", gap: 8, flexWrap: "wrap", marginTop: 10 };
const controlButton: CSSProperties = { padding: "8px 10px", borderRadius: 10, border: "1px solid rgba(148,163,184,0.24)", background: "rgba(15,23,42,0.8)", color: "#e2e8f0", fontWeight: 800, cursor: "pointer" };
const insightCounter: CSSProperties = { alignSelf: "center", color: "#94a3b8", fontSize: 12, fontWeight: 900 };
const metricGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(2, minmax(0, 1fr))", gap: 8 };
const metric: CSSProperties = { padding: 10, borderRadius: 12, background: "rgba(15,23,42,0.62)", border: "1px solid rgba(148,163,184,0.16)", display: "grid", gap: 4, color: "#94a3b8" };
const summary: CSSProperties = { margin: 0, color: "#dbeafe", lineHeight: 1.55 };
const reflection: CSSProperties = { padding: 12, borderRadius: 12, background: "rgba(167,139,250,0.12)", color: "#ddd6fe", fontWeight: 800, lineHeight: 1.45 };
const smallMeta: CSSProperties = { color: "#64748b", fontSize: 12, fontWeight: 800 };
const insightGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(230px, 1fr))", gap: 10, marginTop: 14 };
const categoryPill: CSSProperties = { display: "inline-flex", width: "fit-content", marginRight: 8, padding: "3px 7px", borderRadius: 999, background: "rgba(148,163,184,0.12)", color: "#cbd5e1", fontSize: 11, fontWeight: 900 };
const insightHeader: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 10, color: "#f8fafc" };
const statusBox: CSSProperties = { display: "grid", gap: 8, padding: 16, borderRadius: 14, border: "1px solid rgba(148,163,184,0.18)", background: "rgba(15,23,42,0.62)", color: "#cbd5e1" };
const recoveryBox: CSSProperties = { display: "grid", gap: 10, padding: 14, marginBottom: 12, borderRadius: 12, border: "1px solid rgba(56,189,248,0.24)", background: "rgba(14,116,144,0.14)", color: "#cbd5e1" };
const recoveryControls: CSSProperties = { display: "flex", gap: 10, flexWrap: "wrap", alignItems: "center" };
const selectLabel: CSSProperties = { color: "#bae6fd", fontWeight: 900, fontSize: 12 };
const contractSelect: CSSProperties = { minWidth: 220, padding: "10px 12px", borderRadius: 10, border: "1px solid rgba(148,163,184,0.28)", background: "rgba(15,23,42,0.92)", color: "#f8fafc", fontWeight: 800 };
const skeleton: CSSProperties = { padding: 14, borderRadius: 14, background: "rgba(56,189,248,0.1)", color: "#bae6fd", fontWeight: 800 };
const errorBox: CSSProperties = { marginBottom: 12, padding: 12, borderRadius: 12, background: "rgba(251,113,133,0.12)", color: "#fecdd3", fontWeight: 800 };
const warningBox: CSSProperties = { marginTop: 10, padding: 10, borderRadius: 12, background: "rgba(245,158,11,0.12)", color: "#fde68a", fontWeight: 800 };
const linkButton: CSSProperties = { padding: "9px 11px", border: "1px solid rgba(148,163,184,0.24)", borderRadius: 10, background: "transparent", color: "#bae6fd", fontWeight: 900, cursor: "pointer" };

function primaryButton(disabled: boolean): CSSProperties {
  return { padding: "11px 14px", borderRadius: 13, border: "1px solid rgba(56,189,248,0.3)", background: "linear-gradient(135deg, #38bdf8, #22c55e)", color: "#020617", fontWeight: 900, cursor: disabled ? "not-allowed" : "pointer", opacity: disabled ? 0.55 : 1 };
}

function secondaryButton(disabled: boolean): CSSProperties {
  return { padding: "10px 13px", borderRadius: 12, border: "1px solid rgba(148,163,184,0.24)", background: "rgba(15,23,42,0.82)", color: "#e2e8f0", fontWeight: 900, cursor: disabled ? "not-allowed" : "pointer", opacity: disabled ? 0.55 : 1 };
}

function speedButton(active: boolean): CSSProperties {
  return { ...controlButton, background: active ? "rgba(56,189,248,0.2)" : String(controlButton.background), color: active ? "#bae6fd" : String(controlButton.color) };
}

function insightCard(active: boolean): CSSProperties {
  return { padding: 13, borderRadius: 14, border: active ? "1px solid rgba(250,204,21,0.62)" : "1px solid rgba(167,139,250,0.22)", background: active ? "rgba(113,63,18,0.22)" : "rgba(49,46,129,0.2)", color: "#cbd5e1" };
}

function severityPill(severity?: string | null): CSSProperties {
  const important = severity === "IMPORTANT";
  const positive = severity === "POSITIVE";
  const watch = severity === "WATCH";
  return {
    padding: "4px 7px",
    borderRadius: 999,
    background: important ? "rgba(251,113,133,0.14)" : positive ? "rgba(34,197,94,0.14)" : watch ? "rgba(245,158,11,0.14)" : "rgba(56,189,248,0.12)",
    color: important ? "#fecdd3" : positive ? "#bbf7d0" : watch ? "#fde68a" : "#bae6fd",
    fontSize: 11,
    fontWeight: 900
  };
}
