import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import Seo from "../../components/Seo";
import { homepageStructuredData } from "../../seo/structuredData";
import { getFeatureAvailability } from "../../api/featureAvailabilityApi";
import { isFeatureEnabled, type FeatureAvailability } from "../../config/featureFlags";
import { getEnabledProMarketingBenefits } from "../../config/planBenefits";
import { marketingPricing } from "../../config/marketingPricing";
import { trackSeoCtaClick, trackSeoLandingPageView } from "../../utils/seoAttribution";
import "./HomePage.css";
import { MarketingShell } from "./MarketingLayout";
import { sampleAccount } from "./sampleAccount";

const disabledV2Availability: FeatureAvailability = {
  v2FeaturesEnabled: false,
  v3FeaturesEnabled: false,
  features: {
    playbook: false,
    tradingDna: false,
    journey: false,
    tradingMemory: false,
    tradeReplay: false,
    coach: true,
  },
};

const v1HeroBadges = ["CSV Import", "AI Leak Detector", "Daily Coach", "Performance Analytics"];
const HOME_TITLE = "TradePilot Coach | AI Trading Journal & Execution Intelligence";
const HOME_DESCRIPTION =
  "TradePilot Coach is an AI-powered trading journal and execution intelligence platform for futures traders. Analyze trades, uncover behavioral patterns, review execution, and turn trading history into actionable coaching insights.";

const v2HeroBadges = ["Trading Memory", "Similar Trade Retrieval", "Recurring Patterns", "Evidence-Based Metrics"];

const workflowSteps = [
  {
    title: "Import",
    copy: "Bring in supported futures trade history by CSV.",
  },
  {
    title: "Verify",
    copy: "Confirm your imported trade count and net P&L against your platform.",
  },
  {
    title: "Detect",
    copy: "Find recurring execution patterns with evidence from your trades.",
  },
  {
    title: "Explain",
    copy: "Compare baseline and triggered behavior so the finding is understandable.",
  },
  {
    title: "Improve",
    copy: "Leave each review with one clear next-session rule.",
  },
];

const leakFeatures = [
  "Find the behavior costing you the most",
  "See when the leak appears in your sessions",
  "Leave with one focused rule for next time",
];

const comparisonRows = [
  ["Trade history", "Logs trades", "Analyzes trades"],
  ["Performance", "Shows P&L", "Explains performance patterns"],
  ["Review", "Reviews trades individually", "Connects patterns across sessions"],
  ["Next step", "Records history", "Creates an improvement focus"],
];

const audienceItems = [
  "Multi-account journaling",
  "Futures-focused CSV workflows",
  "Session-level analysis",
  "Execution review",
];

const faqs = [
  {
    question: "What CSV formats are supported?",
    answer:
      "TradePilot Coach currently supports TradePilot-compatible Tradovate trade exports and Topstep order-history CSV exports for futures trade imports.",
  },
  {
    question: "Does TradePilot Coach provide trade signals?",
    answer:
      "No. TradePilot Coach analyzes historical execution. It does not provide trade calls, market predictions, or financial advice.",
  },
  {
    question: "What happens when there is not enough data?",
    answer:
      "TradePilot Coach shows limited-data states instead of forcing a confident answer. No-trade days and weeks skip AI generation.",
  },
  {
    question: "How is TradePilot different from uploading a CSV into ChatGPT?",
    answer:
      "TradePilot Coach combines structured trade imports, repeatable trading metrics, behavior detection, and account-level historical context. AI helps explain findings and suggest practical review steps. Where evidence is limited, TradePilot Coach should say so rather than present speculation as certainty.",
  },
  {
    question: "How many trades do I need for useful analysis?",
    answer:
      "Basic performance statistics can be calculated with relatively few trades. More reliable behavioral findings require sufficient qualifying observations, which vary by pattern. TradePilot Coach distinguishes stronger evidence from limited data rather than promising reliable conclusions from every sample.",
  },
  {
    question: "Is my trading data secure?",
    answer:
      "Your imported trade history is scoped to your account. TradePilot Coach does not need your broker password for the supported CSV workflow, does not place trades, and does not sell your trading data.",
  },
];

export default function HomePage() {
  const [availability, setAvailability] = useState<FeatureAvailability>(disabledV2Availability);

  useEffect(() => {
    trackSeoLandingPageView("/");
    let mounted = true;

    getFeatureAvailability()
      .then((response) => {
        if (mounted) {
          setAvailability(response);
        }
      })
      .catch(() => {
        if (mounted) {
          setAvailability(disabledV2Availability);
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  const showTradingMemory = useMemo(
    () => isFeatureEnabled("TRADING_MEMORY", availability),
    [availability]
  );

  const showV2Features = useMemo(
    () =>
      isFeatureEnabled("TRADING_MEMORY", availability) ||
      isFeatureEnabled("TRADING_DNA", availability) ||
      isFeatureEnabled("JOURNEY", availability) ||
      isFeatureEnabled("PLAYBOOK", availability),
    [availability]
  );

  const proBenefits = showV2Features ? getEnabledProMarketingBenefits(availability) : marketingPricing.pro.benefits;

  return (
    <MarketingShell>
      <Seo
        title={HOME_TITLE}
        description={HOME_DESCRIPTION}
        structuredData={homepageStructuredData({ title: HOME_TITLE, description: HOME_DESCRIPTION })}
      />

      <section className="marketing-hero">
        <div className="marketing-container marketing-hero-grid">
          <div>
            <div className="marketing-eyebrow">AI execution intelligence for futures traders</div>
            <h1 className="marketing-title">
              Your P&amp;L tells you what happened. <span className="brand-accent">TradePilot Coach tells you why.</span>
            </h1>
            <p className="marketing-subtitle hero-support">
              Import your futures trading history. Discover recurring execution mistakes, see the evidence behind them, and get one actionable rule for your next session.
            </p>
            <p className="hero-retrospective-copy">Review your completed trades. Understand recurring execution mistakes. Improve your next session. Post-session analysis. No live trade signals or predictions.</p>
            <div className="marketing-hero-actions">
              <Link
                className="marketing-button primary"
                to="/register"
                onClick={() => trackSeoCtaClick("/", "Try TradePilot Free", "/register")}
              >
                Try TradePilot Free
              </Link>
              <Link
                className="marketing-button ghost"
                to="/demo"
                onClick={() => trackSeoCtaClick("/", "See Sample Demo", "/demo")}
              >
                See Sample Demo
              </Link>
            </div>
            <p className="hero-demo-copy">Explore a sample review — no signup or CSV required.</p>
            <div className="import-compatibility-strip" aria-label="Supported CSV imports">
              <strong>CSV imports:</strong>
              <span>Tradovate and Topstep</span>
            </div>
            <p className="marketing-trust-copy">
              {showTradingMemory
                ? <>AI execution intelligence and coaching for futures traders. Import <Link className="marketing-inline-link" to="/topstep-trading-journal">Topstep</Link> and <Link className="marketing-inline-link" to="/tradovate-trading-journal">Tradovate</Link> trade history by CSV.</>
                : <>Built for <Link className="marketing-inline-link" to="/futures-trading-journal">futures</Link> and <Link className="marketing-inline-link" to="/prop-firm-trading-journal">prop-firm traders</Link>. No signals. No trade calls. No promises of profit.</>}
            </p>
            <div className="marketing-proof-row compact" aria-label="TradePilot Coach product highlights">
              {(showTradingMemory ? v2HeroBadges : v1HeroBadges).map((badge) => (
                <div className="marketing-proof-item" key={badge}>{badge}</div>
              ))}
            </div>
          </div>
          {showTradingMemory ? <TradingMemoryHeroPreview /> : <LeakReportMockup />}
        </div>
      </section>

      <section className="marketing-section sample-proof-section" aria-labelledby="sample-proof-title">
        <div className="marketing-container">
          <header className="marketing-section-header centered">
            <div className="marketing-eyebrow">Sample product proof</div>
            <h2 className="marketing-section-title" id="sample-proof-title">From trading history to one clear behavioral insight</h2>
            <p className="marketing-section-copy">The sample account is illustrative only. It shows the shape of a review, not a trading recommendation.</p>
          </header>
          <div className="proof-sequence" aria-label="Sample analysis sequence">
            <article><span>01 — Behavior</span><strong>{sampleAccount.finding.pattern}</strong></article>
            <article><span>02 — Evidence</span><strong>{sampleAccount.totalTrades} trades analyzed. {sampleAccount.qualifyingOccurrences} qualifying occurrences. {sampleAccount.finding.evidenceLabel}.</strong></article>
            <article><span>03 — Observed impact</span><strong>{sampleAccount.finding.triggeredAverageR}R after the trigger versus {sampleAccount.finding.baselineAverageR}R baseline: {sampleAccount.finding.deteriorationR}R worse on average.</strong></article>
            <article><span>04 — Next-session focus</span><strong>{sampleAccount.finding.coachRule}</strong></article>
          </div>
          <p className="marketing-trust-copy sample-trust-note">Illustrative sample data. The R-based comparison comes from the displayed sample; TradePilot detects the pattern first, then AI helps explain it.</p>
        </div>
      </section>

      <section className="marketing-section data-trust-section">
        <div className="marketing-container data-trust-layout">
          <div><div className="marketing-eyebrow">Data trust</div><h2 className="marketing-section-title">Your numbers should match first.</h2><p className="marketing-section-copy">After an import, verify the trade count and net P&amp;L against your platform before you rely on the analysis.</p></div>
          <div className="data-trust-card"><strong>32 trades imported</strong><strong>Net P&amp;L: +$664.30</strong><span>Does this match your trading platform?</span><div><button type="button">Yes, looks correct</button><button type="button">Something looks wrong</button></div></div>
        </div>
      </section>

      <section className="marketing-section differentiation-section" id="features">
        <div className="marketing-container">
          <div className="journal-difference-layout">
            <header className="marketing-section-header">
              <div className="marketing-eyebrow">Evidence-first review</div>
              <h2 className="marketing-section-title">TradePilot detects first. AI explains second.</h2>
              <p className="marketing-section-copy">Structured analysis looks for recurring execution behavior, shows the supporting comparison and its limitations, then turns supported findings into a next-session review focus.</p>
            </header>
            <div className="journal-difference-points" aria-label="TradePilot Coach difference">
              <p><strong>Detect</strong> recurring execution behavior in completed trades.</p>
              <p><strong>Show the evidence</strong> with relevant comparisons, counts, and limitations.</p>
              <p><strong>Coach the next session</strong> with one actionable review rule.</p>
            </div>
          </div>
        </div>
      </section>

      <section className="marketing-section" id="how-it-works">
        <div className="marketing-container">
          <header className="marketing-section-header centered">
            <div className="marketing-eyebrow">How it works</div>
            <h2 className="marketing-section-title">A shorter path from history to a better next session.</h2>
            <p className="marketing-section-copy">
              A short review loop built from your own trading history.
            </p>
          </header>
          <div className="journey-steps" aria-label="TradePilot Coach workflow">
            {workflowSteps.map((step, index) => (
              <article className="journey-step" key={step.title}>
                <span>{index + 1}</span>
                <h3>{step.title}</h3>
                <p>{step.copy}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="marketing-section flagship-section" id="leak-report">
        <div className="marketing-container flagship-grid">
          <div>
            <div className="marketing-eyebrow">AI Leak Detector</div>
            <h2 className="marketing-section-title">Find the Habits Quietly Draining Your Account</h2>
            <p className="marketing-section-copy">
              Turn a costly pattern into a clear execution decision before it repeats.
            </p>
            <div className="feature-bullet-grid">
              {leakFeatures.map((feature) => (
                <div className="feature-bullet" key={feature}>{feature}</div>
              ))}
            </div>
            <Link
              className="marketing-button primary flagship-cta"
              to="/register"
              onClick={() => trackSeoCtaClick("/", "Try TradePilot Free", "/register")}
            >
              Try TradePilot Free
            </Link>
          </div>
          <div className="flagship-panel flagship-panel-emphasis">
            <LeakReportMockup compact />
          </div>
        </div>
      </section>

      <section className="marketing-section" id="ai-coach">
        <div className="marketing-container story-grid">
          <article className="story-panel">
            <div className="marketing-eyebrow">Daily and Weekly Coach</div>
            <h2 className="marketing-section-title">Coaching based on your trades, not generic advice.</h2>
            <p className="marketing-section-copy">
              Your reviews connect recurring behavior across sessions and turn it into one specific, actionable focus.
            </p>
            <div className="story-list coach-outcomes">
              <span><strong>Pattern</strong> What repeated in the reviewed trades.</span>
              <span><strong>Context</strong> The conditions that made it more likely.</span>
              <span><strong>Rule</strong> One action to carry into the next session.</span>
            </div>
            <p className="coach-disclaimer">
              TradePilot Coach analyzes historical execution. It does not provide trade signals or predict market direction.
            </p>
            <Link
              className="marketing-button primary story-cta"
              to="/register"
              onClick={() => trackSeoCtaClick("/", "Try TradePilot Free", "/register")}
            >
              Try TradePilot Free
            </Link>
          </article>
          <CoachOutcomeMockup />
        </div>
      </section>

      <section className="marketing-section">
        <div className="marketing-container">
          <header className="marketing-section-header centered">
            <div className="marketing-eyebrow">Journal comparison</div>
            <h2 className="marketing-section-title">Why traders need more than a journal</h2>
          </header>
          <div className="comparison-table" role="table" aria-label="Traditional Journal versus TradePilot Coach">
            <div className="comparison-row comparison-header" role="row">
              <div role="columnheader">Feature</div>
              <div role="columnheader">Traditional Journal</div>
              <div role="columnheader">TradePilot Coach</div>
            </div>
            {comparisonRows.map(([feature, traditional, tradepilot]) => (
              <div className="comparison-row" role="row" key={feature}>
                <div role="cell" data-label="Feature">{feature}</div>
                <div role="cell" data-label="Traditional Journal">
                  <StatusPill tone="neutral" label={traditional} />
                </div>
                <div role="cell" data-label="TradePilot Coach">
                  <StatusPill tone="included" label={tradepilot} />
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="marketing-section audience-section">
        <div className="marketing-container audience-panel">
          <div>
            <div className="marketing-eyebrow">Who it's for</div>
            <h2 className="marketing-section-title">Built for futures traders who want a more useful review loop.</h2>
            <p className="marketing-section-copy">
              Use your own CSV trade history to review execution, measure discipline, and improve decisions without handing over a broker password.
            </p>
          </div>
          <div className="audience-grid">
            {audienceItems.map((item) => (
              <div className="audience-item" key={item}>{item}</div>
            ))}
          </div>
        </div>
      </section>

      <section className="marketing-section founder-section">
        <div className="marketing-container">
          <div className="founder-statement">
            <div className="marketing-eyebrow">Why it exists</div>
            <h2 className="marketing-section-title">Built from problems futures traders actually face.</h2>
            <p className="marketing-section-copy">The goal is not another dashboard. It is a focused way to turn historical execution into evidence, context, and a next-session rule.</p>
          </div>
        </div>
      </section>

      <section className="marketing-section" id="pricing">
        <div className="marketing-container">
          <header className="marketing-section-header pricing-header">
            <div>
              <div className="marketing-eyebrow">Pricing</div>
              <h2 className="marketing-section-title">Turn your trading history into your next coaching insight.</h2>
              <p className="marketing-section-copy">Start with Free, then subscribe to Pro when you are ready for deeper review and coaching.</p>
            </div>
          </header>
          <div className="pricing-grid">
            <article className="pricing-card">
              <div className="pricing-card-top">
                <h3>{marketingPricing.free.name}</h3>
              </div>
              <div className="pricing-price">{marketingPricing.free.price}</div>
              <p className="marketing-section-copy">{marketingPricing.free.description}</p>
              <ul>
                {marketingPricing.free.benefits.map((benefit) => (
                  <li key={benefit}>{benefit}</li>
                ))}
              </ul>
              <Link className="marketing-button ghost pricing-cta" to="/register">Start Free</Link>
            </article>
            <article className="pricing-card pro">
              <div className="pricing-card-top">
                <h3>{marketingPricing.pro.name}</h3>
                <span className="popular-badge">Available now</span>
              </div>
              <div className="pricing-price">{marketingPricing.pro.price}</div>
              <p className="pricing-annual-price">or {marketingPricing.pro.yearlyPrice} <span>Save {marketingPricing.pro.yearlySavings}</span></p>
              <p className="marketing-section-copy">{marketingPricing.pro.description}</p>
              <ul>
                {proBenefits.map((benefit) => (
                  <li key={benefit}>
                    {benefit.toLowerCase().includes("monthly ai") ? (
                      <span className="credit-highlight">{benefit}</span>
                    ) : (
                      benefit
                    )}
                  </li>
                ))}
              </ul>
              <Link className="marketing-button primary pricing-cta" to="/register">Start Pro</Link>
            </article>
          </div>
          <p className="pricing-note">
            AI usage is subject to the monthly credit allowance. Cached AI results remain viewable without consuming credits.
            Credit packs are also available for additional AI usage.
          </p>
        </div>
      </section>

      <section className="marketing-section faq-section" id="faq">
        <div className="marketing-container">
          <header className="marketing-section-header centered">
            <div className="marketing-eyebrow">FAQ</div>
            <h2 className="marketing-section-title">Clear Expectations Before You Import</h2>
          </header>
          <div className="faq-grid">
            {faqs.map((faq) => (
              <article className="faq-card" key={faq.question}>
                <h3>{faq.question}</h3>
                <p>{faq.answer}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="marketing-section final-cta-section">
        <div className="marketing-container">
          <div className="final-cta">
            <h2 className="marketing-section-title">See what TradePilot Coach finds in your trading.</h2>
            <p className="marketing-section-copy">Explore the sample review first, or create a free account to import your supported trade history.</p>
            <div className="final-cta-actions">
              <Link className="marketing-button primary" to="/register" onClick={() => trackSeoCtaClick("/", "Try TradePilot Free", "/register")}>Try TradePilot Free</Link>
              <Link className="marketing-button ghost" to="/demo" onClick={() => trackSeoCtaClick("/", "See Sample Demo", "/demo")}>See Sample Demo</Link>
            </div>
            <p className="final-cta-note">No broker password required.</p>
          </div>
        </div>
      </section>
    </MarketingShell>
  );
}

function TradingMemoryHeroPreview() {
  return (
    <div className="trading-memory-preview" aria-label="Trading Memory product preview">
      <div className="memory-preview-top">
        <div>
          <div className="report-kicker">Trading Memory</div>
          <h2>Have you made this trade before?</h2>
          <p className="memory-preview-result">Yes — TradePilot found a recurring pattern.</p>
        </div>
        <span className="report-badge">5 similar trades</span>
      </div>
      <div className="memory-insight-block">
        <span>Recurring pattern</span>
        <strong>You exited winners early in 4 of 6 comparable trades.</strong>
        <p>Evidence from your reviewed trade history, with supporting trades linked for review.</p>
      </div>
      <div className="memory-metric-grid">
        <div>
          <span>Average Result</span>
          <strong>+0.54R</strong>
        </div>
        <div>
          <span>Most Common Emotion</span>
          <strong>Fear of giving back profit</strong>
        </div>
      </div>
      <div className="similar-trade-list">
        {[
          ["NQ", "LONG", "+0.50R", "91% semantic match"],
          ["MNQ", "LONG", "+0.62R", "87% semantic match"],
          ["ES", "LONG", "-0.30R", "82% semantic match"],
        ].map(([symbol, direction, result, match]) => (
          <div className="similar-trade-row" key={`${symbol}-${result}`}>
            <strong>{symbol}</strong>
            <span>{direction}</span>
            <span>{result}</span>
            <em>{match}</em>
          </div>
        ))}
      </div>
      <button className="memory-preview-button" type="button">View supporting trades</button>
    </div>
  );
}

function StatusPill({ tone, label }: { tone: "included" | "neutral"; label: string }) {
  const isIncluded = tone === "included";
  return (
    <span className={`status-pill ${isIncluded ? "included" : "neutral"}`}>
      <span aria-hidden="true">{isIncluded ? "✓" : "•"}</span>
      {label}
    </span>
  );
}

function LeakReportMockup({ compact = false }: { compact?: boolean }) {
  return (
    <div className={`leak-report-mockup ${compact ? "compact" : ""}`} aria-label="AI Leak Detector report preview">
      <div className="leak-report-header">
        <div>
          <div className="report-kicker">Execution Leak Report</div>
          <h2>{compact ? "Overtrading follows two consecutive losses." : "You lose the most after continuing to trade following two consecutive losses."}</h2>
        </div>
        <span className="report-badge">{sampleAccount.finding.evidenceLabel}</span>
      </div>
      <div className="report-stats">
        <div>
          <span>Average deterioration</span>
          <strong className="loss">{sampleAccount.finding.deteriorationR}R worse</strong>
        </div>
        {!compact && (
          <div>
            <span>Trades Analyzed</span>
            <strong>{sampleAccount.totalTrades}</strong>
          </div>
        )}
        <div>
          <span>Evidence strength</span>
          <strong>Weak</strong>
          <div className="confidence-track"><span style={{ width: "36%" }} /></div>
        </div>
      </div>
      <div className="recovery-plan">
        <div className="report-kicker">Focused Improvement Plan</div>
        <ol>
          <li>{sampleAccount.finding.coachRule}</li>
          <li>Set a hard trade cap before the session starts.</li>
          {!compact && <li>Review related trades and write one rule.</li>}
        </ol>
      </div>
      <p className="leak-demo-disclaimer">Illustrative sample only. Historical patterns and estimated impact do not predict future results.</p>
    </div>
  );
}

function CoachOutcomeMockup() {
  return (
    <div className="daily-coach-mockup">
      <div className="report-kicker">Coaching output</div>
      <h3>Next-Session Focus</h3>
      <div className="coach-metric mission">
        <span>Detected pattern</span>
        <strong>Trading continued after two consecutive losses.</strong>
      </div>
      <div className="coach-metric">
        <span>Relevant context</span>
        <strong>The pattern was concentrated in the late morning review window.</strong>
      </div>
      <div className="coach-metric">
        <span>Next-session rule</span>
        <strong>Pause for 20 minutes after two consecutive losses.</strong>
      </div>
    </div>
  );
}
