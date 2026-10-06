export const humanize = (value: string | null | undefined) => {
  if (!value) return "-";
  const labels: Record<string, string> = {
    CONSERVATIVE_STOP_FIRST: "Stop first (conservative)",
    BREAK_EVEN: "Break-even",
    FVG_5M: "5m FVG",
    FVG_1M: "1m FVG",
    CONFIRMED_SWING: "Confirmed swing",
    OUTSIDE_SESSION: "Outside session",
    NO_5M_CONTEXT: "No 5m context",
    NO_ZONE_TOUCH: "No zone touch",
    NO_IFVG_CONFIRMATION: "No IFVG confirmation",
    NO_DISPLACEMENT: "No displacement",
    NO_CONFIRMATION_ZONE: "No confirmation zone",
    NO_RETRACE: "No retrace",
    CHASE_FILTER: "Chase filter",
    NO_CAUSAL_STOP: "No causal stop",
    NO_VALID_SWING_TARGET: "No valid swing target",
    MAX_TRADES_REACHED: "Max trades reached",
    ROLLOVER_RESET: "Rollover reset",
  };
  return labels[value] ?? value.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (letter: string) => letter.toUpperCase());
};

export const formatR = (value: number | null | undefined, digits = 2) => {
  if (value == null || !Number.isFinite(value)) return "-";
  return `${value > 0 ? "+" : ""}${value.toFixed(digits)}R`;
};

export const formatPercent = (value: number | null | undefined) => value == null || !Number.isFinite(value) ? "-" : `${(value * 100).toFixed(1)}%`;
export const formatPrice = (value: number | null | undefined) => value == null || !Number.isFinite(value) ? "-" : value.toFixed(3).replace(/\.000$/, "");
export const formatNumber = (value: number | null | undefined) => value == null || !Number.isFinite(value) ? "-" : new Intl.NumberFormat("en-US").format(value);
export const formatDateRange = (start: string, end: string) => `${new Intl.DateTimeFormat("en-US", { month: "short", day: "numeric", year: "numeric" }).format(new Date(`${start}T12:00:00Z`))} - ${new Intl.DateTimeFormat("en-US", { month: "short", day: "numeric", year: "numeric" }).format(new Date(`${end}T12:00:00Z`))}`;
export const formatEntryTime = (value: string) => new Intl.DateTimeFormat("en-US", { timeZone: "America/New_York", hour: "numeric", minute: "2-digit" }).format(new Date(value));
