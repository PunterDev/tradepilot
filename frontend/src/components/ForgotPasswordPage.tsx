import { useState } from "react";
import type React from "react";
import { useNavigate } from "react-router-dom";
import { forgotPassword } from "../api/authApi";
import Seo from "./Seo";
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

export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setMessage("");
    setError("");

    try {
      const response = await forgotPassword(email.trim());
      setMessage(response.message);
    } catch {
      setError("Could not submit the request. Please check the email format and try again.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main style={authPageStyle}>
      <Seo
        title="Reset Your TradePilot Coach Password"
        description="Request a password reset for your TradePilot Coach account."
        path="/forgot-password"
        robots="noindex,nofollow"
      />
      <AuthIntro />
      <section style={authFormShellStyle}>
        <div style={authPanelStyle}>
          <h2 style={{ margin: "0 0 8px", fontSize: 30 }}>Reset password</h2>
          <p style={{ margin: "0 0 24px", color: "#94a3b8", lineHeight: 1.55 }}>
            Enter your email and we’ll send a reset link if the account exists.
          </p>

          <form onSubmit={handleSubmit} style={{ display: "grid", gap: 18 }}>
            <label>
              <span style={authLabelStyle}>Email</span>
              <input
                required
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                autoComplete="email"
                style={authInputStyle}
                placeholder="you@example.com"
              />
            </label>

            {message && <div style={authMessageStyle}>{message}</div>}
            {error && <div style={authErrorStyle}>{error}</div>}

            <button
              type="submit"
              disabled={submitting}
              style={{ ...authButtonStyle, opacity: submitting ? 0.7 : 1 }}
            >
              {submitting ? "Sending..." : "Send reset link"}
            </button>
          </form>

          <p style={{ margin: "22px 0 0", color: "#94a3b8", textAlign: "center" }}>
            <button type="button" onClick={() => navigate("/login")} style={authLinkButtonStyle}>
              Back to login
            </button>
          </p>
        </div>
      </section>
    </main>
  );
}
