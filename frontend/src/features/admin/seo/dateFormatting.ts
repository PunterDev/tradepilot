export function formatInstant(value: string, locale?: string, timeZone?: string) {
  const instant = new Date(value);
  if (Number.isNaN(instant.getTime())) return value;

  return instant.toLocaleString(locale, {
    month: "numeric",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
    timeZoneName: "short",
    ...(timeZone ? { timeZone } : {}),
  });
}

// A YYYY-MM-DD reporting bound is a calendar date, not a UTC instant.
export function formatReportingDate(value: string, locale?: string) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!match) return value;

  const [, year, month, day] = match;
  return new Date(Number(year), Number(month) - 1, Number(day)).toLocaleDateString(locale, {
    month: "numeric",
    day: "numeric",
    year: "numeric",
  });
}
