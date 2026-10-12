import { expect, test } from "@playwright/test";

test("sample demo is public, static, and leads visitors into registration", async ({ page }) => {
  const unexpectedProductRequests: string[] = [];
  page.on("request", (request) => {
    const url = new URL(request.url());
    if (url.pathname.startsWith("/api/") && !url.pathname.endsWith("/seo/events")) {
      unexpectedProductRequests.push(request.url());
    }
  });

  await page.goto("/demo");

  await expect(page.getByRole("heading", { name: "See the review loop before you import." })).toBeVisible();
  await expect(page.getByText("Sample data only. Nothing on this page is a trade signal, recommendation, or promise of results.")).toBeVisible();
  await expect(page.getByRole("button", { name: "Leak Detector" })).toBeVisible();
  await expect(page.getByText("42", { exact: true })).toBeVisible();
  await expect(page.getByText("14", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Leak Detector" }).click();
  await expect(page.getByRole("heading", { name: "Revenge Trading After Losses" })).toBeVisible();
  await expect(page.getByText("9", { exact: true })).toBeVisible();
  await expect(page.getByText("Weak evidence")).toBeVisible();
  await expect(page.getByText("-0.62R vs -0.11R")).toBeVisible();
  await expect(page.getByText("0.51R worse")).toBeVisible();
  await expect(page.getByText("Strong evidence")).toHaveCount(0);
  await page.getByRole("button", { name: "See the coaching rule" }).click();
  await expect(page.getByRole("heading", { name: "Pause 20 minutes after two consecutive losses." })).toBeVisible();
  await expect(page.getByRole("link", { name: "Analyze My Own Trades" })).toHaveAttribute("href", "/register?source=sample-demo");
  expect(unexpectedProductRequests).toEqual([]);
});

test("methodology page explains evidence-first analysis and public privacy boundaries", async ({ page }) => {
  await page.goto("/how-tradepilot-works");

  await expect(page.getByRole("heading", { name: "Your history is the evidence. Coaching is the next step." })).toBeVisible();
  await expect(page.getByText("Deterministic analysis")).toBeVisible();
  await expect(page.getByText("Evidence thresholds")).toBeVisible();
  await expect(page.getByText("AI explanation")).toBeVisible();
  await expect(page.getByText("No broker password. No trade execution.")).toBeVisible();
  await expect(page.getByRole("link", { name: "See TradePilot in Action" })).toHaveAttribute("href", "/demo");
});

for (const path of ["/demo", "/how-tradepilot-works"]) {
  test(`${path} remains readable on a small screen`, async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto(path);
    expect(await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)).toBeLessThanOrEqual(1);
  });
}
