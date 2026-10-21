import { useState } from 'react';
import { CheckCircle2 } from 'lucide-react';
import { ActionButton } from '@/components/actions';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { SkeletonList } from '@/components/feedback/loading';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { cn } from '@/lib/utils';

/** One kind of work the signed-in account itself has to do, with its count already in `text`. */
export type ActionRequiredItem = {
  key: string;
  /** Kind of work, e.g. 今月未提出. Urgent rows show 緊急 instead. */
  tag: string;
  text: string;
  actionLabel: string;
  onAction: () => void;
  /** Red marker and 緊急 label. Keep it rare: when everything is red, nothing is. */
  urgent?: boolean;
};

const COLLAPSED_LIMIT = 3;

/**
 * 対応が必要なこと, shared by the master and admin ホーム. One row per kind of work,
 * urgent rows first. Work that waits on someone else does not belong here.
 */
export function ActionRequiredPanel({
  items,
  loading,
  className,
}: Readonly<{
  items: ActionRequiredItem[];
  loading: boolean;
  className?: string;
}>) {
  const [expanded, setExpanded] = useState(false);
  const ordered = [...items].sort((left, right) => Number(Boolean(right.urgent)) - Number(Boolean(left.urgent)));
  const visible = expanded ? ordered : ordered.slice(0, COLLAPSED_LIMIT);
  const urgentCount = items.filter((item) => item.urgent).length;

  return (
    <Card className={cn('rounded-lg border-border', !loading && items.length > 0 && 'border-primary/60', className)}>
      <CardHeader className="flex-row flex-wrap items-center gap-x-2 gap-y-1 space-y-0">
        <CardTitle className="text-subsection-title font-bold">対応が必要なこと</CardTitle>
        {!loading && items.length > 0 && (
          <span className="inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-primary-strong px-1.5 text-xs font-bold tabular-nums text-primary-foreground">
            {items.length}
          </span>
        )}
        {!loading && urgentCount > 0 && (
          <span className="text-small font-bold text-destructive-strong">うち緊急 {urgentCount}件</span>
        )}
      </CardHeader>
      <CardContent>
        {loading ? (
          <SkeletonList items={2} />
        ) : items.length === 0 ? (
          <div className="flex items-center gap-2 rounded-lg border border-success-border bg-success-subtle px-4 py-3 text-small text-success-strong">
            <CheckCircle2 className="h-4 w-4 shrink-0" aria-hidden="true" />
            対応が必要なことはありません
          </div>
        ) : (
          <>
            <ul className="divide-y divide-border">
              {visible.map((item) => (
                // One line per kind from sm up; on phones the message wraps under tag and link.
                <li
                  key={item.key}
                  className={cn(
                    'relative flex flex-wrap items-center gap-x-3 gap-y-1 py-2.5 pl-3',
                    // The red bar marks urgency; the 緊急 label says it in words for screen readers and color blindness.
                    item.urgent &&
                      'before:absolute before:inset-y-2 before:left-0 before:w-[3px] before:rounded-full before:bg-destructive',
                  )}
                >
                  {/* Fixed tag width from sm up, so every message starts at the same x whatever the tag says. */}
                  <StatusBadge tone={item.urgent ? 'danger' : 'neutral'} className="shrink-0 justify-center sm:min-w-[6rem]">
                    {item.urgent ? '緊急' : item.tag}
                  </StatusBadge>
                  <p className="order-last min-w-0 basis-full break-words text-body text-foreground sm:order-none sm:flex-1 sm:basis-0">
                    {item.text}
                  </p>
                  <ActionButton
                    kind="link"
                    hideIcon
                    className="-my-1 -mr-2 ml-auto h-7 shrink-0"
                    onClick={item.onAction}
                  >
                    {item.actionLabel}
                  </ActionButton>
                </li>
              ))}
            </ul>
            {items.length > COLLAPSED_LIMIT && (
              <ActionButton kind="link" className="mt-2" onClick={() => setExpanded((value) => !value)}>
                {expanded ? '折りたたむ' : `すべて表示（${items.length}件）`}
              </ActionButton>
            )}
          </>
        )}
      </CardContent>
    </Card>
  );
}
