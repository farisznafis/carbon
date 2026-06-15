import { paths } from '@/routes/paths';

type HomePathUser = {
  id?: string;
  role?: 'master' | 'user' | 'admin' | string;
  siteId?: string;
};

export function getHomePath(user?: HomePathUser | null) {
  if (!user) return paths.home;

  if (user.role === 'admin') {
    return paths.adminDashboard;
  }

  if (user.role === 'master') {
    return paths.masterDashboard;
  }

  if (user.siteId) {
    return paths.siteDashboard(user.siteId);
  }

  return paths.home;
}
