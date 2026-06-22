import { useEffect, useMemo, useState, type CSSProperties } from "react";
import type { TradingAccount } from "../../api/accountApi";
import { getLeakDetector, regenerateLeakDetector } from "../../api/leakDetectorApi";
import PageHeader from "../../layout/PageHeader";
import { button } from "../../styles/dashboardStyles";
import type { BillingUsage } from "../../types/billing";
import type { SelectedAccountId } from "../../types/dashboard";
import type { LeakDetectorResponse, LeakResult, LeakSeverity } from "../../types/leakDetectorTypes";

type Props = {
  accounts: TradingAccount[];
  selectedAccountId: SelectedAccountId;
  usage?: BillingUsage | null;
  onSelectAccount: (accountId: SelectedAccountId) => void;
  onUsageChanged: () => void;
  onTradeSelect: (tradeId: number) => void;
};

export default function LeakDetectorPage({
  accounts,
  selectedAccountId,
  usage,
  onSelectAccount,
  onUsageChanged,
  onTradeSelect,
}: Props) {
  const initialAccountId = useMemo(() => {
    if (selectedAccountId === "ALL" || selectedAccountId === "") return accounts[0]?.id;
    return Number(selectedAccountId);
  }, [accounts, selectedAccountId]);
  const [accountId, setAccountId] = useState<number | undefined>(initialAccountId);
  const [data, setData] = useState<LeakDetectorResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [regenerating, setRegenerating] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    setAccountId(initialAccountId);
  }, [initialAccountId]);

  async function load() {
    if (!accountId) return;
    setLoading(true);
    setError("");
    try {
      setData(await getLeakDetector(accountId));
    } catch (err: any) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  async function regenerate() {
    if (!accountId) return;
    setRegenerating(true);
    setError("");
    try {
      setData(await regenerateLeakDetector(accountId));
      onUsageChanged();
    } catch (err: any) {
      setError(errorMessage(err));
    } finally {
      setRegenerating(false);
    }
  }

  useEffect(() => {
    void load();
  }, [accountId]);

  const remaining = usage?.remaining?.AI_CREDITS ?? 0;

  return (
    <>
      <PageHeader
        label="AI Leak Detector"
        title="AI Leak Detector"
        description="Find the patterns quietly costing you money."
      />

      <section style={shell}>
        <div style={toolbar}>
          <div style={controlGroup}>
            <label style={label}>Account</label>
            <select
              value={accountId ?? ""}
              onChange={(event) => {
                const next = Number(event.target.value);
                setAccountId(next);
                onSelectAccount(next);
              }}
              style={select}
            >
              {accounts.map((account) => (
                <option key={account.id} value={account.id}>{account.name}</option>
              ))}
            </select>
          </div>
          <div style={creditBadge}>3 AI credits</div>
          <button type="button" onClick={regenerate} disabled={!accountId || regenerating} style={{ ...button, width: 150, borderRadius: 8 }}>
            {regenerating ? "Regenerating..." : "Regenerate"}
          </button>
        </div>

        {remaining < 3 && (
          <div style={notice}>You have {remaining} AI credits remaining. Cached leak results remain viewable.</div>
        )}

        {loading && <div style={notice}>Analyzing saved trades...</div>}
        {error && <div style={errorBox}>{error}</div>}

        {!loading && data && data.leaks.length === 0 && (
          <div style={emptyState}>
            <h2 style={emptyTitle}>Not enough trade history for high-confidence leak detection yet.</h2>
            <p style={muted}>Import more trades or journal at least 20 trades for stronger leak detection.</p>
          </div>
        )}

        {data && data.leaks.length > 0 && (
          <>
            <SummaryHero data={data} />
            <RecoveryPlan leak={data.leaks[0]} />
            <div style={grid}>
              {data.leaks.map((leak, index) => (
                <LeakCard
                  key={`${leak.leakType}-${leak.title}`}
                  leak={leak}
                  rank={index + 1}
                  onTradeSelect={onTradeSelect}
                />
              ))}
            </div>
          </>
        )}
      </section>
    </>
  );
}

function SummaryHero({ data }: { data: LeakDetectorResponse }) {
  const topLeak = data.leaks[0];
  const monthlyImpact = data.biggestLeakImpact ?? topLeak?.estimatedMonthlyImpact ?? 0;
  const confidence = data.highestConfidence ?? topLeak?.confidenceScore ?? 0;
  const sampleTradeCount = data.sampleTradeCount ?? data.totalTrades;
  const yearlyImprovement = formatPotentialImprovement(monthlyImpact);
  const dataQuality = getDataQualityLabel(sampleTradeCount);
  return (
    <section style={heroCard}>
      <div>
        <div style={heroTopline}>
          <span style={heroEyebrow}>Performance Leak Report</span>
          <span style={reportPill}>{data.cached ? "Cached result" : "Fresh analysis"}</span>
          <span style={reportPill}>Generated from your trade history</span>
        </div>
        <h2 style={heroTitle}>{getHeroTitle(topLeak, data.biggestLeakTitle)}</h2>
        <p style={heroText}>{getHeroSupportText(topLeak)}</p>
        <div style={heroFixLine}>
          <span style={fixLabel}>Suggested fix</span>
          <p style={heroFixText}>{data.biggestLeakFix ?? topLeak?.recommendedFix}</p>
        </div>
        <div style={heroMetaRow}>
          <span style={heroMetaPill}>Generated {formatRelativeGeneratedAt(data.generatedAt)}</span>
          <span style={heroMetaPill}>Data quality: {dataQuality}</span>
          <span style={heroMetaPillStrong}>{sampleTradeCount} trades analyzed</span>
        </div>
      </div>
      <div style={heroStats}>
        <Metric label="Money Lost / Month" value={formatMoney(monthlyImpact)} tone={impactTone} />
        {yearlyImprovement && (
          <Metric label="Potential Yearly Improvement" value={yearlyImprovement} tone={improvementTone} />
        )}
        <ConfidenceMetric score={confidence} />
      </div>
    </section>
  );
}

function RecoveryPlan({ leak }: { leak?: LeakResult }) {
  if (!leak) return null;
  const steps = getRecoveryPlan(leak);
  return (
    <section style={recoverySection}>
      <div style={recoveryHeader}>
        <div style={recoveryIntro}>
          <span style={heroEyebrow}>30-Day Recovery Plan</span>
          <h2 style={recoveryTitle}>Turn this leak into a trading rule.</h2>
          <p style={recoveryText}>Expected improvement target: reduce this leak by 25-50% over the next 30 days.</p>
        </div>
        <div style={recoveryChipRow} aria-label="Recovery plan details">
          <span style={recoveryChip}>30 days</span>
          <span style={recoveryChip}>25-50% target</span>
          <span style={recoveryChip}>Based on biggest leak</span>
        </div>
      </div>
      <div style={recoveryGrid}>
        {steps.map((step) => (
          <div key={step.week} style={recoveryStep}>
            <div style={stepTopline}>
              <span style={stepIcon}>✓</span>
              <span style={stepKicker}>{step.week}</span>
            </div>
            <p style={stepText}>{step.text}</p>
          </div>
        ))}
      </div>
    </section>
  );
}

function LeakCard({ leak, rank, onTradeSelect }: { leak: LeakResult; rank: number; onTradeSelect: (tradeId: number) => void }) {
  const relatedTradeCount = getRelatedTradeCount(leak);
  const ctaText = getLeakCtaText(leak);
  const canOpenRelatedTrade = leak.relatedTradeIds?.length > 0;
  return (
    <article style={card}>
      <div style={cardHeader}>
        <div>
          {rank === 1 && <span style={topRankBadge}>#1 Biggest Opportunity</span>}
          <h2 style={cardTitle}>{leak.title}</h2>
          <p style={summary}>{leak.summary}</p>
        </div>
        <span style={severityBadge(leak.severity)}>{leak.severity}</span>
      </div>
      <div style={metricGrid}>
        <ConfidenceMetric score={leak.confidenceScore} />
        <Metric label="Money Impact" value={`${formatMoney(leak.totalPnl)} total / ${formatMoney(leak.estimatedMonthlyImpact)} monthly`} />
        <Metric label="Trades" value={leak.tradeCount} />
        <Metric label="Win Rate" value={`${Number(leak.winRate ?? 0).toFixed(1)}%`} />
        <Metric label="Avg PnL" value={formatMoney(leak.avgPnl)} />
        {leak.hasValidRMultiple && leak.avgR != null && (
          <Metric label="Avg R" value={formatR(leak.avgR)} />
        )}
      </div>
      <p style={description}>{getCoachExplanation(leak)}</p>
      <div style={fixBox}>
        <span style={fixLabel}>Recommended fix</span>
        <p style={fixText}>{leak.recommendedFix}</p>
      </div>
      {relatedTradeCount > 0 && (
        <button
          type="button"
          onClick={() => canOpenRelatedTrade && onTradeSelect(leak.relatedTradeIds[0])}
          disabled={!canOpenRelatedTrade}
          style={{ ...linkButton, ...(!canOpenRelatedTrade ? disabledLinkButton : {}) }}
        >
          {ctaText} →
        </button>
      )}
    </article>
  );
}

function Metric({ label, value, tone, helper }: { label: string; value: string | number; tone?: CSSProperties; helper?: string }) {
  return (
    <div style={{ ...metric, ...tone }}>
      <span style={metricLabel}>{label}</span>
      <strong style={metricValue}>{value}</strong>
      {helper && <span style={metricHelper}>{helper}</span>}
    </div>
  );
}

function ConfidenceMetric({ score }: { score: number }) {
  const confidence = getConfidenceTier(score);
  const width = Math.max(4, Math.min(100, Number(score ?? 0)));
  return (
    <div style={{ ...metric, ...confidence.style }}>
      <div style={confidenceHeader}>
        <span style={metricLabel}>Confidence</span>
        <strong style={metricValue}>{score}%</strong>
      </div>
      <span style={metricHelper}>{confidence.label}</span>
      <div style={confidenceBarTrack}>
        <span style={{ ...confidenceBarFill, width: `${width}%`, background: confidence.barColor }} />
      </div>
    </div>
  );
}

function formatR(value: number) {
  const rounded = Number(value ?? 0).toFixed(2);
  return `${Number(value) > 0 ? "+" : ""}${rounded}R`;
}

function formatMoney(value: number) {
  const numeric = Number(value ?? 0);
  const absolute = Math.abs(numeric).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  return `${numeric < 0 ? "-$" : "$"}${absolute}`;
}

function formatPotentialImprovement(monthlyImpact: number) {
  const numeric = Number(monthlyImpact ?? 0);
  if (numeric >= 0) return "";
  return `+${formatMoney(Math.abs(numeric) * 12)}/year`;
}

function getConfidenceTier(score: number): { label: string; style: CSSProperties; barColor: string } {
  if (score >= 80) return { label: "Strong signal", style: { borderColor: "rgba(34,197,94,0.36)", background: "rgba(20,83,45,0.26)" }, barColor: "#22c55e" };
  if (score >= 60) return { label: "Good signal", style: { borderColor: "rgba(56,189,248,0.32)", background: "rgba(8,47,73,0.24)" }, barColor: "#38bdf8" };
  if (score >= 40) return { label: "Developing signal", style: { borderColor: "rgba(250,204,21,0.28)", background: "rgba(113,63,18,0.18)" }, barColor: "#facc15" };
  return { label: "Needs more data", style: { borderColor: "rgba(148,163,184,0.18)", background: "rgba(15,23,42,0.52)", color: "#cbd5e1" }, barColor: "#94a3b8" };
}

function getDataQualityLabel(sampleTradeCount: number) {
  if (sampleTradeCount >= 75) return "High";
  if (sampleTradeCount >= 30) return "Medium";
  return "Low";
}

function getRelatedTradeCount(leak: LeakResult) {
  return leak.relatedTradeCount ?? leak.relatedTradeIds?.length ?? leak.tradeCount ?? 0;
}

function getLeakCtaText(leak: LeakResult) {
  const count = getRelatedTradeCount(leak);
  if (count <= 0) return "";
  const tradeWord = count === 1 ? "trade" : "trades";
  switch (leak.leakType) {
    case "LONG_SHORT_BIAS":
      if (/short/i.test(leak.title)) return `Review ${count} short ${tradeWord}`;
      if (/long/i.test(leak.title)) return `Review ${count} long ${tradeWord}`;
      return `Review ${count} directional ${tradeWord}`;
    case "DAY_OF_WEEK": {
      const day = leak.title.match(/\b(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)\b/i)?.[0];
      return `Review ${count} ${day ?? "day-of-week"} ${tradeWord}`;
    }
    case "OVERTRADING":
      return `Review ${count} high-frequency ${tradeWord}`;
    case "SESSION_TIME":
      return `Review ${count} session ${tradeWord}`;
    case "SETUP":
      return `Review ${count} setup ${tradeWord}`;
    case "CONFIDENCE":
      return `Review ${count} confidence-tagged ${tradeWord}`;
    case "EMOTION":
      return `Review ${count} emotion-tagged ${tradeWord}`;
    case "LARGE_LOSS":
      return `Review ${count} large-loss ${tradeWord}`;
    default:
      return `Review ${count} related ${tradeWord}`;
  }
}

function getHeroSupportText(leak?: LeakResult) {
  if (!leak) return "TradePilot found the highest-impact pattern in your recent trading sample.";
  const direction = getWeakDirection(leak);
  if (leak.leakType === "LONG_SHORT_BIAS" && direction) {
    return `Your ${direction.toLowerCase()} trades have produced the largest avoidable drawdown in this sample. Reduce ${direction.toLowerCase()}-side size until your edge improves.`;
  }
  if (leak.leakType === "DAY_OF_WEEK") {
    const day = getDayFromTitle(leak.title);
    return `${day ? `${day} has` : "One trading day has"} been your weakest day in this sample. Treat it as a reduced-risk day until setup quality improves.`;
  }
  if (leak.leakType === "OVERTRADING") return "Your highest trade-count days are producing weaker outcomes. Set the trade cap before the session starts and stop when it is hit.";
  if (leak.leakType === "LARGE_LOSS") return "Your largest losses are doing the most damage. Tighten the maximum-loss rule before adding more size.";
  if (leak.leakType === "SESSION_TIME") return "One session is carrying most of the avoidable drawdown. Trade that window with tighter confirmation until results stabilize.";
  if (leak.leakType === "SETUP") return "One setup is costing more than it is paying. Pause or reduce that setup until the stats improve.";
  if (leak.leakType === "CONFIDENCE") return "One confidence bucket is underperforming. Use it as a warning label before increasing risk.";
  if (leak.leakType === "EMOTION") return "One emotional state is attached to weaker trades. Treat that tag as a signal to slow down.";
  return "TradePilot found the highest-impact pattern in your recent trading sample.";
}

function getHeroTitle(leak?: LeakResult, fallbackTitle?: string | null) {
  const title = fallbackTitle ?? leak?.title ?? "your top trading leak";
  if (!leak) return `Your biggest leak is ${title}.`;
  if (leak.leakType === "LONG_SHORT_BIAS") {
    if (/short/i.test(title)) return "You are losing the most money when trading SHORT.";
    if (/long/i.test(title)) return "You are losing the most money when trading LONG.";
  }
  if (leak.leakType === "DAY_OF_WEEK") {
    const day = title.match(/\b(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)\b/i)?.[0];
    return day ? `${day}s are currently your weakest trading day.` : "One trading day is costing you more than the others.";
  }
  if (leak.leakType === "OVERTRADING") return "Your high trade-count days are costing you money.";
  if (leak.leakType === "LARGE_LOSS") return "Your largest losses are doing the most damage.";
  if (leak.leakType === "SESSION_TIME") return "One trading session is dragging down your results.";
  if (leak.leakType === "SETUP") return "One setup is costing you more than it is paying.";
  if (leak.leakType === "CONFIDENCE") return "One confidence bucket is underperforming.";
  if (leak.leakType === "EMOTION") return "One emotional state is costing you money.";
  return `Your biggest leak is ${title}.`;
}

function getCoachExplanation(leak: LeakResult) {
  const count = getRelatedTradeCount(leak) || leak.tradeCount;
  const winRate = `${Number(leak.winRate ?? 0).toFixed(1)}%`;
  const total = formatMoney(leak.totalPnl);
  const direction = getWeakDirection(leak);
  if (leak.leakType === "LONG_SHORT_BIAS" && direction) {
    return `Your ${direction.toLowerCase()} trades have a ${winRate} win rate across ${count} trades, costing ${total}. Until that thesis improves, reduce size or require stronger confirmation.`;
  }
  if (leak.leakType === "DAY_OF_WEEK") {
    const day = getDayFromTitle(leak.title) ?? "This day";
    return `${day} has been your weakest day in this sample, costing ${total}. Treat it as a reduced-risk day until your setup quality improves.`;
  }
  if (leak.leakType === "OVERTRADING") {
    return `Your highest trade-count days are producing weaker outcomes across ${count} trades. Set a hard trade cap before the session starts.`;
  }
  if (leak.leakType === "LARGE_LOSS") {
    return `Large losses are responsible for ${total} in damage across ${count} trades. Enforce the max-loss rule before looking for the next entry.`;
  }
  if (leak.leakType === "SESSION_TIME") {
    return `This session has produced ${total} across ${count} trades. Require cleaner confirmation in this window or reduce risk until the edge returns.`;
  }
  if (leak.leakType === "SETUP") {
    return `This setup has produced ${total} across ${count} trades. Pause it, lower size, or rewrite the entry criteria before taking more signals.`;
  }
  if (leak.leakType === "CONFIDENCE") {
    return `This confidence bucket is underperforming across ${count} trades. Use it as a risk filter before increasing size.`;
  }
  if (leak.leakType === "EMOTION") {
    return `Trades tagged with this emotional state have produced weaker outcomes. Slow the process down when this tag appears.`;
  }
  return leak.description;
}

function getRecoveryPlan(leak: LeakResult) {
  const direction = getWeakDirection(leak);
  if (leak.leakType === "LONG_SHORT_BIAS" && direction) {
    return [
      { week: "Week 1", text: `Reduce size on ${direction.toLowerCase()} trades and only take the cleanest examples.` },
      { week: "Week 2", text: "Require one extra confirmation before entering the weak direction." },
      { week: "Week 3", text: "Review the related trades and write one rule for when this setup is allowed." },
      { week: "Week 4", text: "Re-run Leak Detector and compare the drawdown against this report." },
    ];
  }
  if (leak.leakType === "DAY_OF_WEEK") {
    const day = getDayFromTitle(leak.title) ?? "the weak day";
    return [
      { week: "Week 1", text: `Cut risk on ${day} and only take A+ setups.` },
      { week: "Week 2", text: "Require one extra confirmation before entering on that day." },
      { week: "Week 3", text: "Review the related trades and write one rule for that session." },
      { week: "Week 4", text: "Re-run Leak Detector and compare the day-of-week impact." },
    ];
  }
  if (leak.leakType === "OVERTRADING") {
    return [
      { week: "Week 1", text: "Set a hard daily trade cap before the opening bell." },
      { week: "Week 2", text: "Require one extra confirmation after the second trade of the day." },
      { week: "Week 3", text: "Review the high-frequency days and define your stop-trading trigger." },
      { week: "Week 4", text: "Re-run Leak Detector and compare high-trade-count days." },
    ];
  }
  if (leak.leakType === "LARGE_LOSS") {
    return [
      { week: "Week 1", text: "Enforce a maximum loss per trade before entering." },
      { week: "Week 2", text: "Require one extra confirmation before adding risk." },
      { week: "Week 3", text: "Review the large-loss trades and write the exit rule you skipped." },
      { week: "Week 4", text: "Re-run Leak Detector and compare large-loss frequency." },
    ];
  }
  return [
    { week: "Week 1", text: "Reduce risk on the weakest pattern." },
    { week: "Week 2", text: "Require one extra confirmation before entering." },
    { week: "Week 3", text: "Review related trades and write one rule." },
    { week: "Week 4", text: "Re-run Leak Detector and compare improvement." },
  ];
}

function getWeakDirection(leak: LeakResult) {
  if (/short/i.test(leak.title)) return "SHORT";
  if (/long/i.test(leak.title)) return "LONG";
  return "";
}

function getDayFromTitle(title: string) {
  return title.match(/\b(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)\b/i)?.[0];
}

function formatRelativeGeneratedAt(value?: string) {
  if (!value) return "just now";
  const generated = new Date(value);
  if (Number.isNaN(generated.getTime())) return "just now";
  const diffMs = Date.now() - generated.getTime();
  const diffMinutes = Math.floor(diffMs / 60000);
  if (diffMinutes < 2) return "just now";
  if (diffMinutes < 60) return `${diffMinutes} minutes ago`;
  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) return `${diffHours} ${diffHours === 1 ? "hour" : "hours"} ago`;
  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 7) return `${diffDays} ${diffDays === 1 ? "day" : "days"} ago`;
  return generated.toLocaleDateString(undefined, { month: "short", day: "numeric" });
}

function errorMessage(error: any) {
  const status = error?.response?.status;
  if (status === 403) return "Upgrade to Pro to access AI Leak Detector.";
  if (status === 429) return error?.response?.data?.message ?? "You have used all AI credits for this month.";
  return "Failed to load AI Leak Detector.";
}

function severityBadge(severity: LeakSeverity): CSSProperties {
  const colors: Record<LeakSeverity, CSSProperties> = {
    LOW: { color: "#bae6fd", borderColor: "rgba(56,189,248,0.24)", background: "rgba(8,47,73,0.22)" },
    MEDIUM: { color: "#fde68a", borderColor: "rgba(250,204,21,0.3)", background: "rgba(113,63,18,0.24)" },
    HIGH: { color: "#fed7aa", borderColor: "rgba(251,146,60,0.36)", background: "rgba(124,45,18,0.3)" },
    CRITICAL: { color: "#fecaca", borderColor: "rgba(248,113,113,0.42)", background: "rgba(127,29,29,0.34)" },
  };
  return { ...badge, ...colors[severity] };
}

const shell: CSSProperties = { display: "grid", gap: 18 };
const toolbar: CSSProperties = { display: "flex", flexWrap: "wrap", alignItems: "end", gap: 14, padding: 18, borderRadius: 8, border: "1px solid rgba(148,163,184,0.16)", background: "rgba(15,23,42,0.72)" };
const controlGroup: CSSProperties = { display: "grid", gap: 8, minWidth: 240 };
const label: CSSProperties = { color: "#94a3b8", fontSize: 12, fontWeight: 900, textTransform: "uppercase", letterSpacing: "0.06em" };
const select: CSSProperties = { padding: "12px 13px", borderRadius: 8, border: "1px solid rgba(148,163,184,0.28)", background: "#020617", color: "#f8fafc", fontWeight: 800 };
const creditBadge: CSSProperties = { padding: "12px 14px", borderRadius: 8, border: "1px solid rgba(52,211,153,0.24)", color: "#86efac", background: "rgba(20,83,45,0.2)", fontWeight: 900 };
const notice: CSSProperties = { padding: 14, borderRadius: 8, border: "1px solid rgba(56,189,248,0.22)", color: "#bae6fd", background: "rgba(8,47,73,0.28)" };
const errorBox: CSSProperties = { padding: 14, borderRadius: 8, border: "1px solid rgba(248,113,113,0.28)", color: "#fecaca", background: "rgba(127,29,29,0.24)" };
const emptyState: CSSProperties = { padding: 34, borderRadius: 8, border: "1px solid rgba(148,163,184,0.16)", background: "rgba(15,23,42,0.72)", textAlign: "center" };
const emptyTitle: CSSProperties = { margin: 0, color: "#f8fafc", fontSize: 24 };
const muted: CSSProperties = { color: "#94a3b8", lineHeight: 1.6 };
const heroCard: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 280px), 1fr))", gap: 20, alignItems: "center", padding: 24, borderRadius: 8, border: "1px solid rgba(52,211,153,0.24)", background: "radial-gradient(circle at top left, rgba(52,211,153,0.22), transparent 32%), linear-gradient(135deg, rgba(20,83,45,0.34), rgba(15,23,42,0.94) 46%, rgba(2,6,23,0.9))", boxShadow: "0 24px 70px rgba(0,0,0,0.28)" };
const heroTopline: CSSProperties = { display: "flex", flexWrap: "wrap", alignItems: "center", gap: 8 };
const heroEyebrow: CSSProperties = { color: "#86efac", fontSize: 12, fontWeight: 950, textTransform: "uppercase", letterSpacing: "0.08em" };
const reportPill: CSSProperties = { display: "inline-flex", alignItems: "center", minHeight: 24, padding: "4px 8px", borderRadius: 999, border: "1px solid rgba(148,163,184,0.14)", color: "#94a3b8", background: "rgba(2,6,23,0.28)", fontSize: 11, fontWeight: 850 };
const heroTitle: CSSProperties = { margin: "12px 0 10px", color: "#f8fafc", fontSize: 30, lineHeight: 1.12 };
const heroText: CSSProperties = { margin: 0, color: "#dbeafe", lineHeight: 1.65, maxWidth: 760 };
const heroFixLine: CSSProperties = { marginTop: 16, padding: 14, borderRadius: 8, border: "1px solid rgba(52,211,153,0.18)", background: "rgba(2,6,23,0.24)" };
const heroFixText: CSSProperties = { margin: "6px 0 0", color: "#dcfce7", lineHeight: 1.5 };
const heroMetaRow: CSSProperties = { display: "flex", flexWrap: "wrap", gap: 8, marginTop: 14 };
const heroMetaPill: CSSProperties = { display: "inline-flex", alignItems: "center", minHeight: 28, padding: "5px 9px", borderRadius: 999, border: "1px solid rgba(148,163,184,0.16)", color: "#cbd5e1", background: "rgba(2,6,23,0.34)", fontSize: 12, fontWeight: 850 };
const heroMetaPillStrong: CSSProperties = { ...heroMetaPill, borderColor: "rgba(52,211,153,0.24)", color: "#bbf7d0", background: "rgba(20,83,45,0.22)" };
const heroStats: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(120px, 1fr))", gap: 10 };
const impactTone: CSSProperties = { borderColor: "rgba(248,113,113,0.3)", background: "rgba(127,29,29,0.24)" };
const improvementTone: CSSProperties = { borderColor: "rgba(52,211,153,0.32)", background: "rgba(20,83,45,0.24)" };
const recoverySection: CSSProperties = { display: "grid", gap: 16, padding: 20, borderRadius: 8, border: "1px solid rgba(56,189,248,0.16)", background: "linear-gradient(135deg, rgba(8,47,73,0.28), rgba(15,23,42,0.78))" };
const recoveryHeader: CSSProperties = { display: "grid", gap: 12 };
const recoveryIntro: CSSProperties = { display: "grid", alignContent: "start", gap: 8 };
const recoveryTitle: CSSProperties = { margin: 0, color: "#f8fafc", fontSize: 22, lineHeight: 1.2 };
const recoveryText: CSSProperties = { margin: 0, color: "#94a3b8", lineHeight: 1.55 };
const recoveryChipRow: CSSProperties = { display: "flex", flexWrap: "wrap", gap: 8 };
const recoveryChip: CSSProperties = { display: "inline-flex", alignItems: "center", minHeight: 26, padding: "5px 9px", borderRadius: 999, border: "1px solid rgba(56,189,248,0.16)", color: "#bae6fd", background: "rgba(8,47,73,0.22)", fontSize: 12, fontWeight: 850 };
const recoveryGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 190px), 1fr))", gap: 12 };
const recoveryStep: CSSProperties = { padding: 14, borderRadius: 8, border: "1px solid rgba(148,163,184,0.14)", background: "rgba(2,6,23,0.34)" };
const stepTopline: CSSProperties = { display: "flex", alignItems: "center", gap: 8 };
const stepIcon: CSSProperties = { display: "inline-grid", placeItems: "center", width: 20, height: 20, borderRadius: 999, color: "#022c22", background: "linear-gradient(135deg, #34d399, #22d3ee)", fontSize: 12, fontWeight: 950 };
const stepKicker: CSSProperties = { color: "#7dd3fc", fontSize: 11, fontWeight: 950, textTransform: "uppercase", letterSpacing: "0.06em" };
const stepText: CSSProperties = { margin: "8px 0 0", color: "#dbeafe", lineHeight: 1.45, fontWeight: 720 };
const grid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 320px), 1fr))", gap: 16 };
const card: CSSProperties = { padding: 18, borderRadius: 8, border: "1px solid rgba(148,163,184,0.16)", background: "linear-gradient(180deg, rgba(15,23,42,0.94), rgba(2,6,23,0.72))", boxShadow: "0 20px 60px rgba(0,0,0,0.24)" };
const cardHeader: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 12, alignItems: "start" };
const cardTitle: CSSProperties = { margin: 0, color: "#f8fafc", fontSize: 20 };
const topRankBadge: CSSProperties = { display: "inline-flex", marginBottom: 8, padding: "5px 8px", borderRadius: 6, border: "1px solid rgba(52,211,153,0.26)", color: "#86efac", background: "rgba(20,83,45,0.22)", fontSize: 11, fontWeight: 950 };
const summary: CSSProperties = { margin: "8px 0 0", color: "#94a3b8", lineHeight: 1.5 };
const badge: CSSProperties = { padding: "5px 8px", borderRadius: 6, border: "1px solid", fontSize: 11, fontWeight: 950 };
const metricGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(2, minmax(0, 1fr))", gap: 10, marginTop: 16 };
const metric: CSSProperties = { padding: 12, borderRadius: 8, border: "1px solid rgba(148,163,184,0.14)", background: "rgba(2,6,23,0.42)" };
const metricLabel: CSSProperties = { display: "block", color: "#64748b", fontSize: 11, fontWeight: 900, textTransform: "uppercase" };
const metricValue: CSSProperties = { display: "block", marginTop: 5, color: "#f8fafc", fontSize: 15 };
const metricHelper: CSSProperties = { display: "block", marginTop: 4, color: "#94a3b8", fontSize: 12, fontWeight: 750 };
const confidenceHeader: CSSProperties = { display: "flex", alignItems: "baseline", justifyContent: "space-between", gap: 8 };
const confidenceBarTrack: CSSProperties = { marginTop: 9, height: 6, borderRadius: 999, overflow: "hidden", background: "rgba(15,23,42,0.8)" };
const confidenceBarFill: CSSProperties = { display: "block", height: "100%", borderRadius: 999 };
const description: CSSProperties = { marginTop: 16, color: "#cbd5e1", lineHeight: 1.55 };
const fixBox: CSSProperties = { marginTop: 16, padding: 14, borderRadius: 8, border: "1px solid rgba(52,211,153,0.2)", background: "rgba(20,83,45,0.18)" };
const fixLabel: CSSProperties = { color: "#86efac", fontSize: 12, fontWeight: 900, textTransform: "uppercase" };
const fixText: CSSProperties = { margin: "6px 0 0", color: "#dcfce7", lineHeight: 1.5 };
const linkButton: CSSProperties = { marginTop: 14, border: "1px solid rgba(56,189,248,0.26)", borderRadius: 8, padding: "10px 12px", background: "rgba(8,47,73,0.24)", color: "#bae6fd", fontWeight: 850, cursor: "pointer" };
const disabledLinkButton: CSSProperties = { opacity: 0.62, cursor: "not-allowed" };
