import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { expect, test, type Page } from "@playwright/test";

const siteUrl = "https://www.tradepilotcoach.com";
const expectancyCalculatorPath = "/tools/trading-expectancy-calculator";
const expectancyCalculatorTitle = "Trading Expectancy Calculator for Futures Traders | TradePilot Coach";
const expectancyCalculatorDescription =
  "Calculate your trading expectancy using win rate, average win and average loss. See your expected value per trade and understand what your trading performance means.";
const overtradingCalculatorPath = "/tools/overtrading-calculator";
const overtradingCalculatorTitle = "Overtrading Calculator: Compare Early vs Later Trades | TradePilot Coach";
const overtradingCalculatorDescription =
  "Compare your early trades with later trades to see whether your win rate, expectancy and P&L deteriorate as you continue trading.";
const revengeCalculatorPath = "/tools/revenge-trading-calculator";
const revengeCalculatorTitle = "Revenge Trading Calculator: Analyze Performance After Losses | TradePilot Coach";
const revengeCalculatorDescription =
  "Compare your normal trading performance with trades taken after losses and consecutive losses to see whether your results deteriorate after losing trades.";
const calculatorCta = "Find My Trading Leaks";
const publicSeoRoutes: string[] = JSON.parse(readFileSync(path.resolve("seo-public-routes.json"), "utf8"));
const privateSitemapPaths = [
  "/login",
  "/register",
  "/dashboard",
  "/admin",
  "/billing",
  "/settings",
  "/journal",
  "/analytics",
  "/calendar",
  "/coach",
  "/trades/",
  "/verify-email",
  "/forgot-password",
  "/reset-password",
];

function sourceForPath(pathname: string) {
  if (pathname === expectancyCalculatorPath) return "expectancy-calculator";
  return pathname.replace(/^\//, "").replace(/\//g, "-").replace(/^tools-/, "");
}

function registerUrlForPath(pathname: string) {
  return `/register?source=${sourceForPath(pathname)}`;
}

async function mockRegistrationAndLogin(page: Page) {
  await page.route(/.*\/api(?:\/v1)?\/auth\/register$/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ message: "Registration successful. Please verify your email before logging in." }),
    });
  });
  await page.route(/.*\/api(?:\/v1)?\/auth\/login$/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        token: "test-token",
        userId: 7,
        name: "Tool Visitor",
        email: "tool@example.com",
        role: "USER",
        plan: "FREE",
      }),
    });
  });
}

async function fillAuthForm(page: Page, mode: "register" | "login") {
  if (mode === "register") {
    await page.getByLabel("Name").fill("Tool Visitor");
  }
  await page.getByLabel("Email").fill("tool@example.com");
  await page.getByLabel("Password").fill("password123");
  await page.getByRole("button", { name: mode === "register" ? "Create account" : "Log in" }).click();
}

async function fillRevengeExample(page: Page) {
  await page.locator("#baselineTrades").fill("40");
  await page.locator("#baselineWins").fill("24");
  await page.locator("#baselineWinPnl").fill("4800");
  await page.locator("#baselineLossPnl").fill("2400");
  await page.locator("#after1Trades").fill("24");
  await page.locator("#after1Wins").fill("12");
  await page.locator("#after1WinPnl").fill("2040");
  await page.locator("#after1LossPnl").fill("1800");
  await page.locator("#after2Trades").fill("18");
  await page.locator("#after2Wins").fill("6");
  await page.locator("#after2WinPnl").fill("1080");
  await page.locator("#after2LossPnl").fill("2160");
}

const landingPages = [
  {
    path: "/futures-trading-journal",
    keyword: "futures trading journal",
    title: "Futures Trading Journal With AI Coaching | TradePilot Coach",
    description:
      "Journal NQ, MNQ, ES and MES futures trades, uncover costly trading patterns, review sessions and get AI-powered coaching with TradePilot Coach.",
    h1: "A Trading Journal Built for Futures Traders",
    cta: "Analyze My Futures Trades",
    links: [
      { label: "Topstep trading journal", to: "/topstep-trading-journal" },
      { label: "Tradovate trading journal", to: "/tradovate-trading-journal" },
      { label: "Prop firm trading journal", to: "/prop-firm-trading-journal" },
      { label: "AI trading journal", to: "/ai-trading-journal" },
      { label: "Revenge trading", to: "/revenge-trading" },
      { label: "Overtrading", to: "/overtrading" },
    ],
  },
  {
    path: "/topstep-trading-journal",
    keyword: "Topstep trading journal",
    title: "Topstep Trading Journal & AI Trade Analysis | TradePilot Coach",
    description:
      "Import your Topstep trade history into TradePilot Coach to analyze performance, trading behavior, sessions and recurring mistakes.",
    h1: "AI Trading Journal for Topstep Traders",
    cta: "Import My Topstep History",
    links: [
      { label: "Futures trading journal", to: "/futures-trading-journal" },
      { label: "Prop firm trading journal", to: "/prop-firm-trading-journal" },
      { label: "AI trading journal", to: "/ai-trading-journal" },
      { label: "Trading after losses", to: "/trading-after-losses" },
    ],
  },
  {
    path: "/tradovate-trading-journal",
    keyword: "Tradovate trading journal",
    title: "Tradovate Trading Journal With AI Insights | TradePilot Coach",
    description:
      "Import Tradovate futures trades into TradePilot Coach and uncover patterns in your performance, risk, sessions and trading behavior.",
    h1: "Turn Your Tradovate Trades Into Actionable Insights",
    cta: "Analyze My Tradovate Trades",
    links: [
      { label: "Futures trading journal", to: "/futures-trading-journal" },
      { label: "Prop firm trading journal", to: "/prop-firm-trading-journal" },
      { label: "AI trading journal", to: "/ai-trading-journal" },
      { label: "Trading psychology", to: "/trading-psychology" },
    ],
  },
  {
    path: "/prop-firm-trading-journal",
    keyword: "prop firm trading journal",
    title: "Prop Firm Trading Journal for Futures Traders | TradePilot Coach",
    description:
      "Analyze prop-firm trading performance, rule-breaking patterns, overtrading and costly behavior with TradePilot Coach's AI-powered trading journal.",
    h1: "A Trading Journal Built for Prop Firm Traders",
    cta: "Find My Biggest Trading Leak",
    links: [
      { label: "Topstep trading journal", to: "/topstep-trading-journal" },
      { label: "Futures trading journal", to: "/futures-trading-journal" },
      { label: "AI trading journal", to: "/ai-trading-journal" },
      { label: "Trading discipline", to: "/trading-discipline" },
      { label: "Breaking trading rules", to: "/breaking-trading-rules" },
    ],
  },
  {
    path: "/ai-trading-journal",
    keyword: "AI trading journal",
    title: "AI Trading Journal for Futures Traders | TradePilot Coach",
    description:
      "TradePilot Coach uses AI to analyze completed trades, identify behavioral patterns and help futures traders review what is actually hurting their performance.",
    h1: "AI That Analyzes Your Trading - Not Predicts It",
    cta: "Analyze My Trading Free",
    links: [
      { label: "Futures trading journal", to: "/futures-trading-journal" },
      { label: "Topstep trading journal", to: "/topstep-trading-journal" },
      { label: "Tradovate trading journal", to: "/tradovate-trading-journal" },
      { label: "Prop firm trading journal", to: "/prop-firm-trading-journal" },
      { label: "Trading psychology", to: "/trading-psychology" },
    ],
  },
  {
    path: "/trading-psychology",
    keyword: "trading psychology",
    title: "Trading Psychology You Can Actually Measure | TradePilot Coach",
    description:
      "Learn how futures traders can measure revenge trading, overtrading, rule breaking, discipline issues and post-loss behavior using completed trade history.",
    h1: "Trading Psychology You Can Actually Measure",
    cta: "Measure My Trading Behavior",
    links: [
      { label: "Revenge trading", to: "/revenge-trading" },
      { label: "Overtrading", to: "/overtrading" },
      { label: "Trading discipline", to: "/trading-discipline" },
      { label: "Breaking trading rules", to: "/breaking-trading-rules" },
      { label: "Trading after losses", to: "/trading-after-losses" },
      { label: "AI trading journal", to: "/ai-trading-journal" },
    ],
  },
  {
    path: "/revenge-trading",
    keyword: "revenge trading",
    title: "Revenge Trading: How to Identify and Measure It | TradePilot Coach",
    description:
      "Learn what revenge trading looks like, how to identify it in your futures trade history, and how to measure what trading after losses may be costing you.",
    h1: "Revenge Trading: Stop Guessing and Measure the Pattern",
    cta: "Check My Trades for Revenge Trading",
    links: [
      { label: "Trading after losses", to: "/trading-after-losses" },
      { label: "Trading discipline", to: "/trading-discipline" },
      { label: "Breaking trading rules", to: "/breaking-trading-rules" },
      { label: "AI trading journal", to: "/ai-trading-journal" },
    ],
  },
  {
    path: "/overtrading",
    keyword: "overtrading",
    title: "Overtrading: Signs, Causes & How to Measure It | TradePilot Coach",
    description:
      "Learn the signs of overtrading, why trade count alone is not enough, and how futures traders can measure whether too many trades are hurting performance.",
    h1: "Are You Losing Money Because You Trade Too Much?",
    cta: "Analyze My Overtrading Pattern",
    links: [
      { label: "Overtrading calculator", to: "/tools/overtrading-calculator" },
      { label: "Trading discipline", to: "/trading-discipline" },
      { label: "Breaking trading rules", to: "/breaking-trading-rules" },
      { label: "Futures trading journal", to: "/futures-trading-journal" },
      { label: "Trading after losses", to: "/trading-after-losses" },
    ],
  },
  {
    path: "/trading-discipline",
    keyword: "trading discipline",
    title: "Trading Discipline: Measure It Instead of Relying on Willpower | TradePilot Coach",
    description:
      "Learn how to improve trading discipline by measuring rule adherence, session behavior, sizing consistency and repeated decision patterns.",
    h1: "Trading Discipline Becomes Easier to Improve When You Can Measure It",
    cta: "Measure My Trading Behavior",
    links: [
      { label: "Revenge trading", to: "/revenge-trading" },
      { label: "Overtrading", to: "/overtrading" },
      { label: "Breaking trading rules", to: "/breaking-trading-rules" },
      { label: "Trading psychology", to: "/trading-psychology" },
    ],
  },
  {
    path: "/breaking-trading-rules",
    keyword: "breaking trading rules",
    title: "Why Traders Keep Breaking Their Own Rules | TradePilot Coach",
    description:
      "Understand why traders break trading rules and how to measure the trigger, violation, cost and adjustment in a futures trading journal.",
    h1: "You Know the Rule. Why Do You Keep Breaking It?",
    cta: "Find My Most Expensive Rule Break",
    links: [
      { label: "Trading discipline", to: "/trading-discipline" },
      { label: "Revenge trading", to: "/revenge-trading" },
      { label: "Trading after losses", to: "/trading-after-losses" },
      { label: "Overtrading", to: "/overtrading" },
      { label: "Trading psychology", to: "/trading-psychology" },
    ],
  },
  {
    path: "/trading-after-losses",
    keyword: "trading after losses",
    title: "Trading After Losses: What Happens to Your Performance? | TradePilot Coach",
    description:
      "Learn how to analyze trading after losses by comparing baseline trades, trades after one loss, and trades after consecutive losses.",
    h1: "What Happens to Your Trading After You Lose?",
    cta: "Analyze My Post-Loss Performance",
    links: [
      { label: "Revenge trading", to: "/revenge-trading" },
      { label: "Overtrading", to: "/overtrading" },
      { label: "Futures trading journal", to: "/futures-trading-journal" },
      { label: "Breaking trading rules", to: "/breaking-trading-rules" },
    ],
  },
];

for (const content of landingPages) {
  test(`${content.path} renders SEO metadata and primary conversion paths`, async ({ page }) => {
    await page.goto(content.path);

    await expect(page.getByRole("heading", { level: 1, name: content.h1 })).toBeVisible();
    await expect(page).toHaveTitle(content.title);

    await expect(page.locator('meta[name="description"]')).toHaveAttribute("content", content.description);
    await expect(page.locator('link[rel="canonical"]')).toHaveAttribute("href", `${siteUrl}${content.path}`);
    await expect(page.locator('meta[property="og:url"]')).toHaveAttribute("content", `${siteUrl}${content.path}`);
    await expect(page.locator('meta[name="twitter:card"]')).toHaveAttribute("content", "summary_large_image");
    await expect(page.locator('meta[name="robots"]')).toHaveCount(0);
    await expect(page.locator("h1")).toHaveCount(1);
    await expect(page.getByRole("link", { name: content.cta }).first()).toHaveAttribute("href", registerUrlForPath(content.path));

    for (const link of content.links) {
      await expect(page.getByRole("link", { name: link.label }).first()).toHaveAttribute("href", link.to);
    }

    const schemas = await page.locator('script[type="application/ld+json"]').evaluateAll((nodes) =>
      nodes.map((node) => JSON.parse(node.textContent ?? "{}"))
    );
    expect(schemas.length).toBeGreaterThan(0);
    expect(JSON.stringify(schemas)).toContain("SoftwareApplication");
    expect(JSON.stringify(schemas)).toContain("FAQPage");
  });
}

for (const route of publicSeoRoutes) {
  test(`${route} remains a publicly indexable route`, async ({ page }) => {
    const response = await page.goto(route, { waitUntil: "domcontentloaded" });
    expect(response?.ok()).toBeTruthy();

    await expect(page).not.toHaveTitle("");
    await expect(page.locator('meta[name="description"]')).toHaveCount(1);
    await expect(page.locator('meta[name="description"]')).toHaveAttribute("content", /\S/);
    await expect(page.locator('link[rel="canonical"]')).toHaveCount(1);

    const canonical = await page.locator('link[rel="canonical"]').getAttribute("href");
    expect(canonical).toBeTruthy();
    const canonicalUrl = new URL(canonical!);
    expect(canonicalUrl.origin).toBe(siteUrl);
    expect(canonicalUrl.pathname).toBe(route);
    expect(canonicalUrl.search).toBe("");
    expect(canonicalUrl.hash).toBe("");

    await expect(page.locator("h1")).toHaveCount(1);
    await expect(page.locator("h1")).toHaveText(/\S/);
    const robots = await page.locator('meta[name="robots"]').evaluateAll((nodes) =>
      nodes.map((node) => node.getAttribute("content") ?? "")
    );
    expect(robots.join(" ").toLowerCase()).not.toContain("noindex");
  });
}

test("SEO landing pages preserve first-touch attribution and CTA events", async ({ page }) => {
  await page.goto("/futures-trading-journal?utm_source=google&utm_medium=organic&utm_campaign=futures");
  await page.waitForFunction(() => window.localStorage.getItem("tradepilot_first_touch_attribution") !== null);

  const attribution = await page.evaluate(() =>
    JSON.parse(window.localStorage.getItem("tradepilot_first_touch_attribution") ?? "{}")
  );
  expect(attribution.landing_page).toBe("/futures-trading-journal");
  expect(attribution.utm_source).toBe("google");
  expect(attribution.utm_medium).toBe("organic");
  expect(attribution.utm_campaign).toBe("futures");

  await page.getByRole("link", { name: "Analyze My Futures Trades" }).first().click();
  await expect(page).toHaveURL(/\/register\?source=futures-trading-journal$/);

  const events = await page.evaluate(() =>
    JSON.parse(window.localStorage.getItem("tradepilot_seo_events") ?? "[]")
  );
  expect(events.map((event: { event: string }) => event.event)).toEqual(
    expect.arrayContaining(["seo_landing_page_view", "seo_cta_click", "signup_started_from_seo"])
  );
});

test("anonymous expectancy-tool CTA routes to register with safe source only", async ({ page }) => {
  await page.goto(`${expectancyCalculatorPath}?winRate=55&avgWin=250&avgLoss=150&trades=40&utm_source=google`);

  await page.getByRole("link", { name: calculatorCta }).click();

  await expect(page).toHaveURL(/\/register\?source=expectancy-calculator$/);
  expect(page.url()).not.toContain("winRate");
  expect(page.url()).not.toContain("avgWin");
  expect(page.url()).not.toContain("avgLoss");
  expect(page.url()).not.toContain("trades=");
});

test("calculator links reset scroll position when moving between public tool pages", async ({ page }) => {
  await page.goto(overtradingCalculatorPath);
  await expect(page.getByRole("heading", { level: 1, name: "Overtrading Calculator" })).toBeVisible();
  const footerExpectancyLink = page.locator("footer").getByRole("link", { name: "Expectancy Calculator" });
  await footerExpectancyLink.scrollIntoViewIfNeeded();
  await expect.poll(() => page.evaluate(() => window.scrollY)).toBeGreaterThan(500);

  await footerExpectancyLink.click();

  await expect(page).toHaveURL(new RegExp(`${expectancyCalculatorPath}$`));
  await expect(page.getByRole("heading", { level: 1, name: "Trading Expectancy Calculator" })).toBeVisible();
  await expect.poll(() => page.evaluate(() => window.scrollY)).toBeLessThan(20);
});

test("route scroll restoration preserves public hash navigation", async ({ page }) => {
  await page.goto(revengeCalculatorPath);
  await expect(page.getByRole("heading", { level: 1, name: "Revenge Trading Calculator" })).toBeVisible();
  const footerPricingLink = page.locator("footer").getByRole("link", { name: "Pricing" });
  await footerPricingLink.scrollIntoViewIfNeeded();
  await expect.poll(() => page.evaluate(() => window.scrollY)).toBeGreaterThan(500);

  await footerPricingLink.click();

  await expect(page).toHaveURL(/\/#pricing$/);
  await expect(page.locator("#pricing")).toBeVisible();
  const pricingTop = await page.locator("#pricing").evaluate((element) => element.getBoundingClientRect().top);
  expect(pricingTop).toBeGreaterThanOrEqual(80);
  expect(pricingTop).toBeLessThan(180);
});

test("authenticated expectancy-tool CTA points directly to activation import", async ({ page }) => {
  await page.addInitScript(() => {
    window.localStorage.setItem("tradepilot_token", "test-token");
    window.localStorage.setItem(
      "tradepilot_user",
      JSON.stringify({ id: 7, name: "Tool Visitor", email: "tool@example.com", role: "USER", plan: "FREE" })
    );
  });

  await page.goto(`${expectancyCalculatorPath}?__prerender=1`);
  await expect(page.getByRole("link", { name: calculatorCta })).toHaveAttribute("href", "/dashboard?activation=import");
});

test("registration preserves public tool source and post-login routes to activation import", async ({ page }) => {
  await mockRegistrationAndLogin(page);
  await page.goto("/register?source=expectancy-calculator");

  await fillAuthForm(page, "register");
  await expect(page.getByText("Registration successful. Please verify your email before logging in.")).toBeVisible();

  const pendingSource = await page.evaluate(() => window.localStorage.getItem("tradepilot_pending_activation_source"));
  expect(pendingSource).toBe("expectancy-calculator");

  await fillAuthForm(page, "login");
  await expect(page).toHaveURL(/\/dashboard\?activation=import$/);
});

test("direct normal registration without source retains dashboard behavior", async ({ page }) => {
  await mockRegistrationAndLogin(page);
  await page.goto("/register");

  await fillAuthForm(page, "register");
  const pendingSource = await page.evaluate(() => window.localStorage.getItem("tradepilot_pending_activation_source"));
  expect(pendingSource).toBeNull();

  await fillAuthForm(page, "login");
  await expect(page).toHaveURL(/\/dashboard$/);
});

test("sitemap exactly matches the public SEO registry and excludes private or auth routes", async ({ request }) => {
  const response = await request.get("/sitemap.xml");
  expect(response.ok()).toBeTruthy();
  const sitemap = await response.text();
  const sitemapUrls = [...sitemap.matchAll(/<loc>\s*([^<]+?)\s*<\/loc>/gi)].map((match) => match[1].trim());
  const sitemapPaths = sitemapUrls.map((url) => new URL(url).pathname);

  expect(new Set(publicSeoRoutes).size).toBe(publicSeoRoutes.length);
  expect(new Set(sitemapUrls).size).toBe(sitemapUrls.length);
  expect([...sitemapPaths].sort()).toEqual([...publicSeoRoutes].sort());

  for (const privatePath of privateSitemapPaths) {
    expect(sitemapPaths.some((path) => path === privatePath || path.startsWith(privatePath))).toBeFalsy();
  }
});

test("revenge trading calculator renders metadata, content and structured data", async ({ page }) => {
  await page.goto(revengeCalculatorPath);

  await expect(page.getByRole("heading", { level: 1, name: "Revenge Trading Calculator" })).toBeVisible();
  await expect(page).toHaveTitle(revengeCalculatorTitle);
  await expect(page.locator('meta[name="description"]')).toHaveAttribute("content", revengeCalculatorDescription);
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute("href", `${siteUrl}${revengeCalculatorPath}`);
  await expect(page.locator('meta[property="og:url"]')).toHaveAttribute("content", `${siteUrl}${revengeCalculatorPath}`);
  await expect(page.locator('meta[name="robots"]')).toHaveCount(0);
  await expect(page.locator("h1")).toHaveCount(1);
  await expect(page.locator(".overtrading-group-card legend").filter({ hasText: "Baseline Trades" })).toBeVisible();
  await expect(page.locator(".overtrading-group-card legend").filter({ hasText: "After 1 Loss" })).toBeVisible();
  await expect(page.locator(".overtrading-group-card legend").filter({ hasText: "After 2 Losses" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Analyze Post-Loss Performance" })).toBeVisible();
  await expect(page.getByText("Compare Your Post-Loss Performance")).toBeVisible();
  await expect(page.getByText("Baseline trades is required.")).toHaveCount(0);
  await expect(page.getByText("What Is Revenge Trading?")).toBeVisible();
  await expect(page.getByText("Post-Loss Deterioration Is Not Automatically Revenge Trading")).toBeVisible();
  await expect(page.getByRole("link", { name: calculatorCta })).toHaveAttribute("href", registerUrlForPath(revengeCalculatorPath));

  for (const [label, href] of [
    ["Revenge trading", "/revenge-trading"],
    ["Trading after losses", "/trading-after-losses"],
    ["Compare early and later trades", "/tools/overtrading-calculator"],
    ["Trading expectancy calculator", "/tools/trading-expectancy-calculator"],
    ["Futures trading journal", "/futures-trading-journal"],
    ["AI trading journal", "/ai-trading-journal"],
  ] as const) {
    await expect(page.getByRole("link", { name: label }).first()).toHaveAttribute("href", href);
  }

  const schemas = await page.locator('script[type="application/ld+json"]').evaluateAll((nodes) =>
    nodes.map((node) => JSON.parse(node.textContent ?? "{}"))
  );
  expect(JSON.stringify(schemas)).toContain("WebApplication");
  expect(JSON.stringify(schemas)).toContain("BreadcrumbList");
});

test("revenge trading calculator uses three desktop input groups without immediate required errors", async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto(revengeCalculatorPath);

  const groups = page.locator(".revenge-calculator-card .overtrading-group-card");
  await expect(groups).toHaveCount(3);

  const boxes = await groups.evaluateAll((elements) =>
    elements.map((element) => {
      const box = element.getBoundingClientRect();
      return { x: box.x, y: box.y, width: box.width };
    })
  );
  expect(Math.max(...boxes.map((box) => box.y)) - Math.min(...boxes.map((box) => box.y))).toBeLessThan(12);
  expect(boxes[1].x).toBeGreaterThan(boxes[0].x + boxes[0].width / 2);
  expect(boxes[2].x).toBeGreaterThan(boxes[1].x + boxes[1].width / 2);
  await expect(page.getByText("Baseline trades is required.")).toHaveCount(0);

  await page.getByRole("button", { name: "Analyze Post-Loss Performance" }).click();
  await expect(page.getByText("Baseline trades is required.")).toBeVisible();
  await expect(page.locator("#baselineTrades")).toBeFocused();
  await expect(page.getByTestId("revenge-analysis-result")).toContainText("Compare Your Post-Loss Performance");
});

test("revenge trading calculator computes post-loss example metrics", async ({ page }) => {
  await page.goto(revengeCalculatorPath);
  await fillRevengeExample(page);
  await expect(page.locator(".revenge-sequence")).toHaveCount(0);
  await page.getByRole("button", { name: "Analyze Post-Loss Performance" }).click();

  await expect(page.locator(".revenge-sequence div").filter({ hasText: "Baseline" })).toContainText("+$60.00/trade");
  await expect(page.locator(".revenge-sequence div").filter({ hasText: "After 1 Loss" })).toContainText("+$10.00/trade");
  await expect(page.locator(".revenge-sequence div").filter({ hasText: "After 2 Losses" })).toContainText("-$60.00/trade");
  await expect(page.locator(".expectancy-result-grid div").filter({ hasText: "Expectancy Change After 1 Loss" })).toContainText("-$50.00/trade");
  await expect(page.locator(".expectancy-result-grid div").filter({ hasText: "Expectancy Change After 2 Losses" })).toContainText("-$120.00/trade");
  await expect(page.locator(".expectancy-monthly").filter({ hasText: "Estimated Performance Gap After 1 Loss" })).toContainText("$1,200");
  await expect(page.locator(".expectancy-monthly").filter({ hasText: "Estimated Performance Gap After 2 Losses" })).toContainText("$2,160");
  await expect(page.getByText("Stronger deterioration appears after consecutive losses.")).toBeVisible();
  await expect(page.getByTestId("revenge-analysis-result")).toBeFocused();
});

test("revenge trading calculator hydrates query params and estimates per-day gap", async ({ page }) => {
  await page.goto(`${revengeCalculatorPath}?baselineTrades=40&baselineWins=24&baselineWinPnl=4800&baselineLossPnl=2400&after1Trades=24&after1Wins=12&after1WinPnl=2040&after1LossPnl=1800&after2Trades=18&after2Wins=6&after2WinPnl=1080&after2LossPnl=2160&days=20`);

  await expect(page.locator("#baselineTrades")).toHaveValue("40");
  await expect(page.locator(".expectancy-result-grid")).toHaveCount(0);
  await page.getByRole("button", { name: "Analyze Post-Loss Performance" }).click();
  await expect(page.locator(".expectancy-result-grid div").filter({ hasText: "Expectancy Change After 2 Losses" })).toContainText("-$120.00/trade");
  await expect(page.getByText("Estimated Post-Loss Gap Per Trading Day")).toBeVisible();
  await expect(page.getByText("$168/day")).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute("href", `${siteUrl}${revengeCalculatorPath}`);
});

test("revenge trading calculator validates inputs and never renders NaN or Infinity", async ({ page }) => {
  await page.goto(revengeCalculatorPath);
  await page.locator("#baselineTrades").fill("0");
  await page.locator("#baselineWins").fill("2");
  await page.locator("#baselineWinPnl").fill("-100");
  await page.locator("#baselineLossPnl").fill("100");
  await page.locator("#after1Trades").fill("2");
  await page.locator("#after1Wins").fill("3");
  await page.locator("#after1WinPnl").fill("100");
  await page.locator("#after1LossPnl").fill("0");
  await page.getByRole("button", { name: "Analyze Post-Loss Performance" }).click();

  await expect(page.getByText("Baseline trades must be at least 1.")).toBeVisible();
  await expect(page.getByText("Baseline total winning P&L cannot be negative.")).toBeVisible();
  await expect(page.getByText("After 1 loss winning trades cannot exceed total trades.")).toBeVisible();
  await expect(page.getByText("NaN")).toHaveCount(0);
  await expect(page.getByText("Infinity")).toHaveCount(0);
});

test("revenge trading calculator normalizes blank P&L for zero-outcome groups", async ({ page }) => {
  await page.goto(revengeCalculatorPath);
  for (const group of ["baseline", "after1", "after2"] as const) {
    await page.locator(`#${group}Trades`).fill("1");
    await page.locator(`#${group}Wins`).fill("0");
    await page.locator(`#${group}LossPnl`).fill(group === "baseline" ? "200" : group === "after1" ? "400" : "800");
  }
  await page.locator("#days").fill("2");
  await page.getByRole("button", { name: "Analyze Post-Loss Performance" }).click();

  await expect(page.getByText("winning P&L is required", { exact: false })).toHaveCount(0);
  await expect(page.locator(".revenge-sequence div").filter({ hasText: "Baseline" })).toContainText("-$200.00/trade");
  await expect(page.locator(".revenge-sequence div").filter({ hasText: "After 1 Loss" })).toContainText("-$400.00/trade");
  await expect(page.locator(".revenge-sequence div").filter({ hasText: "After 2 Losses" })).toContainText("-$800.00/trade");
});

test("revenge trading calculator accepts blank losing P&L for all-winning groups and keeps P&L required otherwise", async ({ page }) => {
  await page.goto(revengeCalculatorPath);
  for (const group of ["baseline", "after1", "after2"] as const) {
    await page.locator(`#${group}Trades`).fill("1");
    await page.locator(`#${group}Wins`).fill("1");
    await page.locator(`#${group}WinPnl`).fill("100");
  }
  await page.getByRole("button", { name: "Analyze Post-Loss Performance" }).click();
  await expect(page.getByText("losing P&L is required", { exact: false })).toHaveCount(0);
  await expect(page.locator(".revenge-sequence")).toBeVisible();

  await page.locator("#baselineWins").fill("0");
  await page.locator("#baselineWinPnl").fill("");
  await page.getByRole("button", { name: "Analyze Post-Loss Performance" }).click();
  await expect(page.getByText("Baseline total losing P&L is required when there are losing trades.")).toBeVisible();
  await expect(page.locator("#baselineLossPnl")).toBeFocused();
});

test("revenge trading calculator hides stale metrics until analyzed again", async ({ page }) => {
  await page.goto(revengeCalculatorPath);
  await fillRevengeExample(page);
  await page.getByRole("button", { name: "Analyze Post-Loss Performance" }).click();
  await expect(page.locator(".revenge-sequence")).toBeVisible();

  await page.locator("#after2LossPnl").fill("1800");
  await expect(page.locator(".revenge-sequence")).toHaveCount(0);
  await expect(page.getByText("Inputs changed. Analyze again to refresh the comparison.")).toBeVisible();

  await page.getByRole("button", { name: "Analyze Post-Loss Performance" }).click();
  await expect(page.locator(".revenge-sequence div").filter({ hasText: "After 2 Losses" })).toContainText("-$40.00/trade");
});

test("revenge trading calculator records anonymous tool events without input values", async ({ page }) => {
  await page.goto(`${revengeCalculatorPath}?utm_source=google&utm_medium=organic&utm_campaign=revenge`);
  await fillRevengeExample(page);
  let events = await page.evaluate(() => JSON.parse(window.localStorage.getItem("tradepilot_seo_events") ?? "[]"));
  expect(events.map((event: { event: string }) => event.event)).not.toContain("revenge_calculator_completed");
  await page.getByRole("button", { name: "Analyze Post-Loss Performance" }).click();
  await page.getByRole("link", { name: calculatorCta }).click();
  await expect(page).toHaveURL(/\/register\?source=revenge-trading-calculator$/);

  events = await page.evaluate(() =>
    JSON.parse(window.localStorage.getItem("tradepilot_seo_events") ?? "[]")
  );
  const names = events.map((event: { event: string }) => event.event);
  expect(names).toEqual(
    expect.arrayContaining([
      "tool_page_view",
      "revenge_calculator_started",
      "revenge_calculator_completed",
      "revenge_result_strong_post_loss_deterioration",
      "revenge_calculator_cta_click",
      "signup_started_from_seo",
    ])
  );
  const analyticsFields = events.map(({ eventId: _eventId, anonymousId: _anonymousId, timestamp: _timestamp, ...event }: Record<string, unknown>) => event);
  expect(JSON.stringify(analyticsFields)).toContain("revenge_trading_calculator");
  expect(analyticsFields).toEqual(
    expect.arrayContaining([
      expect.objectContaining({
        event: "revenge_calculator_cta_click",
        landingPage: revengeCalculatorPath,
        tool: "revenge_trading_calculator",
        ctaLabel: calculatorCta,
        ctaDestination: registerUrlForPath(revengeCalculatorPath),
      }),
    ])
  );
  expect(JSON.stringify(analyticsFields)).not.toContain("4800");
  expect(JSON.stringify(analyticsFields)).not.toContain("2160");
});

test("revenge trading calculator is usable on mobile", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 900 });
  await page.goto(revengeCalculatorPath);
  await fillRevengeExample(page);
  await page.getByRole("button", { name: "Analyze Post-Loss Performance" }).click();

  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBeLessThanOrEqual(1);
  await expect(page.locator(".expectancy-result-grid div").filter({ hasText: "Expectancy Change After 2 Losses" })).toContainText("-$120.00/trade");
});

test("overtrading calculator renders metadata, content and structured data", async ({ page }) => {
  await page.goto(overtradingCalculatorPath);

  await expect(page.getByRole("heading", { level: 1, name: "Overtrading Calculator" })).toBeVisible();
  await expect(page).toHaveTitle(overtradingCalculatorTitle);
  await expect(page.locator('meta[name="description"]')).toHaveAttribute("content", overtradingCalculatorDescription);
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute("href", `${siteUrl}${overtradingCalculatorPath}`);
  await expect(page.locator('meta[property="og:url"]')).toHaveAttribute("content", `${siteUrl}${overtradingCalculatorPath}`);
  await expect(page.locator('meta[name="robots"]')).toHaveCount(0);
  await expect(page.locator("h1")).toHaveCount(1);
  await expect(page.locator(".overtrading-group-card legend").filter({ hasText: "Early Trades" })).toBeVisible();
  await expect(page.locator(".overtrading-group-card legend").filter({ hasText: "Later Trades" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Analyze Later-Trade Performance" })).toBeVisible();
  await expect(page.getByText("Compare Early vs Later Trades")).toBeVisible();
  await expect(page.getByText("Early trades is required.")).toHaveCount(0);
  await expect(page.getByText("Later-Trade Performance Gap")).toBeVisible();
  await expect(page.getByText("What Does Overtrading Actually Mean?")).toBeVisible();
  await expect(page.getByRole("link", { name: calculatorCta })).toHaveAttribute("href", registerUrlForPath(overtradingCalculatorPath));

  for (const [label, href] of [
    ["Overtrading", "/overtrading"],
    ["Revenge trading", "/revenge-trading"],
    ["Futures trading journal", "/futures-trading-journal"],
    ["AI trading journal", "/ai-trading-journal"],
  ] as const) {
    await expect(page.getByRole("link", { name: label }).first()).toHaveAttribute("href", href);
  }

  const schemas = await page.locator('script[type="application/ld+json"]').evaluateAll((nodes) =>
    nodes.map((node) => JSON.parse(node.textContent ?? "{}"))
  );
  expect(JSON.stringify(schemas)).toContain("WebApplication");
  expect(JSON.stringify(schemas)).toContain("BreadcrumbList");
});

test("overtrading calculator uses two desktop input groups without immediate required errors", async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto(overtradingCalculatorPath);

  const groups = page.locator(".overtrading-suite-card .overtrading-group-card");
  await expect(groups).toHaveCount(2);

  const boxes = await groups.evaluateAll((elements) =>
    elements.map((element) => {
      const box = element.getBoundingClientRect();
      return { x: box.x, y: box.y, width: box.width };
    })
  );
  expect(Math.max(...boxes.map((box) => box.y)) - Math.min(...boxes.map((box) => box.y))).toBeLessThan(12);
  expect(boxes[1].x).toBeGreaterThan(boxes[0].x + boxes[0].width / 2);
  await expect(page.getByText("Early trades is required.")).toHaveCount(0);

  await page.getByRole("button", { name: "Analyze Later-Trade Performance" }).click();
  await expect(page.getByText("Early trades is required.")).toBeVisible();
});

test("overtrading calculator computes group metrics and performance gap", async ({ page }) => {
  await page.goto(overtradingCalculatorPath);
  await page.locator("#earlyTrades").fill("36");
  await page.locator("#earlyWins").fill("21");
  await page.locator("#earlyWinPnl").fill("4200");
  await page.locator("#earlyLossPnl").fill("1950");
  await page.locator("#laterTrades").fill("28");
  await page.locator("#laterWins").fill("11");
  await page.locator("#laterWinPnl").fill("2100");
  await page.locator("#laterLossPnl").fill("3850");

  await expect(page.locator(".expectancy-result-grid div").filter({ hasText: "Early Win Rate" })).toContainText("58.33%");
  await expect(page.locator(".expectancy-result-grid div").filter({ hasText: "Later Win Rate" })).toContainText("39.29%");
  await expect(page.locator(".expectancy-result-grid div").filter({ hasText: "Early Expectancy" })).toContainText("+$62.50/trade");
  await expect(page.locator(".expectancy-result-grid div").filter({ hasText: "Later Expectancy" })).toContainText("-$62.50/trade");
  await expect(page.locator(".expectancy-result-grid div").filter({ hasText: "Expectancy Gap" })).toContainText("-$125.00/trade");
  await expect(page.locator(".expectancy-monthly").filter({ hasText: "Estimated Later-Trade Performance Gap" })).toContainText("$3,500");
  await expect(page.getByText("pattern worth investigating")).toBeVisible();
});

test("overtrading calculator hydrates query params and estimates daily gap", async ({ page }) => {
  await page.goto(`${overtradingCalculatorPath}?earlyTrades=36&earlyWins=21&earlyWinPnl=4200&earlyLossPnl=1950&laterTrades=28&laterWins=11&laterWinPnl=2100&laterLossPnl=3850&days=10`);

  await expect(page.locator("#earlyTrades")).toHaveValue("36");
  await expect(page.locator(".expectancy-result-grid div").filter({ hasText: "Expectancy Gap" })).toContainText("-$125.00/trade");
  await expect(page.getByText("Estimated Gap Per Trading Day")).toBeVisible();
  await expect(page.getByText("$350/day")).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute("href", `${siteUrl}${overtradingCalculatorPath}`);
});

test("overtrading calculator validates group inputs and never renders NaN or Infinity", async ({ page }) => {
  await page.goto(overtradingCalculatorPath);
  await page.locator("#earlyTrades").fill("0");
  await page.locator("#earlyWins").fill("4");
  await page.locator("#earlyWinPnl").fill("-100");
  await page.locator("#earlyLossPnl").fill("200");
  await page.locator("#laterTrades").fill("2");
  await page.locator("#laterWins").fill("3");
  await page.locator("#laterWinPnl").fill("100");
  await page.locator("#laterLossPnl").fill("0");

  await expect(page.getByText("Early trades must be at least 1.")).toBeVisible();
  await expect(page.getByText("Early total winning P&L cannot be negative.")).toBeVisible();
  await expect(page.getByText("Later winning trades cannot exceed later trades.")).toBeVisible();
  await expect(page.getByText("NaN")).toHaveCount(0);
  await expect(page.getByText("Infinity")).toHaveCount(0);
});

test("overtrading calculator records anonymous tool events without input values", async ({ page }) => {
  await page.goto(`${overtradingCalculatorPath}?utm_source=google&utm_medium=organic&utm_campaign=overtrading`);
  await page.locator("#earlyTrades").fill("36");
  await page.locator("#earlyWins").fill("21");
  await page.locator("#earlyWinPnl").fill("4200");
  await page.locator("#earlyLossPnl").fill("1950");
  await page.locator("#laterTrades").fill("28");
  await page.locator("#laterWins").fill("11");
  await page.locator("#laterWinPnl").fill("2100");
  await page.locator("#laterLossPnl").fill("3850");
  await page.getByRole("link", { name: calculatorCta }).click();
  await expect(page).toHaveURL(/\/register\?source=overtrading-calculator$/);

  const events = await page.evaluate(() =>
    JSON.parse(window.localStorage.getItem("tradepilot_seo_events") ?? "[]")
  );
  const names = events.map((event: { event: string }) => event.event);
  expect(names).toEqual(
    expect.arrayContaining([
      "tool_page_view",
      "overtrading_calculator_started",
      "overtrading_calculator_completed",
      "overtrading_result_later_worse",
      "overtrading_cta_click",
      "signup_started_from_seo",
    ])
  );
  expect(JSON.stringify(events)).toContain("overtrading_calculator");
  expect(JSON.stringify(events)).not.toContain("4200");
  expect(JSON.stringify(events)).not.toContain("3850");
});

test("overtrading calculator is usable on mobile", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 900 });
  await page.goto(overtradingCalculatorPath);
  await page.locator("#earlyTrades").fill("36");
  await page.locator("#earlyWins").fill("21");
  await page.locator("#earlyWinPnl").fill("4200");
  await page.locator("#earlyLossPnl").fill("1950");
  await page.locator("#laterTrades").fill("28");
  await page.locator("#laterWins").fill("11");
  await page.locator("#laterWinPnl").fill("2100");
  await page.locator("#laterLossPnl").fill("3850");

  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBeLessThanOrEqual(1);
  const groups = page.locator(".overtrading-suite-card .overtrading-group-card");
  const boxes = await groups.evaluateAll((elements) => elements.map((element) => element.getBoundingClientRect().y));
  expect(boxes[1]).toBeGreaterThan(boxes[0]);
  await expect(page.locator(".expectancy-result-grid div").filter({ hasText: "Expectancy Gap" })).toContainText("-$125.00/trade");
});

test("trading expectancy calculator renders metadata, content and structured data", async ({ page }) => {
  await page.goto(expectancyCalculatorPath);

  await expect(page.getByRole("heading", { level: 1, name: "Trading Expectancy Calculator" })).toBeVisible();
  await expect(page).toHaveTitle(expectancyCalculatorTitle);
  await expect(page.locator('meta[name="description"]')).toHaveAttribute("content", expectancyCalculatorDescription);
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute("href", `${siteUrl}${expectancyCalculatorPath}`);
  await expect(page.locator('meta[property="og:url"]')).toHaveAttribute("content", `${siteUrl}${expectancyCalculatorPath}`);
  await expect(page.locator('meta[name="robots"]')).toHaveCount(0);
  await expect(page.locator("h1")).toHaveCount(1);
  await expect(page.getByText("Trading Expectancy Formula")).toBeVisible();
  await expect(page.getByText("How Many Trades Do You Need?")).toBeVisible();
  await expect(page.getByRole("button", { name: "Calculate My Expectancy" })).toBeVisible();
  await expect(page.getByText("Calculate Your Trading Expectancy")).toBeVisible();
  await expect(page.getByText("Win rate is required.")).toHaveCount(0);
  await expect(page.getByRole("link", { name: calculatorCta })).toHaveAttribute("href", registerUrlForPath(expectancyCalculatorPath));

  for (const [label, href] of [
    ["Futures trading journal", "/futures-trading-journal"],
    ["AI trading journal", "/ai-trading-journal"],
    ["Trading psychology", "/trading-psychology"],
    ["Overtrading", "/overtrading"],
    ["Revenge trading", "/revenge-trading"],
    ["Trading after losses", "/trading-after-losses"],
  ] as const) {
    await expect(page.getByRole("link", { name: label }).first()).toHaveAttribute("href", href);
  }

  const schemas = await page.locator('script[type="application/ld+json"]').evaluateAll((nodes) =>
    nodes.map((node) => JSON.parse(node.textContent ?? "{}"))
  );
  expect(JSON.stringify(schemas)).toContain("WebApplication");
  expect(JSON.stringify(schemas)).toContain("BreadcrumbList");
});

test("trading expectancy calculator uses compact desktop inputs without immediate required errors", async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto(expectancyCalculatorPath);

  const primaryInputs = page.locator(".expectancy-suite-card .expectancy-input-grid .expectancy-field").filter({
    has: page.locator("input"),
  });
  await expect(primaryInputs).toHaveCount(5);

  const boxes = await primaryInputs.evaluateAll((elements) =>
    elements.slice(0, 3).map((element) => {
      const box = element.getBoundingClientRect();
      return { x: box.x, y: box.y, width: box.width };
    })
  );
  expect(Math.max(...boxes.map((box) => box.y)) - Math.min(...boxes.map((box) => box.y))).toBeLessThan(12);
  expect(boxes[1].x).toBeGreaterThan(boxes[0].x + boxes[0].width / 2);
  expect(boxes[2].x).toBeGreaterThan(boxes[1].x + boxes[1].width / 2);
  await expect(page.getByText("Win rate is required.")).toHaveCount(0);

  await page.getByRole("button", { name: "Calculate My Expectancy" }).click();
  await expect(page.getByText("Win rate is required.")).toBeVisible();

  await page.getByLabel("Win rate").fill("55");
  await page.getByLabel("Average winning trade").fill("250");
  await page.getByLabel("Average losing trade").fill("150");
  await expect(page.getByText("Win rate is required.")).toHaveCount(0);
  await expect(page.locator(".expectancy-primary-result")).toHaveText("+$70.00");
});

test("trading expectancy calculator computes core expectancy metrics", async ({ page }) => {
  await page.goto(expectancyCalculatorPath);
  await page.getByLabel("Win rate").fill("55");
  await page.getByLabel("Average winning trade").fill("250");
  await page.getByLabel("Average losing trade").fill("150");

  await expect(page.locator(".expectancy-primary-result")).toHaveText("+$70.00");
  await expect(page.getByText("1.67 : 1")).toBeVisible();
  await expect(page.locator(".expectancy-result-grid div").filter({ hasText: "Break-even Win Rate" })).toContainText("37.5%");
  await expect(page.locator(".expectancy-result-grid div").filter({ hasText: "Win probability" })).toContainText("55%");
  await expect(page.locator(".expectancy-result-grid div").filter({ hasText: "Loss probability" })).toContainText("45%");
});

test("trading expectancy calculator handles negative and low-win-rate positive examples", async ({ page }) => {
  await page.goto(expectancyCalculatorPath);
  await page.getByLabel("Win rate").fill("80");
  await page.getByLabel("Average winning trade").fill("50");
  await page.getByLabel("Average losing trade").fill("300");
  await expect(page.locator(".expectancy-primary-result")).toHaveText("-$20.00");
  await expect(page.getByText("negative mathematical expectancy")).toBeVisible();

  await page.getByLabel("Win rate").fill("40");
  await page.getByLabel("Average winning trade").fill("400");
  await page.getByLabel("Average losing trade").fill("150");
  await expect(page.locator(".expectancy-primary-result")).toHaveText("+$70.00");
  await expect(page.getByText("positive mathematical expectancy")).toBeVisible();
});

test("trading expectancy calculator estimates monthly expectancy without implying profit", async ({ page }) => {
  await page.goto(`${expectancyCalculatorPath}?winRate=55&avgWin=250&avgLoss=150&trades=40`);

  await expect(page.locator(".expectancy-primary-result")).toHaveText("+$70.00");
  await expect(page.getByText("Estimated mathematical expectancy")).toBeVisible();
  await expect(page.getByText("+$2,800/month")).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute("href", `${siteUrl}${expectancyCalculatorPath}`);
});

test("trading expectancy calculator preserves fees and monthly calculations", async ({ page }) => {
  await page.goto(`${expectancyCalculatorPath}?winRate=55&avgWin=250&avgLoss=150&trades=40&fees=4.50`);

  await expect(page.locator(".expectancy-primary-result")).toHaveText("+$65.50");
  await expect(page.getByText("Gross Expectancy: +$70 before fees")).toBeVisible();
  await expect(page.getByText("+$2,620/month")).toBeVisible();
  await expect(page.getByText("-$4.50/trade")).toBeVisible();
});

test("trading expectancy calculator validates inputs and never renders NaN", async ({ page }) => {
  await page.goto(expectancyCalculatorPath);
  await page.getByLabel("Win rate").fill("101");
  await page.getByLabel("Average winning trade").fill("-50");
  await page.getByLabel("Average losing trade").fill("0");

  await expect(page.getByText("Win rate must be between 0 and 100.")).toBeVisible();
  await expect(page.getByText("Average winning trade cannot be negative.")).toBeVisible();
  await expect(page.getByText("Average losing trade must be greater than zero.")).toBeVisible();
  await expect(page.getByText("NaN")).toHaveCount(0);
});

test("trading expectancy calculator records anonymous tool events without input values", async ({ page }) => {
  await page.goto(`${expectancyCalculatorPath}?utm_source=google&utm_medium=organic&utm_campaign=expectancy`);
  await page.getByLabel("Win rate").fill("55");
  await page.getByLabel("Average winning trade").fill("250");
  await page.getByLabel("Average losing trade").fill("150");
  await page.getByRole("link", { name: calculatorCta }).click();
  await expect(page).toHaveURL(/\/register\?source=expectancy-calculator$/);

  const events = await page.evaluate(() =>
    JSON.parse(window.localStorage.getItem("tradepilot_seo_events") ?? "[]")
  );
  const names = events.map((event: { event: string }) => event.event);
  expect(names).toEqual(
    expect.arrayContaining([
      "tool_page_view",
      "expectancy_calculator_started",
      "expectancy_calculator_completed",
      "expectancy_result_positive",
      "expectancy_cta_click",
      "signup_started_from_seo",
    ])
  );
  const analyticsFields = events.map(({ eventId: _eventId, anonymousId: _anonymousId, timestamp: _timestamp, ...event }: Record<string, unknown>) => event);
  expect(JSON.stringify(analyticsFields)).toContain("trading_expectancy_calculator");
  expect(analyticsFields).toEqual(
    expect.arrayContaining([
      expect.objectContaining({
        event: "expectancy_cta_click",
        landingPage: expectancyCalculatorPath,
        tool: "trading_expectancy_calculator",
        ctaLabel: calculatorCta,
        ctaDestination: registerUrlForPath(expectancyCalculatorPath),
      }),
    ])
  );
  expect(JSON.stringify(analyticsFields)).not.toContain("250");
  expect(JSON.stringify(analyticsFields)).not.toContain("150");
});

test("trading expectancy calculator is usable on mobile", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 900 });
  await page.goto(expectancyCalculatorPath);
  await page.getByLabel("Win rate").fill("55");
  await page.getByLabel("Average winning trade").fill("250");
  await page.getByLabel("Average losing trade").fill("150");

  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBeLessThanOrEqual(1);
  await expect(page.locator(".expectancy-primary-result")).toHaveText("+$70.00");
});

test("trading expectancy calculator prerendered HTML contains SEO and educational content", async () => {
  const htmlPath = path.resolve("dist/tools/trading-expectancy-calculator/index.html");
  test.skip(!existsSync(htmlPath), "Run npm run build before checking prerendered HTML.");

  const html = readFileSync(htmlPath, "utf8");
  expect(html).toContain(`<title>${expectancyCalculatorTitle}</title>`);
  expect(html).toContain(`<link rel="canonical" href="${siteUrl}${expectancyCalculatorPath}">`);
  expect(html).toContain("<h1");
  expect(html).toContain("Trading Expectancy Calculator");
  expect(html).toContain("Win rate");
  expect(html).toContain("Average winning trade");
  expect(html).toContain("Trading Expectancy Formula");
  expect(html).toContain("How Many Trades Do You Need?");
  expect(html).toContain(calculatorCta);
});

test("overtrading calculator prerendered HTML contains SEO and educational content", async () => {
  const htmlPath = path.resolve("dist/tools/overtrading-calculator/index.html");
  test.skip(!existsSync(htmlPath), "Run npm run build before checking prerendered HTML.");

  const html = readFileSync(htmlPath, "utf8");
  expect(html).toContain(`<title>${overtradingCalculatorTitle}</title>`);
  expect(html).toContain(`<link rel="canonical" href="${siteUrl}${overtradingCalculatorPath}">`);
  expect(html).toContain("<h1");
  expect(html).toContain("Overtrading Calculator");
  expect(html).toContain("Number of trades");
  expect(html).toContain("Total winning P&amp;L");
  expect(html).toContain("Later-Trade Performance Gap");
  expect(html).toContain("What Does Overtrading Actually Mean?");
  expect(html).toContain(calculatorCta);
});

test("revenge trading calculator prerendered HTML contains SEO and educational content", async () => {
  const htmlPath = path.resolve("dist/tools/revenge-trading-calculator/index.html");
  test.skip(!existsSync(htmlPath), "Run npm run build before checking prerendered HTML.");

  const html = readFileSync(htmlPath, "utf8");
  expect(html).toContain(`<title>${revengeCalculatorTitle}</title>`);
  expect(html).toContain(`<link rel="canonical" href="${siteUrl}${revengeCalculatorPath}">`);
  expect(html).toContain("<h1");
  expect(html).toContain("Revenge Trading Calculator");
  expect(html).toContain("Baseline Trades");
  expect(html).toContain("Total winning P&amp;L");
  expect(html).toContain("Estimated Post-Loss Performance Gap");
  expect(html).toContain("What Is Revenge Trading?");
  expect(html).toContain(calculatorCta);
});

test("robots references the sitemap and avoids promoting private app routes", async ({ request }) => {
  const response = await request.get("/robots.txt");
  expect(response.ok()).toBeTruthy();
  const robots = await response.text();

  expect(robots).toContain("Allow: /");
  expect(robots).toContain(`Sitemap: ${siteUrl}/sitemap.xml`);
  expect(robots).toContain("Disallow: /admin");
  expect(robots).toContain("Disallow: /dashboard");
  expect(robots).toContain("Disallow: /coach");
  expect(robots).toContain("Disallow: /settings");
  expect(robots).toContain("Disallow: /journal");
  expect(robots).toContain("Disallow: /trades/");
});

test("unknown public routes render a noindex not-found page", async ({ page }) => {
  await page.goto("/not-a-real-marketing-page");

  await expect(page.getByRole("heading", { level: 1, name: "This page is not in the journal." })).toBeVisible();
  await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", "noindex,follow");
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute("href", `${siteUrl}/not-a-real-marketing-page`);
});

for (const width of [390, 320]) {
  test(`SEO landing page remains readable without horizontal overflow at ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    await page.goto("/ai-trading-journal");

    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
    expect(overflow).toBeLessThanOrEqual(1);
    await expect(page.getByRole("heading", { level: 1, name: "AI That Analyzes Your Trading - Not Predicts It" })).toBeVisible();
    await expect(page.getByRole("link", { name: "Analyze My Trading Free" }).first()).toBeVisible();
  });
}
