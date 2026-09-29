import { expect, test } from "@playwright/test";

const siteUrl = "https://www.tradepilotcoach.com";
const guides = [
  ["/guides/how-to-export-topstep-trades", "How to Export Your Topstep Trades"],
  ["/guides/how-to-export-tradovate-trades", "How to Export Your Tradovate Trade History"],
  ["/guides/how-to-review-a-losing-trading-session", "How to Review a Losing Trading Session Without Chasing P&L"],
  ["/guides/how-to-identify-overtrading-from-trade-history", "How to Tell If You Are Overtrading Using Your Own Trade History"],
] as const;

test("resources hub exposes all supporting guides and navigation", async ({ page }) => {
  await page.goto("/resources");
  await expect(page).toHaveTitle("Trading Journal Resources & Futures Trading Guides | TradePilot Coach");
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute("href", `${siteUrl}/resources`);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Trading Resources for Reviewing Your Execution");
  for (const [path, heading] of guides) await expect(page.getByRole("link", { name: heading })).toHaveAttribute("href", path);
  await expect(page.locator(".marketing-nav").getByRole("link", { name: "Resources" }).first()).toHaveAttribute("href", "/resources");

  await page.setViewportSize({ width: 390, height: 844 });
  const menuButton = page.getByRole("button", { name: "Toggle navigation" });
  await expect(menuButton).toBeVisible();
  await menuButton.click();
  const mobilePanel = page.locator(".marketing-mobile-panel");
  await expect(mobilePanel).toHaveClass(/open/);
  await expect(mobilePanel.getByRole("link", { name: "Resources" })).toHaveAttribute("href", "/resources");
});

for (const [path, heading] of guides) {
  test(`${path} has canonical metadata and truthful breadcrumbs`, async ({ page }) => {
    await page.goto(path);
    await expect(page.locator("h1")).toHaveCount(1);
    await expect(page.getByRole("heading", { level: 1, name: heading })).toBeVisible();
    await expect(page.locator('link[rel="canonical"]')).toHaveAttribute("href", `${siteUrl}${path}`);
    await expect(page.locator('meta[name="robots"]')).toHaveCount(0);
    const schema = await page.locator('script[type="application/ld+json"]').evaluateAll((nodes) => nodes.flatMap((node) => JSON.parse(node.textContent ?? "{}")["@graph"] ?? []));
    expect(schema.some((node: { "@type": string }) => node["@type"] === "WebPage")).toBeTruthy();
    const breadcrumb = schema.find((node: { "@type": string }) => node["@type"] === "BreadcrumbList");
    expect(breadcrumb.itemListElement.map((item: { name: string }) => item.name)).toEqual(["Home", "Resources", heading]);
    await expect(page.getByRole("navigation", { name: "Breadcrumb" }).locator('[aria-current="page"]')).toHaveText(heading);
  });
}

test("guide backlinks connect platform, session, and overtrading clusters", async ({ page }) => {
  await page.goto("/topstep-trading-journal");
  await expect(page.getByRole("link", { name: "How to export Topstep trades" })).toHaveAttribute("href", "/guides/how-to-export-topstep-trades");
  await page.goto("/tradovate-trading-journal");
  await expect(page.getByRole("link", { name: "How to export Tradovate trades" })).toHaveAttribute("href", "/guides/how-to-export-tradovate-trades");
  await page.goto("/overtrading");
  await expect(page.getByRole("link", { name: "How to identify overtrading from trade history" })).toHaveAttribute("href", "/guides/how-to-identify-overtrading-from-trade-history");
  await page.goto("/tools/overtrading-calculator");
  await expect(page.getByRole("link", { name: "Learn how to identify overtrading from trade history." })).toHaveAttribute("href", "/guides/how-to-identify-overtrading-from-trade-history");
});

test("resources and guides remain usable on compact screens", async ({ page }) => {
  for (const width of [390, 320]) {
    await page.setViewportSize({ width, height: 844 });
    await page.goto("/guides/how-to-export-topstep-trades");
    expect(await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)).toBeLessThanOrEqual(1);
  }
});
