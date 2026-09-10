import { useCallback } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import {
  AlertTriangle,
  ArrowRight,
  ArrowUpRight,
  BarChart3,
  FileText,
  UsersRound,
} from 'lucide-react';
import type { AdminProjectDetail } from '@/api/admin/projects';
import type { MonitoringRecordListItem } from '@/api/monitoring';
import { ErrorBoundary } from '@/components/feedback/ErrorBoundary';
import { PageSkeleton, SkeletonCard } from '@/components/feedback/loading';
import { Skeleton } from '@/components/ui/skeleton';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { ProjectMonitoringTable } from '@/features/admin/monitoring/ProjectMonitoringTable';
import {
  useAdminProjectDetailQuery,
  useAdminProjectPlanProgressQuery,
} from '@/hooks/admin/useAdminProjectDetailQuery';
import { paths } from '@/routes/paths';
import { AdminProjectDetailLayout } from './AdminProjectDetailLayout';
import {
  formatProjectDetailValue,
  formatProjectEmission,
  toProjectDetailNumber,
} from './projectDetailDisplay';

const DEFAULT_MONITORING_PAGE = 1;
const DEFAULT_MONITORING_LIMIT = 10;

type ProjectNavigationCardProps = {
  title: string;
  description: string;
  icon: React.ReactNode;
  ariaLabel: string;
  onClick: () => void;
};

/** Reads a positive integer query parameter or returns its fallback. */
function getPositiveIntegerSearchParam(
  searchParams: URLSearchParams,
  key: string,
  fallback: number,
) {
  const parsedValue = Number(searchParams.get(key));
  return Number.isFinite(parsedValue) && parsedValue > 0
    ? parsedValue
    : fallback;
}

/** Builds the compact methodology code shown in the overview. */
function getMethodologyCode(project: AdminProjectDetail) {
  const methodologyCode = [
    project.methodologyCategory,
    project.methodologyNumber,
  ]
    .filter(Boolean)
    .join('-');
  return methodologyCode || '—';
}

/** Calculates a bounded comparison percentage without treating missing data as zero. */
function getEmissionComparisonPercentage(
  value: number | null,
  baseline: number | null,
) {
  if (value == null || baseline == null || baseline <= 0) return 0;
  return Math.min(100, Math.max(0, (value / baseline) * 100));
}

/** Renders the best-effort project-plan progress summary. */
function ProjectPlanProgressCard({
  completedForms,
  totalForms,
  isLoading,
  hasError,
}: Readonly<{
  completedForms: number;
  totalForms: number;
  isLoading: boolean;
  hasError: boolean;
}>) {
  const progressPercentage =
    totalForms > 0 ? (completedForms / totalForms) * 100 : 0;
  const remainingForms = Math.max(totalForms - completedForms, 0);

  return (
    <Card className="h-full rounded-xl border-border shadow-app-sm">
      <CardHeader>
        <CardTitle>計画書進捗</CardTitle>
        <CardDescription>プロジェクト計画書の入力状況です。</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {isLoading ? (
          <div
            role="status"
            aria-label="計画書の進捗を読み込んでいます"
            className="space-y-4 py-2"
          >
            <Skeleton className="h-9 w-40" />
            <Skeleton className="h-2 w-full" />
            <Skeleton className="h-4 w-24" />
          </div>
        ) : hasError ? (
          <p role="alert" className="py-6 text-small text-destructive">
            計画書の進捗を取得できませんでした。
          </p>
        ) : totalForms > 0 ? (
          <>
            <p className="flex items-baseline gap-2 text-muted-foreground">
              <strong className="text-3xl text-foreground">
                {completedForms} / {totalForms}
              </strong>
              <span className="text-small">入力済み</span>
            </p>
            <Progress
              value={progressPercentage}
              className="h-2"
              aria-label={`計画書進捗 ${completedForms}/${totalForms}`}
            />
            <p className="text-small text-muted-foreground">
              残り {remainingForms} 項目
            </p>
          </>
        ) : (
          <p className="py-6 text-small text-muted-foreground">
            計画書の進捗情報はありません。
          </p>
        )}
      </CardContent>
    </Card>
  );
}

/** Renders actual project emissions as a baseline comparison. */
function ProjectEmissionSummaryCard({
  project,
}: Readonly<{ project: AdminProjectDetail }>) {
  const baselineEmission = toProjectDetailNumber(
    project.baselineEmissions ?? project.baselineEmission,
  );
  const projectEmission = toProjectDetailNumber(
    project.postProjectEmissions ?? project.postProjectEmission,
  );
  const reduction = toProjectDetailNumber(project.emissionReduction);

  return (
    <Card className="h-full rounded-xl border-border shadow-app-sm">
      <CardHeader className="gap-2">
        <div className="flex flex-wrap items-baseline justify-between gap-2">
          <CardTitle>排出量サマリ</CardTitle>
          <p className="text-small font-bold text-primary-strong">
            削減量 {formatProjectEmission(reduction)}
          </p>
        </div>
        <CardDescription>
          ベースラインとプロジェクト排出量を比較します。
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-5">
        <div className="space-y-2">
          <div className="flex items-center justify-between gap-3 text-small">
            <span className="text-muted-foreground">ベースライン</span>
            <span className="font-medium text-foreground">
              {formatProjectEmission(baselineEmission)}
            </span>
          </div>
          <Progress
            value={baselineEmission != null && baselineEmission > 0 ? 100 : 0}
            className="h-2"
            aria-label={`ベースライン ${formatProjectEmission(baselineEmission)}`}
          />
        </div>
        <div className="space-y-2">
          <div className="flex items-center justify-between gap-3 text-small">
            <span className="text-muted-foreground">プロジェクト排出量</span>
            <span className="font-medium text-foreground">
              {formatProjectEmission(projectEmission)}
            </span>
          </div>
          <Progress
            value={getEmissionComparisonPercentage(
              projectEmission,
              baselineEmission,
            )}
            className="h-2"
            aria-label={`プロジェクト排出量 ${formatProjectEmission(projectEmission)}`}
          />
        </div>
      </CardContent>
    </Card>
  );
}

/** Renders the project attributes most useful for quick scanning. */
function ProjectOverviewCard({
  project,
  onOpenInformation,
}: Readonly<{
  project: AdminProjectDetail;
  onOpenInformation: () => void;
}>) {
  return (
    <Card className="h-full rounded-xl border-border shadow-app-sm">
      <CardHeader>
        <CardTitle>プロジェクト概要</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <dl className="space-y-4">
          <div className="flex items-center justify-between gap-4">
            <dt className="text-small text-muted-foreground">ステータス</dt>
            <dd className="text-small font-bold text-foreground">
              {formatProjectDetailValue(project.projectStatus)}
            </dd>
          </div>
          <div className="flex items-center justify-between gap-4">
            <dt className="text-small text-muted-foreground">方法論</dt>
            <dd className="text-small font-bold text-foreground">
              {getMethodologyCode(project)}
            </dd>
          </div>
          <div className="flex items-center justify-between gap-4">
            <dt className="text-small text-muted-foreground">地域区分</dt>
            <dd className="text-small font-bold text-foreground">
              {formatProjectDetailValue(project.regionalCategory)}
            </dd>
          </div>
        </dl>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={onOpenInformation}
          className="ml-auto flex px-1 text-primary-strong"
        >
          詳細情報を見る <ArrowRight aria-hidden="true" />
        </Button>
      </CardContent>
    </Card>
  );
}

/** Renders one non-clickable navigation card with a dedicated arrow action. */
function ProjectNavigationCard({
  title,
  description,
  icon,
  ariaLabel,
  onClick,
}: Readonly<ProjectNavigationCardProps>) {
  return (
    <Card className="rounded-xl border-border shadow-app-sm">
      <CardContent className="flex min-h-32 items-center gap-4 p-5 sm:p-6">
        <span
          className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-accent text-primary"
          aria-hidden="true"
        >
          {icon}
        </span>
        <div className="min-w-0 flex-1">
          <h3 className="text-lg font-bold text-foreground">{title}</h3>
          <p className="mt-1 text-small leading-5 text-muted-foreground">
            {description}
          </p>
        </div>
        <Button
          type="button"
          size="icon"
          onClick={onClick}
          aria-label={ariaLabel}
          className="shrink-0 rounded-full"
        >
          <ArrowUpRight aria-hidden="true" />
        </Button>
      </CardContent>
    </Card>
  );
}

/** Displays one admin project overview and its project-scoped monitoring records. */
export default function ProjectDetailAdmin() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const projectQuery = useAdminProjectDetailQuery(projectId);
  const progressQuery = useAdminProjectPlanProgressQuery(projectId);
  const project = projectQuery.data;
  const monitoringPage = getPositiveIntegerSearchParam(
    searchParams,
    'monitoringPage',
    DEFAULT_MONITORING_PAGE,
  );
  const monitoringLimit = getPositiveIntegerSearchParam(
    searchParams,
    'monitoringLimit',
    DEFAULT_MONITORING_LIMIT,
  );

  /** Opens a monitoring record while retaining the list pagination context. */
  const openMonitoringDetail = useCallback(
    (record: MonitoringRecordListItem, page: number, limit: number) => {
      if (!projectId) return;

      navigate(
        `${paths.adminProjectMonitoringDetail(
          projectId,
          record.id,
        )}?page=${page}&limit=${limit}`,
        { state: { monitoringRecord: record } },
      );
    },
    [navigate, projectId],
  );

  if (projectQuery.isLoading) {
    return (
      <PageSkeleton>
        <div className="mx-auto grid w-full max-w-admin gap-5 xl:grid-cols-3">
          {Array.from({ length: 6 }, (_, index) => (
            <SkeletonCard key={index} className="min-h-44" />
          ))}
        </div>
      </PageSkeleton>
    );
  }

  if (!projectId || projectQuery.isError || !project) {
    return (
      <div className="flex min-h-screen items-center justify-center p-6">
        <Card className="w-full max-w-md">
          <CardHeader>
            <CardTitle>プロジェクトが見つかりません</CardTitle>
            <CardDescription>
              {projectQuery.error instanceof Error
                ? projectQuery.error.message
                : 'プロジェクト情報を取得できませんでした。'}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <PageBackButton
              onClick={() => navigate(paths.adminProjectList)}
              aria-label="プロジェクト管理に戻る"
            />
          </CardContent>
        </Card>
      </div>
    );
  }

  const completedForms = progressQuery.data?.completedForms ?? 0;
  const totalForms = progressQuery.data?.totalForms ?? 0;

  return (
    <AdminProjectDetailLayout
      projectId={projectId}
      projectName={project.projectName}
      title="プロジェクト詳細"
      description="登録済みプロジェクトの進捗、排出量、関連データを確認します。"
      backTo={paths.adminProjectList}
      backLabel="プロジェクト管理に戻る"
    >
      <section aria-label="プロジェクト識別情報" className="space-y-1">
        <h2 className="break-words text-2xl font-bold text-foreground">
          {formatProjectDetailValue(project.projectName)}
        </h2>
        <p className="text-small text-muted-foreground">
          プロジェクト番号：{formatProjectDetailValue(project.projectNumber)}
        </p>
      </section>

      <section
        aria-label="プロジェクトサマリ"
        className="grid items-stretch gap-4 xl:grid-cols-3"
      >
        <ProjectPlanProgressCard
          completedForms={completedForms}
          totalForms={totalForms}
          isLoading={progressQuery.isLoading}
          hasError={progressQuery.isError}
        />
        <ProjectEmissionSummaryCard project={project} />
        <ProjectOverviewCard
          project={project}
          onOpenInformation={() =>
            navigate(paths.adminProjectInformation(projectId))
          }
        />
      </section>

      <section
        aria-label="プロジェクト関連ページ"
        className="grid gap-4 xl:grid-cols-3"
      >
        <ProjectNavigationCard
          title="プロジェクト計画書"
          description="計画書の入力内容・関連資料を確認します。"
          icon={<FileText className="h-5 w-5" />}
          ariaLabel="プロジェクト計画書を開く"
          onClick={() => navigate(paths.adminProjectPlan(projectId))}
        />
        <ProjectNavigationCard
          title="モニタリングデータ報告書"
          description="モニタリング結果の報告書を確認します。"
          icon={<BarChart3 className="h-5 w-5" />}
          ariaLabel="モニタリングデータ報告書を開く"
          onClick={() =>
            navigate(paths.adminProjectMonitoringReport(projectId))
          }
        />
        <ProjectNavigationCard
          title="入会届一覧"
          description="このプロジェクトへの入会申請と審査状況を確認します。"
          icon={<UsersRound className="h-5 w-5" />}
          ariaLabel="入会届一覧を開く"
          onClick={() => navigate(paths.adminProjectMembership(projectId))}
        />
      </section>

      <Card id="monitoring-data" className="scroll-mt-24 rounded-xl shadow-app-sm">
        <CardHeader>
          <CardTitle>モニタリングデータ一覧</CardTitle>
          <CardDescription>
            このプロジェクトに登録されたモニタリングデータを確認できます。
          </CardDescription>
        </CardHeader>
        <CardContent>
          <ErrorBoundary
            fallback={({ resetErrorBoundary }) => (
              <div
                role="alert"
                className="flex flex-col items-center gap-3 rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-center"
              >
                <AlertTriangle
                  className="h-5 w-5 text-destructive"
                  aria-hidden="true"
                />
                <p className="text-small font-medium text-foreground">
                  モニタリングデータを表示できませんでした。
                </p>
                <Button
                  type="button"
                  variant="outline"
                  onClick={resetErrorBoundary}
                >
                  再試行
                </Button>
              </div>
            )}
          >
            <ProjectMonitoringTable
              key={projectId}
              projectId={projectId}
              initialPage={monitoringPage}
              initialLimit={monitoringLimit}
              onViewDetail={openMonitoringDetail}
            />
          </ErrorBoundary>
        </CardContent>
      </Card>
    </AdminProjectDetailLayout>
  );
}
