import { renderToString } from "react-dom/server";
import { StaticRouter } from "react-router";
import { HelmetProvider, type HelmetServerState } from "react-helmet-async";
import { SeoLandingPage, seoLandingPages } from "./features/marketing/SeoLandingPage";
import HomePage from "./features/marketing/HomePage";
import ContactPage from "./features/marketing/ContactPage";
import PrivacyPage from "./features/marketing/PrivacyPage";
import TermsPage from "./features/marketing/TermsPage";
import TradingExpectancyCalculatorPage from "./features/marketing/TradingExpectancyCalculatorPage";
import OvertradingCalculatorPage from "./features/marketing/OvertradingCalculatorPage";
import RevengeTradingCalculatorPage from "./features/marketing/RevengeTradingCalculatorPage";
import GuidePage from "./features/marketing/GuidePage";
import { guides } from "./features/marketing/guideContent";
import ResourcesPage from "./features/marketing/ResourcesPage";

const directPages: Record<string, () => React.JSX.Element> = {
  "/": HomePage,
  "/contact": ContactPage,
  "/privacy": PrivacyPage,
  "/terms": TermsPage,
  "/resources": ResourcesPage,
  "/tools/trading-expectancy-calculator": TradingExpectancyCalculatorPage,
  "/tools/overtrading-calculator": OvertradingCalculatorPage,
  "/tools/revenge-trading-calculator": RevengeTradingCalculatorPage,
};

export function renderPublicRoute(pathname: string) {
  const DirectPage = directPages[pathname];
  const landingPage = seoLandingPages[pathname];
  const guide = guides[pathname];

  if (!DirectPage && !landingPage && !guide) {
    throw new Error(`Unsupported public prerender route: ${pathname}`);
  }

  const helmetContext: { helmet?: HelmetServerState | null } = {};
  const page = DirectPage ? <DirectPage /> : guide ? <GuidePage guide={guide} /> : <SeoLandingPage content={landingPage} />;
  const rawBody = renderToString(
    <HelmetProvider context={helmetContext}>
      <StaticRouter location={pathname}>{page}</StaticRouter>
    </HelmetProvider>,
  );
  const helmet = helmetContext.helmet;
  const embeddedHeadTags = /<title\b[^>]*>[\s\S]*?<\/title>|<meta\b[^>]*\/?>|<link\b[^>]*\/?>|<script\b[^>]*>[\s\S]*?<\/script>/gi;
  const embeddedHead = rawBody.match(embeddedHeadTags)?.join("") ?? "";
  const body = rawBody.replace(embeddedHeadTags, "");
  const rawHead = helmet
    ? `${helmet.title.toString()}${helmet.meta.toString()}${helmet.link.toString()}${helmet.script.toString()}`
    : embeddedHead;
  const head = rawHead.replace(/<(meta|link)(\b[^>]*)\/>/gi, "<$1$2>");

  if (!head) {
    throw new Error(`Missing SEO head for public prerender route: ${pathname}`);
  }

  return {
    body,
    head,
  };
}
