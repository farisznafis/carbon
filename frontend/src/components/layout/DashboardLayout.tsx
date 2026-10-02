import { cn } from '@/lib/utils';

export const dashboardPageContainerClassName =
  'mx-auto w-full max-w-app px-4 sm:px-6 lg:px-0';

export const dashboardWidePageContainerClassName =
'mx-auto w-full max-w-page px-4 sm:px-6 lg:px-8';

export const adminPageContainerClassName =
  'mx-auto w-full max-w-admin px-4 sm:px-6 lg:px-gutter';

export const dashboardTabsListClassName =
  'mx-auto flex h-auto w-fit max-w-full flex-wrap items-center justify-center gap-2 rounded-none bg-transparent p-0';

export const dashboardTabTriggerClassName = cn(
  'h-10 min-w-32 rounded-xl border border-border bg-card px-4 py-2',
  'text-sm font-medium text-foreground shadow-app-sm',
  'transition-colors duration-150',
  'hover:border-primary/60 hover:bg-secondary/50',
  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
  'data-[state=active]:border-primary',
  'data-[state=active]:bg-primary',
  'data-[state=active]:text-primary-foreground',
  'data-[state=active]:hover:bg-primary-strong',
);

export const dashboardPageHeaderClassName = cn(
  'flex min-h-36 items-center justify-center text-center xl:min-h-[clamp(8rem,9vw,9rem)]',
  dashboardPageContainerClassName,
);

export const dashboardEmptyStateClassName =
  'rounded-xl border border-dashed border-border bg-card px-6 py-8 text-center text-small text-foreground';

export const adminHomePageContainerClassName =
`
  mx-auto
  w-[83.333vw]
  max-w-[1600px]
  px-[clamp(21.333px,1.667vw,32px)]
`;