import type { CSSProperties } from "react";
import type { ManualPosition } from "./manualPositionCalculator";

type Props = { position: ManualPosition | null; onLong: () => void; onShort: () => void; onLoadBacktest: () => void; onReset: () => void; onChange: (field: "entry" | "stop" | "target", value: number) => void; onTargetR: (value: number) => void };

export default function ManualPositionToolbar({ position, onLong, onShort, onLoadBacktest, onReset, onChange, onTargetR }: Props) {
  return <div style={toolbar} aria-label="Manual position controls"><div style={buttons}><button style={toolButton(position?.direction === "LONG")} onClick={onLong}>Long Position</button><button style={toolButton(position?.direction === "SHORT")} onClick={onShort}>Short Position</button><button style={toolButton(false)} onClick={onLoadBacktest}>Load Backtest Levels</button><button style={toolButton(false)} onClick={onReset} disabled={!position}>Reset Manual Position</button></div>
    {position && <div style={inputs}><label>Manual entry price<input aria-label="Manual entry price" type="number" step="0.25" value={position.entry} onChange={event => onChange("entry", Number(event.target.value))} /></label><label>Manual stop price<input aria-label="Manual stop price" type="number" step="0.25" value={position.stop} onChange={event => onChange("stop", Number(event.target.value))} /></label><label>Manual target price<input aria-label="Manual target price" type="number" step="0.25" value={position.target} onChange={event => onChange("target", Number(event.target.value))} /></label><label>Manual target R<input aria-label="Manual target R" type="number" min="0" step="0.25" onChange={event => onTargetR(Number(event.target.value))} /></label></div>}
  </div>;
}

const toolbar: CSSProperties = { display: "grid", gap: 10, margin: "12px 0" };
const buttons: CSSProperties = { display: "flex", gap: 8, flexWrap: "wrap" };
const inputs: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(150px,1fr))", gap: 8 };
const toolButton = (active: boolean): CSSProperties => ({ border: `1px solid ${active ? "rgba(45,212,191,.8)" : "rgba(148,163,184,.3)"}`, borderRadius: 8, padding: "7px 10px", background: active ? "rgba(13,148,136,.22)" : "rgba(15,23,42,.7)", color: "#e2e8f0", fontWeight: 800, cursor: "pointer" });
