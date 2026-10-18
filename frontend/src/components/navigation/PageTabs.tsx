import type * as React from 'react';
import { forwardRef, type ComponentPropsWithoutRef, type ElementRef } from 'react';
import { StatusBadge, type StatusBadgeTone } from '@/components/data-display/StatusBadge';
import { TabsList, TabsTrigger } from '@/components/ui/tabs';
import { cn } from '@/lib/utils';

/**
 * Underlined page-level tabs that switch a page's main sections
 * (プロジェクト: 管理 / アナリティクス, プロジェクト詳細: 概要 / …).
 * Filters inside a section keep the segmented `TabsList` style instead.
 */
export const PageTabsList = forwardRef<
  ElementRef<typeof TabsList>,
  ComponentPropsWithoutRef<typeof TabsList>
>(({ className, ...props }, ref) => (
  <TabsList
    ref={ref}
    className={cn(
      'flex h-auto w-full justify-start gap-6 overflow-x-auto rounded-none border-b border-border bg-transparent p-0',
      className,
    )}
    {...props}
  />
));
PageTabsList.displayName = 'PageTabsList';

export const PageTabsTrigger = forwardRef<
  ElementRef<typeof TabsTrigger>,
  ComponentPropsWithoutRef<typeof TabsTrigger>
>(({ className, ...props }, ref) => (
  <TabsTrigger
    ref={ref}
    className={cn(
      'h-11 shrink-0 rounded-none border-b-2 border-transparent bg-transparent px-1 py-2 text-body text-muted-foreground shadow-none hover:text-foreground data-[state=active]:border-primary data-[state=active]:bg-transparent data-[state=active]:font-bold data-[state=active]:text-foreground data-[state=active]:shadow-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring focus-visible:ring-offset-0',
      className,
    )}
    {...props}
  />
));
PageTabsTrigger.displayName = 'PageTabsTrigger';

/*
 * Segmented controls: a status filter or sub-view inside one section.
 * Used as tabs (入会届: 承認待ち / 承認済み / 却下) or as a toggle group that
 * filters a list (master 入会届: すべて / 未入会 / 申請中 / 入会中).
 *
 * One recipe for both: the selected segment takes the tone of the status it
 * selects (申請中 → warning, 却下 → danger, …), exactly like StatusBadge, so a
 * status looks the same in filters, badges and chips. Non-status segments
 * (すべて, ユーザー一覧) use the neutral tone. Counts inside stay neutral.
 */
const SEGMENT_CONTAINER_CLASS_NAME =
  'inline-flex h-auto max-w-full flex-wrap items-center justify-start gap-1 rounded-full border border-border bg-muted p-1';

const SEGMENT_ITEM_CLASS_NAME =
  'inline-flex h-8 items-center gap-2 whitespace-nowrap rounded-full border border-transparent bg-transparent px-4 text-small font-medium text-muted-foreground shadow-none transition-colors hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50 data-[state=active]:font-bold data-[state=active]:shadow-app-sm aria-pressed:font-bold aria-pressed:shadow-app-sm';

/** Selected-state colours per tone; mirrors StatusBadge so both read the same. */
const SEGMENT_ACTIVE_TONE_CLASS_NAMES: Record<StatusBadgeTone, string> = {
  neutral:
    'data-[state=active]:border-border data-[state=active]:bg-card data-[state=active]:text-foreground aria-pressed:border-border aria-pressed:bg-card aria-pressed:text-foreground',
  warning:
    'data-[state=active]:border-warning-border data-[state=active]:bg-warning-subtle data-[state=active]:text-[hsl(var(--warning-800))] aria-pressed:border-warning-border aria-pressed:bg-warning-subtle aria-pressed:text-[hsl(var(--warning-800))]',
  success:
    'data-[state=active]:border-success-border data-[state=active]:bg-success-subtle data-[state=active]:text-[hsl(var(--success-800))] aria-pressed:border-success-border aria-pressed:bg-success-subtle aria-pressed:text-[hsl(var(--success-800))]',
  danger:
    'data-[state=active]:border-destructive-border data-[state=active]:bg-destructive-subtle data-[state=active]:text-destructive-strong aria-pressed:border-destructive-border aria-pressed:bg-destructive-subtle aria-pressed:text-destructive-strong',
  info:
    'data-[state=active]:border-info-border data-[state=active]:bg-info-subtle data-[state=active]:text-info-strong aria-pressed:border-info-border aria-pressed:bg-info-subtle aria-pressed:text-info-strong',
};

type SegmentToneProps = {
  /** Tone of the status this segment selects; neutral for non-status segments. */
  tone?: StatusBadgeTone;
};

export const SegmentTabsList = forwardRef<
  ElementRef<typeof TabsList>,
  ComponentPropsWithoutRef<typeof TabsList>
>(({ className, ...props }, ref) => (
  <TabsList
    ref={ref}
    className={cn(SEGMENT_CONTAINER_CLASS_NAME, className)}
    {...props}
  />
));
SegmentTabsList.displayName = 'SegmentTabsList';

export const SegmentTabsTrigger = forwardRef<
  ElementRef<typeof TabsTrigger>,
  ComponentPropsWithoutRef<typeof TabsTrigger> & SegmentToneProps
>(({ className, tone = 'neutral', ...props }, ref) => (
  <TabsTrigger
    ref={ref}
    className={cn(
      SEGMENT_ITEM_CLASS_NAME,
      SEGMENT_ACTIVE_TONE_CLASS_NAMES[tone],
      className,
    )}
    {...props}
  />
));
SegmentTabsTrigger.displayName = 'SegmentTabsTrigger';

/** Toggle-group variant for filters that narrow a list without tab panels. */
export function SegmentGroup({
  className,
  ...props
}: Readonly<React.HTMLAttributes<HTMLDivElement>>) {
  return (
    <div
      role="group"
      className={cn(SEGMENT_CONTAINER_CLASS_NAME, className)}
      {...props}
    />
  );
}

export const SegmentButton = forwardRef<
  HTMLButtonElement,
  Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, 'aria-pressed'> &
    SegmentToneProps & { active: boolean }
>(({ className, tone = 'neutral', active, type = 'button', ...props }, ref) => (
  <button
    ref={ref}
    type={type}
    aria-pressed={active}
    className={cn(
      SEGMENT_ITEM_CLASS_NAME,
      SEGMENT_ACTIVE_TONE_CLASS_NAMES[tone],
      className,
    )}
    {...props}
  />
));
SegmentButton.displayName = 'SegmentButton';

/**
 * Count shown inside a tab or segment label. Neutral by default (the segment
 * itself carries the status colour); `warning` on page tabs when the number
 * counts waiting work (入会届 3).
 */
export function TabCount({
  count,
  tone = 'neutral',
}: Readonly<{ count: number; tone?: StatusBadgeTone }>) {
  return (
    <StatusBadge
      tone={tone}
      className="min-w-5 justify-center px-1.5 py-0 text-xs font-bold tabular-nums"
    >
      {count.toLocaleString('ja-JP')}
    </StatusBadge>
  );
}
