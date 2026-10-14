import { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import type { ProjectMembershipPdf } from '@/api/admin/projectCompanies';
import { isForbiddenApiError } from '@/api/_shared/errors';
import { ActionButton } from '@/components/actions';
import {
  DataGridTable,
  type DataGridTableColumn,
} from '@/components/data-display/DataGridTable';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { SkeletonTable } from '@/components/feedback/loading';
import { PermissionDeniedNotice } from '@/features/admin/permissions/PermissionDeniedNotice';
import { useAdminPermissions } from '@/features/admin/permissions/useAdminPermissions';
import { useProjectCompaniesQuery } from '@/hooks/admin/useProjectCompaniesQuery';
import { usePagination } from '@/hooks/usePagination';
import { paths } from '@/routes/paths';

const INITIAL_ITEMS_PER_PAGE = 10;
const COMPANY_GRID_COLUMNS = 'minmax(220px, 1.4fr) minmax(140px, 0.8fr) minmax(160px, 0.8fr)';
const COMPANY_GRID_MIN_WIDTH = 640;

/** Lists the companies taking part in one project; embedded in the project detail 参加企業 tab. */
export function ProjectCompaniesPanel({ projectId }: Readonly<{ projectId: string }>) {
  const navigate = useNavigate();
  const permissions = useAdminPermissions();
  const canOpenCompany = permissions.hasPermission('PARTICIPATING_COMPANY_READ');
  const canOpenMembership = permissions.hasPermission('PARTICIPATING_COMPANY_APPLICATION_REVIEW');
  const query = useProjectCompaniesQuery(projectId);
  const {
    currentPage,
    totalPages,
    totalItems,
    paginatedItems,
    goToPage,
    itemsPerPage,
    setItemsPerPage,
  } = usePagination(query.data ?? [], { itemsPerPage: INITIAL_ITEMS_PER_PAGE });

  const columns = useMemo<DataGridTableColumn<ProjectMembershipPdf>[]>(
    () => [
      {
        id: 'companyName',
        label: '会社名',
        sortable: true,
        sortValue: (company) => company.companyName,
        render: (company) => (
          <span className="block truncate font-bold text-foreground">
            {company.companyName || '―'}
          </span>
        ),
      },
      {
        id: 'pdfStatus',
        label: '入会届',
        sortable: true,
        sortValue: (company) => (company.pdfUrl ? 0 : 1),
        render: (company) =>
          company.pdfUrl ? (
            <StatusBadge tone="success">提出済み</StatusBadge>
          ) : (
            <StatusBadge>PDF準備中</StatusBadge>
          ),
      },
      {
        id: 'membership',
        label: '入会届詳細',
        render: (company) =>
          canOpenMembership ? (
            <ActionButton
              kind="link"
              onClick={() =>
                navigate(paths.adminProjectMembershipDetail(projectId, company.participationId))
              }
              aria-label={`${company.companyName}の入会届詳細を開く`}
            >
              入会届を見る
            </ActionButton>
          ) : (
            <span className="text-muted-foreground">―</span>
          ),
      },
    ],
    [canOpenMembership, navigate, projectId],
  );

  if (isForbiddenApiError(query.error)) {
    return <PermissionDeniedNotice permission="PROJECT_READ" />;
  }

  if (query.isError) {
    return (
      <div className="space-y-3 rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-center">
        <p role="alert" className="text-small text-destructive">
          参加企業一覧を取得できませんでした。
        </p>
        <ActionButton kind="retry" onClick={() => void query.refetch()} />
      </div>
    );
  }

  if (query.isLoading) {
    return (
      <SkeletonTable
        columns={3}
        rows={3}
        gridTemplateColumns={COMPANY_GRID_COLUMNS}
        minWidth={COMPANY_GRID_MIN_WIDTH}
      />
    );
  }

  return (
    <div className="space-y-4">
      <DataGridTable
        rows={paginatedItems}
        columns={columns}
        gridTemplateColumns={COMPANY_GRID_COLUMNS}
        minWidth={COMPANY_GRID_MIN_WIDTH}
        getRowKey={(company) => String(company.participationId)}
        emptyMessage="このプロジェクトに参加している企業はありません。"
        onRowClick={
          canOpenCompany
            ? (company) => navigate(paths.adminCompanyDetail(company.companyId), { state: { projectId } })
            : undefined
        }
        getRowActionLabel={(company) => `${company.companyName}の企業詳細を開く`}
      />
      <PaginationControls
        currentPage={currentPage}
        totalPages={totalPages}
        totalItems={totalItems}
        itemsPerPage={itemsPerPage}
        onPageChange={goToPage}
        onItemsPerPageChange={setItemsPerPage}
      />
    </div>
  );
}
