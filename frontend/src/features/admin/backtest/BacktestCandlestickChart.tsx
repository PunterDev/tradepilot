import { forwardRef, useEffect, useImperativeHandle, useLayoutEffect, useRef, useState } from "react";
import { CandlestickSeries, ColorType, CrosshairMode, LineStyle, createChart, createSeriesMarkers, type IChartApi, type IPriceLine, type ISeriesApi, type ISeriesMarkersPluginApi, type LogicalRange, type Time } from "lightweight-charts";
import type { BacktestChartCandle, BacktestChartTimeframe, BacktestTradeChart } from "../../../api/backtestApi";
import type { ManualPosition } from "./manualPositionCalculator";

type Selected = { candle?: BacktestChartCandle; overlay?: string };
type ManualLevel = "entry" | "stop" | "target";

export type BacktestChartViewportHandle = { fit: () => void; reset: () => void };

type Props = {
  chart: BacktestTradeChart;
  showZones: boolean;
  showLevels: boolean;
  showSwings: boolean;
  showManual: boolean;
  manual: ManualPosition | null;
  onSelect: (value: Selected) => void;
  onManualChange: (level: ManualLevel, value: number) => void;
  maximized: boolean;
  /** A stable identity for charts whose facts can change as replay progresses. */
  viewportId?: string;
};

const BacktestCandlestickChart = forwardRef<BacktestChartViewportHandle, Props>(function BacktestCandlestickChart(props, ref) {
  const hostRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const seriesRef = useRef<ISeriesApi<"Candlestick", Time> | null>(null);
  const markersRef = useRef<ISeriesMarkersPluginApi<Time> | null>(null);
  const priceLinesRef = useRef<IPriceLine[]>([]);
  const viewportsRef = useRef<Record<string, LogicalRange>>({});
  const followingEdgeRef = useRef<Record<string, boolean>>({});
  const currentKeyRef = useRef("");
  const propsRef = useRef(props);
  const dragRef = useRef<ManualLevel | null>(null);
  const [handleY, setHandleY] = useState<Partial<Record<ManualLevel, number>>>({});

  propsRef.current = props;
  const viewportKey = props.viewportId ?? `${props.chart.instrumentId}:${props.chart.facts.entryTime}:${props.chart.timeframe}`;
  currentKeyRef.current = viewportKey;

  const syncHandles = () => {
    const series = seriesRef.current;
    const manual = propsRef.current.manual;
    if (!series || !manual || !propsRef.current.showManual) { setHandleY({}); return; }
    setHandleY({ entry: series.priceToCoordinate(manual.entry) ?? undefined, stop: series.priceToCoordinate(manual.stop) ?? undefined, target: series.priceToCoordinate(manual.target) ?? undefined });
  };

  useLayoutEffect(() => {
    const host = hostRef.current;
    if (!host) return;
    const chart = createChart(host, {
      width: host.clientWidth,
      height: host.clientHeight,
      layout: { background: { type: ColorType.Solid, color: "rgba(2,6,23,.48)" }, textColor: "#94a3b8", fontFamily: "Inter, ui-sans-serif, system-ui" },
      grid: { vertLines: { color: "rgba(148,163,184,.12)" }, horzLines: { color: "rgba(148,163,184,.12)" } },
      crosshair: { mode: CrosshairMode.Normal, vertLine: { color: "rgba(226,232,240,.55)", labelBackgroundColor: "#0f172a" }, horzLine: { color: "rgba(226,232,240,.55)", labelBackgroundColor: "#0f172a" } },
      rightPriceScale: { borderColor: "rgba(148,163,184,.25)", autoScale: true },
      timeScale: { borderColor: "rgba(148,163,184,.25)", timeVisible: true, secondsVisible: false, rightOffset: 3 },
      handleScroll: { mouseWheel: true, pressedMouseMove: true, horzTouchDrag: true, vertTouchDrag: false },
      handleScale: { mouseWheel: true, pinch: true, axisPressedMouseMove: true },
    });
    const series = chart.addSeries(CandlestickSeries, { upColor: "#22c55e", downColor: "#f43f5e", borderUpColor: "#22c55e", borderDownColor: "#f43f5e", wickUpColor: "#6ee7b7", wickDownColor: "#fda4af", priceFormat: { type: "price", precision: 2, minMove: 0.25 } });
    chartRef.current = chart; seriesRef.current = series; markersRef.current = createSeriesMarkers(series, []);
    let frame = 0;
    const crosshair = (event: Parameters<IChartApi["subscribeCrosshairMove"]>[0] extends (value: infer Value) => void ? Value : never) => {
      const candle = event.seriesData.get(series) as { time?: Time } | undefined;
      if (!candle?.time) return;
      const match = propsRef.current.chart.bars.find(bar => epoch(bar.timestamp) === candle.time);
      if (!match) return;
      cancelAnimationFrame(frame);
      frame = requestAnimationFrame(() => propsRef.current.onSelect({ candle: match }));
    };
    const range = (value: LogicalRange | null) => {
      if (value) {
        viewportsRef.current[currentKeyRef.current] = value;
        const newest = propsRef.current.chart.bars.length - 1;
        followingEdgeRef.current[currentKeyRef.current] = value.to >= newest - 1;
      }
      requestAnimationFrame(syncHandles);
    };
    chart.subscribeCrosshairMove(crosshair); chart.timeScale().subscribeVisibleLogicalRangeChange(range);
    const observer = new ResizeObserver(() => { chart.applyOptions({ width: host.clientWidth, height: host.clientHeight }); requestAnimationFrame(syncHandles); });
    observer.observe(host);
    return () => { cancelAnimationFrame(frame); observer.disconnect(); chart.unsubscribeCrosshairMove(crosshair); chart.timeScale().unsubscribeVisibleLogicalRangeChange(range); chart.remove(); chartRef.current = null; seriesRef.current = null; };
  }, []);

  const fit = () => { const chart = chartRef.current; if (!chart) return; delete viewportsRef.current[currentKeyRef.current]; chart.timeScale().fitContent(); chart.priceScale("right").applyOptions({ autoScale: true }); requestAnimationFrame(syncHandles); };
  useImperativeHandle(ref, () => ({ fit, reset: fit }), []);

  useEffect(() => {
    const chart = chartRef.current, series = seriesRef.current;
    if (!chart || !series) return;
    const bars = props.chart.bars.map(bar => ({ time: epoch(bar.timestamp), open: number(bar.open), high: number(bar.high), low: number(bar.low), close: number(bar.close) }));
    series.setData(bars);
    for (const line of priceLinesRef.current) series.removePriceLine(line);
    priceLinesRef.current = priceLines(props).map(options => series.createPriceLine(options));
    const markers = [{ time: epoch(props.chart.facts.entryTime), position: "inBar" as const, color: "#f8fafc", shape: "circle" as const, text: "Trade Entry" }];
    if (props.chart.facts.exitTime && props.chart.facts.exitPrice != null) markers.push({ time: epoch(props.chart.facts.exitTime), position: "inBar" as const, color: "#fbbf24", shape: "circle" as const, text: props.chart.facts.exitLabel ?? "Trade Exit" });
    markersRef.current?.setMarkers(markers);
    const saved = viewportsRef.current[viewportKey];
    const followsReplayEdge = followingEdgeRef.current[viewportKey] !== false;
    if (saved && !followsReplayEdge) chart.timeScale().setVisibleLogicalRange(saved);
    else if (!saved) fit();
    else chart.timeScale().scrollToRealTime();
    requestAnimationFrame(syncHandles);
  }, [props.chart, props.showZones, props.showLevels, props.showSwings, props.showManual, props.manual, viewportKey]);

  const drag = (level: ManualLevel, event: React.PointerEvent<HTMLButtonElement>) => {
    const series = seriesRef.current, host = hostRef.current;
    if (!series || !host) return;
    event.preventDefault(); event.stopPropagation();
    const price = series.coordinateToPrice(event.clientY - host.getBoundingClientRect().top);
    if (price != null) propsRef.current.onManualChange(level, Number(price));
  };
  const startDrag = (level: ManualLevel, event: React.PointerEvent<HTMLButtonElement>) => { event.preventDefault(); event.stopPropagation(); dragRef.current = level; event.currentTarget.setPointerCapture(event.pointerId); };
  const endDrag = (event: React.PointerEvent<HTMLButtonElement>) => { dragRef.current = null; if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId); };

  return <div ref={hostRef} role="img" aria-label="Backtest trade candlestick chart" data-timeframe={props.chart.timeframe} data-manual-line-source="native-price-lines" style={{ position: "relative", width: "100%", height: props.maximized ? "min(68vh, 760px)" : 430, minHeight: 280, touchAction: "none", cursor: "grab" }}>
    {props.showManual && props.manual && (["target", "entry", "stop"] as ManualLevel[]).map(level => handleY[level] == null ? null : <button key={level} type="button" role="slider" aria-label={`Manual ${level} price drag handle`} aria-valuenow={props.manual![level]} onPointerDown={event => startDrag(level, event)} onPointerMove={event => dragRef.current === level && drag(level, event)} onPointerUp={endDrag} onPointerCancel={endDrag} style={{ position: "absolute", zIndex: 4, top: handleY[level]! - 12, left: 8, width: 210, height: 24, padding: 0, border: 0, background: "transparent", color: manualColor(level), textAlign: "left", cursor: "ns-resize", pointerEvents: "auto", fontSize: 11, fontWeight: 800 }}><span style={{ display: "inline-block", padding: "3px 7px", border: `1px solid ${manualColor(level)}`, borderRadius: 4, background: "rgba(2,6,23,.9)" }}>MANUAL {level.toUpperCase()} {number(props.manual![level]).toFixed(2)}</span></button>)}
  </div>;
});

function priceLines(props: Props) {
  const lines: Array<{ price: number; color: string; title: string; lineStyle: LineStyle; lineWidth: 1 | 2 | 3 }> = [];
  const add = (price: number | string | null | undefined, color: string, title: string, style = LineStyle.Solid, width: 1 | 2 | 3 = 1) => { if (price != null) lines.push({ price: number(price), color, title, lineStyle: style, lineWidth: width }); };
  if (props.showZones) {
    const zones = [props.chart.overlays.setupZone, props.chart.timeframe === "1m" ? props.chart.overlays.confirmationZone : null].filter(Boolean);
    zones.forEach(zone => { if (!zone) return; add(zone.low, "rgba(167,139,250,.8)", `${zone.label} low`, LineStyle.Dashed); add(zone.high, "rgba(167,139,250,.8)", `${zone.label} high`, LineStyle.Dashed); });
  }
  if (props.showLevels) { add(props.chart.overlays.entry.price, "#38bdf8", "Entry", LineStyle.Solid, 2); add(props.chart.overlays.stop.price, "#fb7185", "Stop", LineStyle.Solid, 2); add(props.chart.overlays.tp1?.price, "#4ade80", "TP1", LineStyle.Solid, 2); add(props.chart.overlays.runner?.price, "#fbbf24", "Runner", LineStyle.Solid, 2); }
  if (props.showSwings) { add(props.chart.overlays.stopSwing?.price, "#fde68a", "Stop swing", LineStyle.Dotted); add(props.chart.overlays.targetSwing?.price, "#fde68a", "Target swing", LineStyle.Dotted); }
  if (props.showManual && props.manual) { add(props.manual.entry, "#f8fafc", "Manual entry", LineStyle.Solid, 2); add(props.manual.stop, "#fb923c", "Manual stop", LineStyle.Solid, 2); add(props.manual.target, "#2dd4bf", "Manual target", LineStyle.Solid, 2); }
  return lines;
}

function epoch(value: string) { return Math.floor(new Date(value).getTime() / 1000) as Time; }
function number(value: number | string) { return Number(value); }
function manualColor(level: ManualLevel) { return level === "target" ? "#2dd4bf" : level === "entry" ? "#f8fafc" : "#fb923c"; }

export default BacktestCandlestickChart;
