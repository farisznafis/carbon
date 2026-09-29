import type { SiteProject } from '@/api/site';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { cn } from '@/lib/utils';
import { ExternalLink } from 'lucide-react';
import { ButtonLoader } from '@/components/feedback/loading';

export type ProjectMembershipStatus = 'available' | 'pending' | 'joined';

type ProjectMembershipCardProps = Readonly<{
  project: SiteProject;
  status: ProjectMembershipStatus;
  onJoin?: (project: SiteProject) => void;
  onDownloadPdf?: (project: SiteProject) => void;
  isDownloadingPdf?: boolean;
}>;

// Builds the compact methodology reference shown on membership project cards.
function formatMethodologyReference(project: SiteProject): string {
  return [project.methodNumber, project.methodologyVersion]
    .filter(Boolean)
    .join(' / ') || '-';
}

export function ProjectMembershipCard({
  project,
  status,
  onJoin,
  onDownloadPdf,
  isDownloadingPdf = false,
}: ProjectMembershipCardProps) {
  const showsJoinedState = status === 'joined';
  const canApply = status === 'available';

  return (
    <Card
      className={cn(
        'h-full rounded-lg border-border shadow-none',
        showsJoinedState
          ? 'bg-muted'
          : status === 'pending'
            ? 'bg-muted/40'
            : 'bg-card',
      )}
    >
      <CardContent className='flex h-full flex-col p-6'>
        <div className='flex items-start justify-between gap-4'>
          <div className='min-w-0'>
            <h3 className='break-words text-foreground'>
              {project.projectName || '-'}
            </h3>
            {/* {project.regionalCategory && (
              <p className='text-small mt-2 flex items-center gap-1.5 text-muted-foreground'>
                <MapPin className='h-4 w-4 shrink-0' aria-hidden='true' />
                <span className='break-words'>{project.regionalCategory}</span>
              </p>
            )} */}
          </div>
          <Badge
            className={cn(
              'shrink-0 border-transparent',
              status === 'pending'
                ? 'bg-warning/15 text-warning hover:bg-warning/15'
                : 'bg-secondary text-secondary-foreground hover:bg-secondary',
            )}
          >
            {status === 'joined'
              ? '入会中'
              : status === 'pending'
                ? '申請中'
                : '未入会'}
          </Badge>
        </div>

        <dl className='mt-6 grid gap-x-8 gap-y-5 md:grid-cols-8'>
          <div className='md:col-span-3'>
            <dt className='text-small text-foreground'>方法論名称</dt>
            <dd className='mt-1 break-words font-bold'>
              {project.methodName || '-'}
            </dd>
          </div>
          <div className='md:col-span-2'>
            <dt className='text-small text-foreground'>
              方法論番号・バージョン
            </dt>
            <dd className='mt-1 break-words font-bold'>
              {formatMethodologyReference(project)}
            </dd>
          </div>
          <div className='md:col-span-3'>
            <dt className='text-small text-foreground'>排出削減活動</dt>
            <dd className='mt-1 whitespace-pre-wrap break-words font-bold'>
              {project.methodMainEmission || '-'}
            </dd>
          </div>
        </dl>

        {showsJoinedState && onDownloadPdf && (
          <div className='mt-6 sm:ml-auto'>
            <Button
              type='button'
              variant='outline'
              size='md'
              disabled={isDownloadingPdf}
              onClick={() => onDownloadPdf?.(project)}
            >
              {isDownloadingPdf ? (
                <ButtonLoader />
              ) : (
                <ExternalLink />
              )}
              PDFを表示
            </Button>
          </div>
        )}

        {canApply && onJoin && (
          <div className='mt-6 sm:ml-auto sm:min-w-56'>
            <Button
              type='button'
              size='md'
              className='w-full'
              onClick={() => onJoin(project)}
            >
              このプロジェクトに入会する
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
