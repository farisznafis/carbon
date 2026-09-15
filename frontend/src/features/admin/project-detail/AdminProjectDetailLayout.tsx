import type { ReactNode } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Pencil } from 'lucide-react';
import {
  FormPageContent,
  FormPageTitle,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import {
  PageBackButton,
  floatingPageBackButtonClassName,
} from '@/components/navigation/PageBackButton';
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from '@/components/ui/breadcrumb';
import { Button } from '@/components/ui/button';
import { paths } from '@/routes/paths';
import { useAdminPermissions } from '@/features/admin/permissions/useAdminPermissions';

type AdminProjectDetailLayoutProps = {
  projectId: string;
  projectName: string;
  title: string;
  description: string;
  currentBreadcrumb?: string;
  backTo: string;
  backLabel: string;
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
  backTo,
  backLabel,
  onEdit,
  children,
}: Readonly<AdminProjectDetailLayoutProps>) {
  const navigate = useNavigate();
  const permissions = useAdminPermissions();

  /** Opens the edit flow for the read-only page being shown. */
  const handleEdit = () =>
    permissions.guardAction('PROJECT_UPDATE', () =>
      onEdit ? onEdit() : navigate(paths.adminProjectEdit(projectId)),
    );

  return (
    <FormStandalonePage>
      <PageBackButton
        onClick={() => navigate(backTo)}
        label=""
        aria-label={backLabel}
        className={floatingPageBackButtonClassName}
      />
      <FormPageContent className="max-w-admin space-y-6">
        <Breadcrumb>
          <BreadcrumbList className="flex-nowrap">
            <BreadcrumbItem>
              <BreadcrumbLink asChild>
                <Link to={paths.adminProjectList}>プロジェクト管理</Link>
              </BreadcrumbLink>
            </BreadcrumbItem>
            <BreadcrumbSeparator />
            <BreadcrumbItem className="min-w-0">
              {currentBreadcrumb ? (
                <BreadcrumbLink asChild>
                  <Link
                    to={paths.adminProjectDetail(projectId)}
                    className="block max-w-40 truncate sm:max-w-72"
                  >
                    {projectName}
                  </Link>
                </BreadcrumbLink>
              ) : (
                <BreadcrumbPage className="block max-w-40 truncate sm:max-w-72">
                  {projectName}
                </BreadcrumbPage>
              )}
            </BreadcrumbItem>
            {currentBreadcrumb ? (
              <>
                <BreadcrumbSeparator />
                <BreadcrumbItem>
                  <BreadcrumbPage>{currentBreadcrumb}</BreadcrumbPage>
                </BreadcrumbItem>
              </>
            ) : null}
          </BreadcrumbList>
        </Breadcrumb>

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
