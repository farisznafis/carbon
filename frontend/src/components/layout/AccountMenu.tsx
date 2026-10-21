import { useState } from 'react';
import { ChevronDown, LogOut, User } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { cn } from '@/lib/utils';

type AccountMenuProps = {
  displayName: string;
  avatarInitial: string | null;
  email: string;
  roleLabel: string;
  onProfile: () => void | Promise<void>;
  onLogout: () => Promise<void>;
};

export function AccountMenu({
  displayName,
  avatarInitial,
  email,
  roleLabel,
  onProfile,
  onLogout,
}: Readonly<AccountMenuProps>) {
  const [isOpen, setIsOpen] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  const handleLogout = async () => {
    if (isLoggingOut) return;

    setIsLoggingOut(true);
    try {
      await onLogout();
    } finally {
      setIsLoggingOut(false);
    }
  };

  return (
    <DropdownMenu open={isOpen} onOpenChange={setIsOpen}>
      <DropdownMenuTrigger asChild>
        <Button
          type="button"
          data-tour="account-menu"
          variant="ghost"
          className="group h-12 w-12 gap-2 border border-border px-2 sm:px-3 lg:w-fit"
          aria-label={`アカウントメニューを開く：${displayName}`}
        >
          <span
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-accent text-sm font-medium text-primary-strong"
            aria-hidden="true"
          >
            {avatarInitial ?? <User className="h-4 w-4" />}
          </span>
          <span className="hidden min-w-0 max-w-56 flex-col items-start lg:flex">
            <span
              className="w-full truncate text-left text-sm font-bold leading-5 text-foreground"
              title={displayName}
            >
              {displayName}
            </span>
            <span
              className="w-full truncate text-left text-sm leading-4 text-muted-foreground"
              title={roleLabel}
            >
              {roleLabel}
            </span>
          </span>
          <ChevronDown
            className={cn(
              'hidden h-4 w-4 shrink-0 text-muted-foreground transition-transform duration-200 motion-reduce:transition-none lg:block',
              isOpen && 'rotate-180',
            )}
            aria-hidden="true"
          />
        </Button>
      </DropdownMenuTrigger>

      <DropdownMenuContent align="end" className="w-80 max-w-[calc(100vw-2rem)] p-2">
        <DropdownMenuLabel className="min-w-0 px-3 py-3 font-normal">
          <span className="block truncate text-base font-bold text-foreground">
            {displayName}
          </span>
          <span className="mt-0.5 block truncate text-sm text-muted-foreground">
            {email}
          </span>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem
          onSelect={() => void onProfile()}
          className="gap-3 px-3 py-3"
        >
          <User className="h-4 w-4" aria-hidden="true" />
          <span>プロファイル</span>
        </DropdownMenuItem>
        <DropdownMenuItem
          disabled={isLoggingOut}
          onSelect={() => void handleLogout()}
          className="mt-1 gap-3 px-3 py-3 text-destructive-strong focus:bg-destructive/10 focus:text-destructive-strong"
        >
          <LogOut className="h-4 w-4" aria-hidden="true" />
          <span>{isLoggingOut ? 'ログアウト中...' : 'ログアウト'}</span>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
