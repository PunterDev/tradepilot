import { useState } from "react";
import { backfillFeatureUsage, clearDemoData, mockDowngradeFree, mockUpgradePro, seedDemoData } from "../api/devToolsApi";
import { button, card, mutedText, sectionTitle } from "../styles/dashboardStyles";

type Props = {
  onChanged: () => void | Promise<void>;
};

export default function DevToolsCard({ onChanged }: Props) {
  const [busy, setBusy] = useState("");
  const [message, setMessage] = useState("");

  async function run(label: string, action: () => Promise<void>) {
    setBusy(label);
    setMessage("");
    try {
      await action();
      await onChanged();
      setMessage(`${label} complete.`);
    } catch (err: any) {
      const status = err?.response?.status;
      setMessage(status === 403 ? "Dev tools are disabled for this backend." : `${label} failed.`);
    } finally {
      setBusy("");
    }
  }

  return (
    <section style={{ ...card, marginTop: 32, border: "1px solid rgba(251,191,36,0.24)" }}>
      <div style={{ display: "flex", justifyContent: "space-between", gap: 18, flexWrap: "wrap" }}>
        <div>
          <div style={{ color: "#fbbf24", fontSize: 12, fontWeight: 900, textTransform: "uppercase", letterSpacing: "0.08em" }}>
            Local development
          </div>
          <h2 style={{ ...sectionTitle, marginTop: 8 }}>Dev Tools</h2>
          <p style={{ ...mutedText, lineHeight: 1.6, maxWidth: 760 }}>
            Seed realistic demo data or mock billing state while Stripe webhook testing is paused.
          </p>
          {message && <p style={{ color: "#fde68a", marginTop: 10, fontWeight: 800 }}>{message}</p>}
        </div>
      </div>

      <div style={{ display: "flex", gap: 12, flexWrap: "wrap", marginTop: 18 }}>
        <DevButton label="Seed Demo Data" busy={busy} onClick={() => run("Seed Demo Data", seedDemoData)} />
        <DevButton label="Mock Upgrade to Pro" busy={busy} onClick={() => run("Mock Upgrade to Pro", mockUpgradePro)} />
        <DevButton label="Mock Downgrade to Free" busy={busy} onClick={() => run("Mock Downgrade to Free", mockDowngradeFree)} />
        <DevButton label="Backfill Feature Usage" busy={busy} onClick={() => run("Backfill Feature Usage", backfillFeatureUsage)} />
        <DevButton label="Clear Demo Data" busy={busy} danger onClick={() => run("Clear Demo Data", clearDemoData)} />
      </div>
    </section>
  );
}

function DevButton({
  label,
  busy,
  danger,
  onClick,
}: {
  label: string;
  busy: string;
  danger?: boolean;
  onClick: () => void;
}) {
  const disabled = Boolean(busy);
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      style={{
        ...button,
        width: 190,
        opacity: disabled ? 0.65 : 1,
        background: danger ? "linear-gradient(135deg, #991b1b, #ef4444)" : button.background,
      }}
    >
      {busy === label ? "Working..." : label}
    </button>
  );
}
