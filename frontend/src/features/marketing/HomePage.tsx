import { useState } from "react";
import { Link } from "react-router-dom";
import "./HomePage.css";
import { MarketingShell } from "./MarketingLayout";

const heroBadges = ["AI Leak Detector", "Trading DNA", "Daily Coach", "Performance Analytics"];

const problemCards = [
  {
    title: "Winning strategy, bad execution",
    copy: "You may have a profitable setup, but poor execution quietly destroys your edge.",
  },
  {
    title: "Hidden habits",
    copy: "Specific days, sessions, emotions, or trade directions may be draining your account.",
  },
  {
    title: "Most journals show what happened",
    copy: "TradePilot shows what to change. Charts and win rates are useful, but they rarely tell you which habit is costing you money.",
  },
];

const leakFeatures = [
  "Detects costly trading habits",
  "Calculates monthly money impact",
  "Estimates yearly improvement opportunity",
  "Assigns confidence scores",
  "Links related trades",
  "Generates a 30-day recovery plan",
];

const journalComparison: Array<[string, boolean, boolean]> = [
  ["PnL Tracking", true, true],
  ["Win Rate", true, true],
  ["Calendar", true, true],
  ["Basic Analytics", true, true],
  ["AI Leak Detector", false, true],
  ["Trading DNA", false, true],
  ["Daily Coach", false, true],
  ["Weekly Coach", false, true],
  ["AI Trade Review", false, true],
  ["30-Day Recovery Plans", false, true],
  ["Behavioral Coaching", false, true],
];

const audienceItems = [
  "Prop firm traders",
  "Futures day traders",
  "ICT / smart money traders",
  "Traders trying to improve consistency",
];

const faqs = [
  {
    question: "Does AI invent my stats?",
    answer:
      "No. TradePilot calculates metrics from your trade history first, then uses AI to explain patterns and suggest improvements.",
  },
  {
    question: "Who is TradePilot for?",
    answer:
      "Active futures traders, prop firm traders, and serious retail traders who want to improve consistency.",
  },
  {
    question: "Do cached AI results use credits?",
    answer:
      "No. Cached results are free to view again. Regenerating fresh AI analysis uses credits.",
  },
  {
    question: "Can I start for free?",
    answer:
      "Yes. Free users can use core journaling features and limited AI credits.",
  },
];

export default function HomePage() {
  const [billingCycle, setBillingCycle] = useState<"monthly" | "yearly">("monthly");
  const proPrice = billingCycle === "monthly" ? "$29.99/mo" : "$249/year";

  return (
    <MarketingShell>
      <section className="marketing-hero">
        <div className="marketing-container marketing-hero-grid">
          <div>
            <div className="marketing-eyebrow">AI-powered performance coaching</div>
            <h1 className="marketing-title">Stop wondering why you're losing money.</h1>
            <p className="marketing-subtitle hero-lead">
              TradePilot finds the habits costing you thousands and gives you a plan to improve.
            </p>
            <p className="marketing-subtitle hero-support">
              AI-powered performance coaching for serious futures traders. Discover your biggest leaks,
              understand your trading DNA, and build consistency with personalized AI insights.
            </p>
            <div className="marketing-hero-actions">
              <Link className="marketing-button primary" to="/register">Start Improving Today</Link>
              <a className="marketing-button ghost" href="#leak-report">See AI Leak Report</a>
            </div>
            <div className="marketing-proof-row compact" aria-label="TradePilot product highlights">
              {heroBadges.map((badge) => (
                <div className="marketing-proof-item" key={badge}>{badge}</div>
              ))}
            </div>
          </div>
          <LeakReportMockup />
        </div>
      </section>

      <section className="marketing-section">
        <div className="marketing-container">
          <header className="marketing-section-header centered">
            <div className="marketing-eyebrow">The real problem</div>
            <h2 className="marketing-section-title">Most traders know they are losing. They don't know why.</h2>
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

      <section className="marketing-section flagship-section" id="leak-report">
        <div className="marketing-container flagship-grid">
          <div>
            <div className="marketing-eyebrow">AI Leak Detector</div>
            <h2 className="marketing-section-title">Meet your AI Trading Performance Coach</h2>
            <p className="marketing-section-copy">
              TradePilot analyzes your actual trades to reveal the mistakes silently destroying your performance.
              The stats are calculated first, then AI turns the signal into clear coaching.
            </p>
            <div className="feature-bullet-grid">
              {leakFeatures.map((feature) => (
                <div className="feature-bullet" key={feature}>{feature}</div>
              ))}
            </div>
            <Link className="marketing-button primary flagship-cta" to="/register">Find My Biggest Leak</Link>
          </div>
          <div className="flagship-panel">
            <LeakReportMockup compact />
          </div>
        </div>
      </section>

      <section className="marketing-section" id="features">
        <div className="marketing-container">
          <header className="marketing-section-header centered">
            <div className="marketing-eyebrow">Not another spreadsheet</div>
            <h2 className="marketing-section-title">Traditional journal vs. TradePilot</h2>
          </header>
          <div className="comparison-table" role="table" aria-label="Traditional Journal versus TradePilot">
            <div className="comparison-row comparison-header" role="row">
              <div role="columnheader">Feature</div>
              <div role="columnheader">Traditional Journal</div>
              <div role="columnheader">TradePilot</div>
            </div>
            {journalComparison.map(([feature, traditional, tradepilot]) => (
              <div className="comparison-row" role="row" key={feature}>
                <div role="cell" data-label="Feature">{feature}</div>
                <div role="cell" data-label="Traditional Journal">
                  <StatusPill included={traditional} />
                </div>
                <div role="cell" data-label="TradePilot">
                  <StatusPill included={tradepilot} />
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="marketing-section" id="ai-coach">
        <div className="marketing-container story-grid">
          <article className="story-panel">
            <div className="marketing-eyebrow">Trading DNA</div>
            <h2 className="marketing-section-title">Your strategy is only half the story.</h2>
            <p className="marketing-section-copy">
              Trading DNA reveals the strengths, weaknesses, setup quality, and behavioral patterns behind your decisions.
            </p>
            <div className="story-list">
              <span>Best setups</span>
              <span>Weakest habits</span>
              <span>Emotional tendencies</span>
              <span>Rules for tomorrow</span>
            </div>
          </article>
          <TradingDnaVisual />
        </div>
      </section>

      <section className="marketing-section coach-preview-section">
        <div className="marketing-container story-grid flipped">
          <DailyCoachMockup />
          <article className="story-panel">
            <div className="marketing-eyebrow">Daily Coach</div>
            <h2 className="marketing-section-title">Know what to focus on before the next session.</h2>
            <p className="marketing-section-copy">
              TradePilot turns recent patterns into a sharper pre-market checklist, so your next session starts
              with a plan instead of vague motivation.
            </p>
          </article>
        </div>
      </section>

      <section className="marketing-section audience-section">
        <div className="marketing-container audience-panel">
          <div>
            <div className="marketing-eyebrow">Who it's for</div>
            <h2 className="marketing-section-title">Built for serious futures traders.</h2>
            <p className="marketing-section-copy">
              TradePilot turns raw trade history into coaching insights, behavioral patterns, and a focused improvement plan.
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
              <h2 className="marketing-section-title">One bad habit can cost more than a year of TradePilot.</h2>
              <p className="marketing-section-copy">
                Start with Free. Upgrade to Pro when you're ready for AI coaching, leak detection, and deeper analytics.
              </p>
            </div>
            <div className="billing-toggle" aria-label="Billing cycle">
              <button
                type="button"
                className={billingCycle === "monthly" ? "active" : ""}
                onClick={() => setBillingCycle("monthly")}
              >
                Monthly
              </button>
              <button
                type="button"
                className={billingCycle === "yearly" ? "active" : ""}
                onClick={() => setBillingCycle("yearly")}
              >
                Yearly
              </button>
            </div>
          </header>
          <div className="pricing-grid">
            <article className="pricing-card">
              <div className="pricing-card-top">
                <h3>Free</h3>
              </div>
              <div className="pricing-price">$0</div>
              <p className="marketing-section-copy">Core journaling and analytics to get started.</p>
              <ul>
                <li>Dashboard</li>
                <li>Calendar</li>
                <li>Basic analytics</li>
                <li>10 AI credits/month</li>
              </ul>
              <Link className="marketing-button ghost pricing-cta" to="/register">Start Free</Link>
            </article>
            <article className="pricing-card pro">
              <div className="pricing-card-top">
                <h3>Pro</h3>
                <span className="popular-badge">Flagship AI</span>
              </div>
              <div className="pricing-price">{proPrice}</div>
              {billingCycle === "yearly" && <div className="savings-badge">Save $110/year</div>}
              <p className="marketing-section-copy">AI coaching and performance reports for serious improvement.</p>
              <ul>
                <li>AI Leak Detector</li>
                <li>Trading DNA</li>
                <li>Daily Coach</li>
                <li>Weekly Coach</li>
                <li>30-Day Recovery Plans</li>
                <li>Advanced analytics</li>
                <li><span className="credit-highlight">100 AI credits/month</span></li>
              </ul>
              <Link className="marketing-button primary pricing-cta" to="/register">Start Pro</Link>
            </article>
          </div>
          <p className="pricing-note">Cancel anytime. Taxes may apply. Stripe-secured checkout after account creation.</p>
        </div>
      </section>

      <section className="marketing-section faq-section">
        <div className="marketing-container">
          <header className="marketing-section-header centered">
            <div className="marketing-eyebrow">FAQ</div>
            <h2 className="marketing-section-title">Built for serious traders, not magic promises.</h2>
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
            <h2 className="marketing-section-title">Stop repeating the mistakes that are draining your account.</h2>
            <p className="marketing-section-copy">
              Start free, import your trades, and upgrade when you're ready for the full AI performance coach.
            </p>
            <Link className="marketing-button primary" to="/register">Start Improving Today</Link>
          </div>
        </div>
      </section>
    </MarketingShell>
  );
}

function StatusPill({ included }: { included: boolean }) {
  return (
    <span className={`status-pill ${included ? "included" : "excluded"}`}>
      <span aria-hidden="true">{included ? "✓" : "×"}</span>
      {included ? "Included" : "Not included"}
    </span>
  );
}

function LeakReportMockup({ compact = false }: { compact?: boolean }) {
  return (
    <div className={`leak-report-mockup ${compact ? "compact" : ""}`} aria-label="AI Leak Detector report preview">
      <div className="leak-report-header">
        <div>
          <div className="report-kicker">Performance Leak Report</div>
          <h2>You are losing the most money when trading SHORT.</h2>
        </div>
        <span className="report-badge">Good signal</span>
      </div>
      <div className="report-stats">
        <div>
          <span>Money Lost / Month</span>
          <strong className="loss">-$1,257</strong>
        </div>
        <div>
          <span>Potential Yearly Improvement</span>
          <strong>+$15,087/yr</strong>
        </div>
        <div>
          <span>Confidence</span>
          <strong>76%</strong>
          <div className="confidence-track"><span style={{ width: "76%" }} /></div>
        </div>
      </div>
      <div className="recovery-plan">
        <div className="report-kicker">30-Day Recovery Plan</div>
        <ol>
          <li>Reduce short-side size until confirmation improves.</li>
          <li>Require one extra checklist signal before short entries.</li>
          <li>Review 33 short trades and write one rule.</li>
          <li>Re-run Leak Detector and compare improvement.</li>
        </ol>
      </div>
    </div>
  );
}

function TradingDnaVisual() {
  return (
    <div className="story-visual">
      <div className="dna-score-card">
        <div className="report-kicker">Trading DNA</div>
        <h3>Strength profile</h3>
        <DnaBar label="Best setup quality" value="82%" />
        <DnaBar label="Patience" value="68%" />
        <DnaBar label="Risk control" value="74%" />
        <div className="mock-rule">Rule for tomorrow: only take A+ continuation entries after confirmation.</div>
      </div>
    </div>
  );
}

function DnaBar({ label, value }: { label: string; value: string }) {
  return (
    <div className="dna-bar-row">
      <div><span>{label}</span><strong>{value}</strong></div>
      <div className="mock-bar"><span style={{ width: value }} /></div>
    </div>
  );
}

function DailyCoachMockup() {
  return (
    <div className="daily-coach-mockup">
      <div className="report-kicker">Daily Coach</div>
      <h3>Pre-Market Briefing</h3>
      <div className="coach-metric mission">
        <span>Today's Mission</span>
        <strong>Protect discipline while waiting for confirmation.</strong>
      </div>
      <div className="coach-metric">
        <span>Main Risk</span>
        <strong>Chasing after a missed move</strong>
      </div>
      <div className="coach-metric">
        <span>Top Leak to Watch</span>
        <strong>Short Side Bias</strong>
      </div>
      <div className="coach-metric">
        <span>Rules for Today</span>
        <ul className="coach-rule-list">
          <li>Wait for confirmation</li>
          <li>Stop after two rule breaks</li>
          <li>Reduce size on weak-direction trades</li>
        </ul>
      </div>
    </div>
  );
}
