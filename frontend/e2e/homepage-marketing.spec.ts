import { expect, test, type Page } from "@playwright/test";

const disabledAvailability = {
  v2FeaturesEnabled: false,
  features: {
    playbook: false,
    tradingDna: false,
    journey: false,
    tradingMemory: false,
    coach: true,
  },
};

const enabledAvailability = {
  v2FeaturesEnabled: true,
  features: {
    playbook: true,
    tradingDna: true,
    journey: true,
    tradingMemory: true,
    coach: true,
  },
};

async function mockFeatureAvailability(page: Page, body: unknown) {
  await page.route("**/api/v1/features", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(body),
    });
  });
}

test("homepage fails closed to V1 content when V2 features are disabled", async ({ page }) => {
  await mockFeatureAvailability(page, disabledAvailability);

  await page.goto("/");

  await expect(page.getByRole("heading", { level: 1, name: "Your P&L tells you what happened. TradePilot Coach tells you why." })).toBeVisible();
  await expect(page.getByText("Daily Coach").first()).toBeVisible();
  await expect(page.getByText("Weekly Coach").first()).toBeVisible();
  await expect(page.getByText("Leak Detector").first()).toBeVisible();
  await expect(page.getByText("Trading DNA")).toHaveCount(0);
  await expect(page.getByText("Performance Journey")).toHaveCount(0);
  await expect(page.getByText("Playbook")).toHaveCount(0);
  await expect(page.getByText("TradePilot Coach remembers how you trade.")).toHaveCount(0);
  await expect(page.getByText("AI Trading Memory")).toHaveCount(0);
  await expect(page.getByText("$249")).toHaveCount(0);

  const disabledRouteLinks = await page.locator("a").evaluateAll((links) =>
    links
      .map((link) => link.getAttribute("href") ?? "")
      .filter((href) => /trading-dna|journey|performance-timeline|playbook/i.test(href))
  );
  expect(disabledRouteLinks).toEqual([]);
});

test("homepage positions Trading Memory as the flagship V2 story when enabled", async ({ page }) => {
  await mockFeatureAvailability(page, enabledAvailability);

  await page.goto("/");

  await expect(page.getByRole("heading", { level: 1, name: "Your P&L tells you what happened. TradePilot Coach tells you why." })).toBeVisible();
  await expect(page.getByLabel("TradePilot Coach product highlights").getByText("Trading Memory")).toBeVisible();
  await expect(page.getByRole("heading", { name: "TradePilot Coach remembers how you trade." })).toBeVisible();
  await expect(page.getByText("Have I made this trade before?")).toBeVisible();
  await expect(page.getByText("Most journals tell you what you traded. TradePilot Coach helps you understand how you traded.")).toBeVisible();
  await expect(page.getByText("AI Trading Memory")).toBeVisible();
  await expect(page.getByText("Similar trade comparisons")).toBeVisible();
  await expect(page.getByText("Recurring pattern insights")).toBeVisible();
  await expect(page.getByText("Supporting trade evidence")).toBeVisible();
  await expect(page.getByText("Trading DNA")).toBeVisible();
  await expect(page.getByText("Performance Journey")).toBeVisible();
  await expect(page.getByText("Trading Playbook")).toBeVisible();
  await expect(page.getByText("Advanced Coaching Modules Are Available")).toHaveCount(0);
});

test("homepage feature availability failure hides V2 content and preserves coach marketing", async ({ page }) => {
  await page.route("**/api/v1/features", async (route) => {
    await route.abort();
  });

  await page.goto("/");

  await expect(page.getByText("Coaching Based on Your Trades, Not Generic Advice")).toBeVisible();
  await expect(page.getByText("Daily Coach").first()).toBeVisible();
  await expect(page.getByText("Weekly Coach").first()).toBeVisible();
  await expect(page.getByText("TradePilot Coach remembers how you trade.")).toHaveCount(0);
  await expect(page.getByText("AI Trading Memory")).toHaveCount(0);
  await expect(page.getByText("Trading DNA")).toHaveCount(0);
  await expect(page.getByText("Performance Journey")).toHaveCount(0);
  await expect(page.getByText("Trading Playbook")).toHaveCount(0);
});

test("homepage navigation anchors and CTAs target valid public destinations", async ({ page }) => {
  await mockFeatureAvailability(page, disabledAvailability);
  await page.goto("/");

  for (const id of ["features", "how-it-works", "ai-coach", "pricing", "faq"]) {
    await expect(page.locator(`#${id}`)).toHaveCount(1);
  }

  await expect(page.getByRole("link", { name: "Sign In" }).first()).toHaveAttribute("href", "/login");
  await expect(page.getByRole("link", { name: "Start Free" }).first()).toHaveAttribute("href", "/register");
  await expect(page.getByRole("link", { name: "Start Journaling Free" }).first()).toHaveAttribute("href", "/register");
  await expect(page.getByRole("link", { name: "See How It Works" })).toHaveAttribute("href", "#how-it-works");
  await expect(page.getByRole("heading", { name: "Your next trading insight may already be in your history." })).toBeVisible();
});

test("homepage exposes the journal, behavior, and tool resource clusters", async ({ page }) => {
  await mockFeatureAvailability(page, disabledAvailability);
  await page.goto("/");

  const resources = page.getByLabel("TradePilot Coach resources");
  await expect(page.getByRole("heading", { name: "Explore TradePilot Coach" })).toBeVisible();

  for (const [label, href] of [
    ["AI Trading Journal", "/ai-trading-journal"],
    ["Futures Trading Journal", "/futures-trading-journal"],
    ["Topstep Trading Journal", "/topstep-trading-journal"],
    ["Tradovate Trading Journal", "/tradovate-trading-journal"],
    ["Prop Firm Trading Journal", "/prop-firm-trading-journal"],
    ["Trading Psychology", "/trading-psychology"],
    ["Revenge Trading", "/revenge-trading"],
    ["Overtrading", "/overtrading"],
    ["Trading Discipline", "/trading-discipline"],
    ["Breaking Trading Rules", "/breaking-trading-rules"],
    ["Trading After Losses", "/trading-after-losses"],
    ["Trading Expectancy Calculator", "/tools/trading-expectancy-calculator"],
    ["Overtrading Calculator", "/tools/overtrading-calculator"],
    ["Revenge Trading Calculator", "/tools/revenge-trading-calculator"],
  ] as const) {
    await expect(resources.getByRole("link", { name: label, exact: true })).toHaveAttribute("href", href);
  }
});

test("homepage keeps the four-step workflow readable", async ({ page }) => {
  await mockFeatureAvailability(page, disabledAvailability);
  await page.goto("/");

  const howItWorks = page.locator("#how-it-works");
  const processStrip = howItWorks.getByLabel("TradePilot Coach workflow");
  await expect(processStrip.getByText("Import")).toBeVisible();
  await expect(processStrip.getByText("Review")).toBeVisible();
  await expect(processStrip.getByText("Detect")).toBeVisible();
  await expect(processStrip.getByText("Improve")).toBeVisible();
  await expect(howItWorks.getByRole("heading", { name: "Import", exact: true })).toBeVisible();
  await expect(howItWorks.getByRole("heading", { name: "Review", exact: true })).toBeVisible();
  await expect(howItWorks.getByRole("heading", { name: "Detect", exact: true })).toBeVisible();
  await expect(howItWorks.getByRole("heading", { name: "Improve", exact: true })).toBeVisible();
});

for (const width of [390, 320]) {
  test(`homepage remains readable without horizontal overflow at ${width}px`, async ({ page }) => {
    await mockFeatureAvailability(page, disabledAvailability);
    await page.setViewportSize({ width, height: 900 });
    await page.goto("/");

    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
    expect(overflow).toBeLessThanOrEqual(1);
    await expect(page.getByRole("link", { name: "Start Journaling Free" }).first()).toBeVisible();
    await expect(page.getByText("Why traders need more than a journal")).toBeVisible();
    await expect(page.getByRole("heading", { name: "Explore TradePilot Coach" })).toBeVisible();
  });
}

test("homepage pricing presents monthly V1 benefits clearly", async ({ page }) => {
  await mockFeatureAvailability(page, disabledAvailability);
  await page.goto("/");

  const pricing = page.locator("#pricing");
  await expect(pricing.getByText("$29.99/mo")).toBeVisible();
  await expect(pricing.getByText("100 monthly AI credits")).toBeVisible();
  await expect(pricing.getByText("Trading DNA")).toHaveCount(0);
});
