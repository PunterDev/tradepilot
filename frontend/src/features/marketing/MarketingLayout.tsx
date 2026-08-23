import { useState, type ReactNode } from "react";
import { Link } from "react-router-dom";

type MarketingShellProps = {
  children: ReactNode;
};

export function MarketingShell({ children }: MarketingShellProps) {
  return (
    <main className="marketing-page">
      <MarketingNavbar />
      {children}
      <MarketingFooter />
    </main>
  );
}

export function MarketingNavbar() {
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <nav className="marketing-nav">
      <div className="marketing-container marketing-nav-inner">
        <Link className="marketing-logo" to="/">
          <span className="marketing-navbar-logo">
            <img
              src="/assets/tradepilot-logo-navbar.png"
              alt="TradePilot"
              width={1460}
              height={430}
            />
          </span>
        </Link>
        <div className="marketing-nav-links">
          <a href="/#features">Features</a>
          <a href="/#how-it-works">How It Works</a>
          <a href="/#ai-coach">AI Coach</a>
          <a href="/#pricing">Pricing</a>
          <a href="/#faq">FAQ</a>
        </div>
        <div className="marketing-nav-actions">
          <Link className="marketing-button ghost" to="/login">Login</Link>
          <Link className="marketing-button primary" to="/register">Get Started</Link>
        </div>
        <button
          type="button"
          className="marketing-mobile-toggle"
          aria-label="Toggle navigation"
          onClick={() => setMobileOpen((current) => !current)}
        >
          Menu
        </button>
      </div>
      <div className="marketing-container">
        <div className={`marketing-mobile-panel ${mobileOpen ? "open" : ""}`}>
          <a className="marketing-button ghost" href="/#features">Features</a>
          <a className="marketing-button ghost" href="/#how-it-works">How It Works</a>
          <a className="marketing-button ghost" href="/#ai-coach">AI Coach</a>
          <a className="marketing-button ghost" href="/#pricing">Pricing</a>
          <a className="marketing-button ghost" href="/#faq">FAQ</a>
          <Link className="marketing-button ghost" to="/login">Login</Link>
          <Link className="marketing-button primary" to="/register">Get Started</Link>
        </div>
      </div>
    </nav>
  );
}

export function MarketingFooter() {
  const year = new Date().getFullYear();

  return (
    <footer className="marketing-footer">
      <div className="marketing-container marketing-footer-inner">
        <div>TradePilot {year}</div>
        <div className="marketing-footer-links">
          <a href="/#features">Features</a>
          <a href="/#faq">FAQ</a>
          <Link to="/login">Login</Link>
          <Link to="/register">Register</Link>
          <a href="/#pricing">Pricing</a>
          <Link to="/privacy">Privacy</Link>
          <Link to="/terms">Terms</Link>
          <Link to="/contact">Contact</Link>
        </div>
      </div>
    </footer>
  );
}
