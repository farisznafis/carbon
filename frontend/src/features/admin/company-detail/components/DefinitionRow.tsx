import type { ReactNode } from 'react';

/** Definition-list row: label left / top, value right / bottom */
export function DefinitionRow({
  label,
  children,
}: {
  label: string;
  children: ReactNode;
}) {
  return (
    <div className="grid grid-cols-1 gap-0.5 sm:grid-cols-[180px_1fr] sm:gap-2 sm:items-start py-2 border-b border-border/50 last:border-0">
      <dt className="text-sm text-foreground">{label}</dt>
      <dd className="text-sm font-medium text-foreground break-words">{children}</dd>
    </div>
  );
}
