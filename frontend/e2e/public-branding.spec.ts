import { expect, test } from "@playwright/test";

const siteUrl = "https://www.tradepilotcoach.com";
const socialImage = `${siteUrl}/assets/tradepilot-coach-og.png`;

test("public auth routes use the TradePilot Coach wordmark and noindex metadata", async ({ page }) => {
  await page.goto("/login");

  await expect(page).toHaveTitle("Login to TradePilot Coach | AI Trading Journal and Futures Analytics");
  await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", "noindex,nofollow");
  await expect(page.getByAltText("TradePilot Coach")).toBeVisible();
  await expect(page.getByRole("heading", { name: "Build a better trading review process." })).toBeVisible();

  await page.goto("/forgot-password");
  await expect(page).toHaveTitle("Reset Your TradePilot Coach Password");
  await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", "noindex,nofollow");
});

test("public support and legal pages use TradePilot Coach route-specific metadata", async ({ page }) => {
  for (const [path, title] of [
    ["/contact", "Contact TradePilot Coach | AI Trading Journal Support"],
    ["/privacy", "TradePilot Coach Privacy Policy | AI Trading Journal Data Privacy"],
    ["/terms", "TradePilot Coach Terms of Service | AI Trading Journal"],
  ]) {
    await page.goto(path);
    await expect(page).toHaveTitle(title);
    await expect(page.getByText("TradePilot Coach", { exact: true }).first()).toBeVisible();
  }
});

test("homepage uses the TradePilot Coach social preview metadata", async ({ page }) => {
  await page.goto("/");

  await expect(page.locator('meta[property="og:image"]')).toHaveAttribute("content", socialImage);
  await expect(page.locator('meta[property="og:image:width"]')).toHaveAttribute("content", "1200");
  await expect(page.locator('meta[property="og:image:height"]')).toHaveAttribute("content", "630");
  await expect(page.locator('meta[property="og:image:alt"]')).toHaveAttribute(
    "content",
    "TradePilot Coach AI execution intelligence dashboard preview"
  );
  await expect(page.locator('meta[name="twitter:card"]')).toHaveAttribute("content", "summary_large_image");
  await expect(page.locator('meta[name="twitter:image"]')).toHaveAttribute("content", socialImage);
  await expect(page.locator('meta[name="twitter:image:alt"]')).toHaveAttribute(
    "content",
    "TradePilot Coach AI execution intelligence dashboard preview"
  );
});
