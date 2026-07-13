import { Action, Column, ProTable } from '@/components/data-display/ProTable/ProTable';
import type { PendingCompanyItem } from '@/api/admin/pendingCompanies';
import { useState } from 'react';
import { PendingCompanyDetailDialog } from './PendingCompanyDetailDialog';

type PendingCompanyTableProps = {
  items: PendingCompanyItem[];
  onApprove: (item: PendingCompanyItem) => void;
  onReject: (item: PendingCompanyItem) => void;
  onRowClick?: (item: PendingCompanyItem) => void;
};

export function PendingCompanyTable({
  items,
  onApprove,
  onReject,
  onRowClick,
}: PendingCompanyTableProps) {
  const [detailOpen, setDetailOpen] = useState(false);
  const [selected, setSelected] = useState<PendingCompanyItem | null>(null);

  const columns: Column<PendingCompanyItem>[] = [
    { key: 'companyId', label: '企業ID', sortable: true, truncate: true, maxWidth: 'max-w-[140px]' },
    { key: 'nameJA', label: '企業名', sortable: true, truncate: true, maxWidth: 'max-w-[180px]' },
    { key: 'email', label: 'メールアドレス', sortable: true, truncate: true, maxWidth: 'max-w-[220px]' },
    { key: 'phoneNumber', label: '電話番号', sortable: true, truncate: true, maxWidth: 'max-w-[160px]' },
    // { key: 'fax', label: 'FAX番号', sortable: true, truncate: true, maxWidth: 'max-w-[160px]' },
    // { key: 'corpNumber', label: '法人番号', sortable: true, truncate: true, maxWidth: 'max-w-[180px]' },
    // { key: 'address', label: '住所', sortable: true, truncate: true, maxWidth: 'max-w-[280px]' },
  ];

  const actions: Action<PendingCompanyItem>[] = [
    { label: '承認', onClick: (row) => onApprove(row), colorClass: 'bg-emerald-600 text-white hover:bg-emerald-700' },
    { label: '却下', onClick: (row) => onReject(row), colorClass: 'bg-rose-600 text-white hover:bg-rose-700' },
  ];

  const handleRowClick = (row: PendingCompanyItem) => {
    if (onRowClick) {
      onRowClick(row);
      return;
    }
    setSelected(row);
    setDetailOpen(true);
  };

  return (
    <>
      <ProTable<PendingCompanyItem>
        title={<p className="text-2xl font-bold flex items-center gap-2">申請中アカウント一覧</p>}
        data={items}
        columns={columns}
        actions={actions}
        height="max-h-[320px]"
        emptyState={<p className="text-muted-foreground">申請中アカウントはありません</p>}
        onRowClick={handleRowClick}
      />

      <PendingCompanyDetailDialog
        open={detailOpen}
        item={selected}
        onOpenChange={(o) => { if (!o) setDetailOpen(false); }}
      />
    </>
  );
}

export default PendingCompanyTable;
