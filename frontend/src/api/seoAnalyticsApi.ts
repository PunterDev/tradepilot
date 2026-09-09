import { api } from "./axios";

export type SeoAttributionPayload = {
  anonymousId?: string;
  landingPage: string;
  initialReferrer: string;
  utmSource?: string;
  utmMedium?: string;
  utmCampaign?: string;
  utmContent?: string;
  utmTerm?: string;
  capturedAt: string;
};

export type SeoAcquisitionEventPayload = {
  event: string;
  eventId?: string;
  anonymousId?: string;
  landingPage: string;
  tool?: string;
  ctaLabel?: string;
  ctaDestination?: string;
  referrer?: string;
  utmSource?: string;
  utmMedium?: string;
  utmCampaign?: string;
  utmContent?: string;
  utmTerm?: string;
  timestamp: string;
};

export type SeoAnalyticsKpi = {
  key: string;
  label: string;
  value: number;
  rate: number;
  helper: string;
};

export type SeoAnalyticsFunnelStep = {
  key: string;
  label: string;
  count: number;
  conversionFromPrevious: number;
  conversionFromInitial: number;
};

export type SeoAnalyticsLandingPageRow = {
  landingPage: string;
  views: number;
  ctaClicks: number;
  signups: number;
  firstImports: number;
  firstInsights: number;
  proConversions: number;
  signupRate: number;
  activationRate: number;
  paidConversionRate: number;
};

export type SeoAnalyticsToolRow = {
  tool: string;
  views: number;
  started: number;
  completed: number;
  ctaClicks: number;
  signups: number;
  firstImports: number;
  firstInsights: number;
  proConversions: number;
  completionRate: number;
  signupRate: number;
  activationRate: number;
  paidConversionRate: number;
};

export type SeoAnalyticsSourceRow = {
  source: string;
  medium: string;
  views: number;
  ctaClicks: number;
  signups: number;
  firstInsights: number;
  proConversions: number;
  signupRate: number;
  activationRate: number;
  paidConversionRate: number;
};

export type SeoAnalyticsCampaignRow = {
  campaign: string;
  views: number;
  signups: number;
  firstInsights: number;
  proConversions: number;
  signupRate: number;
  activationRate: number;
  paidConversionRate: number;
};

export type SeoAnalyticsTrendPoint = {
  date: string;
  signups: number;
  firstInsights: number;
};

export type SeoAnalyticsResponse = {
  startDate: string;
  endDate: string;
  generatedAt: string;
  kpis: SeoAnalyticsKpi[];
  funnel: SeoAnalyticsFunnelStep[];
  landingPages: SeoAnalyticsLandingPageRow[];
  tools: SeoAnalyticsToolRow[];
  sources: SeoAnalyticsSourceRow[];
  campaigns: SeoAnalyticsCampaignRow[];
  activationTrend: SeoAnalyticsTrendPoint[];
  latency: {
    medianSignupToFirstImportMinutes?: number | null;
    medianSignupToFirstInsightMinutes?: number | null;
  };
  notes: string[];
};

export type SearchConsoleSummary = {
  impressions: number;
  clicks: number;
  ctr: number;
  averagePosition: number;
  brandedImpressions: number;
  brandedClicks: number;
  nonBrandedImpressions: number;
  nonBrandedClicks: number;
};

export type SearchConsolePageRow = {
  landingPage: string;
  impressions: number;
  clicks: number;
  ctr: number;
  averagePosition: number;
  signups: number;
  firstImports: number;
  firstInsights: number;
  proConversions: number;
  signupPerGscClick: number;
  activationPerGscClick: number;
  proPerGscClick: number;
  activatedPer100Clicks: number;
  proPer100Clicks: number;
};

export type SearchConsoleQueryRow = {
  query: string;
  landingPage: string;
  impressions: number;
  clicks: number;
  ctr: number;
  averagePosition: number;
  searchIntent: string;
  landingPageSignups: number;
  landingPageFirstInsights: number;
  landingPageProConversions: number;
  activatedPer100Clicks: number;
  proPer100Clicks: number;
};

export type SearchConsoleTrendPoint = {
  date: string;
  impressions: number;
  clicks: number;
  ctr: number;
  averagePosition: number;
};

export type SearchConsoleOpportunityRow = {
  category: string;
  query: string;
  landingPage: string;
  impressions: number;
  clicks: number;
  ctr: number;
  averagePosition: number;
  landingPageFirstInsights: number;
  activatedPer100Clicks: number;
  reason: string;
};

export type SearchConsoleAnalyticsResponse = {
  enabled: boolean;
  available: boolean;
  status: string;
  property?: string | null;
  startDate: string;
  endDate: string;
  lastRefreshedAt?: string | null;
  error?: string | null;
  summary: SearchConsoleSummary;
  pages: SearchConsolePageRow[];
  queries: SearchConsoleQueryRow[];
  trend: SearchConsoleTrendPoint[];
  opportunities: SearchConsoleOpportunityRow[];
  notes: string[];
};

export async function recordSeoAcquisitionEvents(events: SeoAcquisitionEventPayload[]) {
  await api.post("/seo/events", { events });
}

export async function getSeoAnalytics(params: { startDate?: string; endDate?: string }): Promise<SeoAnalyticsResponse> {
  const res = await api.get<SeoAnalyticsResponse>("/admin/analytics/seo", { params });
  return res.data;
}

export async function getSearchConsoleAnalytics(params: { startDate?: string; endDate?: string }): Promise<SearchConsoleAnalyticsResponse> {
  const res = await api.get<SearchConsoleAnalyticsResponse>("/admin/analytics/seo/search-console", { params });
  return res.data;
}
