import type { ReactNode } from 'react';
import { cn } from '@/lib/utils';

type StatusBadgeTone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

type StatusBadgeProps = Readonly<{
  children: ReactNode;
  tone?: StatusBadgeTone;
  className?: string;
}>;

const statusBadgeToneClassNames: Record<StatusBadgeTone, string> = {
  success:
    'border-success-border bg-success-subtle text-success-strong',

  warning:
    'border-warning-border bg-warning-subtle text-warning-strong',

  danger:
    'border-destructive-border bg-destructive-subtle text-destructive-strong',

  info:
    'border-info-border bg-info-subtle text-info-strong',

  neutral:
    'border-border bg-secondary text-secondary-foreground',
};

export function StatusBadge({
  children,
  tone = 'neutral',
  className,
}: StatusBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex rounded-full border px-2.5 py-1 text-xs font-medium',
        statusBadgeToneClassNames[tone],
        className,
      )}
    >
      {children}
    </span>
  );
}
