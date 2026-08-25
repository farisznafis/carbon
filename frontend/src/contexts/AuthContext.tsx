import {
  createContext,
  useContext,
  useState,
  useCallback,
  useMemo,
  useEffect,
  ReactNode,
} from 'react';
import * as authApi from '@/api/auth';
import type { User } from '@/api/users';
import { logger } from '@/lib/logger';
import { storage, storageKeys } from '@/lib/storage';
import { useDashboardStore } from '@/stores/dashboardStore';
import { useNotificationStore } from '@/stores/notificationStore';
import { useProfileStore } from '@/stores/ProfileStore';

interface AuthContextType {
  user: User | null;
  login: (companyId: string, email: string, password: string) => Promise<User>;
  adminLogin: (identifier: string, password: string) => Promise<User>;
  logout: () => Promise<void>;
  isAuthenticated: boolean;
  loading: boolean;
}

type AdminLoginResponse = {
  token: string;
  user?: Partial<User>;
  admin?: {
    id?: string | number | null;
    username?: string | null;
    email?: string | null;
    role?: User['role'];
    companyId?: string | null;
    siteId?: string | null;
    siteName?: string | null;
  };
};

type AuthSession = {
  user: User | null;
  isAuthenticated: boolean;
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);
const SESSION_DURATION_MS = 60 * 60 * 1000;
const transientSessionKeys = [
  storageKeys.sites,
  storageKeys.users,
  storageKeys.appliedProjects,
  storageKeys.projects,
  storageKeys.profile,
  storageKeys.dashboardStorage,
];

const clearTransientSessionState = () => {
  storage.removeMany(transientSessionKeys);
  useDashboardStore.getState().resetDashboard();
  useNotificationStore.getState().clearNotifications();
  useProfileStore.getState().clearProfile();
};

const getSessionExpiresAt = () => {
  const rawExpiresAt = storage.get(storageKeys.sessionExpiresAt);
  const expiresAt = rawExpiresAt ? Number(rawExpiresAt) : null;

  return Number.isFinite(expiresAt) ? expiresAt : null;
};

const setSessionExpiry = () => {
  storage.set(storageKeys.sessionExpiresAt, String(Date.now() + SESSION_DURATION_MS));
};

const clearStoredSession = () => {
  storage.removeMany([
    storageKeys.authToken,
    storageKeys.user,
    storageKeys.siteId,
    storageKeys.dashboardStorage,
    storageKeys.profile,
    storageKeys.sessionExpiresAt,
  ]);

  useDashboardStore.getState().resetDashboard();
  useNotificationStore.getState().clearNotifications();
  useProfileStore.getState().clearProfile();
};

const isStoredSessionExpired = () => {
  const hasToken = !!storage.get(storageKeys.authToken);
  const expiresAt = getSessionExpiresAt();

  return hasToken && expiresAt !== null && Date.now() >= expiresAt;
};

const EMPTY_AUTH_SESSION: AuthSession = {
  user: null,
  isAuthenticated: false,
};

const getStoredAuthSession = (): AuthSession => {
  const token = storage.get(storageKeys.authToken);
  const storedUser = storage.getJson<User>(storageKeys.user);
  const hasStoredSession = !!token || !!storedUser || getSessionExpiresAt() !== null;

  if (!token || !storedUser || isStoredSessionExpired()) {
    if (hasStoredSession) {
      clearStoredSession();
    }

    return EMPTY_AUTH_SESSION;
  }

  return {
    user: storedUser,
    isAuthenticated: true,
  };
};

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [session, setSession] = useState<AuthSession>(EMPTY_AUTH_SESSION);
  const { user, isAuthenticated } = session;

  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setSession(getStoredAuthSession());
    setLoading(false);

    /** Synchronizes authentication when another tab or window changes storage. */
    const synchronizeSession = () => {
      const nextSession = getStoredAuthSession();

      setSession((currentSession) => {
        if (
          currentSession.user?.id === nextSession.user?.id &&
          currentSession.isAuthenticated === nextSession.isAuthenticated
        ) {
          return currentSession;
        }

        return nextSession;
      });
    };

    window.addEventListener('storage', synchronizeSession);
    window.addEventListener('focus', synchronizeSession);

    return () => {
      window.removeEventListener('storage', synchronizeSession);
      window.removeEventListener('focus', synchronizeSession);
    };
  }, []);

  const login = useCallback(
    async (companyId: string, email: string, password: string): Promise<User> => {
      clearTransientSessionState();

      try {
        const data = await authApi.login(companyId, email, password);
        const accountId =
          data.user?.userId ?? data.user?.id ?? data.userId ?? data.id ?? companyId;

        const loginUser: User = {
          id: String(accountId),
          email: data.user?.email ?? email,
          username: data.user?.username ?? undefined,
          role: data.role,
          companyId,
          companyName: data.companyName,
          siteId: data.siteId != null ? String(data.siteId) : '',
          siteName: data.companyName,
        };

        storage.set(storageKeys.authToken, data.token);
        storage.setJson(storageKeys.user, loginUser);
        storage.set(storageKeys.siteId, loginUser.siteId);
        setSessionExpiry();
        setSession({
          user: loginUser,
          isAuthenticated: true,
        });

        return loginUser;
      } catch (err) {
        logger.error('Login error:', err);
        throw err;
      }
    },
    [],
  );

  const adminLogin = useCallback(
    async (identifier: string, password: string): Promise<User> => {
      clearTransientSessionState();

      try {
        const data = (await authApi.adminLogin(
          identifier,
          password,
        )) as AdminLoginResponse;

        const adminSource = data.admin ?? data.user ?? null;

        const adminUser: User = {
          id: adminSource?.id ? String(adminSource.id) : 'admin',
          email: adminSource?.email ?? (data.admin?.username) ?? '',
          role: 'admin',
          companyId: adminSource?.companyId ?? undefined,
          siteId: adminSource?.siteId ? String(adminSource.siteId) : '',
          siteName: adminSource?.siteName ?? undefined,
        };

        storage.set(storageKeys.authToken, data.token);
        storage.setJson(storageKeys.user, adminUser);
        storage.remove(storageKeys.siteId);
        setSessionExpiry();
        setSession({
          user: adminUser,
          isAuthenticated: true,
        });

        return adminUser;
      } catch (err) {
        logger.error('Admin login error:', err);
        throw err;
      }
    },
    [],
  );

  const logout = useCallback(async () => {
    try {
      clearStoredSession();
      setSession(EMPTY_AUTH_SESSION);
    } catch (err) {
      logger.error('Logout error:', err);
    }
  }, []);

  const value = useMemo(
    () => ({ user, login, adminLogin, logout, isAuthenticated, loading }),
    [user, login, adminLogin, logout, isAuthenticated, loading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
