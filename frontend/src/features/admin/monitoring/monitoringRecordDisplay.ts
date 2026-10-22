import type { MonitoringRecordListItem } from '@/api/monitoring';

export function formatMonitoringValue(value: unknown): string {
  if (value == null || value === '') return '-';
  return String(value);
}

export function formatMonitoringNumber(
  value: number | null,
  unit?: string,
): string {
  if (value == null || !Number.isFinite(value)) return '-';

  const formattedValue = new Intl.NumberFormat('ja-JP', {
    maximumFractionDigits: 3,
  }).format(value);

  return unit ? `${formattedValue} ${unit}` : formattedValue;
}

export function formatMonitoringDate(value: string | null | undefined): string {
  if (!value) return '-';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat('ja-JP', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  }).format(date);
}

export function formatMonitoringDateTime(
  value: string | null | undefined,
): string {
  if (!value) return '-';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat('ja-JP', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

export function isMatchingMonitoringRecord(
  record: MonitoringRecordListItem,
  monitoringId: string,
): boolean {
  return String(record.id) === monitoringId;
}
