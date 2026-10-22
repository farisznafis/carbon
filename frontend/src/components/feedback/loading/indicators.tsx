import type { HTMLAttributes } from 'react';
import { Leaf, LoaderCircle } from 'lucide-react';
import { cn } from '@/lib/utils';

type AppLoadingScreenProps = HTMLAttributes<HTMLDivElement> & {
  message?: string;
  secondaryMessage?: string;
};

type BrandedLoadingMarkProps = {
  message: string;
  secondaryMessage?: string;
  compact?: boolean;
};

/** Renders the shared leaf and circular data-flow animation with its loading copy. */
function BrandedLoadingMark({ message, secondaryMessage, compact = false }: BrandedLoadingMarkProps) {
  return (
    <div className="flex flex-col items-center">
      <div
        className={cn('relative flex items-center justify-center', compact ? 'h-20 w-20' : 'h-28 w-28')}
        aria-hidden="true"
      >
        <svg className="absolute inset-0 h-full w-full" viewBox="0 0 112 112" fill="none">
          <circle cx="56" cy="56" r="43" stroke="hsl(var(--primary) / 0.24)" strokeWidth="1.5" />
        </svg>
        <span className="loading-orbit absolute inset-0 motion-reduce:animate-none">
          <span className="absolute left-1/2 top-[6px] h-2.5 w-2.5 -translate-x-1/2 rounded-full bg-primary shadow-app-sm" />
        </span>
        <span className={cn('loading-leaf-breathe flex items-center justify-center rounded-full bg-card text-primary-strong shadow-app-md motion-reduce:animate-none', compact ? 'h-10 w-10' : 'h-14 w-14')}>
          <Leaf className={compact ? 'h-6 w-6' : 'h-8 w-8'} strokeWidth={1.7} />
        </span>
      </div>
      <p className={cn('font-bold text-foreground', compact ? 'mt-3 text-small' : 'mt-6 text-large')}>{message}</p>
      {secondaryMessage ? <p className="mt-2 text-small text-muted-foreground">{secondaryMessage}</p> : null}
    </div>
  );
}

/** Renders the branded loader used only while the application context is restored. */
export function AppLoadingScreen({
  message = 'データを読み込んでいます',
  secondaryMessage = '少々お待ちください',
  className,
  ...props
}: AppLoadingScreenProps) {
  return (
    <div
      role="status"
      aria-live="polite"
      aria-busy="true"
      className={cn('flex min-h-dvh items-center justify-center bg-background px-6 text-center', className)}
      {...props}
    >
      <BrandedLoadingMark message={message} secondaryMessage={secondaryMessage} />
    </div>
  );
}

type PageLoadingOverlayProps = HTMLAttributes<HTMLDivElement> & {
  message?: string;
  secondaryMessage?: string;
};

/** Blocks page interaction and shows branded motion without replacing the application shell. */
export function PageLoadingOverlay({
  message = 'ページを読み込んでいます',
  secondaryMessage = '少々お待ちください',
  className,
  ...props
}: PageLoadingOverlayProps) {
  return (
    <div
      role="status"
      aria-live="polite"
      aria-busy="true"
      aria-disabled="true"
      data-page-interactions-disabled="true"
      className={cn(
        'flex min-h-[calc(100dvh-var(--app-header-height))] cursor-wait select-none items-center justify-center bg-background/95 px-6 text-center',
        className,
      )}
      {...props}
    >
      <BrandedLoadingMark message={message} secondaryMessage={secondaryMessage} />
    </div>
  );
}

type InlineLoaderProps = HTMLAttributes<HTMLDivElement> & {
  label?: string;
  showLabel?: boolean;
};

/** Renders a compact status indicator for one independently loading control. */
export function InlineLoader({
  label = '読み込んでいます',
  showLabel = true,
  className,
  ...props
}: InlineLoaderProps) {
  return (
    <div
      role="status"
      aria-live="polite"
      className={cn('inline-flex items-center gap-2 text-small text-muted-foreground', className)}
      {...props}
    >
      <LoaderCircle className="h-4 w-4 animate-spin motion-reduce:animate-none" aria-hidden="true" />
      {showLabel ? <span>{label}</span> : <span className="sr-only">{label}</span>}
    </div>
  );
}

/** Renders the decorative spinner placed beside a pending button label. */
export function ButtonLoader({ className, ...props }: HTMLAttributes<HTMLSpanElement>) {
  return (
    <span className={cn('inline-flex items-center', className)} aria-hidden="true" {...props}>
      <LoaderCircle className="h-4 w-4 animate-spin motion-reduce:animate-none" />
    </span>
  );
}
