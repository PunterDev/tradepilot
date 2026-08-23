import { useEffect, useMemo, useState, type CSSProperties } from "react";
import { getTradeDetail, updateTradeJournal, type TradeDetail } from "../api/tradeDetailApi";
import { useAiReviewPolling } from "../hooks/useAiReviewPolling";
import type { AiTradeReviewResponse } from "../types/ai";
import type { Trade } from "../types/trade";
import { formatCurrency } from "../utils/dashboardUtils";
import TradingMemoryCard from "./trading-memory/TradingMemoryCard";
import AITradeReplayCard from "./trade-replay/AITradeReplayCard";

type TradeDetailModalProps = {
  tradeId: number;
  onClose: () => void;
  onSelectTrade: (tradeId: number) => void;
  onUsageChanged?: () => void;
  tradingMemoryEnabled?: boolean;
  tradeReplayEnabled?: boolean;
};

const SETUP_OPTIONS = [
  "IFVG",
  "FVG",
  "BREAKER",
  "SMT",
  "ORB",
  "LIQUIDITY_SWEEP",
  "TREND_CONTINUATION",
  "RANGE_REVERSION",
  "OTHER",
];

const CONFIDENCE_OPTIONS = [
  { value: "1", label: "1 Low" },
  { value: "2", label: "2" },
  { value: "3", label: "3" },
  { value: "4", label: "4" },
  { value: "5", label: "5 A+ Setup" },
];

const EMOTION_OPTIONS = [
  "CALM",
  "CONFIDENT",
  "PATIENT",
  "FEAR",
  "FOMO",
  "REVENGE",
  "FRUSTRATED",
  "OVERCONFIDENT",
  "TIRED",
  "OTHER",
];

export default function TradeDetailModal({
  tradeId,
  onClose,
  onSelectTrade,
  onUsageChanged,
  tradingMemoryEnabled = false,
  tradeReplayEnabled = false,
}: TradeDetailModalProps) {
  const [detail, setDetail] = useState<TradeDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [journalSetup, setJournalSetup] = useState("");
  const [journalConfidence, setJournalConfidence] = useState("");
  const [journalEmotion, setJournalEmotion] = useState("");
  const [journalNotes, setJournalNotes] = useState("");
  const [journalSaving, setJournalSaving] = useState(false);
  const [journalError, setJournalError] = useState<string | null>(null);
  const [journalSaved, setJournalSaved] = useState(false);
  const reviewPolling = useAiReviewPolling();

  useEffect(() => {
    let cancelled = false;

    async function loadDetail() {
      setLoading(true);
      setError(null);
      reviewPolling.reset();

      try {
        const nextDetail = await getTradeDetail(tradeId);
        if (!cancelled) {
          setDetail(nextDetail);
        }
      } catch {
        if (!cancelled) {
          setError("Unable to load this trade. It may no longer exist or you may not have access.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void loadDetail();

    return () => {
      cancelled = true;
    };
  }, [tradeId]);

  useEffect(() => {
    if (reviewPolling.isCompleted && reviewPolling.review && detail) {
      setDetail({ ...detail, aiReview: reviewPolling.review, aiReviewCached: true });
      onUsageChanged?.();
    }
  }, [reviewPolling.isCompleted, reviewPolling.review]);

  useEffect(() => {
    if (!detail?.trade) return;
    setJournalSetup(detail.trade.setup ?? "");
    setJournalConfidence(String(detail.trade.confidence ?? detail.trade.confidenceRating ?? ""));
    setJournalEmotion(detail.trade.emotion ?? "");
    setJournalNotes(detail.trade.notes ?? "");
    setJournalError(null);
    setJournalSaved(false);
  }, [detail?.trade?.id]);

  const trade = detail?.trade;
  const displayedReview = reviewPolling.review ?? detail?.aiReview ?? null;
  const reviewIsCached = Boolean(detail?.aiReview && !reviewPolling.isLoading && !reviewPolling.review);

  const reviewRequest = useMemo(() => {
    if (!trade) return null;

    const requestTrade: Trade = {
      id: trade.id,
      accountId: trade.accountId,
      accountName: trade.accountName,
      symbol: trade.symbol,
      side: trade.side ?? "UNKNOWN",
      pnl: toNumber(trade.pnlAmount),
      quantity: trade.quantity ?? undefined,
      entryPrice: toNumber(trade.entryPrice),
      exitPrice: toNumber(trade.exitPrice),
      entryTime: trade.entryTime,
      exitTime: trade.exitTime,
      notes: trade.notes ?? undefined,
      setup: trade.setup,
      confidence: trade.confidence ?? trade.confidenceRating ?? null,
      emotion: trade.emotion,
      session: trade.session,
      mistakeTag: trade.mistakeTag,
      pnlR: trade.pnlR == null ? null : toNumber(trade.pnlR),
      tradeDate: trade.tradeDate,
      psychologyNotes: trade.psychologyNotes,
      lessonLearned: trade.lessonLearned,
    };

    return {
      tradeId: trade.id,
      trade: requestTrade,
      journalText: [
        trade.notes ? `Notes: ${trade.notes}` : "",
        trade.confidence ?? trade.confidenceRating ? `Confidence: ${trade.confidence ?? trade.confidenceRating}/5` : "",
        trade.emotion ? `Emotion: ${humanize(trade.emotion)}` : "",
        trade.psychologyNotes ? `Psychology: ${trade.psychologyNotes}` : "",
        trade.lessonLearned ? `Lesson: ${trade.lessonLearned}` : "",
        trade.mistakeTag ? `Mistake: ${humanize(trade.mistakeTag)}` : "",
        trade.setup ? `Setup: ${humanize(trade.setup)}` : "",
        trade.session ? `Session: ${humanize(trade.session)}` : "",
      ].filter(Boolean).join("\n"),
    };
  }, [trade]);

  async function startReview(forceRefresh: boolean) {
    if (!reviewRequest) return;
    await reviewPolling.start(reviewRequest, { forceRefresh });
  }

  function validateJournal() {
    if (journalSetup.length > 50) return "Setup must be 50 characters or less.";
    const confidence = journalConfidence ? Number(journalConfidence) : null;
    if (confidence !== null && (!Number.isInteger(confidence) || confidence < 1 || confidence > 5)) {
      return "Confidence must be between 1 and 5.";
    }
    if (journalEmotion.length > 50) return "Emotion must be 50 characters or less.";
    if (journalNotes.length > 1000) return "Notes must be 1000 characters or less.";
    return null;
  }

  async function saveJournal() {
    if (!trade) return;
    const validationError = validateJournal();
    if (validationError) {
      setJournalError(validationError);
      setJournalSaved(false);
      return;
    }

    setJournalSaving(true);
    setJournalError(null);
    setJournalSaved(false);
    try {
      const updatedTrade = await updateTradeJournal(trade.id, {
        setup: journalSetup || null,
        confidence: journalConfidence ? Number(journalConfidence) : null,
        emotion: journalEmotion || null,
        notes: journalNotes || null,
      });
      setDetail((current) => current ? { ...current, trade: updatedTrade } : current);
      setJournalSaved(true);
    } catch (err: any) {
      setJournalError(err?.response?.data?.message || "Unable to save journal metadata.");
    } finally {
      setJournalSaving(false);
    }
  }

  return (
    <div style={overlay} onClick={onClose}>
      <section style={modal} data-testid="trade-detail-modal" onClick={(event) => event.stopPropagation()}>
        <div style={header}>
          <div>
            <div style={eyebrow}>Trade Detail</div>
            <h2 style={title}>
              {trade ? `${trade.symbol} ${trade.side ?? ""}` : "Loading trade"}
            </h2>
            {trade && (
              <p style={muted}>
                {formatDateTime(trade.entryTime ?? trade.tradeDate)} · {trade.accountName ?? "Trading account"}
              </p>
            )}
          </div>

          <button type="button" onClick={onClose} style={closeButton} aria-label="Close trade detail">
            ×
          </button>
        </div>

        {loading && (
          <div style={loadingState}>
            <div style={spinner} />
            <span>Loading trade review workspace...</span>
          </div>
        )}

        {!loading && error && <div style={errorState}>{error}</div>}

        {!loading && trade && detail && (
          <>
            <div style={navRow}>
              <button
                type="button"
                disabled={!detail.previousTradeId}
                onClick={() => detail.previousTradeId && onSelectTrade(detail.previousTradeId)}
                style={navButton(!detail.previousTradeId)}
              >
                Previous Trade
              </button>
              <button
                type="button"
                disabled={!detail.nextTradeId}
                onClick={() => detail.nextTradeId && onSelectTrade(detail.nextTradeId)}
                style={navButton(!detail.nextTradeId)}
              >
                Next Trade
              </button>
            </div>

            <div style={metaGrid}>
              <Metric label="Symbol" value={trade.symbol} testId="trade-detail-symbol" />
              <Metric label="Date / Time" value={formatDateTime(trade.entryTime ?? trade.tradeDate)} />
              <Metric label="Session" value={humanize(trade.session)} />
              <Metric label="Account" value={trade.accountName ?? "-"} />
              <Metric label="Side" value={trade.side ?? "-"} testId="trade-detail-side" />
              <Metric label="Quantity" value={String(trade.quantity ?? "-")} />
              <Metric label="Entry" value={formatNumber(trade.entryPrice)} testId="trade-detail-entry" />
              <Metric label="Exit" value={formatNumber(trade.exitPrice)} testId="trade-detail-exit" />
              <Metric
                label="P&L"
                value={formatCurrency(toNumber(trade.pnlAmount))}
                tone={toNumber(trade.pnlAmount) >= 0 ? "positive" : "negative"}
                testId="trade-detail-pnl"
              />
              <Metric label="R Multiple" value={trade.pnlR == null ? "-" : `${toNumber(trade.pnlR).toFixed(2)}R`} />
            </div>

            <div style={twoColumn}>
              <section style={panel}>
                <h3 style={panelTitle}>Journal</h3>
                <div style={journalGrid}>
                  <label style={fieldLabel}>
                    <span style={smallLabel}>Setup</span>
                    <select
                      value={journalSetup}
                      onChange={(event) => {
                        setJournalSetup(event.target.value);
                        setJournalSaved(false);
                      }}
                      style={fieldInput}
                    >
                      <option value="">No setup selected</option>
                      {SETUP_OPTIONS.map((option) => (
                        <option key={option} value={option}>{humanize(option)}</option>
                      ))}
                    </select>
                  </label>

                  <label style={fieldLabel}>
                    <span style={smallLabel}>Confidence</span>
                    <select
                      value={journalConfidence}
                      onChange={(event) => {
                        setJournalConfidence(event.target.value);
                        setJournalSaved(false);
                      }}
                      style={fieldInput}
                    >
                      <option value="">No confidence selected</option>
                      {CONFIDENCE_OPTIONS.map((option) => (
                        <option key={option.value} value={option.value}>{option.label}</option>
                      ))}
                    </select>
                  </label>

                  <label style={fieldLabel}>
                    <span style={smallLabel}>Emotion</span>
                    <select
                      value={journalEmotion}
                      onChange={(event) => {
                        setJournalEmotion(event.target.value);
                        setJournalSaved(false);
                      }}
                      style={fieldInput}
                    >
                      <option value="">No emotion selected</option>
                      {EMOTION_OPTIONS.map((option) => (
                        <option key={option} value={option}>{humanize(option)}</option>
                      ))}
                    </select>
                  </label>
                </div>

                <label style={{ ...fieldLabel, marginTop: 14 }}>
                  <span style={smallLabel}>Notes</span>
                  <textarea
                    value={journalNotes}
                    onChange={(event) => {
                      setJournalNotes(event.target.value);
                      setJournalSaved(false);
                    }}
                    maxLength={1000}
                    placeholder="Waited for displacement confirmation."
                    style={journalTextarea}
                  />
                </label>

                <div style={journalFooter}>
                  <span style={journalHint}>{journalNotes.length}/1000</span>
                  <button
                    type="button"
                    onClick={() => void saveJournal()}
                    disabled={journalSaving}
                    style={journalButton(journalSaving)}
                  >
                    {journalSaving ? "Saving..." : "Save Journal"}
                  </button>
                </div>
                {journalSaved && <div style={successInline}>Journal saved.</div>}
                {journalError && <div style={errorInline}>{journalError}</div>}

                <div style={tagRow}>
                  <Tag label={humanize(trade.setup) || "No setup tagged"} tone="blue" />
                  <Tag label={trade.confidence ?? trade.confidenceRating ? `Confidence ${trade.confidence ?? trade.confidenceRating}` : "No confidence tagged"} tone="gray" />
                  <Tag label={humanize(trade.emotion) || "No emotion tagged"} tone={trade.emotion ? "green" : "gray"} />
                  <Tag label={humanize(trade.mistakeTag) || "No mistake tagged"} tone={trade.mistakeTag ? "red" : "gray"} />
                  <Tag label={trade.followedPlan === false ? "Plan broken" : "Plan followed"} tone={trade.followedPlan === false ? "red" : "green"} />
                </div>
                <TextBlock label="Psychology" value={trade.psychologyNotes} />
                <TextBlock label="Lesson" value={trade.lessonLearned} />
              </section>

              <section style={panel}>
                <div style={sectionHeader}>
                  <h3 style={panelTitle}>AI Review</h3>
                  {reviewIsCached && <span style={cachePill}>Cached</span>}
                </div>

                {displayedReview ? (
                  <AiReview review={displayedReview} />
                ) : (
                  <div style={emptyReview}>
                    <strong>No AI review yet.</strong>
                    <span>Generate one to capture execution, psychology, and improvement notes for this trade.</span>
                  </div>
                )}

                {reviewPolling.isLoading && (
                  <div style={loadingInline}>{reviewPolling.loadingText}</div>
                )}

                {reviewPolling.error && (
                  <div style={errorInline}>{friendlyAiError(reviewPolling.error)}</div>
                )}

                <div style={actionRow}>
                  {!displayedReview && (
                    <button
                      type="button"
                      data-testid="generate-trade-review"
                      onClick={() => void startReview(false)}
                      disabled={reviewPolling.isLoading}
                      style={primaryButton(reviewPolling.isLoading)}
                    >
                      Generate AI Review
                    </button>
                  )}
                  {displayedReview && (
                    <button
                      type="button"
                      data-testid="generate-trade-review"
                      onClick={() => void startReview(true)}
                      disabled={reviewPolling.isLoading}
                      style={primaryButton(reviewPolling.isLoading)}
                    >
                      Regenerate Review
                    </button>
                  )}
                </div>
              </section>
            </div>

            <div style={twoColumn}>
              <section style={panel}>
                <h3 style={panelTitle}>Related Behavioral Insights</h3>
                <List items={detail.relatedBehavioralInsights} empty="No related behavioral insights yet." />
              </section>

              <section style={panel}>
                <h3 style={panelTitle}>Discipline Violations</h3>
                {detail.disciplineViolations.length === 0 ? (
                  <p style={muted}>No discipline violations attached to this trade.</p>
                ) : (
                  <div style={{ display: "grid", gap: 10 }}>
                    {detail.disciplineViolations.map((violation) => (
                      <div key={violation.id} style={violationCard}>
                        <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
                          <strong style={{ color: "#f8fafc" }}>{violation.ruleTitle}</strong>
                          <span style={severityPill(violation.severity)}>{humanize(violation.severity)}</span>
                        </div>
                        <p style={{ ...muted, margin: "8px 0 0" }}>{violation.reason ?? violation.category}</p>
                      </div>
                    ))}
                  </div>
                )}
              </section>
            </div>

            <TradingMemoryCard
              tradeId={trade.id}
              enabled={tradingMemoryEnabled}
              onSelectTrade={onSelectTrade}
            />

            <AITradeReplayCard tradeId={trade.id} enabled={tradeReplayEnabled} />
          </>
        )}
      </section>
    </div>
  );
}

function Metric({ label, value, tone, testId }: { label: string; value: string; tone?: "positive" | "negative"; testId?: string }) {
  return (
    <div style={metricCard}>
      <span style={metricLabel}>{label}</span>
      <strong data-testid={testId} style={{ ...metricValue, color: tone === "positive" ? "#6ee7b7" : tone === "negative" ? "#fda4af" : "#f8fafc" }}>
        {value}
      </strong>
    </div>
  );
}

function TextBlock({ label, value }: { label: string; value?: string | null }) {
  return (
    <div style={{ marginTop: 14 }}>
      <div style={smallLabel}>{label}</div>
      <p style={textBlock}>{value?.trim() || "No note captured."}</p>
    </div>
  );
}

function Tag({ label, tone }: { label: string; tone: "blue" | "red" | "green" | "gray" }) {
  const colors = {
    blue: ["rgba(56,189,248,0.14)", "#7dd3fc", "rgba(56,189,248,0.32)"],
    red: ["rgba(251,113,133,0.12)", "#fda4af", "rgba(251,113,133,0.3)"],
    green: ["rgba(52,211,153,0.12)", "#86efac", "rgba(52,211,153,0.28)"],
    gray: ["rgba(148,163,184,0.1)", "#cbd5e1", "rgba(148,163,184,0.22)"],
  }[tone];

  return <span style={{ ...tag, background: colors[0], color: colors[1], border: `1px solid ${colors[2]}` }}>{label}</span>;
}

function AiReview({ review }: { review: AiTradeReviewResponse }) {
  return (
    <div style={{ display: "grid", gap: 14 }}>
      <div style={{ display: "flex", gap: 12, alignItems: "center" }}>
        <span style={gradePill}>{review.grade ?? "AI"}</span>
        <p style={{ ...muted, margin: 0 }}>{review.summary ?? "AI review is available for this trade."}</p>
      </div>
      <List title="Strengths" items={review.strengths ?? []} empty="No strengths listed." />
      <List title="Mistakes" items={review.mistakes ?? []} empty="No mistakes listed." />
      <List title="Improvement Plan" items={review.improvementPlan ?? []} empty="No improvement plan listed." />
    </div>
  );
}

function List({ title, items, empty }: { title?: string; items: string[]; empty: string }) {
  return (
    <div>
      {title && <div style={smallLabel}>{title}</div>}
      {items.length === 0 ? (
        <p style={muted}>{empty}</p>
      ) : (
        <ul style={list}>
          {items.map((item, index) => (
            <li key={`${item}-${index}`}>{item}</li>
          ))}
        </ul>
      )}
    </div>
  );
}

function toNumber(value: number | string | null | undefined) {
  if (value === null || value === undefined || value === "") return 0;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function formatNumber(value: number | string | null | undefined) {
  if (value === null || value === undefined || value === "") return "-";
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed.toFixed(2) : String(value);
}

function formatDateTime(value?: string) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString([], { month: "short", day: "numeric", year: "numeric", hour: "numeric", minute: "2-digit" });
}

function humanize(value?: string | null) {
  if (!value) return "";
  return value.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (match: string) => match.toUpperCase());
}

function friendlyAiError(value: string) {
  return value.includes("PLAN_LIMIT_EXCEEDED")
    ? "You have reached this month's AI review limit. Cached reviews remain available."
    : value;
}

const overlay: CSSProperties = {
  position: "fixed",
  inset: 0,
  zIndex: 60,
  padding: 24,
  overflowY: "auto",
  background: "rgba(2,6,23,0.82)",
  backdropFilter: "blur(14px)",
};

const modal: CSSProperties = {
  width: "min(1180px, 100%)",
  margin: "24px auto",
  padding: 28,
  borderRadius: 24,
  border: "1px solid rgba(148,163,184,0.24)",
  background: "linear-gradient(180deg, rgba(15,23,42,0.99), rgba(2,6,23,0.96))",
  boxShadow: "0 34px 110px rgba(0,0,0,0.58)",
};

const header: CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  gap: 18,
  marginBottom: 18,
};

const eyebrow: CSSProperties = {
  color: "#38bdf8",
  fontSize: 12,
  fontWeight: 900,
  letterSpacing: "0.08em",
  textTransform: "uppercase",
};

const title: CSSProperties = {
  margin: "8px 0 4px",
  color: "#f8fafc",
  fontSize: 34,
  letterSpacing: "-0.02em",
};

const muted: CSSProperties = {
  color: "#94a3b8",
  lineHeight: 1.6,
};

const closeButton: CSSProperties = {
  width: 40,
  height: 40,
  borderRadius: 10,
  border: "1px solid rgba(148,163,184,0.24)",
  background: "rgba(15,23,42,0.86)",
  color: "#cbd5e1",
  cursor: "pointer",
  fontSize: 26,
  lineHeight: 1,
};

const navRow: CSSProperties = {
  display: "flex",
  justifyContent: "flex-end",
  gap: 10,
  marginBottom: 16,
};

function navButton(disabled: boolean): CSSProperties {
  return {
    padding: "10px 14px",
    borderRadius: 12,
    border: "1px solid rgba(148,163,184,0.24)",
    background: "rgba(15,23,42,0.85)",
    color: "#e2e8f0",
    fontWeight: 800,
    opacity: disabled ? 0.45 : 1,
    cursor: disabled ? "not-allowed" : "pointer",
  };
}

const metaGrid: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))",
  gap: 12,
  marginBottom: 18,
};

const metricCard: CSSProperties = {
  padding: 14,
  borderRadius: 14,
  border: "1px solid rgba(148,163,184,0.18)",
  background: "rgba(15,23,42,0.72)",
};

const metricLabel: CSSProperties = {
  display: "block",
  color: "#64748b",
  fontSize: 11,
  fontWeight: 900,
  textTransform: "uppercase",
  letterSpacing: "0.06em",
  marginBottom: 7,
};

const metricValue: CSSProperties = {
  color: "#f8fafc",
  fontSize: 17,
};

const twoColumn: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))",
  gap: 16,
  marginTop: 16,
};

const panel: CSSProperties = {
  padding: 20,
  borderRadius: 18,
  border: "1px solid rgba(148,163,184,0.18)",
  background: "rgba(15,23,42,0.68)",
};

const journalGrid: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))",
  gap: 12,
  marginBottom: 12,
};

const fieldLabel: CSSProperties = {
  display: "grid",
  gap: 7,
};

const fieldInput: CSSProperties = {
  width: "100%",
  padding: "11px 12px",
  borderRadius: 12,
  border: "1px solid rgba(148,163,184,0.24)",
  background: "rgba(2,6,23,0.72)",
  color: "#f8fafc",
  colorScheme: "dark",
  outline: "none",
};

const journalTextarea: CSSProperties = {
  ...fieldInput,
  minHeight: 106,
  resize: "vertical",
  lineHeight: 1.55,
  fontFamily: "inherit",
};

const journalFooter: CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  gap: 12,
  marginTop: 12,
  marginBottom: 14,
  flexWrap: "wrap",
};

const journalHint: CSSProperties = {
  color: "#64748b",
  fontSize: 12,
  fontWeight: 800,
};

function journalButton(disabled: boolean): CSSProperties {
  return {
    padding: "11px 15px",
    borderRadius: 13,
    border: "1px solid rgba(52,211,153,0.32)",
    background: "linear-gradient(135deg, #22c55e, #16a34a)",
    color: "#ffffff",
    fontWeight: 900,
    cursor: disabled ? "not-allowed" : "pointer",
    opacity: disabled ? 0.55 : 1,
    boxShadow: disabled ? "none" : "0 14px 32px rgba(34,197,94,0.22)",
  };
}

const successInline: CSSProperties = {
  marginBottom: 12,
  padding: "10px 12px",
  borderRadius: 12,
  background: "rgba(34,197,94,0.12)",
  color: "#bbf7d0",
  fontWeight: 850,
};

const sectionHeader: CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  gap: 12,
  marginBottom: 14,
};

const panelTitle: CSSProperties = {
  margin: "0 0 14px",
  color: "#f8fafc",
  fontSize: 18,
  letterSpacing: "-0.01em",
};

const tagRow: CSSProperties = {
  display: "flex",
  flexWrap: "wrap",
  gap: 8,
};

const tag: CSSProperties = {
  padding: "7px 10px",
  borderRadius: 999,
  fontSize: 12,
  fontWeight: 900,
};

const smallLabel: CSSProperties = {
  color: "#cbd5e1",
  fontSize: 12,
  fontWeight: 900,
  textTransform: "uppercase",
  letterSpacing: "0.06em",
  marginBottom: 8,
};

const textBlock: CSSProperties = {
  margin: 0,
  minHeight: 42,
  color: "#cbd5e1",
  lineHeight: 1.6,
};

const cachePill: CSSProperties = {
  padding: "6px 9px",
  borderRadius: 999,
  border: "1px solid rgba(52,211,153,0.28)",
  background: "rgba(52,211,153,0.1)",
  color: "#86efac",
  fontSize: 11,
  fontWeight: 900,
};

const emptyReview: CSSProperties = {
  display: "grid",
  gap: 8,
  padding: 16,
  borderRadius: 14,
  border: "1px solid rgba(56,189,248,0.22)",
  background: "rgba(8,47,73,0.22)",
  color: "#cbd5e1",
};

const loadingInline: CSSProperties = {
  marginTop: 14,
  padding: "10px 12px",
  borderRadius: 12,
  background: "rgba(56,189,248,0.1)",
  color: "#bae6fd",
  fontWeight: 800,
};

const errorInline: CSSProperties = {
  marginTop: 14,
  padding: "10px 12px",
  borderRadius: 12,
  background: "rgba(251,113,133,0.12)",
  color: "#fecdd3",
  fontWeight: 800,
};

const actionRow: CSSProperties = {
  display: "flex",
  justifyContent: "flex-end",
  marginTop: 16,
};

function primaryButton(disabled: boolean): CSSProperties {
  return {
    minWidth: 190,
    padding: "12px 16px",
    borderRadius: 14,
    border: "1px solid rgba(52,211,153,0.3)",
    background: "linear-gradient(135deg, #10b981, #38bdf8)",
    color: "#020617",
    fontWeight: 900,
    cursor: disabled ? "not-allowed" : "pointer",
    opacity: disabled ? 0.55 : 1,
  };
}

const gradePill: CSSProperties = {
  minWidth: 46,
  height: 46,
  borderRadius: 14,
  display: "grid",
  placeItems: "center",
  background: "rgba(52,211,153,0.14)",
  border: "1px solid rgba(52,211,153,0.32)",
  color: "#86efac",
  fontWeight: 1000,
  fontSize: 18,
};

const list: CSSProperties = {
  margin: 0,
  paddingLeft: 18,
  color: "#cbd5e1",
  lineHeight: 1.65,
};

const violationCard: CSSProperties = {
  padding: 13,
  borderRadius: 14,
  border: "1px solid rgba(251,113,133,0.22)",
  background: "rgba(76,29,55,0.22)",
};

function severityPill(severity?: string): CSSProperties {
  const high = severity?.toLowerCase().includes("high");
  return {
    padding: "5px 8px",
    borderRadius: 999,
    background: high ? "rgba(239,68,68,0.16)" : "rgba(245,158,11,0.12)",
    color: high ? "#fecaca" : "#fde68a",
    fontSize: 11,
    fontWeight: 900,
  };
}

const loadingState: CSSProperties = {
  minHeight: 260,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  gap: 12,
  color: "#cbd5e1",
};

const spinner: CSSProperties = {
  width: 24,
  height: 24,
  borderRadius: "50%",
  border: "3px solid rgba(148,163,184,0.25)",
  borderTopColor: "#38bdf8",
};

const errorState: CSSProperties = {
  padding: 18,
  borderRadius: 14,
  border: "1px solid rgba(251,113,133,0.28)",
  background: "rgba(127,29,29,0.2)",
  color: "#fecdd3",
  fontWeight: 800,
};
