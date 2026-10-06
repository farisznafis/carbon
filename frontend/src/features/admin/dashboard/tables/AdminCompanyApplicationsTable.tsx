import type { AdminCompanyListItem } from '@/api/admin/pendingCompanies';
import {
  DataGridTable,
  type DataGridTableColumn,
} from '@/components/data-display/DataGridTable';
import { RowActionButton } from '@/components/data-display/RowActionButton';
import { CompanyStatusBadge } from '@/features/admin/company-detail/CompanyStatusBadge';
import { COMPANY_STATUS_CONTENT } from '@/features/admin/company-detail/companyStatusContent';

type AdminCompanyApplicationsTableProps = {
  items: AdminCompanyListItem[];
  emptyMessage: string;
  onDetails: (item: AdminCompanyListItem) => void;
  onReview: (item: AdminCompanyListItem) => void;
};

const companyTableGridColumns =
  'minmax(180px, 1.35fr) minmax(110px, 0.75fr) minmax(240px, 1.45fr) minmax(110px, 0.65fr) minmax(220px, 1fr)';

function formatRepresentativeName(item: AdminCompanyListItem) {
  const name = [item.representativeLastName, item.representativeFirstName]
    .filter(Boolean)
    .join(' ')
    .trim();

  return name || '-';
}

/** Pending applications need a decision, so they open the review; everything else opens details. */
function isPendingApplication(item: AdminCompanyListItem) {
  return item.status === 'PENDING';
}

export function AdminCompanyApplicationsTable({
  items,
  emptyMessage,
  onDetails,
  onReview,
}: AdminCompanyApplicationsTableProps) {
  const openRow = (item: AdminCompanyListItem) =>
    isPendingApplication(item) ? onReview(item) : onDetails(item);

  const columns: DataGridTableColumn<AdminCompanyListItem>[] = [
    {
      id: 'nameJA',
      label: '企業名',
      sortable: true,
      sortValue: (item) => item.nameJA,
      render: (item) => (
        <span className="block truncate text-body font-bold text-foreground">
          {item.nameJA || '-'}
        </span>
      ),
    },
    {
      id: 'representative',
      label: '代表者',
      sortable: true,
      sortValue: formatRepresentativeName,
      render: (item) => (
        <span className="block truncate text-body text-foreground">
          {formatRepresentativeName(item)}
        </span>
      ),
    },
    {
      id: 'email',
      label: 'メールアドレス',
      sortable: true,
      sortValue: (item) => item.email,
      render: (item) => (
        <span className="block break-all text-body text-foreground">
          {item.email || '-'}
        </span>
      ),
    },
    {
      id: 'status',
      label: 'ステータス',
      sortable: true,
      sortValue: (item) => COMPANY_STATUS_CONTENT[item.status].label,
      render: (item) => <CompanyStatusBadge status={item.status} />,
    },
    {
      id: 'actions',
      label: '操作',
      render: (item) =>
        isPendingApplication(item) ? (
          <RowActionButton intent="review" onClick={() => openRow(item)}>
            申請内容を確認
          </RowActionButton>
        ) : (
          <RowActionButton onClick={() => openRow(item)} />
        ),
    },
  ];

  return (
    <DataGridTable
      rows={items}
      columns={columns}
      gridTemplateColumns={companyTableGridColumns}
      minWidth={980}
      getRowKey={(item) => item.id ?? item.companyId}
      emptyMessage={emptyMessage}
      onRowClick={openRow}
    />
  );
}
