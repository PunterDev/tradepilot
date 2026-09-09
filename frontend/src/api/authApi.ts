import { api, TOKEN_STORAGE_KEY } from "./axios";
import { getAuthItem, removeAuthItem, setAuthItem, USER_STORAGE_KEY } from "./authStorage";
import type { SeoAttributionPayload } from "./seoAnalyticsApi";

export type AuthUser = {
  id: number;
  name: string;
  email: string;
  plan?: "FREE" | "PRO";
  role?: "USER" | "ADMIN";
  monthlyAiCredits?: number;
  monthlyAiCreditsUsed?: number;
  monthlyAiCreditsRemaining?: number;
  purchasedAiCreditsBalance?: number;
  totalAiCreditsAvailable?: number;
};

export type AuthResponse = {
  token: string;
  userId: number;
  name: string;
  email: string;
  plan?: "FREE" | "PRO";
  role?: "USER" | "ADMIN";
  monthlyAiCredits?: number;
  monthlyAiCreditsUsed?: number;
  monthlyAiCreditsRemaining?: number;
  purchasedAiCreditsBalance?: number;
  totalAiCreditsAvailable?: number;
};

export type AuthSession = {
  token: string;
  user: AuthUser;
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type RegisterRequest = LoginRequest & {
  name: string;
  seoAttribution?: SeoAttributionPayload | null;
};

export type MessageResponse = {
  message: string;
};

function toSession(auth: AuthResponse): AuthSession {
  return {
    token: auth.token,
    user: {
      id: auth.userId,
      name: auth.name,
      email: auth.email,
      plan: auth.plan ?? "FREE",
      role: auth.role ?? "USER",
      monthlyAiCredits: auth.monthlyAiCredits,
      monthlyAiCreditsUsed: auth.monthlyAiCreditsUsed,
      monthlyAiCreditsRemaining: auth.monthlyAiCreditsRemaining,
      purchasedAiCreditsBalance: auth.purchasedAiCreditsBalance,
      totalAiCreditsAvailable: auth.totalAiCreditsAvailable,
    },
  };
}

export function persistSession(session: AuthSession) {
  setAuthItem(TOKEN_STORAGE_KEY, session.token);
  setAuthItem(USER_STORAGE_KEY, JSON.stringify(session.user));
}

export function getStoredSession(): AuthSession | null {
  const token = getAuthItem(TOKEN_STORAGE_KEY);
  const rawUser = getAuthItem(USER_STORAGE_KEY);

  if (!token || !rawUser) return null;

  try {
    const user = JSON.parse(rawUser) as AuthUser;
    return { token, user };
  } catch {
    logout();
    return null;
  }
}

export async function login(payload: LoginRequest): Promise<AuthSession> {
  const res = await api.post<AuthResponse>("/auth/login", payload);
  const session = toSession(res.data);
  persistSession(session);
  return session;
}

export async function register(payload: RegisterRequest): Promise<MessageResponse> {
  const res = await api.post<MessageResponse>("/auth/register", payload);
  return res.data;
}

export async function getCurrentUser(): Promise<AuthUser> {
  const res = await api.get<AuthUser>("/auth/me");
  const stored = getStoredSession();

  if (stored) {
    persistSession({ token: stored.token, user: res.data });
  }

  return res.data;
}

export function logout() {
  removeAuthItem(TOKEN_STORAGE_KEY);
  removeAuthItem(USER_STORAGE_KEY);
}

export async function verifyEmail(token: string): Promise<MessageResponse> {
  const res = await api.post<MessageResponse>("/auth/verify-email", { token });
  return res.data;
}

export async function resendVerification(email: string): Promise<MessageResponse> {
  const res = await api.post<MessageResponse>("/auth/resend-verification", { email });
  return res.data;
}

export async function forgotPassword(email: string): Promise<MessageResponse> {
  const res = await api.post<MessageResponse>("/auth/forgot-password", { email });
  return res.data;
}

export async function resetPassword(token: string, newPassword: string): Promise<MessageResponse> {
  const res = await api.post<MessageResponse>("/auth/reset-password", {
    token,
    newPassword,
  });
  return res.data;
}
