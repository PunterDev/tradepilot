import axios from "axios";

export type CoachLimitState = {
  limitReached: boolean;
  message: string;
  hasCachedSnapshot: boolean;
};

export function isLimitReachedError(error: unknown) {
  return axios.isAxiosError(error) && error.response?.status === 429;
}

export function coachLimitMessage(error: unknown, fallback: string) {
  if (!axios.isAxiosError(error)) return fallback;

  const responseData = error.response?.data;
  if (typeof responseData === "string" && responseData.trim()) {
    return responseData;
  }

  if (responseData && typeof responseData === "object") {
    const data = responseData as { message?: unknown; error?: unknown };
    if (typeof data.message === "string" && data.message.trim()) {
      return data.message;
    }
    if (typeof data.error === "string" && data.error.trim()) {
      return data.error;
    }
  }

  return fallback;
}
