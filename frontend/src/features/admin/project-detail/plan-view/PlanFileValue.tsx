import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ExternalLink, FileText } from 'lucide-react';
import { getProjectPlanFileDownloadUrl } from '@/api/admin/projects';
import { Button } from '@/components/ui/button';
import { toast } from '@/hooks/use-toast';
import { fileNameFromKey, isImageKey } from './planViewValues';

type PlanFileValueProps = {
  projectId: string;
  s3Key: string;
};

/** Signed URLs last 15 minutes; refetch well before one expires on an open page. */
const URL_STALE_MS = 10 * 60 * 1000;

/** One attachment of a saved plan page: its name, a preview for images, and 表示. */
export function PlanFileValue({ projectId, s3Key }: Readonly<PlanFileValueProps>) {
  const [isOpening, setIsOpening] = useState(false);
  const isImage = isImageKey(s3Key);

  const preview = useQuery({
    queryKey: ['admin', 'projects', projectId, 'plan-file', s3Key],
    queryFn: () => getProjectPlanFileDownloadUrl(projectId, s3Key),
    enabled: isImage,
    staleTime: URL_STALE_MS,
    retry: false,
  });

  /**
   * Opens the tab before asking for the URL: a window opened after an await is
   * no longer tied to the click, and browsers block it as a popup.
   */
  const handleOpen = async () => {
    const tab = window.open('', '_blank');
    setIsOpening(true);
    try {
      const url = await getProjectPlanFileDownloadUrl(projectId, s3Key);
      if (tab) {
        tab.opener = null;
        tab.location.href = url;
      } else {
        window.location.assign(url);
      }
    } catch (e) {
      tab?.close();
      toast({
        title: 'ファイルを表示できませんでした',
        description: e instanceof Error ? e.message : undefined,
        variant: 'destructive',
      });
    } finally {
      setIsOpening(false);
    }
  };

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-3">
        <span className="flex min-w-0 items-center gap-2">
          <FileText className="h-4 w-4 shrink-0 text-muted-foreground" aria-hidden="true" />
          <span className="break-all">{fileNameFromKey(s3Key)}</span>
        </span>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => void handleOpen()}
          disabled={isOpening}
        >
          <ExternalLink aria-hidden="true" />
          {isOpening ? '読み込み中...' : '表示'}
        </Button>
      </div>
      {isImage && preview.data ? (
        <img
          src={preview.data}
          alt={fileNameFromKey(s3Key)}
          className="max-h-72 max-w-full rounded-md border border-border object-contain"
        />
      ) : null}
    </div>
  );
}
