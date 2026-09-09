import { Link, useLocation } from "react-router-dom";
import Seo from "../../components/Seo";
import "./HomePage.css";
import { MarketingShell } from "./MarketingLayout";

export default function NotFoundPage() {
  const location = useLocation();

  return (
    <MarketingShell>
      <Seo
        title="Page Not Found | TradePilot"
        description="This TradePilot page could not be found. Explore the AI trading journal homepage or create a free account."
        path={location.pathname}
        robots="noindex,follow"
      />
      <section className="marketing-hero">
        <div className="marketing-container not-found-panel">
          <div className="marketing-eyebrow">404</div>
          <h1 className="marketing-title">This page is not in the journal.</h1>
          <p className="marketing-subtitle">
            The URL may have moved, or it may not be a public TradePilot page.
          </p>
          <div className="marketing-hero-actions">
            <Link className="marketing-button primary" to="/">Open Homepage</Link>
            <Link className="marketing-button ghost" to="/register">Start Journaling Free</Link>
          </div>
        </div>
      </section>
    </MarketingShell>
  );
}
