import type { CSSProperties, HTMLAttributes, ReactNode } from 'react';
import { Skeleton } from '@/components/ui/skeleton';
import { cn } from '@/lib/utils';

// 方針：葉のアニメーション（AppLoadingScreen / PageLoadingOverlay）はアプリ起動とページ遷移のみ。
// ページ内のデータ読み込みは、最終的なレイアウトに近い形のスケルトンで表示する。

type SkeletonStatusProps = HTMLAttributes<HTMLDivElement> & {
  label: string;
};

/** スクリーンリーダー向けに読み込み中であることを伝える共通の外枠。 */
function SkeletonStatus({ label, className, children, ...props }: SkeletonStatusProps) {
  return (
    <div
      role="status"
      aria-live="polite"
      aria-busy="true"
      aria-label={label}
      className={className}
      {...props}
    >
      <span className="sr-only">{label}</span>
      {children}
    </div>
  );
}

const cardClassName = 'rounded-lg border border-border bg-card p-5';
// 行ごとに長さを変えて、実際の文章に近い見た目にする
const lineWidths = ['w-full', 'w-11/12', 'w-4/5', 'w-2/3', 'w-3/4'];

function SkeletonLines({ lines }: { lines: number }) {
  return (
    <div className="space-y-2.5">
      {Array.from({ length: lines }, (_, index) => (
        <Skeleton
          key={index}
          className={cn('h-3.5', index === lines - 1 && lines > 1 ? 'w-1/2' : lineWidths[index % lineWidths.length])}
        />
      ))}
    </div>
  );
}

type SkeletonTextProps = HTMLAttributes<HTMLDivElement> & {
  lines?: number;
};

export function SkeletonText({ lines = 3, ...props }: SkeletonTextProps) {
  return (
    <SkeletonStatus label="データを読み込んでいます" {...props}>
      <SkeletonLines lines={lines} />
    </SkeletonStatus>
  );
}

type LabeledSkeletonProps = HTMLAttributes<HTMLDivElement> & {
  /** 読み上げ用の文言。画面には表示しない */
  label?: string;
};

export function SkeletonCard({ label = 'データを読み込んでいます', className, ...props }: LabeledSkeletonProps) {
  return (
    <SkeletonStatus label={label} className={cn(cardClassName, 'space-y-4', className)} {...props}>
      <Skeleton className="h-5 w-40" />
      <SkeletonLines lines={3} />
    </SkeletonStatus>
  );
}

export function SkeletonMetricCard({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <SkeletonStatus
      label="データを読み込んでいます"
      className={cn(cardClassName, 'flex flex-col justify-between gap-3', className)}
      {...props}
    >
      <Skeleton className="h-4 w-24" />
      <Skeleton className="h-8 w-20" />
      <Skeleton className="h-3 w-32" />
    </SkeletonStatus>
  );
}

type SkeletonTableProps = HTMLAttributes<HTMLDivElement> & {
  columns?: number;
  rows?: number;
  gridTemplateColumns?: string;
  minWidth?: number;
};

export function SkeletonTable({
  columns = 4,
  rows = 6,
  gridTemplateColumns,
  minWidth,
  className,
  ...props
}: SkeletonTableProps) {
  const gridStyle: CSSProperties = {
    gridTemplateColumns: gridTemplateColumns ?? `repeat(${columns}, minmax(0, 1fr))`,
  };

  return (
    <SkeletonStatus
      label="一覧を読み込んでいます"
      className={cn('overflow-x-auto rounded-lg border border-border bg-card', className)}
      {...props}
    >
      <div style={minWidth ? { minWidth } : undefined}>
        <div className="grid gap-4 border-b border-border bg-muted/40 px-4 py-3" style={gridStyle}>
          {Array.from({ length: columns }, (_, index) => (
            <Skeleton key={index} className="h-3.5 w-2/3" />
          ))}
        </div>
        {Array.from({ length: rows }, (_, rowIndex) => (
          <div
            key={rowIndex}
            className="grid items-center gap-4 border-b border-border px-4 py-4 last:border-b-0"
            style={gridStyle}
          >
            {Array.from({ length: columns }, (_, columnIndex) => (
              <Skeleton
                key={columnIndex}
                className={cn('h-4', lineWidths[(rowIndex + columnIndex) % lineWidths.length])}
              />
            ))}
          </div>
        ))}
      </div>
    </SkeletonStatus>
  );
}

// 棒グラフ風の高さ（%）。毎回同じ形になるよう固定値にする
const chartBarHeights = [45, 70, 55, 85, 60, 75, 50, 90, 65, 80, 55, 70];

export function SkeletonChart({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <SkeletonStatus
      label="グラフを読み込んでいます"
      className={cn(cardClassName, 'flex min-h-[360px] flex-col gap-4', className)}
      {...props}
    >
      <Skeleton className="h-5 w-40" />
      <Skeleton className="h-3.5 w-64 max-w-full" />
      <div className="flex min-h-32 flex-1 items-end gap-2 border-b border-l border-border pb-px pl-px">
        {chartBarHeights.map((height, index) => (
          <Skeleton key={index} className="flex-1 rounded-b-none" style={{ height: `${height}%` }} />
        ))}
      </div>
    </SkeletonStatus>
  );
}

type SkeletonListProps = LabeledSkeletonProps & {
  items?: number;
};

export function SkeletonList({ items = 3, label = '一覧を読み込んでいます', className, ...props }: SkeletonListProps) {
  return (
    <SkeletonStatus label={label} className={cn('space-y-3', className)} {...props}>
      {Array.from({ length: items }, (_, index) => (
        <div key={index} className="flex items-center gap-3 rounded-lg border border-border bg-card p-4">
          <Skeleton className="h-9 w-9 shrink-0 rounded-full" />
          <div className="min-w-0 flex-1 space-y-2">
            <Skeleton className={cn('h-4', index % 2 === 0 ? 'w-1/2' : 'w-2/5')} />
            <Skeleton className={cn('h-3', index % 2 === 0 ? 'w-3/4' : 'w-2/3')} />
          </div>
        </div>
      ))}
    </SkeletonStatus>
  );
}

export function SkeletonAvatar({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <SkeletonStatus label="データを読み込んでいます" className={cn('inline-flex', className)} {...props}>
      <Skeleton className="h-10 w-10 rounded-full" />
    </SkeletonStatus>
  );
}

type PageSkeletonProps = HTMLAttributes<HTMLDivElement> & {
  children?: ReactNode;
  label?: string;
};

/** ページ全体のデータ読み込み。children がなければ、見出し・指標・本文の汎用レイアウトを表示する。 */
export function PageSkeleton({
  children,
  label = 'ページを読み込んでいます',
  className,
  ...props
}: PageSkeletonProps) {
  return (
    <SkeletonStatus label={label} className={cn('w-full px-4 py-page-y sm:px-6', className)} {...props}>
      {children ?? (
        <div className="mx-auto w-full max-w-7xl space-y-6">
          <div className="space-y-3">
            <Skeleton className="h-7 w-48" />
            <Skeleton className="h-4 w-80 max-w-full" />
          </div>
          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
            {Array.from({ length: 4 }, (_, index) => (
              <Skeleton key={index} className="h-[6.5rem] rounded-lg" />
            ))}
          </div>
          <Skeleton className="h-72 rounded-lg" />
        </div>
      )}
    </SkeletonStatus>
  );
}
