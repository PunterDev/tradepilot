export const SESSION_ORDER = [
    "NY_OPEN_FIRST_15",
    "NY_AM_EDGE_WINDOW",
    "LATE_MORNING",
    "LUNCH",
    "PM_SESSION",
    "OTHER",
    "UNKNOWN",
];

export const DAY_ORDER = [
    "MONDAY",
    "TUESDAY",
    "WEDNESDAY",
    "THURSDAY",
    "FRIDAY",
    "SATURDAY",
    "SUNDAY",
];

export function formatSessionLabel(session?: string) {
    switch (normalizeSessionBucket(session)) {
        case "NY_OPEN_FIRST_15":
            return "NY Open First 15m";
        case "NY_AM_EDGE_WINDOW":
            return "9:45–11:00 AM";
        case "LATE_MORNING":
            return "Late Morning";
        case "LUNCH":
            return "Lunch";
        case "PM_SESSION":
            return "PM Session";
        case "OTHER":
            return "Other";
        case "UNKNOWN":
            return "Unknown";
        default:
            return session || "Unknown";
    }
}

export function normalizeSessionBucket(session?: string | null) {
    const normalized = (session ?? "")
        .trim()
        .toUpperCase()
        .replace(/[\s-]+/g, "_");

    switch (normalized) {
        case "NY_OPEN_FIRST_15":
        case "NY_OPEN":
        case "OPEN":
            return "NY_OPEN_FIRST_15";
        case "NY_AM":
        case "NYAM":
        case "NY_AM_EDGE_WINDOW":
        case "AM_SESSION":
            return "NY_AM_EDGE_WINDOW";
        case "LATE_MORNING":
            return "LATE_MORNING";
        case "LUNCH":
            return "LUNCH";
        case "NY_PM":
        case "NYPM":
        case "PM_SESSION":
        case "PM":
            return "PM_SESSION";
        case "UNKNOWN":
        case "UNSPECIFIED":
        case "":
            return "OTHER";
        default:
            return SESSION_ORDER.includes(normalized) ? normalized : "OTHER";
    }
}

export function normalizeWeekdayKey(day?: string | null) {
    const normalized = (day ?? "").trim().toUpperCase();

    switch (normalized) {
        case "1":
        case "1.0":
        case "MON":
        case "MONDAY":
            return "MONDAY";
        case "2":
        case "2.0":
        case "TUE":
        case "TUESDAY":
            return "TUESDAY";
        case "3":
        case "3.0":
        case "WED":
        case "WEDNESDAY":
            return "WEDNESDAY";
        case "4":
        case "4.0":
        case "THU":
        case "THURSDAY":
            return "THURSDAY";
        case "5":
        case "5.0":
        case "FRI":
        case "FRIDAY":
            return "FRIDAY";
        case "6":
        case "6.0":
        case "SAT":
        case "SATURDAY":
            return "SATURDAY";
        case "7":
        case "7.0":
        case "SUN":
        case "SUNDAY":
            return "SUNDAY";
        default:
            return "UNKNOWN";
    }
}

export function formatDayLabel(day?: string) {
    if (!day) return "Unknown";

    return day.charAt(0).toUpperCase() + day.slice(1).toLowerCase();
}

export function getSessionOrder(session?: string) {
    const index = SESSION_ORDER.indexOf(normalizeSessionBucket(session));
    return index === -1 ? SESSION_ORDER.length : index;
}

export function formatAnalyticsText(value: string) {
    return value
        .replace(/\bNY_OPEN_FIRST_15\b/g, formatSessionLabel("NY_OPEN_FIRST_15"))
        .replace(/\bNY_AM_EDGE_WINDOW\b/g, formatSessionLabel("NY_AM_EDGE_WINDOW"))
        .replace(/\bNY_AM\b/g, formatSessionLabel("NY_AM"))
        .replace(/\bLATE_MORNING\b/g, formatSessionLabel("LATE_MORNING"))
        .replace(/\bLUNCH\b/g, formatSessionLabel("LUNCH"))
        .replace(/\bPM_SESSION\b/g, formatSessionLabel("PM_SESSION"))
        .replace(/\bNY_PM\b/g, formatSessionLabel("NY_PM"))
        .replace(/\bOTHER\b/g, formatSessionLabel("OTHER"));
}

export function getDayOrder(day?: string) {
    const index = DAY_ORDER.indexOf(day || "");
    return index === -1 ? DAY_ORDER.length : index;
}

export function formatMoney(value?: number) {
    return `$${(value ?? 0).toFixed(2)}`;
}

export function getPnlColor(value?: number) {
    return (value ?? 0) >= 0 ? "#4ade80" : "#f87171";
}
