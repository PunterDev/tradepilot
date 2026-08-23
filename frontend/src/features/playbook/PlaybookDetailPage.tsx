import type { CSSProperties } from "react";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  archivePlaybookSetup,
  duplicatePlaybookSetup,
  getPlaybookSetup,
  regeneratePlaybookAnalysis,
  restorePlaybookSetup,
  type ChecklistType,
  type PlaybookChecklistItem,
  type PlaybookDetailState,
  type PlaybookEligibility,
  type PlaybookRequirement,
  type PlaybookSetupDetail,
} from "../../api/playbookApi";
import PageHeader from "../../layout/PageHeader";
import { badge, button, card, compactGrid, dangerButton, grid, label, muted, secondaryButton, shell, statValue } from "./playbookStyles";
import { formatMoney, formatPct, formatR } from "./playbookUtils";

type Props = {
  onUsageChanged: () => void;
};

type DefinitionSection = {
  key: PlaybookRequirement;
  title: string;
  type: ChecklistType;
  complete: boolean;
  action: string;
  items: string[];
};

const disabledButton: CSSProperties = {
  ...secondaryButton,
  opacity: 0.72,
  cursor: "not-allowed",
};

const emptyStateCard: CSSProperties = {
  padding: 16,
  borderRadius: 16,
  border: "1px solid rgba(45,212,191,0.18)",
  background: "linear-gradient(135deg, rgba(20,184,166,0.09), rgba(15,23,42,0.72))",
};

export default function PlaybookDetailPage({ onUsageChanged }: Props) {
  const { setupId } = useParams();
  const navigate = useNavigate();
  const id = Number(setupId);
  const [setup, setSetup] = useState<PlaybookSetupDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  function reload() {
    setLoading(true);
    setError("");
    getPlaybookSetup(id)
      .then(setSetup)
      .catch((err) => setError(err?.response?.data?.message ?? err?.response?.data?.userMessage ?? "Unable to load setup."))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    if (Number.isFinite(id)) reload();
  }, [id]);

  async function run(action: () => Promise<any>, after?: (value: any) => void) {
    setSaving(true);
    setError("");
    try {
      const value = await action();
      after?.(value);
    } catch (err: any) {
      setError(err?.response?.data?.message ?? err?.response?.data?.userMessage ?? "Action failed.");
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <section style={shell}><div style={card}>Loading setup...</div></section>;
  if (!setup) return <section style={shell}><div style={{ ...card, color: "#fecaca" }}>{error || "Setup not found."}</div></section>;

  const analysis = setup.analysis;
  const metrics = analysis.metrics;
  const health = analysis.healthScore;
  const eligibility = analysis.eligibility;
  const detailState = eligibility.detailState;
  const hasLinkedTrades = eligibility.linkedTradeCount > 0;
  const hasBehaviorComparison = eligibility.behaviorComparisonEligible;
  const canGenerateAi = eligibility.aiReviewEligible;
  const incompleteOrNoTrades = detailState === "INCOMPLETE_SETUP" || detailState === "NO_LINKED_TRADES";
  const definitionSections = buildDefinitionSections(setup, eligibility);
  const primary = primaryCta(setup.id, eligibility, Boolean(analysis.aiReview), navigate, () => run(() => regeneratePlaybookAnalysis(setup.id), () => { onUsageChanged(); reload(); }), saving);

  return (
    <>
      <PageHeader
        label="Playbook • Pro"
        title={setup.name}
        description={setup.shortDescription || "Compare your documented playbook with linked historical trades."}
      />
      <section style={shell}>
        {error && <div style={{ ...card, color: "#fecaca" }}>{error}</div>}
        <HeaderActions
          setup={setup}
          detailState={detailState}
          primary={primary}
          saving={saving}
          onEdit={() => navigate(`/playbook/${setup.id}/edit`)}
          onDuplicate={() => run(() => duplicatePlaybookSetup(setup.id), (copy) => navigate(`/playbook/${copy.id}`))}
          onArchive={() => run(() => archivePlaybookSetup(setup.id), setSetup)}
          onRestore={() => run(() => restorePlaybookSetup(setup.id), setSetup)}
        />

        {isPlaceholderName(setup.name) && (
          <section style={emptyStateCard}>
            <span style={label}>Setup name</span>
            <h2 style={{ margin: "6px 0" }}>Give this setup a descriptive name</h2>
            <p style={muted}>Examples: NY AM FVG Continuation, Liquidity Sweep Reversal, Opening Range Breakout.</p>
          </section>
        )}

        {setup.status === "DRAFT" && (
          <section style={emptyStateCard}>
            <span style={label}>Draft setup</span>
            <p style={{ margin: "6px 0 0", color: "#cbd5e1" }}>Draft setups are still being defined and are not ready for performance validation.</p>
          </section>
        )}

        {detailState === "INCOMPLETE_SETUP" && <CompletionChecklist eligibility={eligibility} sections={definitionSections} />}

        {hasLinkedTrades ? (
          <div style={compactGrid}>
            <Stat label="Linked trades" value={String(metrics.totalLinkedTrades)} />
            <Stat label="Reviewed trades" value={`${eligibility.reviewedTradeCount}/${eligibility.aiReviewMinimumTrades}`} />
            <Stat label="Total R" value={formatR(metrics.totalR)} />
            <Stat label="Net P&L" value={formatMoney(metrics.netPnl)} />
            <Stat label="Win rate" value={formatPct(metrics.winRate)} />
            <Stat label="Profit factor" value={metrics.profitFactor == null ? "Not available yet" : Number(metrics.profitFactor).toFixed(2)} />
            <Stat label="Avg R" value={metrics.averageR == null ? "Not available yet" : formatR(metrics.averageR)} />
            <Stat label="Compliance" value={setup.complianceScore == null ? "Not available yet" : `${setup.complianceScore}/100`} />
          </div>
        ) : (
          <section style={emptyStateCard}>
            <span style={label}>Historical performance summary</span>
            <h2 style={{ margin: "6px 0" }}>No linked trades yet</h2>
            <p style={muted}>Link historical trades to validate this setup and measure how consistently it is executed.</p>
            <button type="button" style={{ ...button, marginTop: 14 }} onClick={() => navigate("/trades")}>Link Your First Trade</button>
          </section>
        )}

        <section style={grid}>
          <HealthPanel health={health} eligibility={eligibility} limited={incompleteOrNoTrades} />
          <DefinitionPanel setup={setup} sections={definitionSections} />
        </section>

        <ChecklistRules items={setup.checklistItems} comparisons={hasBehaviorComparison ? analysis.behaviorComparisons : []} zeroTrades={!hasLinkedTrades} onEdit={() => navigate(`/playbook/${setup.id}/edit`)} />

        <BehaviorComparison setupId={setup.id} eligibility={eligibility} comparisons={analysis.behaviorComparisons} visible={hasBehaviorComparison} onReviewRules={() => navigate(`/playbook/${setup.id}/edit`)} />

        <AiReviewPanel
          analysis={analysis}
          eligibility={eligibility}
          canGenerate={canGenerateAi}
          saving={saving}
          onGenerate={() => run(() => regeneratePlaybookAnalysis(setup.id), () => { onUsageChanged(); reload(); })}
        />
      </section>
    </>
  );
}

function HeaderActions({ setup, detailState, primary, saving, onEdit, onDuplicate, onArchive, onRestore }: {
  setup: PlaybookSetupDetail;
  detailState: PlaybookDetailState;
  primary: { label: string; disabled?: boolean; title?: string; onClick: () => void; };
  saving: boolean;
  onEdit: () => void;
  onDuplicate: () => void;
  onArchive: () => void;
  onRestore: () => void;
}) {
  return (
    <div style={card}>
      <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <span style={badge}>{setup.status}</span>
          <span style={badge}>{setup.category}</span>
          <span style={badge}>{setup.direction}</span>
          <span style={{ ...badge, borderColor: "rgba(148,163,184,0.2)", color: "#cbd5e1", background: "rgba(15,23,42,0.6)" }}>{labelForState(detailState)}</span>
          {setup.analysis.aiReviewStale && <span style={{ ...badge, color: "#fde68a", borderColor: "rgba(245,158,11,0.28)" }}>AI review stale</span>}
        </div>
        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
          <button type="button" style={primary.disabled ? disabledButton : button} disabled={saving || primary.disabled} title={primary.title} onClick={primary.onClick}>{saving ? "Working..." : primary.label}</button>
          <button type="button" style={secondaryButton} onClick={onEdit}>Edit</button>
          <button type="button" style={secondaryButton} disabled={saving} onClick={onDuplicate}>Duplicate</button>
          {setup.status === "ARCHIVED"
            ? <button type="button" style={secondaryButton} disabled={saving} onClick={onRestore}>Restore</button>
            : <button type="button" style={dangerButton} disabled={saving} onClick={onArchive}>Archive</button>}
        </div>
      </div>
    </div>
  );
}

function CompletionChecklist({ eligibility, sections }: { eligibility: PlaybookEligibility; sections: DefinitionSection[] }) {
  const steps = [
    ...sections.map((section) => ({
      key: section.key,
      label: section.title,
      status: section.complete ? "Complete" : "Incomplete",
      action: section.action,
      locked: false,
      optional: false,
    })),
    {
      key: "FIRST_LINKED_TRADE_REQUIRED" as PlaybookRequirement,
      label: "Link first trade",
      status: eligibility.linkedTradeCount > 0 ? "Complete" : "Locked by prerequisite",
      action: "Link first trade",
      locked: !eligibility.definitionComplete,
      optional: false,
    },
    {
      key: "FIVE_REVIEWED_TRADES_REQUIRED" as PlaybookRequirement,
      label: "Review checklist compliance",
      status: eligibility.reviewedTradeCount >= eligibility.behaviorComparisonMinimumTrades ? "Complete" : "Locked by prerequisite",
      action: "Review linked trades",
      locked: eligibility.linkedTradeCount === 0,
      optional: false,
    },
    {
      key: "TEN_REVIEWED_TRADES_REQUIRED" as PlaybookRequirement,
      label: "Link 10 reviewed trades to unlock AI Review",
      status: eligibility.aiReviewEligible ? "Complete" : "Locked by prerequisite",
      action: "Review more trades",
      locked: eligibility.reviewedTradeCount < eligibility.behaviorComparisonMinimumTrades,
      optional: false,
    },
  ];
  const completed = steps.filter((step) => step.status === "Complete").length;

  return (
    <section style={card}>
      <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
        <div>
          <span style={label}>Complete your playbook</span>
          <h2 style={{ margin: "6px 0" }}>Document the setup before validating it against historical trades.</h2>
          <p style={muted}>{completed} of {steps.length} steps completed</p>
        </div>
        <span style={badge}>{eligibility.completedDefinitionSections} of {eligibility.totalDefinitionSections} definition sections</span>
      </div>
      <div style={{ ...grid, marginTop: 16 }}>
        {steps.map((step) => (
          <div key={step.key} style={{ padding: 14, borderRadius: 16, background: "rgba(2,6,23,0.48)", border: "1px solid rgba(148,163,184,0.12)" }}>
            <span style={{ ...badge, color: step.status === "Complete" ? "#86efac" : step.locked ? "#cbd5e1" : "#fde68a" }}>{step.status}</span>
            <strong style={{ display: "block", marginTop: 10 }}>{step.label}</strong>
            <p style={{ ...muted, marginTop: 6 }}>{step.locked ? "Complete earlier steps to unlock this validation step." : step.action}</p>
          </div>
        ))}
      </div>
    </section>
  );
}

function HealthPanel({ health, eligibility, limited }: { health: PlaybookSetupDetail["analysis"]["healthScore"]; eligibility: PlaybookEligibility; limited: boolean }) {
  return (
    <div style={card}>
      <span style={label}>{limited ? "Playbook incomplete" : "Playbook Health"}</span>
      <strong style={statValue}>Health score: {health.score}/100</strong>
      <p style={muted}>
        {limited
          ? "This score currently reflects definition completeness only. Historical evidence, rule adherence, and recent relevance are not available yet."
          : `${health.confidence} confidence. Profitability alone cannot produce a high health score.`}
      </p>
      <div style={{ display: "grid", gap: 10, marginTop: 12 }}>
        <Health label="Definition" value={health.definitionCompleteness} />
        <Health label="Historical evidence" value={health.historicalEvidence} unavailable={eligibility.linkedTradeCount === 0 ? "Not available - no linked trades" : undefined} />
        <Health label="Rule adherence" value={health.ruleAdherence} unavailable={eligibility.reviewedTradeCount === 0 ? "Not available - no reviewed checklists" : undefined} />
        <Health label="Performance consistency" value={health.performanceConsistency} unavailable={eligibility.linkedTradeCount < 5 ? "Not available - insufficient trade history" : undefined} />
        <Health label="Recent relevance" value={health.recentRelevance} unavailable={eligibility.linkedTradeCount === 0 ? "Not available - no recent linked trades" : undefined} />
      </div>
    </div>
  );
}

function DefinitionPanel({ setup, sections }: { setup: PlaybookSetupDetail; sections: DefinitionSection[] }) {
  return (
    <div style={card}>
      <span style={label}>Setup Definition</span>
      <strong style={{ display: "block", marginTop: 6 }}>{sections.filter((section) => section.complete).length} of {sections.length} sections complete</strong>
      {sections.map((section) => (
        <List key={section.key} title={section.title} items={section.items} fallback={section.action} />
      ))}
      {setup.description && (
        <div style={{ marginTop: 12 }}>
          <strong>Description</strong>
          <p style={{ color: "#cbd5e1", lineHeight: 1.6 }}>{setup.description}</p>
        </div>
      )}
    </div>
  );
}

function ChecklistRules({ items, comparisons, zeroTrades, onEdit }: {
  items: PlaybookChecklistItem[];
  comparisons: PlaybookSetupDetail["analysis"]["behaviorComparisons"];
  zeroTrades: boolean;
  onEdit: () => void;
}) {
  const comparisonById = new Map(comparisons.map((comparison) => [comparison.checklistItemId, comparison]));
  const activeItems = items.filter((item) => item.active !== false);
  return (
    <section style={card}>
      <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
        <div>
          <span style={label}>Checklist rules</span>
          <h2 style={{ margin: "6px 0" }}>Rules future compliance analysis will evaluate</h2>
        </div>
        <button type="button" style={secondaryButton} onClick={onEdit}>Add checklist rules</button>
      </div>
      {activeItems.length === 0 ? (
        <div style={{ ...emptyStateCard, marginTop: 14 }}>
          <strong>No checklist rules documented yet</strong>
          <p style={{ ...muted, marginTop: 6 }}>Add rules before validating this setup against real execution.</p>
        </div>
      ) : (
        <div style={{ ...grid, marginTop: 14 }}>
          {activeItems.map((item) => {
            const comparison = item.id ? comparisonById.get(item.id) : undefined;
            const total = comparison ? comparison.metTradeCount + comparison.notMetTradeCount : 0;
            const rate = comparison && total > 0 ? Math.round((comparison.metTradeCount / total) * 100) : null;
            return (
              <div key={item.id ?? `${item.checklistType}-${item.label}`} style={{ padding: 14, borderRadius: 16, background: "rgba(2,6,23,0.48)", border: "1px solid rgba(148,163,184,0.12)" }}>
                <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                  <span style={badge}>{friendlyType(item.checklistType)}</span>
                  <span style={{ ...badge, color: item.required ? "#fde68a" : "#cbd5e1", borderColor: "rgba(148,163,184,0.2)" }}>{item.required ? "Required" : "Optional"}</span>
                  {item.weight > 1 && <span style={{ ...badge, color: "#cbd5e1", borderColor: "rgba(148,163,184,0.2)" }}>Weight {item.weight}</span>}
                </div>
                <strong style={{ display: "block", marginTop: 10 }}>{item.label}</strong>
                {item.description && <p style={{ ...muted, marginTop: 6 }}>{item.description}</p>}
                {!zeroTrades && rate != null && <p style={{ color: "#cbd5e1", margin: "8px 0 0" }}>{rate}% completion rate across reviewed trades</p>}
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}

function BehaviorComparison({ setupId, eligibility, comparisons, visible, onReviewRules }: {
  setupId: number;
  eligibility: PlaybookEligibility;
  comparisons: PlaybookSetupDetail["analysis"]["behaviorComparisons"];
  visible: boolean;
  onReviewRules: () => void;
}) {
  if (!visible) {
    return (
      <section style={emptyStateCard}>
        <span style={label}>Playbook vs. Actual Behavior</span>
        <h2 style={{ margin: "6px 0" }}>Link and review at least {eligibility.behaviorComparisonMinimumTrades} trades</h2>
        <p style={muted}>Compare documented rules with actual execution after enough reviewed trades exist.</p>
        <p style={{ color: "#cbd5e1", margin: "10px 0 0" }}>{eligibility.reviewedTradeCount} of {eligibility.behaviorComparisonMinimumTrades} reviewed trades</p>
        <button type="button" style={{ ...secondaryButton, marginTop: 14 }} onClick={onReviewRules}>Review Rule Compliance</button>
      </section>
    );
  }

  return (
    <section style={card}>
      <span style={label}>Playbook vs. Actual Behavior</span>
      <div style={{ display: "grid", gap: 10, marginTop: 12 }}>
        {comparisons.length === 0 ? (
          <p style={muted}>Add required checklist rules and link reviewed trades to compare documented rules against actual behavior.</p>
        ) : comparisons.map((comparison) => (
          <div key={`${setupId}-${comparison.checklistItemId}`} style={{ padding: 12, borderRadius: 14, background: "rgba(2,6,23,0.5)", border: "1px solid rgba(148,163,184,0.12)" }}>
            <strong>{comparison.rule}</strong>
            <p style={muted}>{comparison.interpretation}</p>
            <div style={compactGrid}>
              <Stat label="Met trades" value={String(comparison.metTradeCount)} />
              <Stat label="Not met" value={String(comparison.notMetTradeCount)} />
              <Stat label="Met avg R" value={comparison.metAverageR == null ? "Not available yet" : formatR(comparison.metAverageR)} />
              <Stat label="Not-met avg R" value={comparison.notMetAverageR == null ? "Not available yet" : formatR(comparison.notMetAverageR)} />
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}

function AiReviewPanel({ analysis, eligibility, canGenerate, saving, onGenerate }: {
  analysis: PlaybookSetupDetail["analysis"];
  eligibility: PlaybookEligibility;
  canGenerate: boolean;
  saving: boolean;
  onGenerate: () => void;
}) {
  return (
    <section style={{ ...card, display: "grid", gap: 12 }}>
      <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
        <div>
          <span style={label}>AI Coaching Review</span>
          {!canGenerate && (
            <p style={{ ...muted, marginTop: 6 }}>Link and review at least {eligibility.aiReviewMinimumTrades} trades before generating an AI performance review.</p>
          )}
        </div>
        <button
          type="button"
          style={canGenerate ? button : disabledButton}
          disabled={!canGenerate || saving}
          aria-describedby={!canGenerate ? "playbook-ai-review-disabled-reason" : undefined}
          title={!canGenerate ? `AI Review requires ${eligibility.aiReviewMinimumTrades} reviewed trades` : "Generate AI Review · 3 credits"}
          onClick={onGenerate}
        >
          {canGenerate ? `${analysis.aiReview ? "Regenerate" : "Generate"} AI Review · 3 credits` : `AI Review requires ${eligibility.aiReviewMinimumTrades} reviewed trades`}
        </button>
      </div>
      {!canGenerate && <p id="playbook-ai-review-disabled-reason" style={muted}>Current progress: {eligibility.reviewedTradeCount} of {eligibility.aiReviewMinimumTrades} reviewed trades. No credits are consumed until this action is eligible.</p>}
      {analysis.aiReview && (
        <>
          <h2 style={{ margin: 0 }}>{analysis.aiReview.headline}</h2>
          <p style={muted}>{analysis.aiReview.summary}</p>
          <div style={grid}>
            <Insight label="Strongest behavior" value={analysis.aiReview.strongestBehavior} />
            <Insight label="Setup weakness" value={analysis.aiReview.biggestSetupWeakness} />
            <Insight label="Execution weakness" value={analysis.aiReview.biggestExecutionWeakness} />
            <Insight label="Next focus" value={analysis.aiReview.nextFocus} />
          </div>
        </>
      )}
    </section>
  );
}

function Stat({ label: statLabel, value }: { label: string; value: string }) {
  return (
    <div style={{ padding: 12, borderRadius: 14, background: "rgba(2,6,23,0.48)", border: "1px solid rgba(148,163,184,0.12)" }}>
      <span style={label}>{statLabel}</span>
      <strong style={{ display: "block", marginTop: 6 }}>{value}</strong>
    </div>
  );
}

function Health({ label: healthLabel, value, unavailable }: { label: string; value: number; unavailable?: string }) {
  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", gap: 10, color: "#cbd5e1", fontSize: 13 }}>
        <span>{healthLabel}</span>
        <strong>{unavailable ?? `${value}/100`}</strong>
      </div>
      {!unavailable && (
        <div style={{ height: 7, borderRadius: 999, background: "rgba(51,65,85,0.7)", overflow: "hidden", marginTop: 5 }}>
          <div style={{ width: `${Math.max(0, Math.min(100, value))}%`, height: "100%", background: "linear-gradient(90deg, #22c55e, #14b8a6)" }} />
        </div>
      )}
    </div>
  );
}

function List({ title, items, fallback }: { title: string; items: string[]; fallback: string }) {
  return (
    <div style={{ marginTop: 12 }}>
      <strong>{title}</strong>
      {items?.length ? <ul style={{ color: "#cbd5e1", lineHeight: 1.6 }}>{items.map((item) => <li key={item}>{item}</li>)}</ul> : <p style={muted}>{fallback}</p>}
    </div>
  );
}

function Insight({ label: insightLabel, value }: { label: string; value: string }) {
  return (
    <div style={{ padding: 13, borderRadius: 14, background: "rgba(2,6,23,0.48)", border: "1px solid rgba(148,163,184,0.12)" }}>
      <span style={label}>{insightLabel}</span>
      <p style={{ margin: "7px 0 0", color: "#cbd5e1" }}>{value}</p>
    </div>
  );
}

function buildDefinitionSections(setup: PlaybookSetupDetail, eligibility: PlaybookEligibility): DefinitionSection[] {
  const itemsByType = (type: ChecklistType) => setup.checklistItems
    .filter((item) => item.checklistType === type && item.active !== false && item.label?.trim())
    .map((item) => item.label);
  const missing = new Set(eligibility.missingRequirements);
  return [
    { key: "MARKET_CONTEXT_REQUIRED", title: "Market Context", type: "CONTEXT", complete: !missing.has("MARKET_CONTEXT_REQUIRED"), action: "Add market context", items: setup.marketContext },
    { key: "ENTRY_RULE_REQUIRED", title: "Entry Rules", type: "ENTRY", complete: !missing.has("ENTRY_RULE_REQUIRED"), action: "Add entry rules", items: itemsByType("ENTRY") },
    { key: "INVALIDATION_RULE_REQUIRED", title: "Invalidation Rules", type: "INVALIDATION", complete: !missing.has("INVALIDATION_RULE_REQUIRED"), action: "Add invalidation rules", items: itemsByType("INVALIDATION") },
    { key: "RISK_RULE_REQUIRED", title: "Risk Rules", type: "RISK", complete: !missing.has("RISK_RULE_REQUIRED"), action: "Add risk rules", items: setup.riskRules.length ? setup.riskRules : itemsByType("RISK") },
    { key: "MANAGEMENT_RULE_REQUIRED", title: "Management Rules", type: "MANAGEMENT", complete: !missing.has("MANAGEMENT_RULE_REQUIRED"), action: "Add management rules", items: setup.managementRules.length ? setup.managementRules : itemsByType("MANAGEMENT") },
    { key: "POST_TRADE_RULE_REQUIRED", title: "Post-Trade Rules", type: "POST_TRADE", complete: !missing.has("POST_TRADE_RULE_REQUIRED"), action: "Add post-trade rules", items: setup.postTradeRules.length ? setup.postTradeRules : itemsByType("POST_TRADE") },
  ];
}

function primaryCta(setupId: number, eligibility: PlaybookEligibility, hasReview: boolean, navigate: ReturnType<typeof useNavigate>, generate: () => void, saving: boolean) {
  if (eligibility.detailState === "INCOMPLETE_SETUP") {
    return { label: "Complete Setup", onClick: () => navigate(`/playbook/${setupId}/edit`) };
  }
  if (eligibility.detailState === "NO_LINKED_TRADES") {
    return { label: "Link Your First Trade", onClick: () => navigate("/trades") };
  }
  if (eligibility.detailState === "EARLY_EVIDENCE") {
    return { label: "Review Linked Trades", onClick: () => navigate(`/playbook/${setupId}/edit`) };
  }
  if (eligibility.detailState === "COMPARISON_ELIGIBLE") {
    return { label: "Review Rule Compliance", onClick: () => navigate(`/playbook/${setupId}/edit`) };
  }
  return {
    label: `${hasReview ? "Regenerate" : "Generate"} AI Review · 3 credits`,
    disabled: saving,
    title: "Generate AI review · 3 credits",
    onClick: generate,
  };
}

function labelForState(state: PlaybookDetailState) {
  const labels: Record<PlaybookDetailState, string> = {
    INCOMPLETE_SETUP: "Incomplete setup",
    NO_LINKED_TRADES: "No linked trades",
    EARLY_EVIDENCE: "Early evidence",
    COMPARISON_ELIGIBLE: "Comparison eligible",
    AI_REVIEW_ELIGIBLE: "AI review eligible",
    MATURE_SETUP: "Mature setup",
  };
  return labels[state];
}

function friendlyType(type: ChecklistType) {
  return type.replace("_", " ").toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function isPlaceholderName(name: string) {
  const normalized = name.trim().toLowerCase();
  return normalized === "playbook" || normalized === "untitled" || normalized === "new setup";
}
