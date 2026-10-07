import { expect, test, type Page } from "@playwright/test";

const account = { id: 7, name: "Reconciliation account", propFirm: "TRADOVATE", status: "ACTIVE" };
const preview = { detectedFormat: "TRADOVATE", sourceAccountNames: [], totalRows: 32, filledRows: 32, cancelledRowsIgnored: 0, invalidRows: 0, completedTrades: 32, duplicateTrades: 0, incompletePositions: 0, warnings: [], trades: [{ symbol: "NQ", side: "LONG", tradeDate: "2026-10-01", entryTime: "2026-10-01T10:00:00", exitTime: "2026-10-01T10:05:00", quantity: 1, entryPrice: 20000, exitPrice: 20010, pnlAmount: 664.3, originalBrokerSymbol: "NQ", contractSymbol: "NQ", externalTradeKey: "trade-1", sourceExecutionIds: [], duplicate: false }] };
const reconciliation = (status: string, reasons: string[] = []) => ({ importId: 123, source: "TRADOVATE", tradeCount: 32, netPnl: 664.3, status, reviewedAt: null, mismatchReasons: reasons, comment: null });

async function openDashboard(page: Page, initial = reconciliation("NOT_REVIEWED")) {
  let current = initial;
  await page.route("**/api/**", async route => {
    const requestUrl = new URL(route.request().url());
    if (requestUrl.port === new URL(page.url() || "http://localhost:5174").port) return route.continue();
    const path = requestUrl.pathname;
    if (path.endsWith("/accounts")) return route.fulfill({ json: [account] });
    if (path.endsWith("/auth/me")) return route.fulfill({ json: { id: 1, name: "PW", email: "pw@example.com", plan: "PRO", role: "USER" } });
    if (path.includes("/trades/import/preview")) return route.fulfill({ json: preview });
    if (path.includes("/trades/import/csv")) return route.fulfill({ json: { preview, savedTrades: [], savedCount: 32, importId: 123 } });
    if (path.includes("/data-trust/imports/123/reconciliation")) { if (route.request().method() === "PUT") { const body = route.request().postDataJSON(); current = reconciliation(body.status, body.mismatchReasons ?? []); } return route.fulfill({ json: current }); }
    if (path.endsWith("/trades")) return route.fulfill({ json: { content: [] } });
    if (path.includes("/calendar") || path.includes("/activation")) return route.fulfill({ json: [] });
    if (path.includes("/billing/usage")) return route.fulfill({ json: { plan: "PRO", limits: {}, usageThisMonth: {}, remaining: {} } });
    if (path.endsWith("/features")) return route.fulfill({ json: { v2FeaturesEnabled: false, v3FeaturesEnabled: false, features: { coach: true } } });
    return route.fulfill({ json: {} });
  });
  await page.goto("/login", { waitUntil: "domcontentloaded" });
  await page.evaluate(() => { const user = { id: 1, name: "PW", email: "pw@example.com", plan: "PRO", role: "USER" }; localStorage.setItem("tradepilot_token", "pw"); localStorage.setItem("tradepilot_user", JSON.stringify(user)); localStorage.setItem("tradepilot_selected_account_id", "7"); sessionStorage.setItem("tradepilot_token", "pw"); sessionStorage.setItem("tradepilot_user", JSON.stringify(user)); });
  await page.goto("about:blank");
  await page.goto("/dashboard");
  await page.getByTestId("import-file-input").setInputFiles({ name: "trades.csv", mimeType: "text/csv", buffer: Buffer.from("header\nvalue") });
  await page.getByTestId("import-confirm-button").click();
  await expect(page.getByText("Does this match your trading platform?")).toBeVisible();
}

test("reconciliation match, mismatch, revision, and dashboard continuation", async ({ page }) => {
  await openDashboard(page);
  await expect(page.getByText("Trades imported: 32")).toBeVisible();
  await expect(page.getByText("Net P&L: +$664.30")).toBeVisible();
  await page.getByRole("button", { name: "Yes, looks correct" }).click();
  await expect(page.getByText("You marked this import as matching your platform.")).toBeVisible();
  await page.getByRole("button", { name: "Change response" }).click();
  await page.getByLabel("P&L doesn't match").check();
  await page.getByLabel("Trade count doesn't match").check();
  await page.getByRole("button", { name: "Submit" }).click();
  await expect(page.getByText("You reported an issue with this import.")).toBeVisible();
  await expect(page.getByText(/P&L doesn't match.*Trade count doesn't match/)).toBeVisible();
  await expect(page.getByRole("heading", { name: "Dashboard" })).toBeVisible();
});
