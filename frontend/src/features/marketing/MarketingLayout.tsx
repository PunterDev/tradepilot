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
              src="/assets/tradepilot-coach-logo.png"
              alt="TradePilot Coach"
              width={1120}
              height={340}
            />
          </span>
        </Link>
        <div className="marketing-nav-links">
          <a href="/#features">Features</a>
          <a href="/#how-it-works">How It Works</a>
          <a href="/#pricing">Pricing</a>
          <a href="/tools/trading-expectancy-calculator">Free Tools</a>
          <Link to="/resources">Resources</Link>
          <a href="/#faq">FAQ</a>
        </div>
        <div className="marketing-nav-actions">
          <Link className="marketing-button ghost" to="/login">Sign In</Link>
          <Link className="marketing-button primary" to="/register">Start Free</Link>
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
          <a className="marketing-button ghost" href="/#pricing">Pricing</a>
          <Link className="marketing-button ghost" to="/tools/trading-expectancy-calculator">Free Tools</Link>
          <Link className="marketing-button ghost" to="/resources">Resources</Link>
          <a className="marketing-button ghost" href="/#faq">FAQ</a>
          <Link className="marketing-button ghost" to="/login">Sign In</Link>
          <Link className="marketing-button primary" to="/register">Start Free</Link>
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
        <div className="marketing-footer-brand"><strong>TradePilot <span>Coach</span></strong><small>AI-powered trading performance and execution intelligence for futures traders.</small><em>{year}</em></div>
        <div className="marketing-footer-links">
          <div className="marketing-footer-group">
            <span>Product</span>
            <a href="/#features">Features</a>
            <a href="/#pricing">Pricing</a>
            <a href="/#faq">FAQ</a>
            <Link to="/login">Login</Link>
            <Link to="/register">Register</Link>
          </div>
          <div className="marketing-footer-group">
            <span>Tools</span>
            <Link to="/tools/trading-expectancy-calculator">Expectancy Calculator</Link>
            <Link to="/tools/overtrading-calculator">Overtrading Calculator</Link>
            <Link to="/tools/revenge-trading-calculator">Revenge Trading Calculator</Link>
          </div>
          <div className="marketing-footer-group">
            <span>Journals</span>
            <Link to="/futures-trading-journal">Futures Journal</Link>
            <Link to="/topstep-trading-journal">Topstep Journal</Link>
            <Link to="/tradovate-trading-journal">Tradovate Journal</Link>
            <Link to="/prop-firm-trading-journal">Prop Firm Journal</Link>
            <Link to="/ai-trading-journal">AI Journal</Link>
          </div>
          <div className="marketing-footer-group">
            <span>Behavior</span>
            <Link to="/trading-psychology">Trading Psychology</Link>
            <Link to="/revenge-trading">Revenge Trading</Link>
            <Link to="/overtrading">Overtrading</Link>
            <Link to="/trading-discipline">Trading Discipline</Link>
            <Link to="/breaking-trading-rules">Rule Breaking</Link>
            <Link to="/trading-after-losses">After Losses</Link>
          </div>
          <div className="marketing-footer-group">
            <span>Company</span>
            <Link to="/privacy">Privacy</Link>
            <Link to="/terms">Terms</Link>
            <Link to="/contact">Contact</Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
