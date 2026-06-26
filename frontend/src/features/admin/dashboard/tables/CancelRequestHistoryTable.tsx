import { Action, Column, ProTable } from '@/components/data-display/ProTable/ProTable';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { usePagination } from '@/hooks/usePagination';

export type CancelRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export type CancelRequestItem = {
  id: string;
  userName: string;
  siteName: string;
  reason: string;
  requestedAt: string;
  status: CancelRequestStatus;
};

type CancelRequestHistoryTableProps = {
  items: CancelRequestItem[];
  onApprove: (id: string) => void;
  onReject: (id: string) => void;
  showTitle?: boolean;
};

export function CancelRequestHistoryTable({
  items,
  onApprove,
  onReject,
  showTitle = true,
}: CancelRequestHistoryTableProps) {
  const {
    currentPage,
    totalPages,
    totalItems,
    paginatedItems,
    goToPage,
    itemsPerPage,
    setItemsPerPage,
  } = usePagination(items);

  const columns: Column<CancelRequestItem>[] = [
    {
      key: 'id',
      label: '申請ID',
      sortable: true,
      truncate: true,
      maxWidth: 'max-w-[140px]',
    },
    {
      key: 'userName',
      label: 'ユーザ名',
      sortable: true,
      truncate: true,
      maxWidth: 'max-w-[180px]',
    },
    {
      key: 'siteName',
      label: '拠点名',
      sortable: true,
      truncate: true,
      maxWidth: 'max-w-[180px]',
    },
    {
      key: 'reason',
      label: '理由',
      sortable: false,
      truncate: true,
      maxWidth: 'max-w-[260px]',
    },
    {
      key: 'requestedAt',
      label: '申請日時',
      sortable: true,
      truncate: true,
      maxWidth: 'max-w-[160px]',
    },
    {
      key: 'status',
      label: 'ステータス',
      sortable: true,
      truncate: true,
      maxWidth: 'max-w-[120px]',
      render: (_, row) => (
        <span
          className={
            row.status === 'PENDING'
              ? 'rounded-full px-2 py-1 text-xs bg-amber-100 text-amber-800'
              : row.status === 'APPROVED'
                ? 'rounded-full px-2 py-1 text-xs bg-emerald-100 text-emerald-800'
                : 'rounded-full px-2 py-1 text-xs bg-red-100 text-red-800'
          }
        >
          {row.status}
        </span>
      ),
    },
  ];

  const actions: Action<CancelRequestItem>[] = [
    {
      label: '承認',
      onClick: (row) => onApprove(row.id),
      colorClass: 'text-white',
    },
    {
      label: '却下',
      onClick: (row) => onReject(row.id),
      colorClass: 'text-white',
    },
  ];

  return (
    <div className='space-y-4'>
      <ProTable
      title={
        showTitle ? (
          <p className='text-2xl font-bold flex items-center gap-2'>
            取消一覧
          </p>
        ) : undefined
      }
      data={paginatedItems}
      columns={columns}
      actions={actions}
      height='max-h-[420px]'
      emptyState={
        <p className='text-muted-foreground'>取消一覧はありません</p>
      }
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
