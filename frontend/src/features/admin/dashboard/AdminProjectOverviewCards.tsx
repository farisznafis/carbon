import type { Project } from '@/api/projects';
import { SkeletonChart } from '@/components/feedback/loading';
import { Card, CardContent } from '@/components/ui/card';
import { ProjectReductionTrendCard } from './overview/ProjectReductionTrendCard';
import { ProjectStatusFlowCard } from './overview/ProjectStatusFlowCard';

type AdminProjectOverviewCardsProps = {
  projects: Project[];
  projectsLoading: boolean;
  projectsError: string | null;
};

/**
 * Renders the overview charts above the admin project list. Plan-entry
 * progress lives on each project's detail page and is highlighted in the list,
 * so it is intentionally not repeated here.
 */
export function AdminProjectOverviewCards({
  projects,
  projectsLoading,
  projectsError,
}: Readonly<AdminProjectOverviewCardsProps>) {
  if (projectsLoading) {
    return (
      <div className="grid gap-4 xl:grid-cols-2">
        <SkeletonChart />
        <SkeletonChart />
      </div>
    );
  }

  if (projectsError) {
    return (
      <Card className="rounded-lg border-border">
        <CardContent>
          <p role="alert" className="py-8 text-center text-small text-destructive-strong">
            {projectsError}
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <section
      aria-label="プロジェクト概要"
      className="grid items-stretch gap-4 xl:grid-cols-2"
    >
      <ProjectStatusFlowCard projects={projects} />
      <ProjectReductionTrendCard projects={projects} />
    </section>
  );
}
