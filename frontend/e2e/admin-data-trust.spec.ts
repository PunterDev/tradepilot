import { expect, test } from "@playwright/test";

const quality = {
  importTrust: { total: 12, reviewed: 10, matched: 8, mismatched: 2, notReviewed: 2, userConfirmedMatchRate: 80, bySource: [{ key: "TRADOVATE", rated: 9, positive: 8, negative: 1, rate: 88.89 }], mismatchReasons: { PNL_MISMATCH: 2 } },
  insightTrust: { rated: 10, positive: 7, negative: 3, positiveRate: 70, byFindingType: [{ key: "OVERTRADING", rated: 3, positive: 2, negative: 1, rate: 66.67 }], byEvidenceStrength: [{ key: "STRONG", rated: 3, positive: 2, negative: 1, rate: 66.67 }], byVersion: [{ key: "v1 / r1", rated: 3, positive: 2, negative: 1, rate: 66.67 }], negativeReasons: { WRONG_CONCLUSION: 1 } },
};

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, content-type",
  "Access-Control-Allow-Methods": "GET, POST, PUT, OPTIONS",
};

test("admin data trust renders aggregate dimensions", async ({ page }) => {
  const admin = { id: 1, name: "Admin", email: "admin@example.com", role: "ADMIN", plan: "PRO" };
  await page.route("**/api/**", async (route) => {
    const requestUrl = new URL(route.request().url());
    if (requestUrl.origin === new URL(page.url()).origin) return route.continue();

    const { pathname } = requestUrl;
    if (pathname.endsWith("/auth/me")) return route.fulfill(json(admin));
    if (pathname.endsWith("/accounts")) return route.fulfill(json([{ id: 1, name: "Admin Account", propFirm: "TOPSTEP", status: "ACTIVE" }]));
    if (pathname.endsWith("/billing/usage")) return route.fulfill(json({ plan: "PRO", limits: {}, usageThisMonth: {}, remaining: {} }));
    if (pathname.endsWith("/features")) return route.fulfill(json({ v2FeaturesEnabled: false, v3FeaturesEnabled: false, features: { coach: true } }));
    if (pathname.endsWith("/activation/status")) return route.fulfill(json({}));
    if (pathname.endsWith("/pre-analysis/access")) return route.fulfill(json({ allowed: false }));
    if (pathname.endsWith("/trades")) return route.fulfill(json({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 }));
    if (pathname.endsWith("/analytics/calendar-summary")) return route.fulfill(json([]));
    if (pathname.endsWith("/stats/summary")) return route.fulfill(json({ totalTrades: 0, winRate: 0, totalPnl: 0 }));
    if (pathname.endsWith("/stats/by-setup") || pathname.endsWith("/stats/by-session") || pathname.endsWith("/stats/by-symbol") || pathname.endsWith("/stats/by-weekday")) return route.fulfill(json([]));
    if (pathname.endsWith("/admin/dashboard/summary")) return route.fulfill(json({}));
    if (pathname.endsWith("/admin/data-trust/quality")) return route.fulfill(json(quality));
    return route.fulfill({ status: 404, headers: corsHeaders });
  });
  await page.goto("/login", { waitUntil: "domcontentloaded" });
  await page.evaluate((user) => { window.localStorage.setItem("tradepilot_token", "admin-token"); window.localStorage.setItem("tradepilot_user", JSON.stringify(user)); window.sessionStorage.setItem("tradepilot_token", "admin-token"); window.sessionStorage.setItem("tradepilot_user", JSON.stringify(user)); }, admin);
  await page.goto("about:blank");
  await page.goto("/admin/data-trust");
  await expect(page.getByRole("heading", { name: "Data Trust & Insight Quality" })).toBeVisible();
  await expect(page.getByText("Reviewed imports")).toBeVisible();
  await expect(page.getByText("User-confirmed match rate")).toBeVisible();
  await expect(page.getByText("PNL MISMATCH")).toBeVisible();
  await expect(page.getByText("WRONG CONCLUSION")).toBeVisible();
});

function json(body: unknown) { return { status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify(body) }; }
