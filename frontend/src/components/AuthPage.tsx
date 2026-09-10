import { useEffect, useMemo, useState } from "react";
import type React from "react";
import axios from "axios";
import { useLocation, useNavigate } from "react-router-dom";
import {
  login,
  register,
  resendVerification,
  type AuthSession,
  type LoginRequest,
  type RegisterRequest,
} from "../api/authApi";
import {
  AuthIntro,
  authButtonStyle,
  authErrorStyle,
  authFormShellStyle,
  authInputStyle,
  authLabelStyle,
  authLinkButtonStyle,
  authMessageStyle,
  authPageStyle,
  authPanelStyle,
} from "./authStyles";
import { activationSourceFromSearch, persistActivationSource } from "../utils/activationRedirect";
import Seo from "./Seo";
import { readSeoAttributionPayload } from "../utils/seoAttribution";

type AuthMode = "login" | "register";

type AuthPageProps = {
  onAuthenticated: (session: AuthSession) => void;
  initialMode?: AuthMode;
  sessionMessage?: string;
};

function getErrorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data;

    if (typeof data === "string") return data;
    if (data && typeof data === "object") {
      const maybeMessage = (data as { message?: unknown; error?: unknown }).message;
      const maybeError = (data as { message?: unknown; error?: unknown }).error;

      if (typeof maybeMessage === "string") return maybeMessage;
      if (typeof maybeError === "string") return maybeError;
    }
  }

  return "Authentication failed. Check your details and try again.";
}

export default function AuthPage({ onAuthenticated, initialMode = "login", sessionMessage = "" }: AuthPageProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const sessionExpiredMessage =
    (location.state as { sessionExpiredMessage?: string } | null)?.sessionExpiredMessage ?? "";
  const initialMessage = sessionMessage || sessionExpiredMessage;
  const [mode, setMode] = useState<AuthMode>(initialMode);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [resending, setResending] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState(initialMessage);
  const [unverifiedEmail, setUnverifiedEmail] = useState("");

  useEffect(() => {
    setMessage(initialMessage);
  }, [initialMessage]);

  const isRegister = mode === "register";
  const seoPath = location.pathname === "/register" || isRegister ? "/register" : "/login";
  const seoTitle = isRegister
    ? "Create a TradePilot Coach Account | AI Trading Journal for Futures Traders"
    : "Login to TradePilot Coach | AI Trading Journal and Futures Analytics";
  const seoDescription = isRegister
    ? "Create your TradePilot Coach account to import trades, track performance, and use AI coaching for futures trading improvement."
    : "Log in to TradePilot Coach to access your AI trading journal, futures analytics dashboard, coaching insights, and performance reports.";
  const copy = useMemo(
    () =>
      isRegister
        ? {
            title: "Create your account",
            subtitle: "Start with a private workspace for imports, journaling, and execution coaching.",
            action: "Create account",
            switchPrompt: "Already have an account?",
            switchAction: "Log in",
          }
        : {
            title: "Welcome back",
            subtitle: "Log in to continue your TradePilot Coach workspace.",
            action: "Log in",
            switchPrompt: "New to TradePilot Coach?",
            switchAction: "Create an account",
          },
    [isRegister]
  );

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setMessage("");
    setSubmitting(true);

    try {
      const trimmedEmail = email.trim();
      const payload: LoginRequest | RegisterRequest = isRegister
        ? { name: name.trim(), email: trimmedEmail, password, seoAttribution: readSeoAttributionPayload() }
        : { email: trimmedEmail, password };

      if (isRegister) {
        const response = await register(payload as RegisterRequest);
        persistActivationSource(activationSourceFromSearch(location.search));
        setMessage(response.message);
        setMode("login");
        setPassword("");
        return;
      }

      const session = await login(payload);
      onAuthenticated(session);
    } catch (err) {
      const nextError = getErrorMessage(err);
      setError(nextError);
      if (nextError === "Please verify your email before logging in.") {
        setUnverifiedEmail(email.trim());
      }
    } finally {
      setSubmitting(false);
    }
  }

  function switchMode(nextMode: AuthMode) {
    setMode(nextMode);
    setError("");
    setMessage("");
    setUnverifiedEmail("");
  }

  async function handleResendVerification() {
    if (!unverifiedEmail) return;

    setResending(true);
    setError("");
    setMessage("");

    try {
      const response = await resendVerification(unverifiedEmail);
      setMessage(response.message);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setResending(false);
    }
  }

  return (
    <main style={authPageStyle}>
      <Seo title={seoTitle} description={seoDescription} path={seoPath} robots="noindex,nofollow" />
      <AuthIntro />

      <section style={authFormShellStyle}>
        <div style={authPanelStyle}>
          <div style={{ display: "flex", gap: 8, marginBottom: 24 }}>
            <button
              type="button"
              onClick={() => switchMode("login")}
              style={{
                flex: 1,
                padding: "10px 12px",
                borderRadius: 8,
                border: "1px solid rgba(148,163,184,0.22)",
                background: mode === "login" ? "rgba(34,197,94,0.18)" : "transparent",
                color: mode === "login" ? "#bbf7d0" : "#94a3b8",
                fontWeight: 900,
                cursor: "pointer",
              }}
            >
              Login
            </button>
            <button
              type="button"
              onClick={() => switchMode("register")}
              style={{
                flex: 1,
                padding: "10px 12px",
                borderRadius: 8,
                border: "1px solid rgba(148,163,184,0.22)",
                background: mode === "register" ? "rgba(34,197,94,0.18)" : "transparent",
                color: mode === "register" ? "#bbf7d0" : "#94a3b8",
                fontWeight: 900,
                cursor: "pointer",
              }}
            >
              Register
            </button>
          </div>

          <h2 style={{ margin: "0 0 8px", fontSize: 30, lineHeight: 1.1 }}>{copy.title}</h2>
          <p style={{ margin: "0 0 24px", color: "#94a3b8", lineHeight: 1.55 }}>
            {copy.subtitle}
          </p>

          <form onSubmit={handleSubmit} style={{ display: "grid", gap: 18 }}>
            {isRegister && (
              <label>
                <span style={authLabelStyle}>Name</span>
                <input
                  required
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  autoComplete="name"
                  style={authInputStyle}
                  placeholder="Atish Shrestha"
                />
              </label>
            )}

            <label>
              <span style={authLabelStyle}>Email</span>
              <input
                required
                type="email"
                data-testid="login-email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                autoComplete="email"
                style={authInputStyle}
                placeholder="you@example.com"
              />
            </label>

            <label>
              <span style={authLabelStyle}>Password</span>
              <input
                required
                type="password"
                data-testid="login-password"
                minLength={isRegister ? 8 : undefined}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete={isRegister ? "new-password" : "current-password"}
                style={authInputStyle}
                placeholder={isRegister ? "At least 8 characters" : "Your password"}
              />
            </label>

            {!isRegister && (
              <button
                type="button"
                onClick={() => navigate("/forgot-password")}
                style={{ ...authLinkButtonStyle, justifySelf: "end" }}
              >
                Forgot password?
              </button>
            )}

            {message && (
              <div role="status" data-testid="auth-message" style={authMessageStyle}>
                {message}
              </div>
            )}

            {error && (
              <div
                role="alert"
                data-testid="login-error"
                style={authErrorStyle}
              >
                {error}
                {unverifiedEmail && (
                  <div style={{ marginTop: 10 }}>
                    <button
                      type="button"
                      onClick={handleResendVerification}
                      disabled={resending}
                      style={authLinkButtonStyle}
                    >
                      {resending ? "Sending..." : "Resend verification email"}
                    </button>
                  </div>
                )}
              </div>
            )}

            <button type="submit" data-testid="login-submit" disabled={submitting} style={{ ...authButtonStyle, opacity: submitting ? 0.7 : 1 }}>
              {submitting ? "Please wait..." : copy.action}
            </button>
          </form>

          <p style={{ margin: "22px 0 0", color: "#94a3b8", textAlign: "center" }}>
            {copy.switchPrompt}{" "}
            <button
              type="button"
              onClick={() => switchMode(isRegister ? "login" : "register")}
              style={authLinkButtonStyle}
            >
              {copy.switchAction}
            </button>
          </p>
        </div>
      </section>
    </main>
  );
}
