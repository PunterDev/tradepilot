import { expect, test } from "@playwright/test";
import { expectAppShell, getDevSession, installSession, requireBackend, seedDemoData } from "./helpers";

test("login form authenticates a local dev user", async ({ page, request }) => {
  const email = process.env.E2E_LOGIN_EMAIL;
  const password = process.env.E2E_LOGIN_PASSWORD;
  test.skip(
    !email || !password,
    "Set E2E_LOGIN_EMAIL and E2E_LOGIN_PASSWORD for the real login-form smoke test."
  );
  if (!email || !password) return;

  const backendReady = await requireBackend(request);
  test.skip(
    !backendReady,
    "Backend is unavailable; start Spring Boot before running the login-form smoke test."
  );
  if (!backendReady) return;

  await page.goto("/login");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Log in" }).click();

  await expect(page).toHaveURL(/\/dashboard|\/admin/);
  await expectAppShell(page);
});

test("dashboard loads for an authenticated user", async ({ page, request }) => {
  const session = await getDevSession(request);
  test.skip(
    !session,
    "Local dev auth is unavailable. Start backend with DEV_AUTH_ENABLED=true."
  );
  if (!session) return;

  await installSession(page, session);
  await page.goto("/dashboard");

  await expect(page).not.toHaveURL(/\/login/);
  await expectAppShell(page);
  await expect(
    page
      .getByRole("heading", { name: "Dashboard", level: 1 })
      .or(page.getByRole("heading", { name: "Welcome to TradePilot", level: 2 }))
      .first()
  ).toBeVisible();
});

test("normal user cannot access the admin route", async ({ page, request }) => {
  const session = await getDevSession(request);
  test.skip(
    !session,
    "Local dev auth is unavailable. Start backend with DEV_AUTH_ENABLED=true."
  );
  if (!session) return;

  if (session.user.role === "ADMIN") {
    test.skip(true, "Dev auth returned an ADMIN user; use a normal user token for this access-control smoke.");
  }

  await installSession(page, session);
  await page.goto("/admin");

  await expect(page).not.toHaveURL(/\/login/);
  await expect(page.getByText("Admin access required")).toBeVisible();
});

test("analytics heatmap renders seeded demo data", async ({ page, request }) => {
  const session = await getDevSession(request);
  test.skip(
    !session,
    "Local dev auth is unavailable. Start backend with DEV_AUTH_ENABLED=true."
  );
  if (!session) return;

  const seeded = await seedDemoData(request, session);
  test.skip(
    !seeded,
    "Demo data seeding is unavailable. Start backend with spring profile dev or app.dev-tools-enabled=true."
  );
  await installSession(page, session);
  await page.goto("/analytics");

  await expect(page.getByRole("heading", { name: "Performance Heatmap" })).toBeVisible();
  await expect(page.getByText("No heatmap data yet")).toHaveCount(0);
  await expect(page.getByText(/trades/i).first()).toBeVisible();
});

test("expired or invalid auth redirects to login with a session message", async ({ page, request }) => {
  const backendReady = Boolean(await getDevSession(request)) || (await requireBackend(request));
  test.skip(!backendReady, "Backend is unavailable; start Spring Boot before running auth-expiry E2E.");

  await page.addInitScript(() => {
    window.localStorage.setItem("tradepilot_token", "expired.invalid.jwt");
    window.localStorage.setItem(
      "tradepilot_user",
      JSON.stringify({ id: 999, name: "Expired User", email: "expired@example.com", role: "USER", plan: "FREE" })
    );
  });

  await page.goto("/dashboard");

  await expect(page).toHaveURL(/\/login/);
  await expect(page.getByText("Your session expired. Please log in again.")).toBeVisible();
});
