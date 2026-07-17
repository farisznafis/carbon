import { cn } from '@/lib/utils';

const cancelRequestStatusPresentation: Record<
  string,
  { label: string; className: string }
> = {
  PENDING: {
    label: '承認待ち',
    className: 'border-amber-200 bg-amber-50 text-amber-800',
  },
  APPROVED: {
    label: '承認済み',
    className: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  },
  REJECTED: {
    label: '却下',
    className: 'border-red-200 bg-red-50 text-red-800',
  },
};

export function CancelRequestStatusBadge({ status }: { status: string }) {
  const normalizedStatus = status.toUpperCase();
  const presentation = cancelRequestStatusPresentation[normalizedStatus];

  return (
    <span
      className={cn(
        'inline-flex rounded-full border px-2.5 py-1 text-xs font-medium',
        presentation?.className ??
          'border-border bg-muted text-muted-foreground',
      )}
    >
      {presentation?.label ?? status}
    </span>
  );
}
