import { cn } from '@/lib/utils';

export const dashboardPageContainerClassName =
  'mx-auto w-full max-w-app px-4 sm:px-6 lg:px-0';

export const adminPageContainerClassName =
  'mx-auto w-full max-w-admin px-4 sm:px-6 lg:px-8';

export const dashboardTabsListClassName =
  'mx-auto flex h-auto w-fit max-w-full flex-wrap items-center justify-center gap-2 rounded-none bg-transparent p-0';

export const dashboardTabTriggerClassName = cn(
  'h-10 min-w-32 rounded-xl border border-border bg-card px-4 py-2',
  'text-sm font-normal text-foreground shadow-app-sm transition-all duration-300 ease-in-out',
  'hover:border-primary/60 hover:bg-secondary/50',
  'data-[state=active]:border-primary data-[state=active]:bg-primary data-[state=active]:text-primary-foreground',
  'data-[state=active]:scale-105 data-[state=active]:hover:bg-primary/90',
);

export const dashboardPageHeaderClassName = cn(
  'flex min-h-36 items-center justify-center text-center',
  dashboardPageContainerClassName,
);

export const dashboardEmptyStateClassName =
  'rounded-xl border border-dashed border-border bg-card px-6 py-8 text-center text-small text-foreground';
