import { useEffect, useMemo, useState, type CSSProperties } from "react";
import {
  downloadMarketData,
  estimateMarketData,
  getMarketDataCoverage,
  type MarketDataCoverage,
  type MarketDataEstimate,
  type MarketDataRequest,
  type MarketInstrument,
} from "../../../api/marketDataApi";
import PageHeader from "../../../layout/PageHeader";
import { button, card, mutedText, sectionTitle } from "../../../styles/dashboardStyles";

function localDateOnly(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function defaultStartDate() {
  const date = new Date();
  date.setDate(date.getDate() - 30);
  return localDateOnly(date);
}

function requestSignature(request: MarketDataRequest) {
  return `${request.instrument}:${request.startDate}:${request.endDate}`;
}

function formatInstant(value?: string | null) {
  if (!value) return "No data";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString(undefined, { year: "numeric", month: "short", day: "numeric", timeZoneName: "short" });
}

function friendlyError(error: unknown, fallback: string) {
  const message = (error as { response?: { data?: { userMessage?: string } } })?.response?.data?.userMessage;
  return message || fallback;
}

export default function MarketDataPage() {
  const [instrument, setInstrument] = useState<MarketInstrument>("NQ");
  const [startDate, setStartDate] = useState(defaultStartDate());
  const [endDate, setEndDate] = useState(localDateOnly(new Date()));
  const [coverage, setCoverage] = useState<MarketDataCoverage[]>([]);
  const [estimate, setEstimate] = useState<MarketDataEstimate | null>(null);
  const [estimatedSignature, setEstimatedSignature] = useState("");
  const [loadingCoverage, setLoadingCoverage] = useState(true);
  const [estimating, setEstimating] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const request = useMemo<MarketDataRequest>(() => ({ instrument, startDate, endDate }), [instrument, startDate, endDate]);
  const currentSignature = requestSignature(request);
  const selectedCoverage = coverage.find((item) => item.instrument === instrument);
  const estimateCurrent = estimatedSignature === currentSignature && estimate !== null;

  async function loadCoverage() {
    setLoadingCoverage(true);
    try {
      setCoverage(await getMarketDataCoverage());
    } catch (loadError) {
      setError(friendlyError(loadError, "Unable to load stored market-data coverage."));
    } finally {
      setLoadingCoverage(false);
    }
  }

  useEffect(() => {
    void loadCoverage();
  }, []);

  function updateRequest(next: Partial<MarketDataRequest>) {
    if (next.instrument) setInstrument(next.instrument);
    if (next.startDate !== undefined) setStartDate(next.startDate);
    if (next.endDate !== undefined) setEndDate(next.endDate);
    setEstimate(null);
    setEstimatedSignature("");
    setNotice("");
    setError("");
  }

  async function handleEstimate() {
    if (!startDate || !endDate || endDate < startDate) {
      setError("Choose an end date on or after the start date.");
      return;
    }
    setEstimating(true);
    setError("");
    setNotice("");
    try {
      const next = await estimateMarketData(request);
      setEstimate(next);
      setEstimatedSignature(currentSignature);
    } catch (estimateError) {
      setEstimate(null);
      setEstimatedSignature("");
      setError(friendlyError(estimateError, "Unable to estimate Databento cost. Check the server logs for details."));
    } finally {
      setEstimating(false);
    }
  }

  async function handleDownload() {
    if (!estimateCurrent) return;
    setConfirmOpen(false);
    setDownloading(true);
    setError("");
    setNotice("");
    try {
      const result = await downloadMarketData(request);
      setNotice(`Historical data import completed. ${number(result.insertedBars)} bars inserted; ${number(result.skippedExistingBars)} existing bars skipped.`);
      await loadCoverage();
    } catch (downloadError) {
      setError(friendlyError(downloadError, "Historical data download could not be completed."));
    } finally {
      setDownloading(false);
    }
  }

  function updateMissingData() {
    if (!selectedCoverage?.lastBar) return;
    const nextStart = new Date(selectedCoverage.lastBar);
    nextStart.setUTCDate(nextStart.getUTCDate() + 1);
    updateRequest({ startDate: nextStart.toISOString().slice(0, 10), endDate: localDateOnly(new Date()) });
  }

  return (
    <div style={pageWrap}>
      <PageHeader
        label="Admin Market Data"
        title="Market Data"
        description="Download and retain canonical 1-minute futures OHLCV history for future internal research."
        actions={<span style={adminBadge}>Admin only</span>}
      />

      {error && <div style={errorBox}>{error}</div>}
      {notice && <div style={successBox}>{notice}</div>}

      <section style={mainGrid}>
        <article style={card}>
          <h2 style={sectionTitle}>Historical Download</h2>
          <p style={mutedText}>Data Source: Databento · Dataset: GLBX.MDP3 · Schema: 1-minute OHLCV</p>
          <div style={formGrid}>
            <label style={fieldLabel}>Instrument
              <select aria-label="Instrument" value={instrument} onChange={(event) => updateRequest({ instrument: event.target.value as MarketInstrument })} style={inputStyle} disabled={downloading}>
                <option value="NQ">NQ</option>
                <option value="ES">ES</option>
              </select>
            </label>
            <label style={fieldLabel}>Start date
              <input aria-label="Start date" type="date" value={startDate} max={endDate} onChange={(event) => updateRequest({ startDate: event.target.value })} style={inputStyle} disabled={downloading} />
            </label>
            <label style={fieldLabel}>End date
              <input aria-label="End date" type="date" value={endDate} min={startDate} onChange={(event) => updateRequest({ endDate: event.target.value })} style={inputStyle} disabled={downloading} />
            </label>
          </div>
          <div style={sourceGrid}>
            <Info label="Continuous contract" value={instrument === "NQ" ? "NQ.v.0" : "ES.v.0"} />
            <Info label="Storage timeframe" value="1m" />
            <Info label="Rollover handling" value="Databento instrument_id preserved" />
          </div>
          {estimateCurrent && estimate && (
            <div style={estimateBox}>
              <strong>Estimated cost: ${Number(estimate.estimatedCostUsd).toFixed(2)}</strong>
              <span>{estimate.requestedDays} requested days · {estimate.missingChunks} missing monthly chunks</span>
            </div>
          )}
          <div style={actionRow}>
            <button type="button" onClick={() => void handleEstimate()} disabled={estimating || downloading} style={secondaryButton(estimating || downloading)}>
              {estimating ? "Estimating..." : "Estimate Cost"}
            </button>
            <button type="button" onClick={() => setConfirmOpen(true)} disabled={!estimateCurrent || downloading} style={primaryButton(!estimateCurrent || downloading)}>
              {downloading ? "Downloading..." : "Download Historical Data"}
            </button>
          </div>
          {!estimateCurrent && <p style={helperText}>Estimate the current instrument and date range before requesting historical data.</p>}
        </article>

        <article style={card}>
          <div style={coverageHeader}>
            <div>
              <h2 style={sectionTitle}>Stored Data Coverage</h2>
              <p style={mutedText}>Completed monthly imports are skipped on a matching rerun. A failed month is safely retried with duplicate bars ignored.</p>
            </div>
            <button type="button" onClick={() => void loadCoverage()} disabled={loadingCoverage || downloading} style={refreshButton}>Refresh</button>
          </div>
          <div style={coverageList}>
            {(["NQ", "ES"] as MarketInstrument[]).map((item) => {
              const row = coverage.find((entry) => entry.instrument === item);
              return (
                <section key={item} style={coverageCard}>
                  <div><strong style={coverageSymbol}>{item}</strong><span style={coverageContract}>{item}.v.0 · 1m</span></div>
                  <div style={coverageMetric}><span>Stored coverage</span><strong>{row?.firstBar ? `${formatInstant(row.firstBar)} to ${formatInstant(row.lastBar)}` : "No data"}</strong></div>
                  <div style={coverageMetric}><span>Bars</span><strong>{number(row?.barCount ?? 0)}</strong></div>
                </section>
              );
            })}
          </div>
          {selectedCoverage?.lastBar && (
            <button type="button" onClick={updateMissingData} disabled={downloading} style={secondaryButton(downloading)}>
              Update Missing Data
            </button>
          )}
        </article>
      </section>

      {confirmOpen && estimate && (
        <div style={overlay} role="presentation" onMouseDown={() => !downloading && setConfirmOpen(false)}>
          <section style={modal} role="dialog" aria-modal="true" aria-labelledby="market-data-confirm-title" onMouseDown={(event) => event.stopPropagation()}>
            <h2 id="market-data-confirm-title" style={modalTitle}>Confirm Historical Download</h2>
            <p style={modalText}>You are about to request historical Databento data with an estimated cost of ${Number(estimate.estimatedCostUsd).toFixed(2)}. Continue?</p>
            <div style={actionRow}>
              <button type="button" onClick={() => setConfirmOpen(false)} disabled={downloading} style={secondaryButton(downloading)}>Cancel</button>
              <button type="button" onClick={() => void handleDownload()} disabled={downloading} style={primaryButton(downloading)}>Download Data</button>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return <div style={infoTile}><span>{label}</span><strong>{value}</strong></div>;
}

function number(value: number) { return new Intl.NumberFormat().format(value); }

const pageWrap: CSSProperties = { display: "grid", gap: 18 };
const mainGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 320px), 1fr))", gap: 18, alignItems: "start" };
const formGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 160px), 1fr))", gap: 12, marginTop: 18 };
const fieldLabel: CSSProperties = { display: "grid", gap: 6, fontSize: 12, color: "#cbd5e1", fontWeight: 700 };
const inputStyle: CSSProperties = { minHeight: 40, borderRadius: 6, border: "1px solid rgba(148,163,184,0.32)", background: "rgba(15,23,42,0.78)", color: "#f8fafc", padding: "8px 10px", font: "inherit" };
const sourceGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 150px), 1fr))", gap: 10, marginTop: 16 };
const infoTile: CSSProperties = { display: "grid", gap: 4, minWidth: 0, padding: 10, border: "1px solid rgba(148,163,184,0.18)", background: "rgba(15,23,42,0.38)", borderRadius: 6, color: "#cbd5e1", fontSize: 12 };
const estimateBox: CSSProperties = { display: "grid", gap: 4, marginTop: 16, padding: 12, border: "1px solid rgba(45,212,191,0.4)", borderRadius: 6, background: "rgba(20,184,166,0.1)", color: "#ccfbf1" };
const actionRow: CSSProperties = { display: "flex", gap: 10, flexWrap: "wrap", alignItems: "center", marginTop: 16 };
const secondaryButton = (disabled: boolean): CSSProperties => ({ ...button, width: "auto", padding: "9px 13px", background: "rgba(15,23,42,0.72)", border: "1px solid rgba(148,163,184,0.35)", boxShadow: "none", opacity: disabled ? 0.52 : 1, cursor: disabled ? "not-allowed" : "pointer" });
const primaryButton = (disabled: boolean): CSSProperties => ({ ...button, width: "auto", padding: "9px 13px", opacity: disabled ? 0.52 : 1, cursor: disabled ? "not-allowed" : "pointer", boxShadow: disabled ? "none" : undefined });
const helperText: CSSProperties = { ...mutedText, margin: "12px 0 0", fontSize: 12 };
const errorBox: CSSProperties = { padding: 12, borderRadius: 6, border: "1px solid rgba(248,113,113,0.5)", background: "rgba(127,29,29,0.28)", color: "#fecaca" };
const successBox: CSSProperties = { padding: 12, borderRadius: 6, border: "1px solid rgba(52,211,153,0.45)", background: "rgba(6,78,59,0.28)", color: "#bbf7d0" };
const adminBadge: CSSProperties = { padding: "6px 9px", border: "1px solid rgba(52,211,153,0.35)", background: "rgba(16,185,129,0.12)", borderRadius: 999, color: "#bbf7d0", fontSize: 12, fontWeight: 800 };
const coverageHeader: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 12, alignItems: "start" };
const refreshButton: CSSProperties = { ...button, width: "auto", padding: "7px 10px", boxShadow: "none", fontSize: 12 };
const coverageList: CSSProperties = { display: "grid", gap: 10, marginTop: 16, marginBottom: 16 };
const coverageCard: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 120px), 1fr))", alignItems: "center", gap: 12, padding: 12, borderRadius: 6, border: "1px solid rgba(148,163,184,0.18)", background: "rgba(15,23,42,0.4)" };
const coverageSymbol: CSSProperties = { display: "block", color: "#f8fafc", fontSize: 18 };
const coverageContract: CSSProperties = { color: "#94a3b8", fontSize: 12 };
const coverageMetric: CSSProperties = { display: "grid", gap: 3, color: "#94a3b8", fontSize: 12 };
const overlay: CSSProperties = { position: "fixed", inset: 0, zIndex: 50, display: "grid", placeItems: "center", padding: 20, background: "rgba(2,6,23,0.78)" };
const modal: CSSProperties = { width: "min(100%, 480px)", padding: 22, borderRadius: 8, background: "#0f172a", border: "1px solid rgba(45,212,191,0.35)", color: "#f8fafc", boxShadow: "0 24px 70px rgba(0,0,0,0.45)" };
const modalTitle: CSSProperties = { margin: 0, fontSize: 20 };
const modalText: CSSProperties = { margin: "12px 0 0", color: "#cbd5e1", lineHeight: 1.55 };
