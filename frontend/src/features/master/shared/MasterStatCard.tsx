import type { ReactNode } from 'react';
import { ArrowRight } from 'lucide-react';
import { SkeletonMetricCard } from '@/components/feedback/loading';
import { Card } from '@/components/ui/card';
import { cn } from '@/lib/utils';

/**
 * KPI card shared by the master ホーム and アナリティクス tabs. Same anatomy as the admin
 * ホーム MetricCard: label and note on one line, the number below.
 */
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
  if (loading) return <SkeletonMetricCard className="min-h-[6.5rem]" />;

  const body = (
    <>
      <span className="flex items-start justify-between gap-3">
        {/* The note always takes its own line: inline, it wrapped in some cards and not others at 150% scaling. */}
        <span className="flex min-w-0 flex-col gap-0.5">
          {/* Phrase-aware breaks where supported, so a narrow label does not orphan one character. */}
          <span className="text-body !font-bold text-foreground [word-break:auto-phrase]">{label}</span>
          {/* keep-all breaks only at spaces in every browser (うちプロジェクト参加｜5拠点); anywhere is the overflow fallback. */}
          {note && (
            <span className="text-subtle text-muted-foreground [overflow-wrap:anywhere] [word-break:keep-all]">
              {note}
            </span>
          )}
        </span>
        {/* Phones drop the arrow: half-width cards need the room for the label, and the card is still the button. */}
        {onClick && (
          <ArrowRight
            className="mt-0.5 hidden h-4 w-4 shrink-0 text-primary-strong transition-transform group-hover:translate-x-0.5 sm:block"
            aria-hidden="true"
          />
        )}
      </span>
      {/* Pinned to the bottom so every card's number shares one baseline, whatever wraps above it. */}
      <span className="mt-auto flex items-baseline gap-1.5">
        <span className="text-metric font-bold leading-none tracking-tight tabular-nums text-foreground">{value}</span>
        {unit && <span className="text-base font-bold text-foreground">{unit}</span>}
      </span>
    </>
  );
  const bodyClassName = 'flex h-full w-full flex-col gap-3 rounded-lg p-card text-left';

  return (
    <Card className={cn('h-full rounded-lg border-border', onClick && 'transition-colors hover:border-primary/40')}>
      {onClick ? (
        <button
          type="button"
          onClick={onClick}
          className={cn(bodyClassName, 'group focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring')}
        >
          {body}
        </button>
      ) : (
        <div className={bodyClassName}>{body}</div>
      )}
    </Card>
  );
}
