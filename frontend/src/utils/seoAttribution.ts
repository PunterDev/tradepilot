import { recordSeoAcquisitionEvents, type SeoAttributionPayload, type SeoAcquisitionEventPayload } from "../api/seoAnalyticsApi";

const ATTRIBUTION_STORAGE_KEY = "tradepilot_first_touch_attribution";
const SEO_EVENT_STORAGE_KEY = "tradepilot_seo_events";
const ANONYMOUS_ID_STORAGE_KEY = "tradepilot_seo_anonymous_id";

const UTM_KEYS = ["utm_source", "utm_medium", "utm_campaign", "utm_content", "utm_term"] as const;

export type SeoAttribution = {
  landing_page: string;
  initial_referrer: string;
  captured_at: string;
  anonymous_id?: string;
} & Partial<Record<(typeof UTM_KEYS)[number], string>>;

export type SeoEvent = {
  event:
    | "seo_landing_page_view"
    | "seo_cta_click"
    | "signup_started_from_seo"
    | "tool_page_view"
    | "expectancy_calculator_started"
    | "expectancy_calculator_completed"
    | "expectancy_result_positive"
    | "expectancy_result_negative"
    | "expectancy_result_breakeven"
    | "expectancy_cta_click"
    | "overtrading_calculator_started"
    | "overtrading_calculator_completed"
    | "overtrading_result_later_worse"
    | "overtrading_result_similar"
    | "overtrading_result_later_better"
    | "overtrading_cta_click"
    | "revenge_calculator_started"
    | "revenge_calculator_completed"
    | "revenge_result_no_clear_deterioration"
    | "revenge_result_mild_deterioration"
    | "revenge_result_strong_post_loss_deterioration"
    | "revenge_calculator_cta_click";
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

function anonymousId() {
  try {
    const existing = window.localStorage.getItem(ANONYMOUS_ID_STORAGE_KEY);
    if (existing) return existing;
    const generated = window.crypto?.randomUUID?.() ?? `seo_${Date.now()}_${Math.random().toString(36).slice(2)}`;
    window.localStorage.setItem(ANONYMOUS_ID_STORAGE_KEY, generated);
    return generated;
  } catch {
    return undefined;
  }
}

function readAttribution(): SeoAttribution | null {
  try {
    const raw = window.localStorage.getItem(ATTRIBUTION_STORAGE_KEY);
    return raw ? (JSON.parse(raw) as SeoAttribution) : null;
  } catch {
    return null;
  }
}

function toEventPayload(
  event: SeoEvent["event"],
  landingPage: string,
  ctaLabel?: string,
  ctaDestination?: string,
  tool?: string
): SeoEvent {
  const attribution = readAttribution();
  return {
    event,
    eventId: window.crypto?.randomUUID?.(),
    anonymousId: anonymousId(),
    landingPage,
    tool,
    ctaLabel,
    ctaDestination,
    referrer: attribution?.initial_referrer,
    utmSource: attribution?.utm_source,
    utmMedium: attribution?.utm_medium,
    utmCampaign: attribution?.utm_campaign,
    utmContent: attribution?.utm_content,
    utmTerm: attribution?.utm_term,
    timestamp: new Date().toISOString(),
  };
}

function pushEvent(payload: SeoEvent) {
  const dataLayer = (window as Window & { dataLayer?: unknown[] }).dataLayer;
  if (Array.isArray(dataLayer)) {
    dataLayer.push(payload);
  }

  try {
    const existing = JSON.parse(window.localStorage.getItem(SEO_EVENT_STORAGE_KEY) ?? "[]") as SeoEvent[];
    window.localStorage.setItem(SEO_EVENT_STORAGE_KEY, JSON.stringify([...existing.slice(-24), payload]));
  } catch {
    window.localStorage.setItem(SEO_EVENT_STORAGE_KEY, JSON.stringify([payload]));
  }

  void recordSeoAcquisitionEvents([payload as SeoAcquisitionEventPayload]).catch(() => {
    // SEO telemetry should never interrupt public-page navigation or calculator use.
  });
}

export function captureFirstTouchAttribution(landingPage: string) {
  if (readAttribution()) return;

  const params = new URLSearchParams(window.location.search);
  const attribution: SeoAttribution = {
    landing_page: landingPage,
    initial_referrer: document.referrer,
    captured_at: new Date().toISOString(),
    anonymous_id: anonymousId(),
  };

  UTM_KEYS.forEach((key) => {
    const value = params.get(key);
    if (value) {
      attribution[key] = value;
    }
  });

  window.localStorage.setItem(ATTRIBUTION_STORAGE_KEY, JSON.stringify(attribution));
}

export function readSeoAttributionPayload(): SeoAttributionPayload | null {
  const attribution = readAttribution();
  if (!attribution) return null;
  return {
    anonymousId: attribution.anonymous_id ?? anonymousId(),
    landingPage: attribution.landing_page,
    initialReferrer: attribution.initial_referrer,
    utmSource: attribution.utm_source,
    utmMedium: attribution.utm_medium,
    utmCampaign: attribution.utm_campaign,
    utmContent: attribution.utm_content,
    utmTerm: attribution.utm_term,
    capturedAt: attribution.captured_at,
  };
}

export function trackSeoLandingPageView(landingPage: string) {
  captureFirstTouchAttribution(landingPage);
  pushEvent(toEventPayload("seo_landing_page_view", landingPage));
}

export function trackSeoCtaClick(landingPage: string, ctaLabel: string, ctaDestination: string) {
  pushEvent(toEventPayload("seo_cta_click", landingPage, ctaLabel, ctaDestination));
  if (ctaDestination.startsWith("/register")) {
    pushEvent(toEventPayload("signup_started_from_seo", landingPage, ctaLabel, ctaDestination));
  }
}

export function trackSeoToolEvent(event: SeoEvent["event"], landingPage: string, tool: string) {
  captureFirstTouchAttribution(landingPage);
  pushEvent(toEventPayload(event, landingPage, undefined, undefined, tool));
}

export function trackSeoToolCtaClick(
  landingPage: string,
  tool: string,
  ctaLabel: string,
  ctaDestination: string,
  event: SeoEvent["event"] = "expectancy_cta_click"
) {
  captureFirstTouchAttribution(landingPage);
  pushEvent(toEventPayload(event, landingPage, ctaLabel, ctaDestination, tool));
  if (ctaDestination.startsWith("/register")) {
    pushEvent(toEventPayload("signup_started_from_seo", landingPage, ctaLabel, ctaDestination, tool));
  }
}
