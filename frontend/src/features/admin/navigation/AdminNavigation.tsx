import { Link, useLocation } from 'react-router-dom';
import { Menu } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet';
import { cn } from '@/lib/utils';
import {
  ADMIN_NAVIGATION_ITEMS,
  getActiveAdminNavigationKey,
} from './adminNavigationConfig';

type AdminNavigationLinksProps = {
  mobile?: boolean;
};

/** Renders the route-aware admin navigation links for desktop or mobile. */
function AdminNavigationLinks({ mobile = false }: AdminNavigationLinksProps) {
  const location = useLocation();
  const legacyTab = new URLSearchParams(location.search).get('tab');
  const activeKey = getActiveAdminNavigationKey(location.pathname, legacyTab);

  return (
    <div
      className={cn(
        mobile ? 'flex flex-col gap-1' : 'flex h-full items-stretch justify-center',
      )}
      aria-label="管理画面のメインナビゲーション"
    >
      {ADMIN_NAVIGATION_ITEMS.map((item) => {
        const isActive = item.key === activeKey;
        const link = (
          <Link
            to={item.href}
            aria-current={isActive ? 'page' : undefined}
            className={cn(
              'relative flex items-center whitespace-nowrap text-sm text-foreground transition-colors hover:text-primary',
              mobile
                ? 'min-h-11 rounded-lg px-4 py-2'
                : 'min-w-24 justify-center px-3 xl:min-w-28 2xl:min-w-32',
              isActive && 'font-bold text-primary',
              mobile && isActive && 'bg-secondary/60',
              !mobile &&
                isActive &&
                'after:absolute after:inset-x-4 after:bottom-0 after:h-1 after:rounded-t-full after:bg-primary',
            )}
          >
            {item.label}
          </Link>
        );

        return mobile ? (
          <SheetClose asChild key={item.key}>
            {link}
          </SheetClose>
        ) : (
          <div key={item.key} className="h-full">
            {link}
          </div>
        );
      })}
    </div>
  );
}

/** Provides the desktop admin navigation and a compact drawer on smaller screens. */
export function AdminNavigation() {
  return (
    <>
      <div className="hidden h-14 xl:block">
        <AdminNavigationLinks />
      </div>

      <Sheet>
        <SheetTrigger asChild>
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="h-12 w-12 border border-border xl:hidden"
            aria-label="管理メニューを開く"
          >
            <Menu className="h-5 w-5" aria-hidden="true" />
          </Button>
        </SheetTrigger>
        <SheetContent side="left" className="w-[min(88vw,360px)]">
          <SheetHeader className="text-left">
            <SheetTitle>管理メニュー</SheetTitle>
          </SheetHeader>
          <nav className="mt-6">
            <AdminNavigationLinks mobile />
          </nav>
        </SheetContent>
      </Sheet>
    </>
  );
}
