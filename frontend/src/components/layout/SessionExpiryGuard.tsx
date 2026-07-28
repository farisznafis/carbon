import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';

const SESSION_EXPIRES_AT_KEY = 'sessionExpiresAt';

const getStoredSessionExpiresAt = () => {
  const rawExpiresAt = localStorage.getItem(SESSION_EXPIRES_AT_KEY);
  const expiresAt = rawExpiresAt ? Number(rawExpiresAt) : null;

  return Number.isFinite(expiresAt) ? expiresAt : null;
};

export const SessionExpiryGuard = () => {
  const navigate = useNavigate();
  const { isAuthenticated, logout } = useAuth();
  const [isSessionExpired, setIsSessionExpired] = useState(false);
  const expiryTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isHandlingExpiryRef = useRef(false);

  useEffect(() => {
    if (!isAuthenticated) {
      isHandlingExpiryRef.current = false;
      setIsSessionExpired(false);
      return;
    }

    const handleExpiredSession = () => {
      if (isHandlingExpiryRef.current) {
        return;
      }

      isHandlingExpiryRef.current = true;
      setIsSessionExpired(true);
    };

    const expiresAt = getStoredSessionExpiresAt();

    if (expiresAt === null) {
      return;
    }

    const remainingMs = expiresAt - Date.now();

    if (remainingMs <= 0) {
      handleExpiredSession();
      return;
    }

    expiryTimerRef.current = setTimeout(handleExpiredSession, remainingMs);

    return () => {
      if (expiryTimerRef.current) {
        clearTimeout(expiryTimerRef.current);
        expiryTimerRef.current = null;
      }
    };
  }, [isAuthenticated]);

  const handleReturnToLogin = () => {
    const loginPath = window.location.pathname.startsWith('/admin')
      ? '/admin/login'
      : '/login';

    void logout();
    setIsSessionExpired(false);
    navigate(loginPath, { replace: true });
  };

  return (
    <AlertDialog open={isSessionExpired}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>アカウントセッションの有効期限が切れました。</AlertDialogTitle>
          {/* <AlertDialogDescription>
            ログインから1時間が経過しました。続行するには、もう一度ログインしてください。
          </AlertDialogDescription> */}
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogAction onClick={handleReturnToLogin}>
            もう一度ログイン
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
};
