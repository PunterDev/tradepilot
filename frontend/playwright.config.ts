import { defineConfig, devices } from "@playwright/test";

const port = Number(process.env.E2E_FRONTEND_PORT ?? 5173);
const host = process.env.E2E_FRONTEND_HOST ?? "localhost";
const apiBaseURL = process.env.E2E_API_URL ?? process.env.VITE_API_BASE_URL ?? "http://localhost:8080/api";

export default defineConfig({
  testDir: "./e2e",
  timeout: 30_000,
  expect: {
    timeout: 8_000,
  },
  fullyParallel: false,
  reporter: [["list"], ["html", { open: "never" }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? `http://${host}:${port}`,
    trace: "on-first-retry",
    video: "retain-on-failure",
  },
  webServer: {
    command: `npm run dev -- --host 127.0.0.1 --port ${port}`,
    url: `http://${host}:${port}`,
    reuseExistingServer: true,
    timeout: 90_000,
    env: {
      ...process.env,
      VITE_API_BASE_URL: apiBaseURL,
    },
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
});
