import { expect, test } from "@playwright/test";

const admin = { id: 1, name: "Admin", email: "admin@example.com", role: "ADMIN", plan: "PRO" };
const user = { id: 2, name: "User", email: "user@example.com", role: "USER", plan: "FREE" };
const headers = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "authorization, content-type", "Access-Control-Allow-Methods": "GET, POST, PUT, OPTIONS" };
const summary = { startDate: "2026-10-12", endDate: "2026-10-18", importAttempts: 10, completedImports: 8, failedImports: 2, successRate: 80, previewFailures: 3, tradesSaved: 42, duplicates: 5, firstImports: 4, parserBreakdown: [{ key: "TOPSTEP", count: 6 }, { key: "TRADOVATE", count: 3 }, { key: "UNKNOWN", count: 1 }], failureBreakdown: [{ stage: "preview", category: "UNSUPPORTED_FORMAT", count: 2 }, { stage: "save", category: "SAVE_ERROR", count: 2 }], generatedAt: "2026-10-18T15:00:00" };
const recent = [{ occurredAt: "2026-10-18T14:30:00", user: "u***@example.com", accountId: 4, stage: "save", status: "COMPLETED", parserSource: "TOPSTEP", parsedTrades: 5, savedRows: 4, duplicateRows: 1, failureCategory: null, firstImport: true }];

test("admin import health shows operational lifecycle metrics without raw metadata", async ({ page }) => {
  await mockWorkspace(page, admin);
  await page.goto("/admin/import-health");
  await expect(page.getByRole("heading", { name: "Import Health" })).toBeVisible();
  await expect(page.getByRole("link", { name: "Import Health" })).toBeVisible();
  await expect(page.getByText("Import Attempts")).toBeVisible();
  await expect(page.getByText("Preview Failures", { exact: true })).toBeVisible();
  await expect(page.getByText("UNKNOWN")).toBeVisible();
  await expect(page.getByText("UNSUPPORTED_FORMAT")).toBeVisible();
  await expect(page.getByText("Recent Imports")).toBeVisible();
  await expect(page.getByTestId("recent-imports-scroll")).toHaveCSS("overflow-x", "auto");
  await page.getByRole("button", { name: "Today" }).click();
  await expect(page.getByRole("button", { name: "Today" })).toHaveAttribute("aria-pressed", "true");
  for (const width of [1440, 1024, 768, 390]) {
    await page.setViewportSize({ width, height: 844 });
    await expect(page.getByRole("heading", { name: "Import Health" })).toBeVisible();
    expect(await page.locator("html").evaluate((element) => element.scrollWidth <= element.clientWidth + 1)).toBe(true);
  }
});

test("normal users are denied the import health route", async ({ page }) => {
  await mockWorkspace(page, user);
  await page.goto("/admin/import-health");
  await expect(page.getByRole("heading", { name: "Admin access required" })).toBeVisible();
  await expect(page.getByRole("link", { name: "Import Health" })).toHaveCount(0);
});

async function mockWorkspace(page: import("@playwright/test").Page, currentUser: typeof admin) {
  await page.route("**/api/**", async route => {
    const requestUrl = new URL(route.request().url());
    if (requestUrl.origin === new URL(page.url()).origin) return route.continue();
    const pathname = requestUrl.pathname;
    const json = (body: unknown) => route.fulfill({ status: 200, headers, contentType: "application/json", body: JSON.stringify(body) });
    if (pathname.endsWith("/auth/me")) return json(currentUser);
    if (pathname.endsWith("/accounts")) return json([{ id: 1, name: "Account" }]);
    if (pathname.endsWith("/billing/usage")) return json({ plan: currentUser.plan, limits: {}, remaining: {} });
    if (pathname.endsWith("/features")) return json({ v2FeaturesEnabled: false, v3FeaturesEnabled: false, features: { coach: true } });
    if (pathname.endsWith("/activation/status")) return json({});
    if (pathname.endsWith("/pre-analysis/access")) return json({ allowed: false });
    if (pathname.endsWith("/backtests/access") || pathname.endsWith("/manual-backtest/access")) return json({ allowed: false });
    if (pathname.endsWith("/admin/import-health/summary")) return json(summary);
    if (pathname.endsWith("/admin/import-health/recent")) return json(recent);
    if (pathname.endsWith("/trades")) return json({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 });
    if (pathname.includes("/stats/") || pathname.endsWith("/analytics/calendar-summary")) return json([]);
    if (pathname.endsWith("/admin/dashboard/summary")) return json({});
    return route.fulfill({ status: 404, headers });
  });
  await page.goto("/login", { waitUntil: "domcontentloaded" });
  await page.evaluate((value) => { localStorage.setItem("tradepilot_token", "test-token"); localStorage.setItem("tradepilot_user", JSON.stringify(value)); sessionStorage.setItem("tradepilot_token", "test-token"); sessionStorage.setItem("tradepilot_user", JSON.stringify(value)); }, currentUser);
  await page.goto("about:blank").catch(() => undefined);
}
