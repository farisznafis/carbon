import { StatusBadge } from '@/components/data-display/StatusBadge';

const cancelRequestStatusPresentation: Record<
  string,
  { label: string; tone: 'warning' | 'success' | 'danger' }
> = {
  PENDING: {
    label: '承認待ち',
    tone: 'warning',
  },
  APPROVED: {
    label: '承認済み',
    tone: 'success',
  },
  REJECTED: {
    label: '却下',
    tone: 'danger',
  },
};

export function CancelRequestStatusBadge({ status }: { status: string }) {
  const normalizedStatus = status.toUpperCase();
  const presentation = cancelRequestStatusPresentation[normalizedStatus];

  return (
    <StatusBadge tone={presentation?.tone}>
      {presentation?.label ?? status}
    </StatusBadge>
  );
}
