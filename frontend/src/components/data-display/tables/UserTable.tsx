import {
  Action,
  Column,
  ProTable,
} from '@/components/data-display/ProTable/ProTable';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { InlineHelp } from '@/components/feedback/InlineHelp/InlineHelp';
import { Badge } from '@/components/ui/badge';
import type { UserList } from '@/api/users';
import { usePagination } from '@/hooks/usePagination';
import { Users } from 'lucide-react';

type UserTableProps = {
  users: UserList[];
  onEdit?: (user: UserList) => void;
  onRowClick?: (user: UserList) => void;
};

export function UserTable({ users, onEdit, onRowClick }: UserTableProps) {
  const userColumns: Column<UserList>[] = [
    {
      id: 'user-name',
      key: 'firstName',
      label: '氏名',
      render: (_value, row) => {
        const fullName = `${row.lastName ?? ''} ${row.firstName ?? ''}`.trim();

        return (
          <div className='flex items-center gap-2'>
            <span>{fullName}</span>
            {row.role === 'master' && (
              <Badge className='bg-blue-600 hover:bg-blue-600 text-white font-medium px-2 py-0.5 text-xs rounded'>
                マスタ
              </Badge>
            )}
          </div>
        );
      },
      sortable: true,
      truncate: true,
      maxWidth: 'max-w-[180px]',
    },
    {
      id: 'user-email',
      key: 'email',
      label: 'メール',
      sortable: true,
      truncate: true,
      maxWidth: 'max-w-[240px]',
    },
    {
      id: 'user-kana',
      key: 'firstNameKana',
      label: 'ユーザー名',
      render: (_value, row) =>
        `${row.lastNameKana ?? ''} ${row.firstNameKana ?? ''}`.trim(),
      sortable: true,
      truncate: true,
      maxWidth: 'max-w-[220px]',
    },
    {
      id: 'user-site',
      key: 'siteId',
      label: '拠点',
      render: (_value, row) => {
        if (row.siteId == null || row.siteId === '') {
          return <Badge variant='destructive'>拠点に未接続</Badge>;
        }

        return row.siteName ?? row.siteId;
      },
      sortable: true,
      truncate: true,
      maxWidth: 'max-w-[220px]',
    },
  ];

  const userActions: Action<UserList>[] = [
    {
      label: '更新',
      onClick: (user) => onEdit?.(user),
      colorClass: 'text-white',
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
  } = usePagination(users);

  return (
    <>
      <ProTable
        title={
          <p className='flex items-center gap-2 text-2xl font-bold'>
            <Users className='h-6 w-6' />
            ユーザ
            <InlineHelp
              content={
                <div className='text-sm'>
                  ユーザの行をクリックすると、プロフィール詳細へ移動します。
                </div>
              }
            />
          </p>
        }
        data={paginatedUsers}
        columns={userColumns}
        actions={userActions}
        onRowClick={onRowClick}
        height='max-h-[300px]'
        emptyState={
          <p className='text-muted-foreground'>
            まだユーザが登録されていません
          </p>
        }
        clickable={Boolean(onRowClick)}
      />

      <div className='mt-4'>
        <PaginationControls
          currentPage={currentPage}
          totalPages={totalPages}
          totalItems={totalItems}
          itemsPerPage={itemsPerPage}
          onPageChange={goToPage}
          onItemsPerPageChange={setItemsPerPage}
        />
      </div>
    </>
  );
}
