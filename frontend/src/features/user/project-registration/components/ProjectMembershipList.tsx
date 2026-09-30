import type { SiteProject } from '@/api/site';
import { Card, CardContent } from '@/components/ui/card';
import { ProjectMembershipCard } from './ProjectMembershipCard';
import type { ProjectMembershipListItem } from '@/features/user/project-registration/projectMembershipStatus';

type ProjectMembershipListProps = Readonly<{
  projectItems: ProjectMembershipListItem[];
  emptyMessage: string;
  headingAs: 'h2' | 'h3';
  onJoin: (
    project: SiteProject,
  ) => void;
  onDownloadPdf: (
    project: SiteProject,
  ) => void;
  isDownloadingPdf: (
    project: SiteProject,
  ) => boolean;
}>;

/**
 * Renders the projects after sorting
 * and filtering have been applied.
 */
export function ProjectMembershipList({
  projectItems,
  emptyMessage,
  headingAs,
  onJoin,
  onDownloadPdf,
  isDownloadingPdf,
}: ProjectMembershipListProps) {
  const Heading = headingAs;

  return (
    <section
      aria-labelledby="public-projects-heading"
      data-membership-reveal
      className="border-t border-border pt-6"
    >
      <div className="mb-4 flex items-center justify-between gap-3">
        <Heading
          id="public-projects-heading"
          className="text-base font-semibold text-foreground"
        >
          公開中のプロジェクト
        </Heading>

        <span className="shrink-0 text-small tabular-nums text-muted-foreground">
          {projectItems.length}件
        </span>
      </div>

      {projectItems.length > 0 ? (
        <div className="space-y-4">
          {projectItems.map(
            ({
              project,
              status,
            }) => (
              <div
                key={project.id}
                data-membership-project
              >
                <ProjectMembershipCard
                  project={project}
                  status={status}
                  onJoin={onJoin}
                  onDownloadPdf={
                    onDownloadPdf
                  }
                  isDownloadingPdf={isDownloadingPdf(
                    project,
                  )}
                />
              </div>
            ),
          )}
        </div>
      ) : (
        <Card className="rounded-lg border-dashed border-border shadow-none">
          <CardContent className="p-8 text-center text-foreground">
            {emptyMessage}
          </CardContent>
        </Card>
      )}
    </section>
  );
}