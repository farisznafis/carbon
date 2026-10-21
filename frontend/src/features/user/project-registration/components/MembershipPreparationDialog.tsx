import type { SiteProject } from '@/api/site';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

type MembershipPreparationDialogProps = Readonly<{
  project: SiteProject | null;
  onClose: () => void;
  onConfirm: () => void;
}>;

// Keeps the preparation checklist content structured and easy to maintain in one place.
const MEMBERSHIP_PREPARATION_SECTIONS = [
  {
    title: '設備情報',
    items: [
      '設備の稼働開始日',
      '太陽光パネルの公称最大出力',
      'パワーコンディショナーのメーカー、型式、台数、製造番号',
      'パワーコンディショナーの夜間待機電力',
      '蓄電池の設置有無',
      '蓄電池のメーカー、型式、容量（設置している場合）',
      '設備の総投資額',
      '補助金の受給有無、名称、金額（受給している場合）',
      'プロジェクト実施前の電力購入単価',
    ],
  },
  {
    title: '確認資料',
    items: [
      '設備の稼働開始日を確認できる資料',
      '太陽光パネルの仕様書またはカタログ',
      'パワーコンディショナーの仕様書',
      '蓄電池の仕様書またはカタログ（設置している場合）',
      '設備の総投資額を確認できる資料',
      '補助金交付決定を確認できる資料（補助金を受給している場合）',
      '電力購入単価を確認できる資料',
    ],
    note: '対応ファイル形式：PDF、CSV、JPG、JPEG、PNG',
  },
  {
    title: '確認・同意事項',
    items: [
      '各確認事項への回答および同意',
      '事業者名、代表者名、代表者役職',
      '環境社会配慮および持続可能性に関する同意',
      'プロジェクト入会規約への同意',
    ],
    note: '該当する場合、特定排出者コードや省エネ法に関する事業者番号・工場番号の入力が必要です。',
  },
] as const;

/** Shows the preparation checklist before navigating to the membership form. */
export function MembershipPreparationDialog({
  project,
  onClose,
  onConfirm,
}: MembershipPreparationDialogProps) {
  return (
    <Dialog
      open={project !== null}
      onOpenChange={(open) => {
        if (!open) {
          onClose();
        }
      }}
    >
      <DialogContent className="flex max-h-[calc(100dvh-2rem)] w-[calc(100vw-2rem)] max-w-2xl flex-col gap-0 overflow-hidden rounded-lg p-0">
        <DialogHeader className="shrink-0 border-b border-border p-6 pr-12">
          <DialogTitle>入会届の提出に必要なもの</DialogTitle>
          <DialogDescription className="text-small font-normal text-muted-foreground">
            <span className="block font-bold text-foreground">
              {project?.projectName || '-'}
            </span>
            <span className="mt-1 block">
              申請を始める前に、以下の情報と確認資料をご準備ください。
            </span>
          </DialogDescription>
        </DialogHeader>

        <div className="min-h-0 flex-1 overflow-y-auto px-6 py-5">
          <div className="space-y-6">
            {MEMBERSHIP_PREPARATION_SECTIONS.map((section) => (
              <section key={section.title} className="space-y-3">
                <h3 className="text-base font-bold leading-6 text-foreground">
                  {section.title}
                </h3>
                <ul className="list-disc space-y-2 pl-5 text-small font-normal text-foreground marker:text-primary-strong">
                  {section.items.map((item) => (
                    <li key={item} className="pl-1">
                      {item}
                    </li>
                  ))}
                </ul>
                {'note' in section && (
                  <p className="rounded-lg bg-muted px-4 py-3 text-small font-normal text-foreground">
                    {section.note}
                  </p>
                )}
              </section>
            ))}
          </div>
        </div>

        <DialogFooter className="shrink-0 border-t border-border p-6">
          <DialogClose asChild>
            <Button type="button" variant="outline" size="md" className="w-full sm:w-auto">
              閉じる
            </Button>
          </DialogClose>
          <Button type="button" size="md" className="w-full sm:w-auto" onClick={onConfirm}>
            入会届へ進む
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
