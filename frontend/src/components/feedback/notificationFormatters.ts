export function formatNotificationDate(value: string | null): string {
  if (!value) return '-';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat('ja-JP', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date);
}

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
