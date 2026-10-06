import { expect, test, type Page } from "@playwright/test";

const founderUser = { id: 99, name: "Founder", email: "founder@tradepilot.local", role: "USER", plan: "FREE" };
const adminUser = { id: 100, name: "Admin", email: "admin@tradepilot.local", role: "ADMIN", plan: "PRO" };
const user = { id: 42, name: "Trader", email: "trader@tradepilot.local", role: "USER", plan: "FREE" };
const corsHeaders = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "authorization, content-type", "Access-Control-Allow-Methods": "GET, POST, PUT, OPTIONS" };
const completedRun = { id: 7, instrument: "NQ", startDate: "2026-09-07", endDate: "2026-09-10", status: "COMPLETED", createdAt: "2026-09-11T12:00:00Z", resultJson: JSON.stringify(result()) };

test("founder backtest renders configuration and completed model results", async ({ page }) => {
  const pageErrors: string[] = [];
  const consoleErrors: string[] = [];
  page.on("pageerror", (error) => pageErrors.push(error.message));
  page.on("console", (message) => { if (message.type() === "error") consoleErrors.push(message.text()); });
  await mockWorkspace(page, founderUser, [completedRun], trades(), [{ id: 1, name: "Research" }], true);
  await openFounderBacktest(page);
  await expect(page.getByRole("heading", { name: "Playbook Backtest" })).toBeVisible();
  await expect(page.getByText("Private Research", { exact: true })).toBeVisible();
  await expect(page.getByText("Founder only", { exact: true })).toBeVisible();
  for (const label of ["Instrument", "Start date", "End date", "Direction", "Context start", "Entry start", "Entry end", "Max trades / session", "IFVG confirmation", "Ambiguity policy"])
    await expect(page.getByLabel(label)).toBeVisible();
  await expect(page.getByLabel("Ambiguity policy")).toHaveValue("CONSERVATIVE_STOP_FIRST");
  await page.getByRole("button", { name: "Advanced Rules" }).click();
  for (const label of ["Minimum target R", "Target lookback (trading days)", "Runner additional R", "Runner lookback (trading days)"])
    await expect(page.getByLabel(label)).toBeVisible();

  await openCompletedRun(page);
  await expect(page.getByText("1 trade from 12 candidates", { exact: true })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Performance Summary" })).toBeVisible();
  for (const label of ["Trades", "Win rate", "Expectancy", "Total R", "Weighted R", "Max drawdown", "Profit factor"])
    await expect(page.getByText(label, { exact: true }).first()).toBeVisible();
  await expect(page.getByRole("paragraph").filter({ hasText: "Model A" })).toBeVisible();
  await expect(page.getByRole("paragraph").filter({ hasText: "Model B" })).toBeVisible();

  await expect(page.getByRole("heading", { name: "Equity Curve" })).toBeVisible();
  await page.getByRole("button", { name: "Raw R" }).click();
  await page.getByRole("button", { name: "Weighted R" }).click();
  const breakdownSection = page.getByRole("heading", { name: "Performance Breakdowns" }).locator("../..");
  await page.getByRole("tab", { name: "Weekday" }).click();
  await expect(breakdownSection.getByRole("cell", { name: "MONDAY", exact: true })).toBeVisible();
  await page.getByRole("tab", { name: "Direction" }).click();
  await expect(breakdownSection.getByRole("cell", { name: "LONG", exact: true })).not.toHaveCount(0);
  await page.getByRole("tab", { name: "Entry Time" }).click();
  await expect(breakdownSection.getByRole("cell", { name: "09:45-09:59", exact: true })).toBeVisible();
  await page.getByRole("tab", { name: "Month" }).click();
  await expect(breakdownSection.getByRole("cell", { name: "2026-09", exact: true })).toBeVisible();
  await expect(page.getByText("No valid swing target", { exact: true })).toBeVisible();
  await expect(page.getByText("12", { exact: true })).toBeVisible();
  expect(pageErrors).toEqual([]);
  expect(consoleErrors).toEqual([]);
});

test("founder backtest explorer filters, paginates, and exposes deterministic trade detail", async ({ page }) => {
  await mockWorkspace(page, founderUser, [completedRun], trades(), [{ id: 1, name: "Research" }], true);
  await openFounderBacktest(page);
  await openCompletedRun(page);

  await expect(page.getByRole("heading", { name: "Trade Explorer" })).toBeVisible();
  const reviewQueue = page.getByRole("heading", { name: "Manual Review Queue" }).locator("..");
  await expect(reviewQueue.getByText(/representative trades selected/i)).toBeVisible();
  await expect(reviewQueue.getByRole("button", { name: "Review Trade" }).first()).toBeVisible();
  const explorer = page.getByRole("heading", { name: "Trade Explorer" }).locator("../..");
  await page.getByLabel("Explorer direction").selectOption("LONG");
  await expect(page.getByText("2026-09-09", { exact: true }).first()).toBeVisible();
  await page.getByLabel("Explorer direction").selectOption("SHORT");
  await expect(page.getByText("2026-09-10", { exact: true }).first()).toBeVisible();
  await page.getByLabel("Explorer direction").selectOption("BOTH");

  await page.getByLabel("Explorer outcome").selectOption("WIN");
  await expect(page.getByText("2026-09-09", { exact: true }).first()).toBeVisible();
  await page.getByLabel("Explorer outcome").selectOption("LOSS");
  await expect(page.getByText("2026-09-10", { exact: true }).first()).toBeVisible();
  await page.getByLabel("Explorer outcome").selectOption("BE");
  await expect(page.getByText("2026-09-16", { exact: true }).first()).toBeVisible();
  await page.getByLabel("Explorer outcome").selectOption("ALL");
  await page.getByLabel("Explorer weekday").selectOption("WEDNESDAY");
  await expect(page.getByText("2026-09-09", { exact: true }).first()).toBeVisible();
  await page.getByLabel("Explorer weekday").selectOption("ALL");
  await page.getByLabel("Explorer month").selectOption("2026-10");
  await expect(page.getByText("2026-10-01", { exact: true }).first()).toBeVisible();
  await page.getByLabel("Explorer month").selectOption("ALL");

  await page.getByRole("button", { name: "Next" }).click();
  await expect(page.getByText("2026-10-01", { exact: true }).first()).toBeVisible();
  await page.getByRole("button", { name: "Previous" }).click();
  await expect(page.getByRole("button", { name: "Open Chart" }).first()).toBeVisible();
  await page.getByRole("button", { name: "Open Chart" }).first().click();
  await expect(page.getByRole("heading", { name: /NQ .* LONG/ })).toBeVisible();
  await expect(page.getByRole("img", { name: "Backtest trade candlestick chart" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Previous Trade" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Next Trade" })).toHaveCount(0);
  for (const label of ["Direction", "Entry", "Stop", "TP1", "Runner", "Instrument ID", "Ambiguous intrabar"])
    await expect(page.getByText(label, { exact: true })).not.toHaveCount(0);
  await expect(page.getByText("5m IFVG", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Long Position" }).click();
  await expect(page.getByRole("heading", { name: "Manual Long Position" })).toBeVisible();
  await expect(page.getByRole("slider", { name: "Manual entry price drag handle" })).toBeVisible();
  await page.getByRole("spinbutton", { name: "Manual entry price" }).fill("29548");
  await page.getByRole("spinbutton", { name: "Manual stop price" }).fill("29538");
  await page.getByRole("spinbutton", { name: "Manual target price" }).fill("29568");
  await expect(page.getByText("+2R", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Load Backtest Levels" }).click();
  await expect(page.getByRole("spinbutton", { name: "Manual entry price" })).toHaveValue("29547.25");
  await expect(page.getByRole("spinbutton", { name: "Manual stop price" })).toHaveValue("29530.75");
  await expect(page.getByRole("spinbutton", { name: "Manual target price" })).toHaveValue("29555.75");
  await page.getByRole("button", { name: "5m" }).click();
  await expect(page.getByRole("button", { name: "5m" })).toBeVisible();
  await expect(page.getByRole("spinbutton", { name: "Manual entry price" })).toHaveValue("29547.25");
  await page.getByRole("button", { name: "Reset Manual Position" }).click();
  await expect(page.getByRole("heading", { name: "Manual Long Position" })).toHaveCount(0);
  await expect(page.getByRole("slider", { name: "Manual entry price drag handle" })).toHaveCount(0);
  await expect(page.getByText("5m IFVG", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Close Chart" }).click();
  await expect(page.getByRole("heading", { name: /NQ .* LONG/ })).toHaveCount(0);
  await page.getByRole("row").filter({ hasText: "2026-09-09" }).first().click();
  await expect(page.getByRole("heading", { name: "Trade Detail" })).toBeVisible();
  const detail = page.getByRole("heading", { name: "Trade Detail" }).locator("../../..");
  for (const label of ["Instrument", "Instrument ID", "Direction", "Entry", "Stop", "TP1", "Runner", "Model A", "Model B"])
    await expect(detail.getByText(label, { exact: true })).toBeVisible();
  await expect(detail.getByText("42004177", { exact: true })).toBeVisible();
});

test("founder backtest renders running and failed run states safely", async ({ page }) => {
  const running = { ...completedRun, id: 8, status: "RUNNING", resultJson: undefined };
  await mockWorkspace(page, founderUser, [running], [], [{ id: 1, name: "Research" }], true);
  await openFounderBacktest(page);
  await page.getByRole("button", { name: /#8/ }).click();
  await expect(page.locator("span").filter({ hasText: "Running" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Performance Summary" })).toHaveCount(0);

  const failed = { ...completedRun, id: 9, status: "FAILED", errorMessage: "Historical data is incomplete for this range.", resultJson: undefined };
  await mockWorkspace(page, founderUser, [failed], [], [{ id: 1, name: "Research" }], true);
  await openFounderBacktest(page);
  await page.getByRole("button", { name: /#9/ }).click();
  await expect(page.getByText("Historical data is incomplete for this range.", { exact: true })).toBeVisible();
  await expect(page.getByText(/Exception|at com\./)).toHaveCount(0);
});

test("admin users without the private entitlement cannot see or open backtest", async ({ page }) => {
  await mockWorkspace(page, adminUser, [], [], [], false);
  await signInAndOpen(page, adminUser, "/dashboard");
  await expect(page.getByRole("link", { name: "Playbook Backtest" })).toHaveCount(0);
  await page.goto("/backtest");
  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByText("Playbook Backtest", { exact: true })).toHaveCount(0);
});

test("normal users without the private entitlement cannot open backtest directly", async ({ page }) => {
  await mockWorkspace(page, user, [], [], [{ id: 1, name: "Primary" }], false);
  await signInAndOpen(page, user, "/backtest");
  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByRole("link", { name: "Playbook Backtest" })).toHaveCount(0);
  await expect(page.getByText("Playbook Backtest", { exact: true })).toHaveCount(0);
});

test("founder backtest keeps research panels inside laptop viewports", async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await mockWorkspace(page, founderUser, [completedRun], trades(), [{ id: 1, name: "Research" }], true);
  await openFounderBacktest(page);
  await openCompletedRun(page);
  for (const viewport of [{ width: 1440, height: 900 }, { width: 1280, height: 800 }, { width: 1180, height: 800 }, { width: 1024, height: 768 }, { width: 900, height: 768 }, { width: 768, height: 900 }]) {
    await page.setViewportSize(viewport);
    await expect(page.getByRole("heading", { name: "Playbook Backtest" })).toBeVisible();
    await expect(page.getByRole("heading", { name: "Backtest Configuration" })).toBeVisible();
    await expect(page.getByText("Completed", { exact: true }).first()).toBeVisible();
    await expect(page.getByRole("paragraph").filter({ hasText: "Model A" })).toBeVisible();
    await expect(page.getByRole("paragraph").filter({ hasText: "Model B" })).toBeVisible();
    await expect.poll(() => page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
  }
});

test("one chart workspace switches between explorer and manual review without refetching the same trade", async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 800 });
  await mockWorkspace(page, founderUser, [completedRun], trades(), [{ id: 1, name: "Research" }], true);
  const chartRequests: string[] = [];
  page.on("request", request => {
    if (/\/api(?:\/v1)?\/backtests\/7\/trades\/\d+\/chart\?timeframe=/.test(request.url())) chartRequests.push(request.url());
  });
  await openFounderBacktest(page);
  await openCompletedRun(page);
  await page.getByRole("heading", { name: "Manual Review Queue" }).scrollIntoViewIfNeeded();
  const request = page.waitForRequest(value => /\/api(?:\/v1)?\/backtests\/7\/trades\/\d+\/chart\?timeframe=1m/.test(value.url()));
  await page.getByRole("button", { name: "Review Trade" }).first().click();
  await request;
  await expect(page.getByLabel("Chart Review workspace")).toBeVisible();
  await expect(page.getByRole("img", { name: "Backtest trade candlestick chart" })).toBeVisible();
  await expect(page.getByText("Manual Review", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Previous Trade" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Next Trade" })).toBeVisible();
  await expect.poll(() => page.getByLabel("Chart Review workspace").evaluate(node => node.getBoundingClientRect().top < window.innerHeight * .35)).toBe(true);
  await page.getByRole("button", { name: "Long Position" }).click();
  await page.getByRole("spinbutton", { name: "Manual target price" }).fill("29568");
  await page.waitForTimeout(150);
  const initialChartRequestCount = chartRequests.length;

  const explorerOpener = page.getByRole("button", { name: "Open Chart" }).first();
  await explorerOpener.click();
  await expect(page.getByText("Manual Review", { exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Previous Trade" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Next Trade" })).toHaveCount(0);
  await expect.poll(() => page.evaluate(() => document.activeElement?.getAttribute("aria-label"))).toBe("Chart Review workspace");
  await expect.poll(() => page.getByLabel("Chart Review workspace").evaluate(node => node.getBoundingClientRect().top < window.innerHeight * .35)).toBe(true);
  await page.waitForTimeout(150);
  expect(chartRequests).toHaveLength(initialChartRequestCount);
  await expect(page.getByRole("spinbutton", { name: "Manual target price" })).toHaveValue("29568");
  const hourlyRequest = page.waitForRequest(value => /\/api(?:\/v1)?\/backtests\/7\/trades\/1\/chart\?timeframe=1h/.test(value.url()));
  await page.getByRole("button", { name: "1H" }).click();
  await hourlyRequest;
  await expect(page.getByRole("img", { name: "Backtest trade candlestick chart" })).toBeVisible();
  await expect.poll(() => chartRequests.length).toBe(initialChartRequestCount + 1);
  await expect(page.getByText("Trade Entry", { exact: true })).toBeVisible();
  await expect(page.getByRole("spinbutton", { name: "Manual target price" })).toHaveValue("29568");

  const fourHourRequest = page.waitForRequest(value => /\/api(?:\/v1)?\/backtests\/7\/trades\/1\/chart\?timeframe=4h/.test(value.url()));
  await page.getByRole("button", { name: "4H" }).click();
  await fourHourRequest;
  await expect(page.getByRole("img", { name: "Backtest trade candlestick chart" })).toBeVisible();
  await expect.poll(() => chartRequests.length).toBe(initialChartRequestCount + 2);
  await expect(page.getByRole("spinbutton", { name: "Manual target price" })).toHaveValue("29568");

  await page.getByRole("button", { name: "1m" }).click();
  await expect(page.getByRole("img", { name: "Backtest trade candlestick chart" })).toBeVisible();
  await page.waitForTimeout(150);
  expect(chartRequests).toHaveLength(initialChartRequestCount + 2);
  const fiveMinuteRequest = page.waitForRequest(value => /\/api(?:\/v1)?\/backtests\/7\/trades\/1\/chart\?timeframe=5m/.test(value.url()));
  await page.getByRole("button", { name: "5m" }).click();
  await fiveMinuteRequest;
  await expect.poll(() => chartRequests.length).toBe(initialChartRequestCount + 3);

  await page.setViewportSize({ width: 1024, height: 768 });
  await page.getByRole("button", { name: "Review Trade" }).first().click();
  await expect(page.getByText("Manual Review", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Previous Trade" })).toBeVisible();
  await expect.poll(() => page.evaluate(() => document.activeElement?.getAttribute("aria-label"))).toBe("Chart Review workspace");
  await expect.poll(() => page.getByLabel("Chart Review workspace").evaluate(node => node.getBoundingClientRect().top < window.innerHeight * .35)).toBe(true);
  await page.waitForTimeout(150);
  expect(chartRequests).toHaveLength(initialChartRequestCount + 3);
  await expect(page.getByRole("spinbutton", { name: "Manual target price" })).toHaveValue("29568");
  await expect(page.getByRole("img", { name: "Backtest trade candlestick chart" })).toBeVisible();
  const nextRequest = page.waitForRequest(value => /\/api(?:\/v1)?\/backtests\/7\/trades\/2\/chart\?timeframe=5m/.test(value.url()));
  await page.getByRole("button", { name: "Next Trade" }).click();
  await nextRequest;
  await expect(page.getByRole("img", { name: "Backtest trade candlestick chart" })).toBeVisible();
  await expect.poll(() => chartRequests.length).toBe(initialChartRequestCount + 4);

  const thirdExplorerOpener = page.getByRole("button", { name: "Open Chart" }).nth(2);
  const differentTradeRequest = page.waitForRequest(value => /\/api(?:\/v1)?\/backtests\/7\/trades\/3\/chart\?timeframe=5m/.test(value.url()));
  await thirdExplorerOpener.click();
  await differentTradeRequest;
  await expect(page.getByText("Manual Review", { exact: true })).toHaveCount(0);
  await page.getByRole("button", { name: "Close Chart" }).click();
  await expect(page.getByLabel("Chart Review workspace")).toHaveCount(0);
  await expect(thirdExplorerOpener).toBeFocused();
});

test("manual review chart reports a retryable inline loading failure", async ({ page }) => {
  await mockWorkspace(page, founderUser, [completedRun], trades(), [{ id: 1, name: "Research" }], true, true);
  await openFounderBacktest(page);
  await openCompletedRun(page);
  await page.getByRole("button", { name: "Review Trade" }).first().click();
  await expect(page.getByText("Unable to load trade chart.", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Retry" })).toBeVisible();
});

test("interactive chart maximizes, preserves review navigation and restores with Escape", async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await mockWorkspace(page, founderUser, [completedRun], trades(), [{ id: 1, name: "Research" }], true);
  await openFounderBacktest(page);
  await openCompletedRun(page);
  await page.getByRole("button", { name: "Review Trade" }).first().click();
  const workspace = page.getByLabel("Historical Trade Chart");
  await expect(workspace).toHaveAttribute("data-maximized", "false");
  await page.getByRole("button", { name: "Long Position" }).click();
  await page.getByRole("spinbutton", { name: "Manual target price" }).fill("29568");
  await page.getByRole("button", { name: "Maximize chart" }).click();
  await expect(workspace).toHaveAttribute("data-maximized", "true");
  await expect(page.getByRole("img", { name: "Backtest trade candlestick chart" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Previous Trade" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Next Trade" })).toBeVisible();
  const manualEntry = page.getByRole("spinbutton", { name: "Manual entry price" });
  const entryBeforeDrag = await manualEntry.inputValue();
  const entryHandle = page.getByRole("slider", { name: "Manual entry price drag handle" });
  const handleBox = await entryHandle.boundingBox();
  expect(handleBox).not.toBeNull();
  if (handleBox) {
    await page.mouse.move(handleBox.x + handleBox.width * .5, handleBox.y + handleBox.height * .5);
    await page.mouse.down();
    await page.mouse.move(handleBox.x + handleBox.width * .5, handleBox.y - 36, { steps: 5 });
    await page.mouse.up();
  }
  await expect(manualEntry).not.toHaveValue(entryBeforeDrag);
  for (const label of ["5m", "1H", "4H", "1m"]) {
    await page.getByRole("button", { name: label }).click();
    await expect(workspace).toHaveAttribute("data-maximized", "true");
    await expect(page.getByRole("img", { name: "Backtest trade candlestick chart" })).toBeVisible();
  }
  const chartBox = await page.getByRole("img", { name: "Backtest trade candlestick chart" }).boundingBox();
  if (chartBox) {
    await page.mouse.move(chartBox.x + chartBox.width * .7, chartBox.y + chartBox.height * .5);
    await page.mouse.down(); await page.mouse.move(chartBox.x + chartBox.width * .3, chartBox.y + chartBox.height * .5); await page.mouse.up();
    await page.mouse.wheel(0, -160);
  }
  await expect(page.getByRole("spinbutton", { name: "Manual target price" })).toHaveValue("29568");
  await page.getByRole("button", { name: "Next Trade" }).click();
  await expect(workspace).toHaveAttribute("data-maximized", "true");
  await expect(page.getByText("Manual Review", { exact: true })).toBeVisible();
  await page.getByRole("spinbutton", { name: "Manual target price" }).focus();
  await page.keyboard.press("Escape");
  await expect(workspace).toHaveAttribute("data-maximized", "false");
  await expect(page.getByRole("button", { name: "Maximize chart" })).toBeVisible();
});

async function openCompletedRun(page: Page) {
  await page.getByRole("button", { name: /#7/ }).click();
  await expect(page.getByRole("heading", { name: "Trade Explorer" })).toBeVisible();
}

async function openFounderBacktest(page: Page) {
  await signInAndOpen(page, founderUser, "/dashboard");
  const backtestLink = page.getByRole("link", { name: "Playbook Backtest" });
  await expect(backtestLink).toBeVisible();
  await backtestLink.click();
  await expect(page).toHaveURL(/\/backtest$/);
}

async function mockWorkspace(page: Page, currentUser: typeof adminUser, runs: unknown[], backtestTrades: unknown[], accounts: unknown[] = [], backtestAllowed = false, chartFailure = false) {
  await page.route(apiPath("auth/me"), (route) => route.fulfill(json(currentUser)));
  await page.route(apiPath("accounts"), (route) => route.fulfill(json(accounts)));
  await page.route(apiPath("billing/usage"), (route) => route.fulfill(json({ plan: currentUser.plan, usageThisMonth: {}, limits: {}, remaining: {} })));
  await page.route(apiPath("features"), (route) => route.fulfill(json({ v2FeaturesEnabled: true, v3FeaturesEnabled: true, features: { playbook: true, tradingDna: true, journey: true, tradingMemory: true, tradeReplay: true, coach: true } })));
  await page.route(apiPath("activation/status"), (route) => route.fulfill(json({})));
  await page.route(apiPath("pre-analysis/access"), (route) => route.fulfill(json({ allowed: false })));
  await page.route(apiPath("manual-backtests/access"), (route) => route.fulfill(json({ allowed: false })));
  await page.route(apiPath("backtests/access"), (route) => route.fulfill(json({ allowed: backtestAllowed, entitlementKey: "PLAYBOOK_BACKTEST_BETA", label: "Playbook Backtest", badge: "Private Research" })));
  await page.route(apiPath("trades"), (route) => route.fulfill(json({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 })));
  await page.route(apiPath("analytics/calendar-summary"), (route) => route.fulfill(json([])));
  await page.route(apiPath("stats/summary"), (route) => route.fulfill(json({ totalTrades: 0, winRate: 0, totalPnl: 0 })));
  await page.route(apiPath("stats/by-setup"), (route) => route.fulfill(json([])));
  await page.route(apiPath("stats/by-session"), (route) => route.fulfill(json([])));
  await page.route(apiPath("stats/by-symbol"), (route) => route.fulfill(json([])));
  await page.route(apiPath("stats/by-weekday"), (route) => route.fulfill(json([])));
  await page.route(/.*\/api(?:\/v1)?\/execution\/accounts\/\d+\/summary\/?(?:\?.*)?$/, (route) => route.fulfill(json({})));
  await page.route(apiPath("admin/dashboard/summary"), (route) => route.fulfill(json({})));
  await page.route(/.*\/api(?:\/v1)?\/backtests\/\d+\/trades\/\d+\/chart\/?(?:\?.*)?$/, (route) => chartFailure ? route.fulfill({ status: 500, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ message: "Chart unavailable" }) }) : route.fulfill(json(chart(new URL(route.request().url()).searchParams.get("timeframe") ?? "1m"))));
  await page.route(/.*\/api(?:\/v1)?\/backtests\/\d+\/trades\/?(?:\?.*)?$/, (route) => route.fulfill(json(backtestTrades)));
  await page.route(/.*\/api(?:\/v1)?\/backtests\/\d+\/?(?:\?.*)?$/, (route) => route.fulfill(json(runs[0] ?? {})));
  await page.route(apiPath("backtests"), (route) => route.fulfill(json(runs)));
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

function result() {
  const metrics = (totalTrades: number, weightedTotalR: number) => ({ totalTrades, wins: 1, losses: 1, breakEvens: 1, winRate: .5, winRateExcludingBe: .5, averageWinner: .526718, averageLoser: -.25, expectancy: .138359, profitFactor: 2.106872, totalRawR: .526718, weightedTotalR, maxDrawdownR: .25, maxConsecutiveLosses: 1, equityCurve: [{ tradeIndex: 1, tradeDate: "2026-09-09", tradeR: .526718, weightedTradeR: .526718, cumulativeRawR: .526718, cumulativeWeightedR: .526718 }] });
  return { candidateSetups: 12, acceptedCanonicalTrades: 1, modelA: metrics(3, .526718), modelB: metrics(3, .671756), breakdowns: {
    weekday: { "MONDAY|MODEL_A": metrics(1, .1), "MONDAY|MODEL_B": metrics(1, .2), "WEDNESDAY|MODEL_A": metrics(1, .526718), "WEDNESDAY|MODEL_B": metrics(1, .671756) },
    direction: { "LONG|MODEL_A": metrics(1, .526718), "LONG|MODEL_B": metrics(1, .671756), "SHORT|MODEL_A": metrics(1, -.25), "SHORT|MODEL_B": metrics(1, -.2) },
    entryTime: { "09:45-09:59|MODEL_A": metrics(1, .1), "09:45-09:59|MODEL_B": metrics(1, .2), "10:00-10:14|MODEL_A": metrics(1, .526718), "10:00-10:14|MODEL_B": metrics(1, .671756) },
    month: { "2026-09|MODEL_A": metrics(1, .526718), "2026-09|MODEL_B": metrics(1, .671756) },
  }, rejectionCounts: { OUTSIDE_SESSION: 2397, NO_DISPLACEMENT: 2398, NO_ZONE_TOUCH: 206, NO_CONFIRMATION_ZONE: 36, CHASE_FILTER: 2, NO_RETRACE: 1, NO_CAUSAL_STOP: 1, NO_VALID_SWING_TARGET: 12, MAX_TRADES_REACHED: 1 } };
}

function trades() {
  const dates = ["2026-09-09", "2026-09-10", "2026-09-16", "2026-09-17", "2026-09-18", "2026-09-21", "2026-09-22", "2026-09-23", "2026-09-24", "2026-09-25", "2026-10-01", "2026-10-02"];
  return dates.map((tradeDate, index) => {
    const modelAR = index % 3 === 0 ? .526718 : index % 3 === 1 ? -.25 : 0;
    return { id: index + 1, tradeDate, instrument: "NQ", instrumentId: 42004177, direction: index % 2 ? "SHORT" : "LONG", entryTime: `${tradeDate}T14:12:00Z`, entryPrice: 29547.25 + index, stopPrice: 29530.75, tp1Price: 29555.75, runnerTargetPrice: 29560.5, modelAR, modelBR: modelAR === 0 ? 0 : modelAR + .145038, modelAWeightedR: modelAR, modelBWeightedR: modelAR === 0 ? 0 : modelAR + .145038, setupMetadataJson: JSON.stringify({ setupZoneDirection: "BULLISH", setupZone: "29353.750000-29358.750000", ifvgConfirmationTime: "2026-09-09T12:35:00Z", confirmationZone: "29543.750000-29550.500000", targetSwingTimestamp: "2026-09-07T22:49:00Z", runnerObjectivePrice: 29560.5, runnerObjectiveType: "FVG_5M", runnerObjectiveTimeframe: "5m", modelAExitPrice: 29555.75, modelBExitPrice: 29558, ambiguousIntrabar: false }) };
  });
}

function chart(timeframe = "1m") {
  const start = Date.parse("2026-09-09T13:27:00Z");
  const bars = Array.from({ length: 8 }, (_, index) => ({ timestamp: new Date(start + index * 60_000).toISOString(), open: 29544 + index, high: 29545 + index, low: 29543 + index, close: 29544.5 + index, volume: 100 + index }));
  return { instrument: "NQ", continuousSymbol: "NQ.v.0", instrumentId: 42004177, timezone: "America/New_York", timeframe, bars,
    overlays: { setupZone: { label: "5m IFVG", low: 29540, high: 29544, availableAt: "2026-09-09T14:05:00Z", inversionConfirmedAt: "2026-09-09T14:10:00Z" }, confirmationZone: { label: "1m confirmation", low: 29543.75, high: 29550.5, availableAt: "2026-09-09T14:11:00Z", inversionConfirmedAt: null }, entry: { label: "Entry", price: 29547.25, timestamp: "2026-09-09T14:12:00Z", detail: "Entry fill" }, stop: { label: "Stop", price: 29530.75, timestamp: "2026-09-09T14:12:00Z", detail: "Initial stop" }, tp1: { label: "TP1", price: 29555.75, timestamp: "2026-09-09T14:12:00Z", detail: "0.515152R" }, runner: { label: "Runner", price: 29560.5, timestamp: "2026-09-09T14:12:00Z", detail: "FVG_5M 5m" }, stopSwing: { label: "Stop swing", price: 29530.75, timestamp: "2026-09-09T13:55:00Z", confirmedAt: "2026-09-09T14:00:00Z" }, targetSwing: { label: "TP1 target swing", price: 29555.75, timestamp: "2026-09-09T13:49:00Z", confirmedAt: "2026-09-09T13:54:00Z" } },
    facts: { direction: "LONG", entryTime: "2026-09-09T14:12:00Z", entryPrice: 29547.25, stopPrice: 29530.75, riskPoints: 16.5, tp1Price: 29555.75, tp1R: .515152, runnerPrice: 29560.5, runnerR: .80303, runnerObjectiveType: "FVG_5M", modelAR: .515152, modelBR: .651515, instrumentId: 42004177, ambiguousIntrabar: false } };
}

function json(body: unknown) { return { status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify(body) }; }
function apiPath(path: string) { return new RegExp(`.*\\/api(?:\\/v1)?\\/${path.replace(/\//g, "\\/")}\\/?(?:\\?.*)?$`); }
