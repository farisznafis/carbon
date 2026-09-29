import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Link } from 'react-router-dom';

import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

type SidebarHeaderProps = {
  homeHref: string;
  collapsed: boolean;
  onToggleCollapsed: () => void;
};

/**
 * Renders the desktop product identity and sidebar collapse control.
 *
 * Expanded:
 * [ Logo GX                  < ]
 *
 * Collapsed:
 * [ Logo > ]
 */
export function SidebarHeader({
  homeHref,
  collapsed,
  onToggleCollapsed,
}: Readonly<SidebarHeaderProps>) {
  return (
    <div
      className={cn(
        'flex h-20 shrink-0 items-center border-b border-sidebar-border',
        collapsed ? 'justify-center gap-1 px-1' : 'gap-3 px-5',
      )}
    >
      <Link
        to={homeHref}
        aria-label="ホーム"
        className={cn(
          'flex min-w-0 items-center',
          collapsed ? 'justify-center' : 'gap-3',
        )}
      >
        <div
          className={cn(
            'shrink-0 rounded-lg gradient-primary',
            collapsed ? 'h-7 w-7' : 'h-9 w-9',
          )}
        />

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
          'shrink-0 rounded-full border border-border bg-card shadow-app-sm',
          'hover:bg-muted',
          collapsed ? 'h-7 w-7' : 'ml-auto h-8 w-8',
        )}
      >
        {collapsed ? (
          <ChevronRight
            className="h-3.5 w-3.5"
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