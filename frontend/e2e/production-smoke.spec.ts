import { expect, test } from "@playwright/test";

const enabled = process.env.RUN_PROD_SMOKE === "true";
const email = process.env.TRADEPILOT_SMOKE_EMAIL;
const password = process.env.TRADEPILOT_SMOKE_PASSWORD;

test.describe("controlled production smoke", () => {
  test.skip(!enabled, "Set RUN_PROD_SMOKE=true to run against a deployed environment.");

  test("controlled account can log in, use core routes, and cannot see V2/V3", async ({ page }) => {
    test.skip(!email || !password, "Set TRADEPILOT_SMOKE_EMAIL and TRADEPILOT_SMOKE_PASSWORD for the dedicated smoke account.");
    if (!email || !password) return;

    await page.goto("/login");
    await page.getByLabel("Email").fill(email);
    await page.getByLabel("Password").fill(password);
    await page.getByRole("button", { name: "Log in" }).click();
    await expect(page).toHaveURL(/\/(dashboard|admin)/);

    await page.goto("/calendar");
    await expect(page).not.toHaveURL(/\/login/);
    await expect(page.getByText("AI Pre-Analysis", { exact: true })).toHaveCount(0);

    await page.goto("/coach/pre-analysis");
    await expect(page).toHaveURL(/\/dashboard/);

    await page.getByRole("button", { name: "Logout" }).click();
    await expect(page).toHaveURL(/\/login/);
  });
});
