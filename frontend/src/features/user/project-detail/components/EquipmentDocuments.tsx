import { useState } from 'react';
import { ExternalLink, LoaderCircle } from 'lucide-react';
import {
  getSiteProjectEquipmentPresignedUrl,
  type SiteProjectEquipment,
  type SiteProjectEquipmentFile,
} from '@/api/site';
import { Button } from '@/components/ui/button';
import {
  formatEquipmentFileType,
  getEquipmentFileDisplayName,
} from '@/features/user/equipment/equipmentFileDisplay';
import { useToast } from '@/hooks/use-toast';

function openInNewTab(url: string) {
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.target = '_blank';
  anchor.rel = 'noopener noreferrer';
  anchor.click();
}

export function EquipmentDocuments({
  equipment,
}: {
  equipment: SiteProjectEquipment;
}) {
  const { toast } = useToast();
  const [openingFileId, setOpeningFileId] = useState<string | null>(null);
  const files = equipment.files ?? [];
  const evidenceDocuments = equipment.evidenceDocuments ?? [];

  const showFile = async (file: SiteProjectEquipmentFile) => {
    if (!file.s3Key || openingFileId) return;
    const fileKey = file.fileId || file.s3Key;
    setOpeningFileId(fileKey);

    try {
      openInNewTab(await getSiteProjectEquipmentPresignedUrl(file.s3Key));
    } catch (error) {
      toast({
        title: 'ファイルを表示できませんでした',
        description:
          error instanceof Error
            ? error.message
            : 'ファイルの取得に失敗しました。',
        variant: 'destructive',
      });
    } finally {
      setOpeningFileId(null);
    }
  };

  return (
    <section className="space-y-4 rounded-xl border border-border bg-card p-5">
      <h3 className="text-large text-foreground">確認資料</h3>

      {files.length === 0 && evidenceDocuments.length === 0 ? (
        <p className="text-small text-muted-foreground">
          登録された資料はありません。
        </p>
      ) : (
        <div className="space-y-3">
          {files.map((file, index) => {
            const fileKey = file.fileId || file.s3Key || String(index);
            return (
              <div
                key={fileKey}
                className="flex flex-col gap-3 rounded-lg border border-border p-4 sm:flex-row sm:items-center sm:justify-between"
              >
                <div className="min-w-0">
                  <p className="font-medium text-foreground">
                    {formatEquipmentFileType(file.fileType)}
                  </p>
                  <p className="mt-1 break-all text-small text-muted-foreground">
                    {getEquipmentFileDisplayName(file)}
                  </p>
                </div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={!file.s3Key || openingFileId !== null}
                  onClick={() => void showFile(file)}
                >
                  {openingFileId === fileKey ? (
                    <LoaderCircle className="animate-spin" />
                  ) : (
                    <ExternalLink />
                  )}
                  表示
                </Button>
              </div>
            );
          })}

          {evidenceDocuments.map((document, index) => (
            <p
              key={`${document}-${index}`}
              className="break-all rounded-lg border border-border p-4 text-small text-foreground"
            >
              {document || '-'}
            </p>
          ))}
        </div>
      )}
    </section>
  );
}
