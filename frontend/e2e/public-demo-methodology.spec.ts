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
  await expect(page.getByRole("heading", { name: "Review the session before drawing a conclusion." })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Post-loss overtrading" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "One thing to focus on next session." })).toBeVisible();
  await expect(page.getByText("Static sample data only. It is not customer performance, a trade signal, financial advice, or a promise of future results.")).toBeVisible();
  await expect(page.getByText("64").first()).toBeVisible();
  await expect(page.getByText("-$483.75").first()).toBeVisible();
  await expect(page.getByText("Does this match your trading platform?")).toBeVisible();
  await expect(page.getByRole("link", { name: "Leak Detector" })).toBeVisible();
  await expect(page.getByText("MODERATE evidence").first()).toBeVisible();
  await expect(page.getByText("14").first()).toBeVisible();
  await expect(page.getByText("Historical simulation", { exact: true })).toBeVisible();
  await expect(page.getByText("If this sample had paused after the trigger:")).toBeVisible();
  await expect(page.getByText("Net historical impact")).toBeVisible();
  await expect(page.getByText("Historical simulation based on this sample only. Not a prediction of future results.")).toBeVisible();
  await expect(page.getByText("10-19 Moderate")).toBeVisible();
  await expect(page.getByText("Current sample")).toBeVisible();
  await expect(page.getByText("R values in this demo use a predefined $100 risk unit.")).toBeVisible();
  await page.getByRole("link", { name: "See the coaching rule" }).click();
  await expect(page.getByRole("heading", { name: "After two consecutive losses, pause trading for 20 minutes." })).toBeVisible();
  await expect(page.getByRole("link", { name: "Try TradePilot Free" }).last()).toHaveAttribute("href", "/register?source=sample-demo");
  await expect(page.getByRole("link", { name: "Back home" })).toHaveAttribute("href", "/");
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

for (const viewport of [
  { width: 375, height: 812 },
  { width: 768, height: 1024 },
  { width: 1440, height: 900 },
]) {
  test(`/demo keeps its review flow within the viewport at ${viewport.width}px`, async ({ page }) => {
    await page.setViewportSize(viewport);
    await page.goto("/demo");
    expect(await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)).toBeLessThanOrEqual(1);
  });
}
