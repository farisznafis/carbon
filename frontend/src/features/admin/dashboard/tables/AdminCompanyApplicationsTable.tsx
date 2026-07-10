import type {
  PendingCompanyItem,
  PendingCompanyStatus,
} from '@/api/admin/pendingCompanies';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

type AdminCompanyApplicationsTableProps = {
  items: PendingCompanyItem[];
  emptyMessage: string;
  onDetails: (item: PendingCompanyItem) => void;
  onReview: (item: PendingCompanyItem) => void;
  onDeleteRejected?: (item: PendingCompanyItem) => void;
};

const statusClassName: Record<PendingCompanyStatus, string> = {
  申請受付: 'bg-amber-100 text-amber-800',
  却下: 'bg-red-100 text-red-800',
  承認済み: 'bg-emerald-100 text-emerald-800',
  停止: 'bg-slate-100 text-slate-700',
};

function formatRepresentativeName(item: PendingCompanyItem) {
  const name = [
    item.representativeLastName,
    item.representativeFirstName,
  ]
    .filter(Boolean)
    .join(' ')
    .trim();

  return name || '-';
}

function StatusBadge({ status }: { status: PendingCompanyStatus }) {
  return (
    <span
      className={cn(
        'inline-flex rounded-md px-2.5 py-1 text-small font-bold',
        statusClassName[status],
      )}
    >
      {status}
    </span>
  );
}

function ActionButtons({
  item,
  onDetails,
  onReview,
  onDeleteRejected,
}: {
  item: PendingCompanyItem;
  onDetails: (item: PendingCompanyItem) => void;
  onReview: (item: PendingCompanyItem) => void;
  onDeleteRejected?: (item: PendingCompanyItem) => void;
}) {
  if (item.status === '申請受付') {
    return (
      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" size="md" onClick={() => onDetails(item)}>
          詳細
        </Button>
        <Button type="button" size="md" onClick={() => onReview(item)}>
          申請内容を確認
        </Button>
      </div>
    );
  }

  if (item.status === '却下') {
    return (
      <div className="flex justify-end">
        <Button
          type="button"
          variant="destructive"
          size="md"
          onClick={() => onDeleteRejected?.(item)}
        >
          削除
        </Button>
      </div>
    );
  }

  return (
    <div className="flex justify-end">
      <Button type="button" variant="outline" size="md" onClick={() => onDetails(item)}>
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
  onDeleteRejected,
}: AdminCompanyApplicationsTableProps) {
  return (
    <div className="overflow-hidden rounded-xl border border-border bg-card">
      <div className="grid grid-cols-[minmax(220px,1.6fr)_minmax(120px,1fr)_minmax(260px,1.6fr)_minmax(120px,0.7fr)_minmax(220px,1fr)] gap-4 border-b border-border bg-muted/40 px-6 py-4 text-small font-bold text-foreground">
        <span>企業名</span>
        <span>代表者</span>
        <span>メールアドレス</span>
        <span>ステータス</span>
        <span>アクション</span>
      </div>

      {items.length === 0 ? (
        <div className="px-6 py-10 text-center text-small text-muted-foreground">
          {emptyMessage}
        </div>
      ) : (
        items.map((item) => (
          <div
            key={item.id}
            className="grid grid-cols-[minmax(220px,1.6fr)_minmax(120px,1fr)_minmax(260px,1.6fr)_minmax(120px,0.7fr)_minmax(220px,1fr)] items-center gap-4 border-b border-border px-6 py-5 last:border-b-0"
          >
            <span className="text-large font-bold text-slate-800">
              {item.nameJA || '-'}
            </span>
            <span className="text-body text-foreground">
              {formatRepresentativeName(item)}
            </span>
            <span className="break-all text-body text-foreground">
              {item.email || '-'}
            </span>
            <span>
              <StatusBadge status={item.status} />
            </span>
            <ActionButtons
              item={item}
              onDetails={onDetails}
              onReview={onReview}
              onDeleteRejected={onDeleteRejected}
            />
          </div>
        ))
      )}
    </div>
  );
}
