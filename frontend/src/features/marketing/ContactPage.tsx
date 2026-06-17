import { useState, type FormEvent } from "react";
import axios from "axios";
import { sendContactMessage } from "../../api/contactApi";
import Seo from "../../components/Seo";
import "./HomePage.css";
import { MarketingShell } from "./MarketingLayout";

type ContactForm = {
  name: string;
  email: string;
  subject: string;
  message: string;
};

const initialForm: ContactForm = {
  name: "",
  email: "",
  subject: "",
  message: "",
};

export default function ContactPage() {
  const [form, setForm] = useState<ContactForm>(initialForm);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  function updateField(field: keyof ContactForm, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
    setError("");
    setSuccess(false);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmed = {
      name: form.name.trim(),
      email: form.email.trim(),
      subject: form.subject.trim(),
      message: form.message.trim(),
    };

    if (!trimmed.name || !trimmed.email || !trimmed.subject || !trimmed.message) {
      setError("Please complete every field before sending.");
      return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed.email)) {
      setError("Enter a valid email address.");
      return;
    }

    setSubmitting(true);
    setError("");
    setSuccess(false);

    try {
      await sendContactMessage(trimmed);
      setForm(initialForm);
      setSuccess(true);
    } catch (err) {
      setError(getContactError(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <MarketingShell>
      <Seo
        title="Contact TradePilot | AI Trading Journal Support"
        description="Contact TradePilot for questions about AI trading journals, futures analytics, CSV imports, billing, and trader coaching workflows."
        path="/contact"
      />
      <section className="policy-hero contact-hero">
        <div className="marketing-container">
          <div className="marketing-eyebrow">Contact Us</div>
          <h1 className="marketing-title">Talk to TradePilot</h1>
          <p className="marketing-subtitle">
            Questions about journaling, prop firm workflows, AI coaching, or billing? Send a note and we will get back to you soon.
          </p>
        </div>
      </section>

      <section className="contact-section">
        <div className="marketing-container contact-grid">
          <aside className="contact-info-card">
            <h2>Direct support</h2>
            <p>
              For direct support, email: <a href="mailto:support@tradepilot.app">support@tradepilot.app</a>
            </p>
            <div className="contact-mini-list">
              <span>Prop firm trader questions</span>
              <span>CSV import help</span>
              <span>AI coach and billing support</span>
            </div>
          </aside>

          <form className="contact-form" onSubmit={handleSubmit}>
            <label>
              <span>Name</span>
              <input
                value={form.name}
                onChange={(event) => updateField("name", event.target.value)}
                placeholder="Your name"
              />
            </label>
            <label>
              <span>Email</span>
              <input
                type="email"
                value={form.email}
                onChange={(event) => updateField("email", event.target.value)}
                placeholder="you@example.com"
              />
            </label>
            <label>
              <span>Subject</span>
              <input
                value={form.subject}
                onChange={(event) => updateField("subject", event.target.value)}
                placeholder="How can we help?"
              />
            </label>
            <label>
              <span>Message</span>
              <textarea
                value={form.message}
                onChange={(event) => updateField("message", event.target.value)}
                placeholder="Tell us what you need."
                rows={6}
              />
            </label>

            {error && <div className="contact-alert error">{error}</div>}
            {success && (
              <div className="contact-alert success">
                Thanks - your message has been received. We'll get back to you soon.
              </div>
            )}

            <button
              className="marketing-button primary contact-submit"
              type="submit"
              disabled={submitting}
              style={{ opacity: submitting ? 0.72 : 1, cursor: submitting ? "not-allowed" : "pointer" }}
            >
              {submitting ? "Sending..." : success ? "Message Sent" : "Send Message"}
            </button>
          </form>
        </div>
      </section>
    </MarketingShell>
  );
}

function getContactError(error: unknown) {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data;
    if (data && typeof data === "object") {
      const maybeError = (data as { error?: unknown; message?: unknown }).error;
      const maybeMessage = (data as { error?: unknown; message?: unknown }).message;
      if (typeof maybeMessage === "string") return maybeMessage;
      if (typeof maybeError === "string") return maybeError;
    }
  }

  return "Unable to send your message right now. Please try again or email support@tradepilot.app.";
}
