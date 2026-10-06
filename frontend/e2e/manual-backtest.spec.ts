import { expect, test, type Page } from "@playwright/test";

const founder = { id: 99, name: "Founder", email: "founder@tradepilot.local", role: "USER", plan: "FREE" };
const user = { id: 42, name: "Trader", email: "trader@tradepilot.local", role: "USER", plan: "FREE" };
const headers = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "authorization, content-type", "Access-Control-Allow-Methods": "GET, POST, PUT, PATCH, OPTIONS" };

test("entitled founder replays only server-revealed bars and controls a manual position", async ({ page }) => {
  await mockReplay(page, founder, true);
  await signIn(page, founder, "/manual-backtest");
  await expect(page.getByRole("heading", { name: "Manual Backtest", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Start Replay" }).click();
  const workspace = page.getByLabel("Manual Backtest replay workspace");
  await expect(workspace).toBeVisible();
  await expect(page.getByRole("img", { name: "Backtest trade candlestick chart" })).toBeVisible();
  await expect(page.getByText(/revealed candles/).first()).toBeVisible();
  await page.getByRole("button", { name: "Next Bar" }).click();
  await expect(page.getByRole("heading", { name: "NQ · 2026-09-11 · 10:12 AM" })).toBeVisible();
  await page.getByRole("button", { name: "5m" }).click();
  await expect(page.getByRole("heading", { name: "NQ · 2026-09-11 · 10:12 AM" })).toBeVisible();
  await page.getByRole("button", { name: "1H" }).click();
  await page.getByRole("button", { name: "4H" }).click();
  await page.getByRole("button", { name: "1m" }).click();
  await page.getByRole("button", { name: "Long", exact: true }).click();
  const chart = page.getByRole("img", { name: "Backtest trade candlestick chart" });
  await expect(chart).toHaveAttribute("data-manual-line-source", "native-price-lines");
  await expect(page.getByRole("slider", { name: "Manual entry price drag handle" })).toHaveCount(1);
  await expect(page.getByRole("slider", { name: "Manual stop price drag handle" })).toHaveCount(1);
  await expect(page.getByRole("slider", { name: "Manual target price drag handle" })).toHaveCount(1);
  await page.getByLabel("Manual replay stop").fill("99");
  await page.getByLabel("Manual replay target").fill("104");
  await expect(page.getByRole("slider", { name: "Manual stop price drag handle" })).toHaveCount(1);
  await expect(page.getByRole("slider", { name: "Manual target price drag handle" })).toHaveCount(1);
  await page.getByRole("button", { name: "Place Order" }).click();
  await expect(page.getByText(/PENDING/)).toBeVisible();
  await page.getByRole("button", { name: "Next Bar" }).click();
  await expect(page.getByRole("heading", { name: "LONG OPEN" })).toBeVisible();
  await page.getByRole("button", { name: "Move Stop to BE" }).click();
  await page.getByRole("button", { name: "Flatten" }).click();
  await page.getByRole("button", { name: "Maximize replay chart" }).click();
  await expect(workspace).toHaveAttribute("data-maximized", "true");
  await page.getByRole("button", { name: "Next Bar" }).click();
  await page.getByRole("button", { name: "Restore replay chart" }).click();
  await expect(workspace).toHaveAttribute("data-maximized", "false");
});

test("playback advances one bar at a time without replacing the normal chart or resetting its timeframe", async ({ page }) => {
  const replay = await mockReplay(page, founder, true);
  await signIn(page, founder, "/manual-backtest");
  await page.getByRole("button", { name: "Start Replay" }).click();
  const workspace = page.getByLabel("Manual Backtest replay workspace");
  const chart = page.getByRole("img", { name: "Backtest trade candlestick chart" });
  await expect(chart).toBeVisible();
  const originalChartNode = await chart.elementHandle();
  await expect(workspace).toHaveAttribute("data-maximized", "false");
  await page.getByRole("button", { name: "5m" }).click();
  await expect(page.getByRole("button", { name: "5m" })).toHaveAttribute("aria-pressed", "true");
  await expect(chart).toHaveAttribute("data-timeframe", "5m");
  const replayHeading = workspace.getByRole("heading").first();
  const initialCursor = await replayHeading.textContent();
  await page.getByLabel("Playback speed").selectOption("10");
  await page.getByRole("button", { name: "Play", exact: true }).click();
  await expect.poll(() => replay.advanceCalls, { timeout: 4_000 }).toBeGreaterThanOrEqual(2);
  await expect(replayHeading).not.toHaveText(initialCursor ?? "");
  await expect(chart).toBeVisible();
  expect(await chart.evaluate((element, original) => element === original, originalChartNode)).toBe(true);
  await expect(workspace).toHaveAttribute("data-maximized", "false");
  await expect(page.getByRole("button", { name: "5m" })).toHaveAttribute("aria-pressed", "true");
  await expect(page.getByText("Loading historical context...")).toHaveCount(0);
  await page.getByRole("button", { name: "Pause", exact: true }).click();
  await page.waitForTimeout(250);
  const pausedCursor = await replayHeading.textContent();
  await page.waitForTimeout(350);
  await expect(replayHeading).toHaveText(pausedCursor ?? "");
  expect(replay.maxConcurrentAdvances).toBeLessThanOrEqual(1);
});

test("playback preserves maximized mode until the user restores it", async ({ page }) => {
  const replay = await mockReplay(page, founder, true);
  await signIn(page, founder, "/manual-backtest");
  await page.getByRole("button", { name: "Start Replay" }).click();
  const workspace = page.getByLabel("Manual Backtest replay workspace");
  await expect(page.getByRole("img", { name: "Backtest trade candlestick chart" })).toBeVisible();
  await page.getByRole("button", { name: "Maximize replay chart" }).click();
  await expect(workspace).toHaveAttribute("data-maximized", "true");
  await page.getByLabel("Playback speed").selectOption("10");
  await page.getByRole("button", { name: "Play", exact: true }).click();
  await expect.poll(() => replay.advanceCalls, { timeout: 4_000 }).toBeGreaterThanOrEqual(2);
  await page.getByRole("button", { name: "Pause", exact: true }).click();
  await expect(workspace).toHaveAttribute("data-maximized", "true");
  await page.getByRole("button", { name: "Restore replay chart" }).click();
  await expect(workspace).toHaveAttribute("data-maximized", "false");
});

test("unentitled users cannot open Manual Backtest directly", async ({ page }) => {
  await mockReplay(page, user, false);
  await signIn(page, user, "/manual-backtest");
  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByRole("link", { name: "Manual Backtest" })).toHaveCount(0);
});

async function mockReplay(page: Page, currentUser: typeof founder, allowed: boolean) {
  let cursor = "2026-09-11T14:11:00Z";
  let active: any = null;
  let inFlightAdvances = 0;
  let maxConcurrentAdvances = 0;
  let advanceCalls = 0;
  const session = () => ({ id: 31, symbol: "NQ", continuousSymbol: "NQ.v.0", tradingDate: "2026-09-11", timezone: "America/New_York", startTime: "2026-09-11T13:30:00Z", endTime: "2026-09-11T15:00:00Z", cursorTs: cursor, status: "ACTIVE", riskProfile: "UNIFORM_1R", notes: null, createdAt: "2026-09-11T12:00:00Z", updatedAt: cursor, completedAt: null, summary: { trades: 0, pendingOrders: active?.status === "PENDING" ? 1 : 0, wins: 0, losses: 0, breakEvens: 0, winRate: 0, rawR: 0, weightedR: 0, averageR: 0, averageWinner: 0, averageLoser: 0, profitFactor: null, maxDrawdown: 0, maxLosingStreak: 0 } });
  await page.route(api("auth/me"), route => route.fulfill(json(currentUser)));
  await page.route(api("accounts"), route => route.fulfill(json([{ id: 1, name: "Research" }])));
  await page.route(api("billing/usage"), route => route.fulfill(json({ plan: "FREE", usageThisMonth: {}, limits: {}, remaining: {} })));
  await page.route(api("features"), route => route.fulfill(json({ v2FeaturesEnabled: true, v3FeaturesEnabled: true, features: { playbook: true, tradingDna: true, journey: true, tradingMemory: true, tradeReplay: true, coach: true } })));
  await page.route(api("activation/status"), route => route.fulfill(json({})));
  await page.route(api("pre-analysis/access"), route => route.fulfill(json({ allowed: false })));
  await page.route(api("backtests/access"), route => route.fulfill(json({ allowed: false })));
  await page.route(api("manual-backtests/access"), route => route.fulfill(json({ allowed, entitlementKey: "MANUAL_BACKTEST_BETA", label: "Manual Backtest", badge: "Private Research" })));
  for (const path of ["trades", "analytics/calendar-summary", "stats/by-setup", "stats/by-session", "stats/by-symbol", "stats/by-weekday"]) await page.route(api(path), route => route.fulfill(json([])));
  await page.route(api("stats/summary"), route => route.fulfill(json({ totalTrades: 0, winRate: 0, totalPnl: 0 })));
  await page.route(/.*\/api(?:\/v1)?\/manual-backtests\/sessions\/31\/chart\/?.*/, route => route.fulfill(json(chart(new URL(route.request().url()).searchParams.get("timeframe") ?? "1m", cursor, active))));
  await page.route(api("manual-backtests/sessions"), route => {
    if (route.request().method() === "POST") return route.fulfill(json(session()));
    return route.fulfill(json([session()]));
  });
  await page.route(/.*\/api(?:\/v1)?\/manual-backtests\/sessions\/31\/advance\/?$/, async route => { inFlightAdvances += 1; advanceCalls += 1; maxConcurrentAdvances = Math.max(maxConcurrentAdvances, inFlightAdvances); await new Promise(resolve => setTimeout(resolve, 30)); const prior = new Date(cursor).getTime(); cursor = new Date(prior + 60_000).toISOString(); if (active?.status === "PENDING") active = { ...active, status: "OPEN", entryTs: cursor, entryPrice: 101 }; else if (active?.pendingFlatten) active = null; inFlightAdvances -= 1; await route.fulfill(json(session())); });
  await page.route(/.*\/api(?:\/v1)?\/manual-backtests\/sessions\/31\/orders\/?$/, route => { const request = route.request().postDataJSON(); active = { id: 8, direction: request.direction, orderType: request.orderType, configuredEntryPrice: request.configuredEntryPrice, orderPlacedTs: cursor, entryTs: null, entryPrice: null, initialStopPrice: request.stopPrice, currentStopPrice: request.stopPrice, targetPrice: request.targetPrice, initialRiskPoints: null, rawR: null, riskMultiplier: 1, weightedR: null, status: "PENDING" }; return route.fulfill(json(active)); });
  await page.route(/.*\/api(?:\/v1)?\/manual-backtests\/sessions\/31\/position\/break-even\/?$/, route => { active = { ...active, currentStopPrice: active.entryPrice }; return route.fulfill(json(active)); });
  await page.route(/.*\/api(?:\/v1)?\/manual-backtests\/sessions\/31\/position\/flatten\/?$/, route => { active = { ...active, pendingFlatten: true }; return route.fulfill(json(active)); });
  await page.route(/.*\/api(?:\/v1)?\/manual-backtests\/sessions\/31\/position\/?$/, route => route.fulfill(json(active)));
  return { get maxConcurrentAdvances() { return maxConcurrentAdvances; }, get advanceCalls() { return advanceCalls; } };
}
function chart(timeframe: string, cursor: string, active: any) { const cursorMs = new Date(cursor).getTime(); const bars = Array.from({ length: 3 }, (_, index) => ({ timestamp: new Date(cursorMs - (2 - index) * 60_000).toISOString(), open: 100 + index, high: 101 + index, low: 99 + index, close: 100.5 + index, volume: 100 + index, complete: timeframe === "1m" })); return { symbol: "NQ", continuousSymbol: "NQ.v.0", timezone: "America/New_York", timeframe, cursorTs: cursor, bars, activeTrade: active }; }
async function signIn(page: Page, currentUser: typeof founder, path: string) { await page.goto("/login"); await page.evaluate(value => { localStorage.setItem("tradepilot_token", "e2e-token"); localStorage.setItem("tradepilot_user", JSON.stringify(value)); sessionStorage.setItem("tradepilot_token", "e2e-token"); sessionStorage.setItem("tradepilot_user", JSON.stringify(value)); }, currentUser); await page.goto(path); }
function json(body: unknown) { return { status: 200, headers, contentType: "application/json", body: JSON.stringify(body) }; }
function api(path: string) { return new RegExp(`.*\\/api(?:\\/v1)?\\/${path.replace(/\//g, "\\/")}\\/?(?:\\?.*)?$`); }
