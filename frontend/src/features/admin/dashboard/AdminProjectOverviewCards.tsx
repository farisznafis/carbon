import { ArrowDown } from 'lucide-react';
import type { ProjectPlanProgressSummary } from '@/api/admin/projects';
import type { Project } from '@/api/projects';
import { SkeletonList } from '@/components/feedback/loading';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { cn } from '@/lib/utils';
import {
  formatProjectDateTime,
  normalizeProjectStatus,
  PROJECT_STATUS_PRESENTATIONS,
} from './projectStatusPresentation';

const REDUCTION_SEGMENT_COUNT = 10;
const IN_PROGRESS_PREVIEW_LIMIT = 2;

type AdminProjectOverviewCardsProps = {
  projects: Project[];
  projectsLoading: boolean;
  projectsError: string | null;
  planProgress: ReadonlyMap<string, ProjectPlanProgressSummary>;
  planProgressLoading: boolean;
  planProgressError: string | null;
  onOpenPlan: (project: Project, planSlug?: string) => void;
  onShowAllInProgress: () => void;
};

/** Clamps project-plan completion into a valid progress percentage. */
function getProgressPercentage(progress: ProjectPlanProgressSummary) {
  if (progress.totalForms <= 0) return 0;
  return Math.min(
    100,
    Math.max(0, (progress.completedForms / progress.totalForms) * 100),
  );
}

/** Resolves the most useful available timestamp for one project. */
function getProjectTimestamp(project: Project) {
  return project.updatedAt ?? project.createdAt ?? null;
}

/** Renders a shared card-level API failure without blocking adjacent cards. */
function CardError({ message }: Readonly<{ message: string }>) {
  return (
    <p role="alert" className="py-8 text-center text-small text-destructive">
      {message}
    </p>
  );
}

/** Renders project counts using the canonical project status order. */
function ProjectStatusSummary({ projects }: Readonly<{ projects: Project[] }>) {
  const statusCounts = new Map<string, number>();
  projects.forEach((project) => {
    const status = normalizeProjectStatus(project.projectStatus) || 'その他';
    statusCounts.set(status, (statusCounts.get(status) ?? 0) + 1);
  });

  const knownStatuses = new Set<string>(
    PROJECT_STATUS_PRESENTATIONS.map((presentation) => presentation.status),
  );
  const otherCount = [...statusCounts.entries()].reduce(
    (total, [status, count]) => total + (knownStatuses.has(status) ? 0 : count),
    0,
  );

  return (
    <Card className="h-full rounded-xl border-border shadow-app-sm">
      <CardHeader className="flex-row items-center justify-between space-y-0">
        <CardTitle>プロジェクト状況</CardTitle>
        <Badge variant="secondary">
          {projects.length.toLocaleString('ja-JP')}件
        </Badge>
      </CardHeader>
      <CardContent>
        <dl className="grid grid-cols-2 gap-x-6 gap-y-3">
          {PROJECT_STATUS_PRESENTATIONS.map((presentation) => (
            <div
              key={presentation.status}
              className="flex min-w-0 items-center justify-between gap-3"
            >
              <dt className="flex min-w-0 items-center gap-2 text-small text-foreground">
                <span
                  className={cn(
                    'h-2 w-2 shrink-0 rounded-full',
                    presentation.dotClassName,
                  )}
                  aria-hidden="true"
                />
                <span className="truncate">{presentation.status}</span>
              </dt>
              <dd className="font-bold text-foreground">
                {statusCounts.get(presentation.status) ?? 0}
              </dd>
            </div>
          ))}
          {otherCount > 0 && (
            <div className="flex items-center justify-between gap-3">
              <dt className="flex items-center gap-2 text-small text-foreground">
                <span
                  className="h-2 w-2 rounded-full bg-muted-foreground"
                  aria-hidden="true"
                />
                その他
              </dt>
              <dd className="font-bold text-foreground">{otherCount}</dd>
            </div>
          )}
        </dl>
      </CardContent>
    </Card>
  );
}

/** Renders the five largest actual project reduction values as segmented bars. */
function ProjectReductionSummary({ projects }: Readonly<{ projects: Project[] }>) {
  const reductionProjects = projects
    .filter((project) => project.emissionReduction !== undefined)
    .sort(
      (left, right) =>
        (right.emissionReduction ?? 0) - (left.emissionReduction ?? 0),
    )
    .slice(0, 5);
  const maximumReduction = Math.max(
    ...reductionProjects.map((project) => project.emissionReduction ?? 0),
    0,
  );
  const numberFormatter = new Intl.NumberFormat('ja-JP', {
    maximumFractionDigits: 1,
  });

  return (
    <Card className="h-full rounded-xl border-border shadow-app-sm">
      <CardHeader>
        <CardTitle>プロジェクト別累計削減量</CardTitle>
      </CardHeader>
      <CardContent>
        {reductionProjects.length === 0 ? (
          <p className="py-8 text-center text-small text-muted-foreground">
            削減量データはまだありません。
          </p>
        ) : (
          <ul className="space-y-4">
            {reductionProjects.map((project) => {
              const reduction = project.emissionReduction ?? 0;
              const filledSegments =
                reduction > 0 && maximumReduction > 0
                  ? Math.max(
                      1,
                      Math.ceil(
                        (reduction / maximumReduction) *
                          REDUCTION_SEGMENT_COUNT,
                      ),
                    )
                  : 0;

              return (
                <li
                  key={project.id}
                  className="grid grid-cols-[minmax(6rem,.85fr)_minmax(7rem,1.25fr)_auto] items-center gap-3"
                >
                  <span
                    className="truncate text-small font-medium text-foreground"
                    title={project.projectName}
                  >
                    {project.projectName}
                  </span>
                  <span className="flex h-5 overflow-hidden" aria-hidden="true">
                    {Array.from(
                      { length: filledSegments },
                      (_, segmentIndex) => (
                        <span
                          key={segmentIndex}
                          className="w-4 border-r border-primary-foreground/70 bg-primary last:border-r-0"
                        />
                      ),
                    )}
                  </span>
                  <span className="whitespace-nowrap text-small text-muted-foreground">
                    <strong className="text-foreground">
                      {numberFormatter.format(reduction)}
                    </strong>{' '}
                    t-CO₂
                  </span>
                </li>
              );
            })}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}

/** Renders recent incomplete project plans and their resume actions. */
function InProgressProjectSummary({
  projects,
  planProgress,
  planProgressLoading,
  planProgressError,
  onOpenPlan,
  onShowAll,
}: Readonly<{
  projects: Project[];
  planProgress: ReadonlyMap<string, ProjectPlanProgressSummary>;
  planProgressLoading: boolean;
  planProgressError: string | null;
  onOpenPlan: (project: Project, planSlug?: string) => void;
  onShowAll: () => void;
}>) {
  const incompleteProjects = projects
    .map((project) => ({
      project,
      progress: planProgress.get(String(project.id)),
    }))
    .filter(
      (
        entry,
      ): entry is {
        project: Project;
        progress: ProjectPlanProgressSummary;
      } =>
        Boolean(
          entry.progress &&
            !entry.progress.complete &&
            entry.progress.totalForms > 0,
        ),
    )
    .sort((left, right) => {
      const rightTime = new Date(
        getProjectTimestamp(right.project) ?? '',
      ).getTime();
      const leftTime = new Date(
        getProjectTimestamp(left.project) ?? '',
      ).getTime();
      return (
        (Number.isNaN(rightTime) ? 0 : rightTime) -
        (Number.isNaN(leftTime) ? 0 : leftTime)
      );
    });

  return (
    <Card className="h-full rounded-xl border-border shadow-app-sm">
      <CardHeader>
        <CardTitle>作成中のプロジェクト</CardTitle>
      </CardHeader>
      <CardContent>
        {planProgressLoading ? (
          <SkeletonList items={IN_PROGRESS_PREVIEW_LIMIT} />
        ) : planProgressError ? (
          <CardError message={planProgressError} />
        ) : incompleteProjects.length === 0 ? (
          <p className="py-8 text-center text-small text-muted-foreground">
            作成中のプロジェクトはありません。
          </p>
        ) : (
          <>
            <ul className="divide-y divide-border">
              {incompleteProjects
                .slice(0, IN_PROGRESS_PREVIEW_LIMIT)
                .map(({ project, progress }) => (
                  <li
                    key={project.id}
                    className="space-y-3 py-4 first:pt-0"
                  >
                    <div className="flex items-center justify-between gap-3">
                      <div className="min-w-0">
                        <p className="truncate text-body font-bold text-foreground">
                          {project.projectName}
                        </p>
                        <p className="mt-1 text-subtle text-muted-foreground">
                          入力済み {progress.completedForms} / {progress.totalForms} ページ
                        </p>
                      </div>
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() =>
                          onOpenPlan(
                            project,
                            progress.nextIncompleteSlug ?? undefined,
                          )
                        }
                      >
                        続きから
                      </Button>
                    </div>
                    <Progress
                      value={getProgressPercentage(progress)}
                      className="h-2"
                      aria-label={`${project.projectName}の作成進捗`}
                    />
                    <p className="text-subtle text-muted-foreground">
                      最終更新：{formatProjectDateTime(getProjectTimestamp(project))}
                    </p>
                  </li>
                ))}
            </ul>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={onShowAll}
              className="mt-3 ml-auto flex text-primary-strong"
            >
              すべての作成中プロジェクトを見る <ArrowDown />
            </Button>
          </>
        )}
      </CardContent>
    </Card>
  );
}

/** Renders the three data-driven overview cards above the admin project list. */
export function AdminProjectOverviewCards({
  projects,
  projectsLoading,
  projectsError,
  planProgress,
  planProgressLoading,
  planProgressError,
  onOpenPlan,
  onShowAllInProgress,
}: Readonly<AdminProjectOverviewCardsProps>) {
  if (projectsLoading) {
    return (
      <div className="grid gap-4 xl:grid-cols-3">
        <SkeletonList items={3} />
        <SkeletonList items={3} />
        <SkeletonList items={3} />
      </div>
    );
  }

  if (projectsError) {
    return (
      <Card className="rounded-xl border-border shadow-app-sm">
        <CardContent>
          <CardError message={projectsError} />
        </CardContent>
      </Card>
    );
  }

  return (
    <section
      aria-label="プロジェクト概要"
      className="grid items-stretch gap-4 xl:grid-cols-3"
    >
      <ProjectStatusSummary projects={projects} />
      <ProjectReductionSummary projects={projects} />
      <InProgressProjectSummary
        projects={projects}
        planProgress={planProgress}
        planProgressLoading={planProgressLoading}
        planProgressError={planProgressError}
        onOpenPlan={onOpenPlan}
        onShowAll={onShowAllInProgress}
      />
    </section>
  );
}
