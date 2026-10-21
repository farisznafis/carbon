import type { ReactNode } from 'react';
import { SkeletonChart } from '@/components/feedback/loading';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { cn } from '@/lib/utils';

type ChartCardProps = {
  title: string;
  description?: ReactNode;
  /** Short headline shown next to the title, e.g. a total. */
  headline?: ReactNode;
  isLoading?: boolean;
  error?: string | null;
  isEmpty?: boolean;
  emptyMessage?: ReactNode;
  children: ReactNode;
  className?: string;
};

/**
 * Shared card shell for dashboard charts, so loading, error and empty states
 * look identical wherever a chart is shown.
 */
export function ChartCard({
  title,
  description,
  headline,
  isLoading = false,
  error = null,
  isEmpty = false,
  emptyMessage = '表示できるデータはまだありません。',
  children,
  className,
}: Readonly<ChartCardProps>) {
  const renderBody = () => {
    if (isLoading) return <SkeletonChart className="min-h-72" />;
    if (error) {
      return (
        <p role="alert" className="py-12 text-center text-small text-destructive-strong">
          {error}
        </p>
      );
    }
    if (isEmpty) {
      return (
        <p className="py-12 text-center text-small text-muted-foreground">
          {emptyMessage}
        </p>
      );
    }
    return children;
  };

  return (
    <Card
      className={cn(
        'flex h-full flex-col rounded-lg border-border',
        className,
      )}
    >
      <CardHeader className="gap-1.5">
        <div className="flex flex-wrap items-baseline justify-between gap-2">
          <CardTitle>{title}</CardTitle>
          {headline}
        </div>
        {description && <CardDescription>{description}</CardDescription>}
      </CardHeader>
      <CardContent className="flex flex-1 flex-col">{renderBody()}</CardContent>
    </Card>
  );
}
