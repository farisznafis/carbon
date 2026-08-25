import type { HTMLAttributes, ReactNode } from 'react';
import { LoadingPanel, PageLoadingOverlay } from './indicators';

// Skeleton visual dinonaktifkan sementara sampai bentuknya disesuaikan dengan setiap halaman.
// Public API tetap dipertahankan agar seluruh loading state memakai animasi tanpa migrasi berisiko.

type SkeletonTextProps = HTMLAttributes<HTMLDivElement> & {
  lines?: number;
};

/** Temporarily renders animated loading in place of text skeleton lines. */
export function SkeletonText({ lines: _lines, ...props }: SkeletonTextProps) {
  return <LoadingPanel message="データを読み込んでいます" {...props} />;
}

/** Temporarily renders animated loading in place of a card skeleton. */
export function SkeletonCard(props: HTMLAttributes<HTMLDivElement>) {
  return <LoadingPanel {...props} />;
}

/** Temporarily renders animated loading in place of a metric-card skeleton. */
export function SkeletonMetricCard(props: HTMLAttributes<HTMLDivElement>) {
  return <LoadingPanel className="min-h-36" {...props} />;
}

type SkeletonTableProps = HTMLAttributes<HTMLDivElement> & {
  columns?: number;
  rows?: number;
  gridTemplateColumns?: string;
  minWidth?: number;
};

/** Temporarily renders one animated panel in place of a table skeleton. */
export function SkeletonTable({
  columns: _columns,
  rows: _rows,
  gridTemplateColumns: _gridTemplateColumns,
  minWidth: _minWidth,
  ...props
}: SkeletonTableProps) {
  return <LoadingPanel message="一覧を読み込んでいます" className="min-h-80" {...props} />;
}

/** Temporarily renders one animated panel in place of a chart skeleton. */
export function SkeletonChart(props: HTMLAttributes<HTMLDivElement>) {
  return <LoadingPanel message="グラフを読み込んでいます" className="min-h-[360px]" {...props} />;
}

type SkeletonListProps = HTMLAttributes<HTMLDivElement> & {
  items?: number;
};

/** Temporarily renders one animated panel in place of a list skeleton. */
export function SkeletonList({ items: _items, ...props }: SkeletonListProps) {
  return <LoadingPanel message="一覧を読み込んでいます" className="min-h-48" {...props} />;
}

/** Temporarily renders compact animation in place of an avatar skeleton. */
export function SkeletonAvatar(props: HTMLAttributes<HTMLDivElement>) {
  return <LoadingPanel className="min-h-24" {...props} />;
}

type PageSkeletonProps = HTMLAttributes<HTMLDivElement> & {
  children?: ReactNode;
  label?: string;
};

/** Temporarily routes legacy page-skeleton calls to the branded page animation. */
export function PageSkeleton({
  children: _children,
  label = 'ページを読み込んでいます',
  ...props
}: PageSkeletonProps) {
  return <PageLoadingOverlay message={label} {...props} />;
}
