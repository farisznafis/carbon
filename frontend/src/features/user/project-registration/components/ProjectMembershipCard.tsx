import type { SiteProject } from '@/api/site';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { cn } from '@/lib/utils';
import { Download, LoaderCircle } from 'lucide-react';

type ProjectMembershipStatus = 'available' | 'joined';

type ProjectMembershipCardProps = Readonly<{
  project: SiteProject;
  status: ProjectMembershipStatus;
  onJoin?: (project: SiteProject) => void;
  onDownloadPdf?: (project: SiteProject) => void;
  isDownloadingPdf?: boolean;
}>;

export function ProjectMembershipCard({
  project,
  status,
  onJoin,
  onDownloadPdf,
  isDownloadingPdf = false,
}: ProjectMembershipCardProps) {
  const isJoined = status === 'joined';

  return (
    <Card className='h-full rounded-lg border-border bg-card shadow-none'>
      <CardContent className='flex h-full flex-col p-6'>
        <div className='flex items-start justify-between gap-4'>
          <div className='min-w-0'>
            <h3 className='break-words text-foreground'>
              {project.projectName || '-'}
            </h3>
            <p className='text-small mt-2 whitespace-pre-wrap break-words text-foreground'>
              {project.methodDetails || '-'}
            </p>
          </div>
          <Badge
            className={cn(
              'shrink-0 border-transparent',
              isJoined
                ? 'bg-success/15 text-success hover:bg-success/15'
                : 'bg-warning/15 text-warning hover:bg-warning/15',
            )}
          >
            {isJoined ? '参加中' : '未参加'}
          </Badge>
        </div>

        <dl className='mt-6 grid gap-x-8 gap-y-5 md:grid-cols-8'>
          <div className='md:col-span-2'>
            <dt className='text-small text-foreground'>方法論名称</dt>
            <dd className='mt-1 break-words font-bold'>
              {project.methodName || '-'}
            </dd>
          </div>
          <div className='md:col-span-2'>
            <dt className='text-small text-foreground'>方法論番号</dt>
            <dd className='mt-1 break-words font-bold'>
              {project.methodNumber || '-'}
            </dd>
          </div>
          <div className='md:col-span-2'>
            <dt className='text-small text-foreground'>排出削減活動</dt>
            <dd className='mt-1 whitespace-pre-wrap break-words font-bold'>
              {project.methodMainEmission || '-'}
            </dd>
          </div>
        </dl>

        {isJoined && (
          <div className='mt-6 sm:ml-auto'>
            <Button
              type='button'
              variant='outline'
              size='md'
              disabled={
                !project.participationId || !onDownloadPdf || isDownloadingPdf
              }
              onClick={() => onDownloadPdf?.(project)}
            >
              {isDownloadingPdf ? (
                <LoaderCircle className='animate-spin' />
              ) : (
                <Download />
              )}
              PDFをダウンロード
            </Button>
          </div>
        )}

        {!isJoined && onJoin && (
          <div className='mt-6 sm:ml-auto sm:min-w-56'>
            <Button
              type='button'
              size='md'
              className='w-full'
              onClick={() => onJoin(project)}
            >
              入会届を提出
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
