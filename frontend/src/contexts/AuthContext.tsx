import {
  createContext,
  useContext,
  useState,
  useCallback,
  useMemo,
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
  logout: () => void;
  isAuthenticated: boolean;
  isLoggedIn: () => boolean;
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

const AuthContext = createContext<AuthContextType | undefined>(undefined);
const SESSION_DURATION_MS = 60 * 60 * 1000;
const transientSessionKeys = [
  storageKeys.authToken,
  storageKeys.sites,
  storageKeys.users,
  storageKeys.appliedProjects,
  storageKeys.projects,
  storageKeys.profile,
];

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

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(() => {
    if (isStoredSessionExpired()) {
      clearStoredSession();
      return null;
    }

    return storage.getJson<User>(storageKeys.user);
  });

  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(() => {
    return !!storage.get(storageKeys.authToken) && !isStoredSessionExpired();
  });

  const isLoggedIn = useCallback(() => !!user, [user]);

  const [loading] = useState(false);

  const login = useCallback(
    async (companyId: string, email: string, password: string): Promise<User> => {
      storage.removeMany(transientSessionKeys);
      useNotificationStore.getState().clearNotifications();
      useProfileStore.getState().clearProfile();

      try {
        const data = await authApi.login(companyId, email, password);
        
        storage.set(storageKeys.authToken, data.token);
        storage.setJson(storageKeys.user, data.user);
        storage.set(storageKeys.siteId, data.user.siteId || '');
        setSessionExpiry();
        setUser(data.user);
        setIsAuthenticated(true);

        return data.user;
      } catch (err) {
        logger.error('Login error:', err);
        throw err;
      }
    },
    [],
  );

  const adminLogin = useCallback(
    async (identifier: string, password: string): Promise<User> => {
      storage.removeMany(transientSessionKeys);
      useNotificationStore.getState().clearNotifications();
      useProfileStore.getState().clearProfile();

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
        setUser(adminUser);
        setIsAuthenticated(true);

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
      setUser(null);
      setIsAuthenticated(false);
    } catch (err) {
      logger.error('Logout error:', err);
    }
  }, []);

  const value = useMemo(
    () => ({ user, login, adminLogin, logout, isAuthenticated, isLoggedIn, loading }),
    [user, login, adminLogin, logout, isAuthenticated, isLoggedIn, loading],
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
