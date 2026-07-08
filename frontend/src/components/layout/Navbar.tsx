import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Bell, LogOut, User, X } from 'lucide-react';
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
import { getHomePath } from '@/utils/getHomePath';
import { paths } from '@/routes/paths';

export const Navbar = () => {
  const { user, logout, isAuthenticated } = useAuth();
  const {
    notifications,
    loading: notificationsLoading,
    error: notificationsError,
    unreadCount,
    markAsRead,
  } = useNotifications();
  const navigate = useNavigate();
  const location = useLocation();

  const linkTarget = getHomePath(user);
  const isAdminLoginPage = location.pathname === paths.adminLogin;

  const handleLogout = () => {
    logout();
    navigate(paths.home);
  };

  return (
    <nav className="sticky top-0 z-50 border-b bg-card/50 backdrop-blur-sm">
      <div className="container mx-auto px-4 py-4">
        <div className="flex items-center justify-between">
          <Link to={linkTarget} className="flex items-center space-x-2">
            <div className="h-8 w-8 rounded-lg gradient-primary" />
            <span className="text-xl font-bold">GX</span>
          </Link>

          <div className="flex items-center space-x-3">
            {isAuthenticated && user ? (
              <>
                <Sheet>
                  <SheetTrigger asChild>
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="relative"
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
                    <div className="flex-1 overflow-y-auto px-6 py-2">
                      <NotificationsList
                        notifications={notifications}
                        loading={notificationsLoading}
                        error={notificationsError}
                        onMarkAsRead={markAsRead}
                      />
                    </div>
                  </SheetContent>
                </Sheet>

                <div className="flex items-center space-x-0">
                  <Button
                    variant="ghost"
                    onClick={() => navigate(paths.profile)}
                    className="flex items-center space-x-1"
                  >
                    <User className="h-4 w-4" />
                    <span>プロファイル</span>
                  </Button>
                  <Button
                    variant="ghost"
                    onClick={handleLogout}
                    className="flex items-center space-x-1"
                  >
                    <LogOut className="h-4 w-4" />
                    <span>ログアウト</span>
                  </Button>
                </div>
              </>
            ) : (
              !isAdminLoginPage && (
                <div className="flex items-center space-x-2">
                  <Button onClick={() => navigate(paths.signup)}>
                    新規登録
                  </Button>
                  <Button variant="ghost" onClick={() => navigate(paths.login)}>
                    ログイン
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
