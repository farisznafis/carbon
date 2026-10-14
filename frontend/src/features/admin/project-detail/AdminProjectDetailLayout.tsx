import type { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { Pencil } from 'lucide-react';
import {
  FormPageContent,
  FormPageTitle,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import { usePageBreadcrumb } from '@/components/navigation/pageBreadcrumbContext';
import { Button } from '@/components/ui/button';
import { paths } from '@/routes/paths';
import { useAdminPermissions } from '@/features/admin/permissions/useAdminPermissions';

type AdminProjectDetailLayoutProps = {
  projectId: string;
  projectName: string;
  title: string;
  description: string;
  /** Set on sub-pages; the project name then links back to the project detail. */
  currentBreadcrumb?: string;
  /** Opened from admin Home, so the trail starts there instead of the project list. */
  fromHome?: boolean;
  /**
   * Where 更新 leads. Defaults to the project edit flow; the plan page sends
   * the operator to the wizard page they are reading instead.
   */
  onEdit?: () => void;
  children: ReactNode;
};

/** Provides the shared navigation and heading hierarchy for admin project pages. */
export function AdminProjectDetailLayout({
  projectId,
  projectName,
  title,
  description,
  currentBreadcrumb,
  fromHome = false,
  onEdit,
  children,
}: Readonly<AdminProjectDetailLayoutProps>) {
  const navigate = useNavigate();
  const permissions = useAdminPermissions();

  usePageBreadcrumb([
    fromHome
      ? { label: 'ホーム', to: paths.adminDashboard }
      : { label: 'プロジェクト', to: paths.adminProjectList },
    currentBreadcrumb
      ? { label: projectName || 'プロジェクト詳細', to: paths.adminProjectDetail(projectId) }
      : { label: projectName || 'プロジェクト詳細' },
    ...(currentBreadcrumb ? [{ label: currentBreadcrumb }] : []),
  ]);

  /** Opens the edit flow for the read-only page being shown. */
  const handleEdit = () =>
    permissions.guardAction('PROJECT_UPDATE', () =>
      onEdit ? onEdit() : navigate(paths.adminProjectEdit(projectId)),
    );

  return (
    <FormStandalonePage>
      <FormPageContent className="max-w-admin space-y-6">
        <header className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0">
            <FormPageTitle as="h1">{title}</FormPageTitle>
            <p className="mt-2 text-sm leading-6 text-muted-foreground sm:text-base">
              {description}
            </p>
          </div>
          <Button
            type="button"
            variant="outline"
            size="md"
            onClick={handleEdit}
            className="self-start sm:self-auto"
          >
            <Pencil aria-hidden="true" />
            更新
          </Button>
        </header>

        {children}
      </FormPageContent>
    </FormStandalonePage>
  );
}
