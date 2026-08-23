import { api } from "./api";

export type EndpointMetric = {
  endpoint: string;
  category?: string | null;
  requestCount: number;
  avgDurationMs: number;
  maxDurationMs: number;
  latestRequestId?: string | null;
};

export type SlowRequest = {
  endpoint: string;
  durationMs: number;
  status: number;
  userId?: number | null;
  severity: string;
  createdAt: string;
};

export type AiFeatureCost = {
  feature: string;
  calls: number;
  tokens: number;
  estimatedCost: number;
};

export type CacheHealth = {
  cacheName: string;
  hits: number;
  misses: number;
  hitRate: number;
};

export type EmailFailure = {
  recipient: string;
  template: string;
  errorMessage?: string | null;
  createdAt: string;
};

export type StripeEvent = {
  eventType: string;
  success: boolean;
  durationMs: number;
  errorMessage?: string | null;
  createdAt: string;
};

export type SystemAlert = {
  id: number;
  type: string;
  severity: string;
  message: string;
  status: "OPEN" | "RESOLVED" | string;
  createdAt: string;
  resolvedAt?: string | null;
  resolvedBy?: number | null;
};

export type ErrorStatusBreakdown = {
  status: number;
  count: number;
};

export type ErrorEndpointBreakdown = {
  endpoint: string;
  category?: string | null;
  count: number;
  latestRequestId?: string | null;
  latestTimestamp?: string | null;
  latestErrorCode?: string | null;
};

export type LatencyCategory = {
  category: string;
  requestCount: number;
  avgDurationMs: number;
  p95DurationMs: number;
};

export type FailureSummary = {
  source: string;
  timestamp?: string | null;
  requestId?: string | null;
  userId?: number | null;
  endpoint?: string | null;
  feature?: string | null;
  errorCode?: string | null;
  userMessage?: string | null;
};

export type ObservabilitySummary = {
  requestsToday: number;
  avgResponseTime: number;
  p95ResponseTime: number;
  slowestEndpoint: string;
  errorRate: number;
  errorRateSeverity?: string | null;
  aiCallsToday: number;
  aiFailuresToday?: number;
  aiTokensToday: number;
  aiCostToday: number;
  aiCostMonth: number;
  aiAvgResponseTime: number;
  aiGenerationP95ResponseTime?: number;
  aiGenerationP95Severity?: string | null;
  mostExpensiveFeature: string;
  mostUsedModel: string;
  cacheHits: number;
  cacheMisses: number;
  cacheHitRate: number;
  cacheHitRateSeverity?: string | null;
  emailsSent: number;
  emailsFailed: number;
  emailSuccessRate: number;
  emailHasAttempts?: boolean;
  emailSuccessRateDisplay?: string | null;
  emailHealthSeverity?: string | null;
  stripeSuccesses: number;
  stripeFailures: number;
  stripeSuccessRate: number;
  stripeHasEvents?: boolean;
  stripeSuccessRateDisplay?: string | null;
  stripeHealthSeverity?: string | null;
  lastStripeEventType?: string | null;
  lastStripeEventTime?: string | null;
  creditPackPurchasesCompletedToday?: number;
  standardApiP95ResponseTime?: number;
  standardApiP95Severity?: string | null;
  adminApiP95ResponseTime?: number;
  slowestEndpoints: EndpointMetric[];
  slowRequests: SlowRequest[];
  aiFeatureCosts: AiFeatureCost[];
  cacheHealth: CacheHealth[];
  recentEmailFailures: EmailFailure[];
  recentStripeEvents: StripeEvent[];
  alerts: SystemAlert[];
  resolvedAlerts: SystemAlert[];
  errorsByStatus?: ErrorStatusBreakdown[];
  errorsByEndpoint?: ErrorEndpointBreakdown[];
  latencyCategories?: LatencyCategory[];
  lastApiFailure?: FailureSummary | null;
  lastStripeFailure?: FailureSummary | null;
  lastEmailFailure?: FailureSummary | null;
  lastAiFailure?: FailureSummary | null;
};

export type ApiTrendPoint = {
  date: string;
  requests: number;
  avgDurationMs: number;
  errorRate: number;
};

export type AiTrendPoint = {
  date: string;
  tokens: number;
  estimatedCost: number;
};

export type ObservabilityTrends = {
  apiPerformance: ApiTrendPoint[];
  aiCosts: AiTrendPoint[];
};

export type ObservabilityRange = "TODAY" | "LAST_7_DAYS" | "LAST_30_DAYS" | "CUSTOM";

export type ObservabilityFilter = {
  range: ObservabilityRange;
  startDate?: string;
  endDate?: string;
};

function filterParams(filter: ObservabilityFilter) {
  const params: Record<string, string> = { range: filter.range };
  if (filter.range === "CUSTOM") {
    if (filter.startDate) params.startDate = filter.startDate;
    if (filter.endDate) params.endDate = filter.endDate;
  }
  return params;
}

export async function getObservabilitySummary(filter: ObservabilityFilter): Promise<ObservabilitySummary> {
  const res = await api.get<ObservabilitySummary>("/admin/observability/summary", {
    params: filterParams(filter),
  });
  return res.data;
}

export async function getObservabilityTrends(filter: ObservabilityFilter): Promise<ObservabilityTrends> {
  const res = await api.get<ObservabilityTrends>("/admin/observability/trends", {
    params: filterParams(filter),
  });
  return res.data;
}

export async function resolveObservabilityAlert(id: number) {
  const res = await api.patch(`/admin/observability/alerts/${id}/resolve`);
  return res.data;
}
