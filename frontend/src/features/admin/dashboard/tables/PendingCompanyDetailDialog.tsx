import { useState } from 'react';
import { ChevronDown } from 'lucide-react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import type { PendingCompanyItem } from '@/api/admin/pendingCompanies';

function FieldRow({
  label,
  value,
  className,
}: {
  label: string;
  value?: string | number | boolean | null;
  className?: string;
}) {
  return (
    <div className={className}>
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="break-words font-medium">
        {value === null || value === undefined || value === '' ? '-' : String(value)}
      </div>
    </div>
  );
}

function ListValue({ items }: { items: string[] }) {
  if (!items || items.length === 0) {
    return <div className="font-medium">-</div>;
  }

  return (
    <div className="flex flex-wrap gap-2">
      {items.map((item) => (
        <span key={item} className="rounded-md border bg-white px-2 py-1 text-sm font-medium">
          {item}
        </span>
      ))}
    </div>
  );
}

export function PendingCompanyDetailDialog({
  open,
  item,
  onOpenChange,
}: {
  open: boolean;
  item: PendingCompanyItem | null;
  onOpenChange: (open: boolean) => void;
}) {
  const [expandedIndex, setExpandedIndex] = useState<number | null>(null);

  const toggleAccordion = (index: number) => {
    setExpandedIndex(expandedIndex === index ? null : index);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] w-[92%] max-w-3xl overflow-y-auto rounded-xl">
        <DialogHeader>
          <DialogTitle>申請情報の詳細</DialogTitle>
        </DialogHeader>

        <div className="space-y-6 pb-4">
          <div className="rounded-lg border bg-slate-50 p-4">
            <h3 className="mb-4 text-sm font-bold">会社情報</h3>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <FieldRow label="会社ID" value={item?.companyId} />
              <FieldRow label="会社名" value={item?.nameJA} />
              <FieldRow label="会社名カナ" value={item?.nameFurigana} />
              <FieldRow label="会社名英語" value={item?.nameEN} />
              <FieldRow label="メールアドレス" value={item?.email} />
              <FieldRow label="電話番号" value={item?.phoneNumber} />
              <FieldRow label="FAX番号" value={item?.fax} />
              <FieldRow label="法人番号" value={item?.corpNumber} />
              <FieldRow label="住所" value={item?.address} className="md:col-span-2" />
            </div>
          </div>

          <div className="rounded-lg border bg-slate-50 p-4">
            <h3 className="mb-4 text-sm font-bold">申請内容</h3>
            <div className="space-y-4">
              <div>
                <div className="mb-2 text-xs text-muted-foreground">参加したいプロジェクト</div>
                <ListValue items={item?.requestedMethodologies ?? []} />
              </div>
              <FieldRow
                label="現在の活動情報"
                value={item?.activityInformation}
                className="whitespace-pre-wrap"
              />
            </div>
          </div>

          {item?.methodologies && item.methodologies.length > 0 && (
            <div className="rounded-lg border bg-slate-50 p-4">
              <h3 className="mb-3 text-sm font-bold">方法論詳細</h3>
              <div className="space-y-2">
                {item.methodologies.map((methodology, idx) => (
                  <div key={idx} className="overflow-hidden rounded-lg border bg-white">
                    <button
                      onClick={() => toggleAccordion(idx)}
                      className="flex w-full items-center justify-between p-3 transition-colors hover:bg-slate-100"
                    >
                      <div className="flex flex-1 items-center gap-3 text-left">
                        <span className="text-sm font-semibold">{methodology.methodology}</span>
                        <span className="rounded bg-blue-100 px-2 py-1 text-xs text-blue-700">
                          {methodology.hasSubsidy
                            ? `補助金: ${methodology.subsidyName || '未設定'}`
                            : '補助金なし'}
                        </span>
                      </div>
                      <ChevronDown
                        className={`h-4 w-4 transition-transform ${
                          expandedIndex === idx ? 'rotate-180' : ''
                        }`}
                      />
                    </button>

                    {expandedIndex === idx && (
                      <div className="space-y-4 border-t bg-slate-50 p-3">
                        {methodology.remarks && (
                          <div>
                            <FieldRow label="備考" value={methodology.remarks} />
                          </div>
                        )}

                        {methodology.equipmentItems && methodology.equipmentItems.length > 0 && (
                          <div className="border-t pt-3">
                            <p className="mb-3 text-xs font-bold text-slate-700">設備情報</p>
                            <div className="space-y-3">
                              {methodology.equipmentItems.map((equipment, eIdx) => (
                                <div key={eIdx} className="rounded border border-slate-200 bg-white p-3">
                                  <div className="grid grid-cols-1 gap-3 text-xs md:grid-cols-2">
                                    <FieldRow label="設置日" value={equipment.installationDate} />
                                    <FieldRow label="メーカー" value={equipment.manufacturer} />
                                    <FieldRow label="型番" value={equipment.modelNo} />
                                    <FieldRow
                                      label="設置場所"
                                      value={equipment.installationAddress}
                                      className="md:col-span-2"
                                    />
                                  </div>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
