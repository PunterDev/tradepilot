import { useState, type ReactNode } from "react";
import { Link } from "react-router-dom";
import "./HomePage.css";
import { MarketingShell } from "./MarketingLayout";

const problems = [
  "You revenge trade after losses",
  "You size up after wins",
  "You overtrade Fridays and OPEX",
  "You skip journaling because spreadsheets are boring",
];

const features = [
  ["AI Trade Review", "Every trade gets a coach-style read on entry quality, management, and execution leaks."],
  ["Daily / Weekly Coach", "Turn your latest trading into focused feedback instead of another pile of notes."],
  ["Trading DNA", "Find recurring strengths, weaknesses, emotional tendencies, and risk patterns."],
  ["Session Replay", "Review the full trading day chronologically like game film."],
  ["PnL Calendar", "Spot when you perform best, when you give back, and when to stop trading."],
  ["Discipline Tracking", "Track rule breaks, overtrading, revenge trading, and plan adherence."],
  ["Prop Firm Risk Awareness", "Stay aware of drawdown pressure and behaviors that threaten payouts."],
];

export default function HomePage() {
  const [billingCycle, setBillingCycle] = useState<"monthly" | "yearly">("monthly");
  const proPrice = billingCycle === "monthly" ? "$29.99/mo" : "$249/year";

  return (
    <MarketingShell>
      <section className="marketing-hero">
        <div className="marketing-container marketing-hero-grid">
          <div>
            <div className="marketing-eyebrow">Built for futures and prop firm traders</div>
            <h1 className="marketing-title">Your AI Trading Mentor for Futures Traders</h1>
            <p className="marketing-subtitle">
              Import your trades, uncover the mistakes costing you payouts, and get a daily rule to trade better tomorrow.
            </p>
            <div className="marketing-hero-actions">
              <Link className="marketing-button primary" to="/register">Start Free</Link>
              <a className="marketing-button ghost" href="#how-it-works">See How It Works</a>
            </div>
            <div className="marketing-proof-row" aria-label="TradePilot outcomes">
              <div className="marketing-proof-item">Built for NQ, ES, MNQ, MES traders</div>
              <div className="marketing-proof-item">CSV import support</div>
              <div className="marketing-proof-item">AI daily and weekly coaching</div>
              <div className="marketing-proof-item">Prop firm focused</div>
            </div>
          </div>
          <HeroMockup />
        </div>
      </section>

      <section className="marketing-section">
        <div className="marketing-container">
          <header className="marketing-section-header">
            <div className="marketing-eyebrow">The real problem</div>
            <h2 className="marketing-section-title">Most traders do not need more indicators. They need better feedback.</h2>
          </header>
          <div className="marketing-card-grid">
            {problems.map((problem, index) => (
              <article className="marketing-card" key={problem}>
                <div className="marketing-card-icon">{index + 1}</div>
                <h3>{problem}</h3>
                <p>TradePilot turns the pattern into something visible, measurable, and fixable.</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="marketing-section" id="features">
        <div className="marketing-container">
          <header className="marketing-section-header">
            <div className="marketing-eyebrow">The coaching system</div>
            <h2 className="marketing-section-title">TradePilot turns your trade history into a coaching plan.</h2>
            <p className="marketing-section-copy">
              Stop treating each trade like an isolated event. See the behaviors, conditions, and decisions that repeat.
            </p>
          </header>
          <div className="marketing-card-grid features">
            {features.map(([title, copy], index) => (
              <article className="marketing-card" key={title}>
                <div className="marketing-card-icon">{String.fromCharCode(65 + index)}</div>
                <h3>{title}</h3>
                <p>{copy}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="marketing-section" id="ai-coach">
        <div className="marketing-container deep-dive">
          <DeepDive
            eyebrow="Session Review"
            title="Replay the trading day like game film."
            copy="See every trade in order, identify the inflection point, and leave with one rule for the next session."
            visual={<TimelineVisual />}
          />
          <DeepDive
            eyebrow="Trading DNA"
            title="Find the behavior patterns hiding in your history."
            copy="TradePilot surfaces strengths, weaknesses, worst environments, emotional tendencies, and risk patterns."
            visual={<DnaVisual />}
          />
          <DeepDive
            eyebrow="AI Daily Coach"
            title="One focused rule for tomorrow."
            copy="No generic motivation. Just the next behavior that deserves your attention before you trade again."
            visual={<CoachVisual />}
          />
          <DeepDive
            eyebrow="PnL Calendar"
            title="See exactly when you perform best and when you should stop trading."
            copy="Quickly spot Friday weakness, OPEX risk, hot streaks, drawdown clusters, and recovery days."
            visual={<CalendarVisual />}
          />
        </div>
      </section>

      <section className="marketing-section" id="how-it-works">
        <div className="marketing-container">
          <header className="marketing-section-header">
            <div className="marketing-eyebrow">How it works</div>
            <h2 className="marketing-section-title">Three steps from raw trades to a better plan.</h2>
          </header>
          <div className="steps-grid">
            {["Import your trades", "Review your patterns", "Trade tomorrow with one clear rule"].map((step, index) => (
              <article className="marketing-card" key={step}>
                <div className="marketing-card-icon">{index + 1}</div>
                <h3>{step}</h3>
                <p>{index === 0 ? "Upload your Tradovate CSV and organize trades by account." : index === 1 ? "Use calendars, DNA, session review, and AI coaching to find what repeats." : "Walk into the next session with a specific behavior to protect."}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="marketing-section" id="pricing">
        <div className="marketing-container">
          <header className="marketing-section-header pricing-header">
            <div>
              <div className="marketing-eyebrow">Pricing preview</div>
              <h2 className="marketing-section-title">Start simple. Upgrade when AI coaching becomes part of your process.</h2>
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
              <p className="marketing-section-copy">For getting your journal out of spreadsheets.</p>
              <ul>
                <li>CSV import</li>
                <li>Limited AI reviews</li>
                <li>PnL calendar</li>
                <li>Basic analytics</li>
              </ul>
              <Link className="marketing-button ghost pricing-cta" to="/register">Start Free</Link>
            </article>
            <article className="pricing-card pro">
              <div className="pricing-card-top">
                <h3>Pro</h3>
                <span className="popular-badge">Most Popular</span>
              </div>
              <div className="pricing-price">{proPrice}</div>
              {billingCycle === "yearly" && <div className="savings-badge">Save $110/year</div>}
              <p className="marketing-section-copy">For traders who want feedback loops, not just records.</p>
              <ul>
                <li>More AI reviews</li>
                <li>Daily and weekly coach</li>
                <li>Trading DNA</li>
                <li>Session review</li>
                <li>Advanced analytics</li>
                <li>Priority feature access</li>
              </ul>
              <Link className="marketing-button primary pricing-cta" to="/register">Start Pro</Link>
            </article>
          </div>
          <p className="pricing-note">Cancel anytime. Taxes may apply. Stripe-secured checkout.</p>
          <div className="contact-cta">
            <div>
              <div className="marketing-eyebrow">Contact</div>
              <h2>Have questions about TradePilot?</h2>
              <p>
                Whether you're a prop firm trader, futures trader, or just getting started with journaling,
                reach out anytime.
              </p>
            </div>
            <div className="contact-cta-actions">
              <Link className="marketing-button ghost" to="/contact">Contact Us</Link>
              <Link className="marketing-button primary" to="/register">Start Free</Link>
            </div>
          </div>
          <div className="final-cta">
            <h2 className="marketing-section-title">Stop guessing why you lose. Let your trades tell you.</h2>
            <Link className="marketing-button primary" to="/register">Create Your Free Account</Link>
          </div>
        </div>
      </section>

    </MarketingShell>
  );
}

function HeroMockup() {
  return (
    <div className="hero-mockup" aria-label="TradePilot dashboard preview">
      <div className="mock-window">
        <div className="mock-topbar">
          <div className="mock-dots">
            <span className="mock-dot" />
            <span className="mock-dot" />
            <span className="mock-dot" />
          </div>
          <strong>TradePilot Command Center</strong>
        </div>
        <div className="mock-body">
          <div className="mock-card">
            <h3>PnL Calendar</h3>
            <div className="mock-calendar">
              {["+$420", "-$180", "+$95", "$0", "+$310", "-$240", "+$150", "+$80", "-$90", "+$520"].map((pnl) => (
                <div className={`mock-day ${pnl.startsWith("+") ? "win" : pnl.startsWith("-") ? "loss" : ""}`} key={pnl}>{pnl}</div>
              ))}
            </div>
          </div>
          <div className="mock-card mock-coach">
            <h3>AI Coach</h3>
            <div className="mock-rule">Tomorrow: no continuation entries inside 5m consolidation.</div>
          </div>
          <div className="mock-card">
            <h3>Session Review</h3>
            <TimelineVisual />
          </div>
          <div className="mock-card">
            <h3>Trading DNA</h3>
            <DnaBars />
          </div>
        </div>
      </div>
    </div>
  );
}

function DeepDive({ eyebrow, title, copy, visual }: { eyebrow: string; title: string; copy: string; visual: ReactNode }) {
  return (
    <article className="deep-panel">
      <div className="deep-copy">
        <div className="marketing-eyebrow">{eyebrow}</div>
        <h2 className="marketing-section-title">{title}</h2>
        <p className="marketing-section-copy">{copy}</p>
      </div>
      <div className="mini-visual">{visual}</div>
    </article>
  );
}

function TimelineVisual() {
  return (
    <div className="mock-timeline">
      <div className="mock-trade win"><span>09:47</span><strong>NQ Long</strong><span>+$120</span></div>
      <div className="mock-trade win"><span>10:18</span><strong>MNQ Long</strong><span>+$85</span></div>
      <div className="mock-trade loss"><span>10:42</span><strong>NQ Long</strong><span>-$210</span></div>
    </div>
  );
}

function DnaBars() {
  return (
    <div className="mock-dna-bars">
      <div><span>Discipline</span><div className="mock-bar"><span style={{ width: "78%" }} /></div></div>
      <div><span>Patience</span><div className="mock-bar"><span style={{ width: "64%" }} /></div></div>
      <div><span>Risk control</span><div className="mock-bar"><span style={{ width: "72%" }} /></div></div>
    </div>
  );
}

function DnaVisual() {
  return (
    <div className="mock-coach">
      <DnaBars />
      <div className="mock-rule">Pattern found: early winners often lead to oversized follow-up trades.</div>
    </div>
  );
}

function CoachVisual() {
  return (
    <div className="mock-coach">
      <div className="mock-rule">One rule: wait for HTF alignment before entering.</div>
      <div className="marketing-card" style={{ minHeight: 0 }}>
        <h3>What hurt performance</h3>
        <p>Two entries came before confirmation after a losing trade.</p>
      </div>
    </div>
  );
}

function CalendarVisual() {
  return (
    <div className="mock-calendar">
      {["+$320", "+$180", "-$260", "+$90", "+$410", "-$120", "$0", "+$250", "-$80", "+$145"].map((pnl) => (
        <div className={`mock-day ${pnl.startsWith("+") ? "win" : pnl.startsWith("-") ? "loss" : ""}`} key={pnl}>{pnl}</div>
      ))}
    </div>
  );
}
