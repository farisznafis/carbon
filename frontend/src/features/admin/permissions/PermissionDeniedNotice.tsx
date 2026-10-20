import { ShieldAlert } from 'lucide-react';
import type { AdminOperatorPermissionCode } from '@/api/admin/operators';
import { cn } from '@/lib/utils';
import { getOperatorPermissionLabel } from '@/features/admin/operators/operatorPermissions';

type PermissionDeniedNoticeProps = {
  permission: AdminOperatorPermissionCode;
  compact?: boolean;
  className?: string;
};

/** Displays a local permission warning without replacing the surrounding page. */
export function PermissionDeniedNotice({ permission, compact = false, className }: Readonly<PermissionDeniedNoticeProps>) {
  return (
    <div role="status" className={cn('flex items-start gap-3 rounded-lg border border-destructive/30 bg-destructive/5 text-foreground', compact ? 'px-4 py-3' : 'px-5 py-6 sm:px-6', className)}>
      <ShieldAlert className={cn('mt-0.5 shrink-0 text-destructive', compact ? 'h-4 w-4' : 'h-5 w-5')} aria-hidden="true" />
      <div className="min-w-0">
        <p className={cn('font-bold', compact ? 'text-small' : 'text-body')}>この情報を表示する権限がありません</p>
        {!compact ? <p className="mt-1 text-small leading-6 text-muted-foreground">この情報にアクセスするには、管理者に権限の付与を依頼してください。</p> : null}
        <p className="mt-1 text-xs text-muted-foreground">必要な権限：{getOperatorPermissionLabel(permission)}</p>
      </div>
    </div>
  );
}
