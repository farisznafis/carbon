import type { UserList } from '@/api/users';
import {
  DataGridTable,
  type DataGridTableColumn,
} from '@/components/data-display/DataGridTable';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { InlineHelp } from '@/components/feedback/InlineHelp/InlineHelp';
import { RowActionButton } from '@/components/data-display/RowActionButton';
import { usePagination } from '@/hooks/usePagination';
import { Users } from 'lucide-react';
import { useMemo } from 'react';

const userTableGridColumns =
  'minmax(180px, 1fr) minmax(240px, 1.25fr) minmax(180px, 1fr) minmax(180px, 1fr) minmax(100px, 0.55fr)';

type UserTableProps = {
  users: UserList[];
  onEdit?: (user: UserList) => void;
  onRowClick?: (user: UserList) => void;
  masterSiteLabel?: string;
};

export function UserTable({
  users,
  onEdit,
  onRowClick,
  masterSiteLabel,
}: UserTableProps) {
  const sortedUsers = useMemo(() => {
    return [...users].sort((a, b) => {
      const aIsMaster = a.role === 'master';
      const bIsMaster = b.role === 'master';

      if (aIsMaster && !bIsMaster) return -1;
      if (!aIsMaster && bIsMaster) return 1;
      return 0;
    });
  }, [users]);

  const columns: DataGridTableColumn<UserList>[] = [
    {
      id: 'name',
      label: '氏名',
      sortable: true,
      sortValue: (user) =>
        `${user.lastName ?? ''} ${user.firstName ?? ''}`.trim(),
      render: (user) => {
        const fullName = `${user.lastName ?? ''} ${user.firstName ?? ''}`.trim();

        return (
          <div className="flex min-w-0 items-center gap-2">
            <span className="truncate text-body">{fullName || '-'}</span>
            {user.role === 'master' && (
              <StatusBadge tone="info">マスター</StatusBadge>
            )}
          </div>
        );
      },
    },
    {
      id: 'email',
      label: 'メールアドレス',
      sortable: true,
      sortValue: (user) => user.email,
      render: (user) => <span className="block truncate text-body">{user.email}</span>,
    },
    {
      id: 'username',
      label: 'ユーザーネーム',
      sortable: true,
      sortValue: (user) => user.username,
      render: (user) => (
        <span className="block truncate text-body">{user.username ?? '—'}</span>
      ),
    },
    {
      id: 'site',
      label: '拠点',
      sortable: true,
      sortValue: (user) => user.siteName ?? user.siteId,
      render: (user) => {
        if (user.role === 'master' && masterSiteLabel) {
          return <StatusBadge tone="info">{masterSiteLabel}</StatusBadge>;
        }

        if (user.siteId == null || user.siteId === '') {
          return <StatusBadge tone="danger">未接続</StatusBadge>;
        }

        return <span className="block truncate text-body">{user.siteName ?? user.siteId}</span>;
      },
    },
    {
      id: 'actions',
      label: '操作',
      render: (user) => (
        <RowActionButton onClick={() => onEdit?.(user)}>更新</RowActionButton>
      ),
    },
  ];

  const {
    currentPage,
    totalPages,
    totalItems,
    itemsPerPage,
    paginatedItems: paginatedUsers,
    goToPage,
    setItemsPerPage,
  } = usePagination(sortedUsers);

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2 text-2xl font-bold">
        <Users className="h-6 w-6" />
        <span>社員</span>
        <InlineHelp
          content={
            <div className="text-sm">
              社員の行をクリックすると、プロファイル詳細へ移動します。
            </div>
          }
        />
      </div>
      <DataGridTable
        rows={paginatedUsers}
        columns={columns}
        gridTemplateColumns={userTableGridColumns}
        minWidth={960}
        getRowKey={(user) => user.id}
        emptyMessage="まだ社員が登録されていません"
        onRowClick={onRowClick}
        getRowActionLabel={() => 'プロファイル'}
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
