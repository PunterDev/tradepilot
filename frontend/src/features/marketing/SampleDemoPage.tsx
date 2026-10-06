import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Seo from "../../components/Seo";
import { pageStructuredData, type BreadcrumbItem } from "../../seo/structuredData";
import { trackSeoCtaClick, trackSeoEvent } from "../../utils/seoAttribution";
import { Breadcrumbs } from "./Breadcrumbs";
import "./HomePage.css";
import { MarketingShell } from "./MarketingLayout";

const PATH = "/demo";
const TITLE = "See TradePilot Coach in Action | Sample Futures Trading Account";
const DESCRIPTION = "Explore an illustrative TradePilot Coach account with performance context, a behavioral leak, and a focused coaching rule. No signup or CSV required.";
const breadcrumbs: BreadcrumbItem[] = [{ label: "Home", path: "/" }, { label: "Sample demo", path: PATH }];

export default function SampleDemoPage() {
  const [activeView, setActiveView] = useState<"summary" | "leak" | "coach">("summary");

  useEffect(() => {
    trackSeoEvent("sample_demo_opened", PATH);
  }, []);

  function selectView(next: "summary" | "leak" | "coach") {
    setActiveView(next);
    if (next === "leak") trackSeoEvent("sample_demo_leak_detector_viewed", PATH);
    if (next === "coach") trackSeoEvent("sample_demo_completed", PATH);
  }

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
          </div>
        </div>
      </section>

      <section className="marketing-section demo-workspace-section">
        <div className="marketing-container">
          <div className="demo-disclaimer" role="note">Sample data only. Nothing on this page is a trade signal, recommendation, or promise of results.</div>
          <div className="demo-workspace">
            <aside className="demo-sidebar" aria-label="Sample TradePilot Coach workflow">
              <strong>Sample futures account</strong>
              <span>Illustrative NQ review</span>
              <button type="button" className={activeView === "summary" ? "active" : ""} onClick={() => selectView("summary")}>Performance summary</button>
              <button type="button" className={activeView === "leak" ? "active" : ""} onClick={() => selectView("leak")}>Leak Detector</button>
              <button type="button" className={activeView === "coach" ? "active" : ""} onClick={() => selectView("coach")}>Coaching rule</button>
            </aside>
            <div className="demo-content">
              {activeView === "summary" && <DemoSummary onViewLeak={() => selectView("leak")} />}
              {activeView === "leak" && <DemoLeak onViewCoach={() => selectView("coach")} />}
              {activeView === "coach" && <DemoCoach />}
            </div>
          </div>
          <div className="demo-final-cta">
            <div>
              <div className="marketing-eyebrow">Your own history</div>
              <h2>Ready to review your actual execution?</h2>
              <p>No broker password required for the supported CSV import workflow.</p>
            </div>
            <Link
              className="marketing-button primary"
              to="/register?source=sample-demo"
              onClick={() => {
                trackSeoEvent("sample_demo_registration_started", PATH, "Analyze My Own Trades", "/register");
                trackSeoCtaClick(PATH, "Analyze My Own Trades", "/register");
              }}
            >
              Analyze My Own Trades
            </Link>
          </div>
        </div>
      </section>
    </MarketingShell>
  );
}

function DemoSummary({ onViewLeak }: { onViewLeak: () => void }) {
  return <>
    <div className="demo-panel-heading"><div><span>Performance summary</span><h2>Review the session before drawing a conclusion.</h2></div><em>Sample data</em></div>
    <div className="demo-metric-grid">
      <DemoMetric label="Completed trades" value="42" helper="Across the selected sample" />
      <DemoMetric label="Average result" value="-0.18R" helper="Historical sample only" tone="loss" />
      <DemoMetric label="Most active window" value="9:30-11:00 ET" helper="Session context" />
      <DemoMetric label="Post-loss trades" value="14" helper="Available for comparison" />
    </div>
    <div className="demo-analysis-card">
      <div><span>Session analysis</span><h3>The later part of this sample contains more trades after losses than the earlier baseline.</h3><p>This is a starting point for review, not a diagnosis or prediction.</p></div>
      <button type="button" className="marketing-button ghost" onClick={onViewLeak}>View the evidence</button>
    </div>
  </>;
}

function DemoLeak({ onViewCoach }: { onViewCoach: () => void }) {
  return <>
    <div className="demo-panel-heading"><div><span>Leak Detector</span><h2>One pattern, supported by the selected trades.</h2></div><em>Sample data</em></div>
    <div className="demo-leak-card">
      <div className="demo-leak-topline"><span>Primary behavioral pattern</span><strong>Strong evidence</strong></div>
      <h3>Revenge Trading After Losses</h3>
      <p>In this illustrative account, trades placed after two consecutive losses performed worse than the earlier-session comparison group.</p>
      <div className="demo-evidence-grid">
        <DemoMetric label="Qualifying trades" value="9" helper="Selected pattern sample" />
        <DemoMetric label="Comparison" value="-0.62R vs -0.11R" helper="After-loss vs baseline average" tone="loss" />
        <DemoMetric label="Evidence strength" value="Strong" helper="Enough repeated observations to review" />
        <DemoMetric label="Estimated historical impact" value="Directional" helper="Not guaranteed future recovery" />
      </div>
      <div className="demo-explanation"><strong>What this means</strong><p>Evidence strength describes how much repeated historical support this pattern has. Estimated impact is directional: it summarizes the observed trades and does not predict future P&L.</p></div>
      <button type="button" className="marketing-button primary" onClick={onViewCoach}>See the coaching rule</button>
    </div>
  </>;
}

function DemoCoach() {
  return <>
    <div className="demo-panel-heading"><div><span>Coaching recommendation</span><h2>Turn the review into one guardrail for the next session.</h2></div><em>Sample data</em></div>
    <div className="demo-coach-card">
      <span>Sample rule</span>
      <h3>After two consecutive losses, pause trading for 20 minutes.</h3>
      <p>Use the pause to reset the plan, identify the next valid setup, and decide whether another trade still meets your rules.</p>
      <div className="demo-coach-checklist"><span>Based on historical behavior, not a market prediction</span><span>One observable rule to review after the next session</span><span>Adjust only after collecting more evidence</span></div>
    </div>
  </>;
}

function DemoMetric({ label, value, helper, tone }: { label: string; value: string; helper: string; tone?: "loss" }) {
  return <div className={`demo-metric ${tone ?? ""}`}><span>{label}</span><strong>{value}</strong><small>{helper}</small></div>;
}
