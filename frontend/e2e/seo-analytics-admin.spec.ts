import { expect, test } from "@playwright/test";

const adminUser = { id: 99, name: "Admin", email: "admin@tradepilot.local", role: "ADMIN", plan: "PRO" };
const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, content-type",
  "Access-Control-Allow-Methods": "GET, POST, PUT, OPTIONS",
};

test("admin SEO acquisition dashboard renders KPI, funnel, tool, source, and campaign sections", async ({ page }) => {
  await page.route(apiPath("auth/me"), async (route) => {
    await route.fulfill({
      status: 200,
      headers: corsHeaders,
      contentType: "application/json",
      body: JSON.stringify({ id: 99, name: "Admin", email: "admin@tradepilot.local", role: "ADMIN", plan: "PRO" }),
    });
  });
  await page.route(apiPath("features"), async (route) => {
    await route.fulfill({
      status: 200,
      headers: corsHeaders,
      contentType: "application/json",
      body: JSON.stringify({ v2FeaturesEnabled: true, v3FeaturesEnabled: true, features: { playbook: true, tradingDna: true, journey: true, tradingMemory: true, tradeReplay: true, coach: true } }),
    });
  });
  await page.route(apiPath("billing/usage"), async (route) => {
    await route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ plan: "PRO", limits: {}, remaining: {} }) });
  });
  await page.route(apiPath("accounts"), async (route) => {
    await route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify([{ id: 1, name: "Admin Account" }]) });
  });
  await page.route(apiPath("activation/status"), async (route) => {
    await route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({}) });
  });
  await page.route(apiPath("pre-analysis/access"), async (route) => {
    await route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ allowed: false }) });
  });
  await page.route(apiPath("admin/dashboard/summary"), async (route) => {
    await route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({}) });
  });
  await page.route(apiPath("trades"), async (route) => {
    await route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 }) });
  });
  await page.route(apiPath("analytics/calendar-summary"), async (route) => {
    await route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify([]) });
  });
  await page.route(apiPath("stats/summary"), async (route) => {
    await route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify({ totalTrades: 0, winRate: 0, totalPnl: 0 }) });
  });
  await page.route(apiPath("stats/by-setup"), async (route) => {
    await route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify([]) });
  });
  await page.route(apiPath("stats/by-session"), async (route) => {
    await route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify([]) });
  });
  await page.route(apiPath("stats/by-symbol"), async (route) => {
    await route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify([]) });
  });
  await page.route(apiPath("stats/by-weekday"), async (route) => {
    await route.fulfill({ status: 200, headers: corsHeaders, contentType: "application/json", body: JSON.stringify([]) });
  });
  await page.route(apiPath("admin/analytics/seo/search-console"), async (route) => {
    await route.fulfill({
      status: 200,
      headers: corsHeaders,
      contentType: "application/json",
      body: JSON.stringify({
        enabled: true,
        available: true,
        status: "Connected",
        property: "sc-domain:tradepilotcoach.com",
        startDate: "2026-08-01",
        endDate: "2026-08-30",
        lastRefreshedAt: "2026-08-30T17:05:00",
        error: null,
        summary: {
          impressions: 3000,
          clicks: 150,
          ctr: 5,
          averagePosition: 7.2,
          brandedImpressions: 200,
          brandedClicks: 10,
          nonBrandedImpressions: 2800,
          nonBrandedClicks: 140,
        },
        pages: [{
          landingPage: "/tools/revenge-trading-calculator",
          impressions: 2800,
          clicks: 140,
          ctr: 5,
          averagePosition: 7.4,
          signups: 20,
          firstImports: 12,
          firstInsights: 8,
          proConversions: 2,
          signupPerGscClick: 14.29,
          activationPerGscClick: 5.71,
          proPerGscClick: 1.43,
          activatedPer100Clicks: 5.71,
          proPer100Clicks: 1.43,
        }],
        queries: [
          {
            query: "revenge trading calculator",
            landingPage: "/tools/revenge-trading-calculator",
            impressions: 2800,
            clicks: 140,
            ctr: 5,
            averagePosition: 7.4,
            searchIntent: "Non-Branded",
            landingPageSignups: 20,
            landingPageFirstInsights: 8,
            landingPageProConversions: 2,
            activatedPer100Clicks: 5.71,
            proPer100Clicks: 1.43,
          },
          {
            query: "<img src=x onerror=alert(1)>",
            landingPage: "/tools/revenge-trading-calculator",
            impressions: 0,
            clicks: 0,
            ctr: 0,
            averagePosition: 0,
            searchIntent: "Non-Branded",
            landingPageSignups: 20,
            landingPageFirstInsights: 8,
            landingPageProConversions: 2,
            activatedPer100Clicks: 0,
            proPer100Clicks: 0,
          },
        ],
        trend: [{ date: "2026-08-30", impressions: 1400, clicks: 70, ctr: 5, averagePosition: 7.2 }],
        opportunities: [{
          category: "Striking Distance",
          query: "revenge trading calculator",
          landingPage: "/tools/revenge-trading-calculator",
          impressions: 2800,
          clicks: 140,
          ctr: 5,
          averagePosition: 7.4,
          landingPageFirstInsights: 8,
          activatedPer100Clicks: 5.71,
          reason: "Average position is between 5 and 20 with meaningful impressions.",
        }],
        notes: ["Conversions are joined by normalized landing page only."],
      }),
    });
  });
  await page.route(apiPath("admin/analytics/seo"), async (route) => {
    await route.fulfill({
      status: 200,
      headers: corsHeaders,
      contentType: "application/json",
      body: JSON.stringify({
        startDate: "2026-08-01",
        endDate: "2026-08-30",
        generatedAt: "2026-08-30T17:00:00",
        kpis: [
          { key: "views", label: "Organic landing views", value: 120, rate: 0, helper: "SEO page and tool views" },
          { key: "ctaClicks", label: "SEO CTA clicks", value: 36, rate: 30, helper: "Register intent" },
          { key: "signups", label: "SEO signups", value: 18, rate: 15, helper: "First-touch registrations" },
          { key: "activated", label: "SEO activated users", value: 9, rate: 50, helper: "First personalized insight" },
          { key: "pro", label: "SEO Pro conversions", value: 3, rate: 16.7, helper: "Attributed Pro upgrades" },
        ],
        funnel: [
          { key: "landing_view", label: "Landing Page View", count: 120, conversionFromPrevious: 100, conversionFromInitial: 100 },
          { key: "cta_click", label: "CTA Click", count: 36, conversionFromPrevious: 30, conversionFromInitial: 30 },
          { key: "signup_completed", label: "Signup Completed", count: 18, conversionFromPrevious: 50, conversionFromInitial: 15 },
          { key: "first_import", label: "First Import", count: 12, conversionFromPrevious: 66.7, conversionFromInitial: 10 },
          { key: "first_insight", label: "First Insight", count: 9, conversionFromPrevious: 75, conversionFromInitial: 7.5 },
          { key: "pro_upgrade", label: "Pro Upgrade", count: 3, conversionFromPrevious: 33.3, conversionFromInitial: 2.5 },
        ],
        landingPages: [{ landingPage: "/tools/revenge-trading-calculator", views: 40, ctaClicks: 12, signups: 7, firstImports: 5, firstInsights: 4, proConversions: 1, signupRate: 17.5, activationRate: 57.1, paidConversionRate: 14.3 }],
        tools: [{ tool: "revenge_trading", views: 40, started: 28, completed: 22, ctaClicks: 12, signups: 7, firstImports: 5, firstInsights: 4, proConversions: 1, completionRate: 78.6, signupRate: 17.5, activationRate: 57.1, paidConversionRate: 14.3 }],
        sources: [{ source: "google", medium: "organic", views: 80, ctaClicks: 24, signups: 12, firstInsights: 6, proConversions: 2, signupRate: 15, activationRate: 50, paidConversionRate: 16.7 }],
        campaigns: [
          { campaign: "revenge", views: 40, signups: 7, firstInsights: 4, proConversions: 1, signupRate: 17.5, activationRate: 57.1, paidConversionRate: 14.3 },
          { campaign: "<img src=x onerror=alert(1)>", views: 1, signups: 0, firstInsights: 0, proConversions: 0, signupRate: 0, activationRate: 0, paidConversionRate: 0 },
        ],
        activationTrend: [{ date: "2026-08-30", signups: 3, firstInsights: 2 }],
        latency: { medianSignupToFirstImportMinutes: 18, medianSignupToFirstInsightMinutes: 44 },
        notes: ["Anonymous analytics use a random local browser id only."],
      }),
    });
  });

  await page.goto("/login", { waitUntil: "domcontentloaded" });
  await page.evaluate((user) => {
    window.localStorage.setItem("tradepilot_token", "admin-token");
    window.localStorage.setItem("tradepilot_user", JSON.stringify(user));
    window.sessionStorage.setItem("tradepilot_token", "admin-token");
    window.sessionStorage.setItem("tradepilot_user", JSON.stringify(user));
  }, adminUser);
  await page.goto("about:blank");

  await page.goto("/admin/analytics/seo");

  await expect(page.getByRole("heading", { name: "SEO Acquisition & Activation" })).toBeVisible();
  await expect(page.getByText("Organic landing views")).toBeVisible();
  await expect(page.getByText("Landing Page Performance")).toBeVisible();
  await expect(page.getByText("Free Tool Performance")).toBeVisible();
  await expect(page.getByText("Source / UTM Performance")).toBeVisible();
  await expect(page.getByText("Campaign Performance")).toBeVisible();
  await expect(page.getByText("Google Search Console")).toBeVisible();
  await expect(page.getByText("Google Impressions")).toBeVisible();
  await expect(page.getByText("3,000")).toBeVisible();
  await expect(page.getByText("Google Search Queries")).toBeVisible();
  await expect(page.getByText("revenge trading calculator").first()).toBeVisible();
  await expect(page.getByText("Activated / 100 Clicks")).toBeVisible();
  await expect(page.getByText("SEO Opportunities")).toBeVisible();
  await expect(page.getByText("Striking Distance")).toBeVisible();
  await expect(page.getByText("Revenge Trading", { exact: true })).toBeVisible();
  await expect(page.getByText("<img src=x onerror=alert(1)>").first()).toBeVisible();
  await expect(page.getByText("Signup to first insight")).toBeVisible();
});

function apiPath(path: string) {
  return new RegExp(`.*\\/api(?:\\/v1)?\\/${path.replace(/\//g, "\\/")}\\/?(?:\\?.*)?$`);
}
