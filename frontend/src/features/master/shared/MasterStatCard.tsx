import type { ReactNode } from 'react';
import { SkeletonMetricCard } from '@/components/feedback/loading';
import { Card } from '@/components/ui/card';
import { cn } from '@/lib/utils';

/** KPI card shared by the master ホーム and アナリティクス tabs. */
export function MasterStatCard({
  label,
  value,
  unit,
  note,
  loading,
  onClick,
}: {
  label: string;
  value: ReactNode;
  unit?: string;
  note?: ReactNode;
  loading: boolean;
  onClick?: () => void;
}) {
  if (loading) return <SkeletonMetricCard className="min-h-[7.5rem]" />;

  const body = (
    <>
      <span className="text-body !font-bold text-foreground">{label}</span>
      <span className="flex items-baseline gap-1.5">
        <span className="text-metric font-bold leading-none tracking-tight tabular-nums text-foreground">{value}</span>
        {unit && <span className="text-base font-bold text-foreground">{unit}</span>}
      </span>
      {note && <span className="text-subtle text-muted-foreground">{note}</span>}
    </>
  );
  const bodyClassName = 'flex h-full w-full flex-col gap-3 rounded-lg p-card text-left';

  return (
    <Card className={cn('h-full rounded-lg border-border', onClick && 'transition-colors hover:border-primary/40')}>
      {onClick ? (
        <button
          type="button"
          onClick={onClick}
          className={cn(bodyClassName, 'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring')}
        >
          {body}
        </button>
      ) : (
        <div className={bodyClassName}>{body}</div>
      )}
    </Card>
  );
}
