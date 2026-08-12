import { useState } from 'react';
import { ExternalLink, LoaderCircle } from 'lucide-react';
import { getSiteProjectEquipmentPresignedUrl } from '@/api/site';
import { Button } from '@/components/ui/button';
import { useToast } from '@/hooks/use-toast';
import { cn } from '@/lib/utils';

type PresignedFileLinkProps = {
  s3Key: string;
  label: string;
  className?: string;
};

function openInNewTab(url: string) {
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.target = '_blank';
  anchor.rel = 'noopener noreferrer';
  anchor.click();
}

export function PresignedFileLink({
  s3Key,
  label,
  className,
}: PresignedFileLinkProps) {
  const { toast } = useToast();
  const [isLoading, setIsLoading] = useState(false);

  const handleOpen = async () => {
    if (!s3Key.trim() || isLoading) return;

    setIsLoading(true);
    try {
      openInNewTab(await getSiteProjectEquipmentPresignedUrl(s3Key));
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
      setIsLoading(false);
    }
  };

  return (
    <Button
      type="button"
      variant="link"
      className={cn('h-auto gap-1 p-0 text-sm font-normal underline', className)}
      disabled={!s3Key.trim() || isLoading}
      aria-label={`${label}を表示`}
      onClick={() => void handleOpen()}
    >
      {isLoading ? (
        <LoaderCircle className="h-3.5 w-3.5 animate-spin" />
      ) : (
        <ExternalLink className="h-3.5 w-3.5" />
      )}
      {label}
    </Button>
  );
}
