import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import Seo from "../../components/Seo";
import { getFeatureAvailability } from "../../api/featureAvailabilityApi";
import { isFeatureEnabled, type FeatureAvailability } from "../../config/featureFlags";
import { getEnabledProMarketingBenefits } from "../../config/planBenefits";
import { marketingPricing } from "../../config/marketingPricing";
import { trackSeoCtaClick, trackSeoLandingPageView } from "../../utils/seoAttribution";
import "./HomePage.css";
import { MarketingShell } from "./MarketingLayout";

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

const v2HeroBadges = ["Trading Memory", "Similar Trade Retrieval", "Recurring Patterns", "Evidence-Based Metrics"];

const problemCards = [
  {
    title: "Incomplete Trade Records",
    copy: "Manual journaling is slow, inconsistent, and easy to abandon.",
  },
  {
    title: "Hidden Execution Leaks",
    copy: "Overtrading, poor timing, directional bias, and oversized losses are difficult to spot one trade at a time.",
  },
  {
    title: "No Clear Next Step",
    copy: "Traders often collect data without knowing which behavior to correct next.",
  },
];

const memoryTransitionCards = [
  {
    title: "Remember",
    copy: "Turn reviewed trades into long-term trading memory.",
  },
  {
    title: "Compare",
    copy: "Find historically similar setups, decisions, and emotional states.",
  },
  {
    title: "Improve",
    copy: "Measure recurring behaviors using evidence from your own journal.",
  },
];

const workflowSteps = [
  {
    title: "Import or record your trades",
    copy: "TradePilot organizes your execution data, notes, and outcomes.",
  },
  {
    title: "Review the trade",
    copy: "Capture the setup, entry, exit, emotion, and decision quality.",
  },
  {
    title: "Build trading memory",
    copy: "TradePilot securely indexes the reviewed trade for future comparisons.",
  },
  {
    title: "Discover recurring patterns",
    copy: "New reviews surface similar past trades and evidence-backed behavioral insights.",
  },
];

const leakFeatures = [
  "Detects costly execution habits",
  "Calculates money impact from real trades",
  "Assigns confidence and data-quality signals",
  "Links related trades for review",
  "Creates a focused improvement plan",
  "Keeps cached reviews viewable without extra credits",
];

const tradingMemoryCapabilities = [
  {
    title: "Similar Trade Retrieval",
    copy: "Find past trades with similar setups, decisions, mistakes, and emotional context.",
  },
  {
    title: "Recurring Pattern Detection",
    copy: "See when the same execution leak appears repeatedly across comparable trades.",
  },
  {
    title: "Evidence-Based Metrics",
    copy: "Review actual R-multiples, outcomes, and supporting trades behind every pattern.",
  },
  {
    title: "Personalized Memory Insight",
    copy: "Receive concise coaching based on your own reviewed history, not generic trading advice.",
  },
];

const v1Features = [
  {
    title: "Review",
    copy: "AI Trade Review and Session Review help you understand individual trades and complete sessions.",
  },
  {
    title: "Coach",
    copy: "Daily Coach and Weekly Coach turn journal data into improvement guidance.",
  },
  {
    title: "Track",
    copy: "CSV import, multi-account tracking, calendar, analytics, and Performance Timeline show progress over time.",
  },
];

const comparisonRows = [
  ["CSV trade import", "Manual or limited", "Included"],
  ["P&L tracking", "Basic", "Included"],
  ["Win rate", "Basic", "Included"],
  ["Trading calendar", "Basic or manual", "Included"],
  ["Execution analytics", "Limited", "Included"],
  ["Session review", "Not typically included", "Included"],
  ["Leak detection", "Not typically included", "Included"],
  ["Daily coaching", "Not typically included", "Included"],
  ["Weekly coaching", "Not typically included", "Included"],
  ["Actionable improvement plan", "Manual", "Included"],
];

const audienceItems = [
  "Prop firm traders",
  "Funded account traders",
  "Futures day traders",
  "Traders building consistency",
];

const faqs = [
  {
    question: "What CSV formats are supported?",
    answer:
      "TradePilot currently supports TradePilot-compatible Tradovate trade exports and Topstep order-history CSV exports for futures trade imports.",
  },
  {
    question: "Does TradePilot provide trade signals?",
    answer:
      "No. TradePilot analyzes historical execution. It does not provide trade calls, market predictions, or financial advice.",
  },
  {
    question: "What does the AI Coach analyze?",
    answer:
      "It reviews your trade history, selected dates, P&L, direction, setup notes, confidence, emotion, and available metadata.",
  },
  {
    question: "What happens when there is not enough data?",
    answer:
      "TradePilot shows limited-data states instead of forcing a confident answer. No-trade days and weeks skip AI generation.",
  },
  {
    question: "Will TradePilot make me profitable?",
    answer:
      "No. It helps you review behavior and build a better process. Trading remains risky, and outcomes are never guaranteed.",
  },
  {
    question: "What is included in Pro?",
    answer:
      "Pro includes Session Review, Leak Detector, Daily Coach, Weekly Coach, full current analytics, and a monthly AI credit allowance.",
  },
  {
    question: "Is my trading data secure?",
    answer:
      "TradePilot uses authenticated APIs, account ownership checks, and role-based admin access to keep user data scoped correctly.",
  },
  {
    question: "Who is TradePilot built for?",
    answer:
      "It is designed for futures and prop firm traders who want to review execution, measure discipline, and improve decisions using their own trade history.",
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
        title="TradePilot - AI Trading Journal for Futures & Prop Firm Traders"
        description="Analyze your futures trading, uncover revenge trading, overtrading and costly behavioral patterns, and get AI-powered coaching. Built for futures and prop-firm traders."
        structuredData={{
          "@context": "https://schema.org",
          "@graph": [
            {
              "@type": "Organization",
              name: "TradePilot",
              url: "https://www.tradepilotcoach.com",
              logo: "https://www.tradepilotcoach.com/assets/tradepilot-logo-homepage.png",
            },
            {
              "@type": "SoftwareApplication",
              name: "TradePilot",
              applicationCategory: "FinanceApplication",
              operatingSystem: "Web",
              url: "https://www.tradepilotcoach.com",
              description: "AI trading journal and behavioral trading coach for futures and prop-firm traders.",
              offers: {
                "@type": "Offer",
                name: "Free",
                price: "0",
                priceCurrency: "USD",
              },
            },
          ],
        }}
      />

      <section className="marketing-hero">
        <div className="marketing-container marketing-hero-grid">
          <div>
            <div className="marketing-eyebrow">AI-powered trading journal</div>
            <h1 className="marketing-title">
              {showTradingMemory
                ? "Your trading journal should remember more than your P&L."
                : "Know What Is Costing You Money in Your Trading"}
            </h1>
            <p className="marketing-subtitle hero-support">
              {showTradingMemory
                ? "TradePilot reviews your trades, finds similar decisions from your history, and reveals the recurring habits shaping your performance."
                : "Import your futures trades, review your execution metrics, uncover costly habits, and get clear daily and weekly coaching based on your actual trading data."}
            </p>
            <div className="marketing-hero-actions">
              <Link
                className="marketing-button primary"
                to="/register"
                onClick={() => trackSeoCtaClick("/", "Start Journaling Free", "/register")}
              >
                Start Journaling Free
              </Link>
              <a className="marketing-button ghost" href="#how-it-works">See How It Works</a>
            </div>
            <p className="marketing-trust-copy">
              {showTradingMemory
                ? "AI trading journal and behavioral coach built for futures and prop-firm traders. Import Topstep and Tradovate trade history by CSV."
                : "Built for futures and prop-firm traders. Import Topstep and Tradovate trade history by CSV. No signals. No trade calls. No promises of profit."}
            </p>
            <div className="marketing-proof-row compact" aria-label="TradePilot product highlights">
              {(showTradingMemory ? v2HeroBadges : v1HeroBadges).map((badge) => (
                <div className="marketing-proof-item" key={badge}>{badge}</div>
              ))}
            </div>
          </div>
          {showTradingMemory ? <TradingMemoryHeroPreview /> : <LeakReportMockup />}
        </div>
      </section>

      {showTradingMemory && (
        <section className="marketing-section memory-transition-section">
          <div className="marketing-container">
            <header className="marketing-section-header centered">
              <div className="marketing-eyebrow">Personal pattern recognition</div>
              <h2 className="marketing-section-title">
                A journal records what happened. TradePilot helps you understand why it keeps happening.
              </h2>
              <p className="marketing-section-copy">
                Individual trade reviews are useful. Real improvement comes from recognizing the decisions,
                emotions, and execution mistakes that repeat across dozens of trades.
              </p>
            </header>
            <div className="marketing-card-grid three">
              {memoryTransitionCards.map((card, index) => (
                <article className="marketing-card memory-step-card" key={card.title}>
                  <div className="marketing-card-icon">{index + 1}</div>
                  <h3>{card.title}</h3>
                  <p>{card.copy}</p>
                </article>
              ))}
            </div>
          </div>
        </section>
      )}

      <section className="marketing-section">
        <div className="marketing-container">
          <header className="marketing-section-header centered">
            <div className="marketing-eyebrow">The behavior gap</div>
            <h2 className="marketing-section-title">Your P&amp;L Shows the Result. It Does Not Explain the Behavior.</h2>
          </header>
          <div className="marketing-card-grid three">
            {problemCards.map((card, index) => (
              <article className="marketing-card problem-card" key={card.title}>
                <div className="marketing-card-icon">{index + 1}</div>
                <h3>{card.title}</h3>
                <p>{card.copy}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="marketing-section" id="how-it-works">
        <div className="marketing-container">
          <header className="marketing-section-header centered">
            <div className="marketing-eyebrow">How it works</div>
            <h2 className="marketing-section-title">From CSV Import to Actionable Coaching</h2>
            <p className="marketing-section-copy">
              TradePilot turns imported trade history into a short review loop that is easy to repeat after each session.
            </p>
          </header>
          <div className="workflow-flow" aria-label="TradePilot workflow">
            <span>Import</span>
            <span>Review</span>
            <span>Memory</span>
            <span>Patterns</span>
          </div>
          <div className="steps-grid">
            {workflowSteps.map((step, index) => (
              <article className="marketing-card step-card" key={step.title}>
                <div className="marketing-card-icon">{index + 1}</div>
                <h3>{step.title}</h3>
                <p>{step.copy}</p>
                {step.title.startsWith("Import") && (
                  <p className="step-note">
                    Currently supports TradePilot-compatible Tradovate and Topstep CSV exports.
                  </p>
                )}
              </article>
            ))}
          </div>
        </div>
      </section>

      {showTradingMemory && (
        <section className="marketing-section trading-memory-section" id="trading-memory">
          <div className="marketing-container trading-memory-grid">
            <div>
              <div className="marketing-eyebrow">Private beta</div>
              <h2 className="marketing-section-title">TradePilot remembers how you trade.</h2>
              <p className="marketing-section-copy">
                Every reviewed trade becomes part of your private trading memory. When you analyze a new trade,
                TradePilot finds similar moments from your history and shows what you did, how you felt, and what
                happened next.
              </p>
              <div className="memory-capability-grid">
                {tradingMemoryCapabilities.map((capability) => (
                  <article className="memory-capability-card" key={capability.title}>
                    <h3>{capability.title}</h3>
                    <p>{capability.copy}</p>
                  </article>
                ))}
              </div>
            </div>
            <TradingMemoryInsightMockup />
          </div>
        </section>
      )}

      <section className="marketing-section flagship-section" id="leak-report">
        <div className="marketing-container flagship-grid">
          <div>
            <div className="marketing-eyebrow">AI Leak Detector</div>
            <h2 className="marketing-section-title">Find the Habits Quietly Draining Your Account</h2>
            <p className="marketing-section-copy">
              Spot sample-based patterns like overtrading, weak time windows, directional imbalance, oversized losses,
              and repeated mistakes after a losing trade.
            </p>
            <div className="feature-bullet-grid">
              {leakFeatures.map((feature) => (
                <div className="feature-bullet" key={feature}>{feature}</div>
              ))}
            </div>
            <Link
              className="marketing-button primary flagship-cta"
              to="/register"
              onClick={() => trackSeoCtaClick("/", "Find My Biggest Leak", "/register")}
            >
              Find My Biggest Leak
            </Link>
          </div>
          <div className="flagship-panel">
            <LeakReportMockup compact />
          </div>
        </div>
      </section>

      {showTradingMemory && (
        <section className="marketing-section recurring-pattern-section">
          <div className="marketing-container recurring-pattern-panel">
            <div>
              <div className="marketing-eyebrow">Recurring behavior</div>
              <h2 className="marketing-section-title">Go beyond another discipline score.</h2>
              <p className="marketing-section-copy">
                A score tells you how you performed. A recurring pattern shows what you repeatedly do, when it
                happens, and which trades support the conclusion.
              </p>
            </div>
            <div className="pattern-comparison-grid">
              <article className="pattern-comparison-card muted">
                <span>Generic journal</span>
                <strong>Discipline score: 72</strong>
              </article>
              <article className="pattern-comparison-card">
                <span>TradePilot</span>
                <strong>
                  You cut winners early in 4 of 6 comparable continuation trades after starting the morning red.
                </strong>
                <p>Every pattern links back to the actual trades used in the analysis.</p>
              </article>
            </div>
          </div>
        </section>
      )}

      <section className="marketing-section" id="ai-coach">
        <div className="marketing-container story-grid">
          <article className="story-panel">
            <div className="marketing-eyebrow">Daily and Weekly Coach</div>
            <h2 className="marketing-section-title">Coaching Based on Your Trades, Not Generic Advice</h2>
            <p className="marketing-section-copy">
              TradePilot turns your selected day or week into structured feedback, a clear action rule, and data-quality
              guardrails that keep the coach honest.
            </p>
            <div className="story-list">
              <span><strong>Session Review</strong> Understand the key issue from a selected session.</span>
              <span><strong>Daily Coach</strong> Review one day's execution, strengths, and mistakes.</span>
              <span><strong>Weekly Coach</strong> Identify repeated behavior across the week.</span>
              <span><strong>Next-Session Action Plan</strong> Leave with one focused rule.</span>
            </div>
            <p className="coach-disclaimer">
              TradePilot analyzes historical execution. It does not provide trade signals or predict market direction.
            </p>
            <Link
              className="marketing-button primary story-cta"
              to="/register"
              onClick={() => trackSeoCtaClick("/", "Try the AI Coach", "/register")}
            >
              Try the AI Coach
            </Link>
          </article>
          <DailyCoachMockup />
        </div>
      </section>

      <section className="marketing-section" id="features">
        <div className="marketing-container">
          <header className="marketing-section-header centered">
            <div className="marketing-eyebrow">Launch features</div>
            <h2 className="marketing-section-title">Everything You Need to Review Your Trading</h2>
            <p className="marketing-section-copy">
              TradePilot supports the review loop traders need first: understand individual trades, coach the next
              session, and track whether execution is improving.
            </p>
          </header>
          <div className="marketing-card-grid features">
            {v1Features.map((feature, index) => (
              <article className="marketing-card feature-card" key={feature.title}>
                <div className="marketing-card-icon">{index + 1}</div>
                <h3>{feature.title}</h3>
                <p>{feature.copy}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      {showTradingMemory && (
        <section className="marketing-section privacy-memory-section">
          <div className="marketing-container audience-panel privacy-memory-panel">
            <div>
              <div className="marketing-eyebrow">Private by design</div>
              <h2 className="marketing-section-title">Built from your trading history, not everyone else’s.</h2>
              <p className="marketing-section-copy">
                Trading Memory searches only within your account. Your journal entries, emotional notes, and trade
                history remain scoped to your private TradePilot experience.
              </p>
            </div>
            <div className="audience-grid">
              <div className="audience-item">User-scoped trade retrieval</div>
              <div className="audience-item">No cross-user comparisons</div>
              <div className="audience-item">No public sharing by default</div>
              <div className="audience-item">Delete and regenerate controls</div>
            </div>
          </div>
        </section>
      )}

      <section className="marketing-section">
        <div className="marketing-container">
          <header className="marketing-section-header centered">
            <div className="marketing-eyebrow">Journal comparison</div>
            <h2 className="marketing-section-title">Traditional Journal vs. TradePilot</h2>
          </header>
          <div className="comparison-table" role="table" aria-label="Traditional Journal versus TradePilot">
            <div className="comparison-row comparison-header" role="row">
              <div role="columnheader">Feature</div>
              <div role="columnheader">Traditional Journal</div>
              <div role="columnheader">TradePilot</div>
            </div>
            {comparisonRows.map(([feature, traditional, tradepilot]) => (
              <div className="comparison-row" role="row" key={feature}>
                <div role="cell" data-label="Feature">{feature}</div>
                <div role="cell" data-label="Traditional Journal">
                  <StatusPill tone="neutral" label={traditional} />
                </div>
                <div role="cell" data-label="TradePilot">
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
            <h2 className="marketing-section-title">Built for Serious Futures Traders.</h2>
            <p className="marketing-section-copy">
              TradePilot is designed for traders who want to review execution, measure discipline, and improve decisions
              using their own trade history.
            </p>
          </div>
          <div className="audience-grid">
            {audienceItems.map((item) => (
              <div className="audience-item" key={item}>{item}</div>
            ))}
          </div>
        </div>
      </section>

      <section className="marketing-section" id="pricing">
        <div className="marketing-container">
          <header className="marketing-section-header pricing-header">
            <div>
              <div className="marketing-eyebrow">Pricing</div>
              <h2 className="marketing-section-title">One bad habit can cost more than a month of TradePilot.</h2>
              <p className="marketing-section-copy">
                Start with Free. Upgrade to Pro when you are ready for leak detection, session reviews, AI coaching,
                and eligible V2 memory features.
              </p>
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
                <span className="popular-badge">Limited access</span>
              </div>
              <div className="pricing-price">{marketingPricing.pro.price}</div>
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
              <Link className="marketing-button primary pricing-cta" to="/register">Join Pro waitlist</Link>
            </article>
          </div>
          <p className="pricing-note">
            AI usage is subject to the monthly credit allowance. Cached AI results remain viewable without consuming credits.
            Pro availability is currently limited while public payments are being prepared.
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
          <div className="final-cta">
            <h2 className="marketing-section-title">
              {showTradingMemory
                ? "Your next trade is new. Your behavior probably isn’t."
                : "Turn Your Next Trading Session Into Better Data"}
            </h2>
            <p className="marketing-section-copy">
              {showTradingMemory
                ? "Start building a journal that helps you recognize the patterns behind your performance."
                : "Import your trades, review your execution, identify the behavior costing you money, and leave with one clear priority for the next session."}
            </p>
            <div className="final-cta-actions">
              <Link className="marketing-button primary" to="/register">Start Journaling Free</Link>
              <a className="marketing-button ghost" href="#pricing">View Pricing</a>
            </div>
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
          <h2>Have I made this trade before?</h2>
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

function TradingMemoryInsightMockup() {
  return (
    <div className="memory-insight-mockup">
      <div className="report-kicker">Memory Insight</div>
      <h3>This trade matches a recurring early-exit pattern.</h3>
      <p>
        In four comparable trades, you protected open profit during consolidation even though your setup had not
        invalidated.
      </p>
      <div className="memory-evidence-list">
        <span>Verified sample: 6 comparable trades</span>
        <span>Pattern occurrence: 4 trades</span>
        <span>Supporting trades available</span>
      </div>
      <strong className="memory-lesson">Review the supporting trades before your next session.</strong>
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
          <h2>You lose the most after continuing to trade following two consecutive losses.</h2>
        </div>
        <span className="report-badge">Good signal</span>
      </div>
      <div className="report-stats">
        <div>
          <span>Illustrative monthly impact</span>
          <strong className="loss">-$1,257</strong>
        </div>
        <div>
          <span>Trades Analyzed</span>
          <strong>89</strong>
        </div>
        <div>
          <span>Confidence</span>
          <strong>76%</strong>
          <div className="confidence-track"><span style={{ width: "76%" }} /></div>
        </div>
      </div>
      <div className="recovery-plan">
        <div className="report-kicker">Focused Improvement Plan</div>
        <ol>
          <li>Set a hard trade cap before the session starts.</li>
          <li>Pause for 20 minutes after two consecutive losses before taking another trade.</li>
          <li>Review related trades and write one rule.</li>
          <li>Re-run Leak Detector after the next sample improves.</li>
        </ol>
      </div>
    </div>
  );
}

function DailyCoachMockup() {
  return (
    <div className="daily-coach-mockup">
      <div className="report-kicker">Daily Coach</div>
      <h3>Next-Session Focus</h3>
      <div className="coach-metric mission">
        <span>Today</span>
        <strong>Review the prior session before choosing a new risk posture.</strong>
      </div>
      <div className="coach-metric">
        <span>Main Risk</span>
        <strong>Taking another trade before the setup is confirmed</strong>
      </div>
      <div className="coach-metric">
        <span>Action Rule</span>
        <strong>Define direction thesis, invalidation, and target before entry.</strong>
      </div>
      <div className="coach-metric">
        <span>Data Quality</span>
        <strong>Coaching confidence adjusts when the selected day has limited data.</strong>
      </div>
    </div>
  );
}
