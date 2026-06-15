import type { ReactNode } from 'react';
import { Hash } from 'lucide-react';

type ProjectDetailItemProps = {
  label: string;
  value: ReactNode;
  icon?: ReactNode;
  fullWidth?: boolean;
};

export function ProjectDetailItem({
  label,
  value,
  icon,
  fullWidth = false,
}: ProjectDetailItemProps) {
  return (
    <div className={fullWidth ? 'md:col-span-2' : ''}>
      <div className='flex items-start gap-3'>
        <span className='mt-1 inline-flex h-7 w-7 items-center justify-center rounded bg-muted shrink-0'>
          {icon ?? <Hash className='h-4 w-4 text-muted-foreground' />}
        </span>
        <div className='min-w-0'>
          <dt className='text-xs text-muted-foreground tracking-wide'>
            {label}
          </dt>
          <dd className='font-medium break-words'>{value}</dd>
        </div>
      </div>
    </div>
  );
}