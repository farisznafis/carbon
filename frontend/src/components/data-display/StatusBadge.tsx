import type { ReactNode } from 'react';
import { cn } from '@/lib/utils';

/**
 * Pick the tone by what the status means, never by the screen it is on:
 * - warning: waiting for someone's action (承認待ち, 申請受付, 確認待ち)
 * - success: completed, approved or active (承認済み, 入力済み, 稼働中)
 * - danger:  rejected or failed (却下, 失敗)
 * - info:    in progress (実行中)
 * - neutral: no state, a plain tag or a count (未入力, 停止中, 任意, role names)
 */
export type StatusBadgeTone =
  | 'success'
  | 'warning'
  | 'danger'
  | 'info'
  | 'neutral';

type StatusBadgeProps = Readonly<{
  children: ReactNode;
  tone?: StatusBadgeTone;
  className?: string;
}>;

export const statusBadgeToneClassNames: Record<
  StatusBadgeTone,
  string
> = {
  // #064E3B on #ECFDF3 -> WCAG AAA
  success:
    'border-success-border bg-success-subtle text-[hsl(var(--success-800))]',

  // #713F12 on #FFF9E8 -> WCAG AAA
  warning:
    'border-warning-border bg-warning-subtle text-[hsl(var(--warning-800))]',

  danger:
    'border-destructive-border bg-destructive-subtle text-destructive-strong',

  info:
    'border-info-border bg-info-subtle text-info-strong',

  // #344154 on #EEF1F5 -> WCAG AAA
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