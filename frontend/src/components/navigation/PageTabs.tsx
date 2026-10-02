import { forwardRef, type ComponentPropsWithoutRef, type ElementRef } from 'react';
import { TabsList, TabsTrigger } from '@/components/ui/tabs';
import { cn } from '@/lib/utils';

/**
 * Underlined page-level tabs that switch a page's main sections
 * (プロジェクト: 管理 / アナリティクス, プロジェクト詳細: 概要 / …).
 * Filters inside a section keep the segmented `TabsList` style instead.
 */
export const PageTabsList = forwardRef<
  ElementRef<typeof TabsList>,
  ComponentPropsWithoutRef<typeof TabsList>
>(({ className, ...props }, ref) => (
  <TabsList
    ref={ref}
    className={cn(
      'flex h-auto w-full justify-start gap-6 overflow-x-auto rounded-none border-b border-border bg-transparent p-0',
      className,
    )}
    {...props}
  />
));
PageTabsList.displayName = 'PageTabsList';

export const PageTabsTrigger = forwardRef<
  ElementRef<typeof TabsTrigger>,
  ComponentPropsWithoutRef<typeof TabsTrigger>
>(({ className, ...props }, ref) => (
  <TabsTrigger
    ref={ref}
    className={cn(
      'h-11 shrink-0 rounded-none border-b-2 border-transparent bg-transparent px-1 py-2 text-body text-muted-foreground shadow-none hover:text-foreground data-[state=active]:border-primary data-[state=active]:bg-transparent data-[state=active]:font-semibold data-[state=active]:text-foreground data-[state=active]:shadow-none focus-visible:ring-2 focus-visible:ring-ring',
      className,
    )}
    {...props}
  />
));
PageTabsTrigger.displayName = 'PageTabsTrigger';
