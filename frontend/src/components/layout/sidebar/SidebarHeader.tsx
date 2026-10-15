import { PanelLeftClose, PanelLeftOpen } from 'lucide-react';
import { Link } from 'react-router-dom';

import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

type SidebarHeaderProps = {
  homeHref: string;
  collapsed: boolean;
  onToggleCollapsed: () => void;
};


/**
 * Expanded: logo on the left, collapse toggle on the right.
 * Collapsed: the toggle takes the logo's place; ホーム stays reachable from the nav.
 */
export function SidebarHeader({
  homeHref,
  collapsed,
  onToggleCollapsed,
}: Readonly<SidebarHeaderProps>) {
  return (
    <div
      className={cn(
        'flex h-header shrink-0 items-center gap-2 px-3',
        'border-b border-sidebar-border',
        collapsed ? 'justify-center' : 'justify-between',
      )}
    >
      {!collapsed && (
        <Link
          to={homeHref}
          aria-label="ホーム"
          className="ml-1.5 flex min-w-0 items-center gap-3"
        >
          <div className="h-9 w-9 shrink-0 rounded-lg gradient-primary" />
          <span className="truncate text-xl font-bold text-foreground">
            GX
          </span>
        </Link>
      )}

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
        title={collapsed ? 'サイドバーを展開' : 'サイドバーを折りたたむ'}
        aria-expanded={!collapsed}
        className="h-9 w-9 shrink-0 text-muted-foreground hover:bg-muted hover:text-foreground"
      >
        {collapsed ? (
          <PanelLeftOpen className="h-5 w-5" aria-hidden="true" />
        ) : (
          <PanelLeftClose className="h-5 w-5" aria-hidden="true" />
        )}
      </Button>
    </div>
  );
}
