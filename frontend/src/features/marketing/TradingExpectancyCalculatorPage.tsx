import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import Seo from "../../components/Seo";
import { pageStructuredData, type BreadcrumbItem } from "../../seo/structuredData";
import { activationAwareCtaDestination } from "../../utils/activationRedirect";
import { trackSeoToolCtaClick, trackSeoToolEvent } from "../../utils/seoAttribution";
import "./HomePage.css";
import { MarketingShell } from "./MarketingLayout";
import { Breadcrumbs } from "./Breadcrumbs";

const PAGE_PATH = "/tools/trading-expectancy-calculator";
const TOOL_ID = "trading_expectancy_calculator";
const CTA_LABEL = "Find My Trading Leaks";
const CTA_SOURCE = "expectancy-calculator";
const TITLE = "Trading Expectancy Calculator for Futures Traders | TradePilot Coach";
const DESCRIPTION =
  "Calculate your trading expectancy using win rate, average win and average loss. See your expected value per trade and understand what your trading performance means.";
const BREADCRUMBS: BreadcrumbItem[] = [
  { label: "Home", path: "/" },
  { label: "Trading Expectancy Calculator", path: PAGE_PATH },
];
const MAX_VALUE = 1_000_000_000;
const BREAKEVEN_EPSILON = 0.005;

type FieldKey = "winRate" | "avgWin" | "avgLoss" | "trades" | "fees";
type ResultState = "positive" | "negative" | "breakeven";

type ParsedField = {
  value: number | null;
  error?: string;
};

type CalculatorResult =
  | {
      ready: false;
      errors: Partial<Record<FieldKey, string>>;
    }
  | {
      ready: true;
      errors: Partial<Record<FieldKey, string>>;
      winRate: number;
      lossRate: number;
      avgWin: number;
      avgLoss: number;
      trades: number | null;
      fees: number;
      grossExpectancy: number;
      netExpectancy: number;
      monthlyExpectancy: number | null;
      rewardToRisk: number;
      breakevenWinRate: number;
      state: ResultState;
    };

function parsePositiveDecimal(raw: string, label: string, required: boolean): ParsedField {
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

function parseWinRate(raw: string): ParsedField {
  const parsed = parsePositiveDecimal(raw, "Win rate", true);
  if (parsed.error || parsed.value === null) return parsed;
  if (parsed.value > 100) {
    return { value: null, error: "Win rate must be between 0 and 100." };
  }
  return parsed;
}

type MoneyFormatOptions = {
  maximumFractionDigits?: number;
};

function formatMoney(value: number, options?: MoneyFormatOptions) {
  const safeValue = Number.isFinite(value) ? value : 0;
  const requestedDigits = options?.maximumFractionDigits ?? 2;
  const maximumFractionDigits = Number.isFinite(requestedDigits)
    ? Math.min(20, Math.max(0, Math.trunc(requestedDigits)))
    : 2;
  const sign = safeValue > 0 ? "+" : safeValue < 0 ? "-" : "";
  const absolute = Math.abs(safeValue);
  return `${sign}${new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: Math.min(2, maximumFractionDigits),
    maximumFractionDigits,
  }).format(absolute)}`;
}

function formatMoneyWithCents(value: number) {
  return formatMoney(value);
}

function formatCurrencyWithCents(value: number) {
  const safeValue = Number.isFinite(value) ? value : 0;
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Math.abs(safeValue));
}

function formatPct(value: number, maximumFractionDigits = 1) {
  return `${new Intl.NumberFormat("en-US", {
    maximumFractionDigits,
  }).format(value)}%`;
}

function cleanQueryValue(value: string | null, parser: (raw: string) => ParsedField) {
  if (!value) return "";
  const parsed = parser(value);
  return parsed.error || parsed.value === null ? "" : value;
}

export default function TradingExpectancyCalculatorPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [inputs, setInputs] = useState<Record<FieldKey, string>>(() => ({
    winRate: cleanQueryValue(searchParams.get("winRate"), parseWinRate),
    avgWin: cleanQueryValue(searchParams.get("avgWin"), (value) => parsePositiveDecimal(value, "Average winning trade", true)),
    avgLoss: cleanQueryValue(searchParams.get("avgLoss"), (value) => parsePositiveDecimal(value, "Average losing trade", true)),
    trades: cleanQueryValue(searchParams.get("trades"), (value) => parsePositiveDecimal(value, "Trades per month", false)),
    fees: cleanQueryValue(searchParams.get("fees"), (value) => parsePositiveDecimal(value, "Average fees and commissions", false)),
  }));
  const [touchedFields, setTouchedFields] = useState<Partial<Record<FieldKey, boolean>>>({});
  const [analysisAttempted, setAnalysisAttempted] = useState(false);
  const hasTrackedStart = useRef(false);
  const lastTrackedState = useRef<ResultState | null>(null);

  const calculation = useMemo<CalculatorResult>(() => {
    const winRate = parseWinRate(inputs.winRate);
    const avgWin = parsePositiveDecimal(inputs.avgWin, "Average winning trade", true);
    const avgLoss = parsePositiveDecimal(inputs.avgLoss, "Average losing trade", true);
    const trades = parsePositiveDecimal(inputs.trades, "Trades per month", false);
    const fees = parsePositiveDecimal(inputs.fees, "Average fees and commissions", false);
    const errors: Partial<Record<FieldKey, string>> = {
      winRate: winRate.error,
      avgWin: avgWin.error,
      avgLoss: avgLoss.error,
      trades: trades.error,
      fees: fees.error,
    };

    if (avgLoss.value === 0 && inputs.avgLoss.trim()) {
      errors.avgLoss = "Average losing trade must be greater than zero.";
    }
    const ready = winRate.value !== null && avgWin.value !== null && avgLoss.value !== null && avgLoss.value > 0 && !errors.winRate && !errors.avgWin && !errors.avgLoss && !errors.fees && !errors.trades;
    if (!ready) {
      return { ready: false, errors };
    }

    const validWinRate = winRate.value as number;
    const validAvgWin = avgWin.value as number;
    const validAvgLoss = avgLoss.value as number;

    const winProbability = validWinRate / 100;
    const lossProbability = 1 - winProbability;
    const grossExpectancy = winProbability * validAvgWin - lossProbability * validAvgLoss;
    const feesPerTrade = fees.value ?? 0;
    const netExpectancy = grossExpectancy - feesPerTrade;
    const state: ResultState =
      Math.abs(netExpectancy) <= BREAKEVEN_EPSILON ? "breakeven" : netExpectancy > 0 ? "positive" : "negative";

    return {
      ready: true,
      errors,
      winRate: validWinRate,
      lossRate: 100 - validWinRate,
      avgWin: validAvgWin,
      avgLoss: validAvgLoss,
      trades: trades.value,
      fees: feesPerTrade,
      grossExpectancy,
      netExpectancy,
      monthlyExpectancy: trades.value && trades.value > 0 ? netExpectancy * trades.value : null,
      rewardToRisk: validAvgWin / validAvgLoss,
      breakevenWinRate: (validAvgLoss / (validAvgWin + validAvgLoss)) * 100,
      state,
    };
  }, [inputs]);

  const visibleErrors = useMemo<Partial<Record<FieldKey, string>>>(() => {
    const next: Partial<Record<FieldKey, string>> = {};
    (Object.entries(calculation.errors) as Array<[FieldKey, string | undefined]>).forEach(([key, error]) => {
      if (error && (analysisAttempted || touchedFields[key] || inputs[key].trim())) {
        next[key] = error;
      }
    });
    return next;
  }, [analysisAttempted, calculation.errors, inputs, touchedFields]);

  useEffect(() => {
    trackSeoToolEvent("tool_page_view", PAGE_PATH, TOOL_ID);
  }, []);

  useEffect(() => {
    if (!hasTrackedStart.current && Object.values(inputs).some((value) => value.trim())) {
      hasTrackedStart.current = true;
      trackSeoToolEvent("expectancy_calculator_started", PAGE_PATH, TOOL_ID);
    }
  }, [inputs]);

  useEffect(() => {
    if (!calculation.ready) return;
    trackSeoToolEvent("expectancy_calculator_completed", PAGE_PATH, TOOL_ID);
    if (lastTrackedState.current !== calculation.state) {
      lastTrackedState.current = calculation.state;
      trackSeoToolEvent(`expectancy_result_${calculation.state}` as const, PAGE_PATH, TOOL_ID);
    }
  }, [calculation]);

  useEffect(() => {
    const next = new URLSearchParams(window.location.search);
    const queryKeys: FieldKey[] = ["winRate", "avgWin", "avgLoss", "trades", "fees"];
    queryKeys.forEach((key) => next.delete(key));
    queryKeys.forEach((key) => {
      const value = inputs[key].trim();
      if (value && !calculation.errors[key]) next.set(key, value);
    });
    const current = new URLSearchParams(window.location.search);
    if (next.toString() !== current.toString()) {
      setSearchParams(next, { replace: true });
    }
  }, [calculation.errors, inputs, setSearchParams]);

  const schema = pageStructuredData({ path: PAGE_PATH, title: TITLE, description: DESCRIPTION, breadcrumbs: BREADCRUMBS });

  const setField = (key: FieldKey, value: string) => {
    setInputs((current) => ({ ...current, [key]: value }));
  };
  const markTouched = (key: FieldKey) => {
    setTouchedFields((current) => ({ ...current, [key]: true }));
  };
  const handleAnalyze = () => {
    setAnalysisAttempted(true);
  };
  const ctaDestination = activationAwareCtaDestination(CTA_SOURCE);

  return (
    <MarketingShell>
      <Seo title={TITLE} description={DESCRIPTION} path={PAGE_PATH} canonical={PAGE_PATH} structuredData={schema} />

      <section className="marketing-hero expectancy-hero">
        <div className="marketing-container">
          <Breadcrumbs items={BREADCRUMBS} />
          <div className="expectancy-hero-grid">
          <div>
            <div className="marketing-eyebrow">Free trading tool</div>
            <h1 className="marketing-title">Trading Expectancy Calculator</h1>
            <p className="marketing-subtitle hero-support">
              See the historical expected value per trade based on your win rate, average win and average loss.
            </p>
            <p className="marketing-section-copy">
              Add trades per month and estimated fees to see a broader historical estimate.
            </p>
            <p className="marketing-trust-copy">Free. No signup required.</p>
          </div>
          <CalculatorPanel
            inputs={inputs}
            errors={visibleErrors}
            calculation={calculation}
            onAnalyze={handleAnalyze}
            onBlur={markTouched}
            onChange={setField}
          />
          </div>
        </div>
      </section>

      <section className="marketing-section">
        <div className="marketing-container expectancy-content-grid">
          <article className="seo-story-card expectancy-education-card">
            <div className="marketing-eyebrow">Formula</div>
            <h2>Trading Expectancy Formula</h2>
            <p className="expectancy-formula">Expectancy = (Win Rate x Average Win) - (Loss Rate x Average Loss)</p>
            <p>
              Static example: a 55% win rate with a $250 average win and $150 average loss creates
              (0.55 x 250) - (0.45 x 150), or +$70 per trade before fees.
            </p>
          </article>
          <article className="seo-story-card expectancy-education-card">
            <div className="marketing-eyebrow">Break-even</div>
            <h2>Break-even Win Rate</h2>
            <p className="expectancy-formula">Average Loss / (Average Win + Average Loss)</p>
            <p>
              With a $250 average win and $150 average loss, break-even win rate is 150 / 400 = 37.5%.
              This is the approximate win rate required for zero expectancy before fees, commissions and slippage.
            </p>
          </article>
        </div>
      </section>

      <section className="marketing-section">
        <div className="marketing-container seo-section-grid">
          <article className="seo-story-card">
            <div className="marketing-eyebrow">Sample size</div>
            <h2>How Many Trades Do You Need?</h2>
            <p>
              A strategy can have positive expectancy and still experience losing streaks. A small number of trades may
              produce misleading results, especially in fast futures markets.
            </p>
            <ul>
              <li>Review expectancy by setup, session, direction and instrument.</li>
              <li>Compare NQ, MNQ, ES and MES separately when your data supports it.</li>
              <li>Check time of day, day of week, account and behavioral conditions.</li>
            </ul>
          </article>
          <article className="seo-story-card">
            <div className="marketing-eyebrow">Win rate</div>
            <h2>Expectancy vs Win Rate</h2>
            <p>
              High win rate does not automatically mean positive trading expectancy. Hypothetical example: 80% winners,
              $50 average win and $300 average loss equals 40 - 60, or -$20 per trade.
            </p>
            <ul>
              <li>Small winners can be overwhelmed by occasional large losses.</li>
              <li>Average loss size matters as much as win probability.</li>
              <li>Fees, commissions and slippage can move a result below break-even.</li>
            </ul>
          </article>
          <article className="seo-story-card">
            <div className="marketing-eyebrow">Reward-to-risk</div>
            <h2>Expectancy vs Reward-to-Risk</h2>
            <p>
              A low win-rate strategy can still have positive expectancy when average winners are sufficiently larger
              than average losers. Hypothetical example: 40% winners, $400 average win and $150 average loss equals +$70 per trade.
            </p>
            <ul>
              <li>Reward-to-risk ratio helps explain the shape of expectancy.</li>
              <li>Expectancy still depends on both win rate and loss rate.</li>
              <li>Segmenting actual trades shows whether the pattern is consistent.</li>
            </ul>
          </article>
        </div>
      </section>

      <section className="marketing-section">
        <div className="marketing-container audience-panel seo-link-panel">
          <div>
            <div className="marketing-eyebrow">TradePilot Coach bridge</div>
            <h2 className="marketing-section-title">Want TradePilot Coach to Find What Changes Your Expectancy?</h2>
            <p className="marketing-section-copy">
              Expectancy tells you what your trading looks like overall. Import your completed Topstep or Tradovate trades and let TradePilot Coach
              analyze historical patterns across losses, sessions, instruments and trading behavior.
            </p>
            <Link
              className="marketing-button primary"
              to={ctaDestination}
              onClick={() => trackSeoToolCtaClick(PAGE_PATH, TOOL_ID, CTA_LABEL, ctaDestination)}
            >
              {CTA_LABEL}
            </Link>
            <p className="revenge-bridge-note">CSV import. No broker connection required.</p>
          </div>
          <nav className="audience-grid" aria-label="Related trading expectancy resources">
            <Link className="audience-item seo-related-link" to="/futures-trading-journal">Futures trading journal</Link>
            <Link className="audience-item seo-related-link" to="/ai-trading-journal">AI trading journal</Link>
            <Link className="audience-item seo-related-link" to="/trading-psychology">Trading psychology</Link>
            <Link className="audience-item seo-related-link" to="/overtrading">Overtrading</Link>
            <Link className="audience-item seo-related-link" to="/revenge-trading">Revenge trading</Link>
            <Link className="audience-item seo-related-link" to="/trading-after-losses">Trading after losses</Link>
          </nav>
        </div>
      </section>

      <section className="marketing-section faq-section">
        <div className="marketing-container">
          <header className="marketing-section-header centered">
            <div className="marketing-eyebrow">FAQ</div>
            <h2 className="marketing-section-title">Trading Expectancy Questions</h2>
          </header>
          <div className="faq-grid">
            <article className="faq-card">
              <h3>What is trading expectancy?</h3>
              <p>Trading expectancy is the average expected value per trade based on win rate, average win and average loss.</p>
            </article>
            <article className="faq-card">
              <h3>Does this calculator predict future profit?</h3>
              <p>No. Expectancy describes a statistical average based on the inputs provided and does not guarantee future profitability.</p>
            </article>
          </div>
        </div>
      </section>
    </MarketingShell>
  );
}

function CalculatorPanel({
  inputs,
  errors,
  calculation,
  onAnalyze,
  onBlur,
  onChange,
}: {
  inputs: Record<FieldKey, string>;
  errors: Partial<Record<FieldKey, string>>;
  calculation: CalculatorResult;
  onAnalyze: () => void;
  onBlur: (key: FieldKey) => void;
  onChange: (key: FieldKey, value: string) => void;
}) {
  return (
    <section className="expectancy-calculator-card calculator-suite-card expectancy-suite-card" aria-label="Trading expectancy calculator">
      <div className="expectancy-inputs expectancy-input-grid">
        <CalculatorInput
          id="winRate"
          label="Win rate"
          suffix="%"
          placeholder="55"
          value={inputs.winRate}
          error={errors.winRate}
          onBlur={() => onBlur("winRate")}
          onChange={(value) => onChange("winRate", value)}
        />
        <CalculatorInput
          id="avgWin"
          label="Average winning trade"
          prefix="$"
          placeholder="250"
          value={inputs.avgWin}
          error={errors.avgWin}
          onBlur={() => onBlur("avgWin")}
          onChange={(value) => onChange("avgWin", value)}
        />
        <CalculatorInput
          id="avgLoss"
          label="Average losing trade"
          prefix="$"
          placeholder="150"
          helper="Enter the size of your average losing trade as a positive number."
          value={inputs.avgLoss}
          error={errors.avgLoss}
          onBlur={() => onBlur("avgLoss")}
          onChange={(value) => onChange("avgLoss", value)}
        />
        <CalculatorInput
          id="trades"
          label="Trades per month"
          placeholder="40"
          optional
          value={inputs.trades}
          error={errors.trades}
          onBlur={() => onBlur("trades")}
          onChange={(value) => onChange("trades", value)}
        />
        <CalculatorInput
          id="fees"
          label="Average fees and commissions per trade"
          prefix="$"
          placeholder="4.50"
          optional
          value={inputs.fees}
          error={errors.fees}
          onBlur={() => onBlur("fees")}
          onChange={(value) => onChange("fees", value)}
        />
      </div>
      <div className="calculator-action-row">
        <button type="button" className="revenge-analysis-button" onClick={onAnalyze}>
          Calculate My Expectancy
        </button>
        <p>Results update automatically as valid inputs are entered.</p>
      </div>
      <ResultPanel result={calculation} />
    </section>
  );
}

function CalculatorInput({
  id,
  label,
  value,
  placeholder,
  prefix,
  suffix,
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
  suffix?: string;
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
        {suffix && <strong aria-hidden="true">{suffix}</strong>}
      </div>
      {helper && <small id={`${id}-helper`}>{helper}</small>}
      {error && <small id={`${id}-error`} className="expectancy-error" role="alert">{error}</small>}
    </label>
  );
}

function ResultPanel({ result }: { result: CalculatorResult }) {
  if (!result.ready) {
    return (
      <aside className="expectancy-result-card empty calculator-empty-state" aria-live="polite">
        <div className="report-kicker">Expected value per trade</div>
        <strong>Calculate Your Trading Expectancy</strong>
        <ol className="revenge-empty-steps">
          <li>Enter your historical win rate.</li>
          <li>Add your average winning trade.</li>
          <li>Add your average losing trade.</li>
          <li>Calculate the expected value per trade.</li>
        </ol>
        <p>Results are based only on the values you enter and do not predict future profitability.</p>
      </aside>
    );
  }

  const isNetDifferent = result.fees > 0;
  return (
    <aside className={`expectancy-result-card ${result.state}`} aria-live="polite">
      <div className="report-kicker">{isNetDifferent ? "Net expectancy" : "Expectancy result"}</div>
      <div className="calculator-result-highlight">
        <span>Expected value / trade</span>
        <strong className="expectancy-primary-result">{formatMoneyWithCents(result.netExpectancy)}</strong>
      </div>
      {isNetDifferent && (
        <p className="expectancy-gross-result">Gross Expectancy: {formatMoney(result.grossExpectancy)} before fees</p>
      )}
      <dl className="expectancy-result-grid">
        <div><dt>Win probability</dt><dd>{formatPct(result.winRate)}</dd></div>
        <div><dt>Loss probability</dt><dd>{formatPct(result.lossRate)}</dd></div>
        <div><dt>Average win</dt><dd>{formatMoney(result.avgWin)}</dd></div>
        <div><dt>Average loss</dt><dd>{formatMoney(result.avgLoss)}</dd></div>
        <div><dt>Reward-to-Risk Ratio</dt><dd>{result.rewardToRisk.toFixed(2)} : 1</dd></div>
        <div><dt>Break-even Win Rate</dt><dd>{formatPct(result.breakevenWinRate)}</dd></div>
      </dl>
      {result.monthlyExpectancy !== null && (
        <div className="expectancy-monthly">
          <span>Estimated mathematical expectancy</span>
          <strong>{formatMoney(result.monthlyExpectancy, { maximumFractionDigits: 0 })}/month</strong>
        </div>
      )}
      {isNetDifferent && (
        <div className="expectancy-monthly calculator-fees-impact">
          <span>Fees impact</span>
          <strong>-{formatCurrencyWithCents(result.fees)}/trade</strong>
        </div>
      )}
      <p className="expectancy-interpretation">
        {result.state === "positive" &&
          `Your inputs produce a positive mathematical expectancy. Based on these assumptions, each trade is worth approximately ${formatMoneyWithCents(result.netExpectancy)} on average over a sufficiently large sample.`}
        {result.state === "negative" &&
          `Your inputs produce a negative mathematical expectancy. Based on these assumptions, each trade loses approximately ${formatCurrencyWithCents(result.netExpectancy)} on average over a sufficiently large sample.`}
        {result.state === "breakeven" &&
          "Your inputs are near breakeven before additional slippage or changing market conditions."}
      </p>
      <p className="expectancy-disclaimer">
        Expectancy describes a statistical average based on the inputs provided. It does not predict the outcome of any individual trade and does not guarantee future profitability.
      </p>
    </aside>
  );
}
