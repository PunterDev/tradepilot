import { expect, test, type Page } from "@playwright/test";

const adminUser = { id: 99, name: "Admin", email: "admin@tradepilot.local", role: "ADMIN", plan: "PRO" };
const user = { id: 42, name: "Trader", email: "trader@tradepilot.local", role: "USER", plan: "FREE" };
const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, content-type",
  "Access-Control-Allow-Methods": "GET, POST, PUT, OPTIONS",
};

test("admin market data requires a current estimate before a confirmed download", async ({ page }) => {
  await mockWorkspace(page, adminUser, []);
  let downloadRequested = false;
  await page.route(apiPath("admin/market-data/coverage"), (route) => route.fulfill({
    status: 200,
    headers: corsHeaders,
    contentType: "application/json",
    body: JSON.stringify([
      { instrument: "NQ", continuousSymbol: "NQ.v.0", timeframe: "1m", firstBar: "2026-01-01T00:00:00Z", lastBar: "2026-01-31T23:59:00Z", barCount: 100 },
      { instrument: "ES", continuousSymbol: "ES.v.0", timeframe: "1m", firstBar: null, lastBar: null, barCount: 0 },
    ]),
  }));
  await page.route(apiPath("admin/market-data/estimate"), (route) => {
    const request = JSON.parse(route.request().postData() || "{}");
    if (request.endDate === "2026-03-01") {
      return route.fulfill({ status: 503, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ userMessage: "Historical market data provider is temporarily unavailable." }) });
    }
    return route.fulfill({
      status: 200,
      headers: corsHeaders,
      contentType: "application/json",
      body: JSON.stringify({ instrument: "ES", continuousSymbol: "ES.v.0", startDate: "2026-02-01", endDate: "2026-02-28", estimatedCostUsd: 1.25, requestedDays: 28, missingChunks: 1 }),
    });
  });
  await page.route(apiPath("admin/market-data/download"), (route) => {
    downloadRequested = true;
    return route.fulfill({
      status: 200,
      headers: corsHeaders,
      contentType: "application/json",
      body: JSON.stringify({ instrument: "ES", continuousSymbol: "ES.v.0", requestedStartDate: "2026-02-01", requestedEndDate: "2026-02-28", downloadedBars: 10, skippedExistingBars: 0, insertedBars: 10, coverageStart: "2026-02-01T00:00:00Z", coverageEnd: "2026-02-28T23:59:00Z", status: "COMPLETED" }),
    });
  });

  await signInAndOpen(page, adminUser, "/admin/market-data");

  await expect(page.getByRole("heading", { name: "Market Data" })).toBeVisible();
  await expect(page.getByText("Admin only")).toBeVisible();
  await expect(page.getByText("NQ.v.0 · 1m")).toBeVisible();
  await expect(page.getByRole("button", { name: "Download Historical Data" })).toBeDisabled();

  await page.getByLabel("Instrument").selectOption("ES");
  await page.getByLabel("Start date").fill("2026-02-01");
  await page.getByLabel("End date").fill("2026-02-28");
  await page.getByRole("button", { name: "Estimate Cost" }).click();
  await expect(page.getByText("Estimated cost: $1.25")).toBeVisible();
  await expect(page.getByRole("button", { name: "Download Historical Data" })).toBeEnabled();

  await page.getByLabel("End date").fill("2026-03-01");
  await expect(page.getByRole("button", { name: "Download Historical Data" })).toBeDisabled();
  await page.getByRole("button", { name: "Estimate Cost" }).click();
  await expect(page.getByText("Historical market data provider is temporarily unavailable.")).toBeVisible();
  await expect(page.getByRole("button", { name: "Download Historical Data" })).toBeDisabled();

  await page.getByLabel("End date").fill("2026-02-28");
  await page.getByRole("button", { name: "Estimate Cost" }).click();
  await page.getByRole("button", { name: "Download Historical Data" }).click();
  await expect(page.getByRole("dialog", { name: "Confirm Historical Download" })).toBeVisible();
  await expect(page.getByText("You are about to request historical Databento data with an estimated cost of $1.25. Continue?")).toBeVisible();
  await page.getByRole("button", { name: "Download Data" }).click();

  await expect(page.getByText("Historical data import completed. 10 bars inserted; 0 existing bars skipped.")).toBeVisible();
  expect(downloadRequested).toBe(true);
});

test("regular users cannot access or navigate to admin market data", async ({ page }) => {
  await mockWorkspace(page, user, [{ id: 1, name: "Primary" }]);
  await signInAndOpen(page, user, "/admin/market-data");

  await expect(page.getByRole("heading", { name: "Admin access required" })).toBeVisible();
  await expect(page.getByText("Market Data", { exact: true })).toHaveCount(0);
});

async function mockWorkspace(page: Page, currentUser: typeof adminUser, accounts: unknown[]) {
  await page.route(apiPath("auth/me"), (route) => route.fulfill(json(currentUser)));
  await page.route(apiPath("accounts"), (route) => route.fulfill(json(accounts)));
  await page.route(apiPath("billing/usage"), (route) => route.fulfill(json({ plan: currentUser.plan, limits: {}, remaining: {} })));
  await page.route(apiPath("features"), (route) => route.fulfill(json({ v2FeaturesEnabled: true, v3FeaturesEnabled: true, features: { playbook: true, tradingDna: true, journey: true, tradingMemory: true, tradeReplay: true, coach: true } })));
  await page.route(apiPath("activation/status"), (route) => route.fulfill(json({})));
  await page.route(apiPath("pre-analysis/access"), (route) => route.fulfill(json({ allowed: false })));
  await page.route(apiPath("trades"), (route) => route.fulfill(json({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 })));
  await page.route(apiPath("analytics/calendar-summary"), (route) => route.fulfill(json([])));
  await page.route(apiPath("stats/summary"), (route) => route.fulfill(json({ totalTrades: 0, winRate: 0, totalPnl: 0 })));
  await page.route(apiPath("stats/by-setup"), (route) => route.fulfill(json([])));
  await page.route(apiPath("stats/by-session"), (route) => route.fulfill(json([])));
  await page.route(apiPath("stats/by-symbol"), (route) => route.fulfill(json([])));
  await page.route(apiPath("stats/by-weekday"), (route) => route.fulfill(json([])));
  await page.route(apiPath("admin/dashboard/summary"), (route) => route.fulfill(json({})));
}

async function signInAndOpen(page: Page, currentUser: typeof adminUser, path: string) {
  await page.goto("/login", { waitUntil: "domcontentloaded" });
  await page.evaluate((storedUser) => {
    window.localStorage.setItem("tradepilot_token", "e2e-token");
    window.localStorage.setItem("tradepilot_user", JSON.stringify(storedUser));
    window.sessionStorage.setItem("tradepilot_token", "e2e-token");
    window.sessionStorage.setItem("tradepilot_user", JSON.stringify(storedUser));
  }, currentUser);
  await page.goto(path);
}

function json(body: unknown) {
  return { status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify(body) };
}

function apiPath(path: string) {
  return new RegExp(`.*\\/api(?:\\/v1)?\\/${path.replace(/\//g, "\\/")}\\/?(?:\\?.*)?$`);
}
