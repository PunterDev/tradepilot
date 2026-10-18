import { expect, test, type Page } from "@playwright/test";

const adminUser = { id: 99, name: "Admin", email: "admin@tradepilot.local", role: "ADMIN", plan: "PRO" };
const comparison = { currentValue: 1, previousValue: 0, changePercent: 100, percentagePointDelta: 0, direction: "NEW" };
const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, content-type",
  "Access-Control-Allow-Methods": "GET, POST, PUT, OPTIONS",
};

const metadata = {
  key: "AI_REVIEW",
  displayName: "AI Review",
  shortName: "AI Review",
  description: "AI trade review generation.",
  category: "AI_COACHING",
  eventType: "AI_GENERATION",
  coreFeature: true,
  aiFeature: true,
  paidAction: true,
  passiveEvent: false,
  sortOrder: 10,
  iconKey: "sparkles",
  colorKey: "emerald",
};

const feature = {
  ...metadata,
  feature: "AI_REVIEW",
  knownFeature: true,
  uniqueUsers: 3,
  totalUsage: 9,
  activeEventCount: 6,
  passiveEventCount: 1,
  aiGenerationCount: 2,
  overallAdoptionRate: 60,
  activeUserAdoptionRate: 75,
  eventsPerUser: 3,
  repeatUsers: 2,
  repeatUsageRate: 66.7,
  previousPeriodUsers: 1,
  previousPeriodEvents: 2,
  previousPeriodActiveEvents: 1,
  previousPeriodPassiveEvents: 1,
  previousPeriodAiGenerations: 1,
  previousPeriodRepeatUsers: 0,
  previousPeriodRepeatUsageRate: 0,
  userGrowthPercent: 200,
  eventGrowthPercent: 350,
  adoptionDeltaPercentagePoints: 25,
  activeActionGrowthPercent: 500,
  passiveToActiveConversionRate: 600,
  engagementScore: 90,
  opportunityScore: 20,
  healthStatus: "HIGH_ADOPTION",
  status: "High adoption",
};

test("admin feature usage renders trusted product analytics baseline", async ({ page }) => {
  const consoleErrors: string[] = [];
  page.on("console", (message) => {
    if (message.type() === "error") consoleErrors.push(message.text());
  });

  await mockAdminFeatureUsage(page);
  await page.goto("/admin/feature-usage", { waitUntil: "networkidle" });

  await expect(page.getByText("Admin Workspace")).toBeVisible();
  await expect(page.getByText("Product Analytics")).toBeVisible();
  await expect(page.getByText("AI credits")).toHaveCount(0);
  await expect(page.getByText("Accounts")).toHaveCount(0);

  await expect(page.getByRole("heading", { name: "Feature Usage" })).toBeVisible();
  for (const label of ["Active Users", "Active Actions", "Passive Views", "AI Generations", "Highest Active Adoption", "Features Adopted"]) {
    await expect(page.getByText(label, { exact: true }).first()).toBeVisible();
  }

  for (const label of ["Range", "Plan", "Category", "Event Type", "Activity", "Sort"]) {
    await expect(page.getByLabel(label)).toBeVisible();
  }
  await expect(page.getByLabel("Sort")).toContainText("Highest adoption");

  await page.getByRole("button", { name: /AI Review/ }).click();
  await expect(page.getByRole("dialog", { name: "AI Review" })).toBeVisible();
  await expect(page.getByText("Repeat Users")).toBeVisible();
  await expect(page.getByText("Previous Period")).toBeVisible();
  await expect(page.getByRole("link", { name: "View related funnels" })).toBeVisible();

  expect(consoleErrors).toEqual([]);
});

async function mockAdminFeatureUsage(page: Page) {
  await page.route(apiPath("auth/me"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ id: 99, name: "Admin", email: "admin@tradepilot.local", role: "ADMIN", plan: "PRO" }) }));
  await page.route(apiPath("accounts"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify([{ id: 1, name: "Admin Account", propFirm: "TOPSTEP", status: "ACTIVE" }]) }));
  await page.route(apiPath("billing/usage"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ plan: "PRO", subscriptionStatus: "ACTIVE", limits: {}, usageThisMonth: {}, remaining: {} }) }));
  await page.route(apiPath("features"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ v2FeaturesEnabled: true, v3FeaturesEnabled: true, features: { playbook: true, tradingDna: true, journey: true, tradingMemory: true, tradeReplay: true, coach: true } }) }));
  await page.route(apiPath("pre-analysis/access"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ allowed: false }) }));
  await page.route(apiPath("backtests/access"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ allowed: false }) }));
  await page.route(apiPath("manual-backtests/access"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ allowed: false }) }));
  await page.route(apiPath("admin/dashboard/summary"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({}) }));
  await page.route(apiPath("trades"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 }) }));
  await page.route(apiPath("analytics/calendar-summary"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify([]) }));
  await page.route(apiPath("stats/summary"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ totalTrades: 0, winRate: 0, totalPnl: 0 }) }));
  await page.route(apiPath("stats/by-setup"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify([]) }));
  await page.route(apiPath("stats/by-session"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify([]) }));
  await page.route(apiPath("stats/by-symbol"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify([]) }));
  await page.route(apiPath("stats/by-weekday"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify([]) }));
  await page.route(apiPath("admin/feature-usage/summary"), (route) => route.fulfill({
    status: 200,
    headers: corsHeaders,
    contentType: "application/json",
    body: JSON.stringify({
      totalUsers: 4,
      activeUsers30d: 4,
      activeUsers: 4,
      totalEvents: 9,
      activeActions: 6,
      passiveViews: 1,
      aiGenerations: 2,
      uniqueFeaturesUsed: 1,
      avgEventsPerActiveUser: 2.25,
      avgUniqueFeaturesPerActiveUser: 1,
      features: [feature],
      mostUsedFeature: "AI_REVIEW",
      leastUsedFeature: "AI_REVIEW",
      highestVolumeFeature: "AI_REVIEW",
      highestAdoptionFeature: "AI_REVIEW",
      highestActiveAdoptionFeature: "AI_REVIEW",
      lowestAdoptionFeature: "AI_REVIEW",
      activeUsersComparison: comparison,
      activeActionsComparison: comparison,
      passiveViewsComparison: comparison,
      aiGenerationsComparison: comparison,
      featuresAdoptedComparison: comparison,
      insights: [],
      activationFunnel: {
        signedUpUsers: 4,
        importedUsers: 3,
        firstInsightUsers: 2,
        secondAiFeatureUsers: 1,
        proConversionUsers: 1,
        signupToImportRate: 75,
        importToFirstInsightRate: 66.7,
        firstInsightToSecondAiRate: 50,
        firstInsightToProRate: 50,
      },
      dataQuality: [],
      metadata: [metadata],
      selectedStartDate: "2026-08-01",
      selectedEndDate: "2026-08-30",
      previousStartDate: "2026-07-02",
      previousEndDate: "2026-07-31",
      generatedAt: "2026-08-30T17:00:00",
    }),
  }));
  await page.route(apiPath("admin/feature-usage/trends"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ dailyCoach: [], weeklyCoach: [], tradingDna: [], aiReview: [], sessionReview: [], series: { AI_REVIEW: [{ date: "2026-08-30", count: 9 }] }, fastestGrowingFeature: "AI_REVIEW", largestDeclineFeature: null, mostStableFeature: null }) }));
  await page.route(apiPath("admin/feature-usage/users"), (route) => route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify([{ userId: 1, email: "user@example.com", maskedEmail: "u***@example.com", plan: "PRO", trades: 12, activeFeatures: 1, activeActions: 6, passiveEvents: 1, aiGenerations: 2, totalEvents: 9, lastActiveAt: "2026-08-30T17:00:00", engagementLevel: "High engagement", featureUsageCounts: { AI_REVIEW: 9 } }]) }));

  await page.goto("/login", { waitUntil: "domcontentloaded" });
  await page.evaluate((user) => {
    window.localStorage.setItem("tradepilot_token", "admin-token");
    window.localStorage.setItem("tradepilot_user", JSON.stringify(user));
    window.sessionStorage.setItem("tradepilot_token", "admin-token");
    window.sessionStorage.setItem("tradepilot_user", JSON.stringify(user));
  }, adminUser);
  await page.goto("about:blank");
}

function apiPath(path: string) {
  return new RegExp(`.*\\/api(?:\\/v1)?\\/${path.replace(/\//g, "\\/")}\\/?(?:\\?.*)?$`);
}
