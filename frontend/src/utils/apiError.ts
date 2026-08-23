import axios from "axios";

type ApiErrorBody = {
  message?: unknown;
  error?: unknown;
  userMessage?: unknown;
  requestId?: unknown;
};

export function extractApiError(error: unknown, fallback: string) {
  if (!axios.isAxiosError(error)) {
    return fallback;
  }
  const data = error.response?.data;
  const requestId = requestIdFrom(error);
  let message = "";
  if (typeof data === "string" && data.trim()) {
    message = data.trim();
  } else if (typeof data === "object" && data !== null) {
    const body = data as ApiErrorBody;
    message = firstString(body.userMessage, body.message, body.error);
  }
  return appendRequestId(message || fallback, requestId);
}

export function requestIdFrom(error: unknown) {
  if (!axios.isAxiosError(error)) {
    return "";
  }
  const body = error.response?.data;
  if (typeof body === "object" && body !== null) {
    const requestId = (body as ApiErrorBody).requestId;
    if (typeof requestId === "string" && requestId.trim()) {
      return requestId.trim();
    }
  }
  const headerValue = error.response?.headers?.["x-request-id"] ?? error.response?.headers?.["x-request-Id"];
  return typeof headerValue === "string" ? headerValue.trim() : "";
}

function firstString(...values: unknown[]) {
  for (const value of values) {
    if (typeof value === "string" && value.trim()) {
      return value.trim();
    }
  }
  return "";
}

function appendRequestId(message: string, requestId: string) {
  if (!requestId || requestId === "unavailable" || message.includes("Reference ID:")) {
    return message;
  }
  return `${message} Reference ID: ${requestId}`;
}
