import { Link } from "react-router-dom";
import Seo from "../../components/Seo";
import { pageStructuredData, type BreadcrumbItem } from "../../seo/structuredData";
import "./HomePage.css";
import { Breadcrumbs } from "./Breadcrumbs";
import { guides } from "./guideContent";
import { MarketingShell } from "./MarketingLayout";

const PATH = "/resources";
const TITLE = "Trading Journal Resources & Futures Trading Guides | TradePilot Coach";
const DESCRIPTION = "Practical futures trading-journal resources for CSV imports, completed-trade review, trading psychology, discipline, and execution analysis.";
const breadcrumbs: BreadcrumbItem[] = [{ label: "Home", path: "/" }, { label: "Resources", path: PATH }];

const groups = [
  { title: "Import & platform guides", links: [guides["/guides/how-to-export-topstep-trades"], guides["/guides/how-to-export-tradovate-trades"]] },
  { title: "Trading review guides", links: [guides["/guides/how-to-review-a-losing-trading-session"], guides["/guides/how-to-identify-overtrading-from-trade-history"]] },
  { title: "Trading journals", links: [{ path: "/ai-trading-journal", h1: "AI Trading Journal", description: "Review completed trades with execution and behavior context." }, { path: "/futures-trading-journal", h1: "Futures Trading Journal", description: "Organize futures trade history for a repeatable review." }, { path: "/topstep-trading-journal", h1: "Topstep Trading Journal", description: "Review supported Topstep order-history imports." }, { path: "/tradovate-trading-journal", h1: "Tradovate Trading Journal", description: "Review compatible Tradovate completed trade history." }, { path: "/prop-firm-trading-journal", h1: "Prop Firm Trading Journal", description: "Use completed trade history to inspect prop-firm workflows." }] },
  { title: "Behavior", links: [{ path: "/trading-psychology", h1: "Trading Psychology", description: "Review behavior through completed trade history." }, { path: "/revenge-trading", h1: "Revenge Trading", description: "Study post-loss behavior without assumptions." }, { path: "/overtrading", h1: "Overtrading", description: "Measure whether later trading differs from your normal context." }, { path: "/trading-discipline", h1: "Trading Discipline", description: "Turn repeatable observations into process guardrails." }, { path: "/breaking-trading-rules", h1: "Breaking Trading Rules", description: "Review rule adherence using completed trades." }, { path: "/trading-after-losses", h1: "Trading After Losses", description: "Compare decisions and results after losses." }] },
  { title: "Free tools", links: [{ path: "/tools/trading-expectancy-calculator", h1: "Trading Expectancy Calculator", description: "Calculate historical expected value per trade." }, { path: "/tools/overtrading-calculator", h1: "Overtrading Calculator", description: "Compare early and later completed trades." }, { path: "/tools/revenge-trading-calculator", h1: "Revenge Trading Calculator", description: "Compare baseline and post-loss trading history." }] },
];

export default function ResourcesPage() {
  return <MarketingShell><Seo title={TITLE} description={DESCRIPTION} path={PATH} structuredData={pageStructuredData({ path: PATH, title: TITLE, description: DESCRIPTION, breadcrumbs })} />
    <section className="marketing-hero resources-hero"><div className="marketing-container"><Breadcrumbs items={breadcrumbs} /><div className="guide-heading"><div className="marketing-eyebrow">TradePilot Coach resources</div><h1 className="marketing-title">Trading Resources for Reviewing Your Execution</h1><p className="marketing-subtitle hero-support">Practical resources for importing completed trade history, reviewing losing sessions, and measuring repeated execution behavior.</p></div></div></section>
    <section className="marketing-section"><div className="marketing-container resources-groups">{groups.map((group) => <section key={group.title}><h2>{group.title}</h2><div className="resources-grid">{group.links.map((link) => <Link className="resource-card" key={link.path} to={link.path}><h3>{link.h1}</h3><p>{link.description}</p><span>Explore resource</span></Link>)}</div></section>)}</div></section>
  </MarketingShell>;
}
