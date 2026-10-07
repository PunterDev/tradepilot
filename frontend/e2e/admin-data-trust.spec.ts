import { expect, test } from "@playwright/test";

const quality = {
  importTrust: { total: 12, reviewed: 10, matched: 8, mismatched: 2, notReviewed: 2, userConfirmedMatchRate: 80, bySource: [{ key: "TRADOVATE", rated: 9, positive: 8, negative: 1, rate: 88.89 }], mismatchReasons: { PNL_MISMATCH: 2 } },
  insightTrust: { rated: 10, positive: 7, negative: 3, positiveRate: 70, byFindingType: [{ key: "OVERTRADING", rated: 3, positive: 2, negative: 1, rate: 66.67 }], byEvidenceStrength: [{ key: "STRONG", rated: 3, positive: 2, negative: 1, rate: 66.67 }], byVersion: [{ key: "v1 / r1", rated: 3, positive: 2, negative: 1, rate: 66.67 }], negativeReasons: { WRONG_CONCLUSION: 1 } },
};

test("admin data trust renders aggregate dimensions", async ({ page }) => {
  const admin = { id: 1, name: "Admin", email: "admin@example.com", role: "ADMIN", plan: "PRO" };
  await page.route(apiPath("auth/me"), route => route.fulfill(json(admin)));
  await page.route(apiPath("accounts"), route => route.fulfill(json([{ id: 1, name: "Admin Account", propFirm: "TOPSTEP", status: "ACTIVE" }])));
  await page.route(apiPath("billing/usage"), route => route.fulfill(json({ plan: "PRO", limits: {}, usageThisMonth: {}, remaining: {} })));
  await page.route(apiPath("features"), route => route.fulfill(json({ v2FeaturesEnabled: false, v3FeaturesEnabled: false, features: { coach: true } })));
  await page.route(apiPath("activation/status"), route => route.fulfill(json({})));
  await page.route(apiPath("pre-analysis/access"), route => route.fulfill(json({ allowed: false })));
  await page.route(apiPath("trades"), route => route.fulfill(json({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 })));
  await page.route(apiPath("analytics/calendar-summary"), route => route.fulfill(json([])));
  await page.route(apiPath("stats/summary"), route => route.fulfill(json({ totalTrades: 0, winRate: 0, totalPnl: 0 })));
  await page.route(apiPath("stats/by-setup"), route => route.fulfill(json([])));
  await page.route(apiPath("stats/by-session"), route => route.fulfill(json([])));
  await page.route(apiPath("stats/by-symbol"), route => route.fulfill(json([])));
  await page.route(apiPath("stats/by-weekday"), route => route.fulfill(json([])));
  await page.route(apiPath("admin/dashboard/summary"), route => route.fulfill(json({})));
  await page.route(apiPath("admin/data-trust/quality"), route => route.fulfill(json(quality)));
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

function json(body: unknown) { return { status: 200, contentType: "application/json", body: JSON.stringify(body) }; }
function apiPath(path: string) { return new RegExp(`.*\\/api(?:\\/v1)?\\/${path.replace(/\//g, "\\/")}\\/?(?:\\?.*)?$`); }
