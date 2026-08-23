import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import type { TradingAccount } from "../../api/accountApi";
import {
  createPlaybookDraftFromDescription,
  createPlaybookSetup,
  getPlaybookSetup,
  updatePlaybookSetup,
  type ChecklistType,
  type PlaybookSetupPayload,
} from "../../api/playbookApi";
import PageHeader from "../../layout/PageHeader";
import type { SelectedAccountId } from "../../types/dashboard";
import { button, card, grid, input, label, muted, secondaryButton, shell, textarea } from "./playbookStyles";
import { checklistTypes, emptySetup, joinCsv, splitCsv } from "./playbookUtils";

type Props = {
  accounts: TradingAccount[];
  selectedAccountId: SelectedAccountId;
  onUsageChanged: () => void;
};

export default function PlaybookFormPage({ accounts, selectedAccountId, onUsageChanged }: Props) {
  const { setupId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const editId = setupId ? Number(setupId) : null;
  const initialAccountId = typeof selectedAccountId === "number" ? selectedAccountId : undefined;
  const [form, setForm] = useState<PlaybookSetupPayload>(() => emptySetup(initialAccountId));
  const [aiDescription, setAiDescription] = useState("");
  const [loading, setLoading] = useState(Boolean(editId));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const aiMode = new URLSearchParams(location.search).get("mode") === "ai";

  useEffect(() => {
    if (!editId) return;
    setLoading(true);
    getPlaybookSetup(editId)
      .then((setup) => setForm({
        accountId: setup.accountId ?? null,
        name: setup.name,
        shortDescription: setup.shortDescription ?? "",
        description: setup.description ?? "",
        category: setup.category,
        status: setup.status,
        direction: setup.direction,
        instruments: setup.instruments ?? [],
        sessions: setup.sessions ?? [],
        timeframes: setup.timeframes ?? [],
        tags: setup.tags ?? [],
        marketContext: setup.marketContext ?? [],
        riskRules: setup.riskRules ?? [],
        managementRules: setup.managementRules ?? [],
        postTradeRules: setup.postTradeRules ?? [],
        iconKey: "book",
        colorKey: "teal",
        imageUrl: null,
        checklistItems: setup.checklistItems ?? [],
      }))
      .catch((err) => setError(err?.response?.data?.message ?? "Unable to load setup."))
      .finally(() => setLoading(false));
  }, [editId]);

  async function save() {
    setSaving(true);
    setError("");
    try {
      const saved = editId ? await updatePlaybookSetup(editId, form) : await createPlaybookSetup(form);
      navigate(`/playbook/${saved.id}`);
    } catch (err: any) {
      setError(err?.response?.data?.message ?? "Unable to save setup.");
    } finally {
      setSaving(false);
    }
  }

  async function draftWithAi() {
    if (!aiDescription.trim()) return;
    setSaving(true);
    setError("");
    try {
      const draft = await createPlaybookDraftFromDescription(aiDescription, form.accountId);
      if (draft.draft) setForm(draft.draft);
      onUsageChanged();
    } catch (err: any) {
      setError(err?.response?.data?.message ?? "Unable to create AI draft.");
    } finally {
      setSaving(false);
    }
  }

  const grouped = useMemo(() => {
    const map = new Map<ChecklistType, typeof form.checklistItems>();
    checklistTypes.forEach((type) => map.set(type, form.checklistItems.filter((item) => item.checklistType === type)));
    return map;
  }, [form.checklistItems]);

  return (
    <>
      <PageHeader
        label="Playbook • Pro"
        title={editId ? "Edit Playbook Setup" : "Create Playbook Setup"}
        description="Document setup rules first. Validation and AI review happen after trades are linked."
      />
      <section style={shell}>
        {error && <div style={{ ...card, color: "#fecaca" }}>{error}</div>}
        {loading ? <div style={card}>Loading setup...</div> : (
          <>
            {aiMode && !editId && (
              <div style={{ ...card, display: "grid", gap: 12 }}>
                <span style={label}>AI draft from description · 1 credit</span>
                <textarea style={textarea} value={aiDescription} onChange={(event) => setAiDescription(event.target.value)} placeholder="Describe the setup you trade..." />
                <button type="button" style={secondaryButton} disabled={saving} onClick={draftWithAi}>{saving ? "Drafting..." : "Create draft structure"}</button>
                <p style={muted}>The draft remains unsaved until you review and click Save Setup.</p>
              </div>
            )}

            <div style={card}>
              <div style={grid}>
                <Field label="Name"><input style={input} value={form.name} onChange={(e) => patch({ name: e.target.value })} /></Field>
                <Field label="Account">
                  <select style={input} value={form.accountId ?? ""} onChange={(e) => patch({ accountId: e.target.value ? Number(e.target.value) : null })}>
                    <option value="">All accounts / no scope</option>
                    {accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}
                  </select>
                </Field>
                <Field label="Category"><select style={input} value={form.category} onChange={(e) => patch({ category: e.target.value as any })}>{["CUSTOM","CONTINUATION","REVERSAL","BREAKOUT","PULLBACK","LIQUIDITY_SWEEP","FVG","IFVG","OPENING_RANGE","TREND_FOLLOWING","MEAN_REVERSION"].map(v => <option key={v}>{v}</option>)}</select></Field>
                <Field label="Status"><select style={input} value={form.status} onChange={(e) => patch({ status: e.target.value as any })}>{["DRAFT","TESTING","VALIDATED","PAUSED","ARCHIVED"].map(v => <option key={v}>{v}</option>)}</select></Field>
                <Field label="Direction"><select style={input} value={form.direction} onChange={(e) => patch({ direction: e.target.value as any })}>{["BOTH","LONG","SHORT"].map(v => <option key={v}>{v}</option>)}</select></Field>
                <Field label="Instruments"><input style={input} value={joinCsv(form.instruments)} onChange={(e) => patch({ instruments: splitCsv(e.target.value) })} placeholder="MES, MNQ" /></Field>
                <Field label="Sessions"><input style={input} value={joinCsv(form.sessions)} onChange={(e) => patch({ sessions: splitCsv(e.target.value) })} placeholder="NY AM, London" /></Field>
                <Field label="Tags"><input style={input} value={joinCsv(form.tags)} onChange={(e) => patch({ tags: splitCsv(e.target.value) })} /></Field>
              </div>
              <div style={{ display: "grid", gap: 12, marginTop: 14 }}>
                <Field label="Short description"><input style={input} value={form.shortDescription ?? ""} onChange={(e) => patch({ shortDescription: e.target.value })} /></Field>
                <Field label="Detailed description"><textarea style={textarea} value={form.description ?? ""} onChange={(e) => patch({ description: e.target.value })} /></Field>
              </div>
            </div>

            {checklistTypes.map((type) => (
              <div key={type} style={{ ...card, display: "grid", gap: 10 }}>
                <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
                  <strong>{type.replace("_", " ")}</strong>
                  <button type="button" style={secondaryButton} onClick={() => addChecklistItem(type)}>Add rule</button>
                </div>
                {(grouped.get(type) ?? []).map((item, index) => {
                  const globalIndex = form.checklistItems.indexOf(item);
                  return (
                    <div key={`${type}-${index}`} style={{ display: "grid", gridTemplateColumns: "minmax(0, 1fr) 90px 90px auto", gap: 8, alignItems: "center" }}>
                      <input style={input} value={item.label} onChange={(e) => updateChecklist(globalIndex, { label: e.target.value })} />
                      <label style={{ color: "#cbd5e1", fontSize: 13 }}><input type="checkbox" checked={item.required} onChange={(e) => updateChecklist(globalIndex, { required: e.target.checked })} /> Required</label>
                      <input style={input} type="number" min={1} value={item.weight} onChange={(e) => updateChecklist(globalIndex, { weight: Number(e.target.value) })} />
                      <button type="button" style={secondaryButton} onClick={() => removeChecklist(globalIndex)}>Remove</button>
                    </div>
                  );
                })}
              </div>
            ))}

            <div style={{ display: "flex", gap: 10, justifyContent: "flex-end", flexWrap: "wrap" }}>
              <button type="button" style={secondaryButton} onClick={() => navigate("/playbook")}>Cancel</button>
              <button type="button" style={button} disabled={saving} onClick={save}>{saving ? "Saving..." : "Save Setup"}</button>
            </div>
          </>
        )}
      </section>
    </>
  );

  function patch(next: Partial<PlaybookSetupPayload>) {
    setForm((current) => ({ ...current, ...next }));
  }

  function addChecklistItem(type: ChecklistType) {
    setForm((current) => ({
      ...current,
      checklistItems: [...current.checklistItems, { checklistType: type, label: "", required: true, weight: 1, displayOrder: current.checklistItems.length, active: true }],
    }));
  }

  function updateChecklist(index: number, next: Partial<PlaybookSetupPayload["checklistItems"][number]>) {
    setForm((current) => ({ ...current, checklistItems: current.checklistItems.map((item, i) => i === index ? { ...item, ...next } : item) }));
  }

  function removeChecklist(index: number) {
    setForm((current) => ({ ...current, checklistItems: current.checklistItems.filter((_, i) => i !== index) }));
  }
}

function Field({ label: fieldLabel, children }: { label: string; children: React.ReactNode }) {
  return (
    <label style={{ display: "grid", gap: 7 }}>
      <span style={label}>{fieldLabel}</span>
      {children}
    </label>
  );
}
