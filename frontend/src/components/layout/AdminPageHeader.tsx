import type { ReactNode } from 'react';
import { cn } from '@/lib/utils';

type AdminPageHeaderProps = {
  title: string;
  description?: string;
  action?: ReactNode;
  className?: string;
  headingAs?: 'h1' | 'h2';
};

/** Renders a consistent primary page heading across the admin application. */
export function AdminPageHeader({
  title,
  description,
  action,
  className,
  headingAs: Heading = 'h1',
}: Readonly<AdminPageHeaderProps>) {
  return (
    <header
      className={cn(
        'flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between',
        className,
      )}
    >
      <div>
        <Heading className="text-page-title text-foreground">{title}</Heading>
        {description && (
          <p className="mt-2 text-base text-foreground">{description}</p>
        )}
      </div>
      {action && <div className="shrink-0 self-start sm:self-auto">{action}</div>}
    </header>
  );
}
