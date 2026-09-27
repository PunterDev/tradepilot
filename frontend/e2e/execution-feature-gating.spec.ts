import { expect, test, type Page, type Route } from "@playwright/test";

const user = { id: 1, name: "Mock User", email: "mock@example.com", plan: "PRO", role: "USER" };
const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, content-type",
  "Access-Control-Allow-Methods": "GET, POST, PUT, OPTIONS",
};

test("does not request V2 execution data while V2 is unknown or disabled", async ({ page }) => {
  const executionRequests = await mockDashboard(page, false, 200);

  await page.goto("/dashboard", { waitUntil: "networkidle" });

  await expect(page.getByRole("heading", { name: "Dashboard", level: 1 })).toBeVisible();
  await page.waitForTimeout(250);
  expect(executionRequests()).toBe(0);
});

test("allows the execution summary request when V2 is enabled", async ({ page }) => {
  const executionRequests = await mockDashboard(page, true);

  await page.goto("/dashboard", { waitUntil: "networkidle" });

  await expect(page.getByRole("heading", { name: "Dashboard", level: 1 })).toBeVisible();
  await expect.poll(executionRequests).toBeGreaterThan(0);
});

async function mockDashboard(page: Page, v2FeaturesEnabled: boolean, featureDelayMs = 0) {
  let executionRequests = 0;

  page.on("request", (request) => {
    if (new URL(request.url()).pathname.includes("/api/v1/execution/")) {
      executionRequests += 1;
    }
  });

  await page.route(apiPath("auth/me"), (route) => fulfill(route, user));
  await page.route(apiPath("accounts"), (route) => fulfill(route, [{ id: 1, name: "Mock Account", propFirm: "TOPSTEP", status: "ACTIVE" }]));
  await page.route(apiPath("billing/usage"), (route) => fulfill(route, {
    plan: "PRO",
    subscriptionStatus: "ACTIVE",
    limits: { AI_CREDITS: 100 },
    usageThisMonth: { AI_CREDITS: 0 },
    remaining: { AI_CREDITS: 100 },
    totalAiCreditsAvailable: 100,
  }));
  await page.route(apiPath("features"), async (route) => {
    if (featureDelayMs) await page.waitForTimeout(featureDelayMs);
    await fulfill(route, {
      v2FeaturesEnabled,
      v3FeaturesEnabled: false,
      features: {
        playbook: v2FeaturesEnabled,
        tradingDna: v2FeaturesEnabled,
        journey: v2FeaturesEnabled,
        tradingMemory: false,
        tradeReplay: false,
        coach: true,
      },
    });
  });
  await page.route(apiPath("activation/status"), (route) => fulfill(route, { complete: true, steps: [] }));
  await page.route(apiPath("pre-analysis/access"), (route) => fulfill(route, { allowed: false }));
  await page.route(apiPath("trades"), (route) => fulfill(route, { content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 }));
  await page.route(apiPath("analytics/calendar-summary"), (route) => fulfill(route, []));
  await page.route(apiPath("stats/summary"), (route) => fulfill(route, { totalTrades: 0, winRate: 0, totalPnl: 0 }));
  await page.route(apiPath("execution/accounts/1/summary"), (route) => fulfill(route, {
    accountId: 1,
    cleanWins: 0,
    cleanLosses: 0,
    undisciplinedWins: 0,
    undisciplinedLosses: 0,
    patterns: [],
  }));

  await page.addInitScript((storedUser) => {
    window.localStorage.setItem("tradepilot_token", "mock-token");
    window.localStorage.setItem("tradepilot_user", JSON.stringify(storedUser));
    window.localStorage.setItem("tradepilot_selected_account_id", "1");
  }, user);

  return () => executionRequests;
}

async function fulfill(route: Route, body: unknown) {
  await route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify(body) });
}

function apiPath(path: string) {
  return new RegExp(`.*\\/api(?:\\/v1)?\\/${path.replace(/\//g, "\\/")}\\/?(?:\\?.*)?$`);
}
