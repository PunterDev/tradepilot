import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import type { TradingAccount } from "../../api/accountApi";
import { getPlaybookOverview, getPlaybookSetups, type PlaybookOverview, type PlaybookSetupSummary } from "../../api/playbookApi";
import PageHeader from "../../layout/PageHeader";
import type { SelectedAccountId } from "../../types/dashboard";
import { badge, button, card, compactGrid, grid, label, muted, secondaryButton, shell, statValue } from "./playbookStyles";
import { formatPct, formatR } from "./playbookUtils";

type Props = {
  accounts: TradingAccount[];
  selectedAccountId: SelectedAccountId;
  onSelectAccount: (accountId: SelectedAccountId) => void;
};

export default function PlaybookPage({ accounts, selectedAccountId, onSelectAccount }: Props) {
  const navigate = useNavigate();
  const [overview, setOverview] = useState<PlaybookOverview | null>(null);
  const [setups, setSetups] = useState<PlaybookSetupSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const accountId = typeof selectedAccountId === "number" ? selectedAccountId : selectedAccountId === "ALL" ? undefined : Number(selectedAccountId) || undefined;

  useEffect(() => {
    setLoading(true);
    setError("");
    Promise.all([getPlaybookOverview(accountId), getPlaybookSetups({ accountId })])
      .then(([overviewData, setupData]) => {
        setOverview(overviewData);
        setSetups(setupData);
      })
      .catch((err) => setError(err?.response?.data?.message ?? "Unable to load Playbook."))
      .finally(() => setLoading(false));
  }, [accountId]);

  return (
    <>
      <PageHeader
        label="Playbook • Pro"
        title="AI Trading Playbook"
        description="Build, validate, and improve the setups that define your edge."
      />
      <section style={shell}>
        <div style={card}>
          <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap", alignItems: "end" }}>
            <div style={{ display: "grid", gap: 8, minWidth: 240 }}>
              <span style={label}>Account</span>
              <select
                value={accountId ?? "ALL"}
                onChange={(event) => onSelectAccount(event.target.value === "ALL" ? "ALL" : Number(event.target.value))}
                style={{ padding: "11px 12px", borderRadius: 12, background: "rgba(2,6,23,0.7)", color: "#e5e7eb", border: "1px solid rgba(148,163,184,0.24)" }}
              >
                <option value="ALL">All Accounts</option>
                {accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}
              </select>
            </div>
            <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
              <button type="button" style={secondaryButton} onClick={() => navigate("/playbook/new?mode=ai")}>Create with AI</button>
              <button type="button" style={button} onClick={() => navigate("/playbook/new")}>Create Setup</button>
            </div>
          </div>
        </div>

        {error && <div style={{ ...card, color: "#fecaca" }}>{error}</div>}
        {loading && <div style={card}>Loading Playbook...</div>}

        {overview && (
          <div style={compactGrid}>
            <Stat label="Active setups" value={overview.totalActiveSetups} />
            <Stat label="Testing" value={overview.testingSetups} />
            <Stat label="Validated" value={overview.validatedSetups} />
            <Stat label="Drafts" value={overview.draftSetups} />
          </div>
        )}

        {!loading && setups.length === 0 ? (
          <div style={{ ...card, display: "grid", gap: 12 }}>
            <span style={badge}>Playbook vs. Actual Behavior</span>
            <h2 style={{ margin: 0 }}>Create your first setup.</h2>
            <p style={muted}>Document the conditions that define a trade, then link historical trades to compare compliant and noncompliant outcomes.</p>
            <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
              <button type="button" style={button} onClick={() => navigate("/playbook/new")}>Manual Setup</button>
              <button type="button" style={secondaryButton} onClick={() => navigate("/playbook/new?mode=ai")}>Draft from Description</button>
            </div>
          </div>
        ) : (
          <div style={grid}>
            {setups.map((setup) => <SetupCard key={setup.id} setup={setup} onOpen={() => navigate(`/playbook/${setup.id}`)} />)}
          </div>
        )}
      </section>
    </>
  );
}

function Stat({ label: statLabel, value }: { label: string; value: number }) {
  return (
    <div style={card}>
      <span style={label}>{statLabel}</span>
      <strong style={statValue}>{value}</strong>
    </div>
  );
}

function SetupCard({ setup, onOpen }: { setup: PlaybookSetupSummary; onOpen: () => void }) {
  return (
    <article style={{ ...card, display: "grid", gap: 12 }}>
      <div style={{ display: "flex", justifyContent: "space-between", gap: 10 }}>
        <span style={badge}>{setup.status}</span>
        {setup.aiReviewStale && <span style={{ ...badge, color: "#fde68a", borderColor: "rgba(245,158,11,0.28)" }}>Stale AI</span>}
      </div>
      <div>
        <h3 style={{ margin: "0 0 6px", fontSize: 21 }}>{setup.name}</h3>
        <p style={muted}>{setup.shortDescription || "No short description yet."}</p>
      </div>
      <div style={compactGrid}>
        <Mini label="Trades" value={String(setup.linkedTradeCount)} />
        <Mini label="Total R" value={formatR(setup.totalR)} />
        <Mini label="Win rate" value={formatPct(setup.winRate)} />
        <Mini label="Health" value={setup.healthScore == null ? "—" : `${setup.healthScore}/100`} />
      </div>
      <button type="button" style={secondaryButton} onClick={onOpen}>Open setup</button>
    </article>
  );
}

function Mini({ label: miniLabel, value }: { label: string; value: string }) {
  return (
    <div style={{ padding: 10, borderRadius: 12, background: "rgba(2,6,23,0.5)", border: "1px solid rgba(148,163,184,0.12)" }}>
      <span style={label}>{miniLabel}</span>
      <strong style={{ display: "block", marginTop: 5 }}>{value}</strong>
    </div>
  );
}
