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

/** Presents a project and keeps its status-specific action in a shared card layout. */
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
      <CardContent className='flex h-full min-w-0 flex-col p-5'>
        <div className='flex min-w-0 items-start justify-between gap-4'>
          <div className='min-w-0 flex-1'>
            <h3 className='break-words font-semibold leading-snug text-foreground'>
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

        <dl className='mt-4 grid min-w-0 gap-x-6 gap-y-3 md:grid-cols-8'>
          <div className='min-w-0 md:col-span-3'>
            <dt className='text-small text-muted-foreground'>方法論名称</dt>
            <dd className='mt-1 break-words font-semibold text-foreground'>
              {project.methodName || '-'}
            </dd>
          </div>
          <div className='min-w-0 md:col-span-2'>
            <dt className='text-small text-muted-foreground'>
              方法論番号・バージョン
            </dt>
            <dd className='mt-1 break-words font-semibold text-foreground'>
              {formatMethodologyReference(project)}
            </dd>
          </div>
          <div className='min-w-0 md:col-span-3'>
            <dt className='text-small text-muted-foreground'>排出削減活動</dt>
            <dd className='mt-1 whitespace-pre-wrap break-words font-semibold text-foreground'>
              {project.methodMainEmission || '-'}
            </dd>
          </div>
        </dl>

        <div className='mt-4 border-t border-border pt-3'>
          <div className='flex min-h-10 items-center justify-end'>
            {canApply && onJoin ? (
              <Button
                type='button'
                size='md'
                className='w-full sm:w-auto sm:min-w-56'
                onClick={() => onJoin(project)}
              >
                このプロジェクトに入会する
              </Button>
            ) : showsJoinedState && onDownloadPdf ? (
              <Button
                type='button'
                variant='outline'
                size='md'
                className='w-full sm:w-auto'
                disabled={isDownloadingPdf}
                onClick={() => onDownloadPdf(project)}
              >
                {isDownloadingPdf ? <ButtonLoader /> : <ExternalLink />}
                PDFを表示
              </Button>
            ) : (
              <span className='text-small font-medium text-muted-foreground'>
                {status === 'pending'
                  ? '申請内容を確認中です'
                  : showsJoinedState
                    ? '入会中'
                    : '未入会'}
              </span>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
