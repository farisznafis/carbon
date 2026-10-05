const emissionNumberFormatter = new Intl.NumberFormat('ja-JP', {
  maximumFractionDigits: 3,
});

/** Formats an ISO date or timestamp period as a Japanese year/month label. */
export function formatChartPeriod(period: string) {
  const dateParts = period.match(/^(\d{4})-(\d{2})(?:-(\d{2}))?/);
  if (!dateParts) return period;

  const [, year, month, day] = dateParts;
  return day ? `${year}年${month}月${day}日` : `${year}年${month}月`;
}

/** Formats an emission amount with up to three fraction digits. */
export function formatEmissionValue(value: number) {
  return Number.isFinite(value) ? emissionNumberFormatter.format(value) : '—';
}
