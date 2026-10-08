import { useEffect } from "react";
import { Link } from "react-router-dom";
import Seo from "../../components/Seo";
import { pageStructuredData, type BreadcrumbItem } from "../../seo/structuredData";
import { trackSeoCtaClick, trackSeoLandingPageView } from "../../utils/seoAttribution";
import { Breadcrumbs } from "./Breadcrumbs";
import "./HomePage.css";
import { MarketingShell } from "./MarketingLayout";

const PATH = "/how-tradepilot-works";
const TITLE = "How TradePilot Coach Works | Evidence-Based Trading Review";
const DESCRIPTION = "Learn how TradePilot Coach turns supported CSV trade history into deterministic evidence, behavioral patterns, AI explanations, and focused coaching rules.";
const breadcrumbs: BreadcrumbItem[] = [{ label: "Home", path: "/" }, { label: "How TradePilot Works", path: PATH }];

const steps = [
  ["Imported trades", "You import supported completed-trade CSV history. TradePilot Coach does not need your broker password for this workflow."],
  ["Deterministic analysis", "The product groups measurable history such as session timing, trade count, outcomes, setup labels, direction, confidence, and journal context."],
  ["Evidence thresholds", "It waits for enough relevant history before surfacing a pattern. With too little data, the product shows a limited-data state instead of forcing a conclusion."],
  ["Pattern detection", "Repeated negative groups and behavioral patterns are compared against the surrounding sample, then ranked for review."],
  ["AI explanation", "AI helps translate the measured result into readable context and a review workflow. It is not asked to invent a pattern from nothing or predict a market move."],
  ["Coaching rule", "The aim is one observable next-session guardrail that you can review against your own trades."],
] as const;

export default function HowTradePilotWorksPage() {
  useEffect(() => {
    trackSeoLandingPageView(PATH);
  }, []);

  return <MarketingShell>
    <Seo title={TITLE} description={DESCRIPTION} path={PATH} structuredData={pageStructuredData({ path: PATH, title: TITLE, description: DESCRIPTION, breadcrumbs })} />
    <section className="marketing-hero methodology-hero">
      <div className="marketing-container">
        <Breadcrumbs items={breadcrumbs} />
        <div className="marketing-eyebrow">Methodology</div>
        <h1 className="marketing-title">Your history is the evidence. Coaching is the next step.</h1>
        <p className="marketing-subtitle hero-support">TradePilot Coach uses deterministic, rule-based analysis to identify measurable behavior in completed trades, then uses AI to make the review easier to understand and act on.</p>
      </div>
    </section>
    <section className="marketing-section">
      <div className="marketing-container">
        <div className="methodology-flow" aria-label="TradePilot Coach analysis pipeline">
          {steps.map(([title, copy], index) => <article className="methodology-step" key={title}><span>{index + 1}</span><h2>{title}</h2><p>{copy}</p></article>)}
        </div>
      </div>
    </section>
    <section className="marketing-section methodology-detail-section">
      <div className="marketing-container methodology-detail-grid">
        <article className="methodology-detail-card"><h2>Does AI decide whether you made a mistake?</h2><p>No. The underlying analysis first looks for measurable trade-history patterns. AI can explain a detected pattern and help frame a coaching rule, but it does not turn a single trade into a verdict or provide trade signals.</p></article>
        <article className="methodology-detail-card"><h2>What does evidence strength mean?</h2><p>Evidence strength is a plain-language label for how much repeated historical support is available for a pattern. More relevant trades and a clearer negative difference make a pattern more useful to review. It is not a guarantee that the pattern will continue.</p></article>
        <article className="methodology-detail-card"><h2>What is estimated impact?</h2><p>Estimated historical impact summarizes the observed frequency and average result of a pattern across the sample. It is directional, based on the trades already imported, and is not a promise of recoverable profit or future performance.</p></article>
        <article className="methodology-detail-card"><h2>What if there is not enough data?</h2><p>TradePilot Coach shows a limited-data state rather than manufacturing certainty. For Leak Detector, the current product requires at least 10 imported trades before it evaluates behavioral patterns.</p></article>
      </div>
    </section>
    <section className="marketing-section methodology-trust-section">
      <div className="marketing-container audience-panel methodology-trust-panel">
        <div><div className="marketing-eyebrow">Private review workflow</div><h2 className="marketing-section-title">No broker password. No trade execution.</h2><p className="marketing-section-copy">Supported CSV imports create private, account-scoped trade records for analysis. TradePilot Coach does not place trades, copy trades, or provide market predictions.</p></div>
        <div className="audience-grid"><div className="audience-item">Supported CSV imports</div><div className="audience-item">Private account-scoped data</div><div className="audience-item">Historical review, not trade signals</div><div className="audience-item">Request account deletion through support</div></div>
      </div>
    </section>
    <section className="marketing-section"><div className="marketing-container final-cta"><h2 className="marketing-section-title">See the workflow with sample data.</h2><p className="marketing-section-copy">No signup and no CSV required.</p><div className="final-cta-actions"><Link className="marketing-button ghost" to="/demo" onClick={() => trackSeoCtaClick(PATH, "See TradePilot in Action", "/demo")}>See TradePilot in Action</Link><Link className="marketing-button primary" to="/register" onClick={() => trackSeoCtaClick(PATH, "Analyze My Own Trades", "/register")}>Analyze My Own Trades</Link></div></div></section>
  </MarketingShell>;
}
