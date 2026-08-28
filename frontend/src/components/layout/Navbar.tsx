import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Bell, X } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { NotificationsList } from '@/components/feedback/NotificationsList';
import { Button } from '@/components/ui/button';
import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet';
import { useNotifications } from '@/hooks/useNotifications';
import { toast } from '@/hooks/use-toast';
import { getHomePath } from '@/utils/getHomePath';
import { isLoginPath, paths } from '@/routes/paths';
import { cn } from '@/lib/utils';
import { AdminNavigation } from '@/features/admin/navigation/AdminNavigation';
import { AccountMenu } from './AccountMenu';
import { useAccountIdentity } from './useAccountIdentity';

type AuthenticatedNavbarContentProps = {
  user: NonNullable<ReturnType<typeof useAuth>['user']>;
  onLogout: () => Promise<void>;
};

function AuthenticatedNavbarContent({
  user,
  onLogout,
}: Readonly<AuthenticatedNavbarContentProps>) {
  const navigate = useNavigate();
  const { displayName, email, roleLabel, resolveProfilePath } = useAccountIdentity(user);

  /** Opens the matched admin operator profile without guessing an account. */
  const handleProfile = async () => {
    try {
      const profilePath = await resolveProfilePath();
      if (profilePath) {
        navigate(profilePath);
        return;
      }
    } catch {
      // The shared message below also covers API failures while resolving the account.
    }

    toast({
      title: 'エラー',
      description: 'プロファイルを表示できませんでした。',
      variant: 'destructive',
    });
  };

  return (
    <AccountMenu
      displayName={displayName}
      email={email}
      roleLabel={roleLabel}
      onProfile={handleProfile}
      onLogout={onLogout}
    />
  );
}

export const Navbar = () => {
  const { user, logout, isAuthenticated } = useAuth();
  const {
    notifications,
    loading: notificationsLoading,
    error: notificationsError,
    unreadCount,
    markAsRead,
    deleteNotification,
  } = useNotifications();
  const navigate = useNavigate();
  const location = useLocation();

  const linkTarget = getHomePath(user);
  const isLoginPage = isLoginPath(location.pathname);
  const isAdminLoginPage =
    isLoginPage && location.pathname.startsWith(paths.adminLogin);
  const isAdminAuthenticated = isAuthenticated && user?.role === 'admin';

  const handleLogout = async () => {
    const redirectPath =
      user?.role === 'admin' ? paths.adminLogin : paths.home;

    await logout();
    navigate(redirectPath, { replace: true });
  };

  const handleMarkAsRead = async (
    notificationId: Parameters<typeof markAsRead>[0],
  ) => {
    try {
      await markAsRead(notificationId);
    } catch {
      toast({
        title: 'エラー',
        description: '通知を既読にできませんでした。',
        variant: 'destructive',
      });
    }
  };

  const handleDeleteNotification = async (
    notificationId: Parameters<typeof deleteNotification>[0],
  ) => {
    try {
      await deleteNotification(notificationId);
    } catch {
      toast({
        title: 'エラー',
        description: '通知を削除できませんでした。',
        variant: 'destructive',
      });
    }
  };

  return (
    <nav className="sticky top-0 z-50 shrink-0 border-b bg-card/50 backdrop-blur-sm">
      <div
        className={cn(
          'w-full px-4 py-4 sm:px-6 lg:px-8',
          isAuthenticated && user && 'py-3',
          isAdminAuthenticated && 'h-20 px-6 py-0',
          isLoginPage && '[@media(max-height:480px)]:py-2',
        )}
      >
        <div
          className={cn(
            'flex items-center justify-between',
            isAdminAuthenticated &&
              'relative h-full justify-start gap-3',
          )}
        >
          <Link to={linkTarget} className="flex items-center space-x-2">
            <div className="h-8 w-8 rounded-lg gradient-primary" />
            <span className="text-xl font-bold">GX</span>
          </Link>

          {isAdminAuthenticated && <AdminNavigation />}

          <div
            className={cn(
              'flex items-center space-x-3',
              isAdminAuthenticated && 'ml-auto',
            )}
          >
            {isAuthenticated && user ? (
              <>
                <Sheet>
                  <SheetTrigger asChild>
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="relative h-11 w-11 border border-border"
                      aria-label="お知らせ"
                    >
                      <Bell className="h-4 w-4" />
                      {unreadCount > 0 && (
                        <span className="absolute -right-0 -top-0 flex min-h-4 min-w-4 items-center justify-center rounded-full bg-destructive px-1 text-[10px] font-semibold leading-none text-destructive-foreground">
                          {unreadCount > 99 ? '99+' : unreadCount}
                        </span>
                      )}
                    </Button>
                  </SheetTrigger>
                  <SheetContent
                    side="right"
                    hideCloseButton
                    className="flex h-full flex-col overflow-hidden p-0"
                  >
                    <SheetHeader className="sticky top-0 z-10 flex-row items-center justify-between space-y-0 border-b bg-background px-6 py-4 text-left">
                      <SheetTitle>お知らせ</SheetTitle>
                      <SheetClose asChild>
                        <Button type="button" variant="ghost" size="icon" aria-label="閉じる">
                          <X className="h-4 w-4" />
                        </Button>
                      </SheetClose>
                    </SheetHeader>
                    <div className="flex-1 overflow-y-auto p-4">
                      <NotificationsList
                        notifications={notifications}
                        loading={notificationsLoading}
                        error={notificationsError}
                        onMarkAsRead={handleMarkAsRead}
                        onDelete={handleDeleteNotification}
                      />
                    </div>
                  </SheetContent>
                </Sheet>

                <AuthenticatedNavbarContent
                  user={user}
                  onLogout={handleLogout}
                />
              </>
            ) : (
              !isAdminLoginPage && (
                <div className="flex items-center space-x-2">
                  <Button variant="ghost" onClick={() => navigate(paths.login)}>
                    ログイン
                  </Button>
                  <Button onClick={() => navigate(paths.signup)}>
                    新規登録
                  </Button>
                </div>
              )
            )}
          </div>
        </div>
      </div>
    </nav>
  );
};
