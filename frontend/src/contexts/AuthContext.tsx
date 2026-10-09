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
import { getJwtUserId } from '@/lib/jwt';
import { useDashboardStore } from '@/stores/dashboardStore';
import { useNotificationStore } from '@/stores/notificationStore';
import { useProfileStore } from '@/stores/ProfileStore';
import { queryClient } from '@/lib/queryClient';

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
  id?: string | number | null;
  userId?: string | number | null;
  operatorId?: string | number | null;
  user?: Partial<User> & {
    userId?: string | number | null;
    operatorId?: string | number | null;
  };
  admin?: {
    id?: string | number | null;
    userId?: string | number | null;
    operatorId?: string | number | null;
    username?: string | null;
    name?: string | null;
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
const LEGACY_ADMIN_SESSION_ID = 'admin';
const transientSessionKeys = [
  storageKeys.sites,
  storageKeys.users,
  storageKeys.appliedProjects,
  storageKeys.projects,
  storageKeys.profile,
  storageKeys.dashboardStorage,
];

const clearTransientSessionState = () => {
  queryClient.clear();
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
  queryClient.clear();
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

/** Compares every stable account identifier so placeholder IDs cannot retain another login. */
function isSameAuthSession(
  currentSession: AuthSession,
  nextSession: AuthSession,
): boolean {
  return (
    currentSession.isAuthenticated === nextSession.isAuthenticated &&
    currentSession.user?.id === nextSession.user?.id &&
    currentSession.user?.username === nextSession.user?.username &&
    currentSession.user?.email === nextSession.user?.email &&
    currentSession.user?.companyId === nextSession.user?.companyId
  );
}

const isStoredSessionExpired = () => {
  const hasToken = !!storage.get(storageKeys.authToken);
  const expiresAt = getSessionExpiresAt();

  return hasToken && expiresAt !== null && Date.now() >= expiresAt;
};

const EMPTY_AUTH_SESSION: AuthSession = {
  user: null,
  isAuthenticated: false,
};

/** Normalizes an operator ID and ignores the legacy placeholder value. */
function normalizeAdminOperatorId(
  operatorId: string | number | null | undefined,
): string | null {
  if (operatorId === null || operatorId === undefined) return null;

  const normalizedOperatorId = String(operatorId).trim();
  return normalizedOperatorId &&
    normalizedOperatorId.toLocaleLowerCase('en-US') !== LEGACY_ADMIN_SESSION_ID
    ? normalizedOperatorId
    : null;
}

/** Returns whether an admin session still uses the legacy placeholder ID. */
function needsAdminSessionIdMigration(user: User): boolean {
  return user.role === 'admin' && !normalizeAdminOperatorId(user.id);
}

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

  if (needsAdminSessionIdMigration(storedUser)) {
    const jwtUserId = getJwtUserId(token);
    if (!jwtUserId) {
      clearStoredSession();
      return EMPTY_AUTH_SESSION;
    }

    const migratedUser = { ...storedUser, id: jwtUserId };
    storage.setJson(storageKeys.user, migratedUser);
    return {
      user: migratedUser,
      isAuthenticated: true,
    };
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
        if (isSameAuthSession(currentSession, nextSession)) {
          return currentSession;
        }

        queryClient.clear();
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
        const accountId = [
          adminSource?.operatorId,
          adminSource?.userId,
          adminSource?.id,
          data.user?.operatorId,
          data.user?.userId,
          data.user?.id,
          data.operatorId,
          data.userId,
          data.id,
          getJwtUserId(data.token),
        ]
          .map(normalizeAdminOperatorId)
          .find((candidateId): candidateId is string => Boolean(candidateId));

        if (!accountId) {
          throw new Error(
            'ログイン情報から運用者IDを取得できませんでした。管理者にお問い合わせください。',
          );
        }

        const adminUser: User = {
          id: accountId,
          email: adminSource?.email ?? '',
          username: adminSource?.username ?? undefined,
          name: adminSource?.name ?? undefined,
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

/** For decoration that adapts to the role but must not require a provider, e.g. breadcrumbs. */
export const useOptionalAuth = () => useContext(AuthContext);
