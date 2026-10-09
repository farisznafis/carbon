import type { PageBreadcrumbItem } from '@/components/navigation/PageBreadcrumb';
import { useSiteBreadcrumbRoot } from '@/features/user/navigation/useSiteBreadcrumbRoot';
import { paths } from '@/routes/paths';

type EquipmentBreadcrumbRootOptions = {
  siteId: string;
  /** Where the equipment form returns to, as carried in its navigation state. */
  returnTo?: string | null;
  fromMasterDashboard: boolean;
  /** Leaves the equipment flow the same way its cancel button does. */
  onReturn: () => void;
};

/**
 * The trail above the equipment form and its confirmation. The form is opened
 * from the project detail 設備一覧 tab or from the registration wizard; with
 * neither, it falls back to the membership list like its cancel button.
 */
export function useEquipmentBreadcrumbRoot({
  siteId,
  returnTo,
  fromMasterDashboard,
  onReturn,
}: EquipmentBreadcrumbRootOptions): PageBreadcrumbItem[] {
  const siteRoot = useSiteBreadcrumbRoot(siteId);
  const root = fromMasterDashboard
    ? [{ label: 'プロジェクトの入会届', to: paths.masterDashboardMembership }]
    : siteRoot;

  if (returnTo) {
    const parentLabel = returnTo.startsWith('/sites/')
      ? 'プロジェクト詳細'
      : 'プロジェクト参加申請';
    return [...root, { label: parentLabel, onClick: onReturn }];
  }

  return fromMasterDashboard
    ? root
    : [...root, { label: 'プロジェクトへの入会', onClick: onReturn }];
}
