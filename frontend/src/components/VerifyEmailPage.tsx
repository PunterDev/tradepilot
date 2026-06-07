import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { verifyEmail } from "../api/authApi";
import {
  AuthIntro,
  authButtonStyle,
  authErrorStyle,
  authFormShellStyle,
  authMessageStyle,
  authPageStyle,
  authPanelStyle,
} from "./authStyles";

export default function VerifyEmailPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    async function runVerification() {
      const token = searchParams.get("token");

      if (!token) {
        setError("Verification link is missing a token.");
        setLoading(false);
        return;
      }

      try {
        const response = await verifyEmail(token);
        setMessage(response.message);
      } catch {
        setError("This verification link is invalid or expired.");
      } finally {
        setLoading(false);
      }
    }

    runVerification();
  }, [searchParams]);

  return (
    <main style={authPageStyle}>
      <AuthIntro />
      <section style={authFormShellStyle}>
        <div style={authPanelStyle}>
          <h2 style={{ margin: "0 0 8px", fontSize: 30 }}>Verify email</h2>
          <p style={{ margin: "0 0 24px", color: "#94a3b8", lineHeight: 1.55 }}>
            Confirming your TradePilot account.
          </p>

          {loading && <div style={authMessageStyle}>Checking verification link...</div>}
          {message && <div style={authMessageStyle}>{message}</div>}
          {error && <div style={authErrorStyle}>{error}</div>}

          <button
            type="button"
            onClick={() => navigate("/login")}
            style={{ ...authButtonStyle, marginTop: 18 }}
          >
            Back to login
          </button>
        </div>
      </section>
    </main>
  );
}
