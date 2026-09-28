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
    title: "Import",
    copy: "Bring in your trade history.",
  },
  {
    title: "Review",
    copy: "See performance and execution in context.",
  },
  {
    title: "Detect",
    copy: "Surface recurring behavioral patterns.",
  },
  {
    title: "Improve",
    copy: "Turn insight into your next coaching focus.",
  },
];

const leakFeatures = [
  "Find the behavior costing you the most",
  "See when the leak appears in your sessions",
  "Leave with one focused rule for next time",
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

const comparisonRows = [
  ["Trade history", "Logs trades", "Analyzes trades"],
  ["Performance", "Shows P&L", "Explains performance patterns"],
  ["Notes", "Stores notes", "Detects behavioral leaks"],
  ["Review", "Reviews trades individually", "Connects patterns across sessions"],
  ["Interpretation", "Requires manual interpretation", "Produces coaching insights"],
  ["Next step", "Records history", "Creates an improvement focus"],
];

const audienceItems = [
  "Multi-account journaling",
  "Futures-focused CSV workflows",
  "Session-level analysis",
  "Execution review",
];

const resourceClusters = [
  {
    title: "Trading Journals",
    links: [
      ["AI Trading Journal", "/ai-trading-journal"],
      ["Futures Trading Journal", "/futures-trading-journal"],
      ["Topstep Trading Journal", "/topstep-trading-journal"],
      ["Tradovate Trading Journal", "/tradovate-trading-journal"],
      ["Prop Firm Trading Journal", "/prop-firm-trading-journal"],
    ],
  },
  {
    title: "Trading Behavior",
    links: [
      ["Trading Psychology", "/trading-psychology"],
      ["Revenge Trading", "/revenge-trading"],
      ["Overtrading", "/overtrading"],
      ["Trading Discipline", "/trading-discipline"],
      ["Breaking Trading Rules", "/breaking-trading-rules"],
      ["Trading After Losses", "/trading-after-losses"],
    ],
  },
  {
    title: "Free Tools",
    links: [
      ["Trading Expectancy Calculator", "/tools/trading-expectancy-calculator"],
      ["Overtrading Calculator", "/tools/overtrading-calculator"],
      ["Revenge Trading Calculator", "/tools/revenge-trading-calculator"],
    ],
  },
] as const;

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
    question: "What does the AI Coach analyze?",
    answer:
      "It reviews your trade history, selected dates, P&L, direction, setup notes, confidence, emotion, and available metadata.",
  },
  {
    question: "What happens when there is not enough data?",
    answer:
      "TradePilot Coach shows limited-data states instead of forcing a confident answer. No-trade days and weeks skip AI generation.",
  },
  {
    question: "Will TradePilot Coach make me profitable?",
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
      "TradePilot Coach uses authenticated APIs, account ownership checks, and role-based admin access to keep user data scoped correctly.",
  },
  {
    question: "Who is TradePilot Coach built for?",
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
        title="TradePilot Coach | AI Trading Journal & Execution Intelligence"
        description="TradePilot Coach is an AI-powered trading journal and execution intelligence platform for futures traders. Analyze trades, uncover behavioral patterns, review execution, and turn trading history into actionable coaching insights."
        structuredData={{
          "@context": "https://schema.org",
          "@graph": [
            {
              "@type": "Organization",
              name: "TradePilot Coach",
              url: "https://www.tradepilotcoach.com",
              logo: "https://www.tradepilotcoach.com/assets/tradepilot-coach-logo.png",
            },
            {
              "@type": "SoftwareApplication",
              name: "TradePilot Coach",
              applicationCategory: "FinanceApplication",
              operatingSystem: "Web",
              url: "https://www.tradepilotcoach.com",
              description: "AI trading journal, execution intelligence, and performance coaching for futures traders.",
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
            <div className="marketing-eyebrow">AI execution intelligence for futures traders</div>
            <h1 className="marketing-title">
              Your P&amp;L tells you what happened. <span className="brand-accent">TradePilot Coach tells you why.</span>
            </h1>
            <p className="marketing-subtitle hero-support">
              Turn your trading history into actionable execution insights. TradePilot Coach analyzes your trades, behavior,
              timing, discipline, and recurring mistakes so you can understand what improves your performance - and what keeps costing you. Explore the <Link className="marketing-inline-link" to="/ai-trading-journal">AI trading journal</Link> built for <Link className="marketing-inline-link" to="/futures-trading-journal">futures traders</Link>.
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

      {showTradingMemory && (
        <section className="marketing-section memory-transition-section">
          <div className="marketing-container">
            <header className="marketing-section-header centered">
              <div className="marketing-eyebrow">Personal pattern recognition</div>
              <h2 className="marketing-section-title">
                A journal records what happened. TradePilot Coach helps you understand why it keeps happening.
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

      <section className="marketing-section differentiation-section" id="features">
        <div className="marketing-container">
          <div className="journal-difference-layout">
            <header className="marketing-section-header">
              <div className="marketing-eyebrow">More than a journal</div>
              <h2 className="marketing-section-title">Most journals tell you what you traded. TradePilot Coach helps you understand how you traded.</h2>
            </header>
            <div className="journal-difference-points">
              <p>See the behavior behind the result.</p>
              <p>Find timing and discipline patterns.</p>
              <p>Turn review into one clear next focus.</p>
            </div>
          </div>
        </div>
      </section>

      <section className="marketing-section" id="how-it-works">
        <div className="marketing-container">
          <header className="marketing-section-header centered">
            <div className="marketing-eyebrow">How it works</div>
            <h2 className="marketing-section-title">From CSV Import to Actionable Coaching</h2>
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

      {showTradingMemory && (
        <section className="marketing-section trading-memory-section" id="trading-memory">
          <div className="marketing-container trading-memory-grid">
            <div>
              <div className="marketing-eyebrow">Private beta</div>
              <h2 className="marketing-section-title">TradePilot Coach remembers how you trade.</h2>
              <p className="marketing-section-copy">
                Every reviewed trade becomes part of your private trading memory. When you analyze a new trade,
                TradePilot Coach finds similar moments from your history and shows what you did, how you felt, and what
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
              onClick={() => trackSeoCtaClick("/", "Find My Biggest Leak", "/register")}
            >
              Find My Biggest Leak
            </Link>
          </div>
          <div className="flagship-panel flagship-panel-emphasis">
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
                <span>TradePilot Coach</span>
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
              Your reviews connect recurring behavior across sessions and turn it into one specific, actionable focus.
            </p>
            <div className="story-list">
              <span><strong>Your history</strong> Feedback starts with the trades and sessions you actually reviewed.</span>
              <span><strong>Recurring behavior</strong> See what repeats before it becomes another result.</span>
              <span><strong>One clear focus</strong> Leave with a specific rule for the next session.</span>
            </div>
            <p className="coach-disclaimer">
              TradePilot Coach analyzes historical execution. It does not provide trade signals or predict market direction.
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

      {showTradingMemory && (
        <section className="marketing-section privacy-memory-section">
          <div className="marketing-container audience-panel privacy-memory-panel">
            <div>
              <div className="marketing-eyebrow">Private by design</div>
              <h2 className="marketing-section-title">Built from your trading history, not everyone else’s.</h2>
              <p className="marketing-section-copy">
                Trading Memory searches only within your account. Your journal entries, emotional notes, and trade
                history remain scoped to your private TradePilot Coach experience.
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
            <h2 className="marketing-section-title">Built for Serious Futures Traders.</h2>
            <p className="marketing-section-copy">
              TradePilot Coach is designed for traders who want to review execution, measure discipline, and improve decisions
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

      <section className="marketing-section resource-discovery-section" aria-labelledby="explore-tradepilot-coach">
        <div className="marketing-container">
          <header className="marketing-section-header centered">
            <div className="marketing-eyebrow">Explore</div>
            <h2 className="marketing-section-title" id="explore-tradepilot-coach">Explore TradePilot Coach</h2>
            <p className="marketing-section-copy">Find the journal guides, behavior frameworks, and free tools that match the review you want to do next.</p>
          </header>
          <nav className="marketing-card-grid three resource-cluster-grid" aria-label="TradePilot Coach resources">
            {resourceClusters.map((cluster) => (
              <section className="marketing-card resource-cluster" key={cluster.title}>
                <h3>{cluster.title}</h3>
                <div className="resource-cluster-links">
                  {cluster.links.map(([label, to]) => <Link key={to} to={to}>{label}</Link>)}
                </div>
              </section>
            ))}
          </nav>
        </div>
      </section>

      <section className="marketing-section" id="pricing">
        <div className="marketing-container">
          <header className="marketing-section-header pricing-header">
            <div>
              <div className="marketing-eyebrow">Pricing</div>
              <h2 className="marketing-section-title">Turn your trading history into your next coaching insight.</h2>
              <p className="marketing-section-copy">
                Start with Free. Pro availability is currently limited while public payments are being prepared.
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
          <div className="final-cta">
            <h2 className="marketing-section-title">Your next trading insight may already be in your history.</h2>
            <p className="marketing-section-copy">Start journaling and let TradePilot Coach help you uncover it.</p>
            <div className="final-cta-actions">
              <Link className="marketing-button primary" to="/register">Start Journaling Free</Link>
            </div>
          </div>
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
          <h2>{compact ? "Overtrading follows two consecutive losses." : "You lose the most after continuing to trade following two consecutive losses."}</h2>
        </div>
        <span className="report-badge">Good signal</span>
      </div>
      <div className="report-stats">
        <div>
          <span>Illustrative monthly impact</span>
          <strong className="loss">-$1,257</strong>
        </div>
        {!compact && (
          <div>
            <span>Trades Analyzed</span>
            <strong>89</strong>
          </div>
        )}
        <div>
          <span>Confidence</span>
          <strong>76%</strong>
          <div className="confidence-track"><span style={{ width: "76%" }} /></div>
        </div>
      </div>
      <div className="recovery-plan">
        <div className="report-kicker">Focused Improvement Plan</div>
        <ol>
          <li>Pause for 20 minutes after two consecutive losses.</li>
          <li>Set a hard trade cap before the session starts.</li>
          {!compact && <li>Review related trades and write one rule.</li>}
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
