import { StatusBadge } from '@/components/data-display/StatusBadge';

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
    <StatusBadge
      tone={isSuccess ? 'success' : isFailed ? 'danger' : 'warning'}
    >
      {(BATCH_STATUS_LABELS[normalizedStatus] ?? status) || '-'}
    </StatusBadge>
  );
}
