import { cn } from '@/lib/utils';

export const dashboardPageContainerClassName =
  'mx-auto w-full max-w-app px-4 sm:px-6 lg:px-0';

export const dashboardWidePageContainerClassName =
'mx-auto w-full max-w-page px-4 sm:px-6 lg:px-8';

export const adminPageContainerClassName =
  'mx-auto w-full max-w-admin px-4 sm:px-6 lg:px-gutter';

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