import type { ReactNode } from 'react';
import {
  CircleAlert,
  CircleCheck,
  Info,
  TriangleAlert,
  X,
} from 'lucide-react';

import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

type NoticeBannerTone =
  | 'info'
  | 'warning'
  | 'success'
  | 'danger';

type NoticeBannerProps = {
  tone?: NoticeBannerTone;
  title?: ReactNode;
  children: ReactNode;
  action?: ReactNode;
  onClose?: () => void;
  className?: string;
};

const noticeBannerStyles: Record<
  NoticeBannerTone,
  {
    container: string;
    icon: string;
    title: string;
  }
> = {
  info: {
    container:
      'border-primary/25 bg-primary/5',
    icon: 'text-primary',
    title: 'text-primary',
  },

  warning: {
    container:
      'border-warning-border bg-warning-subtle',
    icon: 'text-warning-strong',
    title: 'text-warning-strong',
  },

  success: {
    container:
      'border-success-border bg-success-subtle',
    icon: 'text-success-strong',
    title: 'text-success-strong',
  },

  danger: {
    container:
      'border-destructive-border bg-destructive-subtle',
    icon: 'text-destructive-strong',
    title: 'text-destructive-strong',
  },
};

const noticeBannerIcons: Record<
  NoticeBannerTone,
  typeof Info
> = {
  info: Info,
  warning: TriangleAlert,
  success: CircleCheck,
  danger: CircleAlert,
};

export function NoticeBanner({
  tone = 'info',
  title,
  children,
  action,
  onClose,
  className,
}: Readonly<NoticeBannerProps>) {
  const styles = noticeBannerStyles[tone];
  const Icon = noticeBannerIcons[tone];

  const isUrgent =
    tone === 'warning' ||
    tone === 'danger';

  return (
    <section
      role={isUrgent ? 'alert' : undefined}
      className={cn(
        'flex items-start gap-3 rounded-lg border p-4',
        styles.container,
        className,
      )}
    >
      <Icon
        className={cn(
          'mt-0.5 h-5 w-5 shrink-0',
          styles.icon,
        )}
        aria-hidden='true'
      />

      <div className='min-w-0 flex-1'>
        {title ? (
          <p
            className={cn(
              'text-sm font-bold leading-5',
              styles.title,
            )}
          >
            {title}
          </p>
        ) : null}

        <div
          className={cn(
            'text-sm leading-6 text-foreground',
            title && 'mt-1',
          )}
        >
          {children}
        </div>
      </div>

      {action ? (
        <div className='shrink-0'>
          {action}
        </div>
      ) : null}

      {onClose ? (
        <Button
          type='button'
          variant='ghost'
          size='icon'
          className='-mr-2 -mt-2 h-8 w-8 shrink-0'
          onClick={onClose}
          aria-label='お知らせを閉じる'
        >
          <X
            className='h-4 w-4'
            aria-hidden='true'
          />
        </Button>
      ) : null}
    </section>
  );
}