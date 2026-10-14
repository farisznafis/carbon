import { formatNotificationDate } from '@/components/feedback/notificationFormatters';

export const formatTons = (value: number | null | undefined) =>
  value === null || value === undefined
    ? '—'
    : value.toLocaleString('ja-JP', { minimumFractionDigits: 1, maximumFractionDigits: 1 });

/** "前月比 +3.2%" style change text; direction is spelled out, never shown by color alone. */
export const formatChange = (label: string, change: number | null) =>
  change === null ? null : `${label} ${change > 0 ? '+' : change < 0 ? '−' : '±'}${Math.abs(change).toFixed(1)}%`;

/** 10分前 / 2時間前 / 昨日 / n日前, then the full date after a week. */
export function formatRelativeTime(value: string | null, now = new Date()) {
  if (!value) return '—';
  const time = new Date(value.replace(/(\.\d{3})\d+/, '$1')).getTime();
  if (Number.isNaN(time)) return value;
  const minutes = Math.floor((now.getTime() - time) / 60000);
  if (minutes < 1) return 'たった今';
  if (minutes < 60) return `${minutes}分前`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}時間前`;
  const days = Math.floor(hours / 24);
  if (days === 1) return '昨日';
  if (days < 7) return `${days}日前`;
  return formatNotificationDate(value).split(' ')[0];
}
