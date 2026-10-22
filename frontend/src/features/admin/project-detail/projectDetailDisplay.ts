/** Converts nullable project-detail values into display-safe text. */
export function formatProjectDetailValue(value: unknown) {
  if (value == null || value === '') return '—';
  return String(value);
}

/** Converts a project API value into a finite number without inventing zero. */
export function toProjectDetailNumber(value: unknown): number | null {
  if (value == null || value === '') return null;
  const parsedValue = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(parsedValue) ? parsedValue : null;
}

/** Formats numeric project-detail values using Japanese grouping. */
export function formatProjectDetailNumber(value: unknown) {
  const parsedValue = toProjectDetailNumber(value);
  if (parsedValue == null) return '—';

  return new Intl.NumberFormat('ja-JP', {
    maximumFractionDigits: 1,
  }).format(parsedValue);
}

/** Formats one emissions value together with its domain unit. */
export function formatProjectEmission(value: unknown) {
  const formattedValue = formatProjectDetailNumber(value);
  return formattedValue === '—' ? formattedValue : `${formattedValue} t-CO₂`;
}

/** Formats a nullable API timestamp for read-only detail display. */
export function formatProjectDetailDateTime(value?: string | null) {
  if (!value) return '—';

  const parsedDate = new Date(value);
  if (Number.isNaN(parsedDate.getTime())) return value;

  return new Intl.DateTimeFormat('ja-JP', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(parsedDate);
}

/** Joins name fragments without exposing placeholder fragments such as '- -'. */
export function formatProjectPersonName(...nameParts: unknown[]) {
  const name = nameParts
    .filter((part) => part != null && String(part).trim() !== '')
    .map(String)
    .join(' ');
  return name || '—';
}
