import type { SiteProject } from '@/api/site';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { ProjectMembershipCard } from './ProjectMembershipCard';
import type {
  MembershipStatusFilter,
  ProjectMembershipListItem,
} from '@/features/user/project-registration/projectMembershipStatus';

type ProjectMembershipListProps = Readonly<{
  projectItems: ProjectMembershipListItem[];
  emptyMessage: string;
  headingAs: 'h2' | 'h3';
  activeFilter: MembershipStatusFilter;
  onShowAll: () => void;
  onJoin: (project: SiteProject) => void;
  onDownloadPdf: (project: SiteProject) => void;
  isDownloadingPdf: (project: SiteProject) => boolean;
}>;

/** Renders the projects after sorting and filtering have been applied. */
export function ProjectMembershipList({
  projectItems,
  emptyMessage,
  headingAs,
  activeFilter,
  onShowAll,
  onJoin,
  onDownloadPdf,
  isDownloadingPdf,
}: ProjectMembershipListProps) {
  const Heading = headingAs;

  return (
    <section aria-labelledby="public-projects-heading" data-membership-reveal>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <Heading id="public-projects-heading" className="text-base font-semibold text-foreground">
          公開中のプロジェクト
        </Heading>
        <div className="flex items-center gap-3">
          <span className="shrink-0 text-small text-muted-foreground">
            {projectItems.length}件
          </span>
          {activeFilter !== 'all' && (
            <Button type="button" variant="ghost" size="sm" onClick={onShowAll}>
              すべて表示
            </Button>
          )}
        </div>
      </div>

      {projectItems.length > 0 ? (
        <div className="space-y-4">
          {projectItems.map(({ project, status }) => (
            <div key={project.id} data-membership-project>
              <ProjectMembershipCard
                project={project}
                status={status}
                onJoin={onJoin}
                onDownloadPdf={onDownloadPdf}
                isDownloadingPdf={isDownloadingPdf(project)}
              />
            </div>
          ))}
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
