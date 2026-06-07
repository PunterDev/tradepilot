import "./HomePage.css";
import { MarketingShell } from "./MarketingLayout";
import { PolicyContent } from "./PrivacyPage";

const sections = [
  {
    title: "Use of TradePilot",
    body: "TradePilot provides journaling, analytics, AI review, coaching, and account-management tools for traders. You are responsible for maintaining accurate account information and using the service lawfully.",
  },
  {
    title: "No Financial Advice",
    body: "TradePilot is a journaling and analytics tool. It does not provide financial advice, investment recommendations, or trade signals.",
  },
  {
    title: "Trading Risk Disclaimer",
    body: "Trading futures and other financial products involves substantial risk. Analytics, AI summaries, and historical performance data do not guarantee future results or payouts.",
  },
  {
    title: "Subscriptions and Billing",
    body: "Paid plans may be billed through Stripe on a recurring basis. Pricing, plan limits, renewal timing, cancellation, and taxes may vary by plan and environment.",
  },
  {
    title: "User Data",
    body: "You retain responsibility for the trade records, notes, CSV uploads, and other data you submit. TradePilot uses this data to operate product features and generate requested insights.",
  },
  {
    title: "Acceptable Use",
    body: "You may not misuse TradePilot, attempt unauthorized access, interfere with security, scrape protected systems, or use the service to violate laws or third-party rights.",
  },
  {
    title: "Account Termination",
    body: "TradePilot may suspend or terminate accounts that violate these terms, present security risk, abuse service limits, or misuse billing and account systems.",
  },
  {
    title: "Limitation of Liability",
    body: "TradePilot is provided as a software tool. To the maximum extent permitted by law, TradePilot is not liable for trading losses, missed payouts, business interruption, or indirect damages.",
  },
  {
    title: "Contact",
    body: "Questions about these terms can be sent through the support channel provided in the application.",
  },
];

export default function TermsPage() {
  return (
    <MarketingShell>
      <section className="policy-hero">
        <div className="marketing-container">
          <div className="marketing-eyebrow">Terms of Service</div>
          <h1 className="marketing-title">Terms of Service</h1>
          <p className="marketing-subtitle">
            These terms explain the rules for using TradePilot, including AI analytics, subscriptions, trading-risk disclaimers, and acceptable use.
          </p>
        </div>
      </section>
      <PolicyContent sections={sections} />
    </MarketingShell>
  );
}
