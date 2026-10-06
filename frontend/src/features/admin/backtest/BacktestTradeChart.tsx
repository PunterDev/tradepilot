import { useEffect, useMemo, useRef, useState, type CSSProperties } from "react";
import { getBacktestTradeChart, type BacktestChartCandle, type BacktestChartTimeframe, type BacktestTrade, type BacktestTradeChart } from "../../../api/backtestApi";
import { button, card, mutedText, sectionTitle } from "../../../styles/dashboardStyles";
import ManualPositionMetrics from "./ManualPositionMetrics";
import ManualPositionToolbar from "./ManualPositionToolbar";
import { calculateManualPosition, snapToTick, targetForR, tickSizeFor, type ManualPosition } from "./manualPositionCalculator";
import BacktestCandlestickChart, { type BacktestChartViewportHandle } from "./BacktestCandlestickChart";

type Props = { runId: number; tradeId: number; chartMode: "EXPLORE" | "REVIEW"; reviewTrades: BacktestTrade[]; onNavigate: (trade: BacktestTrade) => void; onClose: () => void };
type Selected = { candle?: BacktestChartCandle; overlay?: string };
type ManualLevel = "entry" | "stop" | "target";

export default function BacktestTradeChart({ runId, tradeId, chartMode, reviewTrades, onNavigate, onClose }: Props) {
  const [timeframe, setTimeframe] = useState<BacktestChartTimeframe>("1m");
  const [chart, setChart] = useState<BacktestTradeChart | null>(null);
  const [chartCache, setChartCache] = useState<Record<string, BacktestTradeChart>>({});
  const [error, setError] = useState("");
  const [selected, setSelected] = useState<Selected>({});
  const [manual, setManual] = useState<ManualPosition | null>(null);
  const [maximized, setMaximized] = useState(false);
  const viewportRef = useRef<BacktestChartViewportHandle>(null);
  const workspaceRef = useRef<HTMLElement>(null);
  const scrollPositionRef = useRef(0);
  const [showZones, setShowZones] = useState(true), [showLevels, setShowLevels] = useState(true), [showSwings, setShowSwings] = useState(true), [showManual, setShowManual] = useState(true);
  const tickSize = tickSizeFor(chart?.instrument ?? "");
  const calculation = useMemo(() => calculateManualPosition(manual, tickSize), [manual, tickSize]);
  const cacheKey = `${runId}:${tradeId}:${timeframe}`;
  useEffect(() => {
    let current = true;
    const cached = chartCache[cacheKey];
    setError(""); setSelected({});
    if (cached) { setChart(cached); return () => { current = false; }; }
    setChart(null);
    void getBacktestTradeChart(runId, tradeId, timeframe)
      .then(data => { if (!current) return; setChartCache(cache => ({ ...cache, [cacheKey]: data })); setChart(data); })
      .catch(() => current && setError("Unable to load trade chart."));
    return () => { current = false; };
  }, [runId, tradeId, timeframe, cacheKey, chartCache]);
  const updateManual = (field: ManualLevel, value: number) => { if (!tickSize || !Number.isFinite(value)) return; setManual(current => current ? { ...current, [field]: snapToTick(value, tickSize) ?? current[field] } : current); };
  const choosePosition = (direction: "LONG" | "SHORT") => chart && setManual(initialPosition(chart, direction, selected.candle?.close));
  const loadBacktest = () => chart && setManual({ direction: chart.facts.direction === "SHORT" ? "SHORT" : "LONG", entry: n(chart.facts.entryPrice), stop: n(chart.facts.stopPrice), target: n(chart.facts.tp1Price) });
  const updateTargetR = (value: number) => { const target = targetForR(manual, value, tickSize); if (target != null) updateManual("target", target); };
  const queueIndex = reviewTrades.findIndex(trade => trade.id === tradeId);
  const reviewMode = chartMode === "REVIEW";
  useEffect(() => {
    if (!maximized) return;
    scrollPositionRef.current = window.scrollY;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const onKeyDown = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null;
      if (!workspaceRef.current?.contains(document.activeElement)) return;
      if (event.key === "Escape") { setMaximized(false); return; }
      if (target?.matches("input, select, textarea, [contenteditable=true]")) return;
      if (event.key.toLowerCase() === "f") viewportRef.current?.fit();
      if (event.key.toLowerCase() === "r") viewportRef.current?.reset();
    };
    window.addEventListener("keydown", onKeyDown);
    return () => { document.body.style.overflow = previousOverflow; window.removeEventListener("keydown", onKeyDown); window.scrollTo(0, scrollPositionRef.current); };
  }, [maximized]);
  return <section ref={workspaceRef} tabIndex={-1} style={{ ...card, ...(maximized ? maximizedCard : normalCard) }} aria-label="Historical Trade Chart" data-maximized={maximized}><div style={headingRow}><div><p style={chartEyebrow}>{reviewMode ? "Chart review" : "Trade chart"}</p>{reviewMode && <p style={modeIndicator}>Manual Review</p>}<h2 style={sectionTitle}>{chart ? `${chart.instrument} · ${new Intl.DateTimeFormat("en-US", { timeZone: chart.timezone, month: "short", day: "numeric", year: "numeric" }).format(new Date(chart.facts.entryTime))} · ${chart.facts.direction}` : "Loading trade chart..."}</h2><p style={mutedText}>{chart ? `Entry ${time(chart.facts.entryTime, chart.timezone)} · Engine: ${fmt(chart.facts.entryPrice)} / Stop ${fmt(chart.facts.stopPrice)} / TP1 ${fmt(chart.facts.tp1R)}R / Runner ${fmt(chart.facts.runnerR)}R` : "Loading the bounded stored-candle review."}</p></div><div style={controls}>{reviewMode && <><button style={smallButton(queueIndex > 0)} disabled={queueIndex <= 0} onClick={() => onNavigate(reviewTrades[queueIndex - 1])}>Previous Trade</button><button style={smallButton(queueIndex >= 0 && queueIndex < reviewTrades.length - 1)} disabled={queueIndex < 0 || queueIndex >= reviewTrades.length - 1} onClick={() => onNavigate(reviewTrades[queueIndex + 1])}>Next Trade</button></>}<button title="Fit chart (F)" aria-label="Fit chart" style={smallButton(true)} onClick={() => viewportRef.current?.fit()}>Fit</button><button title="Reset chart view (R)" aria-label="Reset chart view" style={smallButton(true)} onClick={() => viewportRef.current?.reset()}>Reset View</button><button title={maximized ? "Restore chart (Esc)" : "Maximize chart"} aria-label={maximized ? "Restore chart" : "Maximize chart"} style={smallButton(true)} onClick={() => setMaximized(value => { if (!value) requestAnimationFrame(() => workspaceRef.current?.focus()); return !value; })}>{maximized ? "Restore" : "Maximize"}</button><button style={smallButton(true)} onClick={onClose}>Close Chart</button></div></div>
    <div style={controls} aria-label="Timeframe controls">{(["1m", "5m", "1h", "4h"] as BacktestChartTimeframe[]).map(value => <button key={value} style={smallButton(timeframe === value)} onClick={() => setTimeframe(value)}>{timeframeLabel(value)}</button>)}</div>
    {error ? <div style={errorPanel}><p style={errorText}>{error}</p><button style={smallButton(true)} onClick={() => setChartCache(cache => { const next = { ...cache }; delete next[cacheKey]; return next; })}>Retry</button></div> : !chart ? <div style={loadingPanel}>Loading trade chart...</div> : <><div style={{ ...layout, gridTemplateColumns: maximized ? "minmax(0,1fr) minmax(220px,300px)" : layout.gridTemplateColumns }}><div style={chartPanel}><BacktestCandlestickChart ref={viewportRef} chart={chart} showZones={showZones} showLevels={showLevels} showSwings={showSwings} showManual={showManual} manual={manual} onSelect={setSelected} onManualChange={updateManual} maximized={maximized}/><div style={overlayLegend}>{showZones && chart.overlays.setupZone && <span>{chart.overlays.setupZone.label}</span>}{showZones && chart.timeframe === "1m" && chart.overlays.confirmationZone && <span>{chart.overlays.confirmationZone.label}</span>}<span>Trade Entry</span></div><Selection selected={selected} timezone={chart.timezone} timeframe={chart.timeframe} /></div><div style={sidePanels}><Facts chart={chart} /><ManualPositionMetrics position={manual} calculation={calculation} chart={chart} /></div></div><div style={controls} aria-label="Chart overlays"><button style={smallButton(showZones)} onClick={() => setShowZones(value => !value)}>Setup zones</button><button style={smallButton(showLevels)} onClick={() => setShowLevels(value => !value)}>Engine levels</button><button style={smallButton(showSwings)} onClick={() => setShowSwings(value => !value)}>Swings</button><button style={smallButton(showManual)} onClick={() => setShowManual(value => !value)}>Manual Position</button></div><ManualPositionToolbar position={manual} onLong={() => choosePosition("LONG")} onShort={() => choosePosition("SHORT")} onLoadBacktest={loadBacktest} onReset={() => setManual(null)} onChange={updateManual} onTargetR={updateTargetR} /></>}
  </section>;
}

function Facts({ chart }: { chart: BacktestTradeChart }) { const f=chart.facts; const rows:Array<[string,string]>=[["Direction",f.direction],["Entry",`${fmt(f.entryPrice)} · ${time(f.entryTime,chart.timezone)}`],["Stop",fmt(f.stopPrice)],["Risk points",fmt(f.riskPoints)],["TP1",`${fmt(f.tp1Price)}${f.tp1R != null ? ` · ${fmt(f.tp1R)}R` : ""}`],["Runner",`${fmt(f.runnerPrice)}${f.runnerR != null ? ` · ${fmt(f.runnerR)}R` : ""}`],["Runner objective",f.runnerObjectiveType ?? "-"],["Model A",`${fmt(f.modelAR)}R`],["Model B",`${fmt(f.modelBR)}R`],["Instrument ID",String(f.instrumentId)],["Ambiguous intrabar",f.ambiguousIntrabar ? "Yes" : "No"]]; return <aside style={factsPanel}><h3 style={{...sectionTitle,fontSize:15}}>Backtest Result</h3>{rows.map(([label,value])=><div key={label} style={fact}><span>{label}</span><strong>{value}</strong></div>)}</aside>; }
function Selection({ selected, timezone, timeframe }: { selected: Selected; timezone: string; timeframe: BacktestChartTimeframe }) { if (selected.overlay) return <p style={selection}>{selected.overlay}</p>; if (!selected.candle) return <p style={selection}>Hover or click a candle for OHLCV. Drag a manual level or use the price inputs for local analysis.</p>; const c=selected.candle; return <p style={selection}>{candleInterval(c.timestamp, timeframe, timezone)} · O {fmt(c.open)} · H {fmt(c.high)} · L {fmt(c.low)} · C {fmt(c.close)} · V {fmt(c.volume)}</p>; }
function initialPosition(chart: BacktestTradeChart, direction: "LONG" | "SHORT", selectedClose?: number | string) { const entry=n(selectedClose ?? chart.facts.entryPrice), risk=Math.abs(n(chart.facts.entryPrice)-n(chart.facts.stopPrice)), same=chart.facts.direction===direction; return { direction, entry, stop:same?n(chart.facts.stopPrice):(direction==="LONG"?entry-risk:entry+risk), target:same?n(chart.facts.tp1Price):(direction==="LONG"?entry+risk:entry-risk) }; }
function n(value:number|string|null|undefined){const parsed=Number(value);return Number.isFinite(parsed)?parsed:0;} function fmt(value:number|string|null|undefined){return value==null?"-":n(value).toFixed(3).replace(/\.000$/,"");} function time(value:string|null|undefined, timezone:string){if(!value)return "-";return new Intl.DateTimeFormat("en-US",{timeZone:timezone,month:"short",day:"numeric",hour:"numeric",minute:"2-digit"}).format(new Date(value));} function timeframeLabel(value:BacktestChartTimeframe){return value==="1h"?"1H":value==="4h"?"4H":value;} function candleInterval(value:string,timeframe:BacktestChartTimeframe,timezone:string){if(timeframe==="1m"||timeframe==="5m")return time(value,timezone);const start=new Date(value),end=new Date(start.getTime()+(timeframe==="1h"?60:240)*60_000),format=new Intl.DateTimeFormat("en-US",{timeZone:timezone,month:"short",day:"numeric",hour:"numeric",minute:"2-digit"});return `${format.format(start)}–${new Intl.DateTimeFormat("en-US",{timeZone:timezone,hour:"numeric",minute:"2-digit"}).format(end)} ET`; }
const headingRow:CSSProperties={display:"flex",justifyContent:"space-between",gap:12,alignItems:"center",flexWrap:"wrap"};const chartEyebrow:CSSProperties={margin:"0 0 4px",color:"#2dd4bf",fontSize:11,fontWeight:900,textTransform:"uppercase",letterSpacing:".1em"};const modeIndicator:CSSProperties={margin:"0 0 6px",color:"#bae6fd",fontSize:12,fontWeight:800};const controls:CSSProperties={display:"flex",gap:8,flexWrap:"wrap",margin:"10px 0 14px"};const layout:CSSProperties={display:"grid",gridTemplateColumns:"repeat(auto-fit,minmax(min(100%,280px),1fr))",gap:16};const chartPanel:CSSProperties={minWidth:0};const sidePanels:CSSProperties={display:"grid",alignContent:"start",gap:12};const overlayLegend:CSSProperties={display:"flex",gap:8,flexWrap:"wrap",marginTop:8,fontSize:11,color:"#a5b4fc"};const loadingPanel:CSSProperties={minHeight:260,display:"grid",placeItems:"center",border:"1px dashed rgba(148,163,184,.28)",borderRadius:10,color:"#cbd5e1"};const errorPanel:CSSProperties={display:"flex",alignItems:"center",gap:10,flexWrap:"wrap",minHeight:100,padding:14,border:"1px solid rgba(248,113,113,.35)",borderRadius:10};const factsPanel:CSSProperties={display:"grid",alignContent:"start",gap:8,padding:12,border:"1px solid rgba(148,163,184,.18)",borderRadius:10,background:"rgba(15,23,42,.4)"};const fact:CSSProperties={display:"grid",gap:2,fontSize:13,color:"#94a3b8"};const selection:CSSProperties={margin:"10px 0 0",padding:10,borderRadius:8,background:"rgba(15,23,42,.55)",color:"#cbd5e1",fontSize:13};const errorText:CSSProperties={color:"#fecaca"};const smallButton=(active:boolean):CSSProperties=>({...button,boxShadow:"none",padding:"6px 9px",opacity:active?1:.62});const normalCard:CSSProperties={borderRadius:10,padding:22,boxShadow:"0 14px 36px rgba(0,0,0,.16)"};const maximizedCard:CSSProperties={position:"fixed",inset:12,zIndex:1000,overflow:"auto",borderRadius:10,padding:22,background:"#020617",boxShadow:"0 24px 70px rgba(0,0,0,.5)"};
