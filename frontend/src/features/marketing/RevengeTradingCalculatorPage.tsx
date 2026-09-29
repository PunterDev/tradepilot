import { useEffect, useMemo, useRef, useState, type RefObject } from "react";
import { Link, useSearchParams } from "react-router-dom";
import Seo, { SITE_URL } from "../../components/Seo";
import { activationAwareCtaDestination } from "../../utils/activationRedirect";
import { trackSeoToolCtaClick, trackSeoToolEvent } from "../../utils/seoAttribution";
import "./HomePage.css";
import { MarketingShell } from "./MarketingLayout";

const PAGE_PATH = "/tools/revenge-trading-calculator";
const TOOL_ID = "revenge_trading_calculator";
const CTA_LABEL = "Find My Trading Leaks";
const CTA_SOURCE = "revenge-trading-calculator";
const TITLE = "Revenge Trading Calculator: Analyze Performance After Losses | TradePilot Coach";
const DESCRIPTION =
  "Compare your normal trading performance with trades taken after losses and consecutive losses to see whether your results deteriorate after losing trades.";
const MAX_VALUE = 1_000_000_000;
const SIMILAR_ABSOLUTE_THRESHOLD = 5;
const SIMILAR_RATIO = 0.1;
const STRONG_RATIO = 0.25;

type GroupKey = "baseline" | "after1" | "after2";
type FieldKey =
  | "baselineTrades"
  | "baselineWins"
  | "baselineWinPnl"
  | "baselineLossPnl"
  | "after1Trades"
  | "after1Wins"
  | "after1WinPnl"
  | "after1LossPnl"
  | "after2Trades"
  | "after2Wins"
  | "after2WinPnl"
  | "after2LossPnl"
  | "days";
type ResultState = "no_clear_deterioration" | "mild_deterioration" | "strong_post_loss_deterioration";

type ParsedField = {
  value: number | null;
  error?: string;
};

type TradeGroupInput = {
  trades: number;
  wins: number;
  winningPnl: number;
  losingPnl: number;
};

type TradeGroupMetrics = {
  trades: number;
  wins: number;
  losses: number;
  winRate: number;
  lossRate: number;
  averageWin: number | null;
  averageLoss: number | null;
  netPnl: number;
  expectancy: number;
  rewardToRisk: number | null;
};

type PostLossComparison = {
  winRateChange: number;
  expectancyChange: number;
  performanceGap: number;
};

type RevengeResult =
  | {
      ready: false;
      errors: Partial<Record<FieldKey, string>>;
    }
  | {
      ready: true;
      errors: Partial<Record<FieldKey, string>>;
      baseline: TradeGroupMetrics;
      after1: TradeGroupMetrics;
      after2: TradeGroupMetrics;
      after1Comparison: PostLossComparison;
      after2Comparison: PostLossComparison;
      days: number | null;
      combinedPerformanceGap: number;
      gapPerDay: number | null;
      state: ResultState;
    };

type SubmittedRevengeResult = Extract<RevengeResult, { ready: true }>;

function parseDecimal(raw: string, label: string, required: boolean): ParsedField {
  const value = raw.trim();
  if (!value) {
    return required ? { value: null, error: `${label} is required.` } : { value: null };
  }
  if (!/^-?\d*(?:\.\d*)?$/.test(value) || value === "." || value === "-.") {
    return { value: null, error: `${label} must be a valid number.` };
  }
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    return { value: null, error: `${label} must be a valid number.` };
  }
  if (parsed < 0) {
    return { value: null, error: `${label} cannot be negative.` };
  }
  if (parsed > MAX_VALUE) {
    return { value: null, error: `${label} is too large for this calculator.` };
  }
  return { value: parsed };
}

function parseWholeNumber(raw: string, label: string, required = true): ParsedField {
  const parsed = parseDecimal(raw, label, required);
  if (parsed.error || parsed.value === null) return parsed;
  if (!Number.isInteger(parsed.value)) {
    return { value: null, error: `${label} must be a whole number.` };
  }
  return parsed;
}

function calculateTradeGroupMetrics(input: TradeGroupInput): TradeGroupMetrics {
  const losses = input.trades - input.wins;
  const winRate = input.wins / input.trades;
  const lossRate = losses / input.trades;
  const averageWin = input.wins > 0 ? input.winningPnl / input.wins : null;
  const averageLoss = losses > 0 ? input.losingPnl / losses : null;
  const netPnl = input.winningPnl - input.losingPnl;
  const expectancy = netPnl / input.trades;
  const rewardToRisk = averageWin !== null && averageLoss !== null && averageLoss > 0 ? averageWin / averageLoss : null;

  return {
    trades: input.trades,
    wins: input.wins,
    losses,
    winRate,
    lossRate,
    averageWin,
    averageLoss,
    netPnl,
    expectancy,
    rewardToRisk,
  };
}

function calculatePostLossComparison(baseline: TradeGroupMetrics, postLoss: TradeGroupMetrics): PostLossComparison {
  return {
    winRateChange: postLoss.winRate - baseline.winRate,
    expectancyChange: postLoss.expectancy - baseline.expectancy,
    performanceGap: (baseline.expectancy - postLoss.expectancy) * postLoss.trades,
  };
}

function classifyPostLossPattern(baselineExpectancy: number, after1Change: number, after2Change: number): ResultState {
  const similarThreshold = Math.max(SIMILAR_ABSOLUTE_THRESHOLD, Math.abs(baselineExpectancy) * SIMILAR_RATIO);
  const strongestDeterioration = Math.min(after1Change, after2Change);
  if (strongestDeterioration >= -similarThreshold) return "no_clear_deterioration";

  // Strong language is reserved for consecutive-loss degradation beyond 25% of baseline expectancy.
  const strongThreshold = Math.max(similarThreshold, Math.abs(baselineExpectancy) * STRONG_RATIO);
  if (after2Change <= -strongThreshold) return "strong_post_loss_deterioration";
  return "mild_deterioration";
}

function formatMoney(value: number, maximumFractionDigits = 2) {
  const sign = value > 0 ? "+" : value < 0 ? "-" : "";
  return `${sign}${new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: Math.abs(value) % 1 === 0 ? 0 : 2,
    maximumFractionDigits,
  }).format(Math.abs(value))}`;
}

function formatMoneyWithCents(value: number) {
  const sign = value > 0 ? "+" : value < 0 ? "-" : "";
  return `${sign}${new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Math.abs(value))}`;
}

function formatUnsignedMoney(value: number, maximumFractionDigits = 0) {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: Math.abs(value) % 1 === 0 ? 0 : 2,
    maximumFractionDigits,
  }).format(Math.abs(value));
}

function formatPct(value: number, maximumFractionDigits = 2) {
  return `${new Intl.NumberFormat("en-US", { maximumFractionDigits }).format(value * 100)}%`;
}

function formatPercentagePoint(value: number) {
  const sign = value > 0 ? "+" : value < 0 ? "-" : "";
  return `${sign}${new Intl.NumberFormat("en-US", { maximumFractionDigits: 2 }).format(Math.abs(value * 100))} percentage points`;
}

function cleanQueryValue(value: string | null, parser: (raw: string) => ParsedField) {
  if (!value) return "";
  const parsed = parser(value);
  return parsed.error || parsed.value === null ? "" : value;
}

export default function RevengeTradingCalculatorPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [inputs, setInputs] = useState<Record<FieldKey, string>>(() => ({
    baselineTrades: cleanQueryValue(searchParams.get("baselineTrades"), (value) => parseWholeNumber(value, "Baseline trades")),
    baselineWins: cleanQueryValue(searchParams.get("baselineWins"), (value) => parseWholeNumber(value, "Baseline winning trades")),
    baselineWinPnl: cleanQueryValue(searchParams.get("baselineWinPnl"), (value) => parseDecimal(value, "Baseline total winning P&L", true)),
    baselineLossPnl: cleanQueryValue(searchParams.get("baselineLossPnl"), (value) => parseDecimal(value, "Baseline total losing P&L", true)),
    after1Trades: cleanQueryValue(searchParams.get("after1Trades"), (value) => parseWholeNumber(value, "After 1 loss trades")),
    after1Wins: cleanQueryValue(searchParams.get("after1Wins"), (value) => parseWholeNumber(value, "After 1 loss winning trades")),
    after1WinPnl: cleanQueryValue(searchParams.get("after1WinPnl"), (value) => parseDecimal(value, "After 1 loss total winning P&L", true)),
    after1LossPnl: cleanQueryValue(searchParams.get("after1LossPnl"), (value) => parseDecimal(value, "After 1 loss total losing P&L", true)),
    after2Trades: cleanQueryValue(searchParams.get("after2Trades"), (value) => parseWholeNumber(value, "After 2 losses trades")),
    after2Wins: cleanQueryValue(searchParams.get("after2Wins"), (value) => parseWholeNumber(value, "After 2 losses winning trades")),
    after2WinPnl: cleanQueryValue(searchParams.get("after2WinPnl"), (value) => parseDecimal(value, "After 2 losses total winning P&L", true)),
    after2LossPnl: cleanQueryValue(searchParams.get("after2LossPnl"), (value) => parseDecimal(value, "After 2 losses total losing P&L", true)),
    days: cleanQueryValue(searchParams.get("days"), (value) => parseWholeNumber(value, "Trading days represented", false)),
  }));
  const [touchedFields, setTouchedFields] = useState<Partial<Record<FieldKey, boolean>>>({});
  const [analysisAttempted, setAnalysisAttempted] = useState(false);
  const [submittedResult, setSubmittedResult] = useState<SubmittedRevengeResult | null>(null);
  const [isStale, setIsStale] = useState(false);
  const hasTrackedStart = useRef(false);
  const resultRegionRef = useRef<HTMLElement>(null);
  const ctaDestination = activationAwareCtaDestination(CTA_SOURCE);

  const draftResult = useMemo<RevengeResult>(() => {
    const parsed = {
      baselineTrades: parseWholeNumber(inputs.baselineTrades, "Baseline trades"),
      baselineWins: parseWholeNumber(inputs.baselineWins, "Baseline winning trades"),
      baselineWinPnl: parseDecimal(inputs.baselineWinPnl, "Baseline total winning P&L", false),
      baselineLossPnl: parseDecimal(inputs.baselineLossPnl, "Baseline total losing P&L", false),
      after1Trades: parseWholeNumber(inputs.after1Trades, "After 1 loss trades"),
      after1Wins: parseWholeNumber(inputs.after1Wins, "After 1 loss winning trades"),
      after1WinPnl: parseDecimal(inputs.after1WinPnl, "After 1 loss total winning P&L", false),
      after1LossPnl: parseDecimal(inputs.after1LossPnl, "After 1 loss total losing P&L", false),
      after2Trades: parseWholeNumber(inputs.after2Trades, "After 2 losses trades"),
      after2Wins: parseWholeNumber(inputs.after2Wins, "After 2 losses winning trades"),
      after2WinPnl: parseDecimal(inputs.after2WinPnl, "After 2 losses total winning P&L", false),
      after2LossPnl: parseDecimal(inputs.after2LossPnl, "After 2 losses total losing P&L", false),
      days: parseWholeNumber(inputs.days, "Trading days represented", false),
    };
    const errors: Partial<Record<FieldKey, string>> = Object.fromEntries(
      Object.entries(parsed).map(([key, field]) => [key, field.error])
    );

    for (const key of ["baselineTrades", "after1Trades", "after2Trades"] as const) {
      if (parsed[key].value === 0 && inputs[key].trim()) {
        const label = key === "baselineTrades" ? "Baseline trades" : key === "after1Trades" ? "After 1 loss trades" : "After 2 losses trades";
        errors[key] = `${label} must be at least 1.`;
      }
    }
    if (parsed.days.value === 0 && inputs.days.trim()) {
      errors.days = "Trading days represented must be at least 1.";
    }
    for (const [winsKey, tradesKey, label] of [
      ["baselineWins", "baselineTrades", "Baseline winning trades"],
      ["after1Wins", "after1Trades", "After 1 loss winning trades"],
      ["after2Wins", "after2Trades", "After 2 losses winning trades"],
    ] as const) {
      if (parsed[winsKey].value !== null && parsed[tradesKey].value !== null && parsed[winsKey].value > parsed[tradesKey].value) {
        errors[winsKey] = `${label} cannot exceed total trades.`;
      }
    }

    for (const [tradesKey, winsKey, winPnlKey, lossPnlKey, winLabel, lossLabel] of [
      ["baselineTrades", "baselineWins", "baselineWinPnl", "baselineLossPnl", "Baseline total winning P&L", "Baseline total losing P&L"],
      ["after1Trades", "after1Wins", "after1WinPnl", "after1LossPnl", "After 1 loss total winning P&L", "After 1 loss total losing P&L"],
      ["after2Trades", "after2Wins", "after2WinPnl", "after2LossPnl", "After 2 losses total winning P&L", "After 2 losses total losing P&L"],
    ] as const) {
      const trades = parsed[tradesKey].value;
      const wins = parsed[winsKey].value;
      const winningPnl = parsed[winPnlKey];
      const losingPnl = parsed[lossPnlKey];
      if (trades === null || wins === null || wins > trades) continue;

      const losses = trades - wins;
      if (wins === 0 && winningPnl.value === null && !winningPnl.error) {
        winningPnl.value = 0;
      } else if (wins > 0 && winningPnl.value === null && !winningPnl.error) {
        errors[winPnlKey] = `${winLabel} is required when there are winning trades.`;
      }

      if (losses === 0 && losingPnl.value === null && !losingPnl.error) {
        losingPnl.value = 0;
      } else if (losses > 0 && losingPnl.value === null && !losingPnl.error) {
        errors[lossPnlKey] = `${lossLabel} is required when there are losing trades.`;
      }
    }

    const ready =
      parsed.baselineTrades.value !== null &&
      parsed.baselineTrades.value > 0 &&
      parsed.baselineWins.value !== null &&
      parsed.baselineWinPnl.value !== null &&
      parsed.baselineLossPnl.value !== null &&
      parsed.after1Trades.value !== null &&
      parsed.after1Trades.value > 0 &&
      parsed.after1Wins.value !== null &&
      parsed.after1WinPnl.value !== null &&
      parsed.after1LossPnl.value !== null &&
      parsed.after2Trades.value !== null &&
      parsed.after2Trades.value > 0 &&
      parsed.after2Wins.value !== null &&
      parsed.after2WinPnl.value !== null &&
      parsed.after2LossPnl.value !== null &&
      !Object.values(errors).some(Boolean);

    if (!ready) {
      return { ready: false, errors };
    }

    const baseline = calculateTradeGroupMetrics({
      trades: parsed.baselineTrades.value as number,
      wins: parsed.baselineWins.value as number,
      winningPnl: parsed.baselineWinPnl.value as number,
      losingPnl: parsed.baselineLossPnl.value as number,
    });
    const after1 = calculateTradeGroupMetrics({
      trades: parsed.after1Trades.value as number,
      wins: parsed.after1Wins.value as number,
      winningPnl: parsed.after1WinPnl.value as number,
      losingPnl: parsed.after1LossPnl.value as number,
    });
    const after2 = calculateTradeGroupMetrics({
      trades: parsed.after2Trades.value as number,
      wins: parsed.after2Wins.value as number,
      winningPnl: parsed.after2WinPnl.value as number,
      losingPnl: parsed.after2LossPnl.value as number,
    });
    const after1Comparison = calculatePostLossComparison(baseline, after1);
    const after2Comparison = calculatePostLossComparison(baseline, after2);
    const combinedPerformanceGap = after1Comparison.performanceGap + after2Comparison.performanceGap;
    const days = parsed.days.value && parsed.days.value > 0 ? parsed.days.value : null;

    return {
      ready: true,
      errors,
      baseline,
      after1,
      after2,
      after1Comparison,
      after2Comparison,
      days,
      combinedPerformanceGap,
      gapPerDay: days ? combinedPerformanceGap / days : null,
      state: classifyPostLossPattern(baseline.expectancy, after1Comparison.expectancyChange, after2Comparison.expectancyChange),
    };
  }, [inputs]);

  const visibleErrors = useMemo<Partial<Record<FieldKey, string>>>(() => {
    const next: Partial<Record<FieldKey, string>> = {};
    (Object.entries(draftResult.errors) as Array<[FieldKey, string | undefined]>).forEach(([key, error]) => {
      if (error && (analysisAttempted || touchedFields[key] || inputs[key].trim())) {
        next[key] = error;
      }
    });
    return next;
  }, [analysisAttempted, draftResult.errors, inputs, touchedFields]);

  useEffect(() => {
    trackSeoToolEvent("tool_page_view", PAGE_PATH, TOOL_ID);
  }, []);

  useEffect(() => {
    if (!hasTrackedStart.current && Object.values(inputs).some((value) => value.trim())) {
      hasTrackedStart.current = true;
      trackSeoToolEvent("revenge_calculator_started", PAGE_PATH, TOOL_ID);
    }
  }, [inputs]);

  useEffect(() => {
    const next = new URLSearchParams(window.location.search);
    const queryKeys: FieldKey[] = [
      "baselineTrades",
      "baselineWins",
      "baselineWinPnl",
      "baselineLossPnl",
      "after1Trades",
      "after1Wins",
      "after1WinPnl",
      "after1LossPnl",
      "after2Trades",
      "after2Wins",
      "after2WinPnl",
      "after2LossPnl",
      "days",
    ];
    queryKeys.forEach((key) => next.delete(key));
    queryKeys.forEach((key) => {
      const value = inputs[key].trim();
      if (value && !draftResult.errors[key]) next.set(key, value);
    });
    const current = new URLSearchParams(window.location.search);
    if (next.toString() !== current.toString()) {
      setSearchParams(next, { replace: true });
    }
  }, [draftResult.errors, inputs, setSearchParams]);

  useEffect(() => {
    if (!submittedResult) return;
    const frame = window.requestAnimationFrame(() => {
      resultRegionRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
      resultRegionRef.current?.focus({ preventScroll: true });
    });
    return () => window.cancelAnimationFrame(frame);
  }, [submittedResult]);

  const schema = {
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "WebApplication",
        name: "Revenge Trading Calculator",
        applicationCategory: "FinanceApplication",
        operatingSystem: "Web",
        url: `${SITE_URL}${PAGE_PATH}`,
        description: DESCRIPTION,
        offers: {
          "@type": "Offer",
          price: "0",
          priceCurrency: "USD",
        },
      },
      {
        "@type": "BreadcrumbList",
        itemListElement: [
          { "@type": "ListItem", position: 1, name: "Home", item: SITE_URL },
          { "@type": "ListItem", position: 2, name: "Tools", item: `${SITE_URL}/tools` },
          { "@type": "ListItem", position: 3, name: "Revenge Trading Calculator", item: `${SITE_URL}${PAGE_PATH}` },
        ],
      },
    ],
  };

  const setField = (key: FieldKey, value: string) => {
    setInputs((current) => ({ ...current, [key]: value }));
    if (submittedResult) {
      setSubmittedResult(null);
      setIsStale(true);
    }
  };

  const markTouched = (key: FieldKey) => {
    setTouchedFields((current) => ({ ...current, [key]: true }));
  };

  const handleAnalyze = () => {
    setAnalysisAttempted(true);
    if (!draftResult.ready) {
      setSubmittedResult(null);
      setIsStale(Boolean(submittedResult));
      const firstInvalidField = ([
        "baselineTrades", "baselineWins", "baselineWinPnl", "baselineLossPnl",
        "after1Trades", "after1Wins", "after1WinPnl", "after1LossPnl",
        "after2Trades", "after2Wins", "after2WinPnl", "after2LossPnl", "days",
      ] as FieldKey[]).find((field) => draftResult.errors[field]);
      if (firstInvalidField) {
        const field = document.getElementById(firstInvalidField);
        field?.scrollIntoView({ behavior: "smooth", block: "center" });
        field?.focus({ preventScroll: true });
      }
      return;
    }

    setSubmittedResult(draftResult);
    setIsStale(false);
    trackSeoToolEvent("revenge_calculator_completed", PAGE_PATH, TOOL_ID);
    trackSeoToolEvent(`revenge_result_${draftResult.state}` as const, PAGE_PATH, TOOL_ID);
  };

  return (
    <MarketingShell>
      <Seo title={TITLE} description={DESCRIPTION} path={PAGE_PATH} canonical={PAGE_PATH} structuredData={schema} />

      <section className="marketing-hero expectancy-hero">
        <div className="marketing-container expectancy-hero-grid overtrading-hero-grid revenge-hero-grid">
          <div>
            <div className="marketing-eyebrow">Free trading tool</div>
            <h1 className="marketing-title">Revenge Trading Calculator</h1>
            <p className="marketing-subtitle hero-support">
              Compare your normal trading performance with trades taken after one or two consecutive losses. See whether expectancy, win rate, or average trade performance changes after losses.
            </p>
            <p className="marketing-section-copy">
              This calculator measures historical differences. It does not diagnose revenge trading or predict future results.
            </p>
            <p className="marketing-trust-copy">Free. No signup required.</p>
          </div>
          <RevengeCalculatorPanel
            inputs={inputs}
            errors={visibleErrors}
            submittedResult={submittedResult}
            isStale={isStale}
            resultRegionRef={resultRegionRef}
            onAnalyze={handleAnalyze}
            onBlur={markTouched}
            onChange={setField}
          />
        </div>
      </section>

      <section className="marketing-section">
        <div className="marketing-container expectancy-content-grid">
          <article className="seo-story-card expectancy-education-card">
            <div className="marketing-eyebrow">Hypothetical example</div>
            <h2>Baseline vs Post-Loss Trades</h2>
            <p>
              Baseline: 40 trades, 24 wins, $4,800 winning P&L and $2,400 losing P&L creates +$2,400 net and +$60/trade expectancy.
            </p>
            <p>
              After 1 Loss: 24 trades, 12 wins, $2,040 winning P&L and $1,800 losing P&L creates +$240 net and +$10/trade expectancy.
            </p>
            <p>
              After 2 Losses: 18 trades, 6 wins, $1,080 winning P&L and $2,160 losing P&L creates -$1,080 net and -$60/trade expectancy.
            </p>
          </article>
          <article className="seo-story-card expectancy-education-card">
            <div className="marketing-eyebrow">Method</div>
            <h2>Estimated Post-Loss Performance Gap</h2>
            <p className="expectancy-formula">(Baseline Expectancy - Post-Loss Expectancy) x Post-Loss Trades</p>
            <p>
              In the hypothetical example, the gap after 1 loss is ($60 - $10) x 24 = $1,200.
              The gap after 2 losses is ($60 - -$60) x 18 = $2,160.
            </p>
          </article>
        </div>
      </section>

      <section className="marketing-section">
        <div className="marketing-container seo-section-grid">
          <article className="seo-story-card">
            <div className="marketing-eyebrow">Definition</div>
            <h2>What Is Revenge Trading?</h2>
            <p>
              Revenge trading generally refers to taking trades influenced by a desire to quickly recover a previous loss.
              Historical trade data cannot directly prove motivation, but it can show whether behavior and results change after losses.
            </p>
            <ul>
              <li>Lower expectancy, lower win rate or larger average losses.</li>
              <li>More frequent trades, larger size or shorter time between trades.</li>
              <li>Lower-quality setups or trading outside planned hours.</li>
            </ul>
          </article>
          <article className="seo-story-card">
            <div className="marketing-eyebrow">Consecutive losses</div>
            <h2>Why Two Consecutive Losses Matter</h2>
            <p>
              One loss may not materially change behavior. A sequence of losses can affect patience, trade frequency, sizing, setup selection and decision quality.
            </p>
            <ul>
              <li>Do not assume a universal rule like always stopping after two losses.</li>
              <li>Use your own historical data to check for post-loss deterioration.</li>
              <li>Compare NQ, MNQ, ES and MES separately where possible.</li>
            </ul>
          </article>
          <article className="seo-story-card">
            <div className="marketing-eyebrow">Variance</div>
            <h2>Post-Loss Deterioration Is Not Automatically Revenge Trading</h2>
            <p>
              Losing streaks occur naturally, even in positive-expectancy strategies. Weaker post-loss results can also come from market regime changes,
              low sample size, poor setup quality, session timing, volatility, fatigue or discretionary behavior.
            </p>
            <ul>
              <li><Link className="seo-related-link" to="/revenge-trading">Read the revenge trading guide</Link>.</li>
              <li><Link className="seo-related-link" to="/trading-after-losses">Review trading after losses</Link>.</li>
              <li><Link className="seo-related-link" to="/tools/overtrading-calculator">Compare early and later trades</Link>.</li>
            </ul>
          </article>
        </div>
      </section>

      <section className="marketing-section">
        <div className="marketing-container audience-panel seo-link-panel revenge-link-panel">
          <div>
            <div className="marketing-eyebrow">TradePilot Coach bridge</div>
            <h2 className="marketing-section-title">Want TradePilot Coach to Find These Patterns Automatically?</h2>
            <p className="marketing-section-copy">
              Import your completed Topstep or Tradovate trades and let TradePilot Coach analyze historical patterns around losses, sessions, trade frequency, instruments and trading behavior automatically.
            </p>
            <Link
              className="marketing-button primary"
              to={ctaDestination}
              onClick={() => trackSeoToolCtaClick(PAGE_PATH, TOOL_ID, CTA_LABEL, ctaDestination, "revenge_calculator_cta_click")}
            >
              {CTA_LABEL}
            </Link>
            <p className="revenge-bridge-note">CSV import. No broker connection required.</p>
          </div>
          <nav className="audience-grid" aria-label="Related revenge trading resources">
            <Link className="audience-item seo-related-link" to="/revenge-trading">Revenge trading</Link>
            <Link className="audience-item seo-related-link" to="/trading-after-losses">Trading after losses</Link>
            <Link className="audience-item seo-related-link" to="/overtrading">Overtrading</Link>
            <Link className="audience-item seo-related-link" to="/trading-discipline">Trading discipline</Link>
            <Link className="audience-item seo-related-link" to="/breaking-trading-rules">Breaking trading rules</Link>
            <Link className="audience-item seo-related-link" to="/tools/trading-expectancy-calculator">Trading expectancy calculator</Link>
            <Link className="audience-item seo-related-link" to="/futures-trading-journal">Futures trading journal</Link>
            <Link className="audience-item seo-related-link" to="/ai-trading-journal">AI trading journal</Link>
          </nav>
        </div>
      </section>
    </MarketingShell>
  );
}

function RevengeCalculatorPanel({
  inputs,
  errors,
  submittedResult,
  isStale,
  resultRegionRef,
  onAnalyze,
  onBlur,
  onChange,
}: {
  inputs: Record<FieldKey, string>;
  errors: Partial<Record<FieldKey, string>>;
  submittedResult: SubmittedRevengeResult | null;
  isStale: boolean;
  resultRegionRef: RefObject<HTMLElement | null>;
  onAnalyze: () => void;
  onBlur: (key: FieldKey) => void;
  onChange: (key: FieldKey, value: string) => void;
}) {
  return (
    <section className="expectancy-calculator-card overtrading-calculator-card revenge-calculator-card" aria-label="Revenge trading calculator">
      <div className="overtrading-groups">
        <TradeGroupInputs title="Baseline Trades" subtitle="Your normal comparison group" group="baseline" inputs={inputs} errors={errors} onBlur={onBlur} onChange={onChange} />
        <TradeGroupInputs title="After 1 Loss" subtitle="Immediately after one losing trade" group="after1" inputs={inputs} errors={errors} onBlur={onBlur} onChange={onChange} />
        <TradeGroupInputs title="After 2 Losses" subtitle="After at least two consecutive losses" group="after2" inputs={inputs} errors={errors} onBlur={onBlur} onChange={onChange} />
        <CalculatorInput id="days" label="Trading days represented" placeholder="20" optional value={inputs.days} error={errors.days} onBlur={() => onBlur("days")} onChange={(value) => onChange("days", value)} />
      </div>
      <div className="revenge-analysis-action">
        <button type="button" className="revenge-analysis-button" onClick={onAnalyze}>
          Analyze Post-Loss Performance
        </button>
        <p>Enter all three comparison groups, then analyze your post-loss performance.</p>
      </div>
      <RevengeResultPanel result={submittedResult} isStale={isStale} resultRegionRef={resultRegionRef} />
    </section>
  );
}

function TradeGroupInputs({
  title,
  subtitle,
  group,
  inputs,
  errors,
  onBlur,
  onChange,
}: {
  title: string;
  subtitle: string;
  group: GroupKey;
  inputs: Record<FieldKey, string>;
  errors: Partial<Record<FieldKey, string>>;
  onBlur: (key: FieldKey) => void;
  onChange: (key: FieldKey, value: string) => void;
}) {
  const tradesKey = `${group}Trades` as FieldKey;
  const winsKey = `${group}Wins` as FieldKey;
  const winPnlKey = `${group}WinPnl` as FieldKey;
  const lossPnlKey = `${group}LossPnl` as FieldKey;

  return (
    <fieldset className="overtrading-group-card">
      <legend>
        <span>{title}</span>
        <strong>{subtitle}</strong>
      </legend>
      <CalculatorInput id={tradesKey} label="Number of trades" placeholder={group === "baseline" ? "40" : group === "after1" ? "24" : "18"} value={inputs[tradesKey]} error={errors[tradesKey]} onBlur={() => onBlur(tradesKey)} onChange={(value) => onChange(tradesKey, value)} />
      <CalculatorInput id={winsKey} label="Number of winning trades" placeholder={group === "baseline" ? "24" : group === "after1" ? "12" : "6"} value={inputs[winsKey]} error={errors[winsKey]} onBlur={() => onBlur(winsKey)} onChange={(value) => onChange(winsKey, value)} />
      <CalculatorInput id={winPnlKey} label="Total winning P&L" prefix="$" placeholder={group === "baseline" ? "4800" : group === "after1" ? "2040" : "1080"} helper="Combined profit from winning trades in this group." value={inputs[winPnlKey]} error={errors[winPnlKey]} onBlur={() => onBlur(winPnlKey)} onChange={(value) => onChange(winPnlKey, value)} />
      <CalculatorInput id={lossPnlKey} label="Total losing P&L" prefix="$" placeholder={group === "baseline" ? "2400" : group === "after1" ? "1800" : "2160"} helper="Enter the combined loss from losing trades as a positive number." value={inputs[lossPnlKey]} error={errors[lossPnlKey]} onBlur={() => onBlur(lossPnlKey)} onChange={(value) => onChange(lossPnlKey, value)} />
    </fieldset>
  );
}

function CalculatorInput({
  id,
  label,
  value,
  placeholder,
  prefix,
  helper,
  optional = false,
  error,
  onBlur,
  onChange,
}: {
  id: FieldKey;
  label: string;
  value: string;
  placeholder: string;
  prefix?: string;
  helper?: string;
  optional?: boolean;
  error?: string;
  onBlur: () => void;
  onChange: (value: string) => void;
}) {
  const describedBy = [helper ? `${id}-helper` : "", error ? `${id}-error` : ""].filter(Boolean).join(" ") || undefined;
  return (
    <label className="expectancy-field" htmlFor={id}>
      <span>
        {label}
        {optional && <em>Optional</em>}
      </span>
      <div className="expectancy-number-shell">
        {prefix && <strong aria-hidden="true">{prefix}</strong>}
        <input
          id={id}
          name={id}
          type="text"
          inputMode="decimal"
          autoComplete="off"
          placeholder={placeholder}
          value={value}
          aria-invalid={Boolean(error)}
          aria-describedby={describedBy}
          onBlur={onBlur}
          onChange={(event) => onChange(event.target.value)}
        />
      </div>
      {helper && <small id={`${id}-helper`}>{helper}</small>}
      {error && <small id={`${id}-error`} className="expectancy-error" role="alert">{error}</small>}
    </label>
  );
}

function RevengeResultPanel({
  result,
  isStale,
  resultRegionRef,
}: {
  result: SubmittedRevengeResult | null;
  isStale: boolean;
  resultRegionRef: RefObject<HTMLElement | null>;
}) {
  if (!result) {
    return (
      <aside ref={resultRegionRef} className="expectancy-result-card empty revenge-empty-state" aria-live="polite" tabIndex={-1} data-testid="revenge-analysis-result">
        <div className="report-kicker">Post-loss analysis</div>
        <strong>{isStale ? "Inputs changed. Analyze again to refresh the comparison." : "Compare Your Post-Loss Performance"}</strong>
        {isStale ? (
          <p>Analyze again to replace the previous comparison with your updated inputs.</p>
        ) : (
        <ol className="revenge-empty-steps">
          <li>Enter your baseline trades.</li>
          <li>Add trades taken after one loss.</li>
          <li>Add trades taken after two consecutive losses.</li>
          <li>Click Analyze Post-Loss Performance.</li>
        </ol>
        )}
      </aside>
    );
  }

  const heading =
    result.state === "strong_post_loss_deterioration"
      ? "Stronger deterioration appears after consecutive losses."
      : result.state === "mild_deterioration"
        ? "Post-loss performance appears weaker in this comparison."
        : "No clear post-loss deterioration appears in this comparison.";

  return (
    <aside ref={resultRegionRef} className={`expectancy-result-card overtrading-result revenge-result ${result.state}`} aria-live="polite" tabIndex={-1} data-testid="revenge-analysis-result">
      <div className="report-kicker">Post-loss analysis</div>
      <strong className="overtrading-result-heading">{heading}</strong>
      <div className="revenge-sequence" aria-label="Post-loss expectancy sequence">
        <MetricMiniCard label="Baseline" value={`${formatMoneyWithCents(result.baseline.expectancy)}/trade`} helper={formatPct(result.baseline.winRate)} />
        <MetricMiniCard label="After 1 Loss" value={`${formatMoneyWithCents(result.after1.expectancy)}/trade`} helper={formatPct(result.after1.winRate)} />
        <MetricMiniCard label="After 2 Losses" value={`${formatMoneyWithCents(result.after2.expectancy)}/trade`} helper={formatPct(result.after2.winRate)} />
      </div>
      <dl className="expectancy-result-grid">
        <div><dt>Expectancy Change After 1 Loss</dt><dd>{formatMoneyWithCents(result.after1Comparison.expectancyChange)}/trade</dd></div>
        <div><dt>Expectancy Change After 2 Losses</dt><dd>{formatMoneyWithCents(result.after2Comparison.expectancyChange)}/trade</dd></div>
        <div><dt>Win Rate Change After 1 Loss</dt><dd>{formatPercentagePoint(result.after1Comparison.winRateChange)}</dd></div>
        <div><dt>Win Rate Change After 2 Losses</dt><dd>{formatPercentagePoint(result.after2Comparison.winRateChange)}</dd></div>
        <div><dt>Baseline Net P&L</dt><dd>{formatMoney(result.baseline.netPnl)}</dd></div>
        <div><dt>After 2 Losses Net P&L</dt><dd>{formatMoney(result.after2.netPnl)}</dd></div>
      </dl>
      <div className="expectancy-monthly">
        <span>Estimated Performance Gap After 1 Loss</span>
        <strong>{formatUnsignedMoney(result.after1Comparison.performanceGap)}</strong>
      </div>
      <div className="expectancy-monthly">
        <span>Estimated Performance Gap After 2 Losses</span>
        <strong>{formatUnsignedMoney(result.after2Comparison.performanceGap)}</strong>
      </div>
      {result.gapPerDay !== null && (
        <div className="expectancy-monthly overtrading-day-gap">
          <span>Estimated Post-Loss Gap Per Trading Day</span>
          <strong>{formatUnsignedMoney(result.gapPerDay)}/day</strong>
        </div>
      )}
      <p className="expectancy-interpretation">
        {result.state === "strong_post_loss_deterioration" &&
          "Your results deteriorate more noticeably after consecutive losses based on the inputs provided. This may be useful to investigate for changes in decision quality, position size, trade frequency, setup quality, session duration and rule adherence."}
        {result.state === "mild_deterioration" &&
          "Your performance appears weaker after losses. The difference is worth monitoring, but this calculator cannot determine the cause."}
        {result.state === "no_clear_deterioration" &&
          "Your post-loss trading does not show a clear expectancy deterioration based on the inputs provided. This comparison alone does not suggest an obvious post-loss performance problem."}
      </p>
      <p className="expectancy-disclaimer">
        Use this calculator as a pattern-finding tool, not a diagnosis. It does not prove revenge trading or explain why any individual trade was placed.
      </p>
    </aside>
  );
}

function MetricMiniCard({ label, value, helper }: { label: string; value: string; helper: string }) {
  return (
    <div>
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{helper} win rate</small>
    </div>
  );
}
