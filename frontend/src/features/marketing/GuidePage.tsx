import { Link } from "react-router-dom";
import Seo from "../../components/Seo";
import { pageStructuredData, type BreadcrumbItem } from "../../seo/structuredData";
import "./HomePage.css";
import { Breadcrumbs } from "./Breadcrumbs";
import type { GuideContent } from "./guideContent";
import { MarketingShell } from "./MarketingLayout";

export default function GuidePage({ guide }: { guide: GuideContent }) {
  const breadcrumbs: BreadcrumbItem[] = [
    { label: "Home", path: "/" },
    { label: "Resources", path: "/resources" },
    { label: guide.h1, path: guide.path },
  ];

  return <MarketingShell>
    <Seo title={guide.title} description={guide.description} path={guide.path} structuredData={pageStructuredData({ ...guide, breadcrumbs })} />
    <section className="marketing-hero guide-hero"><div className="marketing-container">
      <Breadcrumbs items={breadcrumbs} />
      <div className="guide-heading"><div className="marketing-eyebrow">{guide.eyebrow}</div><h1 className="marketing-title">{guide.h1}</h1><p className="marketing-subtitle hero-support">{guide.intro}</p></div>
      <nav className="guide-toc" aria-label="On this page"><strong>On this page</strong><ol>{guide.sections.map((section, index) => <li key={section.title}><a href={`#section-${index + 1}`}>{section.title}</a></li>)}</ol></nav>
    </div></section>
    <section className="marketing-section guide-content"><div className="marketing-container guide-layout"><article>
      {guide.checklist && <aside className="guide-callout"><h2>Before uploading</h2><ul>{guide.checklist.map((item) => <li key={item}>{item}</li>)}</ul></aside>}
      {guide.sections.map((section, index) => <section className="guide-section" id={`section-${index + 1}`} key={section.title}><h2>{section.title}</h2>{section.paragraphs.map((paragraph) => <p key={paragraph}>{paragraph}</p>)}{section.bullets && <ul>{section.bullets.map((bullet) => <li key={bullet}>{bullet}</li>)}</ul>}</section>)}
      {guide.source && <section className="guide-section"><h2>Source / platform documentation</h2><p><a href={guide.source.href} target="_blank" rel="noopener noreferrer">{guide.source.label}</a></p></section>}
      {guide.disclaimer && <p className="marketing-disclaimer">{guide.disclaimer}</p>}
    </article><aside className="guide-sidebar"><h2>Related resources</h2>{guide.relatedLinks.map((link) => <Link key={link.to} to={link.to}>{link.label}</Link>)}<Link to="/resources">All trading resources</Link><Link className="marketing-button primary" to="/register">{guide.primaryCta}</Link></aside></div></section>
  </MarketingShell>;
}
