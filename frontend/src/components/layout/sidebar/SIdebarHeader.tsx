import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Link } from 'react-router-dom';

import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

type SidebarHeaderProps = {
  homeHref: string;
  collapsed: boolean;
  onToggleCollapsed: () => void;
};

/** Keeps the product mark aligned with sidebar icons in both desktop widths. */
export function SidebarHeader({
  homeHref,
  collapsed,
  onToggleCollapsed,
}: Readonly<SidebarHeaderProps>) {
  return (
    <div
      className={cn(
        'relative flex h-20 shrink-0 items-center',
        'border-b border-sidebar-border',
        collapsed ? 'justify-center px-3' : 'px-3',
      )}
    >
      <Link
        to={homeHref}
        aria-label="ホーム"
        className={cn(
          'flex min-w-0 items-center',
          collapsed ? 'justify-center' : 'ml-1.5 gap-3',
        )}
      >
        <div className="h-9 w-9 shrink-0 rounded-lg gradient-primary" />

        {!collapsed && (
          <span className="truncate text-xl font-bold text-foreground">
            GX
          </span>
        )}
      </Link>

      <Button
        type="button"
        variant="ghost"
        size="icon"
        onClick={onToggleCollapsed}
        aria-label={
          collapsed
            ? 'サイドバーを展開'
            : 'サイドバーを折りたたむ'
        }
        aria-expanded={!collapsed}
        className={cn(
          'absolute right-0 top-1/2 z-30',
          'h-8 w-8',
          'translate-x-1/2 -translate-y-1/2',
          'rounded-full',
          'border border-border',
          'bg-card',
          'shadow-app-sm',
          'hover:bg-muted',
          'focus-visible:outline-none',
          'focus-visible:ring-2',
          'focus-visible:ring-ring',
          'focus-visible:ring-offset-2',
        )}
      >
        {collapsed ? (
          <ChevronRight
            className="h-4 w-4"
            aria-hidden="true"
          />
        ) : (
          <ChevronLeft
            className="h-4 w-4"
            aria-hidden="true"
          />
        )}
      </Button>
    </div>
  );
}
