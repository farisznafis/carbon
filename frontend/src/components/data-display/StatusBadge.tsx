import type { ReactNode } from 'react';
import { cn } from '@/lib/utils';

type StatusBadgeTone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

type StatusBadgeProps = Readonly<{
  children: ReactNode;
  tone?: StatusBadgeTone;
  className?: string;
}>;

const statusBadgeToneClassNames: Record<StatusBadgeTone, string> = {
  success: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  warning: 'border-amber-200 bg-amber-50 text-amber-800',
  danger: 'border-red-200 bg-red-50 text-red-800',
  info: 'border-blue-200 bg-blue-50 text-blue-800',
  neutral: 'border-border bg-muted text-foreground',
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
