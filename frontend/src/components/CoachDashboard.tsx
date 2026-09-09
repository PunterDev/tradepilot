import { useEffect, useState } from "react";
import { useDailyCoach } from "../hooks/useDailyCoach";
import { useWeeklyCoach } from "../hooks/useWeeklyCoach";
import { DailyCoachCard } from "./DailyCoachCard";
import { WeeklyCoachCard } from "./WeeklyCoachCard";
import UpgradeModal from "./UpgradeModal";
import type { SelectedAccountId } from "../types/dashboard";
import type { BillingUsage } from "../types/billing";
import type { ActivationStatus } from "../types/activation";
import { creditsRemaining } from "../utils/featureAccess";

type Props = {
  selectedAccountId: SelectedAccountId;
  usage?: BillingUsage | null;
  activationStatus?: ActivationStatus | null;
  onUsageChanged?: () => void;
};

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

function startOfWeekIso(date = new Date()) {
  const d = new Date(date);
  const day = d.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  d.setDate(d.getDate() + diff);
  return d.toISOString().slice(0, 10);
}

export function CoachDashboard({ selectedAccountId, usage, activationStatus, onUsageChanged }: Props) {
  const [activeTab, setActiveTab] = useState<"daily" | "weekly">("daily");
  const [dailyDate, setDailyDate] = useState(todayIso());
  const [weeklyStartDate, setWeeklyStartDate] = useState(startOfWeekIso());
  const [upgradeOpen, setUpgradeOpen] = useState(false);

  const allAccountsSelected = selectedAccountId === "ALL";
  const accountId = allAccountsSelected ? undefined : Number(selectedAccountId);
  const normalCoachAccess = usage?.plan === "PRO";

  const daily = useDailyCoach(normalCoachAccess ? accountId : undefined, dailyDate);
  const weeklyPreviewAvailable = Boolean(activationStatus?.proPreview?.available);
  const weekly = useWeeklyCoach(accountId, weeklyStartDate, weeklyPreviewAvailable, normalCoachAccess);
  const dailyRemaining = usage ? creditsRemaining(usage) : 1;
  const weeklyRemaining = usage ? creditsRemaining(usage) : 1;

  useEffect(() => {
    if (!normalCoachAccess && weeklyPreviewAvailable) {
      setActiveTab("weekly");
    }
  }, [normalCoachAccess, weeklyPreviewAvailable]);

  if (allAccountsSelected) {
    return (
      <section
        style={{
          marginTop: 40,
          border: "1px solid rgba(148,163,184,0.18)",
          borderRadius: 30,
          padding: 30,
          background:
            "linear-gradient(135deg, rgba(15,23,42,0.96), rgba(2,6,23,0.98))",
        }}
      >
        <div style={{ textAlign: "center", padding: "60px 20px" }}>
          <div
            style={{
              color: "#38bdf8",
              fontSize: 13,
              fontWeight: 800,
              letterSpacing: 1.4,
              textTransform: "uppercase",
              marginBottom: 10,
            }}
          >
            TradePilot Mentor
          </div>

          <h2
            style={{
              color: "#f8fafc",
              fontSize: 34,
              fontWeight: 800,
              margin: 0,
            }}
          >
            AI Coach
          </h2>

          <p
            style={{
              color: "#94a3b8",
              fontSize: 16,
              maxWidth: 720,
              margin: "14px auto 0",
              lineHeight: 1.7,
            }}
          >
            AI Coach currently works with one trading account at a time.
            Select a specific account to analyze emotional patterns, setups,
            discipline, and behavioral performance.
          </p>
        </div>
      </section>
    );
  }

  return (
    <section
      style={{
        marginTop: 40,
        border: "1px solid rgba(148,163,184,0.18)",
        borderRadius: 30,
        padding: 30,
        background:
          "linear-gradient(135deg, rgba(15,23,42,0.96), rgba(2,6,23,0.98))",
        boxShadow: "0 24px 80px rgba(0,0,0,0.35)",
      }}
    >
      <UpgradeModal open={upgradeOpen} onClose={() => setUpgradeOpen(false)} plan={usage?.plan} />
      <div style={{ marginBottom: 28 }}>
        <div
          style={{
            color: "#38bdf8",
            fontSize: 13,
            fontWeight: 800,
            letterSpacing: 1.4,
            textTransform: "uppercase",
            marginBottom: 10,
          }}
        >
          TradePilot Mentor
        </div>

        <h2
          style={{
            color: "#f8fafc",
            fontSize: 34,
            fontWeight: 800,
            lineHeight: 1.1,
            margin: 0,
          }}
        >
          AI Daily / Weekly Coach
        </h2>

        <p
          style={{
            color: "#94a3b8",
            fontSize: 16,
            marginTop: 14,
            maxWidth: 950,
            lineHeight: 1.6,
          }}
        >
          Review your selected account’s behavior, repeated mistakes, best
          setups, emotional patterns, and next-session action plan.
        </p>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))",
          gap: 18,
          marginBottom: 24,
        }}
      >
        <div
          style={{
            border: "1px solid rgba(148,163,184,0.18)",
            borderRadius: 22,
            padding: 20,
            background: "rgba(15,23,42,0.72)",
          }}
        >
          <label
            style={{
              color: "#cbd5e1",
              fontWeight: 700,
              fontSize: 15,
              display: "block",
              marginBottom: 12,
            }}
          >
            Daily Review Date
          </label>

          <input
            type="date"
            value={dailyDate}
            onChange={(e) => setDailyDate(e.target.value)}
            style={{
              width: "100%",
              padding: "14px 16px",
              borderRadius: 16,
              border: "1px solid rgba(148,163,184,0.28)",
              background: "#020617",
              color: "#f8fafc",
              fontWeight: 700,
              fontSize: 16,
              outline: "none",
              cursor: "pointer",
              colorScheme: "dark",
            }}
          />
        </div>

        <div
          style={{
            border: "1px solid rgba(148,163,184,0.18)",
            borderRadius: 22,
            padding: 20,
            background: "rgba(15,23,42,0.72)",
          }}
        >
          <label
            style={{
              color: "#cbd5e1",
              fontWeight: 700,
              fontSize: 15,
              display: "block",
              marginBottom: 12,
            }}
          >
            Weekly Review Start Date
          </label>

          <input
            type="date"
            value={weeklyStartDate}
            onChange={(e) => setWeeklyStartDate(e.target.value)}
            style={{
              width: "100%",
              padding: "14px 16px",
              borderRadius: 16,
              border: "1px solid rgba(148,163,184,0.28)",
              background: "#020617",
              color: "#f8fafc",
              fontWeight: 700,
              fontSize: 16,
              outline: "none",
              cursor: "pointer",
              colorScheme: "dark",
            }}
          />
        </div>
      </div>

      <div
        style={{
          display: "flex",
          gap: 10,
          padding: 6,
          borderRadius: 18,
          background: "rgba(15,23,42,0.9)",
          marginBottom: 28,
        }}
      >
        <button
          type="button"
          onClick={() => {
            setActiveTab("daily");
            daily.reload();
          }}
          style={{
            flex: 1,
            border: "none",
            borderRadius: 14,
            padding: "14px 18px",
            cursor: "pointer",
            fontWeight: 800,
            fontSize: 16,
            color: activeTab === "daily" ? "#020617" : "#cbd5e1",
            background: activeTab === "daily" ? "#22c55e" : "transparent",
            boxShadow:
              activeTab === "daily"
                ? "0 10px 30px rgba(34,197,94,0.35)"
                : "none",
          }}
        >
          Daily Coach
        </button>

        <button
          type="button"
          onClick={() => {
            setActiveTab("weekly");
            weekly.reload();
          }}
          style={{
            flex: 1,
            border: "none",
            borderRadius: 14,
            padding: "14px 18px",
            cursor: "pointer",
            fontWeight: 800,
            fontSize: 16,
            color: activeTab === "weekly" ? "#020617" : "#cbd5e1",
            background: activeTab === "weekly" ? "#22c55e" : "transparent",
            boxShadow:
              activeTab === "weekly"
                ? "0 10px 30px rgba(34,197,94,0.35)"
                : "none",
          }}
        >
          Weekly Coach
        </button>
      </div>

      {activeTab === "daily" ? (
        <DailyCoachCard
          snapshot={daily.data}
          loading={daily.loading}
          generating={daily.generating}
          error={daily.error}
          onRegenerate={daily.regenerate}
          remaining={dailyRemaining}
          limitReached={daily.limitReached}
          limitMessage={daily.limitMessage}
          hasCachedSnapshot={daily.hasCachedSnapshot}
          onUsageChanged={onUsageChanged}
          onLimitClick={() => setUpgradeOpen(true)}
        />
      ) : (
          <WeeklyCoachCard
            snapshot={weekly.data}
            loading={weekly.loading}
            generating={weekly.generating}
            error={weekly.error}
            onRegenerate={weekly.regenerate}
            executionFocus={weekly.executionFocus}
            remaining={weeklyRemaining}
          limitReached={weekly.limitReached}
          limitMessage={weekly.limitMessage}
          hasCachedSnapshot={weekly.hasCachedSnapshot}
          onUsageChanged={onUsageChanged}
          onLimitClick={() => setUpgradeOpen(true)}
          previewAvailable={weeklyPreviewAvailable}
          previewGenerating={weekly.generating}
          onPreview={weekly.generatePreview}
        />
      )}
    </section>
  );
}
