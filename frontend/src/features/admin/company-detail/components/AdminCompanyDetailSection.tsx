import type { ReactNode } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { cn } from '@/lib/utils';
import { displayCompanyDetailValue } from '../companyDetailDisplay';

export type AdminCompanyDetailField = {
  label: string;
  value?: string | number | null;
  className?: string;
  content?: ReactNode;
};

type AdminCompanyDetailSectionProps = {
  title: string;
  icon: ReactNode;
  fields: AdminCompanyDetailField[];
  className?: string;
};

export function AdminCompanyDetailSection({
  title,
  icon,
  fields,
  className,
}: AdminCompanyDetailSectionProps) {
  return (
    <Card className={cn('rounded-lg', className)}>
      <CardHeader className="flex-row items-center gap-3 space-y-0 pb-4">
        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary text-primary-foreground">
          {icon}
        </span>
        <CardTitle className="text-large font-bold text-foreground">
          {title}
        </CardTitle>
      </CardHeader>
      <CardContent className="grid grid-cols-1 gap-x-8 gap-y-5 md:grid-cols-2">
        {fields.map((field) => (
          <div
            key={`${title}-${field.label}`}
            className={cn('min-w-0 space-y-1', field.className)}
          >
            <p className="text-small font-bold text-foreground">
              {field.label}
            </p>
            {field.content ?? (
              <p className="break-words text-body font-bold text-foreground">
                {displayCompanyDetailValue(field.value)}
              </p>
            )}
          </div>
        ))}
      </CardContent>
    </Card>
  );
}
