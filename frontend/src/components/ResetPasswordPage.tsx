import { useState } from "react";
import type React from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { resetPassword } from "../api/authApi";
import Seo from "./Seo";
import {
  AuthIntro,
  authButtonStyle,
  authErrorStyle,
  authFormShellStyle,
  authInputStyle,
  authLabelStyle,
  authMessageStyle,
  authPageStyle,
  authPanelStyle,
} from "./authStyles";

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setMessage("");
    setError("");

    const token = searchParams.get("token");

    if (!token) {
      setError("Reset link is missing a token.");
      setSubmitting(false);
      return;
    }

    if (password !== confirmPassword) {
      setError("Passwords do not match.");
      setSubmitting(false);
      return;
    }

    try {
      const response = await resetPassword(token, password);
      setMessage(response.message);
      setPassword("");
      setConfirmPassword("");
    } catch {
      setError("This reset link is invalid, expired, or already used.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main style={authPageStyle}>
      <Seo
        title="Choose a New TradePilot Coach Password"
        description="Set a new password for your TradePilot Coach account."
        path="/reset-password"
        robots="noindex,nofollow"
      />
      <AuthIntro />
      <section style={authFormShellStyle}>
        <div style={authPanelStyle}>
          <h2 style={{ margin: "0 0 8px", fontSize: 30 }}>Choose a new password</h2>
          <p style={{ margin: "0 0 24px", color: "#94a3b8", lineHeight: 1.55 }}>
            Use at least 8 characters.
          </p>

          <form onSubmit={handleSubmit} style={{ display: "grid", gap: 18 }}>
            <label>
              <span style={authLabelStyle}>New password</span>
              <input
                required
                minLength={8}
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete="new-password"
                style={authInputStyle}
                placeholder="At least 8 characters"
              />
            </label>

            <label>
              <span style={authLabelStyle}>Confirm password</span>
              <input
                required
                minLength={8}
                type="password"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                autoComplete="new-password"
                style={authInputStyle}
                placeholder="Repeat new password"
              />
            </label>

            {message && <div style={authMessageStyle}>{message}</div>}
            {error && <div style={authErrorStyle}>{error}</div>}

            <button
              type="submit"
              disabled={submitting}
              style={{ ...authButtonStyle, opacity: submitting ? 0.7 : 1 }}
            >
              {submitting ? "Resetting..." : "Reset password"}
            </button>
          </form>

          {message && (
            <button
              type="button"
              onClick={() => navigate("/login")}
              style={{ ...authButtonStyle, marginTop: 18 }}
            >
              Back to login
            </button>
          )}
        </div>
      </section>
    </main>
  );
}
