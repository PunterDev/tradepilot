import { useEffect } from "react";
import { Link } from "react-router-dom";
import Seo from "../../components/Seo";
import { pageStructuredData, type BreadcrumbItem } from "../../seo/structuredData";
import { trackSeoCtaClick, trackSeoEvent } from "../../utils/seoAttribution";
import { Breadcrumbs } from "./Breadcrumbs";
import "./HomePage.css";
import { MarketingShell } from "./MarketingLayout";
import { publicSampleAccount, sampleEvidenceMethod } from "./sampleAccount";

const PATH = "/demo";
const TITLE = "See TradePilot Coach in Action | Sample Futures Trading Account";
const DESCRIPTION = "Explore an illustrative TradePilot Coach account with performance context, a behavioral leak, and a focused coaching rule. No signup or CSV required.";
const breadcrumbs: BreadcrumbItem[] = [{ label: "Home", path: "/" }, { label: "Sample demo", path: PATH }];
const evidenceLabel = publicSampleAccount.primaryFinding.evidenceStrength.toLowerCase();

export default function SampleDemoPage() {
  useEffect(() => {
    trackSeoEvent("sample_demo_opened", PATH);
  }, []);

  return (
    <MarketingShell>
      <Seo
        title={TITLE}
        description={DESCRIPTION}
        path={PATH}
        structuredData={pageStructuredData({ path: PATH, title: TITLE, description: DESCRIPTION, breadcrumbs })}
      />
      <section className="marketing-hero demo-hero">
        <div className="marketing-container">
          <Breadcrumbs items={breadcrumbs} />
          <div className="demo-intro">
            <div className="marketing-eyebrow">Illustrative demo account</div>
            <h1 className="marketing-title">See the review loop before you import.</h1>
            <p className="marketing-subtitle hero-support">
              This is static sample data, not customer performance. Walk through the same kind of account summary,
              behavioral evidence, and coaching focus TradePilot Coach is designed to make easier to review.
            </p>
            <div className="marketing-hero-actions demo-top-actions">
              <Link className="marketing-button primary" to={publicSampleAccount.ctas.registrationPath} onClick={() => trackSeoCtaClick(PATH, publicSampleAccount.ctas.registrationLabel, "/register")}>{publicSampleAccount.ctas.registrationLabel}</Link>
              <a className="marketing-button ghost" href="#demo-summary">Explore the sample</a>
            </div>
            <p className="demo-hero-preview"><strong>Sample finding:</strong> {publicSampleAccount.primaryFinding.title} · {publicSampleAccount.primaryFinding.occurrenceCount} occurrences · {evidenceLabel} evidence</p>
          </div>
        </div>
      </section>

      <section className="marketing-section demo-workspace-section">
        <div className="marketing-container">
          <div className="demo-disclaimer" role="note">{publicSampleAccount.account.disclosure}</div>
          <div className="demo-workspace">
            <aside className="demo-sidebar" aria-label="Sample TradePilot Coach workflow">
              <span className="demo-sidebar-kicker">Sample walkthrough</span>
              <strong>{publicSampleAccount.account.label}</strong>
              <span>{publicSampleAccount.account.reviewWindow}</span>
              <a className="active" href="#demo-summary"><b>1</b>Performance summary</a>
              <a href="#demo-leak"><b>2</b>Leak Detector</a>
              <a href="#demo-coach"><b>3</b>Coaching rule</a>
            </aside>
            <div className="demo-content demo-story">
              <section id="demo-summary" className="demo-stage"><DemoSummary /></section>
              <section id="demo-leak" className="demo-stage"><DemoLeak /></section>
              <section id="demo-coach" className="demo-stage"><DemoCoach /></section>
            </div>
          </div>
          <div className="demo-final-cta">
            <div>
              <div className="marketing-eyebrow">Your own history</div>
              <h2>Analyze your own trading history.</h2>
              <p>Import a supported Tradovate or Topstep export when you are ready to review your own completed trades.</p>
            </div>
            <div className="demo-final-actions">
              <Link
                className="marketing-button primary"
                to={publicSampleAccount.ctas.registrationPath}
              onClick={() => {
                  trackSeoEvent("sample_demo_registration_started", PATH, publicSampleAccount.ctas.registrationLabel, "/register");
                  trackSeoCtaClick(PATH, publicSampleAccount.ctas.registrationLabel, "/register");
              }}
              >
                {publicSampleAccount.ctas.registrationLabel}
              </Link>
              <Link className="marketing-button ghost" to="/">Back home</Link>
            </div>
          </div>
        </div>
      </section>
    </MarketingShell>
  );
}

function DemoSummary() {
  return <>
    <div className="demo-panel-heading"><div><span>Performance summary</span><h2>Review the session before drawing a conclusion.</h2></div><em>Sample data</em></div>
    <div className="demo-metric-grid">
      <DemoMetric label="Completed trades" value={String(publicSampleAccount.dataTrust.completedTrades)} helper="Across the selected sample" />
      <DemoMetric label="Net P&L" value={publicSampleAccount.dataTrust.netPnl} helper="Historical sample only" tone="loss" />
      <DemoMetric label="Win rate" value={publicSampleAccount.account.winRate} helper="Historical sample only" />
      <DemoMetric label="Active session" value={publicSampleAccount.account.activeSession} helper="Session context" />
    </div>
    <div className="demo-data-trust-card">
      <div><span>Data Trust</span><strong>{publicSampleAccount.dataTrust.importSources}</strong></div>
      <p>{publicSampleAccount.dataTrust.confirmationPrompt}</p>
      <div className="demo-trust-actions"><button type="button">Yes, looks correct</button><button type="button">Something looks wrong</button></div>
    </div>
    <div className="demo-analysis-card">
      <div><span>Session analysis</span><h3>This is where most trading journals stop.</h3><p>{publicSampleAccount.account.dateRange} · {publicSampleAccount.account.instruments} · The next stage compares what happened after the two-loss trigger with the rest of this same fictional sample.</p></div>
      <a className="marketing-button ghost" href="#demo-leak" onClick={() => trackSeoEvent("sample_demo_leak_detector_viewed", PATH)}>View the evidence</a>
    </div>
  </>;
}

function DemoLeak() {
  return <>
    <div className="demo-panel-heading"><div><span>Leak Detector</span><h2>One pattern, supported by the selected trades.</h2></div><em>Sample data</em></div>
    <div className="demo-leak-card">
      <div className="demo-leak-topline"><span>Primary behavioral pattern</span><strong>{publicSampleAccount.primaryFinding.evidenceStrength} evidence</strong></div>
      <h3>{publicSampleAccount.primaryFinding.title}</h3>
      <p>After two losses, these trades averaged {publicSampleAccount.primaryFinding.triggered.result} versus {publicSampleAccount.primaryFinding.baseline.result} across the rest of this fictional sample.</p>
      <div className="demo-evidence-grid">
        <DemoMetric label="Qualifying occurrences" value={String(publicSampleAccount.primaryFinding.occurrenceCount)} helper="Selected pattern sample" />
        <DemoMetric label="Baseline" value={publicSampleAccount.primaryFinding.baseline.result} helper={publicSampleAccount.primaryFinding.baseline.label} />
        <DemoMetric label="Triggered" value={publicSampleAccount.primaryFinding.triggered.result} helper={publicSampleAccount.primaryFinding.triggered.label} tone="loss" />
        <DemoMetric label="Evidence strength" value={publicSampleAccount.primaryFinding.evidenceStrength} helper="10-19 repeated occurrences" />
      </div>
      <div className="demo-timeline" aria-label="Illustrative post-loss trade timeline">
        <strong>Illustrative trade timeline</strong>
        <p>The highlighted trades follow the two-loss trigger.</p>
        {publicSampleAccount.primaryFinding.timeline.map((trade) => <div className={trade.triggered ? "triggered" : ""} key={trade.label}><span>{trade.label}</span><span>{trade.time}</span><span>{trade.result}</span><em>{trade.triggered ? "After two-loss trigger" : "Trigger sequence"}</em></div>)}
      </div>
      <div className="demo-counterfactual">
        <div className="demo-counterfactual-heading"><span>Historical simulation</span><strong>If this sample had paused after the trigger:</strong></div>
        <div className="demo-impact-metric"><span>Losses avoided</span><strong>+{publicSampleAccount.primaryFinding.counterfactual.lossesAvoided}</strong></div>
        <div className="demo-impact-metric"><span>Winners forgone</span><strong>-{publicSampleAccount.primaryFinding.counterfactual.winnersForgone}</strong></div>
        <div className="demo-impact-metric net"><span>Net historical impact</span><strong>+{publicSampleAccount.primaryFinding.counterfactual.netHistoricalImpact}</strong></div>
        <p>Historical simulation based on this sample only. Not a prediction of future results.</p>
      </div>
      <div className="demo-evidence-method"><strong>Evidence strength</strong><span>0-4 <b>Insufficient</b></span><span>5-9 <b>Weak</b></span><span className="current"><b>10-19 Moderate</b><em>Current sample</em></span><span>20+ <b>Strong</b></span></div>
      <div className="demo-explanation"><strong>What this means</strong><p>In this sample, trades taken after two consecutive losses performed worse than the trader&apos;s normal baseline.</p><p>{sampleEvidenceMethod}</p><p>R values in this demo use a predefined $100 risk unit.</p></div>
      <a className="marketing-button primary" href="#demo-coach" onClick={() => trackSeoEvent("sample_demo_completed", PATH)}>See the coaching rule</a>
    </div>
  </>;
}

function DemoCoach() {
  return <>
    <div className="demo-panel-heading"><div><span>Coaching recommendation</span><h2>One thing to focus on next session.</h2></div><em>Sample data</em></div>
    <div className="demo-coach-card">
      <span>Sample rule</span>
      <h3>{publicSampleAccount.coach.rule}</h3>
      <p>{publicSampleAccount.coach.focus}</p>
      <div className="demo-coach-checklist"><span>Triggered trades averaged {publicSampleAccount.primaryFinding.triggered.result} vs {publicSampleAccount.primaryFinding.baseline.result} baseline</span><span>{publicSampleAccount.primaryFinding.occurrenceCount} qualifying occurrences — {evidenceLabel} evidence</span><span>Based on historical behavior, not a market prediction</span></div>
    </div>
  </>;
}

function DemoMetric({ label, value, helper, tone }: { label: string; value: string; helper: string; tone?: "loss" }) {
  return <div className={`demo-metric ${tone ?? ""}`}><span>{label}</span><strong>{value}</strong><small>{helper}</small></div>;
}
