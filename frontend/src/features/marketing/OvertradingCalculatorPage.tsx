import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import Seo, { SITE_URL } from "../../components/Seo";
import { activationAwareCtaDestination } from "../../utils/activationRedirect";
import { trackSeoToolCtaClick, trackSeoToolEvent } from "../../utils/seoAttribution";
import "./HomePage.css";
import { MarketingShell } from "./MarketingLayout";

const PAGE_PATH = "/tools/overtrading-calculator";
const TOOL_ID = "overtrading_calculator";
const CTA_LABEL = "Find My Trading Leaks";
const CTA_SOURCE = "overtrading-calculator";
const TITLE = "Overtrading Calculator: Compare Early vs Later Trades | TradePilot";
const DESCRIPTION =
  "Compare your early trades with later trades to see whether your win rate, expectancy and P&L deteriorate as you continue trading.";
const MAX_VALUE = 1_000_000_000;
const MIN_EXPECTANCY_GAP = 10;
const MATERIALITY_RATIO = 0.05;

type GroupKey = "early" | "later";
type FieldKey =
  | "earlyTrades"
  | "earlyWins"
  | "earlyWinPnl"
  | "earlyLossPnl"
  | "laterTrades"
  | "laterWins"
  | "laterWinPnl"
  | "laterLossPnl"
  | "days";
type ResultState = "later_worse" | "similar" | "later_better";

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

type ComparisonResult =
  | {
      ready: false;
      errors: Partial<Record<FieldKey, string>>;
    }
  | {
      ready: true;
      errors: Partial<Record<FieldKey, string>>;
      early: TradeGroupMetrics;
      later: TradeGroupMetrics;
      days: number | null;
      winRateDifference: number;
      expectancyGap: number;
      netPnlDifference: number;
      laterPerformanceGap: number;
      gapPerDay: number | null;
      state: ResultState;
    };

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

function calculatePerformanceGap(earlyExpectancy: number, laterExpectancy: number, laterTrades: number) {
  return (earlyExpectancy - laterExpectancy) * laterTrades;
}

function classifyLaterTradePerformance(earlyExpectancy: number, laterExpectancy: number): ResultState {
  const difference = laterExpectancy - earlyExpectancy;
  const largerAbsoluteExpectancy = Math.max(Math.abs(earlyExpectancy), Math.abs(laterExpectancy));
  // Treat tiny differences as similar to avoid dramatic state changes over statistical noise.
  const tolerance = Math.max(MIN_EXPECTANCY_GAP, largerAbsoluteExpectancy * MATERIALITY_RATIO);
  if (Math.abs(difference) <= tolerance) return "similar";
  return difference < 0 ? "later_worse" : "later_better";
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
    minimumFractionDigits: value % 1 === 0 ? 0 : 2,
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

export default function OvertradingCalculatorPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [inputs, setInputs] = useState<Record<FieldKey, string>>(() => ({
    earlyTrades: cleanQueryValue(searchParams.get("earlyTrades"), (value) => parseWholeNumber(value, "Early trades")),
    earlyWins: cleanQueryValue(searchParams.get("earlyWins"), (value) => parseWholeNumber(value, "Early winning trades")),
    earlyWinPnl: cleanQueryValue(searchParams.get("earlyWinPnl"), (value) => parseDecimal(value, "Early total winning P&L", true)),
    earlyLossPnl: cleanQueryValue(searchParams.get("earlyLossPnl"), (value) => parseDecimal(value, "Early total losing P&L", true)),
    laterTrades: cleanQueryValue(searchParams.get("laterTrades"), (value) => parseWholeNumber(value, "Later trades")),
    laterWins: cleanQueryValue(searchParams.get("laterWins"), (value) => parseWholeNumber(value, "Later winning trades")),
    laterWinPnl: cleanQueryValue(searchParams.get("laterWinPnl"), (value) => parseDecimal(value, "Later total winning P&L", true)),
    laterLossPnl: cleanQueryValue(searchParams.get("laterLossPnl"), (value) => parseDecimal(value, "Later total losing P&L", true)),
    days: cleanQueryValue(searchParams.get("days"), (value) => parseWholeNumber(value, "Trading days represented", false)),
  }));
  const [touchedFields, setTouchedFields] = useState<Partial<Record<FieldKey, boolean>>>({});
  const [analysisAttempted, setAnalysisAttempted] = useState(false);
  const hasTrackedStart = useRef(false);
  const lastTrackedState = useRef<ResultState | null>(null);
  const ctaDestination = activationAwareCtaDestination(CTA_SOURCE);

  const comparison = useMemo<ComparisonResult>(() => {
    const parsed = {
      earlyTrades: parseWholeNumber(inputs.earlyTrades, "Early trades"),
      earlyWins: parseWholeNumber(inputs.earlyWins, "Early winning trades"),
      earlyWinPnl: parseDecimal(inputs.earlyWinPnl, "Early total winning P&L", true),
      earlyLossPnl: parseDecimal(inputs.earlyLossPnl, "Early total losing P&L", true),
      laterTrades: parseWholeNumber(inputs.laterTrades, "Later trades"),
      laterWins: parseWholeNumber(inputs.laterWins, "Later winning trades"),
      laterWinPnl: parseDecimal(inputs.laterWinPnl, "Later total winning P&L", true),
      laterLossPnl: parseDecimal(inputs.laterLossPnl, "Later total losing P&L", true),
      days: parseWholeNumber(inputs.days, "Trading days represented", false),
    };
    const errors: Partial<Record<FieldKey, string>> = Object.fromEntries(
      Object.entries(parsed).map(([key, field]) => [key, field.error])
    );

    for (const key of ["earlyTrades", "laterTrades"] as const) {
      if (parsed[key].value === 0 && inputs[key].trim()) {
        errors[key] = key === "earlyTrades" ? "Early trades must be at least 1." : "Later trades must be at least 1.";
      }
    }
    if (parsed.days.value === 0 && inputs.days.trim()) {
      errors.days = "Trading days represented must be at least 1.";
    }
    if (parsed.earlyWins.value !== null && parsed.earlyTrades.value !== null && parsed.earlyWins.value > parsed.earlyTrades.value) {
      errors.earlyWins = "Early winning trades cannot exceed early trades.";
    }
    if (parsed.laterWins.value !== null && parsed.laterTrades.value !== null && parsed.laterWins.value > parsed.laterTrades.value) {
      errors.laterWins = "Later winning trades cannot exceed later trades.";
    }

    const ready =
      parsed.earlyTrades.value !== null &&
      parsed.earlyTrades.value > 0 &&
      parsed.earlyWins.value !== null &&
      parsed.earlyWinPnl.value !== null &&
      parsed.earlyLossPnl.value !== null &&
      parsed.laterTrades.value !== null &&
      parsed.laterTrades.value > 0 &&
      parsed.laterWins.value !== null &&
      parsed.laterWinPnl.value !== null &&
      parsed.laterLossPnl.value !== null &&
      !Object.values(errors).some(Boolean);

    if (!ready) {
      return { ready: false, errors };
    }

    const early = calculateTradeGroupMetrics({
      trades: parsed.earlyTrades.value as number,
      wins: parsed.earlyWins.value as number,
      winningPnl: parsed.earlyWinPnl.value as number,
      losingPnl: parsed.earlyLossPnl.value as number,
    });
    const later = calculateTradeGroupMetrics({
      trades: parsed.laterTrades.value as number,
      wins: parsed.laterWins.value as number,
      winningPnl: parsed.laterWinPnl.value as number,
      losingPnl: parsed.laterLossPnl.value as number,
    });
    const winRateDifference = later.winRate - early.winRate;
    const expectancyGap = later.expectancy - early.expectancy;
    const laterPerformanceGap = calculatePerformanceGap(early.expectancy, later.expectancy, later.trades);
    const days = parsed.days.value && parsed.days.value > 0 ? parsed.days.value : null;

    return {
      ready: true,
      errors,
      early,
      later,
      days,
      winRateDifference,
      expectancyGap,
      netPnlDifference: later.netPnl - early.netPnl,
      laterPerformanceGap,
      gapPerDay: days ? laterPerformanceGap / days : null,
      state: classifyLaterTradePerformance(early.expectancy, later.expectancy),
    };
  }, [inputs]);

  const visibleErrors = useMemo<Partial<Record<FieldKey, string>>>(() => {
    const next: Partial<Record<FieldKey, string>> = {};
    (Object.entries(comparison.errors) as Array<[FieldKey, string | undefined]>).forEach(([key, error]) => {
      if (error && (analysisAttempted || touchedFields[key] || inputs[key].trim())) {
        next[key] = error;
      }
    });
    return next;
  }, [analysisAttempted, comparison.errors, inputs, touchedFields]);

  useEffect(() => {
    trackSeoToolEvent("tool_page_view", PAGE_PATH, TOOL_ID);
  }, []);

  useEffect(() => {
    if (!hasTrackedStart.current && Object.values(inputs).some((value) => value.trim())) {
      hasTrackedStart.current = true;
      trackSeoToolEvent("overtrading_calculator_started", PAGE_PATH, TOOL_ID);
    }
  }, [inputs]);

  useEffect(() => {
    if (!comparison.ready) return;
    trackSeoToolEvent("overtrading_calculator_completed", PAGE_PATH, TOOL_ID);
    if (lastTrackedState.current !== comparison.state) {
      lastTrackedState.current = comparison.state;
      trackSeoToolEvent(`overtrading_result_${comparison.state}` as const, PAGE_PATH, TOOL_ID);
    }
  }, [comparison]);

  useEffect(() => {
    const next = new URLSearchParams(window.location.search);
    const queryKeys: FieldKey[] = [
      "earlyTrades",
      "earlyWins",
      "earlyWinPnl",
      "earlyLossPnl",
      "laterTrades",
      "laterWins",
      "laterWinPnl",
      "laterLossPnl",
      "days",
    ];
    queryKeys.forEach((key) => next.delete(key));
    queryKeys.forEach((key) => {
      const value = inputs[key].trim();
      if (value && !comparison.errors[key]) next.set(key, value);
    });
    const current = new URLSearchParams(window.location.search);
    if (next.toString() !== current.toString()) {
      setSearchParams(next, { replace: true });
    }
  }, [comparison.errors, inputs, setSearchParams]);

  const schema = {
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "WebApplication",
        name: "Overtrading Calculator",
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
          { "@type": "ListItem", position: 3, name: "Overtrading Calculator", item: `${SITE_URL}${PAGE_PATH}` },
        ],
      },
    ],
  };

  const setField = (key: FieldKey, value: string) => {
    setInputs((current) => ({ ...current, [key]: value }));
  };

  const markTouched = (key: FieldKey) => {
    setTouchedFields((current) => ({ ...current, [key]: true }));
  };

  const handleAnalyze = () => {
    setAnalysisAttempted(true);
  };

  return (
    <MarketingShell>
      <Seo title={TITLE} description={DESCRIPTION} path={PAGE_PATH} canonical={PAGE_PATH} structuredData={schema} />

      <section className="marketing-hero expectancy-hero">
        <div className="marketing-container expectancy-hero-grid overtrading-hero-grid">
          <div>
            <div className="marketing-eyebrow">Free trading tool</div>
            <h1 className="marketing-title">Overtrading Calculator</h1>
            <p className="marketing-subtitle hero-support">
              Compare early-session trades with later-session trades to see whether historical performance changes as you continue trading.
            </p>
            <p className="marketing-section-copy">
              The default split uses trades 1-3 vs trade 4+. A later-trade performance difference does not automatically mean overtrading.
            </p>
            <p className="marketing-trust-copy">Free. No signup required.</p>
          </div>
          <OvertradingCalculatorPanel
            inputs={inputs}
            errors={visibleErrors}
            comparison={comparison}
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
            <h2>Early Trades vs Later Trades</h2>
            <p>
              Early Trades: 36 trades, 21 wins, $4,200 total winning P&L and $1,950 total losing P&L.
              Net result is +$2,250 and expectancy is +$62.50/trade.
            </p>
            <p>
              Later Trades: 28 trades, 11 wins, $2,100 total winning P&L and $3,850 total losing P&L.
              Net result is -$1,750 and expectancy is -$62.50/trade.
            </p>
          </article>
          <article className="seo-story-card expectancy-education-card">
            <div className="marketing-eyebrow">Method</div>
            <h2>Later-Trade Performance Gap</h2>
            <p className="expectancy-formula">(Early Expectancy - Later Expectancy) x Later Trades</p>
            <p>
              In the hypothetical example, ($62.50 - -$62.50) x 28 = $3,500. This compares what the later-trade group produced
              with what it would have produced if it had matched the early-trade expectancy.
            </p>
          </article>
        </div>
      </section>

      <section className="marketing-section">
        <div className="marketing-container seo-section-grid">
          <article className="seo-story-card">
            <div className="marketing-eyebrow">Definition</div>
            <h2>What Does Overtrading Actually Mean?</h2>
            <p>
              Overtrading is not simply taking more than a fixed number of trades. A trader may legitimately take many high-quality
              trades on one day and only one trade on another.
            </p>
            <ul>
              <li>Lower expectancy or lower win rate later in the session.</li>
              <li>Larger losses, smaller average wins, or increased position size.</li>
              <li>Shorter time between trades or trading outside the planned session.</li>
            </ul>
          </article>
          <article className="seo-story-card">
            <div className="marketing-eyebrow">Context</div>
            <h2>Why Trade Count Alone Is Not Enough</h2>
            <p>
              Ten trades is not automatically overtrading, and three trades is not automatically disciplined. A systematic scalper may
              have more valid trades than a discretionary swing trader.
            </p>
            <ul>
              <li>A second trade can be poor if it violates the plan.</li>
              <li>Trade quality matters more than a generic count.</li>
              <li>Compare performance by trade number before drawing conclusions.</li>
            </ul>
          </article>
          <article className="seo-story-card">
            <div className="marketing-eyebrow">Review framework</div>
            <h2>What Should You Review Next?</h2>
            <p>
              If later performance is worse, review time of day, performance after losses, position size, setup quality, number of trades,
              planned-session boundaries and whether results differ by NQ, MNQ, ES or MES.
            </p>
            <ul>
              <li><Link className="seo-related-link" to="/trading-after-losses">Compare performance after losses</Link>.</li>
              <li><Link className="seo-related-link" to="/trading-discipline">Review discipline and rule adherence</Link>.</li>
              <li><Link className="seo-related-link" to="/breaking-trading-rules">Inspect broken trading rules</Link>.</li>
            </ul>
          </article>
        </div>
      </section>

      <section className="marketing-section">
        <div className="marketing-container audience-panel seo-link-panel">
          <div>
            <div className="marketing-eyebrow">TradePilot bridge</div>
            <h2 className="marketing-section-title">Want TradePilot to Find Where Your Performance Drops?</h2>
            <p className="marketing-section-copy">
              Import your completed Topstep or Tradovate trades and let TradePilot compare historical performance across trade number, sessions, losses, instruments and trading behavior.
            </p>
            <Link
              className="marketing-button primary"
              to={ctaDestination}
              onClick={() => trackSeoToolCtaClick(PAGE_PATH, TOOL_ID, CTA_LABEL, ctaDestination, "overtrading_cta_click")}
            >
              {CTA_LABEL}
            </Link>
            <p className="revenge-bridge-note">CSV import. No broker connection required.</p>
          </div>
          <nav className="audience-grid" aria-label="Related overtrading resources">
            <Link className="audience-item seo-related-link" to="/overtrading">Overtrading</Link>
            <Link className="audience-item seo-related-link" to="/revenge-trading">Revenge trading</Link>
            <Link className="audience-item seo-related-link" to="/futures-trading-journal">Futures trading journal</Link>
            <Link className="audience-item seo-related-link" to="/ai-trading-journal">AI trading journal</Link>
          </nav>
        </div>
      </section>
    </MarketingShell>
  );
}

function OvertradingCalculatorPanel({
  inputs,
  errors,
  comparison,
  onAnalyze,
  onBlur,
  onChange,
}: {
  inputs: Record<FieldKey, string>;
  errors: Partial<Record<FieldKey, string>>;
  comparison: ComparisonResult;
  onAnalyze: () => void;
  onBlur: (key: FieldKey) => void;
  onChange: (key: FieldKey, value: string) => void;
}) {
  return (
    <section className="expectancy-calculator-card overtrading-calculator-card overtrading-suite-card" aria-label="Overtrading calculator">
      <div className="overtrading-groups">
        <TradeGroupInputs
          title="Early Trades"
          subtitle="Trades 1-3"
          group="early"
          inputs={inputs}
          errors={errors}
          onBlur={onBlur}
          onChange={onChange}
        />
        <TradeGroupInputs
          title="Later Trades"
          subtitle="Trade 4+"
          group="later"
          inputs={inputs}
          errors={errors}
          onBlur={onBlur}
          onChange={onChange}
        />
        <CalculatorInput
          id="days"
          label="Trading days represented"
          placeholder="12"
          optional
          value={inputs.days}
          error={errors.days}
          onBlur={() => onBlur("days")}
          onChange={(value) => onChange("days", value)}
        />
      </div>
      <div className="calculator-action-row">
        <button type="button" className="revenge-analysis-button" onClick={onAnalyze}>
          Analyze Later-Trade Performance
        </button>
        <p>Results update automatically as valid inputs are entered.</p>
      </div>
      <OvertradingResultPanel comparison={comparison} />
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
  const prefix = group === "early" ? "early" : "later";
  const tradesKey = `${prefix}Trades` as FieldKey;
  const winsKey = `${prefix}Wins` as FieldKey;
  const winPnlKey = `${prefix}WinPnl` as FieldKey;
  const lossPnlKey = `${prefix}LossPnl` as FieldKey;

  return (
    <fieldset className="overtrading-group-card">
      <legend>
        <span>{title}</span>
        <strong>{subtitle}</strong>
      </legend>
      <CalculatorInput
        id={tradesKey}
        label="Number of trades"
        placeholder={group === "early" ? "36" : "28"}
        value={inputs[tradesKey]}
        error={errors[tradesKey]}
        onBlur={() => onBlur(tradesKey)}
        onChange={(value) => onChange(tradesKey, value)}
      />
      <CalculatorInput
        id={winsKey}
        label="Number of winning trades"
        placeholder={group === "early" ? "21" : "11"}
        value={inputs[winsKey]}
        error={errors[winsKey]}
        onBlur={() => onBlur(winsKey)}
        onChange={(value) => onChange(winsKey, value)}
      />
      <CalculatorInput
        id={winPnlKey}
        label="Total winning P&L"
        prefix="$"
        placeholder={group === "early" ? "4200" : "2100"}
        helper="Enter the combined profit from winning trades in this group."
        value={inputs[winPnlKey]}
        error={errors[winPnlKey]}
        onBlur={() => onBlur(winPnlKey)}
        onChange={(value) => onChange(winPnlKey, value)}
      />
      <CalculatorInput
        id={lossPnlKey}
        label="Total losing P&L"
        prefix="$"
        placeholder={group === "early" ? "1950" : "3850"}
        helper="Enter the combined loss from losing trades as a positive number."
        value={inputs[lossPnlKey]}
        error={errors[lossPnlKey]}
        onBlur={() => onBlur(lossPnlKey)}
        onChange={(value) => onChange(lossPnlKey, value)}
      />
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

function OvertradingResultPanel({ comparison }: { comparison: ComparisonResult }) {
  if (!comparison.ready) {
    return (
      <aside className="expectancy-result-card empty calculator-empty-state" aria-live="polite">
        <div className="report-kicker">Later-trade comparison</div>
        <strong>Compare Early vs Later Trades</strong>
        <ol className="revenge-empty-steps">
          <li>Enter your early-session trade group.</li>
          <li>Add your later-session trade group.</li>
          <li>Analyze expectancy and performance differences.</li>
        </ol>
        <p>Trade count alone does not establish overtrading. This calculator only compares the historical groups you provide.</p>
      </aside>
    );
  }

  const heading =
    comparison.state === "later_worse"
      ? "Later performance is materially worse in this comparison."
      : comparison.state === "later_better"
        ? "Later performance is better in this comparison."
        : "Early and later trades are broadly similar in this comparison.";

  return (
    <aside className={`expectancy-result-card overtrading-result ${comparison.state}`} aria-live="polite">
      <div className="report-kicker">Later-trade comparison</div>
      <strong className="overtrading-result-heading">{heading}</strong>
      <div className="calculator-result-summary">
        <MetricMiniCard label="Early expectancy" value={`${formatMoneyWithCents(comparison.early.expectancy)}/trade`} />
        <MetricMiniCard label="Later expectancy" value={`${formatMoneyWithCents(comparison.later.expectancy)}/trade`} />
        <MetricMiniCard label="Difference" value={`${formatMoneyWithCents(comparison.expectancyGap)}/trade`} />
      </div>
      <dl className="expectancy-result-grid">
        <div><dt>Early Win Rate</dt><dd>{formatPct(comparison.early.winRate)}</dd></div>
        <div><dt>Later Win Rate</dt><dd>{formatPct(comparison.later.winRate)}</dd></div>
        <div><dt>Early Expectancy</dt><dd>{formatMoneyWithCents(comparison.early.expectancy)}/trade</dd></div>
        <div><dt>Later Expectancy</dt><dd>{formatMoneyWithCents(comparison.later.expectancy)}/trade</dd></div>
        <div><dt>Expectancy Gap</dt><dd>{formatMoneyWithCents(comparison.expectancyGap)}/trade</dd></div>
        <div><dt>Win Rate Difference</dt><dd>{formatPercentagePoint(comparison.winRateDifference)}</dd></div>
        <div><dt>Early Net P&L</dt><dd>{formatMoney(comparison.early.netPnl)}</dd></div>
        <div><dt>Later Net P&L</dt><dd>{formatMoney(comparison.later.netPnl)}</dd></div>
      </dl>
      <div className="expectancy-monthly">
        <span>Estimated Later-Trade Performance Gap</span>
        <strong>{formatUnsignedMoney(comparison.laterPerformanceGap)}</strong>
      </div>
      {comparison.gapPerDay !== null && (
        <div className="expectancy-monthly overtrading-day-gap">
          <span>Estimated Gap Per Trading Day</span>
          <strong>{formatUnsignedMoney(comparison.gapPerDay)}/day</strong>
        </div>
      )}
      <p className="expectancy-interpretation">
        {comparison.state === "later_worse" &&
          "Your later trades are performing worse than your early trades based on the inputs provided. This may indicate that decision quality, setup quality or discipline changes as the session continues. This is a pattern worth investigating."}
        {comparison.state === "similar" &&
          "Your early and later trades have similar expectancy based on these inputs. There is no obvious later-session deterioration in this comparison alone."}
        {comparison.state === "later_better" &&
          "Your later trades are performing better than your early trades based on these inputs. This comparison does not indicate later-session deterioration."}
      </p>
      <p className="expectancy-disclaimer">
        The performance gap is a counterfactual comparison, not proven causation. It does not diagnose overtrading or provide financial advice.
      </p>
    </aside>
  );
}

function MetricMiniCard({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
