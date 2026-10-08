import { expect, test, type Page } from "@playwright/test";

type Finding = Record<string, unknown>;

const account = { id: 7, name: "Trust account", propFirm: "TOPSTEP", status: "ACTIVE" };

function response(finding?: Finding) {
  return {
    accountId: 7, cached: false, aiGenerated: true, totalTrades: 89, sampleTradeCount: 89, dataHash: "trust", generatedAt: "2026-10-06T12:00:00",
    message: "Leak detector found a pattern.", biggestLeakTitle: "Post-loss overtrading", biggestLeakImpact: -100, biggestLeakFix: "Pause after losses.", totalEstimatedMonthlyImpact: -100, highestConfidence: 70,
    leaks: [{ leakType: "POST_LOSS_OVERTRADING", title: "Post-loss overtrading", summary: "Deterministic evidence.", description: "", tradeCount: 14, winRate: 30, totalPnl: -100, avgPnl: -10, estimatedMonthlyImpact: -100, confidenceScore: 70, severity: "HIGH", recommendedFix: "Pause after losses.", relatedTradeCount: 0, relatedTradeIds: [], generatedAt: "2026-10-06T12:00:00", behavioralFinding: finding }],
  };
}

function finding(overrides: Finding = {}) {
  return {
    qualified: true, occurrenceCount: 14, tradeSampleSize: 89, sessionSampleSize: 12, evidenceStrength: "MODERATE",
    baseline: { sampleSize: 75, winRate: 55, expectancyR: 0.18, averagePnl: 120 },
    triggered: { sampleSize: 14, winRate: 21, expectancyR: -0.42, averagePnl: -95 },
    counterfactual: { applicable: true, supported: true, skippedTradeCount: 17, skippedWinnerCount: 6, skippedLoserCount: 11, lossesAvoided: 1820, winnersForgone: 710, actualPnl: -500, counterfactualPnl: 610, netHistoricalImpact: 1110 },
    engineVersion: "insight-trust-v1", ruleVersion: "post-loss-overtrading-v1", threshold: null,
    ...overrides,
  };
}

async function openLeakDetector(page: Page, payload: ReturnType<typeof response>) {
  await page.addInitScript(() => {
    localStorage.setItem("tradepilot_token", "playwright-token");
    localStorage.setItem("tradepilot_user", JSON.stringify({ id: 1, name: "Playwright", email: "playwright@example.com", plan: "PRO", role: "USER" }));
  });
  await page.route("**/api/**", async (route) => {
    const requestUrl = new URL(route.request().url());
    if (requestUrl.port === new URL(page.url() || "http://localhost:5174").port) {
      await route.continue();
      return;
    }
    const path = requestUrl.pathname;
    const body = path.endsWith("/auth/me") ? { id: 1, name: "Playwright", email: "playwright@example.com", plan: "PRO", role: "USER" }
      : path.endsWith("/accounts") ? [account]
      : path.endsWith("/billing/usage") ? { plan: "PRO", subscriptionStatus: "ACTIVE", limits: {}, usageThisMonth: {}, remaining: { AI_LEAK_DETECTOR: 3 } }
      : path.endsWith("/features") ? { v2FeaturesEnabled: false, v3FeaturesEnabled: false, features: { playbook: false, tradingDna: false, journey: false, tradingMemory: false, tradeReplay: false, coach: true } }
      : path.endsWith("/ai/leak-detector") ? payload
      : path.endsWith("/trades") ? { content: [] }
      : path.includes("/calendar") ? []
      : {};
    await route.fulfill({ contentType: "application/json", body: JSON.stringify(body) });
  });
  await page.goto("/leak-detector", { waitUntil: "networkidle" });
  await expect(page.getByRole("heading", { name: "Post-loss overtrading" }).first()).toBeVisible();
}

test("renders qualified structured evidence and the full counterfactual breakdown", async ({ page }) => {
  await openLeakDetector(page, response(finding()));

  await expect(page.getByText("Evidence strength").first()).toBeVisible();
  await expect(page.getByText("MODERATE").first()).toBeVisible();
  await expect(page.getByText(/14 qualifying occurrences.*89 trades analyzed/).first()).toBeVisible();
  await expect(page.getByText(/Normal \+0\.18R.*Triggered -0\.42R/).first()).toBeVisible();
  await expect(page.getByText(/Losses avoided \$1,820\.00/).first()).toBeVisible();
  await expect(page.getByText(/Winners forgone \$710\.00/).first()).toBeVisible();
  await expect(page.getByText(/Estimated historical net impact \+\$1,110\.00/).first()).toBeVisible();
  await expect(page.getByText("Good signal")).toHaveCount(0);
  await expect(page.getByText(/Money saved|Potential savings|Recovered profit/)).toHaveCount(0);
});

test("uses backend P&L fallback without fabricating R", async ({ page }) => {
  await openLeakDetector(page, response(finding({ baseline: { sampleSize: 75, winRate: 55, expectancyR: null, averagePnl: 120 }, triggered: { sampleSize: 14, winRate: 21, expectancyR: null, averagePnl: -95 } })));

  await expect(page.getByText(/Normal \$120\.00.*Triggered -\$95\.00/).first()).toBeVisible();
  await expect(page.getByText(/0\.18R|-0\.42R/)).toHaveCount(0);
});

test("renders an unsupported counterfactual without positive savings treatment", async ({ page }) => {
  await openLeakDetector(page, response(finding({ counterfactual: { applicable: true, supported: false, skippedTradeCount: 2, skippedWinnerCount: 1, skippedLoserCount: 1, lossesAvoided: 200, winnersForgone: 350, actualPnl: 0, counterfactualPnl: -150, netHistoricalImpact: -150 } })));

  await expect(page.getByText("A 20-minute pause would not have improved historical results in this sample.").first()).toBeVisible();
  await expect(page.getByText(/Money saved|Potential savings|Recovered profit/)).toHaveCount(0);
});

test("renders insufficient and legacy payloads safely", async ({ page }) => {
  await openLeakDetector(page, response(finding({ qualified: false, occurrenceCount: 4, evidenceStrength: "INSUFFICIENT" })));
  await expect(page.getByText("Not enough evidence yet (4 qualifying occurrences)").first()).toBeVisible();
  await expect(page.getByText("Money Lost / Month")).toHaveCount(0);

  await openLeakDetector(page, response());
  await expect(page.getByText("Confidence").first()).toBeVisible();
});
