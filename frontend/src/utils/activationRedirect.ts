import { getAuthItem, TOKEN_STORAGE_KEY, USER_STORAGE_KEY } from "../api/authStorage";

const PENDING_ACTIVATION_SOURCE_KEY = "tradepilot_pending_activation_source";

export const ACTIVATION_IMPORT_DESTINATION = "/dashboard?activation=import";

const SOURCE_BY_PUBLIC_PATH: Record<string, string> = {
  "/tools/trading-expectancy-calculator": "expectancy-calculator",
  "/tools/overtrading-calculator": "overtrading-calculator",
  "/tools/revenge-trading-calculator": "revenge-trading-calculator",
  "/futures-trading-journal": "futures-trading-journal",
  "/topstep-trading-journal": "topstep-trading-journal",
  "/tradovate-trading-journal": "tradovate-trading-journal",
  "/prop-firm-trading-journal": "prop-firm-trading-journal",
  "/ai-trading-journal": "ai-trading-journal",
  "/trading-psychology": "trading-psychology",
  "/revenge-trading": "revenge-trading",
  "/overtrading": "overtrading",
  "/trading-discipline": "trading-discipline",
  "/breaking-trading-rules": "breaking-trading-rules",
  "/trading-after-losses": "trading-after-losses",
};

const ALLOWED_SOURCES = new Set(Object.values(SOURCE_BY_PUBLIC_PATH));

function hasBrowserStorage() {
  return typeof window !== "undefined" && Boolean(window.localStorage);
}

export function activationSourceForPublicPath(path: string) {
  return SOURCE_BY_PUBLIC_PATH[path] ?? null;
}

export function isAllowedActivationSource(source: string | null | undefined) {
  return Boolean(source && ALLOWED_SOURCES.has(source));
}

export function registrationDestinationForSource(source: string) {
  return isAllowedActivationSource(source) ? `/register?source=${encodeURIComponent(source)}` : "/register";
}

export function activationSourceFromSearch(search: string) {
  const source = new URLSearchParams(search).get("source");
  return isAllowedActivationSource(source) ? source : null;
}

export function persistActivationSource(source: string | null | undefined) {
  if (!isAllowedActivationSource(source) || !hasBrowserStorage()) return;
  window.localStorage.setItem(PENDING_ACTIVATION_SOURCE_KEY, source as string);
}

export function readPendingActivationSource() {
  if (!hasBrowserStorage()) return null;
  const source = window.localStorage.getItem(PENDING_ACTIVATION_SOURCE_KEY);
  return isAllowedActivationSource(source) ? source : null;
}

export function clearPendingActivationSource() {
  if (!hasBrowserStorage()) return;
  window.localStorage.removeItem(PENDING_ACTIVATION_SOURCE_KEY);
}

export function isStoredUserAuthenticated() {
  if (!hasBrowserStorage()) return false;
  return Boolean(getAuthItem(TOKEN_STORAGE_KEY) && getAuthItem(USER_STORAGE_KEY));
}

export function activationAwareCtaDestination(source: string) {
  return isStoredUserAuthenticated() ? ACTIVATION_IMPORT_DESTINATION : registrationDestinationForSource(source);
}
