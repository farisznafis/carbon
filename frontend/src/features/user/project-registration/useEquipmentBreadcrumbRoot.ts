import type { PageBreadcrumbItem } from '@/components/navigation/PageBreadcrumb';
import { useSiteBreadcrumbRoot } from '@/features/user/navigation/useSiteBreadcrumbRoot';
import { paths } from '@/routes/paths';
import { useDashboardStore } from '@/stores/dashboardStore';

type EquipmentBreadcrumbRootOptions = {
  siteId: string;
  projectId: string;
  /** Handed over by the project detail page; the registration wizard keeps it in the dashboard store. */
  projectName?: string;
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
  projectId,
  projectName,
  returnTo,
  fromMasterDashboard,
  onReturn,
}: EquipmentBreadcrumbRootOptions): PageBreadcrumbItem[] {
  const siteRoot = useSiteBreadcrumbRoot(siteId);
  const currentProject = useDashboardStore((state) => state.currentProject);
  const name =
    projectName || (currentProject?.id === projectId ? currentProject.projectName : undefined);
  const root = fromMasterDashboard
    ? [{ label: 'プロジェクトの入会届', to: paths.masterDashboardMembership }]
    : siteRoot;

  if (returnTo?.startsWith('/sites/')) {
    return [...root, { label: name || 'プロジェクト詳細', onClick: onReturn }];
  }

  if (returnTo) {
    // Mirrors the registration wizard's own trail so the step it came from keeps its place.
    return [
      ...root,
      ...(fromMasterDashboard
        ? []
        : [{ label: 'プロジェクトへの入会', to: paths.projectMembership(siteId) }]),
      { label: name || 'プロジェクト参加申請', onClick: onReturn },
    ];
  }

  return fromMasterDashboard
    ? root
    : [...root, { label: 'プロジェクトへの入会', onClick: onReturn }];
}
