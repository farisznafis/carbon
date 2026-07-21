import { cn } from '@/lib/utils';

const BATCH_STATUS_LABELS: Record<string, string> = {
  SUCCESS: '成功',
  FAILED: '失敗',
};
const SUCCESS_BATCH_STATUSES = ['SUCCESS', 'SUCCEEDED', 'COMPLETED'];
const FAILED_BATCH_STATUSES = ['FAILED', 'ERROR'];

export function BatchHistoryStatusBadge({ status }: { status: string }) {
  const normalizedStatus = status.toUpperCase();
  const isSuccess = SUCCESS_BATCH_STATUSES.includes(normalizedStatus);
  const isFailed = FAILED_BATCH_STATUSES.includes(normalizedStatus);

  return (
    <span
      className={cn(
        'inline-flex rounded-full border px-2.5 py-1 text-xs font-medium',
        isSuccess && 'border-emerald-200 bg-emerald-50 text-emerald-800',
        isFailed && 'border-red-200 bg-red-50 text-red-800',
        !isSuccess &&
          !isFailed &&
          'border-amber-200 bg-amber-50 text-amber-800',
      )}
    >
      {(BATCH_STATUS_LABELS[normalizedStatus] ?? status) || '-'}
    </span>
  );
}
