import { Link } from 'react-router-dom';
import { Portal as TooltipPortal } from '@radix-ui/react-tooltip';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { cn } from '@/lib/utils';
import type { SidebarNavigationItem } from './sidebarTypes';

type SidebarNavItemProps = {
  item: SidebarNavigationItem;
  collapsed: boolean;
  onNavigate?: () => void;
};

/** Renders one accessible destination in either sidebar width. */
export function SidebarNavItem({
  item,
  collapsed,
  onNavigate,
}: SidebarNavItemProps) {
  const Icon = item.icon;
  const link = (
    <Link
      to={item.href}
      onClick={onNavigate}
      aria-label={collapsed ? item.label : undefined}
      aria-current={item.isActive ? 'page' : undefined}
      className={cn(
        'relative flex min-h-11 items-center gap-3 rounded-lg px-3.5 text-sm text-sidebar-foreground transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
        collapsed && 'justify-center px-0',
        item.isActive && (
          'bg-sidebar-accent font-semibold text-sidebar-accent-foreground '
          // 'before:absolute before:bottom-2 before:left-0 before:top-2 ' +
          // 'before:w-1 before:rounded-full before:bg-primary'
        ),
      )}
    >
      <Icon
        className='h-5 w-5 shrink-0'
        aria-hidden='true'
      />
      {!collapsed && <span>{item.label}</span>}
    </Link>
  );

  return (
    <li>
      {collapsed ? (
        <Tooltip>
          <TooltipTrigger asChild>{link}</TooltipTrigger>
          <TooltipPortal>
            <TooltipContent side='right'>{item.label}</TooltipContent>
          </TooltipPortal>
        </Tooltip>
      ) : (
        link
      )}
    </li>
  );
}
