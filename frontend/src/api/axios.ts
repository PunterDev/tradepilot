import axios from "axios";

export const TOKEN_STORAGE_KEY = "tradepilot_token";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api",
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY);

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
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      localStorage.removeItem("tradepilot_user");
      window.dispatchEvent(
        new CustomEvent("tradepilot:auth-expired", {
          detail: { reason: "expired" },
        })
      );
    }

    return Promise.reject(error);
  }
);
