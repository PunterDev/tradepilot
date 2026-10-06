import Seo from "../../components/Seo";
import "./HomePage.css";
import { MarketingShell } from "./MarketingLayout";

const sections = [
  {
    title: "Information We Collect",
    body: "We collect account details such as your name, email address, authentication information, trading account labels, imported trade records, journal notes, and usage activity inside TradePilot Coach.",
  },
  {
    title: "How We Use Your Information",
    body: "We use your information to provide the journal, analytics, AI review, coaching, billing, security, support, and product improvement features you request.",
  },
  {
    title: "Trading Data and Uploaded CSVs",
    body: "For supported CSV imports, TradePilot Coach does not need your broker password. Uploaded CSVs and trade data are used to create your private trading records, analytics, calendars, session reviews, and performance insights. Your trading data is scoped to your authenticated account and is not sold as a customer data product.",
  },
  {
    title: "AI Processing",
    body: "TradePilot Coach may process trade details, notes, and derived analytics with AI systems to generate reviews, coaching summaries, and session feedback. TradePilot Coach is a journaling and analytics tool. It does not provide financial advice, investment recommendations, or trade signals.",
  },
  {
    title: "Payments and Stripe",
    body: "Paid subscriptions are processed by Stripe. TradePilot Coach does not store full payment card numbers. Stripe may process payment details according to its own privacy and security terms.",
  },
  {
    title: "Data Security",
    body: "Account access is protected with authentication and account-scoped access controls designed to keep one user's trading records separate from another's. We use safeguards appropriate to an online software service, but no internet service can promise absolute security.",
  },
  {
    title: "No Trade Execution",
    body: "TradePilot Coach is analytical and educational software. It does not connect to your broker to place trades, copy trades, or send trade instructions.",
  },
  {
    title: "Account Deletion",
    body: "You may request account deletion by contacting support. Deletion requests may remove account data, imported trades, AI snapshots, and related records unless retention is required for legal, billing, or security reasons.",
  },
  {
    title: "Contact",
    body: "For privacy questions or account deletion requests, contact the TradePilot Coach team through the support channel provided in the application.",
  },
];

export default function PrivacyPage() {
  return (
    <MarketingShell>
      <Seo
        title="TradePilot Coach Privacy Policy | AI Trading Journal Data Privacy"
        description="Learn how TradePilot Coach handles account data, imported trade records, AI processing, payments, and privacy for futures traders."
        path="/privacy"
      />
      <PolicyHero
        eyebrow="Privacy Policy"
        title="Privacy Policy"
        copy="TradePilot Coach is built around private trading data. This page explains what we collect, why we use it, and how AI and payments fit into the product."
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
