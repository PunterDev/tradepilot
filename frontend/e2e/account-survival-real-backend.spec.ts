import { expect, test } from "@playwright/test";

const apiBaseUrl = process.env.E2E_API_URL ?? "http://127.0.0.1:18080/api";

type Fixture = {
  ownerToken: string;
  otherToken: string;
  owner: { id: number; name: string; email: string; plan: "FREE" | "PRO"; role: "USER" | "ADMIN" };
  sourceAccountId: number;
  laterAccountId: number;
  sessionDate: string;
};

const subsequentSessionCsv = `symbol,qty,buyPrice,sellPrice,pnl,boughtTimestamp,soldTimestamp
NQZ6,1,20000.00,19990.00,-200.00,10/2/2026 10:00:00 AM,10/2/2026 10:05:00 AM
NQZ6,1,20000.00,20005.00,100.00,10/2/2026 10:30:00 AM,10/2/2026 10:35:00 AM
`;

test("Account Survival uses the real backend for profile, evidence, commitment, evaluation, and ownership", async ({ page, request }) => {
  test.setTimeout(90_000);
  const devTokenResponse = await request.get(`${apiBaseUrl}/dev/token`, { failOnStatusCode: false });
  expect(devTokenResponse.ok(), await devTokenResponse.text()).toBeTruthy();
  const devToken = (await devTokenResponse.json() as { token: string }).token;
  const fixtureResponse = await request.post(`${apiBaseUrl}/dev/e2e/account-survival/prepare`, {
    headers: { Authorization: `Bearer ${devToken}` },
    failOnStatusCode: false,
  });
  expect(fixtureResponse.ok(), await fixtureResponse.text()).toBeTruthy();
  const fixture = await fixtureResponse.json() as Fixture;
  expect(fixture.owner.role).toBe("USER");
  const ownerAuthResponse = await request.get(`${apiBaseUrl}/v1/auth/me`, {
    headers: { Authorization: `Bearer ${fixture.ownerToken}` },
    failOnStatusCode: false,
  });
  expect(ownerAuthResponse.ok(), await ownerAuthResponse.text()).toBeTruthy();
  const owner = await ownerAuthResponse.json() as Fixture["owner"];
  expect(owner.role).toBe("USER");
  expect(owner.plan).toBe("PRO");
  for (const path of [
    `/accounts/${fixture.laterAccountId}/survival`,
    `/accounts/${fixture.laterAccountId}/survival/profile`,
    `/accounts/${fixture.laterAccountId}/survival/commitment?date=${fixture.sessionDate}`,
    `/accounts/${fixture.laterAccountId}/survival/commitment/${fixture.sessionDate}/evaluation`,
    `/accounts/${fixture.laterAccountId}/survival/commitment/${fixture.sessionDate}/evaluations`,
  ]) {
    const freeResponse = await request.get(`${apiBaseUrl}${path}`, {
      headers: { Authorization: `Bearer ${fixture.otherToken}` },
      failOnStatusCode: false,
    });
    expect(freeResponse.status()).toBe(403);
    expect(await freeResponse.json()).toMatchObject({
      errorCode: "SUBSCRIPTION_REQUIRED",
      requiredPlan: "PRO",
      feature: "SESSION_REVIEW",
    });
  }
  const freeWrite = await request.put(`${apiBaseUrl}/accounts/${fixture.laterAccountId}/survival/commitment`, {
    headers: { Authorization: `Bearer ${fixture.otherToken}` },
    data: { effectiveSessionDate: fixture.sessionDate, maxAttempts: 2 },
    failOnStatusCode: false,
  });
  expect(freeWrite.status()).toBe(403);

  const pageErrors: string[] = [];
  const survivalResponses: Array<{ url: string; status: number; body: Record<string, unknown> | null }> = [];
  page.on("pageerror", (error) => pageErrors.push(error.message));
  page.on("response", async (response) => {
    if (!response.url().includes(`/accounts/${fixture.sourceAccountId}/survival`)) return;
    let body: Record<string, unknown> | null = null;
    try { body = await response.json() as Record<string, unknown>; } catch { /* Not every response has a JSON body. */ }
    survivalResponses.push({ url: response.url(), status: response.status(), body });
  });

  await page.addInitScript((session) => {
    localStorage.setItem("tradepilot_token", session.token);
    localStorage.setItem("tradepilot_user", JSON.stringify(session.user));
    localStorage.setItem("tradepilot_selected_account_id", String(session.accountId));
    sessionStorage.setItem("tradepilot_token", session.token);
    sessionStorage.setItem("tradepilot_user", JSON.stringify(session.user));
  }, { token: fixture.ownerToken, user: owner, accountId: fixture.sourceAccountId });

  await page.goto("/session-review");
  await expect(page.getByTestId("session-review-heading")).toBeVisible();
  await expect(page.getByRole("combobox")).toHaveValue(String(fixture.sourceAccountId));
  await page.getByLabel("Trading Date").fill(fixture.sessionDate);
  await expect(page.getByRole("button", { name: "Complete risk profile" })).toBeVisible();

  const profileResponse = page.waitForResponse((response) => response.request().method() === "PUT"
    && response.url().endsWith(`/accounts/${fixture.sourceAccountId}/survival/profile`));
  await page.getByRole("button", { name: "Complete risk profile" }).click();
  await page.getByLabel("Current account balance").fill("50000");
  await page.getByLabel("Failure / drawdown threshold").fill("48000");
  await page.getByLabel("Daily loss limit").fill("500");
  await page.getByLabel("Planned risk per trade (1R)").fill("150");
  await page.getByLabel("Personal maximum contracts").fill("2");
  await page.getByLabel("Firm-permitted maximum contracts").fill("5");
  await page.getByRole("button", { name: "Save risk profile" }).click();
  const savedProfile = await profileResponse;
  expect(savedProfile.ok()).toBeTruthy();
  await expect(page.getByRole("heading", { name: "Fragile" })).toBeVisible();
  await expect(page.getByText("$2,000.00")).toBeVisible();
  await expect(page.getByText("5.0 losses")).toBeVisible();

  await page.reload();
  await page.getByLabel("Trading Date").fill(fixture.sessionDate);
  await expect(page.getByRole("heading", { name: "Fragile" })).toBeVisible();
  await expect(page.getByText("$2,000.00")).toBeVisible();

  const evidenceButton = page.getByRole("button", { name: "Cross-account evidence" });
  await expect(evidenceButton).toBeVisible();
  await evidenceButton.click();
  const evidence = evidenceButton.locator("xpath=..");
  await expect(evidence).toContainText("Topstep 50K A");
  await expect(evidence).toContainText("Apex 50K B");
  await expect(evidence).toContainText("8 min 0s");
  await expect(evidence).toContainText("20.00 units");
  await expect(evidence).toContainText("Moderate");

  const commitmentResponse = page.waitForResponse((response) => response.request().method() === "PUT"
    && response.url().endsWith(`/accounts/${fixture.sourceAccountId}/survival/commitment`));
  await page.getByRole("button", { name: "Edit draft commitment" }).click();
  await page.getByLabel("Maximum attempts").fill("3");
  await page.getByRole("button", { name: "Save commitment" }).click();
  expect((await commitmentResponse).ok()).toBeTruthy();
  const acceptedResponse = page.waitForResponse((response) => response.request().method() === "POST"
    && response.url().endsWith(`/accounts/${fixture.sourceAccountId}/survival/commitment/${fixture.sessionDate}/accept`));
  await page.getByRole("button", { name: "Accept commitment" }).click();
  expect((await acceptedResponse).ok()).toBeTruthy();
  await expect(page.getByText("Accepted").first()).toBeVisible();
  await expect(page.getByRole("button", { name: "Edit draft commitment" })).toHaveCount(0);

  const advancedClock = await request.post(`${apiBaseUrl}/dev/e2e/account-survival/advance-date?date=2026-10-03`, {
    headers: { Authorization: `Bearer ${devToken}` },
    failOnStatusCode: false,
  });
  expect(advancedClock.ok(), await advancedClock.text()).toBeTruthy();

  const importedSession = await request.post(`${apiBaseUrl}/trades/import/csv?accountId=${fixture.sourceAccountId}`, {
    headers: { Authorization: `Bearer ${fixture.ownerToken}` },
    multipart: {
      file: {
        name: "account-survival-subsequent-session.csv",
        mimeType: "text/csv",
        buffer: Buffer.from(subsequentSessionCsv),
      },
    },
    failOnStatusCode: false,
  });
  expect(importedSession.ok(), await importedSession.text()).toBeTruthy();
  expect((await importedSession.json() as { savedCount: number }).savedCount).toBe(2);

  await page.reload();
  await page.getByLabel("Trading Date").fill(fixture.sessionDate);
  await expect(page.getByText("Accepted").first()).toBeVisible();
  await expect(page.getByRole("region", { name: "Commitment evaluation", exact: true })).toContainText("Broken");
  await expect(page.getByText("Max Session Loss — Broken")).toBeVisible();

  for (const viewport of [
    { width: 375, height: 844 },
    { width: 390, height: 844 },
    { width: 768, height: 1024 },
    { width: 1440, height: 1024 },
  ]) {
    await page.setViewportSize(viewport);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth + 1)).toBeTruthy();
  }
  await expect(page.getByText("Fresh Setup Confirmation — Not Evaluable")).toBeVisible();
  await expect(page.getByText("Configured: $200.00 · Observed: -$200.00")).toBeVisible();
  await page.getByRole("button", { name: "Show evidence" }).last().click();
  await expect(page.getByText(/Completed-trade imports do not provide contemporaneous confirmation evidence/)).toBeVisible();

  await page.reload();
  await page.getByLabel("Trading Date").fill(fixture.sessionDate);
  await expect(page.getByText("Max Session Loss — Broken")).toBeVisible();

  const assessment = survivalResponses.find((item) => {
    const turningPoint = item.body?.turningPoint as Record<string, unknown> | undefined;
    return item.url.includes("?date=") && !item.url.includes("commitment")
      && item.body?.state === "FRAGILE"
      && turningPoint?.type === "CROSS_ACCOUNT_CONTINUATION";
  })?.body;
  expect(assessment).toMatchObject({ state: "FRAGILE", remainingBuffer: expect.anything(), typicalLoss: expect.anything(), normalLossCapacity: expect.anything() });
  const turningPoint = assessment?.turningPoint as Record<string, unknown>;
  expect(turningPoint).toMatchObject({ type: "CROSS_ACCOUNT_CONTINUATION", crossAccountEvidence: expect.anything() });
  const crossEvidence = turningPoint.crossAccountEvidence as Record<string, unknown>;
  expect(crossEvidence).toMatchObject({ sourceAccounts: ["Topstep 50K A"], laterAccounts: ["Apex 50K B"], elapsedSeconds: 480, previousNormalizedExposure: expect.anything(), newNormalizedExposure: expect.anything(), confidence: "MODERATE" });
  const durableEvaluation = survivalResponses.slice().reverse().find((item) => item.url.endsWith(`/commitment/${fixture.sessionDate}/evaluation`))?.body;
  expect(durableEvaluation).toMatchObject({ available: true, overallStatus: "BROKEN", evaluationVersion: expect.anything(), rules: expect.any(Array) });
  expect((durableEvaluation?.rules as Array<Record<string, unknown>>).some((rule) => rule.rule === "FRESH_SETUP_CONFIRMATION" && rule.status === "NOT_EVALUABLE")).toBeTruthy();
  const unauthorized = await request.get(`${apiBaseUrl}/accounts/${fixture.sourceAccountId}/survival?date=${fixture.sessionDate}`, {
    headers: { Authorization: `Bearer ${fixture.otherToken}` },
    failOnStatusCode: false,
  });
  expect(unauthorized.status()).toBe(404);
  const unauthorizedBody = await unauthorized.text();
  expect(unauthorizedBody).not.toContain("Topstep 50K A");
  expect(unauthorizedBody).not.toContain("Apex 50K B");
  expect(unauthorizedBody).not.toContain("2000");

  await page.selectOption("select", String(fixture.laterAccountId));
  await expect(page.getByRole("heading", { name: "Insufficient data" })).toBeVisible();

  expect(survivalResponses.filter((response) => response.status >= 400)).toEqual([]);
  expect(pageErrors).toEqual([]);
});
