import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { useTradingDna } from "../hooks/useTradingDna";
import { useState, type CSSProperties } from "react";
import UpgradeModal from "./UpgradeModal";
import type { TradingAccount } from "../api/accountApi";
import type { SelectedAccountId } from "../types/dashboard";
import type { BillingUsage } from "../types/billing";
import {
  button,
  card,
  chartGrid,
  mutedText,
  sectionTitle,
  tooltipStyle,
} from "../styles/dashboardStyles";

type Props = {
  selectedAccountId: SelectedAccountId;
  accounts: TradingAccount[];
  accountsLoading: boolean;
  onInvalidAccount?: () => void;
  usage?: BillingUsage | null;
  onUsageChanged?: () => void;
};

export default function TradingDnaPanel({
  selectedAccountId,
  accounts,
  accountsLoading,
  onInvalidAccount,
  usage,
  onUsageChanged,
}: Props) {
  const dna = useTradingDna({
    selectedAccountId,
    accounts,
    accountsLoading,
    days: 30,
    onInvalidAccount,
  });
  const [upgradeOpen, setUpgradeOpen] = useState(false);
  const data = dna.data;
  const remaining = usage?.remaining.AI_CREDITS ?? 1;
  const limitReached = remaining === 0;

  return (
    <section
      style={{
        ...card,
        marginTop: 36,
        border: "1px solid rgba(56,189,248,0.22)",
        background:
          "linear-gradient(135deg, rgba(8,47,73,0.34), rgba(15,23,42,0.94) 42%, rgba(2,6,23,0.96))",
      }}
    >
      <UpgradeModal open={upgradeOpen} onClose={() => setUpgradeOpen(false)} />
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          gap: 18,
          alignItems: "flex-start",
          flexWrap: "wrap",
        }}
      >
        <div>
          <div
            style={{
              color: "#38bdf8",
              fontSize: 13,
              fontWeight: 900,
              letterSpacing: "0.1em",
              textTransform: "uppercase",
            }}
          >
            Long-term behavior engine
          </div>
          <h2 style={{ ...sectionTitle, fontSize: 34, marginTop: 8 }}>
            Trading DNA
          </h2>
          <p style={{ ...mutedText, maxWidth: 780, lineHeight: 1.65 }}>
            Recurring mistakes, best conditions, emotional tendencies, risk behavior,
            and consistency trends over the last 30 days.
          </p>
          {data?.lastGeneratedAt && (
            <p style={{ ...mutedText, fontSize: 13 }}>
              Last generated: {new Date(data.lastGeneratedAt).toLocaleString()}
              {data.cached ? " · Cached" : " · Fresh"}
            </p>
          )}
        </div>

        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
          <button
            type="button"
            onClick={async () => {
              if (limitReached) {
                setUpgradeOpen(true);
                return;
              }
              await dna.generate();
              onUsageChanged?.();
            }}
            disabled={dna.loading || dna.refreshing || !dna.canLoad}
            style={{ ...button, width: 150 }}
          >
            {dna.loading ? "Loading..." : limitReached ? "Limit reached" : "Generate DNA"}
          </button>
          <button
            type="button"
            onClick={async () => {
              if (limitReached) {
                setUpgradeOpen(true);
                return;
              }
              await dna.refresh();
              onUsageChanged?.();
            }}
            disabled={dna.loading || dna.refreshing || !data}
            style={{
              ...button,
              width: 160,
              background: "rgba(15,23,42,0.9)",
              border: "1px solid rgba(148,163,184,0.32)",
              boxShadow: "none",
            }}
          >
            {dna.refreshing ? "Refreshing..." : "Refresh Analysis"}
          </button>
        </div>
      </div>

      {dna.loading && <TradingDnaSkeleton />}

      {!accountsLoading && !dna.canLoad && (
        <div
          style={{
            marginTop: 18,
            padding: 16,
            borderRadius: 16,
            border: "1px solid rgba(56,189,248,0.26)",
            background: "rgba(8,47,73,0.24)",
            color: "#bae6fd",
          }}
        >
          Select one of your current trading accounts, or All Accounts, to load Trading DNA.
        </div>
      )}

      {limitReached && (
        <div
          style={{
            marginTop: 18,
            padding: 14,
            borderRadius: 16,
            border: "1px solid rgba(250,204,21,0.32)",
            background: "rgba(113,63,18,0.22)",
            color: "#fde68a",
          }}
        >
          You have reached this month’s Trading DNA generation limit. Cached analysis remains viewable.
        </div>
      )}

      {dna.error && (
        <div
          style={{
            marginTop: 18,
            padding: 16,
            borderRadius: 16,
            background: "rgba(127,29,29,0.35)",
            border: "1px solid rgba(248,113,113,0.35)",
            color: "#fecaca",
          }}
        >
          {dna.error}
        </div>
      )}

      {data?.limitReached && (
        <div
          style={{
            marginTop: 18,
            padding: 14,
            borderRadius: 16,
            border: "1px solid rgba(250,204,21,0.32)",
            background: "rgba(113,63,18,0.22)",
            color: "#fde68a",
          }}
        >
          {data.message ?? "Trading DNA generation limit reached. Cached analysis remains viewable when available."}
        </div>
      )}

      {data && (data.status === "INSUFFICIENT_DATA" || data.status === "LIMIT_REACHED") && (
        <div
          style={{
            marginTop: 22,
            padding: 22,
            borderRadius: 20,
            border: "1px solid rgba(250,204,21,0.28)",
            background: "rgba(113,63,18,0.22)",
          }}
        >
          <h3 style={{ color: "#f8fafc", margin: "0 0 8px" }}>
            Not enough trades yet
          </h3>
          <p style={{ ...mutedText, margin: 0 }}>
            {data.message ??
              `Import at least 5 trades for this scope to generate reliable Trading DNA. Current sample: ${data.totalTrades} trades.`}
          </p>
        </div>
      )}

      {data && data.status !== "INSUFFICIENT_DATA" && data.status !== "LIMIT_REACHED" && (
        <>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(4, minmax(0, 1fr))",
              gap: 16,
              marginTop: 26,
            }}
          >
            <DnaScore title="Discipline" value={data.disciplineScore} />
            <DnaScore title="Patience" value={data.patienceScore} />
            <DnaScore title="Execution" value={data.executionScore} />
            <DnaScore title="Consistency" value={data.consistencyScore} />
          </div>

          <div style={chartGrid}>
            <DnaList title="Strengths" items={data.strengths} accent="#34d399" />
            <DnaList title="Weaknesses" items={data.weaknesses} accent="#fb7185" />
            <DnaList title="Best Conditions" items={data.bestConditions} accent="#38bdf8" />
            <DnaList title="Worst Conditions" items={data.worstConditions} accent="#f59e0b" />
          </div>

          <div style={{ ...card, marginTop: 24, boxShadow: "none" }}>
            <h3 style={{ ...sectionTitle, fontSize: 22 }}>Performance Timeline</h3>
            {data.timeline.length === 0 ? (
              <div style={timelineEmptyState}>No timeline data available yet.</div>
            ) : (
              <div style={{ width: "100%", minWidth: 0, height: 320 }}>
                <ResponsiveContainer width="100%" height={320}>
                  <LineChart data={data.timeline}>
                    <CartesianGrid stroke="rgba(148,163,184,0.16)" strokeDasharray="4 4" />
                    <XAxis dataKey="date" stroke="#64748b" tick={{ fill: "#94a3b8", fontSize: 12 }} />
                    <YAxis domain={[0, 100]} stroke="#64748b" tick={{ fill: "#94a3b8", fontSize: 12 }} />
                    <Tooltip contentStyle={tooltipStyle} />
                    <Line type="monotone" dataKey="consistencyScore" name="Consistency" stroke="#38bdf8" strokeWidth={3} dot={false} />
                    <Line type="monotone" dataKey="winRate" name="Win Rate" stroke="#34d399" strokeWidth={3} dot={false} />
                    <Line type="monotone" dataKey="emotionalStabilityScore" name="Emotional Stability" stroke="#a78bfa" strokeWidth={3} dot={false} />
                    <Line type="monotone" dataKey="disciplineScore" name="Discipline" stroke="#f59e0b" strokeWidth={3} dot={false} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            )}
          </div>

          <div
            style={{
              marginTop: 24,
              padding: 24,
              borderRadius: 22,
              border: "1px solid rgba(52,211,153,0.22)",
              background: "rgba(2,6,23,0.52)",
            }}
          >
            <h3 style={{ color: "#f8fafc", margin: "0 0 10px" }}>
              AI Coaching Summary
            </h3>
            <p style={{ color: "#cbd5e1", lineHeight: 1.7, marginTop: 0 }}>
              {data.coachingSummary}
            </p>
            <p style={{ color: "#94a3b8", lineHeight: 1.7, marginBottom: 0 }}>
              {data.confidenceSummary}
            </p>
          </div>
        </>
      )}
    </section>
  );
}

function DnaScore({ title, value }: { title: string; value: number }) {
  return (
    <div
      style={{
        padding: 18,
        borderRadius: 18,
        border: "1px solid rgba(148,163,184,0.18)",
        background: "rgba(15,23,42,0.72)",
      }}
    >
      <div style={{ color: "#94a3b8", fontSize: 12, fontWeight: 900, textTransform: "uppercase" }}>
        {title}
      </div>
      <div style={{ marginTop: 8, fontSize: 32, fontWeight: 950, color: scoreColor(value) }}>
        {value}
      </div>
    </div>
  );
}

function DnaList({
  title,
  items,
  accent,
}: {
  title: string;
  items: string[];
  accent: string;
}) {
  return (
    <div style={{ ...card, boxShadow: "none" }}>
      <h3 style={{ color: "#f8fafc", marginTop: 0 }}>{title}</h3>
      {items.length === 0 ? (
        <p style={mutedText}>No clear pattern detected yet.</p>
      ) : (
        <div style={{ display: "grid", gap: 10 }}>
          {items.slice(0, 6).map((item) => (
            <div
              key={item}
              style={{
                padding: "12px 14px",
                borderRadius: 14,
                border: "1px solid rgba(148,163,184,0.14)",
                background: "rgba(2,6,23,0.45)",
                color: "#cbd5e1",
                borderLeft: `3px solid ${accent}`,
                lineHeight: 1.5,
              }}
            >
              {item}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function TradingDnaSkeleton() {
  return (
    <div style={{ display: "grid", gap: 14, marginTop: 24 }}>
      {[0, 1, 2].map((item) => (
        <div
          key={item}
          style={{
            height: item === 0 ? 86 : 150,
            borderRadius: 18,
            background:
              "linear-gradient(90deg, rgba(30,41,59,0.72), rgba(51,65,85,0.52), rgba(30,41,59,0.72))",
          }}
        />
      ))}
    </div>
  );
}

const timelineEmptyState: CSSProperties = {
  width: "100%",
  minWidth: 0,
  height: 320,
  display: "grid",
  placeItems: "center",
  borderRadius: 18,
  border: "1px dashed rgba(148,163,184,0.18)",
  background: "rgba(2,6,23,0.28)",
  color: "#94a3b8",
};

function scoreColor(value: number) {
  if (value >= 75) return "#34d399";
  if (value >= 55) return "#facc15";
  return "#fb7185";
}
