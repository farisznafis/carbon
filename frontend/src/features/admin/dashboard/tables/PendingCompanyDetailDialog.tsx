import { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import type { PendingCompanyItem } from '@/api/admin/pendingCompanies';
import { ChevronDown } from 'lucide-react';

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
      <div className="font-medium break-words">
        {value === null || value === undefined || value === '' ? '-' : String(value)}
      </div>
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
      <DialogContent className="max-w-3xl w-[92%] rounded-xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>申請情報の詳細</DialogTitle>
        </DialogHeader>

        <div className="space-y-6 pb-4">
          {/* Company Summary */}
          <div className="rounded-lg border bg-slate-50 p-4">
            <h3 className="font-bold text-sm mb-4">企業情報</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <FieldRow label="会員ID" value={item?.companyId} />
              <FieldRow label="企業名" value={item?.nameJA} />
              <FieldRow label="企業名（フリガナ）" value={item?.nameFurigana} />
              <FieldRow label="企業名（英）" value={item?.nameEN} />
              <FieldRow label="メールアドレス" value={item?.email} />
              <FieldRow label="電話番号" value={item?.phoneNumber} />
              <FieldRow label="FAX番号" value={item?.fax} />
              <FieldRow label="法人番号" value={item?.corpNumber} />
              <FieldRow label="住所" value={item?.address} className="md:col-span-2" />
            </div>
          </div>

          {/* Methodologies Section */}
          {item?.methodologies && item.methodologies.length > 0 && (
            <div className="rounded-lg border bg-slate-50 p-4">
              <h3 className="font-bold text-sm mb-3">対応方法論</h3>
              <div className="space-y-2">
                {item.methodologies.map((methodology, idx) => (
                  <div key={idx} className="border rounded-lg overflow-hidden bg-white">
                    {/* Accordion Header */}
                    <button
                      onClick={() => toggleAccordion(idx)}
                      className="w-full flex justify-between items-center p-3 hover:bg-slate-100 transition-colors"
                    >
                      <div className="flex gap-3 items-center flex-1 text-left">
                        <span className="font-semibold text-sm">{methodology.methodology}</span>
                        <span className="text-xs px-2 py-1 bg-blue-100 text-blue-700 rounded">
                          {methodology.hasSubsidy
                            ? `補助: ${methodology.subsidyName || '申請中'}`
                            : '補助なし'}
                        </span>
                      </div>
                      <ChevronDown
                        className={`w-4 h-4 transition-transform ${
                          expandedIndex === idx ? 'rotate-180' : ''
                        }`}
                      />
                    </button>

                    {/* Accordion Content */}
                    {expandedIndex === idx && (
                      <div className="border-t bg-slate-50 p-3 space-y-4">
                        {/* Remarks */}
                        {methodology.remarks && (
                          <div>
                            <FieldRow label="備考" value={methodology.remarks} />
                          </div>
                        )}

                        {/* Equipment Items */}
                        {methodology.equipmentItems &&
                          methodology.equipmentItems.length > 0 && (
                            <div className="pt-3 border-t">
                              <p className="text-xs font-bold mb-3 text-slate-700">設備一覧</p>
                              <div className="space-y-3">
                                {methodology.equipmentItems.map((equipment, eIdx) => (
                                  <div
                                    key={eIdx}
                                    className="p-3 bg-white rounded border border-slate-200"
                                  >
                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
                                      <FieldRow
                                        label="設置日"
                                        value={equipment.installationDate}
                                      />
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