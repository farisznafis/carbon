export function formatNotificationDate(value: string | null): string {
  if (!value) return '-';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  const parts = new Intl.DateTimeFormat('ja-JP', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).formatToParts(date);
  const valueByType = Object.fromEntries(
    parts.map((part) => [part.type, part.value]),
  );

  return `${valueByType.year}年${valueByType.month}月${valueByType.day}日 ${valueByType.hour}:${valueByType.minute}`;
}
