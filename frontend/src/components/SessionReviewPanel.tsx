import { useEffect, useMemo, useState, type CSSProperties } from "react";
import {
  generateSessionReview,
  getSessionReview,
  type SessionReview,
  type SessionReviewTrade,
  type SessionTimelineTrade,
} from "../api/sessionReviewApi";
import type { TradingAccount } from "../api/accountApi";
import type { SelectedAccountId } from "../types/dashboard";
import type { TradovateTrade } from "../utils/tradovateCsv";
import { formatCurrency, getTradeCalendarDate } from "../utils/dashboardUtils";

type Props = {
  accounts: TradingAccount[];
  selectedAccountId: SelectedAccountId;
  trades: TradovateTrade[];
  onTradeSelect: (tradeId: number) => void;
};

export default function SessionReviewPanel({ accounts, selectedAccountId, trades, onTradeSelect }: Props) {
  const defaultAccountId = typeof selectedAccountId === "number" ? selectedAccountId : accounts[0]?.id ?? "";
  const defaultDate = useMemo(() => {
    const firstTrade = trades.find((trade) => trade.tradeDate || trade.soldTimestamp);
    return firstTrade ? getTradeCalendarDate(firstTrade) ?? today() : today();
  }, [trades]);

  const [accountId, setAccountId] = useState<number | "">(defaultAccountId);
  const [date, setDate] = useState(defaultDate);
  const [review, setReview] = useState<SessionReview | null>(null);
  const [loading, setLoading] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (defaultAccountId) setAccountId((current) => current || defaultAccountId);
  }, [defaultAccountId]);

  useEffect(() => {
    if (date === today() && defaultDate) setDate(defaultDate);
  }, [defaultDate]);

  useEffect(() => {
    if (!accountId || !date) return;
    void loadReview(accountId, date);
  }, [accountId, date]);

  async function loadReview(nextAccountId: number, nextDate: string) {
    setLoading(true);
    setError(null);
    try {
      setReview(await getSessionReview(nextAccountId, nextDate));
    } catch {
      setReview(null);
      setError("Unable to load this session review.");
    } finally {
      setLoading(false);
    }
  }

  async function handleGenerate(forceRefresh: boolean) {
    if (!accountId || !date) return;
    setGenerating(true);
    setError(null);
    try {
      setReview(await generateSessionReview({ accountId, date, forceRefresh }));
    } catch (err: any) {
      setError(err?.response?.data?.message || "Unable to generate the session review.");
    } finally {
      setGenerating(false);
    }
  }

  const timelineTrades: SessionTimelineTrade[] = review?.timelineTrades?.length
    ? review.timelineTrades
    : review?.trades.map((trade) => ({
        tradeId: trade.id,
        symbol: trade.symbol,
        side: trade.side,
        entryTime: trade.entryTime,
        exitTime: trade.exitTime,
        quantity: trade.quantity,
        pnl: trade.pnlAmount,
        setup: trade.setup,
        outcome: timelineOutcome(toNumber(trade.pnlAmount)),
      })) ?? [];
  const hasTrades = timelineTrades.length > 0;
  const ruleForTomorrow = review?.aiSessionReview?.oneRuleForTomorrow || review?.worstTradeWhy || "Review the timeline, then define one rule before the next session.";

  return (
    <section style={shell}>
      <div style={topDeck}>
        <div style={controlGrid}>
          <Control label="Account">
            <select value={accountId} onChange={(event) => setAccountId(event.target.value ? Number(event.target.value) : "")} style={input}>
              {accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}
            </select>
          </Control>
          <Control label="Trading Date">
            <input type="date" value={date} onChange={(event) => setDate(event.target.value)} style={input} />
          </Control>
          <button type="button" disabled={!review?.previousTradingDay} onClick={() => review?.previousTradingDay && setDate(review.previousTradingDay)} style={subtleButton(!review?.previousTradingDay)}>Previous Day</button>
          <button type="button" disabled={!review?.nextTradingDay} onClick={() => review?.nextTradingDay && setDate(review.nextTradingDay)} style={subtleButton(!review?.nextTradingDay)}>Next Day</button>
          <button type="button" disabled={!hasTrades || generating} onClick={() => void handleGenerate(false)} style={primaryButton(!hasTrades || generating)}>{generating ? "Generating..." : "Generate Review"}</button>
          <button type="button" disabled={!hasTrades || generating || !review?.aiSessionReview} onClick={() => void handleGenerate(true)} style={subtleButton(!hasTrades || generating || !review?.aiSessionReview)}>{generating ? "Refreshing..." : "Refresh Review"}</button>
        </div>

        {error && <div style={errorBox}>{error}</div>}
        {loading && <div style={loadingBox}>Loading session film...</div>}

        {review && (
          <>
            <div style={headlineGrid}>
              <div style={gradeCard(review.sessionGrade)}>
                <span style={smallLabel}>Session Grade</span>
                <strong style={{ fontSize: 52, lineHeight: 1 }}>{review.sessionGrade ?? "N/A"}</strong>
              </div>
              <Metric label="Net P&L" value={formatCurrency(toNumber(review.netPnl ?? review.totalPnl))} tone={toNumber(review.netPnl ?? review.totalPnl) >= 0 ? "positive" : "negative"} />
              <Metric label="Trades" value={String(review.totalTrades ?? review.trades.length)} />
              <Metric label="Win Rate" value={`${toNumber(review.winRate).toFixed(1)}%`} />
              <Metric label="Profit Factor" value={toNumber(review.profitFactor).toFixed(2)} />
              <Metric label="Discipline" value={`${review.sessionDisciplineScore}/100`} tone={review.sessionDisciplineScore >= 80 ? "positive" : "negative"} />
            </div>

            {!hasTrades ? (
              <EmptyFilm />
            ) : (
              <div style={filmLayout}>
                <main>
                  <section style={panel}>
                    <div style={sectionHeader}>
                      <div>
                        <div style={eyebrow}>Game Film</div>
                        <h2 style={sectionTitle}>Session Timeline</h2>
                      </div>
                      <span style={muted}>{timelineTrades.length} executions</span>
                    </div>
                    <div style={timeline}>
                      {timelineTrades.map((trade, index) => (
                        <TimelineTrade key={trade.tradeId} trade={trade} index={index} onTradeSelect={onTradeSelect} />
                      ))}
                    </div>
                  </section>

                  <section style={panel}>
                    <div style={eyebrow}>Session Story</div>
                    <p style={storyText}>{review.sessionStory}</p>
                  </section>

                  <div style={twoCol}>
                    <TradeSpotlight title="Best Trade" trade={review.bestTrade} why={review.bestTradeWhy} emptyLabel="No meaningful winner" positive />
                    <TradeSpotlight title="Worst Trade" trade={review.worstTrade} why={review.worstTradeWhy} emptyLabel="No losing trade" />
                  </div>

                  <section style={panel}>
                    <div style={sectionHeader}>
                      <div>
                        <div style={eyebrow}>Mistake Detector</div>
                        <h2 style={sectionTitle}>Detected Issues</h2>
                      </div>
                    </div>
                    {review.detectedMistakes?.length ? (
                      <div style={mistakeGrid}>
                        {review.detectedMistakes.map((mistake) => (
                          <div key={mistake.type} style={mistakeCard(mistake.severity)}>
                            <strong>{mistake.title}</strong>
                            <p>{mistake.description}</p>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <p style={muted}>No major session mistakes detected from the available trade data.</p>
                    )}
                  </section>

                  <section style={panel}>
                    <div style={eyebrow}>Replay Insights</div>
                    <div style={insightGrid}>
                      <Insight label="First Trade" value={tradeLabel(review.replayInsights?.firstTrade)} />
                      <Insight label="Last Trade" value={tradeLabel(review.replayInsights?.lastTrade)} />
                      <Insight label="Largest Winner" value={tradeLabel(review.replayInsights?.largestWinner, "No meaningful winner")} />
                      <Insight label="Largest Loser" value={tradeLabel(review.replayInsights?.largestLoser, "No losing trade")} />
                      <Insight label="Longest Hold" value={holdLabel(review.replayInsights?.longestHold, review.replayInsights?.longestHoldMinutes)} />
                      <Insight label="Shortest Hold" value={holdLabel(review.replayInsights?.shortestHold, review.replayInsights?.shortestHoldMinutes)} />
                      <Insight label="Best Hour" value={hourLabel(review.replayInsights?.bestTradingHour, review.replayInsights?.bestTradingHourPnl, "No profitable hour")} />
                      <Insight label="Worst Hour" value={hourLabel(review.replayInsights?.worstTradingHour, review.replayInsights?.worstTradingHourPnl, "No losing hour")} />
                    </div>
                  </section>
                </main>

                <aside style={sideRail}>
                  <section style={ruleCard}>
                    <div style={eyebrow}>Rule For Tomorrow</div>
                    <p>{ruleForTomorrow}</p>
                  </section>

                  <section style={panel}>
                    <div style={sectionHeader}>
                      <div>
                        <div style={eyebrow}>AI Session Coach</div>
                        <h2 style={sectionTitle}>Coach Notes</h2>
                      </div>
                    </div>
                    {review.aiReviewCached && <p style={cached}>Cached review. No credits used.</p>}
                    {review.aiSessionReview ? (
                      <div style={coachGrid}>
                        <CoachBlock title="Session Summary" value={review.aiSessionReview.summary} />
                        <CoachBlock title="What Went Well" value={review.aiSessionReview.whatWentWell} />
                        <CoachBlock title="What Hurt Performance" value={review.aiSessionReview.whatHurtPerformance} />
                        <CoachBlock title="Emotional Pattern" value={review.aiSessionReview.emotionalPattern} />
                        <CoachBlock title="Execution Pattern" value={review.aiSessionReview.executionPattern} />
                        <CoachBlock title="Risk Management" value={review.aiSessionReview.riskManagementPattern} />
                      </div>
                    ) : (
                      <p style={muted}>Generate the AI coach review to add narrative coaching and a focused next-session rule.</p>
                    )}
                  </section>
                </aside>
              </div>
            )}
          </>
        )}
      </div>
    </section>
  );
}

function Control({ label, children }: { label: string; children: React.ReactNode }) {
  return <label style={{ display: "grid", gap: 7 }}><span style={smallLabel}>{label}</span>{children}</label>;
}

function Metric({ label, value, tone }: { label: string; value: string; tone?: "positive" | "negative" }) {
  return <div style={metricCard}><span style={smallLabel}>{label}</span><strong style={{ color: tone === "positive" ? "#6ee7b7" : tone === "negative" ? "#fda4af" : "#f8fafc" }}>{value}</strong></div>;
}

function TimelineTrade({ trade, index, onTradeSelect }: { trade: SessionTimelineTrade; index: number; onTradeSelect: (tradeId: number) => void }) {
  const pnl = toNumber(trade.pnl);
  return (
    <button type="button" onClick={() => onTradeSelect(trade.tradeId)} style={timelineCard(pnl)}>
      <div style={timeBlock}>{formatTime(trade.entryTime)}</div>
      <div style={{ flex: 1 }}>
        <strong>{index + 1}. {trade.symbol} {trade.side}</strong>
        <span>{humanize(trade.setup)} · {humanize(trade.outcome)}</span>
      </div>
      <strong style={{ color: pnl > 0 ? "#6ee7b7" : pnl < 0 ? "#fda4af" : "#cbd5e1", fontSize: 20 }}>{formatCurrency(pnl)}</strong>
    </button>
  );
}

function TradeSpotlight({
  title,
  trade,
  why,
  emptyLabel,
  positive,
}: {
  title: string;
  trade?: SessionReviewTrade | null;
  why?: string;
  emptyLabel: string;
  positive?: boolean;
}) {
  return (
    <section style={spotlight(positive, !trade)}>
      <div style={smallLabel}>{title}</div>
      {trade ? (
        <>
          <h3>{trade.symbol} {trade.side}</h3>
          <strong>{formatCurrency(toNumber(trade.pnlAmount))}</strong>
          <p>{humanize(trade.setup)} · {why}</p>
        </>
      ) : <p style={neutralEmptyText}>{emptyLabel}</p>}
    </section>
  );
}

function CoachBlock({ title, value }: { title: string; value?: string }) {
  return <div style={coachBlock}><span style={smallLabel}>{title}</span><p>{value || "Not available yet."}</p></div>;
}

function Insight({ label, value }: { label: string; value: string }) {
  return <div style={metricCard}><span style={smallLabel}>{label}</span><strong>{value}</strong></div>;
}

function EmptyFilm() {
  return <div style={emptyState}><strong>No trades on this date.</strong><span>Select a trading day with imported trades to review the session film.</span></div>;
}

function today() { return new Date().toISOString().slice(0, 10); }
function toNumber(value: number | string | null | undefined) { const parsed = Number(value ?? 0); return Number.isFinite(parsed) ? parsed : 0; }
function timelineOutcome(pnl: number) {
  if (Math.abs(pnl) <= 5) return "BREAKEVEN";
  return pnl > 0 ? "WIN" : "LOSS";
}
function humanize(value?: string | null) { return value ? value.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (m) => m.toUpperCase()) : "Untagged"; }
function formatTime(value?: string) { if (!value) return "-"; const parsed = new Date(value); return Number.isNaN(parsed.getTime()) ? value : parsed.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" }); }
function tradeLabel(trade?: SessionTimelineTrade | null, fallback = "-") { return trade ? `${trade.symbol} ${formatCurrency(toNumber(trade.pnl))}` : fallback; }
function holdLabel(trade?: SessionTimelineTrade | null, minutes?: number | null) { return trade ? `${trade.symbol} · ${minutes ?? 0}m` : "-"; }
function hourLabel(hour?: number | null, pnl?: number | string | null, fallback = "-") { return hour == null ? fallback : `${String(hour).padStart(2, "0")}:00 · ${formatCurrency(toNumber(pnl))}`; }

const shell: CSSProperties = { marginTop: 8 };
const topDeck: CSSProperties = { display: "grid", gap: 18 };
const controlGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))", gap: 12, alignItems: "end" };
const input: CSSProperties = { width: "100%", padding: "12px 13px", borderRadius: 14, border: "1px solid rgba(148,163,184,0.24)", background: "rgba(2,6,23,0.72)", color: "#f8fafc", colorScheme: "dark" };
const headlineGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(130px, 1fr))", gap: 12 };
const filmLayout: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 360px), 1fr))", gap: 24, alignItems: "start", overflow: "visible" };
const panel: CSSProperties = { padding: 20, borderRadius: 24, border: "1px solid rgba(56,189,248,0.18)", background: "linear-gradient(180deg, rgba(15,23,42,0.86), rgba(2,6,23,0.68))", boxShadow: "0 24px 80px rgba(0,0,0,0.24)", marginBottom: 18, minHeight: "auto", overflow: "visible" };
const sideRail: CSSProperties = { minWidth: 0, display: "flex", flexDirection: "column", gap: 24, minHeight: "auto", overflow: "visible" };
const sectionHeader: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 12, alignItems: "center", flexWrap: "wrap" };
const sectionTitle: CSSProperties = { margin: "4px 0 0", color: "#f8fafc", fontSize: 24, letterSpacing: "-0.02em" };
const eyebrow: CSSProperties = { color: "#38bdf8", fontSize: 12, fontWeight: 950, textTransform: "uppercase", letterSpacing: "0.08em" };
const muted: CSSProperties = { color: "#94a3b8", lineHeight: 1.6 };
const smallLabel: CSSProperties = { color: "#94a3b8", fontSize: 11, fontWeight: 950, letterSpacing: "0.07em", textTransform: "uppercase" };
const metricCard: CSSProperties = { padding: 15, borderRadius: 18, border: "1px solid rgba(148,163,184,0.16)", background: "rgba(2,6,23,0.5)", display: "grid", gap: 8, color: "#f8fafc" };
const timeline: CSSProperties = { display: "grid", gap: 12, marginTop: 18 };
const timeBlock: CSSProperties = { minWidth: 86, color: "#bae6fd", fontWeight: 950, fontSize: 13 };
const twoCol: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))", gap: 18, marginBottom: 18 };
const storyText: CSSProperties = { color: "#dbeafe", fontSize: 19, lineHeight: 1.75, margin: "12px 0 0" };
const mistakeGrid: CSSProperties = { display: "grid", gap: 10, marginTop: 14 };
const insightGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))", gap: 12, marginTop: 16 };
const coachGrid: CSSProperties = { display: "grid", gap: 12, marginTop: 14 };
const cached: CSSProperties = { color: "#86efac", fontSize: 13, fontWeight: 800 };
const coachBlock: CSSProperties = { padding: 14, borderRadius: 16, border: "1px solid rgba(148,163,184,0.14)", background: "rgba(2,6,23,0.42)", color: "#cbd5e1" };
const ruleCard: CSSProperties = { ...panel, marginBottom: 0, borderColor: "rgba(52,211,153,0.36)", background: "linear-gradient(180deg, rgba(6,78,59,0.42), rgba(2,6,23,0.78))", fontSize: 24, lineHeight: 1.35, color: "#dcfce7", fontWeight: 900 };
const errorBox: CSSProperties = { padding: 13, borderRadius: 14, border: "1px solid rgba(251,113,133,0.28)", background: "rgba(127,29,29,0.22)", color: "#fecdd3", fontWeight: 800 };
const loadingBox: CSSProperties = { padding: 18, color: "#cbd5e1", borderRadius: 16, border: "1px solid rgba(56,189,248,0.18)", background: "rgba(8,47,73,0.22)" };
const emptyState: CSSProperties = { display: "grid", gap: 8, padding: 20, borderRadius: 18, border: "1px solid rgba(56,189,248,0.22)", background: "rgba(8,47,73,0.22)", color: "#cbd5e1" };
const neutralEmptyText: CSSProperties = { color: "#cbd5e1", fontWeight: 850, margin: "12px 0 0" };

function gradeCard(grade?: string): CSSProperties {
  const color = grade === "A+" || grade === "A" ? "#6ee7b7" : grade === "B" ? "#bae6fd" : grade === "C" ? "#fde68a" : "#fda4af";
  return { ...metricCard, borderColor: color, color, justifyContent: "center" };
}
function timelineCard(pnl: number): CSSProperties {
  const color = pnl > 0 ? "rgba(52,211,153,0.26)" : pnl < 0 ? "rgba(251,113,133,0.26)" : "rgba(148,163,184,0.18)";
  return { width: "100%", display: "flex", alignItems: "center", gap: 14, padding: 16, borderRadius: 20, border: `1px solid ${color}`, background: "rgba(2,6,23,0.52)", color: "#f8fafc", textAlign: "left", cursor: "pointer" };
}
function spotlight(positive?: boolean, neutral?: boolean): CSSProperties {
  if (neutral) {
    return { padding: 20, borderRadius: 24, border: "1px solid rgba(148,163,184,0.22)", background: "rgba(15,23,42,0.42)", color: "#f8fafc" };
  }
  return { padding: 20, borderRadius: 24, border: `1px solid ${positive ? "rgba(52,211,153,0.28)" : "rgba(251,113,133,0.28)"}`, background: positive ? "rgba(6,78,59,0.24)" : "rgba(127,29,29,0.2)", color: "#f8fafc" };
}
function mistakeCard(severity: string): CSSProperties {
  return { padding: 15, borderRadius: 16, border: `1px solid ${severity === "HIGH" ? "rgba(251,113,133,0.32)" : "rgba(251,191,36,0.28)"}`, background: severity === "HIGH" ? "rgba(127,29,29,0.22)" : "rgba(113,63,18,0.18)", color: "#f8fafc" };
}
function primaryButton(disabled: boolean): CSSProperties {
  return { padding: "12px 15px", borderRadius: 14, border: "1px solid rgba(52,211,153,0.3)", background: "linear-gradient(135deg, #10b981, #38bdf8)", color: "#020617", fontWeight: 900, cursor: disabled ? "not-allowed" : "pointer", opacity: disabled ? 0.55 : 1 };
}
function subtleButton(disabled: boolean): CSSProperties {
  return { padding: "12px 14px", borderRadius: 14, border: "1px solid rgba(148,163,184,0.22)", background: "rgba(2,6,23,0.42)", color: "#cbd5e1", fontWeight: 850, cursor: disabled ? "not-allowed" : "pointer", opacity: disabled ? 0.45 : 1 };
}
