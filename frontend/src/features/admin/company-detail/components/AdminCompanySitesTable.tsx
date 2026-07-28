import type { AdminCompanyDetailSite } from '@/api/admin';
import {
  DataGridTable,
  type DataGridTableColumn,
} from '@/components/data-display/DataGridTable';
import { Badge } from '@/components/ui/badge';
import {
  formatAdminCompanyAddress,
  formatAdminCompanyDateTime,
} from '../companyDetailDisplay';

const siteTableColumns =
  'minmax(160px,0.8fr) minmax(220px,1.1fr) minmax(280px,1.5fr) minmax(220px,1.2fr) minmax(140px,0.7fr)';

export function AdminCompanySitesTable({
  sites,
}: {
  sites: AdminCompanyDetailSite[];
}) {
  const columns: DataGridTableColumn<AdminCompanyDetailSite>[] = [
    {
      id: 'siteName',
      label: '拠点名',
      sortable: true,
      sortValue: (site) => site.siteName,
      render: (site) => (
        <span className="text-body font-bold">{site.siteName || '-'}</span>
      ),
    },
    {
      id: 'picEmail',
      label: '担当者メールアドレス',
      sortable: true,
      sortValue: (site) => site.picEmail,
      render: (site) => (
        <span className="break-all text-body">{site.picEmail || '-'}</span>
      ),
    },
    {
      id: 'address',
      label: '所在地',
      sortable: true,
      sortValue: (site) => formatAdminCompanyAddress(site),
      render: (site) => (
        <span className="break-words text-body">
          {formatAdminCompanyAddress(site)}
        </span>
      ),
    },
    {
      id: 'projects',
      label: '参加プロジェクト',
      sortable: true,
      sortValue: (site) => site.projectNames.join('、'),
      render: (site) =>
        site.projectNames.length > 0 ? (
          <div className="flex flex-wrap gap-1.5">
            {site.projectNames.map((projectName, index) => (
              <Badge key={`${projectName}-${index}`} variant="secondary">
                {projectName}
              </Badge>
            ))}
          </div>
        ) : (
          <span className="text-body">-</span>
        ),
    },
    {
      id: 'createdAt',
      label: '登録日時',
      sortable: true,
      sortValue: (site) => site.createdAt,
      render: (site) => (
        <span className="text-body">
          {formatAdminCompanyDateTime(site.createdAt)}
        </span>
      ),
    },
  ];

  return (
    <DataGridTable
      rows={sites}
      columns={columns}
      gridTemplateColumns={siteTableColumns}
      minWidth={1100}
      getRowKey={(site) => `${site.siteName}-${site.picEmail}`}
      emptyMessage="登録されている拠点はありません。"
    />
  );
}
