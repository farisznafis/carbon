/**
 * Values under 1 t get up to 3 decimals: per-site monthly emissions are often
 * 0.0x t, and rounding them to 0.1 makes different sites look identical.
 */
export const formatTons = (value: number | null | undefined) => {
  if (value === null || value === undefined) return '—';
  const isSmall = value !== 0 && Math.abs(value) < 1;
  return value.toLocaleString('ja-JP', {
    minimumFractionDigits: isSmall ? 2 : 1,
    maximumFractionDigits: isSmall ? 3 : 1,
  });
};

/** "前月比 +3.2%" style change text; direction is spelled out, never shown by color alone. */
export const formatChange = (label: string, change: number | null) =>
  change === null ? null : `${label} ${change > 0 ? '+' : change < 0 ? '−' : '±'}${Math.abs(change).toFixed(1)}%`;

