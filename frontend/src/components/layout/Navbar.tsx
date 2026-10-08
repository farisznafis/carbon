import {
  useEffect,
  useRef,
  type ReactNode,
} from 'react';
import {
  Link,
  useLocation,
  useNavigate,
} from 'react-router-dom';
import { Bell, X } from 'lucide-react';

import { NotificationsList } from '@/components/feedback/NotificationsList';
import { PageBreadcrumb } from '@/components/navigation/PageBreadcrumb';
import { usePageBreadcrumbItems } from '@/components/navigation/pageBreadcrumbContext';
import { Button } from '@/components/ui/button';
import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet';
import { useAuth } from '@/contexts/AuthContext';
import { useNotifications } from '@/hooks/useNotifications';
import { toast } from '@/hooks/use-toast';
import { cn } from '@/lib/utils';
import {
  isLoginPath,
  paths,
} from '@/routes/paths';
import { getHomePath } from '@/utils/getHomePath';

import { AccountMenu } from './AccountMenu';
import { useAccountIdentity } from './useAccountIdentity';

type AuthenticatedNavbarContentProps = {
  user: NonNullable<
    ReturnType<typeof useAuth>['user']
  >;
  onLogout: () => Promise<void>;
};

type NavbarProps = {
  sidebarControl?: ReactNode;
  appShell?: boolean;
};

function AuthenticatedNavbarContent({
  user,
  onLogout,
}: Readonly<AuthenticatedNavbarContentProps>) {
  const navigate = useNavigate();

  const {
    avatarInitial,
    displayName,
    email,
    roleLabel,
    resolveProfilePath,
  } = useAccountIdentity(user);

  /**
   * Opens the current account profile without
   * guessing an operator/user identifier.
   */
  const handleProfile = async () => {
    try {
      const profilePath =
        await resolveProfilePath();

      if (profilePath) {
        navigate(profilePath);
        return;
      }
    } catch {
      // The shared message below also covers
      // profile resolution/API failures.
    }

    toast({
      title: 'エラー',
      description:
        'プロファイルを表示できませんでした。',
      variant: 'destructive',
    });
  };

  return (
    <AccountMenu
      avatarInitial={avatarInitial}
      displayName={displayName}
      email={email}
      roleLabel={roleLabel}
      onProfile={handleProfile}
      onLogout={onLogout}
    />
  );
}

/**
 * Global application topbar.
 *
 * Desktop application shell:
 * - product identity lives in SidebarHeader
 * - Navbar contains the page breadcrumb and notification/account actions
 *
 * Mobile:
 * - hamburger + GX identity remain in Navbar
 * - on pages with a breadcrumb, "‹ parent" replaces the GX identity
 */
export const Navbar = ({
  sidebarControl,
  appShell = false,
}: Readonly<NavbarProps>) => {
  const {
    user,
    logout,
    isAuthenticated,
  } = useAuth();

  const {
    notifications,
    loading: notificationsLoading,
    error: notificationsError,
    unreadCount,
    loadNotifications,
    markAsRead,
    deleteNotification,
  } = useNotifications();

  const loadedAdminNotificationSessionRef =
    useRef<string | null>(null);

  const navigate = useNavigate();
  const location = useLocation();

  const linkTarget = getHomePath(user);

  // Detail pages declare a trail; on mobile its "‹ parent" link takes the logo's place.
  const breadcrumbItems = usePageBreadcrumbItems();
  const showBreadcrumb = appShell && breadcrumbItems.length > 0;

  const isLoginPage =
    isLoginPath(location.pathname);

  const isAdminLoginPage =
    isLoginPage &&
    location.pathname.startsWith(
      paths.adminLogin,
    );

  const isAdminAuthenticated =
    isAuthenticated &&
    user?.role === 'admin';

  const adminNotificationSessionKey =
    isAdminAuthenticated
      ? `${user.id}:${user.email}`
      : null;

  /**
   * Loads navbar notifications once for each
   * authenticated admin account.
   */
  useEffect(() => {
    if (!adminNotificationSessionKey) {
      loadedAdminNotificationSessionRef.current =
        null;
      return;
    }

    if (
      loadedAdminNotificationSessionRef.current ===
        adminNotificationSessionKey ||
      typeof loadNotifications !== 'function'
    ) {
      return;
    }

    loadedAdminNotificationSessionRef.current =
      adminNotificationSessionKey;

    void loadNotifications();
  }, [
    adminNotificationSessionKey,
    loadNotifications,
  ]);

  const handleLogout = async () => {
    const redirectPath =
      user?.role === 'admin'
        ? paths.adminLogin
        : paths.home;

    await logout();

    navigate(redirectPath, {
      replace: true,
    });
  };

  const handleMarkAsRead = async (
    notificationId: Parameters<
      typeof markAsRead
    >[0],
  ) => {
    try {
      await markAsRead(notificationId);
    } catch {
      toast({
        title: 'エラー',
        description:
          '通知を既読にできませんでした。',
        variant: 'destructive',
      });
    }
  };

  const handleDeleteNotification = async (
    notificationId: Parameters<
      typeof deleteNotification
    >[0],
  ) => {
    try {
      await deleteNotification(
        notificationId,
      );
    } catch {
      toast({
        title: 'エラー',
        description:
          '通知を削除できませんでした。',
        variant: 'destructive',
      });
    }
  };

  return (
    <nav
      className={cn(
        'sticky top-0 shrink-0 border-b border-border bg-card/50 backdrop-blur-sm',
        appShell ? 'z-40' : 'z-50',
      )}
    >
      <div
        className={cn(
          'w-full px-4 py-4 sm:px-6 lg:px-8',
          isAuthenticated &&
            user &&
            'py-3',
          appShell &&
            'h-header px-4 py-0 sm:px-6 lg:px-gutter',
          isLoginPage &&
            '[@media(max-height:480px)]:py-2',
        )}
      >
        <div
          className={cn(
            'flex items-center justify-between',
            appShell &&
              'relative h-full justify-start gap-3',
          )}
        >
          {/* Mobile application menu button */}
          {sidebarControl}

          {/*
           * Desktop application identity now lives
           * inside SidebarHeader.
           *
           * Keep GX visible for:
           * - public pages
           * - auth pages
           * - mobile application layout
           */}
          <Link
            to={linkTarget}
            aria-label="ホーム"
            className={cn(
              'flex items-center space-x-2',
              appShell && 'lg:hidden',
              showBreadcrumb && 'hidden',
            )}
          >
            <div className="h-8 w-8 rounded-lg gradient-primary" />

            <span className="text-xl font-bold text-foreground">
              GX
            </span>
          </Link>

          {showBreadcrumb && (
            <PageBreadcrumb
              items={breadcrumbItems}
              className="min-w-0 flex-1"
            />
          )}

          <div
            className={cn(
              'flex items-center space-x-3',
              appShell && 'ml-auto shrink-0',
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
                      className="
                        relative
                        h-11
                        w-11
                        border
                        border-border
                      "
                      aria-label="お知らせ"
                    >
                      <Bell
                        className="h-4 w-4"
                        aria-hidden="true"
                      />

                      {unreadCount > 0 && (
                        <span
                          className="
                            absolute
                            -right-0
                            -top-0
                            flex
                            min-h-4
                            min-w-4
                            items-center
                            justify-center
                            rounded-full
                            bg-destructive
                            px-1
                            text-[0.625rem]
                            font-semibold
                            leading-none
                            text-destructive-foreground
                          "
                        >
                          {unreadCount > 99
                            ? '99+'
                            : unreadCount}
                        </span>
                      )}
                    </Button>
                  </SheetTrigger>

                  <SheetContent
                    side="right"
                    hideCloseButton
                    className="
                      flex
                      h-full
                      flex-col
                      overflow-hidden
                      p-0
                    "
                  >
                    <SheetHeader
                      className="
                        sticky
                        top-0
                        z-10
                        flex-row
                        items-center
                        justify-between
                        space-y-0
                        border-b
                        bg-background
                        px-6
                        py-4
                        text-left
                      "
                    >
                      <SheetTitle>
                        お知らせ
                      </SheetTitle>

                      <SheetClose asChild>
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          aria-label="閉じる"
                        >
                          <X
                            className="h-4 w-4"
                            aria-hidden="true"
                          />
                        </Button>
                      </SheetClose>
                    </SheetHeader>

                    <div className="flex-1 overflow-y-auto p-4">
                      <NotificationsList
                        notifications={
                          notifications
                        }
                        loading={
                          notificationsLoading
                        }
                        error={
                          notificationsError
                        }
                        onMarkAsRead={
                          handleMarkAsRead
                        }
                        onDelete={
                          handleDeleteNotification
                        }
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
                  <Button
                    variant="ghost"
                    onClick={() =>
                      navigate(paths.login)
                    }
                  >
                    ログイン
                  </Button>

                  <Button
                    onClick={() =>
                      navigate(paths.signup)
                    }
                  >
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
