import { expect, test, type Page } from "@playwright/test";

const mockUser = { id: 1, name: "Mock User", email: "mock@example.com", plan: "PRO", role: "USER" };
const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, content-type",
  "Access-Control-Allow-Methods": "GET, POST, PUT, OPTIONS",
};

const playbook = {
  id: 10,
  name: "Break + Acceptance + Displacement + FVG/IFVG Retracement",
  description: "Personal private-beta playbook.",
  markets: ["ES", "NQ"],
  contextTimeframes: ["15m"],
  setupTimeframes: ["5m"],
  executionTimeframes: ["1m"],
  setupSequence: ["Break", "Acceptance", "Displacement", "FVG/IFVG", "Retrace", "Confirmation", "Entry"],
  entryRules: ["Do not anticipate breaks", "Do not chase displacement", "Correct bias != valid entry"],
  invalidationRules: ["Structural invalidation"],
  targetRules: ["Next liquidity"],
  avoidRules: ["Avoid choppy/two-sided price action"],
  active: true,
};

const analysisResult = {
  id: 101,
  market: "ES",
  timeframe: "M5",
  displayTimeframe: "5m",
  playbookId: 10,
  playbookName: playbook.name,
  imageUrl: "/pre-analysis/101/image",
  setupStatus: "WAITING_FOR_RETRACE",
  bias: "BEARISH",
  setupAlignmentScore: 67,
  structureQualityScore: 8,
  createdAt: new Date("2026-09-04T14:00:00Z").toISOString(),
  analysis: {
    market: "ES",
    timeframe: "5m",
    playbookName: playbook.name,
    marketState: "BEARISH_EXPANSION",
    structureQualityScore: 8,
    directionalContext: { bias: "BEARISH", confidenceLabel: "MODERATE", reasoning: "Bearish displacement below the decision level." },
    setupStatus: "WAITING_FOR_RETRACE",
    conditions: {
      break: { status: "SATISFIED", evidence: "Decision level broke." },
      acceptance: { status: "SATISFIED", evidence: "Accepted below the level." },
      displacement: { status: "SATISFIED", evidence: "Strong bearish expansion." },
      fvgIfvg: { status: "SATISFIED", evidence: "Visible imbalance after displacement." },
      retrace: { status: "WAITING", evidence: "No clean retracement yet." },
      confirmation: { status: "WAITING", evidence: "No confirmation after retrace." },
    },
    setupAlignmentScore: 67,
    importantLevels: [],
    entryAssessment: { status: "WAIT", summary: "Bearish structure is valid, but confirmation is incomplete.", whatMustHappenNext: ["Retracement into 7,655", "Bearish rejection"] },
    invalidation: { summary: "Sustained reclaim above 7,660.", levels: [7660] },
    potentialTargets: [],
    ruleViolations: [],
    warnings: ["Do not chase current expansion."],
    narrative: "The bearish structure remains valid, but the playbook sequence still requires retracement and confirmation.",
    marketRead: {
      marketBias: "NEUTRAL_BEARISH",
      marketState: "BEARISH_EXPANSION",
      summary: "ES produced bearish displacement below the decision level, but price is already extended from the preferred entry area.",
      entryPosture: "WAIT_FOR_RETRACEMENT",
    },
    contextScores: {
      structure: { score: 8, label: "BEARISH", reason: "Strong bearish displacement below the decision level." },
      location: { score: 4, label: "POOR_TO_CHASE", reason: "Price is extended away from the preferred entry area." },
      confirmation: { score: 3, label: "NOT_CONFIRMED", reason: "Retracement and continuation confirmation are incomplete." },
      relativeStrength: null,
    },
    keyLevels: [
      { price: 7655, label: "Previous Week Low", role: "Retracement decision level", importance: "HIGH", reason: "Broken level that matters for retracement." },
      { price: 7673.5, label: "Daily Open", role: "Structural reference", importance: "MEDIUM", reason: "Reference level above current price." },
      { price: 7691.25, label: "Previous Day High", role: "Upside liquidity", importance: "LOW", reason: "Higher liquidity reference." },
    ],
    bullishScenario: {
      status: "WAITING_FOR_CONFIRMATION",
      thesis: "Bullish alternative requires a reclaim and acceptance above the decision level.",
      entryZoneLow: null,
      entryZoneHigh: null,
      trigger: "Reclaim decision level.",
      confirmations: ["Acceptance above", "Bullish displacement"],
      invalidation: "Failure to reclaim.",
      targets: [],
      quality: "LOW",
      notes: [],
    },
    bearishScenario: {
      status: "WAITING_FOR_CONFIRMATION",
      thesis: "Preferred bearish path needs retracement, rejection, and renewed displacement.",
      entryZoneLow: 7655,
      entryZoneHigh: 7660,
      trigger: "Bearish rejection from the retracement area.",
      confirmations: ["Retracement into the broken level", "Bearish rejection", "Renewed displacement"],
      invalidation: "Sustained reclaim above 7,660.",
      targets: [{ price: 7645, reason: "next downside liquidity", order: 1 }],
      quality: "MEDIUM",
      notes: ["Avoid chasing the current expansion."],
    },
    noTradeScenario: {
      condition: "Price continues lower without retracing, or chops around the level.",
      reason: "No trade while the setup lacks clean retracement and confirmation.",
    },
    tradePlan: {
      preferredScenario: "BEARISH_REJECTION",
      waitFor: "Retracement into the broken 7,655 level.",
      confirmationNeeded: ["Bearish rejection followed by renewed displacement."],
      avoid: ["Chasing the current expansion."],
      invalidation: "Sustained reclaim and acceptance above the level.",
      targetSummary: "Next confirmed downside liquidity levels.",
    },
    crossMarket: {
      available: false,
      instrument: "ES",
      comparisonInstrument: "NQ",
      leader: "UNKNOWN",
      laggard: "UNKNOWN",
      relativeStrengthState: "NOT_ENOUGH_DATA",
      smtStatus: "NOT_ENOUGH_DATA",
      interpretation: "Not enough correlated-market data.",
    },
  },
};

test("pre-analysis result collapses inputs and prioritizes trader posture", async ({ page }) => {
  await mockTradePilotApp(page);
  await openPreAnalysis(page);

  await expect(page.getByRole("heading", { name: "Analyze Chart" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Active Playbook" })).toBeVisible();
  await expect(page.locator("textarea").first()).toHaveAttribute("rows", "3");

  await page.locator('input[accept="image/png,image/jpeg,image/webp"]').setInputFiles({
    name: "es-chart.png",
    mimeType: "image/png",
    buffer: Buffer.from([137, 80, 78, 71]),
  });
  await expect(page.getByText("es-chart.png")).toBeVisible();
  await expect(page.getByRole("button", { name: "Replace", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Analyze Chart" }).click();

  await expect(page.getByRole("region", { name: "Analysis controls" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Analyze Chart" })).toHaveCount(0);
  await expect(page.getByRole("heading", { name: "WAIT FOR RETRACEMENT" })).toBeVisible();
  await expect(page.getByText("Bias").first()).toBeVisible();
  await expect(page.getByText("Neutral → Bearish", { exact: true })).toBeVisible();
  await expect(page.getByText("Setup Progress")).toBeVisible();
  await expect(page.getByText("4/6")).toBeVisible();
  await expect(page.getByText("Canonical Status")).toBeVisible();
  await expect(page.getByText("Waiting for retrace")).toBeVisible();
  await expect(page.getByText("8/10", { exact: true })).toBeVisible();
  await expect(page.getByText("4/10", { exact: true })).toBeVisible();
  await expect(page.getByText("3/10", { exact: true })).toBeVisible();
  await expect(page.getByText("No correlated-market data available.")).toBeVisible();
  await expect(page.getByText("Market Read")).toBeVisible();
  await expect(page.getByText("ES produced bearish displacement below the decision level").first()).toBeVisible();
  await expect(page.getByText("Previous Week Low")).toBeVisible();
  await expect(page.getByText("7,655", { exact: true })).toBeVisible();
  await expect(page.getByText("What TradePilot Is Waiting For")).toBeVisible();
  await expect(page.getByRole("heading", { name: "Preferred Bearish Path" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Bullish Alternative" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "No Trade" })).toBeVisible();
  await expect(page.getByText("Not enough correlated-market data.")).toBeVisible();

  await page.getByRole("button", { name: "Change Chart" }).click();
  await expect(page.getByRole("heading", { name: "Analyze Chart" })).toBeVisible();

  await page.getByRole("button", { name: "View Playbook" }).click();
  await expect(page.getByText("Sequence:")).toBeVisible();
});

test("pre-analysis mobile layout avoids horizontal overflow", async ({ page }) => {
  await mockTradePilotApp(page);
  await openPreAnalysis(page);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.locator('input[accept="image/png,image/jpeg,image/webp"]').setInputFiles({
    name: "es-chart.png",
    mimeType: "image/png",
    buffer: Buffer.from([137, 80, 78, 71]),
  });
  await page.getByRole("button", { name: "Analyze Chart" }).click();
  await expect(page.getByRole("heading", { name: "WAIT FOR RETRACEMENT" })).toBeVisible();

  const hasHorizontalOverflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth + 2);
  expect(hasHorizontalOverflow).toBe(false);
});

test("private pre-analysis route is noindex", async ({ page }) => {
  await mockTradePilotApp(page);
  await openPreAnalysis(page);

  await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", "noindex,nofollow");
});

async function mockTradePilotApp(page: Page) {
  await page.route(apiPath("auth/me"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ id: 1, name: "Mock User", email: "mock@example.com", plan: "PRO", role: "USER" }) }));
  await page.route(apiPath("accounts"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify([{ id: 1, name: "Mock Account", propFirm: "TOPSTEP", status: "ACTIVE" }]) }));
  await page.route(apiPath("billing/usage"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ plan: "PRO", subscriptionStatus: "ACTIVE", limits: { AI_CREDITS: 100 }, usageThisMonth: { AI_CREDITS: 4 }, remaining: { AI_CREDITS: 96 }, totalAiCreditsAvailable: 96 }) }));
  await page.route(apiPath("features"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ v2FeaturesEnabled: true, v3FeaturesEnabled: true, features: { playbook: true, tradingDna: true, journey: true, tradingMemory: true, tradeReplay: true, coach: true } }) }));
  await page.route(apiPath("activation/status"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ complete: true, steps: [] }) }));
  await page.route(apiPath("pre-analysis/access"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ allowed: true, entitlementKey: "AI_PRE_ANALYSIS_BETA", label: "AI Pre-Analysis", badge: "Private Beta" }) }));
  await page.route(apiPath("pre-analysis/playbook"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify(playbook) }));
  await page.route(apiPath("pre-analysis/history"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify([]) }));
  await page.route(apiPath("pre-analysis/101/image"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "image/png", body: Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==", "base64") }));
  await page.route(apiPath("pre-analysis"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify(analysisResult) }));
  await page.route(apiPath("execution/accounts/1/summary"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ available: false, summary: null }) }));
  await page.route(apiPath("trades"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 }) }));
  await page.route(apiPath("analytics/calendar-summary"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify([]) }));
  await page.route(apiPath("stats/summary"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ totalTrades: 0, winRate: 0, totalPnl: 0 }) }));
  await page.route(apiPath("stats/by-setup"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify([]) }));
  await page.route(apiPath("stats/by-session"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify([]) }));
  await page.route(apiPath("stats/by-symbol"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify([]) }));
  await page.route(apiPath("stats/by-weekday"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify([]) }));

  await page.goto("/login", { waitUntil: "domcontentloaded" });
  await page.evaluate((user) => {
    window.localStorage.setItem("tradepilot_token", "mock-token");
    window.localStorage.setItem("tradepilot_user", JSON.stringify(user));
    window.sessionStorage.setItem("tradepilot_token", "mock-token");
    window.sessionStorage.setItem("tradepilot_user", JSON.stringify(user));
    window.localStorage.setItem("tradepilot_selected_account_id", "1");
  }, mockUser);
  await page.goto("about:blank");
}

async function openPreAnalysis(page: Page) {
  await page.goto("/dashboard", { waitUntil: "networkidle" });
  const preAnalysisLink = page.getByRole("link", { name: "AI Pre-Analysis" });
  await expect(preAnalysisLink).toBeVisible();
  await preAnalysisLink.click();
  await expect(page).toHaveURL(/\/coach\/pre-analysis/);
}

function apiPath(path: string) {
  return new RegExp(`.*\\/api(?:\\/v1)?\\/${path.replace(/\//g, "\\/")}\\/?(?:\\?.*)?$`);
}
