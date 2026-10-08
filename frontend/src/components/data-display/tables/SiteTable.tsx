import type { Site } from '@/api/site';
import {
  DataGridTable,
  type DataGridTableColumn,
} from '@/components/data-display/DataGridTable';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { InlineHelp } from '@/components/feedback/InlineHelp/InlineHelp';
import { RowActionButton } from '@/components/data-display/RowActionButton';
import { usePagination } from '@/hooks/usePagination';
import { Building2 } from 'lucide-react';

const siteTableGridColumns =
  'minmax(180px, 0.9fr) minmax(180px, 0.9fr) minmax(260px, 1.3fr) minmax(300px, 1.5fr) minmax(100px, 0.55fr)';

type SiteTableProps = {
  sites: Site[];
  onEdit?: (site: Site) => void;
  onRowClick?: (site: Site) => void;
};

export function SiteTable({ sites, onEdit, onRowClick }: SiteTableProps) {
  const columns: DataGridTableColumn<Site>[] = [
    {
      id: 'siteName',
      label: '拠点名',
      sortable: true,
      sortValue: (site) => site.siteName,
      render: (site) => <span className="block truncate text-body">{site.siteName}</span>,
    },
    {
      id: 'personInCharge',
      label: '責任者',
      sortable: true,
      sortValue: (site) => `${site.picLastName} ${site.picFirstName}`,
      render: (site) => (
        <span className="block truncate text-body">
          {`${site.picLastName} ${site.picFirstName}`.trim() || '-'}
        </span>
      ),
    },
    {
      id: 'address',
      label: '所在地',
      sortable: true,
      sortValue: (site) => site.address,
      render: (site) => (
        <span className="block truncate text-body">{site.address || 'なし'}</span>
      ),
    },
    {
      id: 'projects',
      label: 'プロジェクト',
      sortable: true,
      sortValue: (site) => site.project?.join(', '),
      render: (site) => {
        const projects = Array.isArray(site.project) ? site.project : [];
        return (
          <span className="block truncate text-body">
            {projects.length > 0 ? projects.join(', ') : 'なし'}
          </span>
        );
      },
    },
    {
      id: 'actions',
      label: '操作',
      render: (site) => (
        <RowActionButton onClick={() => onEdit?.(site)}>更新</RowActionButton>
      ),
    },
  ];

  const {
    currentPage,
    totalPages,
    totalItems,
    itemsPerPage,
    paginatedItems: paginatedSites,
    goToPage,
    setItemsPerPage,
  } = usePagination(sites);

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2 text-2xl font-bold">
        <Building2 className="h-6 w-6" />
        <span>拠点</span>
        <InlineHelp
          content={
            <div className="text-sm">
              拠点の行をクリックすると、その拠点のダッシュボードに移動します。
            </div>
          }
        />
      </div>
      <DataGridTable
        rows={paginatedSites}
        columns={columns}
        gridTemplateColumns={siteTableGridColumns}
        minWidth={1100}
        getRowKey={(site) => site.siteId}
        emptyMessage="まだ拠点が登録されていません"
        onRowClick={onRowClick}
        getRowActionLabel={() => 'ダッシュボード'}
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
