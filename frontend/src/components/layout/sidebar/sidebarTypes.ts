import type { LucideIcon } from 'lucide-react';

export type SidebarNavigationItem = {
  key: string;
  label: string;
  href: string;
  icon: LucideIcon;
  isActive: boolean;
};

export type RoleSidebarProps = {
  collapsed: boolean;
  onNavigate?: () => void;
};
