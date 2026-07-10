import type {
  AdminCompanyListItem,
  CompanyStatus,
} from '@/api/admin/pendingCompanies';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

type AdminCompanyApplicationsTableProps = {
  items: AdminCompanyListItem[];
  emptyMessage: string;
  onDetails: (item: AdminCompanyListItem) => void;
  onReview: (item: AdminCompanyListItem) => void;
  onResolveSuspension?: (item: AdminCompanyListItem) => void;
};

const statusMeta: Record<
  CompanyStatus,
  {
    label: string;
    className: string;
  }
> = {
  PENDING: {
    label: '承認待ち',
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
  onResolveSuspension,
}: {
  item: AdminCompanyListItem;
  onDetails: (item: AdminCompanyListItem) => void;
  onReview: (item: AdminCompanyListItem) => void;
  onResolveSuspension?: (item: AdminCompanyListItem) => void;
}) {
  if (item.status === 'PENDING') {
    return (
      <div className='flex justify-start gap-2'>
        <Button
          type='button'
          variant='outline'
          onClick={() => onDetails(item)}
        >
          詳細
        </Button>
        <Button
          type='button'
          onClick={() => onReview(item)}
        >
          申請内容を確認
        </Button>
      </div>
    );
  }

  if (item.status === 'ACTIVE') {
    return (
      <div className='flex justify-start'>
        <Button
          type='button'
          variant='outline'
          onClick={() => onDetails(item)}
        >
          詳細
        </Button>
      </div>
    );
  }

  if (item.status === 'SUSPENDED') {
    return (
      <div className='flex justify-start gap-2'>
        <Button
          type='button'
          variant='outline'
          onClick={() => onDetails(item)}
        >
          詳細
        </Button>
        <Button
          type='button'
          onClick={() => onResolveSuspension?.(item)}
        >
          停止解除
        </Button>
      </div>
    );
  }

  return (
    <div className='flex justify-start gap-2'>
      <Button
        type='button'
        variant='outline'
        onClick={() => onDetails(item)}
      >
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
  onResolveSuspension,
}: AdminCompanyApplicationsTableProps) {
  return (
    <div className='overflow-hidden rounded-xl border border-border bg-card'>
      <div className='grid grid-cols-[minmax(180px,1.4fr)_minmax(100px,0.8fr)_minmax(240px,1.45fr)_minmax(100px,0.6fr)_minmax(220px,1fr)] gap-4 border-b border-border bg-muted/40 px-5 py-3 text-table font-bold text-foreground'>
        <span>企業名</span>
        <span>代表者</span>
        <span>メールアドレス</span>
        <span>ステータス</span>
        <span>アクション</span>
      </div>

      {items.length === 0 ? (
        <div className='px-5 py-8 text-center text-small text-muted-foreground'>
          {emptyMessage}
        </div>
      ) : (
        items.map((item) => (
          <div
            key={item.companyId}
            className='grid grid-cols-[minmax(180px,1.4fr)_minmax(100px,0.8fr)_minmax(240px,1.45fr)_minmax(100px,0.6fr)_minmax(220px,1fr)] items-center gap-4 border-b border-border px-5 py-4 last:border-b-0'
          >
            <span className='text-body font-bold text-slate-800'>
              {item.nameJA || '-'}
            </span>
            <span className='text-body text-foreground'>
              {formatRepresentativeName(item)}
            </span>
            <span className='break-all text-body text-foreground'>
              {item.email || '-'}
            </span>
            <span>
              <StatusBadge status={item.status} />
            </span>
            <ActionButtons
              item={item}
              onDetails={onDetails}
              onReview={onReview}
              onResolveSuspension={onResolveSuspension}
            />
          </div>
        ))
      )}
    </div>
  );
}
