import { House } from 'lucide-react';
import { paths } from '@/routes/paths';

export const MASTER_SIDEBAR_ITEMS = [
  { key: 'dashboard', label: 'ホーム', href: paths.masterDashboard, icon: House },
] as const;
