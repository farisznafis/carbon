import type { MonitoringRecordListItem } from '@/api/monitoring';
import { formatChartPeriod } from '@/components/data-display/charts/chartFormatters';
import { formatNotificationDate } from '@/components/feedback/notificationFormatters';

export type MonitoringRecordStatus = 'active' | 'requested' | 'cancelled';

export function getRecordKey(record: MonitoringRecordListItem) {
  return `${record.id}:${record.timeUpdated}`;
}

/** Date-only values are shown as written, so no time zone can shift them a day. */
export function formatRecordDate(value: string) {
  return /^\d{4}-\d{2}-\d{2}$/.test(value ?? '')
    ? formatChartPeriod(value)
    : formatRecordDateTime(value, false);
}

/** The API sends nanosecond fractions (…28.139719300Z); trims them so every browser parses the value. */
export function formatRecordDateTime(value: string | null, withTime = true) {
  if (!value) return '-';
  const formatted = formatNotificationDate(value.replace(/(\.\d{3})\d+/, '$1'));
  return withTime ? formatted : formatted.split(' ')[0];
}

/** Units live in the column header or label, so cells carry the number alone. */
export function formatNumber(value: number | null) {
  if (value === null || value === undefined || Number.isNaN(value)) return '-';
  return new Intl.NumberFormat('ja-JP').format(value);
}

export function getRecordStatus(
  record: MonitoringRecordListItem,
  requestedKeys: readonly string[],
): MonitoringRecordStatus {
  if (!record.isActive) return 'cancelled';
  return requestedKeys.includes(getRecordKey(record)) ? 'requested' : 'active';
}

export function canRequestCancel(record: MonitoringRecordListItem, status: MonitoringRecordStatus) {
  return status === 'active' && Boolean(record.id) && Boolean(record.timeUpdated);
}
