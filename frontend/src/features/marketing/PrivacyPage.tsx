import Seo from "../../components/Seo";
import "./HomePage.css";
import { MarketingShell } from "./MarketingLayout";

const sections = [
  {
    title: "Information We Collect",
    body: "We collect account details such as your name, email address, authentication information, trading account labels, imported trade records, journal notes, and usage activity inside TradePilot.",
  },
  {
    title: "How We Use Your Information",
    body: "We use your information to provide the journal, analytics, AI review, coaching, billing, security, support, and product improvement features you request.",
  },
  {
    title: "Trading Data and Uploaded CSVs",
    body: "Uploaded CSVs and trade data are used to create your private trading records, analytics, calendars, session reviews, and performance insights. Your trading data is scoped to your authenticated account.",
  },
  {
    title: "AI Processing",
    body: "TradePilot may process trade details, notes, and derived analytics with AI systems to generate reviews, coaching summaries, Trading DNA, and session feedback. TradePilot is a journaling and analytics tool. It does not provide financial advice, investment recommendations, or trade signals.",
  },
  {
    title: "Payments and Stripe",
    body: "Paid subscriptions are processed by Stripe. TradePilot does not store full payment card numbers. Stripe may process payment details according to its own privacy and security terms.",
  },
  {
    title: "Data Security",
    body: "We use authentication, user-scoped access controls, secure token handling, and database-level safeguards designed to protect account and trading data.",
  },
  {
    title: "Account Deletion",
    body: "You may request account deletion by contacting support. Deletion requests may remove account data, imported trades, AI snapshots, and related records unless retention is required for legal, billing, or security reasons.",
  },
  {
    title: "Contact",
    body: "For privacy questions or account deletion requests, contact the TradePilot team through the support channel provided in the application.",
  },
];

export default function PrivacyPage() {
  return (
    <MarketingShell>
      <Seo
        title="TradePilot Privacy Policy | AI Trading Journal Data Privacy"
        description="Learn how TradePilot handles account data, imported trade records, AI processing, payments, and privacy for futures traders."
        path="/privacy"
      />
      <PolicyHero
        eyebrow="Privacy Policy"
        title="Privacy Policy"
        copy="TradePilot is built around private trading data. This page explains what we collect, why we use it, and how AI and payments fit into the product."
      />
      <PolicyContent sections={sections} />
    </MarketingShell>
  );
}

function PolicyHero({ eyebrow, title, copy }: { eyebrow: string; title: string; copy: string }) {
  return (
    <section className="policy-hero">
      <div className="marketing-container">
        <div className="marketing-eyebrow">{eyebrow}</div>
        <h1 className="marketing-title">{title}</h1>
        <p className="marketing-subtitle">{copy}</p>
      </div>
    </section>
  );
}

export function PolicyContent({ sections }: { sections: Array<{ title: string; body: string }> }) {
  return (
    <section className="policy-section">
      <div className="marketing-container policy-grid">
        {sections.map((section) => (
          <article className="policy-card" key={section.title}>
            <h2>{section.title}</h2>
            <p>{section.body}</p>
          </article>
        ))}
      </div>
    </section>
  );
}
