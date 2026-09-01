import type { ReactNode } from 'react';
import { InlineHelp } from '@/components/feedback/InlineHelp/InlineHelp';

type ProfileDetailFieldProps = {
  label: string;
  value?: ReactNode;
  helpText?: string;
};

/** Displays one consistently styled label and value in a read-only profile section. */
export function ProfileDetailField({
  label,
  value,
  helpText,
}: Readonly<ProfileDetailFieldProps>) {
  const hasValue = value !== null && value !== undefined && value !== '';

  return (
    <div className="min-w-0 space-y-1">
      <dt className="flex items-center gap-1 text-sm font-medium text-muted-foreground">
        {label}
        {helpText ? <InlineHelp content={helpText} /> : null}
      </dt>
      <dd className="break-words font-medium text-foreground">
        {hasValue ? value : '-'}
      </dd>
    </div>
  );
}
