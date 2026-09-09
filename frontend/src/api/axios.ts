import axios from "axios";
import { getAuthItem, removeAuthItem, TOKEN_STORAGE_KEY, USER_STORAGE_KEY } from "./authStorage";
export { TOKEN_STORAGE_KEY } from "./authStorage";

export const API_PREFIX = "/api/v1";

const DEFAULT_API_ORIGIN = "http://localhost:8080";

function normalizeApiBaseUrl(rawBaseUrl: string | undefined) {
  const configuredValue = rawBaseUrl?.trim();
  if (import.meta.env.PROD && !configuredValue) {
    throw new Error("VITE_API_BASE_URL is required for production builds.");
  }
  const value = configuredValue || DEFAULT_API_ORIGIN;
  const withoutTrailingSlash = value.replace(/\/+$/, "");

  if (withoutTrailingSlash.endsWith(API_PREFIX)) {
    return withoutTrailingSlash;
  }

  if (withoutTrailingSlash.endsWith("/api")) {
    return `${withoutTrailingSlash}/v1`;
  }

  return `${withoutTrailingSlash}${API_PREFIX}`;
}

export const api = axios.create({
  baseURL: normalizeApiBaseUrl(import.meta.env.VITE_API_BASE_URL),
});

api.interceptors.request.use((config) => {
  const token = getAuthItem(TOKEN_STORAGE_KEY);

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    const requestUrl = String(error?.config?.url ?? "");
    const isAuthRequest =
      requestUrl.includes("/auth/login") || requestUrl.includes("/auth/register");

    if (status === 401 && !isAuthRequest) {
      removeAuthItem(TOKEN_STORAGE_KEY);
      removeAuthItem(USER_STORAGE_KEY);
      window.dispatchEvent(
        new CustomEvent("tradepilot:auth-expired", {
          detail: { reason: "expired" },
        })
      );
    }

    return Promise.reject(error);
  }
);
