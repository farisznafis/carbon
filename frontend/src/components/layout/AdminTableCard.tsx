import type { ReactNode, Ref } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { cn } from '@/lib/utils';

type AdminTableCardProps = {
  title: string;
  count?: number;
  description?: ReactNode;
  /** Primary action aligned with the title, e.g. an "add" button. */
  action?: ReactNode;
  notice?: ReactNode;
  filters?: ReactNode;
  search?: ReactNode;
  footer?: ReactNode;
  children: ReactNode;
  headingRef?: Ref<HTMLHeadingElement>;
  className?: string;
};

/** Provides shared admin table, filter, and search geometry while each feature owns its content. */
export function AdminTableCard({
  title,
  count,
  description,
  action,
  notice,
  filters,
  search,
  footer,
  children,
  headingRef,
  className,
}: Readonly<AdminTableCardProps>) {
  return (
    <Card
      className={cn(
        'rounded-lg border-border bg-card shadow-app-sm',
        className,
      )}
    >
      <CardContent className="p-card">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="min-w-0">
            <div className="flex flex-wrap items-baseline gap-3">
              <h2
                ref={headingRef}
                tabIndex={headingRef ? -1 : undefined}
                className={cn(
                  'text-subsection-title font-bold text-foreground',
                  headingRef &&
                    'scroll-mt-24 outline-none focus-visible:ring-2 focus-visible:ring-ring',
                )}
              >
                {title}
              </h2>

              {count !== undefined && (
                <span className="text-sm font-medium text-foreground">
                  {count.toLocaleString('ja-JP')}件
                </span>
              )}
            </div>

            {description && (
              <div className="mt-1.5 text-sm leading-6 text-foreground">
                {description}
              </div>
            )}
          </div>

          {action && <div className="shrink-0">{action}</div>}
        </div>

        {notice && <div className="mt-4">{notice}</div>}
        {(filters || search) && (
          <div className="mt-6 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            {filters && (
              <div className="flex w-full flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center lg:w-auto [&>*]:h-10 [&>*]:w-full [&>*]:shrink-0 sm:[&>*]:w-auto sm:[&>*]:min-w-40">
                {filters}
              </div>
            )}
            {search && (
              <div className="relative w-full lg:max-w-md">{search}</div>
            )}
          </div>
        )}
        <div className="mt-5">{children}</div>
        {footer && <div className="mt-4">{footer}</div>}
      </CardContent>
    </Card>
  );
}
