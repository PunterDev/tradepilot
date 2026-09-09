import { expect, type APIRequestContext, type Page } from "@playwright/test";

export type DevSession = {
  token: string;
  user: {
    id: number;
    name: string;
    email: string;
    plan: "FREE" | "PRO";
    role: "USER" | "ADMIN";
  };
};

export const apiBaseUrl = process.env.E2E_API_URL ?? "http://localhost:8080/api";

type AuthResponse = {
  token: string;
  userId: number;
  name: string;
  email: string;
  plan?: "FREE" | "PRO";
  role?: "USER" | "ADMIN";
};

export async function requireBackend(request: APIRequestContext) {
  try {
    const response = await request.get(`${apiBaseUrl}/auth/me`, {
      failOnStatusCode: false,
      timeout: 3000,
    });
    return response.status() < 500;
  } catch {
    return false;
  }
}

export async function getDevSession(request: APIRequestContext) {
  let response;
  try {
    response = await request.get(`${apiBaseUrl}/dev/token`, {
      failOnStatusCode: false,
      timeout: 5000,
    });
  } catch {
    return null;
  }

  if (!response.ok()) {
    return null;
  }

  const auth = (await response.json()) as AuthResponse;
  let meResponse;
  try {
    meResponse = await request.get(`${apiBaseUrl}/auth/me`, {
      headers: {
        Authorization: `Bearer ${auth.token}`,
      },
      failOnStatusCode: false,
      timeout: 5000,
    });
  } catch {
    return null;
  }

  if (!meResponse.ok()) {
    return null;
  }

  return {
    token: auth.token,
    user: {
      id: auth.userId,
      name: auth.name,
      email: auth.email,
      plan: auth.plan ?? "FREE",
      role: auth.role ?? "USER",
    },
  } satisfies DevSession;
}

export async function installSession(page: Page, session: DevSession) {
  await page.goto("/", { waitUntil: "domcontentloaded" });
  await page.evaluate((storedSession) => {
    window.localStorage.setItem("tradepilot_token", storedSession.token);
    window.localStorage.setItem("tradepilot_user", JSON.stringify(storedSession.user));
  }, session);
}

export async function seedDemoData(request: APIRequestContext, session: DevSession) {
  const response = await request.post(`${apiBaseUrl}/dev/demo-data/reseed?dryRun=false`, {
    headers: {
      Authorization: `Bearer ${session.token}`,
    },
    failOnStatusCode: false,
    timeout: 15000,
  });

  if (!response.ok()) {
    return false;
  }
  return true;
}

export async function expectAppShell(page: Page) {
  await expect(page.getByRole("button", { name: "Logout" })).toBeVisible();
}
