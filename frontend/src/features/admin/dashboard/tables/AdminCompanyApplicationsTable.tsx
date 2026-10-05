import type {
  AdminCompanyListItem,
  CompanyStatus,
} from '@/api/admin/pendingCompanies';
import {
  DataGridTable,
  type DataGridTableColumn,
} from '@/components/data-display/DataGridTable';
import { RowActionButton } from '@/components/data-display/RowActionButton';
import { StatusBadge } from '@/components/data-display/StatusBadge';

type AdminCompanyApplicationsTableProps = {
  items: AdminCompanyListItem[];
  emptyMessage: string;
  onDetails: (item: AdminCompanyListItem) => void;
  onReview: (item: AdminCompanyListItem) => void;
};

const companyTableGridColumns =
  'minmax(180px, 1.35fr) minmax(110px, 0.75fr) minmax(240px, 1.45fr) minmax(110px, 0.65fr) minmax(220px, 1fr)';

const statusMeta: Record<
  CompanyStatus,
  {
    label: string;
    tone: 'warning' | 'danger' | 'success';
  }
> = {
  PENDING: {
    label: '申請受付',
    tone: 'warning',
  },
  REJECTED: {
    label: '却下',
    tone: 'danger',
  },
  ACTIVE: {
    label: '承認済み',
    tone: 'success',
  },
  SUSPENDED: {
    label: '利用停止',
    tone: 'danger',
  },
};

function formatRepresentativeName(item: AdminCompanyListItem) {
  const name = [item.representativeLastName, item.representativeFirstName]
    .filter(Boolean)
    .join(' ')
    .trim();

  return name || '-';
}

function CompanyStatusBadge({ status }: { status: CompanyStatus }) {
  const meta = statusMeta[status];

  return (
    <StatusBadge tone={meta.tone}>
      {meta.label}
    </StatusBadge>
  );
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
      sortValue: (item) => statusMeta[item.status].label,
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
