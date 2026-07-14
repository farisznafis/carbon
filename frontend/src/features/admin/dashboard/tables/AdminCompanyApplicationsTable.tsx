import type {
  AdminCompanyListItem,
  CompanyStatus,
} from '@/api/admin/pendingCompanies';
import {
  AdminGridTable,
  type AdminGridTableColumn,
} from '@/components/data-display/AdminGridTable';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

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
    className: string;
  }
> = {
  PENDING: {
    label: '申請受付',
    className: 'bg-amber-100 text-amber-800',
  },
  REJECTED: {
    label: '却下',
    className: 'bg-red-100 text-red-800',
  },
  ACTIVE: {
    label: '承認済み',
    className: 'bg-emerald-100 text-emerald-800',
  },
  SUSPENDED: {
    label: '利用停止',
    className: 'bg-slate-100 text-slate-700',
  },
};

function formatRepresentativeName(item: AdminCompanyListItem) {
  const name = [item.representativeLastName, item.representativeFirstName]
    .filter(Boolean)
    .join(' ')
    .trim();

  return name || '-';
}

function StatusBadge({ status }: { status: CompanyStatus }) {
  const meta = statusMeta[status];

  return (
    <span
      className={cn(
        'inline-flex rounded-md px-2 py-0.5 text-table font-bold',
        meta.className,
      )}
    >
      {meta.label}
    </span>
  );
}

function ActionButtons({
  item,
  onDetails,
  onReview,
}: {
  item: AdminCompanyListItem;
  onDetails: (item: AdminCompanyListItem) => void;
  onReview: (item: AdminCompanyListItem) => void;
}) {
  if (item.status === 'PENDING') {
    return (
      <div className="flex justify-start gap-2">
        {/* <Button type="button" variant="outline" onClick={() => onDetails(item)}>
          詳細
        </Button> */}
        <Button type="button" onClick={() => onReview(item)}>
          申請内容を確認
        </Button>
      </div>
    );
  }

  return (
    <div className="flex justify-start">
      <Button type="button" variant="outline" onClick={() => onDetails(item)}>
        詳細
      </Button>
    </div>
  );
}

export function AdminCompanyApplicationsTable({
  items,
  emptyMessage,
  onDetails,
  onReview,
}: AdminCompanyApplicationsTableProps) {
  const columns: AdminGridTableColumn<AdminCompanyListItem>[] = [
    {
      id: 'nameJA',
      label: '企業名',
      sortable: true,
      sortValue: (item) => item.nameJA,
      render: (item) => (
        <span className="block truncate text-body font-bold text-slate-800">
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
      render: (item) => <StatusBadge status={item.status} />,
    },
    {
      id: 'actions',
      label: 'アクション',
      render: (item) => (
        <div
          onClick={(event) => event.stopPropagation()}
          onPointerDown={(event) => event.stopPropagation()}
        >
          <ActionButtons
            item={item}
            onDetails={onDetails}
            onReview={onReview}
          />
        </div>
      ),
    },
  ];

  return (
    <AdminGridTable
      rows={items}
      columns={columns}
      gridTemplateColumns={companyTableGridColumns}
      minWidth={980}
      getRowKey={(item) => item.id ?? item.companyId}
      emptyMessage={emptyMessage}
    />
  );
}
