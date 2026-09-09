import { useEffect, useMemo, useRef, useState, type CSSProperties, type DragEvent, type KeyboardEvent, type ReactNode, type RefObject } from "react";
import { api } from "../../api/axios";
import {
  getPreAnalysis,
  getPreAnalysisHistory,
  getPreAnalysisPlaybook,
  submitPreAnalysis,
  submitPreAnalysisFeedback,
  updatePreAnalysisPlaybook,
  type ConditionEvidence,
  type FeedbackReasonCode,
  type KeyLevel,
  type PreAnalysisMarket,
  type PreAnalysisResult,
  type PreAnalysisSummary,
  type PreAnalysisTimeframe,
  type QualitativeScore,
  type TradeScenario,
  type TradingPlaybook,
} from "../../api/preAnalysisApi";
import { button, heroPill } from "../../styles/dashboardStyles";
import { extractApiError } from "../../utils/apiError";

const acceptedImageTypes = ["image/png", "image/jpeg", "image/webp"];
const maxImageBytes = 8 * 1024 * 1024;

const conditionRows: Array<[keyof PreAnalysisResult["analysis"]["conditions"], string]> = [
  ["break", "Break"],
  ["acceptance", "Acceptance"],
  ["displacement", "Displacement"],
  ["fvgIfvg", "FVG / IFVG"],
  ["retrace", "Retracement"],
  ["confirmation", "Confirmation"],
];

export default function PreAnalysisPage() {
  const [playbook, setPlaybook] = useState<TradingPlaybook | null>(null);
  const [history, setHistory] = useState<PreAnalysisSummary[]>([]);
  const [selected, setSelected] = useState<PreAnalysisResult | null>(null);
  const [market, setMarket] = useState<PreAnalysisMarket>("ES");
  const [timeframe, setTimeframe] = useState<PreAnalysisTimeframe>("M5");
  const [traderNote, setTraderNote] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [filePreviewUrl, setFilePreviewUrl] = useState("");
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [loading, setLoading] = useState(true);
  const [analyzing, setAnalyzing] = useState(false);
  const [savingPlaybook, setSavingPlaybook] = useState(false);
  const [feedbackReason, setFeedbackReason] = useState<FeedbackReasonCode>("MISREAD_STRUCTURE");
  const [feedbackComment, setFeedbackComment] = useState("");
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [inputExpanded, setInputExpanded] = useState(false);
  const [playbookExpanded, setPlaybookExpanded] = useState(false);

  useEffect(() => {
    let active = true;
    Promise.all([getPreAnalysisPlaybook(), getPreAnalysisHistory()])
      .then(([loadedPlaybook, loadedHistory]) => {
        if (!active) return;
        setPlaybook(loadedPlaybook);
        setHistory(loadedHistory);
      })
      .catch((err) => active && setError(extractApiError(err, "Unable to load AI Pre-Analysis.")))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!file) {
      setFilePreviewUrl("");
      return;
    }
    const url = URL.createObjectURL(file);
    setFilePreviewUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [file]);

  const alignment = useMemo(() => {
    if (!selected?.analysis?.conditions) return "—";
    const satisfied = conditionRows.filter(([key]) => selected.analysis.conditions[key]?.status === "SATISFIED").length;
    return `${satisfied}/6`;
  }, [selected]);

  async function handleAnalyze() {
    if (!file) {
      setError("Upload a chart screenshot first.");
      return;
    }
    setAnalyzing(true);
    setError("");
    setNotice("");
    try {
      const result = await submitPreAnalysis({ market, timeframe, traderNote, screenshot: file });
      setSelected(result);
      setInputExpanded(false);
      setPlaybookExpanded(false);
      setHistory(await getPreAnalysisHistory());
      setNotice("Analysis generated.");
    } catch (err) {
      setError(extractApiError(err, "AI Pre-Analysis failed."));
    } finally {
      setAnalyzing(false);
    }
  }

  function chooseFile(nextFile?: File | null) {
    if (!nextFile) return;
    if (!acceptedImageTypes.includes(nextFile.type)) {
      setFile(null);
      setError("Chart screenshot must be PNG, JPEG, or WebP.");
      return;
    }
    if (nextFile.size > maxImageBytes) {
      setFile(null);
      setError("Chart screenshot must be 8MB or smaller.");
      return;
    }
    setError("");
    setFile(nextFile);
  }

  function removeFile() {
    setFile(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  }

  async function loadHistoryItem(id: number) {
    setError("");
    try {
      setSelected(await getPreAnalysis(id));
      setInputExpanded(false);
      setPlaybookExpanded(false);
    } catch (err) {
      setError(extractApiError(err, "Unable to open analysis."));
    }
  }

  async function savePlaybook(next: TradingPlaybook) {
    setSavingPlaybook(true);
    setError("");
    setNotice("");
    try {
      const saved = await updatePreAnalysisPlaybook(next);
      setPlaybook(saved);
      setNotice("Playbook updated.");
    } catch (err) {
      setError(extractApiError(err, "Unable to update playbook."));
    } finally {
      setSavingPlaybook(false);
    }
  }

  async function submitFeedback(rating: 1 | -1) {
    if (!selected) return;
    setError("");
    setNotice("");
    try {
      const feedback = await submitPreAnalysisFeedback(selected.id, {
        rating,
        reasonCode: rating < 0 ? feedbackReason : null,
        comment: feedbackComment,
      });
      setSelected({ ...selected, feedback });
      setHistory(await getPreAnalysisHistory());
      setNotice("Feedback saved.");
    } catch (err) {
      setError(extractApiError(err, "Unable to save feedback."));
    }
  }

  if (loading) {
    return <section style={panel}>Loading AI Pre-Analysis...</section>;
  }
  const showFullInput = !selected || inputExpanded;
  const showFullPlaybook = !selected || playbookExpanded;
  const showInputGrid = showFullInput || showFullPlaybook;
  const showBothPanels = showFullInput && showFullPlaybook;

  return (
    <div style={page}>
      <style>{`
        @media (max-width: 980px) {
          .pre-analysis-grid,
          .pre-analysis-result-grid,
          .pre-analysis-path-grid,
          .pre-analysis-score-strip {
            grid-template-columns: minmax(0, 1fr) !important;
          }
          .pre-analysis-condition-row,
          .pre-analysis-history-button {
            grid-template-columns: minmax(0, 1fr) !important;
          }
          .pre-analysis-selected-upload {
            grid-template-columns: 72px minmax(0, 1fr) !important;
          }
          .pre-analysis-selected-upload img {
            width: 72px !important;
            height: 58px !important;
          }
          .pre-analysis-upload-actions {
            grid-column: 1 / -1 !important;
            justify-content: flex-start !important;
          }
          .pre-analysis-analysis-pair,
          .pre-analysis-scenario-grid {
            grid-template-columns: minmax(0, 1fr) !important;
          }
          .pre-analysis-waiting-card {
            order: -1;
          }
          .pre-analysis-result-header {
            grid-template-columns: minmax(0, 1fr) !important;
          }
          .pre-analysis-compact-toolbar,
          .pre-analysis-result-facts {
            grid-template-columns: minmax(0, 1fr) !important;
          }
        }
      `}</style>
      <section style={header}>
        <div>
          <div style={eyebrowRow}>
            <h1 style={title}>AI Pre-Analysis</h1>
            <span style={privateBadge}>Private Beta</span>
          </div>
          <p style={subtitle}>Check the chart against your playbook before you enter.</p>
        </div>
      </section>

      {notice && <div style={successBox}>{notice}</div>}
      {error && <div style={errorBox}>{error}</div>}

      {selected && (
        <CompactAnalysisToolbar
          result={selected}
          file={file}
          analyzing={analyzing}
          playbook={playbook}
          onChangeChart={() => setInputExpanded(true)}
          onAnalyzeAgain={() => file ? handleAnalyze() : setInputExpanded(true)}
          onViewPlaybook={() => setPlaybookExpanded(true)}
        />
      )}

      {showInputGrid && (
        <div className="pre-analysis-grid" style={inputGrid(showBothPanels)}>
          {showFullInput && (
            <section style={panel}>
              <h2 style={sectionTitle}>Analyze Chart</h2>
              <div style={fieldGrid}>
                <label style={fieldLabel}>Market
                  <select value={market} onChange={(event) => setMarket(event.target.value as PreAnalysisMarket)} style={input}>
                    <option value="ES">ES</option>
                    <option value="NQ">NQ</option>
                  </select>
                </label>
                <label style={fieldLabel}>Timeframe
                  <select value={timeframe} onChange={(event) => setTimeframe(event.target.value as PreAnalysisTimeframe)} style={input}>
                    <option value="M15">15m</option>
                    <option value="M5">5m</option>
                    <option value="M1">1m</option>
                  </select>
                </label>
              </div>
              <label style={fieldLabel}>Chart Screenshot
                <ChartScreenshotUpload
                  file={file}
                  previewUrl={filePreviewUrl}
                  inputRef={fileInputRef}
                  onChoose={chooseFile}
                  onRemove={removeFile}
                />
              </label>
              <label style={fieldLabel}>
                <span>What are you watching? <span style={optionalText}>Optional</span></span>
                <textarea value={traderNote} onChange={(event) => setTraderNote(event.target.value)} placeholder="Looking for a short below 7655" rows={3} style={textarea} />
              </label>
              <button type="button" onClick={handleAnalyze} disabled={analyzing || !file} style={primaryButton(analyzing || !file)}>
                {analyzing ? "Analyzing..." : "Analyze Chart"}
              </button>
              {!file && <p style={helperText}>Upload a chart screenshot to analyze.</p>}
            </section>
          )}

          {showFullPlaybook && <PlaybookPanel playbook={playbook} saving={savingPlaybook} onSave={savePlaybook} compact={Boolean(selected)} />}
        </div>
      )}

      {selected && <ResultPanel result={selected} alignment={alignment} feedbackReason={feedbackReason} feedbackComment={feedbackComment} onReasonChange={setFeedbackReason} onCommentChange={setFeedbackComment} onFeedback={submitFeedback} />}

      <section style={panel}>
        <h2 style={sectionTitle}>Recent Analyses</h2>
        {history.length === 0 ? (
          <p style={muted}>No pre-analyses yet.</p>
        ) : (
          <div style={historyList}>
            {history.map((item) => (
              <button className="pre-analysis-history-button" key={item.id} type="button" onClick={() => loadHistoryItem(item.id)} style={historyButton(selected?.id === item.id)}>
                <span>{new Date(item.createdAt ?? "").toLocaleString()}</span>
                <strong>{item.market} · {item.displayTimeframe}</strong>
                <span>{statusLabel(item.setupStatus)} · {item.bias} · {item.setupAlignmentScore == null ? "—" : `${item.setupAlignmentScore}%`}</span>
                <span>{item.feedbackRating ? (item.feedbackRating > 0 ? "Useful" : "Needs review") : "No feedback"}</span>
              </button>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function ResultPanel({ result, alignment, feedbackReason, feedbackComment, onReasonChange, onCommentChange, onFeedback }: {
  result: PreAnalysisResult;
  alignment: string;
  feedbackReason: FeedbackReasonCode;
  feedbackComment: string;
  onReasonChange: (value: FeedbackReasonCode) => void;
  onCommentChange: (value: string) => void;
  onFeedback: (rating: 1 | -1) => void;
}) {
  const [imageSrc, setImageSrc] = useState("");
  const analysis = result.analysis;
  const marketRead = analysis.marketRead;
  const scores = analysis.contextScores;
  const displayKeyLevels = analysis.keyLevels?.length
    ? analysis.keyLevels
    : analysis.importantLevels.map((level) => ({ price: level.price, label: level.label, role: level.role, category: "LEGACY", importance: null, reason: null }));
  const posture = postureLabel(marketRead?.entryPosture ?? analysis.entryAssessment.status ?? result.setupStatus);
  const bias = humanizeToken(marketRead?.marketBias ?? analysis.directionalContext.bias);
  const primaryScenario = preferredScenarioKind(analysis.tradePlan?.preferredScenario);
  useEffect(() => {
    let active = true;
    api.get(result.imageUrl, { responseType: "blob" })
      .then((res) => {
        if (!active) return;
        setImageSrc(URL.createObjectURL(res.data));
      })
      .catch(() => setImageSrc(""));
    return () => {
      active = false;
      if (imageSrc) URL.revokeObjectURL(imageSrc);
    };
  }, [result.id]);

  return (
    <section style={resultPanel}>
      <div className="pre-analysis-result-header" style={resultHeader}>
        <div>
          <div style={resultMarket}>{result.market} · {result.displayTimeframe}</div>
          <h2 style={postureTitle}>{posture}</h2>
          <p style={postureSummary}>{marketRead?.summary ?? analysis.entryAssessment.summary ?? analysis.directionalContext.reasoning ?? "No market read available."}</p>
        </div>
        <div className="pre-analysis-result-facts" style={resultFacts}>
          <div style={factBlock}>
            <span>Bias</span>
            <strong>{bias}</strong>
          </div>
          <div style={factBlock}>
            <span>Setup Progress</span>
            <strong>{alignment}</strong>
            {result.setupAlignmentScore != null && <small>{result.setupAlignmentScore}%</small>}
          </div>
          <div style={factBlock}>
            <span>Canonical Status</span>
            <strong>{humanizeToken(result.setupStatus)}</strong>
          </div>
        </div>
      </div>
      <div style={scoreStrip}>
        <ScoreTile title="Structure" score={scores?.structure} />
        <ScoreTile title="Location" score={scores?.location} />
        <ScoreTile title="Confirmation" score={scores?.confirmation} />
        <ScoreTile title="Relative Strength" score={scores?.relativeStrength} unavailableText="No correlated-market data available." />
      </div>
      <div className="pre-analysis-analysis-pair" style={analysisPair}>
        <MarketReadSection result={result} />
        <TradePlanCard plan={analysis.tradePlan} fallbackItems={analysis.entryAssessment.whatMustHappenNext} />
      </div>
      <KeyLevelsSection levels={displayKeyLevels} />
      <div className="pre-analysis-result-grid" style={resultGrid}>
        {imageSrc && <img src={imageSrc} alt="Uploaded chart screenshot" style={chartImage} />}
        <div style={conditionGrid}>
          {conditionRows.map(([key, label]) => (
            <ConditionRow key={key} label={label} item={result.analysis.conditions[key]} />
          ))}
        </div>
      </div>
      <div className="pre-analysis-scenario-grid" style={scenarioGrid}>
        {primaryScenario === "bearish" ? (
          <>
            <ScenarioCard title="Preferred Bearish Path" scenario={analysis.bearishScenario} tone="bearish" emphasis />
            <ScenarioCard title="Bullish Alternative" scenario={analysis.bullishScenario} tone="bullish" />
          </>
        ) : primaryScenario === "bullish" ? (
          <>
            <ScenarioCard title="Preferred Bullish Path" scenario={analysis.bullishScenario} tone="bullish" emphasis />
            <ScenarioCard title="Bearish Alternative" scenario={analysis.bearishScenario} tone="bearish" />
          </>
        ) : (
          <>
            <ScenarioCard title="Bullish Path" scenario={analysis.bullishScenario} tone="bullish" />
            <ScenarioCard title="Bearish Path" scenario={analysis.bearishScenario} tone="bearish" />
          </>
        )}
        <NoTradeCard scenario={analysis.noTradeScenario} fallback={result.setupStatus === "NO_TRADE" ? analysis.entryAssessment.summary ?? undefined : undefined} />
      </div>
      <CrossMarketSection crossMarket={analysis.crossMarket} />
      <DetailLine label="Invalidation" value={analysis.invalidation.summary ?? analysis.tradePlan?.invalidation ?? "Not specified."} />
      <DetailPriceLevels title="Invalidation Levels" levels={analysis.invalidation.levels} />
      <DetailLevels title="Potential Targets" levels={analysis.potentialTargets.map((target) => ({ label: target.reason ?? "Target", price: target.price, role: "TARGET" }))} />
      <DetailList title="Rule Violations" items={analysis.ruleViolations} />
      <DetailList title="Warnings" items={analysis.warnings} />
      {analysis.narrative && <p style={narrative}>{analysis.narrative}</p>}
      <div style={feedbackBox}>
        <strong>Was this analysis useful?</strong>
        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
          <button type="button" onClick={() => onFeedback(1)} style={smallButton}>Thumbs up</button>
          <button type="button" onClick={() => onFeedback(-1)} style={smallButton}>Thumbs down</button>
        </div>
        <select value={feedbackReason} onChange={(event) => onReasonChange(event.target.value as FeedbackReasonCode)} style={input}>
          <option value="MISREAD_STRUCTURE">Misread structure</option>
          <option value="WRONG_LEVEL">Wrong level</option>
          <option value="MISSED_FVG">Missed FVG</option>
          <option value="WRONG_BIAS">Wrong bias</option>
          <option value="DID_NOT_FOLLOW_PLAYBOOK">Didn't follow my rules</option>
          <option value="OTHER">Other</option>
        </select>
        <textarea value={feedbackComment} onChange={(event) => onCommentChange(event.target.value)} rows={2} placeholder="Optional comment" style={textarea} />
      </div>
    </section>
  );
}

function ChartScreenshotUpload({
  file,
  previewUrl,
  inputRef,
  onChoose,
  onRemove,
}: {
  file: File | null;
  previewUrl: string;
  inputRef: RefObject<HTMLInputElement | null>;
  onChoose: (file?: File | null) => void;
  onRemove: () => void;
}) {
  const [dragActive, setDragActive] = useState(false);

  function openFilePicker() {
    inputRef.current?.click();
  }

  function onKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      openFilePicker();
    }
  }

  function onDrop(event: DragEvent<HTMLDivElement>) {
    event.preventDefault();
    setDragActive(false);
    onChoose(event.dataTransfer.files?.[0]);
  }

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={openFilePicker}
      onKeyDown={onKeyDown}
      onDragEnter={(event) => {
        event.preventDefault();
        setDragActive(true);
      }}
      onDragOver={(event) => event.preventDefault()}
      onDragLeave={() => setDragActive(false)}
      onDrop={onDrop}
      style={uploadDropzone(dragActive, Boolean(file))}
      aria-label={file ? "Replace chart screenshot" : "Choose chart screenshot"}
    >
      <input
        ref={inputRef}
        type="file"
        accept={acceptedImageTypes.join(",")}
        onChange={(event) => onChoose(event.target.files?.[0])}
        style={hiddenFileInput}
        aria-hidden="true"
        tabIndex={-1}
      />
      {file ? (
        <div className="pre-analysis-selected-upload" style={selectedUpload}>
          {previewUrl && <img src={previewUrl} alt="" style={uploadThumbnail} />}
          <div style={selectedFileDetails}>
            <strong style={selectedFileName}>{file.name}</strong>
            <span>{formatFileSize(file.size)}</span>
          </div>
          <div className="pre-analysis-upload-actions" style={uploadActions}>
            <button type="button" onClick={(event) => { event.stopPropagation(); openFilePicker(); }} style={secondaryButton}>Replace</button>
            <button type="button" onClick={(event) => { event.stopPropagation(); onRemove(); }} style={dangerButton}>Remove</button>
          </div>
        </div>
      ) : (
        <div style={emptyUpload}>
          <div style={uploadIcon}>Upload</div>
          <strong>Drop your chart screenshot here</strong>
          <span>or</span>
          <span style={chooseImageButton}>Choose Image</span>
          <small>PNG, JPG or WEBP · 8 MB max</small>
        </div>
      )}
    </div>
  );
}

function CompactAnalysisToolbar({
  result,
  file,
  analyzing,
  playbook,
  onChangeChart,
  onAnalyzeAgain,
  onViewPlaybook,
}: {
  result: PreAnalysisResult;
  file: File | null;
  analyzing: boolean;
  playbook: TradingPlaybook | null;
  onChangeChart: () => void;
  onAnalyzeAgain: () => void;
  onViewPlaybook: () => void;
}) {
  return (
    <section className="pre-analysis-compact-toolbar" style={compactToolbar} aria-label="Analysis controls">
      <div style={compactToolbarMain}>
        <strong>{result.market} · {result.displayTimeframe}</strong>
        <span>{file?.name ? `Screenshot analyzed · ${file.name}` : "Screenshot analyzed"}</span>
      </div>
      <div style={compactPlaybookSummary}>
        <span>Active Playbook</span>
        <strong>{playbook?.name ?? result.playbookName}</strong>
      </div>
      <div style={compactToolbarActions}>
        <button type="button" onClick={onChangeChart} style={secondaryButton}>Change Chart</button>
        <button type="button" onClick={onAnalyzeAgain} disabled={analyzing} style={primaryButton(analyzing)}>{analyzing ? "Analyzing..." : "Analyze Again"}</button>
        <button type="button" onClick={onViewPlaybook} style={secondaryButton}>View Playbook</button>
      </div>
    </section>
  );
}

function PlaybookPanel({ playbook, saving, onSave, compact = false }: { playbook: TradingPlaybook | null; saving: boolean; onSave: (playbook: TradingPlaybook) => void; compact?: boolean }) {
  const [editing, setEditing] = useState(false);
  if (!playbook) return <section style={panel}>No active playbook.</section>;
  if (editing) {
    return <PlaybookEditor playbook={playbook} saving={saving} onCancel={() => setEditing(false)} onSave={onSave} />;
  }
  const timeframeSummary = [
    playbook.contextTimeframes?.length ? `${playbook.contextTimeframes.join(", ")} context` : null,
    playbook.setupTimeframes?.length ? `${playbook.setupTimeframes.join(", ")} setup` : null,
    playbook.executionTimeframes?.length ? `${playbook.executionTimeframes.join(", ")} execution` : null,
  ].filter(Boolean).join(" · ");
  return (
    <section style={playbookPanel}>
      <div style={playbookHeader}>
        <div>
          <h2 style={sectionTitle}>Active Playbook</h2>
          <strong style={playbookName}>{playbook.name}</strong>
        </div>
        <button type="button" onClick={() => setEditing(true)} style={secondaryButton}>Edit Playbook</button>
      </div>
      {compact && <p style={playbookDescription}>Expanded playbook context for this analysis.</p>}
      {!compact && playbook.description && <p style={playbookDescription}>{playbook.description}</p>}
      {timeframeSummary && <p style={playbookMeta}>{timeframeSummary}</p>}
      <p style={playbookMeta}>Markets: {playbook.markets?.length ? playbook.markets.join(", ") : "Not specified"}</p>
      <SequenceSummary />
      <SummaryList title="Core Rules" items={coreRules(playbook)} />
      {compact && playbook.description && <SummaryList title="Description" items={[playbook.description]} />}
    </section>
  );
}

function PlaybookEditor({ playbook, saving, onSave, onCancel }: { playbook: TradingPlaybook | null; saving: boolean; onSave: (playbook: TradingPlaybook) => void; onCancel: () => void }) {
  const [draft, setDraft] = useState<TradingPlaybook | null>(playbook);
  useEffect(() => setDraft(playbook), [playbook]);
  if (!draft) return <section style={panel}>No active playbook.</section>;
  const updateList = (key: keyof TradingPlaybook, value: string) => setDraft({ ...draft, [key]: splitLines(value) });
  return (
    <section style={panel}>
      <h2 style={sectionTitle}>Active Playbook</h2>
      <input value={draft.name} onChange={(event) => setDraft({ ...draft, name: event.target.value })} style={input} />
      <textarea value={draft.description ?? ""} onChange={(event) => setDraft({ ...draft, description: event.target.value })} rows={3} style={textarea} />
      <RuleTextarea label="Setup Sequence" value={draft.setupSequence} onChange={(value) => updateList("setupSequence", value)} />
      <RuleTextarea label="Entry Rules" value={draft.entryRules} onChange={(value) => updateList("entryRules", value)} />
      <RuleTextarea label="Avoid Rules" value={draft.avoidRules} onChange={(value) => updateList("avoidRules", value)} />
      <div style={editActions}>
        <button type="button" onClick={onCancel} disabled={saving} style={secondaryButton}>Cancel</button>
        <button type="button" onClick={() => onSave(draft)} disabled={saving} style={primaryButton(saving)}>{saving ? "Saving..." : "Save Playbook"}</button>
      </div>
    </section>
  );
}

function SummaryList({ title, items, inline = false }: { title: string; items: string[]; inline?: boolean }) {
  if (!items?.length) return null;
  return (
    <div style={summaryBlock}>
      <strong>{title}:</strong>
      {inline ? (
        <span>{items.join(" -> ")}</span>
      ) : (
        <div style={summaryItems}>{items.map((item) => <span key={item}>• {item}</span>)}</div>
      )}
    </div>
  );
}

function SequenceSummary() {
  const steps = ["Break", "Acceptance", "Displacement", "FVG/IFVG", "Retrace", "Confirmation", "Entry"];
  return (
    <div style={summaryBlock}>
      <strong>Sequence:</strong>
      <div style={sequenceFlow}>
        {steps.map((step, index) => (
          <span key={step} style={sequenceStepWrap}>
            <span style={sequenceStep}>{step}</span>
            {index < steps.length - 1 && <span style={sequenceArrow}>→</span>}
          </span>
        ))}
      </div>
    </div>
  );
}

function coreRules(playbook: TradingPlaybook) {
  const preferred = [
    "Do not anticipate breaks",
    "Do not chase displacement",
    "Correct bias ≠ valid entry",
    "No clean retracement = no trade",
    "Avoid choppy/two-sided price action",
  ];
  const existing = [...(playbook.entryRules ?? []), ...(playbook.avoidRules ?? [])];
  const normalized = new Map(existing.map((rule) => [rule.toLowerCase(), rule]));
  return preferred.map((rule) => normalized.get(rule.toLowerCase()) ?? rule);
}

function RuleTextarea({ label, value, onChange }: { label: string; value: string[]; onChange: (value: string) => void }) {
  return <label style={fieldLabel}>{label}<textarea value={value.join("\n")} onChange={(event) => onChange(event.target.value)} rows={4} style={textarea} /></label>;
}

function ConditionRow({ label, item }: { label: string; item: ConditionEvidence }) {
  return <div className="pre-analysis-condition-row" style={conditionRow}><span>{label}</span><strong>{conditionLabel(item?.status)}</strong><small>{item?.evidence}</small></div>;
}

function ScoreTile({ title, score, unavailableText }: { title: string; score?: QualitativeScore | null; unavailableText?: string }) {
  return (
    <div style={scoreTile}>
      <span style={scoreLabel}>{title}</span>
      <strong style={scoreValue}>{score?.score == null ? "—" : `${score.score}/10`}</strong>
      {score?.label && <small style={scoreDescriptor}>{humanizeToken(score.label)}</small>}
      <p style={scoreReason}>{score?.reason ?? unavailableText ?? "No score detail supplied."}</p>
    </div>
  );
}

function TerminalSection({ title, children }: { title: string; children: ReactNode }) {
  return <section style={terminalSection}><h3 style={terminalTitle}>{title}</h3>{children}</section>;
}

function MarketReadSection({ result }: { result: PreAnalysisResult }) {
  const analysis = result.analysis;
  const marketRead = analysis.marketRead;
  if (!marketRead?.summary && !analysis.directionalContext.reasoning) return null;
  return (
    <TerminalSection title="Market Read">
      <p style={marketReadCopy}>{marketRead?.summary ?? analysis.directionalContext.reasoning}</p>
      <DetailLine label="Bias" value={`${humanizeToken(marketRead?.marketBias ?? analysis.directionalContext.bias)} · ${analysis.directionalContext.confidenceLabel ?? "Unrated"}`} />
      <DetailLine label="State" value={humanizeToken(marketRead?.marketState ?? analysis.marketState ?? "UNCLEAR")} />
      {marketRead?.entryPosture && <DetailLine label="Entry Posture" value={postureLabel(marketRead.entryPosture)} />}
    </TerminalSection>
  );
}

function KeyLevelsSection({ levels }: { levels: KeyLevel[] }) {
  const [expanded, setExpanded] = useState(false);
  if (!levels?.length) return null;
  const sorted = [...levels].sort((left, right) => importanceRank(left.importance) - importanceRank(right.importance));
  const visible = expanded ? sorted : sorted.slice(0, 6);
  return (
    <TerminalSection title="Key Levels">
      <div style={keyLevelList}>
        {visible.map((level, index) => (
          <div key={`${level.price}-${level.label}-${index}`} style={keyLevelRow}>
            <strong>{formatPrice(level.price)}</strong>
            <span>{level.label ?? humanizeToken(level.category ?? "Level")}</span>
            <small>{[level.role, level.importance].filter(Boolean).map(humanizeToken).join(" · ")}</small>
            {level.reason && <p>{level.reason}</p>}
          </div>
        ))}
      </div>
      {sorted.length > visible.length && <button type="button" onClick={() => setExpanded(true)} style={levelToggleButton}>Show all levels</button>}
    </TerminalSection>
  );
}

function ScenarioCard({ title, scenario, tone, emphasis = false }: { title: string; scenario?: TradeScenario | null; tone: "bullish" | "bearish"; emphasis?: boolean }) {
  if (!scenario) return null;
  return (
    <section style={scenarioCard(tone, emphasis)}>
      <div style={scenarioHeader}>
        <h3 style={terminalTitle}>{title}</h3>
        {scenario.status && <span style={scenarioStatus}>{humanizeToken(scenario.status)}</span>}
      </div>
      {scenario.thesis && <p style={sectionCopy}>{scenario.thesis}</p>}
      {(scenario.entryZoneLow != null || scenario.entryZoneHigh != null) && (
        <DetailLine label="Entry Zone" value={`${formatPrice(scenario.entryZoneLow)} - ${formatPrice(scenario.entryZoneHigh)}`} />
      )}
      {scenario.trigger && <DetailLine label="Trigger" value={scenario.trigger} />}
      <DetailList title="Confirmations" items={scenario.confirmations ?? []} />
      <ScenarioTargets targets={scenario.targets ?? []} />
      {scenario.invalidation && <DetailLine label="Invalidation" value={scenario.invalidation} />}
      {scenario.quality && <DetailLine label="Quality" value={humanizeToken(scenario.quality)} />}
      <DetailList title="Notes" items={scenario.notes ?? []} />
    </section>
  );
}

function ScenarioTargets({ targets }: { targets: NonNullable<TradeScenario["targets"]> }) {
  if (!targets.length) return null;
  const sorted = [...targets].sort((left, right) => (left.order ?? 999) - (right.order ?? 999));
  return (
    <div style={detailBlock}>
      <strong>Targets</strong>
      {sorted.length ? sorted.map((target, index) => <span key={`${target.price}-${index}`}>{target.order ?? index + 1}. {formatPrice(target.price)} {target.reason ? `· ${target.reason}` : ""}</span>) : <span>No confirmed target levels supplied.</span>}
    </div>
  );
}

function NoTradeCard({ scenario, fallback }: { scenario?: PreAnalysisResult["analysis"]["noTradeScenario"] | null; fallback?: string }) {
  const condition = scenario?.condition ?? "No-trade condition";
  const reason = scenario?.reason ?? fallback;
  if (!reason) return null;
  return (
    <section style={noTradeSection}>
      <h3 style={terminalTitle}>No Trade</h3>
      <DetailLine label="Condition" value={condition} />
      <p style={sectionCopy}>{reason}</p>
    </section>
  );
}

function TradePlanCard({ plan, fallbackItems }: { plan?: PreAnalysisResult["analysis"]["tradePlan"] | null; fallbackItems: string[] }) {
  if (!plan && !fallbackItems?.length) return null;
  return (
    <section className="pre-analysis-waiting-card" style={waitingSection}>
      <h3 style={terminalTitle}>What TradePilot Is Waiting For</h3>
      {plan?.preferredScenario && <DetailLine label="Primary" value={humanizeToken(plan.preferredScenario)} />}
      {(plan?.waitFor || fallbackItems?.length > 0) && <DetailLine label="Wait For" value={plan?.waitFor ?? fallbackItems.join(" · ")} />}
      <DetailList title="Confirmation" items={plan?.confirmationNeeded ?? fallbackItems ?? []} />
      <DetailList title="Avoid" items={plan?.avoid ?? []} />
      {plan?.invalidation && <DetailLine label="Invalidation" value={plan.invalidation} />}
      {plan?.targetSummary && <DetailLine label="Targets" value={plan.targetSummary} />}
    </section>
  );
}

function CrossMarketSection({ crossMarket }: { crossMarket?: PreAnalysisResult["analysis"]["crossMarket"] | null }) {
  if (!crossMarket) return null;
  if (!crossMarket.available) {
    return (
      <TerminalSection title="Cross-Market">
        <DetailLine label="Status" value="Not available" />
        {crossMarket.interpretation && <p style={sectionCopy}>{crossMarket.interpretation}</p>}
      </TerminalSection>
    );
  }
  return (
    <TerminalSection title="Cross-Market">
      <DetailLine label="Pair" value={`${crossMarket.instrument ?? "—"} vs ${crossMarket.comparisonInstrument ?? "—"}`} />
      <DetailLine label="Leader" value={humanizeToken(crossMarket.leader ?? "UNKNOWN")} />
      <DetailLine label="SMT" value={humanizeToken(crossMarket.smtStatus ?? "NOT_ENOUGH_DATA")} />
      {crossMarket.interpretation && <p style={sectionCopy}>{crossMarket.interpretation}</p>}
    </TerminalSection>
  );
}

function DetailLine({ label, value }: { label: string; value: string }) {
  return <div style={detailLine}><strong>{label}</strong><span>{value}</span></div>;
}

function DetailList({ title, items }: { title: string; items: string[] }) {
  if (!items?.length) return null;
  return <div style={detailBlock}><strong>{title}</strong>{items.map((item) => <span key={item}>• {item}</span>)}</div>;
}

function DetailLevels({ title, levels }: { title: string; levels: { label?: string | null; price?: number | null; role?: string | null }[] }) {
  if (!levels?.length) return null;
  return <div style={detailBlock}><strong>{title}</strong>{levels.map((level, index) => <span key={`${level.label}-${index}`}>{level.label ?? "Level"} {level.price != null ? `· ${level.price}` : ""} {level.role ? `· ${level.role}` : ""}</span>)}</div>;
}

function DetailPriceLevels({ title, levels }: { title: string; levels: number[] }) {
  if (!levels?.length) return null;
  return <div style={detailBlock}><strong>{title}</strong>{levels.map((level, index) => <span key={`${level}-${index}`}>{level}</span>)}</div>;
}

function splitLines(value: string) {
  return value.split("\n").map((item) => item.trim()).filter(Boolean);
}

function formatFileSize(size: number) {
  if (size < 1024 * 1024) return `${Math.max(1, Math.round(size / 1024))} KB`;
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

function statusLabel(status: string) {
  return humanizeToken(status);
}

function conditionLabel(status?: string) {
  return status ? humanizeToken(status) : "Waiting";
}

function formatToken(value?: string | null) {
  return humanizeToken(value);
}

function humanizeToken(value?: string | null) {
  if (!value) return "—";
  const normalized = value.trim().replace(/[\s-]+/g, "_").toUpperCase();
  const special: Record<string, string> = {
    WAITING_FOR_RETRACE: "Waiting for retrace",
    WAIT_FOR_RETRACEMENT: "Wait for retracement",
    WAIT_FOR_CONFIRMATION: "Wait for confirmation",
    WAITING_FOR_CONFIRMATION: "Wait for confirmation",
    POOR_TO_CHASE: "Poor location — do not chase",
    NO_TRADE: "No trade",
    FAVORABLE: "Favorable",
    ACCEPTABLE: "Acceptable",
    SETUP_FORMING: "Setup forming",
    ENTRY_VALID: "Entry valid",
    ARMED: "Armed",
    WATCHING: "Watching",
    INVALIDATED: "Invalidated",
    NEUTRAL_BULLISH: "Neutral → Bullish",
    NEUTRAL_BEARISH: "Neutral → Bearish",
    STRONG_BULLISH: "Strong bullish",
    STRONG_BEARISH: "Strong bearish",
    NOT_ENOUGH_DATA: "Not enough data",
    NOT_AVAILABLE: "Not available",
    WAIT_FOR_MORE_DATA: "Wait for more data",
    BULLISH_RETRACEMENT: "Bullish retracement",
    BEARISH_REJECTION: "Bearish rejection",
    NOT_CONFIRMED: "Not confirmed",
    MID_RANGE: "Mid range",
  };
  if (special[normalized]) return special[normalized];
  return normalized
    .split("_")
    .filter(Boolean)
    .map((part) => part.charAt(0) + part.slice(1).toLowerCase())
    .join(" ");
}

function postureLabel(value?: string | null) {
  const normalized = value?.trim().replace(/[\s-]+/g, "_").toUpperCase();
  if (normalized === "WAIT_FOR_RETRACEMENT") return "WAIT FOR RETRACEMENT";
  if (normalized === "WAIT_FOR_CONFIRMATION" || normalized === "WAITING_FOR_CONFIRMATION") return "WAIT FOR CONFIRMATION";
  if (normalized === "POOR_TO_CHASE") return "POOR LOCATION — DO NOT CHASE";
  if (normalized === "NO_TRADE") return "NO TRADE";
  if (normalized === "FAVORABLE") return "SETUP FAVORABLE";
  if (normalized === "ACCEPTABLE") return "SETUP ACCEPTABLE";
  return humanizeToken(value).toUpperCase();
}

function preferredScenarioKind(value?: string | null): "bullish" | "bearish" | null {
  const normalized = value?.toUpperCase() ?? "";
  if (normalized.includes("BEARISH")) return "bearish";
  if (normalized.includes("BULLISH")) return "bullish";
  return null;
}

function importanceRank(value?: string | null) {
  const normalized = value?.toUpperCase();
  if (normalized === "HIGH") return 0;
  if (normalized === "MEDIUM") return 1;
  if (normalized === "LOW") return 2;
  return 3;
}

function formatPrice(value?: number | null) {
  if (value == null) return "—";
  return Number.isInteger(value) ? value.toLocaleString() : value.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

const page: CSSProperties = { display: "grid", gap: 16 };
const header: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 18, alignItems: "flex-start" };
const eyebrowRow: CSSProperties = { display: "flex", gap: 10, flexWrap: "wrap", alignItems: "center" };
const eyebrow: CSSProperties = { ...heroPill, width: "fit-content" };
const privateBadge: CSSProperties = { ...heroPill, width: "fit-content", padding: "8px 12px", color: "#fde68a", borderColor: "rgba(245,158,11,0.36)", background: "rgba(245,158,11,0.12)" };
const title: CSSProperties = { margin: 0, fontSize: 34, lineHeight: 1.1, color: "#f8fafc" };
const subtitle: CSSProperties = { margin: 0, color: "#cbd5e1", maxWidth: 760 };
const inputGrid = (twoColumns: boolean): CSSProperties => ({ display: "grid", gridTemplateColumns: twoColumns ? "minmax(360px, 1.35fr) minmax(300px, 0.85fr)" : "minmax(0, 1fr)", gap: 16, alignItems: "start" });
const panel: CSSProperties = { background: "rgba(15,23,42,0.78)", border: "1px solid rgba(148,163,184,0.18)", borderRadius: 8, padding: 18 };
const sectionTitle: CSSProperties = { margin: "0 0 14px", color: "#f8fafc", fontSize: 20 };
const fieldGrid: CSSProperties = { display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 };
const fieldLabel: CSSProperties = { display: "grid", gap: 7, color: "#cbd5e1", fontSize: 13, marginBottom: 12 };
const input: CSSProperties = { width: "100%", borderRadius: 8, border: "1px solid rgba(148,163,184,0.24)", background: "rgba(2,6,23,0.58)", color: "#f8fafc", padding: "10px 12px" };
const textarea: CSSProperties = { ...input, resize: "vertical", minHeight: 58 };
const optionalText: CSSProperties = { color: "#94a3b8", fontWeight: 700 };
const helperText: CSSProperties = { margin: "8px 0 0", color: "#94a3b8", fontSize: 13 };
const smallButton: CSSProperties = { ...button, width: "auto", padding: "9px 12px" };
const primaryButton = (disabled: boolean): CSSProperties => ({
  ...button,
  width: "auto",
  minWidth: 150,
  opacity: disabled ? 0.52 : 1,
  cursor: disabled ? "not-allowed" : "pointer",
  filter: disabled ? "grayscale(0.28)" : "none",
  boxShadow: disabled ? "none" : button.boxShadow,
});
const secondaryButton: CSSProperties = { ...button, width: "auto", padding: "9px 12px", border: "1px solid rgba(45,212,191,0.34)", background: "rgba(20,184,166,0.13)", color: "#ccfbf1", boxShadow: "none" };
const dangerButton: CSSProperties = { ...button, width: "auto", padding: "9px 12px", border: "1px solid rgba(248,113,113,0.34)", background: "rgba(127,29,29,0.24)", color: "#fecaca", boxShadow: "none" };
const hiddenFileInput: CSSProperties = { position: "absolute", width: 1, height: 1, opacity: 0, pointerEvents: "none" };
const uploadDropzone = (active: boolean, selected: boolean): CSSProperties => ({
  position: "relative",
  display: "grid",
  placeItems: "center",
  minHeight: selected ? 98 : 190,
  borderRadius: 8,
  border: `1px dashed ${active ? "rgba(45,212,191,0.78)" : "rgba(148,163,184,0.34)"}`,
  background: active ? "rgba(20,184,166,0.13)" : "rgba(2,6,23,0.46)",
  color: "#e5e7eb",
  cursor: "pointer",
  outline: "none",
});
const emptyUpload: CSSProperties = { display: "grid", justifyItems: "center", gap: 9, textAlign: "center", padding: 18 };
const uploadIcon: CSSProperties = { display: "grid", placeItems: "center", width: 54, height: 54, borderRadius: 999, border: "1px solid rgba(45,212,191,0.32)", background: "rgba(20,184,166,0.13)", color: "#ccfbf1", fontSize: 12, fontWeight: 900 };
const chooseImageButton: CSSProperties = { padding: "7px 12px", borderRadius: 8, border: "1px solid rgba(45,212,191,0.34)", background: "rgba(20,184,166,0.13)", color: "#ccfbf1", fontWeight: 900 };
const selectedUpload: CSSProperties = { width: "100%", display: "grid", gridTemplateColumns: "72px minmax(0, 1fr) auto", gap: 12, alignItems: "center", padding: 10 };
const uploadThumbnail: CSSProperties = { width: 72, height: 54, objectFit: "cover", borderRadius: 8, border: "1px solid rgba(148,163,184,0.2)", background: "rgba(15,23,42,0.75)" };
const selectedFileDetails: CSSProperties = { minWidth: 0, display: "grid", gap: 4, color: "#94a3b8" };
const selectedFileName: CSSProperties = { color: "#f8fafc", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" };
const uploadActions: CSSProperties = { display: "flex", gap: 8, flexWrap: "wrap", justifyContent: "flex-end" };
const playbookPanel: CSSProperties = { ...panel, display: "grid", gap: 12 };
const playbookHeader: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 12, alignItems: "flex-start" };
const playbookName: CSSProperties = { display: "block", color: "#f8fafc", lineHeight: 1.35 };
const playbookDescription: CSSProperties = { margin: 0, color: "#cbd5e1", lineHeight: 1.55 };
const playbookMeta: CSSProperties = { margin: 0, color: "#94a3b8", fontSize: 13 };
const summaryBlock: CSSProperties = { display: "grid", gap: 7, color: "#cbd5e1", lineHeight: 1.5 };
const summaryItems: CSSProperties = { display: "grid", gap: 5 };
const sequenceFlow: CSSProperties = { display: "flex", flexWrap: "wrap", gap: "7px 8px", alignItems: "center", color: "#e5e7eb" };
const sequenceStepWrap: CSSProperties = { display: "inline-flex", gap: 8, alignItems: "center", whiteSpace: "nowrap" };
const sequenceStep: CSSProperties = { color: "#f8fafc", fontWeight: 800 };
const sequenceArrow: CSSProperties = { color: "#5eead4", fontWeight: 900 };
const editActions: CSSProperties = { display: "flex", gap: 10, flexWrap: "wrap", justifyContent: "flex-end" };
const errorBox: CSSProperties = { ...panel, borderColor: "rgba(248,113,113,0.35)", color: "#fecaca" };
const successBox: CSSProperties = { ...panel, borderColor: "rgba(45,212,191,0.34)", color: "#ccfbf1" };
const compactToolbar: CSSProperties = { ...panel, display: "grid", gridTemplateColumns: "minmax(180px, 0.8fr) minmax(220px, 1fr) auto", gap: 14, alignItems: "center", padding: "14px 16px" };
const compactToolbarMain: CSSProperties = { display: "grid", gap: 4, color: "#94a3b8" };
const compactPlaybookSummary: CSSProperties = { display: "grid", gap: 4, minWidth: 0, color: "#94a3b8" };
const compactToolbarActions: CSSProperties = { display: "flex", gap: 8, flexWrap: "wrap", justifyContent: "flex-end" };
const resultPanel: CSSProperties = { ...panel, padding: 18 };
const resultHeader: CSSProperties = { display: "grid", gridTemplateColumns: "minmax(0, 1.2fr) minmax(300px, 0.8fr)", gap: 18, alignItems: "start", marginBottom: 14, paddingBottom: 14, borderBottom: "1px solid rgba(148,163,184,0.14)" };
const resultMarket: CSSProperties = { color: "#94a3b8", fontSize: 14, fontWeight: 900, textTransform: "uppercase" };
const postureTitle: CSSProperties = { margin: "8px 0 8px", color: "#f8fafc", fontSize: 34, lineHeight: 1.05, fontWeight: 950 };
const postureSummary: CSSProperties = { margin: 0, maxWidth: 860, color: "#cbd5e1", lineHeight: 1.55 };
const resultFacts: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(3, minmax(0, 1fr))", gap: 10 };
const factBlock: CSSProperties = { display: "grid", gap: 4, padding: 12, borderRadius: 8, background: "rgba(2,6,23,0.38)", border: "1px solid rgba(148,163,184,0.14)", color: "#94a3b8" };
const scoreStrip: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(4, minmax(0, 1fr))", gap: 10, marginBottom: 14 };
const scoreTile: CSSProperties = { display: "grid", gap: 6, minHeight: 132, padding: 14, borderRadius: 8, background: "rgba(2,6,23,0.38)", border: "1px solid rgba(148,163,184,0.14)", color: "#cbd5e1", alignContent: "start" };
const scoreLabel: CSSProperties = { color: "#94a3b8", fontSize: 12, fontWeight: 900, textTransform: "uppercase" };
const scoreValue: CSSProperties = { color: "#f8fafc", fontSize: 28, lineHeight: 1 };
const scoreDescriptor: CSSProperties = { color: "#5eead4", fontWeight: 900 };
const scoreReason: CSSProperties = { margin: 0, color: "#cbd5e1", lineHeight: 1.45, fontSize: 13 };
const analysisPair: CSSProperties = { display: "grid", gridTemplateColumns: "minmax(0, 1fr) minmax(320px, 0.9fr)", gap: 14, alignItems: "stretch", marginBottom: 14 };
const resultGrid: CSSProperties = { display: "grid", gridTemplateColumns: "minmax(260px, 0.8fr) minmax(260px, 1.2fr)", gap: 14, marginTop: 14 };
const chartImage: CSSProperties = { width: "100%", borderRadius: 8, border: "1px solid rgba(148,163,184,0.18)" };
const conditionGrid: CSSProperties = { display: "grid", gap: 10 };
const conditionRow: CSSProperties = { display: "grid", gridTemplateColumns: "130px 120px minmax(0, 1fr)", gap: 10, alignItems: "start", padding: 10, borderRadius: 8, background: "rgba(2,6,23,0.36)", color: "#cbd5e1" };
const terminalSection: CSSProperties = { padding: 14, borderRadius: 8, background: "rgba(2,6,23,0.32)", border: "1px solid rgba(148,163,184,0.14)", color: "#cbd5e1" };
const waitingSection: CSSProperties = { ...terminalSection, borderColor: "rgba(45,212,191,0.30)", background: "rgba(20,184,166,0.10)" };
const terminalTitle: CSSProperties = { margin: "0 0 10px", color: "#f8fafc", fontSize: 15, textTransform: "uppercase", letterSpacing: 0 };
const sectionCopy: CSSProperties = { margin: "0 0 10px", color: "#e5e7eb", lineHeight: 1.6 };
const marketReadCopy: CSSProperties = { ...sectionCopy, maxWidth: 900 };
const keyLevelList: CSSProperties = { display: "grid", gap: 8 };
const keyLevelRow: CSSProperties = { display: "grid", gridTemplateColumns: "110px minmax(140px, 1fr) minmax(120px, 0.9fr)", gap: 10, alignItems: "start", padding: 10, borderRadius: 8, background: "rgba(15,23,42,0.56)" };
const levelToggleButton: CSSProperties = { ...secondaryButton, marginTop: 10, padding: "7px 10px" };
const scenarioGrid: CSSProperties = { display: "grid", gridTemplateColumns: "minmax(0, 1.2fr) minmax(280px, 0.8fr)", gap: 12, marginTop: 14 };
const scenarioHeader: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 10, flexWrap: "wrap", alignItems: "flex-start" };
const scenarioStatus: CSSProperties = { ...heroPill, padding: "5px 8px", fontSize: 11 };
const scenarioCard = (tone: "bullish" | "bearish", emphasis: boolean): CSSProperties => ({
  padding: 14,
  gridColumn: emphasis ? "1 / -1" : undefined,
  borderRadius: 8,
  border: `1px solid ${tone === "bullish" ? "rgba(45,212,191,0.22)" : "rgba(248,113,113,0.22)"}`,
  background: emphasis
    ? tone === "bullish" ? "rgba(20,184,166,0.12)" : "rgba(127,29,29,0.15)"
    : tone === "bullish" ? "rgba(20,184,166,0.07)" : "rgba(127,29,29,0.10)",
  color: "#cbd5e1",
});
const noTradeSection: CSSProperties = { ...terminalSection, background: "rgba(15,23,42,0.46)" };
const detailLine: CSSProperties = { display: "flex", gap: 12, justifyContent: "space-between", padding: "10px 0", borderBottom: "1px solid rgba(148,163,184,0.12)", color: "#cbd5e1" };
const detailBlock: CSSProperties = { display: "grid", gap: 8, padding: "12px 0", color: "#cbd5e1" };
const narrative: CSSProperties = { color: "#e5e7eb", lineHeight: 1.65 };
const feedbackBox: CSSProperties = { display: "grid", gap: 10, marginTop: 16, paddingTop: 16, borderTop: "1px solid rgba(148,163,184,0.16)" };
const historyList: CSSProperties = { display: "grid", gap: 8 };
const historyButton = (active: boolean): CSSProperties => ({ display: "grid", gridTemplateColumns: "1.2fr 0.8fr 1fr 0.8fr", gap: 10, textAlign: "left", color: "#cbd5e1", background: active ? "rgba(20,184,166,0.12)" : "rgba(2,6,23,0.32)", border: "1px solid rgba(148,163,184,0.16)", borderRadius: 8, padding: 12, cursor: "pointer" });
const muted: CSSProperties = { color: "#94a3b8" };
