import type { BatchHistoryItem } from '@/api/batch';
import type { ReactNode } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { BatchHistoryStatusBadge } from './BatchHistoryStatusBadge';
import { formatBatchHistoryDateTime } from './batchHistoryFormatters';

type BatchHistoryDetailDialogProps = {
  open: boolean;
  item: BatchHistoryItem | null;
  onOpenChange: (open: boolean) => void;
};

type DetailItem = {
  label: string;
  value: ReactNode;
};

export function BatchHistoryDetailDialog({
  open,
  item,
  onOpenChange,
}: Readonly<BatchHistoryDetailDialogProps>) {
  if (!item) return null;

  const details: DetailItem[] = [
    { label: 'ID', value: item.id },
    { label: 'ステータス', value: <BatchHistoryStatusBadge status={item.status} /> },
    { label: '有効状態', value: item.is_active ? '有効' : '無効' },
    { label: '開始日時', value: formatBatchHistoryDateTime(item.start_time) },
    { label: '終了日時', value: formatBatchHistoryDateTime(item.end_time) },
    { label: '作成者', value: item.created_by || '-' },
    { label: '作成日時', value: formatBatchHistoryDateTime(item.created_at) },
    { label: '更新者', value: item.updated_by || '-' },
    { label: '更新日時', value: formatBatchHistoryDateTime(item.updated_at) },
    { label: '処理担当者', value: item.claimed_by || '-' },
    { label: '処理取得日時', value: formatBatchHistoryDateTime(item.claimed_at) },
  ];

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] w-[calc(100vw-2rem)] max-w-2xl overflow-y-auto rounded-xl p-8">
        <DialogHeader>
          <DialogTitle className="text-foreground">バッチ履歴の詳細</DialogTitle>
          <DialogDescription className="sr-only">
            バッチ処理の詳細情報
          </DialogDescription>
        </DialogHeader>
        <dl className="grid grid-cols-1 gap-x-6 gap-y-4 sm:grid-cols-2">
          {details.map((detail) => (
            <div key={detail.label} className="space-y-1">
              <dt className="text-small font-medium text-foreground">
                {detail.label}
              </dt>
              <dd className="break-words text-body text-foreground">
                {detail.value}
              </dd>
            </div>
          ))}
        </dl>
      </DialogContent>
    </Dialog>
  );
}
