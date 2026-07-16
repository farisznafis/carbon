import type { AdminCompanyDetailUser } from '@/api/admin';
import {
  AdminGridTable,
  type AdminGridTableColumn,
} from '@/components/data-display/AdminGridTable';
import { formatAdminRole } from '../companyDetailDisplay';

const userTableColumns =
  'minmax(140px,0.8fr) minmax(160px,1fr) minmax(240px,1.4fr) minmax(100px,0.6fr) minmax(160px,1fr)';

export function AdminCompanyUsersTable({
  users,
}: {
  users: AdminCompanyDetailUser[];
}) {
  const columns: AdminGridTableColumn<AdminCompanyDetailUser>[] = [
    {
      id: 'username',
      label: 'ユーザ名',
      sortable: true,
      sortValue: (user) => user.username,
      render: (user) => <span className="text-body">{user.username || '-'}</span>,
    },
    {
      id: 'fullName',
      label: '氏名',
      sortable: true,
      sortValue: (user) => user.fullName,
      render: (user) => (
        <span className="text-body font-bold">{user.fullName || '-'}</span>
      ),
    },
    {
      id: 'email',
      label: 'メールアドレス',
      sortable: true,
      sortValue: (user) => user.email,
      render: (user) => (
        <span className="break-all text-body">{user.email || '-'}</span>
      ),
    },
    {
      id: 'role',
      label: '権限',
      sortable: true,
      sortValue: (user) => formatAdminRole(user.role),
      render: (user) => (
        <span className="text-body">{formatAdminRole(user.role)}</span>
      ),
    },
    {
      id: 'siteName',
      label: '拠点名',
      sortable: true,
      sortValue: (user) => user.siteName,
      render: (user) => <span className="text-body">{user.siteName || '-'}</span>,
    },
  ];

  return (
    <AdminGridTable
      rows={users}
      columns={columns}
      gridTemplateColumns={userTableColumns}
      minWidth={940}
      getRowKey={(user) => `${user.username}-${user.email}`}
      emptyMessage="登録されているユーザはいません。"
    />
  );
}
