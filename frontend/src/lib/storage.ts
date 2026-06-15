export const storageKeys = {
  authToken: 'authToken',
  user: 'user',
  siteId: 'siteId',
  sites: 'sites',
  users: 'users',
  appliedProjects: 'appliedProjects',
  projects: 'projects',
  profile: 'profile',
  dashboardStorage: 'dashboard-storage',
  sessionExpiresAt: 'sessionExpiresAt',
} as const;

type StorageKey = (typeof storageKeys)[keyof typeof storageKeys];

const isBrowser = () => typeof window !== 'undefined';

export const storage = {
  get(key: StorageKey): string | null {
    return isBrowser() ? localStorage.getItem(key) : null;
  },

  set(key: StorageKey, value: string): void {
    if (isBrowser()) {
      localStorage.setItem(key, value);
    }
  },

  remove(key: StorageKey): void {
    if (isBrowser()) {
      localStorage.removeItem(key);
    }
  },

  removeMany(keys: StorageKey[]): void {
    keys.forEach((key) => storage.remove(key));
  },

  getJson<T>(key: StorageKey): T | null {
    const raw = storage.get(key);
    if (!raw) return null;

    try {
      return JSON.parse(raw) as T;
    } catch {
      storage.remove(key);
      return null;
    }
  },

  setJson(key: StorageKey, value: unknown): void {
    storage.set(key, JSON.stringify(value));
  },
};
