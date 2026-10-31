import { expect, test, type Page, type Route } from "@playwright/test";

const accountA = { id: 1, name: "Topstep 50K A", propFirm: "TOPSTEP", accountType: "FUNDED", status: "ACTIVE" };
const accountB = { id: 2, name: "Apex 50K B", propFirm: "APEX", accountType: "FUNDED", status: "ACTIVE" };
const incompleteAccount = { id: 3, name: "Setup needed account", propFirm: "TOPSTEP", accountType: "FUNDED", status: "ACTIVE" };
const user = { id: 101, name: "Account Survival E2E", email: "survival@example.com", plan: "PRO", role: "USER" };

const profile = {
  accountId: 1, currentBalance: "50000.00", failureThreshold: "48000.00", dailyLossLimit: "500.00", plannedRiskPerTrade: "150.00",
  personalMaxContracts: 2, firmMaxContracts: 5, primaryWindowStart: "09:30", primaryWindowEnd: "12:00", timezone: "America/New_York", accountStage: "FUNDED", effectiveDate: "2026-10-01",
};

const crossAssessment = {
  accountId: 1, sessionDate: "2026-10-01", state: "FRAGILE", currentBalance: "50000.00", failureThreshold: "48000.00", remainingBuffer: "2000.00",
  plannedRiskPerTrade: "150.00", typicalLoss: "400.00", typicalLossSampleSize: 5, plannedRiskBufferPercent: "7.50", typicalLossBufferPercent: "20.00", normalLossCapacity: "5.00",
  firmMaxContracts: 5, personalMaxContracts: 2, sustainableMaxContracts: 1, primaryThreat: "Cross account continuation", evidenceStrength: "STRONG", missingDataReasons: [], calculatedAt: "2026-10-01T16:00:00", calculationVersion: "account-survival-v1",
  turningPoint: {
    detected: true, type: "CROSS_ACCOUNT_CONTINUATION", timestamp: "2026-10-01T10:12:05", observableFacts: ["Cross-account continuation detected."],
    previousNormalizedExposure: "20", newNormalizedExposure: "40", minutesSincePriorLoss: 8, pnlBefore: "-240.00", finalSessionPnl: "-780.00", stopBeforeActionPnl: "-240.00", lossesAvoided: "630.00", winnersForgone: "90.00", netImpact: "540.00", evidenceStrength: "MODERATE",
    limitations: ["This is an observed sequence; it does not infer intent."], engineVersion: "account-survival-v1",
    crossAccountEvidence: {
      sourceAccounts: ["Topstep 50K A"], laterAccounts: ["Apex 50K B"], sourceSymbols: ["NQ"], laterSymbols: ["MNQ"], instrumentFamily: "NASDAQ", direction: "SHORT", elapsedSeconds: 480,
      previousRealizedLoss: "-240.00", previousNormalizedExposure: "20", newNormalizedExposure: "40", exposureDifference: "20", exposureMultiplier: "2", timestampPrecision: "SECOND", confidence: "MODERATE",
      limitations: ["Near-synchronous account activity is grouped as one synchronized action; no copier relationship is inferred."], engineVersion: "account-survival-v1",
    },
  },
};

const incompleteAssessment = {
  ...crossAssessment, accountId: 3, state: "INSUFFICIENT_DATA", remainingBuffer: null, plannedRiskPerTrade: null, typicalLoss: null,
  normalLossCapacity: null, primaryThreat: "Configuration or observed-loss evidence is incomplete.", missingDataReasons: ["Complete your account risk profile to calculate account survival."],
  turningPoint: { detected: false, observableFacts: [], evidenceStrength: "INSUFFICIENT", limitations: ["No imported trades are available for this session."] },
};

const review = {
  accountId: 1, date: "2026-10-01", sessionDate: "2026-10-01", account: accountA,
  trades: [{ id: 31, accountId: 1, symbol: "NQ", side: "SHORT", tradeDate: "2026-10-01", entryTime: "2026-10-01T10:00:05", exitTime: "2026-10-01T10:04:05", quantity: 2, pnlAmount: -240 }],
  timelineTrades: [{ tradeId: 31, symbol: "NQ", side: "SHORT", entryTime: "2026-10-01T10:00:05", exitTime: "2026-10-01T10:04:05", quantity: 2, pnl: -240, outcome: "LOSS" }],
  totalTrades: 1, winners: 0, losers: 1, totalPnl: -240, netPnl: -240, winRate: 0, profitFactor: 0, sessionGrade: "C", sessionStory: "Deterministic fixture session.", bestTradeWhy: "—", worstTradeWhy: "Observed loss.", bestTrade: null, worstTrade: null, mistakeSummary: {}, detectedMistakes: [], replayInsights: null, sessionDisciplineScore: 60, aiSessionReview: null, noTrades: false, previousTradingDay: null, nextTradingDay: "2026-10-02",
};

const completedEvaluation = {
  available: true, evaluatedSessionDate: "2026-10-02", overallStatus: "BROKEN", evaluationVersion: "account-survival-v1",
  rules: [
    { rule: "MAX_ATTEMPTS", status: "FOLLOWED", configuredValue: "3", observedValue: "2", message: "Two completed attempts were observed." },
    { rule: "MAX_NORMALIZED_EXPOSURE", status: "FOLLOWED", configuredValue: "20", observedValue: "10", message: "Maximum normalized exposure remained within the commitment." },
    { rule: "MAX_SESSION_LOSS", status: "BROKEN", configuredValue: "200", observedValue: "-250", message: "Realized session loss exceeded the configured maximum." },
    { rule: "POST_LOSS_PAUSE", status: "FOLLOWED", configuredValue: "20", observedValue: "26", message: "The shortest post-loss pause was 26 minutes." },
    { rule: "TRADING_WINDOW", status: "FOLLOWED", configuredValue: "09:30–12:00", observedValue: "10:00", message: "All completed attempts opened inside the configured window." },
    { rule: "FRESH_CONFIRMATION", status: "NOT_EVALUABLE", configuredValue: "Enabled", observedValue: null, message: "Completed-trade data does not prove contemporaneous confirmation." },
  ],
};

type FixtureOptions = { evaluationError?: boolean };

async function installFixture(page: Page, options: FixtureOptions = {}) {
  const savedProfiles: Record<number, Record<string, unknown>> = { 1: { ...profile } };
  let commitment = { id: 41, accountId: 1, effectiveSessionDate: "2026-10-01", maxNormalizedExposure: "20", maxAttempts: 2, maxSessionLoss: "200", postLossPauseMinutes: 20, allowedWindowStart: "09:30", allowedWindowEnd: "12:00", requireFreshSetupConfirmation: true, source: "SYSTEM_RECOMMENDED", status: "DRAFT", acceptedAt: null as string | null };
  let evaluation: unknown = { available: false, overallStatus: "ACCEPTED", rules: [], limitation: "No durable evaluation exists yet." };

  await page.route("**/api/**", async route => {
    const url = new URL(route.request().url());
    if (url.port === new URL(page.url() || "http://localhost:5174").port) return route.continue();
    const path = url.pathname.replace(/^\/api(?:\/v1)?/, "");
    const method = route.request().method();
    if (path === "/auth/me") return fulfill(route, user);
    if (path === "/accounts") return fulfill(route, [accountA, accountB, incompleteAccount]);
    if (path === "/billing/usage") return fulfill(route, { plan: "PRO", subscriptionStatus: "ACTIVE", limits: { AI_CREDITS: 100 }, usageThisMonth: { AI_CREDITS: 0 }, remaining: { AI_CREDITS: 100 }, totalAiCreditsAvailable: 100 });
    if (path === "/features") return fulfill(route, { v2FeaturesEnabled: false, v3FeaturesEnabled: false, features: { coach: true, sessionReview: true } });
    if (path === "/activation/status") return fulfill(route, { complete: true, steps: [] });
    if (path === "/pre-analysis/access") return fulfill(route, { allowed: false });
    if (path === "/trades") return fulfill(route, { content: review.trades, totalElements: 1, totalPages: 1, number: 0, size: 100 });
    if (path === "/analytics/calendar-summary") return fulfill(route, []);
    if (path === "/stats/summary") return fulfill(route, { totalTrades: 1, winRate: 0, totalPnl: -240, bestWinStreak: 0, worstDay: -240 });
    if (path === "/sessions/review") return fulfill(route, { ...review, accountId: Number(url.searchParams.get("accountId") ?? 1) });

    const survivalMatch = path.match(/^\/accounts\/(\d+)\/survival(?:\/(.*))?$/);
    if (survivalMatch) {
      const accountId = Number(survivalMatch[1]); const suffix = survivalMatch[2] ?? "";
      if (suffix === "profile") {
        if (method === "PUT") savedProfiles[accountId] = { ...savedProfiles[accountId], ...route.request().postDataJSON() };
        return fulfill(route, savedProfiles[accountId] ?? null);
      }
      if (suffix === "commitment") {
        if (method === "PUT") commitment = { ...commitment, ...route.request().postDataJSON(), status: "DRAFT" };
        return fulfill(route, accountId === 3 ? null : commitment);
      }
      if (suffix.endsWith("/accept") && method === "POST") { commitment = { ...commitment, status: "ACCEPTED", acceptedAt: "2026-10-01T16:05:00" }; return fulfill(route, commitment); }
      if (suffix.endsWith("/evaluate") && method === "POST") { evaluation = completedEvaluation; return fulfill(route, completedEvaluation); }
      if (suffix.endsWith("/evaluations")) return fulfill(route, { evaluations: evaluation && (evaluation as { available?: boolean }).available ? [evaluation] : [], evaluatedSessions: evaluation && (evaluation as { available?: boolean }).available ? 1 : 0, followedSessions: 0, brokenSessions: evaluation && (evaluation as { overallStatus?: string }).overallStatus === "BROKEN" ? 1 : 0, partiallyObservedSessions: 0, notEvaluableSessions: 0 });
      if (suffix.endsWith("/evaluation")) {
        if (options.evaluationError) return route.fulfill({ status: 500, contentType: "application/json", body: JSON.stringify({ message: "evaluation unavailable" }) });
        return fulfill(route, evaluation);
      }
      return fulfill(route, accountId === 3 ? incompleteAssessment : crossAssessment);
    }
    return fulfill(route, {});
  });
  await page.addInitScript((storedUser) => {
    localStorage.setItem("tradepilot_token", "survival-e2e-token");
    localStorage.setItem("tradepilot_user", JSON.stringify(storedUser));
    sessionStorage.setItem("tradepilot_token", "survival-e2e-token");
    sessionStorage.setItem("tradepilot_user", JSON.stringify(storedUser));
    localStorage.setItem("tradepilot_selected_account_id", "1");
  }, user);
}

async function openSessionReview(page: Page) {
  await page.goto("/session-review");
  await expect(page.getByTestId("session-review-heading")).toBeVisible();
  await expect(page.getByRole("heading", { name: "Cross-account continuation" })).toBeVisible();
}

async function expectNoHorizontalOverflow(page: Page) {
  await expect.poll(() => page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
}

test("Account Survival review completes the deterministic cross-account commitment journey", async ({ page }, testInfo) => {
  await installFixture(page);
  await page.goto("/dashboard");
  await expect(page.getByRole("heading", { name: "Fragile" })).toBeVisible();
  await expect(page.getByText("Remaining buffer")).toBeVisible();
  await expect(page.getByText("$2,000.00")).toBeVisible();
  await page.getByRole("link", { name: "Review account" }).click();
  await openSessionReview(page);

  await expect(page.getByText("Planned / typical loss")).toBeVisible();
  await expect(page.getByText("Size limits")).toBeVisible();
  const evidenceButton = page.getByRole("button", { name: "Cross-account evidence" });
  const evidencePanel = evidenceButton.locator("xpath=..");
  await expect(evidenceButton).toHaveAttribute("aria-expanded", "false");
  await evidenceButton.click();
  await expect(evidenceButton).toHaveAttribute("aria-expanded", "true");
  await expect(evidencePanel.getByText("Topstep 50K A")).toBeVisible();
  await expect(evidencePanel.getByText("Apex 50K B")).toBeVisible();
  await expect(evidencePanel.getByText("8 min 0s")).toBeVisible();
  await expect(evidencePanel.getByText("NASDAQ SHORT")).toBeVisible();
  await expect(evidencePanel.getByText("20.00 units")).toBeVisible();
  await expect(evidencePanel.getByText("40.00 units")).toBeVisible();
  await expect(page.getByText(/If trading had stopped before this action/)).toHaveText(/-?\$240\.00/);
  await expect(page.getByText("Account Survival Review")).not.toHaveText(/revenge|tilt|evasion|copier violation/i);
  await evidenceButton.click();
  await evidenceButton.focus();
  await page.keyboard.press("Enter");
  await expect(evidenceButton).toHaveAttribute("aria-expanded", "true");

  await page.getByRole("button", { name: "Edit draft commitment" }).click();
  await page.getByLabel("Maximum attempts").fill("3");
  await page.getByRole("button", { name: "Save commitment" }).click();
  await expect(page.getByRole("button", { name: "Accept commitment" })).toBeVisible();
  await page.getByRole("button", { name: "Accept commitment" }).click();
  await expect(page.getByText("Accepted").first()).toBeVisible();
  await page.reload();
  await expect(page.getByText("Accepted").first()).toBeVisible();
  await page.getByRole("button", { name: "Evaluate commitment" }).click();
  await expect(page.getByText("Max Session Loss — Broken")).toBeVisible();
  await expect(page.getByText("Fresh Confirmation — Not Evaluable")).toBeVisible();
  await expect(page.getByText("Configured: $200.00 · Observed: -$250.00")).toBeVisible();
  await page.reload();
  await expect(page.getByText("Max Session Loss — Broken")).toBeVisible();
  await expectNoHorizontalOverflow(page);
  await page.screenshot({ path: testInfo.outputPath("account-survival-desktop.png"), fullPage: true });
});

test("Account Survival renders an insufficient-data setup path without fabricated values", async ({ page }) => {
  await installFixture(page);
  await openSessionReview(page);
  await page.getByLabel("Account").selectOption("3");
  await expect(page.getByRole("heading", { name: "Insufficient data" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Complete risk profile" })).toBeVisible();
  await expect(page.getByText("No evidence-backed turning point was detected for this session.")).toBeVisible();
  await expect(page.getByText("Remaining buffer")).toHaveCount(0);
  await page.getByRole("button", { name: "Complete risk profile" }).click();
  await page.getByLabel("Current account balance").fill("50000");
  await page.getByLabel("Failure / drawdown threshold").fill("48000");
  await page.getByLabel("Planned risk per trade (1R)").fill("150");
  await page.getByLabel("Personal maximum contracts").fill("2");
  await page.getByLabel("Firm-permitted maximum contracts").fill("5");
  await page.getByRole("button", { name: "Save risk profile" }).click();
  await page.reload();
  await page.getByLabel("Account").selectOption("3");
  await expect(page.getByLabel("Current account balance")).toHaveValue("50000");
});

test("Account Survival keeps profile and commitment visible when evaluation retrieval fails", async ({ page }) => {
  await installFixture(page, { evaluationError: true });
  await openSessionReview(page);
  await expect(page.getByRole("alert")).toHaveText("Unable to load the commitment evaluation.");
  await expect(page.getByText("Remaining buffer")).toBeVisible();
  await expect(page.getByRole("button", { name: "Edit draft commitment" })).toBeVisible();
});

for (const viewport of [{ width: 375, height: 812 }, { width: 390, height: 844 }, { width: 768, height: 1024 }, { width: 1440, height: 900 }]) {
  test(`Account Survival stays usable at ${viewport.width}x${viewport.height}`, async ({ page }, testInfo) => {
    await page.setViewportSize(viewport);
    await installFixture(page);
    await openSessionReview(page);
    const evidenceButton = page.getByRole("button", { name: "Cross-account evidence" });
    await evidenceButton.click();
    await expect(evidenceButton.locator("xpath=..").getByText("Apex 50K B")).toBeVisible();
    await expect(page.getByRole("button", { name: "Edit draft commitment" })).toBeVisible();
    await page.getByRole("button", { name: "Edit draft commitment" }).click();
    await expect(page.getByLabel("Maximum attempts")).toBeVisible();
    await expectNoHorizontalOverflow(page);
    if (viewport.width !== 390) await page.screenshot({ path: testInfo.outputPath(`account-survival-${viewport.width}.png`), fullPage: true });
  });
}

async function fulfill(route: Route, body: unknown) {
  await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(body) });
}
