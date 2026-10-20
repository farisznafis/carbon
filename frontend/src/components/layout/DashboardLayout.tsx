import { cn } from '@/lib/utils';

// The gutter is added to max-width too, so wide monitors keep the same 77.5rem
// content width while laptops get breathing room from the sidebar and edge.
export const dashboardPageContainerClassName =
  'mx-auto w-full max-w-app px-4 sm:px-6 lg:max-w-[calc(77.5rem+2*var(--page-gutter))] lg:px-gutter';

export const dashboardWidePageContainerClassName =
'mx-auto w-full max-w-page px-4 sm:px-6 lg:px-8';

export const adminPageContainerClassName =
  'mx-auto w-full max-w-admin px-4 sm:px-6 lg:px-gutter';

export const dashboardPageHeaderClassName = cn(
  'flex min-h-36 items-center justify-center text-center xl:min-h-[clamp(8rem,9vw,9rem)]',
  dashboardPageContainerClassName,
);

export const dashboardEmptyStateClassName =
  'rounded-lg border border-dashed border-border bg-card px-6 py-8 text-center text-small text-foreground';

export const adminHomePageContainerClassName =
`
  mx-auto
  w-[83.333vw]
  max-w-[1600px]
  px-[clamp(21.333px,1.667vw,32px)]
`;