import type { SiteProject } from '@/api/site';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { ButtonLoader } from '@/components/feedback/loading';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { cn } from '@/lib/utils';
import { ExternalLink } from 'lucide-react';
import type { ProjectMembershipStatus } from '@/features/user/project-registration/projectMembershipStatus';

export type { ProjectMembershipStatus } from '@/features/user/project-registration/projectMembershipStatus';

type ProjectMembershipCardProps = Readonly<{
  project: SiteProject;
  status: ProjectMembershipStatus;
  onJoin?: (project: SiteProject) => void;
  onDownloadPdf?: (project: SiteProject) => void;
  isDownloadingPdf?: boolean;
}>;

const STATUS_PRESENTATION = {
  available: {
    label: '未入会',
    tone: 'neutral',
    accentClassName: 'border-l-primary',
  },
  pending: {
    label: '申請中',
    tone: 'warning',
    accentClassName: 'border-l-warning',
  },
  joined: {
    label: '入会中',
    tone: 'success',
    accentClassName: 'border-l-success',
  },
} as const;

// Builds the compact methodology reference shown on membership project cards.
function formatMethodologyReference(
  project: SiteProject,
): string {
  return (
    [
      project.methodNumber,
      project.methodologyVersion,
    ]
      .filter(Boolean)
      .join(' / ') || '-'
  );
}

/** Renders the action available for the current membership status. */
function ProjectMembershipCardAction({
  project,
  status,
  onJoin,
  onDownloadPdf,
  isDownloadingPdf = false,
}: ProjectMembershipCardProps) {
  if (status === 'available' && onJoin) {
    return (
      <Button
        type="button"
        size="md"
        className="h-auto min-h-10 w-fit max-w-full whitespace-normal text-center"
        onClick={() => onJoin(project)}
      >
        入会する
      </Button>
    );
  }

  if (status === 'joined' && onDownloadPdf) {
    return (
      <Button
        type="button"
        variant="outline"
        size="md"
        className="h-auto min-h-10 w-fit max-w-full whitespace-normal text-center"
        disabled={isDownloadingPdf}
        onClick={() => onDownloadPdf(project)}
      >
        {isDownloadingPdf ? <ButtonLoader /> : <ExternalLink />}
        PDFを表示
      </Button>
    );
  }

  return (
    <span className="text-small font-medium text-muted-foreground">
      {status === 'pending' ? '申請内容を確認中です' : status === 'joined' ? '入会中' : '未入会'}
    </span>
  );
}

/** Presents a project and keeps its status-specific action in a shared card layout. */
export function ProjectMembershipCard({
  project,
  status,
  onJoin,
  onDownloadPdf,
  isDownloadingPdf = false,
}: ProjectMembershipCardProps) {
  const showsJoinedState =
    status === 'joined';

  const statusPresentation =
    STATUS_PRESENTATION[status];

  return (
    <Card
      className={cn(
        'overflow-hidden rounded-lg border-l-4 border-border shadow-app-sm',
        statusPresentation.accentClassName,
        showsJoinedState
          ? 'bg-muted'
          : status === 'pending'
            ? 'bg-muted/40'
            : 'bg-card',
      )}
    >
      <CardContent className="grid min-w-0 p-0 xl:grid-cols-[minmax(0,1fr)_15rem]">
        <div className="min-w-0 px-5 py-5 sm:px-6">
          <div className="flex min-w-0 flex-wrap items-center gap-3">
            <h3 className="min-w-0 break-words font-semibold leading-snug text-foreground">
              {project.projectName || '-'}
            </h3>

            <StatusBadge
              tone={statusPresentation.tone}
              className="shrink-0 font-semibold"
            >
              {statusPresentation.label}
            </StatusBadge>
          </div>

          <dl className="mt-4 grid min-w-0 gap-x-6 gap-y-3 sm:grid-cols-2 lg:grid-cols-8">
            <div className="min-w-0 lg:col-span-3">
              <dt className="text-small text-muted-foreground">
                方法論名称
              </dt>
              <dd className="mt-1 break-words font-semibold text-foreground">
                {project.methodName || '-'}
              </dd>
            </div>

            <div className="min-w-0 lg:col-span-2">
              <dt className="text-small text-muted-foreground">
                方法論番号・バージョン
              </dt>
              <dd className="mt-1 break-words font-semibold text-foreground">
                {formatMethodologyReference(project)}
              </dd>
            </div>

            <div className="min-w-0 sm:col-span-2 lg:col-span-3">
              <dt className="text-small text-muted-foreground">
                排出削減活動
              </dt>
              <dd className="mt-1 whitespace-pre-wrap break-words font-semibold text-foreground">
                {project.methodMainEmission || '-'}
              </dd>
            </div>
          </dl>
        </div>

        <div className="flex min-h-16 min-w-0 items-center justify-end border-t border-border px-5 py-3 sm:px-6 xl:border-t-0 xl:px-5">
          <ProjectMembershipCardAction
            project={project}
            status={status}
            onJoin={onJoin}
            onDownloadPdf={onDownloadPdf}
            isDownloadingPdf={isDownloadingPdf}
          />
        </div>
      </CardContent>
    </Card>
  );
}
