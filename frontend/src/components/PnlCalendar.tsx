import { useMemo, useState } from "react";
import type { DailyPnl } from "../utils/dashboardUtils";
import type { TradovateTrade } from "../utils/tradovateCsv";
import { getJournalByDate } from "../api/journalApi";
import AiTradeAnalysisPanel from "./AiTradeAnalysisPanel";
import {
  buildDailyPnlFromCalendarTrades,
  formatCurrency,
  formatCompactCurrency,
  getTradeCalendarDate,
  getMonthCalendarDays,
} from "../utils/dashboardUtils";
import {
  button,
  calendarBadge,
  calendarCellBase,
  calendarDayNumber,
  calendarGrid,
  calendarHeaderCell,
  calendarMonthlyFooter,
  calendarPnlAmount,
  calendarPnlWrap,
  calendarTradeCount,
  card,
  hoverPopup,
  input,
  journalGrid,
  label,
  modalCard,
  modalOverlay,
  monthLabel,
  mutedText,
  sectionHeader,
  sectionTitle,
  table,
  tableCard,
  td,
  textarea,
  th,
} from "../styles/dashboardStyles";

type CalendarDisplayMode = "DOLLARS" | "R" | "BOTH";
type SelectedAccountId = number | "ALL" | "";

const CALENDAR_DISPLAY_MODE_KEY = "tradepilot.pnlCalendar.displayMode";

const displayModeOptions: Array<{ value: CalendarDisplayMode; label: string }> = [
  { value: "DOLLARS", label: "$" },
  { value: "R", label: "R" },
  { value: "BOTH", label: "Both" },
];

function getStoredDisplayMode(): CalendarDisplayMode {
  if (typeof window === "undefined") return "DOLLARS";
  const stored = window.localStorage.getItem(CALENDAR_DISPLAY_MODE_KEY);
  return stored === "R" || stored === "BOTH" || stored === "DOLLARS" ? stored : "DOLLARS";
}

function formatR(value: number) {
  const sign = value > 0 ? "+" : "";
  return `${sign}${value.toFixed(2)}R`;
}

function nullableNumber(value: number | null | undefined): number | null {
  return typeof value === "number" && Number.isFinite(value) ? value : null;
}

type PnlCalendarProps = {
  dailyPnl: DailyPnl[];
  trades: TradovateTrade[];
  selectedAccountId: SelectedAccountId;
  calendarYear: number;
  calendarMonth: number;
  setCalendarYear: React.Dispatch<React.SetStateAction<number>>;
  setCalendarMonth: React.Dispatch<React.SetStateAction<number>>;
  journalDate: string;
  setJournalDate: React.Dispatch<React.SetStateAction<string>>;
  journalMood: string;
  setJournalMood: React.Dispatch<React.SetStateAction<string>>;
  journalSetupQuality: string;
  setJournalSetupQuality: React.Dispatch<React.SetStateAction<string>>;
  journalMistakes: string;
  setJournalMistakes: React.Dispatch<React.SetStateAction<string>>;
  journalLesson: string;
  setJournalLesson: React.Dispatch<React.SetStateAction<string>>;
  journalNotes: string;
  setJournalNotes: React.Dispatch<React.SetStateAction<string>>;
  handleSaveJournal: () => void | Promise<void>;
  onTradeSelect?: (tradeId: number) => void;
};

export default function PnlCalendar({
  dailyPnl,
  trades,
  selectedAccountId,
  calendarYear,
  calendarMonth,
  setCalendarYear,
  setCalendarMonth,
  journalDate,
  setJournalDate,
  journalMood,
  setJournalMood,
  journalSetupQuality,
  setJournalSetupQuality,
  journalMistakes,
  setJournalMistakes,
  journalLesson,
  setJournalLesson,
  journalNotes,
  setJournalNotes,
  handleSaveJournal,
  onTradeSelect,
}: PnlCalendarProps) {
  const [hoveredDate, setHoveredDate] = useState<string | null>(null);
  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  const [displayMode, setDisplayMode] = useState<CalendarDisplayMode>(getStoredDisplayMode);

  function updateDisplayMode(nextMode: CalendarDisplayMode) {
    setDisplayMode(nextMode);
    if (typeof window !== "undefined") {
      window.localStorage.setItem(CALENDAR_DISPLAY_MODE_KEY, nextMode);
    }
  }

  const { calendarDailyPnl, dailyPnlByDate, tradesByDate } = useMemo(() => {
    const computedDailyPnl = buildDailyPnlFromCalendarTrades(trades);
    const nextDailyPnlByDate = new Map(
      computedDailyPnl.map((day) => [day.date, day])
    );

    dailyPnl.forEach((day) => {
      const normalizedDate = getTradeCalendarDate({ tradeDate: day.date });
      if (normalizedDate) {
        nextDailyPnlByDate.set(normalizedDate, { ...day, date: normalizedDate });
      }
    });

    const nextTradesByDate = new Map<string, TradovateTrade[]>();

    trades.forEach((trade) => {
      const date = getTradeCalendarDate(trade);

      if (!date) return;

      if (!nextTradesByDate.has(date)) {
        nextTradesByDate.set(date, []);
      }

      nextTradesByDate.get(date)!.push(trade);
    });

    return {
      calendarDailyPnl: Array.from(nextDailyPnlByDate.values())
        .sort((a, b) => a.date.localeCompare(b.date)),
      dailyPnlByDate: nextDailyPnlByDate,
      tradesByDate: nextTradesByDate,
    };
  }, [dailyPnl, trades]);

  const calendarDays = useMemo(
    () => getMonthCalendarDays(calendarYear, calendarMonth),
    [calendarYear, calendarMonth]
  );

  const monthName = new Date(calendarYear, calendarMonth).toLocaleString("default", {
    month: "long",
    year: "numeric",
  });

  const monthlyPnl = useMemo(() => calendarDailyPnl
      .filter((day) => {
        const [year, month] = day.date.split("-").map(Number);
        return year === calendarYear && month === calendarMonth + 1;
      })
      .reduce((sum, day) => sum + day.totalPnl, 0),
    [calendarDailyPnl, calendarMonth, calendarYear]
  );

  const monthlyR = useMemo(() => {
    let total = 0;
    let hasR = false;

    calendarDailyPnl
      .filter((day) => {
        const [year, month] = day.date.split("-").map(Number);
        return year === calendarYear && month === calendarMonth + 1;
      })
      .forEach((day) => {
        const totalR = nullableNumber(day.totalR);
        if (totalR !== null) {
          total += totalR;
          hasR = true;
        }
      });

    return hasR ? total : null;
  }, [calendarDailyPnl, calendarMonth, calendarYear]);

  const selectedDayTrades = selectedDate ? tradesByDate.get(selectedDate) ?? [] : [];
  const selectedDayStats = selectedDate ? dailyPnlByDate.get(selectedDate) : undefined;

  function resetJournalFields() {
    setJournalMood("Focused");
    setJournalSetupQuality("Good");
    setJournalMistakes("");
    setJournalLesson("");
    setJournalNotes("");
  }

  function getTradeSide(trade: TradovateTrade): "LONG" | "SHORT" | "UNKNOWN" {
    if (trade.side === "LONG" || trade.side === "SHORT") return trade.side;

    const boughtTime = new Date(trade.boughtTimestamp).getTime();
    const soldTime = new Date(trade.soldTimestamp).getTime();

    if (!Number.isFinite(boughtTime) || !Number.isFinite(soldTime) || boughtTime === soldTime) {
      return "UNKNOWN";
    }

    return boughtTime < soldTime ? "LONG" : "SHORT";
  }

  async function openDay(date: string) {
    setSelectedDate(date);
    setJournalDate(date);

    try {
      const existing = await getJournalByDate(
        selectedAccountId === "ALL" ? null : Number(selectedAccountId),
        date
      );

      if (existing) {
        setJournalMood(existing.mood ?? "Focused");
        setJournalSetupQuality(existing.setupQuality ?? "Good");
        setJournalMistakes(existing.mistakes ?? "");
        setJournalLesson(existing.lessonLearned ?? "");
        setJournalNotes(existing.notes ?? "");
      } else {
        resetJournalFields();
      }
    } catch (error) {
      console.error("Failed to load journal", error);
      resetJournalFields();
    }
  }

  return (
    <>
      <div style={{ ...card, marginTop: 32 }}>
        <div style={sectionHeader}>
          <div>
            <h2 style={sectionTitle}>PnL Calendar</h2>
            <p style={mutedText}>Hover for a quick preview. Click a trading day to open details and journal.</p>
          </div>

          <div style={{ display: "flex", gap: 12, alignItems: "center", flexWrap: "wrap", justifyContent: "flex-end" }}>
            <div
              aria-label="Calendar display mode"
              style={{
                display: "inline-flex",
                alignItems: "center",
                gap: 4,
                padding: 4,
                borderRadius: 999,
                border: "1px solid rgba(148,163,184,0.18)",
                background: "rgba(15,23,42,0.66)",
              }}
            >
              {displayModeOptions.map((option) => {
                const active = displayMode === option.value;
                return (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => updateDisplayMode(option.value)}
                    aria-pressed={active}
                    style={{
                      border: 0,
                      borderRadius: 999,
                      padding: "7px 12px",
                      minWidth: option.value === "BOTH" ? 58 : 38,
                      fontWeight: 900,
                      fontSize: 12,
                      cursor: "pointer",
                      color: active ? "#02131f" : "#cbd5e1",
                      background: active
                        ? "linear-gradient(135deg, #34d399, #22d3ee)"
                        : "transparent",
                    }}
                  >
                    {option.label}
                  </button>
                );
              })}
            </div>

            <button
              style={{ ...button, width: 105 }}
              onClick={() => {
                if (calendarMonth === 0) {
                  setCalendarMonth(11);
                  setCalendarYear(calendarYear - 1);
                } else {
                  setCalendarMonth(calendarMonth - 1);
                }
              }}
            >
              Prev
            </button>

            <div style={monthLabel}>{monthName}</div>

            <button
              style={{ ...button, width: 105 }}
              onClick={() => {
                if (calendarMonth === 11) {
                  setCalendarMonth(0);
                  setCalendarYear(calendarYear + 1);
                } else {
                  setCalendarMonth(calendarMonth + 1);
                }
              }}
            >
              Next
            </button>
          </div>
        </div>

        <div style={calendarGrid}>
          {["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"].map((day) => (
            <div key={day} style={calendarHeaderCell}>
              {day}
            </div>
          ))}

          {calendarDays.map((calendarDay, index) => {
            const pnlDay = calendarDay.date ? dailyPnlByDate.get(calendarDay.date) : undefined;
            const pnl = pnlDay?.totalPnl ?? 0;
            const totalR = nullableNumber(pnlDay?.totalR);
            const hasTrades = Boolean(pnlDay);
            const signBasis = displayMode === "R" ? totalR ?? 0 : pnl;
            const isWin = signBasis > 0;
            const isLoss = signBasis < 0;
            const primaryValue = displayMode === "R"
              ? totalR === null ? "Set 1R" : formatR(totalR)
              : formatCompactCurrency(pnl);
            const secondaryValue = displayMode === "BOTH"
              ? totalR === null ? "R unavailable" : formatR(totalR)
              : null;

            return (
              <div
                key={`${calendarDay.date}-${index}`}
                onMouseEnter={() => calendarDay.date && setHoveredDate(calendarDay.date)}
                onMouseLeave={() => setHoveredDate(null)}
                onClick={() => {
                  if (calendarDay.date && pnlDay) {
                    void openDay(calendarDay.date);
                  }
                }}
                style={{
                  ...calendarCellBase,
                  cursor: pnlDay ? "pointer" : "default",
                  opacity: calendarDay.dayNumber ? 1 : 0.25,
                  background: !calendarDay.dayNumber
                    ? "rgba(15,23,42,0.35)"
                    : isWin
                      ? "linear-gradient(180deg, rgba(20,83,45,0.72), rgba(6,78,59,0.56))"
                      : isLoss
                        ? "linear-gradient(180deg, rgba(127,29,29,0.68), rgba(76,29,55,0.52))"
                        : "rgba(30,41,59,0.72)",
                  border: isWin
                    ? "1px solid rgba(52,211,153,0.38)"
                    : isLoss
                      ? "1px solid rgba(251,113,133,0.38)"
                      : "1px solid rgba(148,163,184,0.14)",
                }}
              >
                {calendarDay.dayNumber && (
                  <>
                    <div style={calendarDayNumber}>{calendarDay.dayNumber}</div>

                    {hasTrades && (
                      <div
                        style={{
                          ...calendarBadge,
                          color: isWin ? "#34d399" : isLoss ? "#fb7185" : "#94a3b8",
                        }}
                      >
                        {isWin ? "Win" : isLoss ? "Loss" : "Flat"}
                      </div>
                    )}

                    {hasTrades ? (
                      <div style={calendarPnlWrap}>
                        <div
                          style={{
                            ...calendarPnlAmount,
                            color: displayMode === "R" && totalR === null
                              ? "#facc15"
                              : isWin
                                ? "#6ee7b7"
                                : isLoss
                                  ? "#fda4af"
                                  : "#cbd5e1",
                            fontSize: displayMode === "BOTH" ? 17 : calendarPnlAmount.fontSize,
                          }}
                        >
                          {primaryValue}
                        </div>

                        {secondaryValue && (
                          <div
                            style={{
                              marginTop: 2,
                              color: totalR === null ? "#facc15" : totalR >= 0 ? "#a7f3d0" : "#fecdd3",
                              fontSize: 11,
                              fontWeight: 800,
                            }}
                          >
                            {secondaryValue}
                          </div>
                        )}

                        <div style={calendarTradeCount}>
                          {pnlDay!.tradeCount} {pnlDay!.tradeCount === 1 ? "trade" : "trades"} · {pnlDay!.winRate}% win
                        </div>
                      </div>
                    ) : (
                      <div style={{ ...calendarPnlWrap, color: "#64748b", fontSize: 13 }}>
                        No trades
                      </div>
                    )}

                    {hoveredDate === calendarDay.date && pnlDay && (
                      <div style={hoverPopup}>
                        <div style={{ fontWeight: 900, marginBottom: 8 }}>{calendarDay.date}</div>
                        <div>PnL: {formatCurrency(pnlDay.totalPnl)}</div>
                        <div>R: {totalR === null ? "Set 1R" : formatR(totalR)}</div>
                        <div>Trades: {pnlDay.tradeCount}</div>
                        <div>Win Rate: {pnlDay.winRate}%</div>
                        <div style={{ marginTop: 8, color: "#94a3b8" }}>Click to view trades + journal</div>
                      </div>
                    )}
                  </>
                )}
              </div>
            );
          })}
        </div>

        <div style={calendarMonthlyFooter}>
          <strong>{displayMode === "R" ? "Monthly R" : "Monthly PnL"}</strong>
          <strong
            style={{
              color: displayMode === "R"
                ? monthlyR === null
                  ? "#facc15"
                  : monthlyR >= 0
                    ? "#34d399"
                    : "#fb7185"
                : monthlyPnl >= 0
                  ? "#34d399"
                  : "#fb7185",
              display: "inline-flex",
              gap: 10,
              alignItems: "baseline",
            }}
          >
            {displayMode === "R"
              ? monthlyR === null ? "Set 1R" : formatR(monthlyR)
              : formatCurrency(monthlyPnl)}
            {displayMode === "BOTH" && (
              <span style={{ color: monthlyR === null ? "#facc15" : "#94a3b8", fontSize: 13 }}>
                {monthlyR === null ? "R unavailable" : formatR(monthlyR)}
              </span>
            )}
          </strong>
        </div>
      </div>

      {selectedDate && selectedDayStats && (
        <div style={modalOverlay} onClick={() => setSelectedDate(null)}>
          <div style={modalCard} onClick={(e) => e.stopPropagation()}>
            <div style={{ display: "flex", justifyContent: "space-between", gap: 16 }}>
              <div>
                <h2 style={{ ...sectionTitle, marginTop: 0 }}>Trades on {selectedDate}</h2>
                <p style={mutedText}>
                  {selectedDayStats.tradeCount} trades · {selectedDayStats.winRate}% win rate ·{" "}
                  <strong style={{ color: selectedDayStats.totalPnl >= 0 ? "#34d399" : "#fb7185" }}>
                    {formatCurrency(selectedDayStats.totalPnl)}
                  </strong>
                  {" "}· R{" "}
                  <strong
                    style={{
                      color: selectedDayStats.totalR == null
                        ? "#facc15"
                        : selectedDayStats.totalR >= 0
                          ? "#34d399"
                          : "#fb7185",
                    }}
                  >
                    {selectedDayStats.totalR == null ? "unavailable" : formatR(selectedDayStats.totalR)}
                  </strong>
                </p>
              </div>

              <button
                type="button"
                onClick={() => setSelectedDate(null)}
                style={{ ...button, width: 120, background: "#334155" }}
              >
                Close
              </button>
            </div>

            <div style={{ ...tableCard, marginTop: 20 }}>
              <table style={table}>
                <thead>
                  <tr>
                    <th style={th}>Sold Time</th>
                    <th style={th}>Symbol</th>
                    <th style={th}>Qty</th>
                    <th style={th}>Buy</th>
                    <th style={th}>Sell</th>
                    <th style={th}>PnL</th>
                    <th style={th}>Duration</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedDayTrades.length === 0 ? (
                    <tr>
                      <td colSpan={7} style={td}>
                        No trades found for this day.
                      </td>
                    </tr>
                  ) : (
                    selectedDayTrades.map((trade, index) => (
                      <tr
                        key={`${trade.soldTimestamp}-${index}`}
                        onClick={() => {
                          if (trade.id) {
                            setSelectedDate(null);
                            onTradeSelect?.(trade.id);
                          }
                        }}
                        title={trade.id ? "Open trade detail" : undefined}
                        style={{ cursor: trade.id ? "pointer" : "default" }}
                      >
                        <td style={td}>{trade.soldTimestamp}</td>
                        <td style={td}>{trade.symbol}</td>
                        <td style={td}>{trade.qty}</td>
                        <td style={td}>{trade.buyPrice}</td>
                        <td style={td}>{trade.sellPrice}</td>
                        <td
                          style={{
                            ...td,
                            color: trade.pnl >= 0 ? "#34d399" : "#fb7185",
                            fontWeight: 900,
                          }}
                        >
                          {formatCurrency(trade.pnl)}
                        </td>
                        <td style={td}>{trade.duration || "-"}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            <div style={{ marginTop: 24 }}>
              <h3 style={{ ...sectionTitle, marginBottom: 6 }}>
                Journal for {selectedDate}
              </h3>
              <p style={mutedText}>
                Capture your mindset, mistakes, lessons, and execution notes for this trading day.
              </p>

              <div style={journalGrid}>
                <div>
                  <label style={label}>Journal Date</label>
                  <input type="date" value={journalDate} readOnly style={input} />
                </div>

                <div>
                  <label style={label}>Mood</label>
                  <select
                    value={journalMood}
                    onChange={(e) => setJournalMood(e.target.value)}
                    style={input}
                  >
                    <option value="Focused">Focused</option>
                    <option value="Calm">Calm</option>
                    <option value="Anxious">Anxious</option>
                    <option value="Greedy">Greedy</option>
                    <option value="Revenge Trading">Revenge Trading</option>
                    <option value="Disciplined">Disciplined</option>
                  </select>
                </div>

                <div>
                  <label style={label}>Setup Quality</label>
                  <select
                    value={journalSetupQuality}
                    onChange={(e) => setJournalSetupQuality(e.target.value)}
                    style={input}
                  >
                    <option value="A+">A+</option>
                    <option value="A">A</option>
                    <option value="Good">Good</option>
                    <option value="Average">Average</option>
                    <option value="Poor">Poor</option>
                    <option value="Forced Trade">Forced Trade</option>
                  </select>
                </div>
              </div>

              <div style={{ marginTop: 16 }}>
                <label style={label}>Mistakes</label>
                <textarea
                  value={journalMistakes}
                  onChange={(e) => setJournalMistakes(e.target.value)}
                  style={textarea}
                  placeholder="What mistakes did you make today?"
                />
              </div>

              <div style={{ marginTop: 16 }}>
                <label style={label}>Lesson Learned</label>
                <textarea
                  value={journalLesson}
                  onChange={(e) => setJournalLesson(e.target.value)}
                  style={textarea}
                  placeholder="What did the market teach you today?"
                />
              </div>

              <div style={{ marginTop: 16 }}>
                <label style={label}>Notes</label>
                <textarea
                  value={journalNotes}
                  onChange={(e) => setJournalNotes(e.target.value)}
                  style={textarea}
                  placeholder="Session notes..."
                />
              </div>

              <div style={{ display: "flex", justifyContent: "flex-end", marginTop: 18 }}>
                <button
                  type="button"
                  onClick={handleSaveJournal}
                  style={{ ...button, width: 180 }}
                >
                  Save Journal
                </button>
              </div>

              {selectedDayTrades.length > 0 && (() => {
                const primaryTrade = selectedDayTrades[0];
                const side = getTradeSide(primaryTrade);
                const isShort = side === "SHORT";

                const aiTrade = {
                  id: (primaryTrade as TradovateTrade & { id?: number }).id ?? 1,
                  symbol: primaryTrade.symbol,
                  side,
                  pnl: primaryTrade.pnl,
                  quantity: primaryTrade.qty,
                  entryPrice: isShort ? primaryTrade.sellPrice : primaryTrade.buyPrice,
                  exitPrice: isShort ? primaryTrade.buyPrice : primaryTrade.sellPrice,
                  entryTime: isShort ? primaryTrade.soldTimestamp : primaryTrade.boughtTimestamp,
                  exitTime: isShort ? primaryTrade.boughtTimestamp : primaryTrade.soldTimestamp,
                  notes: journalNotes,
                };

                const aiJournalText = `
                  Mood: ${journalMood}
                  Setup Quality: ${journalSetupQuality}
                  Mistakes: ${journalMistakes}
                  Lesson Learned: ${journalLesson}
                  Notes: ${journalNotes}
                `;

                return (
                  <div style={{ marginTop: 24 }}>
                    <AiTradeAnalysisPanel
                      trade={aiTrade}
                      journalText={aiJournalText}
                    />
                  </div>
                );
              })()}
            </div>
          </div>
        </div>
      )}
    </>
  );
}
