import { Clock } from 'lucide-react';
import { formatActionSessionDeadline } from '@/lib/actionSession';

/** States until when an action-link form can be submitted; the deadline is fixed at first opening. */
export function ActionSessionDeadline({ expiresAt }: Readonly<{ expiresAt: number }>) {
  return (
    <p className="flex items-center gap-2 text-small text-foreground">
      <Clock className="h-4 w-4 shrink-0 text-muted-foreground" aria-hidden="true" />
      {formatActionSessionDeadline(expiresAt)} までに入力を完了してください。
    </p>
  );
}
