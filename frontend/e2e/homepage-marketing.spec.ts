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
  await expect(page.getByText("Leak Detector").first()).toBeVisible();
  await expect(page.getByRole("heading", { name: "Coaching based on your trades, not generic advice." })).toBeVisible();
  await expect(page.getByText("Trading DNA")).toHaveCount(0);
  await expect(page.getByText("Performance Journey")).toHaveCount(0);
  await expect(page.getByText("Playbook")).toHaveCount(0);
  await expect(page.getByText("TradePilot Coach remembers how you trade.")).toHaveCount(0);
  await expect(page.getByText("AI Trading Memory")).toHaveCount(0);
  await expect(page.getByText("$249/year")).toBeVisible();

  const disabledRouteLinks = await page.locator("a").evaluateAll((links) =>
    links
      .map((link) => link.getAttribute("href") ?? "")
      .filter((href) => /trading-dna|journey|performance-timeline|playbook/i.test(href))
  );
  expect(disabledRouteLinks).toEqual([]);
});

test("homepage publishes the canonical TradePilot Coach entity graph", async ({ page }) => {
  await mockFeatureAvailability(page, disabledAvailability);
  await page.goto("/");
  await expect(page.getByRole("heading", { level: 1, name: "Your P&L tells you what happened. TradePilot Coach tells you why." })).toBeVisible();

  const scripts = await page.locator('script[type="application/ld+json"]').allTextContents();
  const documents = scripts.map((script) => JSON.parse(script));
  const graph = documents.flatMap((document) => document["@graph"] ?? [document]);
  const organization = graph.find((node) => node["@type"] === "Organization");
  const website = graph.find((node) => node["@type"] === "WebSite");
  const webPage = graph.find((node) => node["@type"] === "WebPage");

  expect(organization).toMatchObject({
    "@id": "https://www.tradepilotcoach.com/#organization",
    name: "TradePilot Coach",
    url: "https://www.tradepilotcoach.com",
  });
  expect(website).toMatchObject({
    "@id": "https://www.tradepilotcoach.com/#website",
    publisher: { "@id": "https://www.tradepilotcoach.com/#organization" },
  });
  expect(webPage).toMatchObject({
    url: "https://www.tradepilotcoach.com",
    isPartOf: { "@id": "https://www.tradepilotcoach.com/#website" },
    publisher: { "@id": "https://www.tradepilotcoach.com/#organization" },
  });
  expect(JSON.stringify(graph)).not.toContain("SoftwareApplication");
});

test("homepage keeps the Trading Memory preview gated behind its feature flag", async ({ page }) => {
  await mockFeatureAvailability(page, enabledAvailability);

  await page.goto("/");

  await expect(page.getByRole("heading", { level: 1, name: "Your P&L tells you what happened. TradePilot Coach tells you why." })).toBeVisible();
  await expect(page.getByLabel("TradePilot Coach product highlights").getByText("Trading Memory")).toBeVisible();
  await expect(page.getByText("Have you made this trade before?")).toBeVisible();
  await expect(page.getByText("Yes — TradePilot found a recurring pattern.")).toBeVisible();
  await expect(page.getByText("Most journals tell you what you traded. TradePilot Coach helps you understand how you traded.")).toBeVisible();
  await expect(page.getByText("AI Trading Memory")).toBeVisible();
  await expect(page.getByText("Trading DNA")).toBeVisible();
  await expect(page.getByText("Performance Journey")).toBeVisible();
  await expect(page.getByText("Trading Playbook")).toBeVisible();
});

test("homepage feature availability failure hides V2 content and preserves coach marketing", async ({ page }) => {
  await page.route("**/api/v1/features", async (route) => {
    await route.abort();
  });

  await page.goto("/");

  await expect(page.getByText("Coaching based on your trades, not generic advice.")).toBeVisible();
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
  await expect(page.getByRole("link", { name: "Try Free" }).first()).toHaveAttribute("href", "/register");
  await expect(page.getByRole("link", { name: "See the Demo" }).first()).toHaveAttribute("href", "/demo");
  await expect(page.getByRole("heading", { name: "See what TradePilot Coach finds in your trading." })).toBeVisible();
  await expect(page.getByRole("link", { name: "Explore sample" })).toHaveAttribute("href", "/demo");
  await expect(page.getByRole("link", { name: "Try TradePilot Free" }).last()).toHaveAttribute("href", "/register");
});

test("homepage uses the canonical fictional sample account without implying a customer result", async ({ page }) => {
  await mockFeatureAvailability(page, disabledAvailability);
  await page.goto("/");

  await expect(page.getByLabel("Sample analysis sequence").getByText("Post-loss overtrading", { exact: true })).toBeVisible();
  await expect(page.getByText("14 qualifying occurrences. MODERATE evidence.")).toBeVisible();
  await expect(page.getByText("64 trades imported")).toBeVisible();
  await expect(page.getByText("Net P&L: -$483.75")).toBeVisible();
  await expect(page.getByText("TradePilot detects the pattern from your trading data first. AI helps explain it—it does not invent the finding.")).toBeVisible();
  await expect(page.getByText("Explore the public sample")).toBeVisible();
});

test("homepage keeps the current five-step workflow readable", async ({ page }) => {
  await mockFeatureAvailability(page, disabledAvailability);
  await page.goto("/");

  const howItWorks = page.locator("#how-it-works");
  const processStrip = howItWorks.getByLabel("TradePilot Coach workflow");
  for (const step of ["Import", "Verify", "Detect", "Explain", "Improve"]) {
    await expect(processStrip.getByRole("heading", { name: step, exact: true })).toBeVisible();
  }
});

for (const width of [390, 320]) {
  test(`homepage remains readable without horizontal overflow at ${width}px`, async ({ page }) => {
    await mockFeatureAvailability(page, disabledAvailability);
    await page.setViewportSize({ width, height: 900 });
    await page.goto("/");

    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
    expect(overflow).toBeLessThanOrEqual(1);
    await expect(page.getByRole("link", { name: "Try Free" }).first()).toBeVisible();
    await expect(page.getByText("Why traders need more than a journal")).toBeVisible();
    await expect(page.getByRole("heading", { name: "Built from problems futures traders actually face." })).toBeVisible();
  });
}

test("homepage pricing presents monthly V1 benefits clearly", async ({ page }) => {
  await mockFeatureAvailability(page, disabledAvailability);
  await page.goto("/");

  const pricing = page.locator("#pricing");
  await expect(pricing.getByText("$29.99/mo")).toBeVisible();
  await expect(pricing.getByText("$249/year")).toBeVisible();
  await expect(pricing.getByText("Save $110.88/year")).toBeVisible();
  await expect(pricing.getByRole("link", { name: "Start Pro" })).toHaveAttribute("href", "/register");
  await expect(pricing.getByText(/payments are being prepared/i)).toHaveCount(0);
  await expect(pricing.getByText(/waitlist/i)).toHaveCount(0);
  await expect(pricing.getByText("100 monthly AI credits")).toBeVisible();
  await expect(pricing.getByText("Trading DNA")).toHaveCount(0);
});
