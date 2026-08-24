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
  email: string;
  onProfile: () => void;
  onLogout: () => Promise<void>;
};

export function AccountMenu({
  displayName,
  email,
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
          variant="ghost"
          className="group h-12 w-12 gap-2 border border-border px-2 sm:px-3 lg:w-[280px]"
          aria-label={`アカウントメニューを開く：${displayName}`}
        >
          <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-secondary text-primary">
            <User className="h-4 w-4" aria-hidden="true" />
          </span>
          <span className="hidden min-w-0 flex-1 flex-col items-start sm:flex">
            <span
              className="w-full truncate text-left text-sm font-medium leading-5 text-foreground"
              title={displayName}
            >
              {displayName}
            </span>
            <span
              className="w-full truncate text-left text-xs leading-4 text-muted-foreground"
              title={email}
            >
              {email}
            </span>
          </span>
          <ChevronDown
            className={cn(
              'hidden h-4 w-4 shrink-0 text-muted-foreground transition-transform duration-200 motion-reduce:transition-none sm:block',
              isOpen && 'rotate-180',
            )}
            aria-hidden="true"
          />
        </Button>
      </DropdownMenuTrigger>

      <DropdownMenuContent align="end" className="w-72 sm:w-72">
        <DropdownMenuLabel className="min-w-0 font-normal sm:hidden">
          <span className="block truncate text-sm font-medium text-foreground">
            {displayName}
          </span>
          <span className="block truncate text-xs text-muted-foreground">
            {email}
          </span>
        </DropdownMenuLabel>
        <DropdownMenuSeparator className="sm:hidden" />
        <DropdownMenuItem onSelect={onProfile} className="gap-2 py-2">
          <User className="h-4 w-4" aria-hidden="true" />
          <span>プロファイル</span>
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem
          disabled={isLoggingOut}
          onSelect={() => void handleLogout()}
          className="gap-2 py-2 text-destructive focus:bg-destructive/10 focus:text-destructive"
        >
          <LogOut className="h-4 w-4" aria-hidden="true" />
          <span>{isLoggingOut ? 'ログアウト中...' : 'ログアウト'}</span>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
