// context/UsersContext.tsx
import React, { createContext, useContext, useState, useCallback } from 'react';
import type { UserList } from '@/api/users';
import { getUsers } from '@/api/users/list';
import { logger } from '@/lib/logger';

interface UsersContextType {
  users: UserList[];
  setUsers: React.Dispatch<React.SetStateAction<UserList[]>>;
  fetchUsers: () => Promise<void>;
  loading: boolean;
  error: string | null;
}

const UsersContext = createContext<UsersContextType | undefined>(undefined);

export const UsersProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [users, setUsers] = useState<UserList[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const res = await getUsers();
      setUsers(res.users);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to fetch users';
      setError(message);
      logger.error('Failed to fetch users:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  return (
    <UsersContext.Provider value={{ users, setUsers, fetchUsers, loading, error }}>
      {children}
    </UsersContext.Provider>
  );
};

export const useUsers = (): UsersContextType => {
  const context = useContext(UsersContext);
  if (!context) throw new Error('useUsers must be used within a UsersProvider');
  return context;
};
