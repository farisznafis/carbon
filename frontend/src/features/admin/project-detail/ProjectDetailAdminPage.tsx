import { useCallback } from 'react';
import {
  Link,
  useLocation,
  useNavigate,
  useParams,
  useSearchParams,
} from 'react-router-dom';
import {
  AlertTriangle,
  ArrowUpRight,
  BarChart3,
  FileText,
} from 'lucide-react';
import type { AdminProjectDetail } from '@/api/admin/projects';
import { isForbiddenApiError } from '@/api/_shared/errors';
import type { MonitoringRecordListItem } from '@/api/monitoring';
import { ActionButton } from '@/components/actions';
import { ErrorBoundary } from '@/components/feedback/ErrorBoundary';
import { PageSkeleton, SkeletonCard } from '@/components/feedback/loading';
import {
  PageTabsList,
  PageTabsTrigger,
  TabCount,
} from '@/components/navigation/PageTabs';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent } from '@/components/ui/tabs';
import type { ParticipationReviewStatus } from '@/api/admin';
import { NoticeBanner } from '@/components/feedback/NoticeBanner';
import { MembershipStatusChips } from '@/features/admin/membership-review/MembershipStatusChips';
import { ParticipationReviewListPanel } from '@/features/admin/membership-review/ParticipationReviewListPanel';
import { useProjectParticipationReviews } from '@/hooks/admin/useProjectParticipationReviews';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { ProjectMonitoringTable } from '@/features/admin/monitoring/ProjectMonitoringTable';
import {
  DOCUMENT_UPLOAD_QUERY_PARAM,
  ProjectDocumentsTab,
} from '@/features/admin/project-files/ProjectDocumentsTab';
import { ProjectDocumentsSummaryCard } from '@/features/admin/project-files/ProjectDocumentsSummaryCard';
import { useAdminPermissions } from '@/features/admin/permissions/useAdminPermissions';
import { PermissionDeniedNotice } from '@/features/admin/permissions/PermissionDeniedNotice';
import { resolveAdminProjectNavigationState } from '@/features/admin/navigation/adminProjectNavigation';
import { useAdminProjectDetailQuery } from '@/hooks/admin/useAdminProjectDetailQuery';
import { paths } from '@/routes/paths';
import { AdminProjectDetailLayout } from './AdminProjectDetailLayout';
import { ProjectCompaniesPanel } from './components/ProjectCompaniesPanel';
import { ProjectEmissionTrendCard } from './components/ProjectEmissionTrendCard';
import { formatProjectDetailValue } from './projectDetailDisplay';

const DEFAULT_MONITORING_PAGE = 1;
const DEFAULT_MONITORING_LIMIT = 10;

const PROJECT_DETAIL_TABS = [
  { value: 'overview', label: '概要' },
  { value: 'monitoring', label: 'モニタリングデータ' },
  { value: 'membership', label: '入会届' },
  { value: 'companies', label: '参加企業' },
  { value: 'documents', label: '書類' },
] as const;

type ProjectDetailTab = (typeof PROJECT_DETAIL_TABS)[number]['value'];

/** Reads the active tab from the URL so back navigation and shared links reopen the same tab. */
function resolveProjectDetailTab(value: string | null): ProjectDetailTab {
  return PROJECT_DETAIL_TABS.some((tab) => tab.value === value)
    ? (value as ProjectDetailTab)
    : 'overview';
}

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

/** Renders the project attributes most useful for quick scanning. */
function ProjectOverviewCard({
  project,
  onOpenInformation,
  membershipCounts,
  onOpenMembership,
}: Readonly<{
  project: AdminProjectDetail;
  onOpenInformation: () => void;
  /** Omitted when the operator cannot review memberships; null while loading. */
  membershipCounts?: Record<ParticipationReviewStatus, number> | null;
  onOpenMembership: (status: ParticipationReviewStatus) => void;
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
          {membershipCounts !== undefined && (
            <div className="flex items-start justify-between gap-4">
              <dt className="pt-0.5 text-small text-muted-foreground">入会届</dt>
              <dd>
                <MembershipStatusChips
                  counts={membershipCounts}
                  onSelect={onOpenMembership}
                />
              </dd>
            </div>
          )}
        </dl>
        <ActionButton
          kind="link"
          onClick={onOpenInformation}
          className="ml-auto flex"
        >
          詳細情報を見る
        </ActionButton>
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

/** Wraps one list-style tab so every project detail tab shares the same card heading. */
function ProjectDetailTabCard({
  title,
  description,
  children,
}: Readonly<{ title: string; description: string; children: React.ReactNode }>) {
  return (
    <Card className="rounded-xl shadow-app-sm">
      <CardHeader>
        <CardTitle>{title}</CardTitle>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent>{children}</CardContent>
    </Card>
  );
}

/** Displays one admin project with its overview, monitoring, membership and participant tabs. */
export default function ProjectDetailAdmin() {
  const location = useLocation();
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = resolveProjectDetailTab(searchParams.get('tab'));
  const permissions = useAdminPermissions();
  const projectQuery = useAdminProjectDetailQuery(projectId);
  const project = projectQuery.data;
  const navigationState = resolveAdminProjectNavigationState(location.state);
  const projectDetailReturnTo =
    navigationState?.returnTo ?? paths.adminProjectList;
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

  /** Switches tabs in place, keeping the return destination carried in navigation state. */
  const canReviewMemberships = permissions.hasPermission(
    'PARTICIPATING_COMPANY_APPLICATION_REVIEW',
  );
  const membershipReviews = useProjectParticipationReviews(
    projectId,
    project,
    canReviewMemberships,
  );
  const pendingMembershipCount = membershipReviews.isReady
    ? membershipReviews.counts.OPEN
    : 0;

  /** Opens the 入会届 tab on one status, keeping the return destination. */
  const openMembership = (status: ParticipationReviewStatus) => {
    if (!projectId) return;
    navigate(paths.adminProjectMembership(projectId, status), {
      replace: true,
      state: location.state,
    });
  };

  const changeTab = (value: string) => {
    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.set('tab', resolveProjectDetailTab(value));
    setSearchParams(nextSearchParams, { replace: true, state: location.state });
  };

  /** From the 概要 書類 card: the 書類 tab with the upload dialog open for one type. */
  const openDocumentUpload = (fileType: string) => {
    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.set('tab', 'documents');
    nextSearchParams.set(DOCUMENT_UPLOAD_QUERY_PARAM, fileType);
    setSearchParams(nextSearchParams, { replace: true, state: location.state });
  };

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

  if (isForbiddenApiError(projectQuery.error)) {
    return (
      <main className="mx-auto w-full max-w-admin px-4 py-10 sm:px-6">
        <header className="mb-6">
          <h1 className="text-form-title font-bold text-foreground">プロジェクト詳細</h1>
          <p className="mt-2 text-muted-foreground">登録済みプロジェクトの情報を確認します。</p>
        </header>
        <PermissionDeniedNotice permission="PROJECT_READ" />
      </main>
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
            <Button asChild variant="outline">
              <Link to={projectDetailReturnTo}>
                {navigationState ? 'ホームに戻る' : 'プロジェクト管理に戻る'}
              </Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <AdminProjectDetailLayout
      projectId={projectId}
      projectName={project.projectName}
      fromHome={Boolean(navigationState)}
      title={formatProjectDetailValue(project.projectName)}
      description="登録済みプロジェクトの進捗、排出量、関連データを確認します。"
    >
      {/* <section aria-label="プロジェクト識別情報" className="space-y-1">
        <h2 className="break-words text-2xl font-bold text-foreground">
          {formatProjectDetailValue(project.projectName)}
        </h2>
        <p className="text-small text-muted-foreground">
          プロジェクト番号：{formatProjectDetailValue(project.projectNumber)}
        </p>
      </section> */}

      <Tabs value={activeTab} onValueChange={changeTab} className="min-w-0">
        <PageTabsList aria-label="プロジェクト詳細の表示">
          {PROJECT_DETAIL_TABS.map((tab) => {
            const badgeCount =
              tab.value === 'membership' ? pendingMembershipCount : 0;

            return (
              <PageTabsTrigger
                key={tab.value}
                value={tab.value}
                aria-label={
                  badgeCount > 0
                    ? `${tab.label}（承認待ち${badgeCount}件）`
                    : undefined
                }
                className="gap-2"
              >
                {tab.label}
                {badgeCount > 0 && <TabCount count={badgeCount} tone="warning" />}
              </PageTabsTrigger>
            );
          })}
        </PageTabsList>

        <TabsContent value="overview" className="m-0 space-y-6 pt-5">
          {pendingMembershipCount > 0 && (
            <NoticeBanner
              tone="warning"
              action={
                <ActionButton kind="review" onClick={() => openMembership('OPEN')}>
                  確認する
                </ActionButton>
              }
            >
              承認待ちの入会届が{pendingMembershipCount}件あります。
            </NoticeBanner>
          )}

          <section
            aria-label="プロジェクトサマリ"
            className="grid items-stretch gap-4 xl:grid-cols-3"
          >
            <ProjectEmissionTrendCard
              projectId={projectId}
              project={project}
              className="xl:col-span-2 xl:row-span-2"
            />
            <ProjectOverviewCard
              project={project}
              onOpenInformation={() =>
                navigate(paths.adminProjectInformation(projectId))
              }
              membershipCounts={
                canReviewMemberships
                  ? membershipReviews.isReady
                    ? membershipReviews.counts
                    : null
                  : undefined
              }
              onOpenMembership={openMembership}
            />
            <ProjectDocumentsSummaryCard
              projectId={projectId}
              onOpenDocuments={() => changeTab('documents')}
              onUpload={openDocumentUpload}
            />
          </section>
        </TabsContent>

        <TabsContent value="monitoring" className="m-0 pt-5">
          <ProjectDetailTabCard
            title="モニタリングデータ一覧"
            description="このプロジェクトに登録されたモニタリングデータを確認できます。"
          >
            {!permissions.hasPermission('MONITORING_READ') ? (
              <PermissionDeniedNotice permission="MONITORING_READ" />
            ) : (
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
                    <ActionButton kind="retry" onClick={resetErrorBoundary} />
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
            )}
          </ProjectDetailTabCard>
        </TabsContent>

        <TabsContent value="membership" className="m-0 pt-5">
          <ProjectDetailTabCard
            title="入会届一覧"
            description="このプロジェクトへの入会申請と審査状況を確認します。"
          >
            {!permissions.hasPermission('PARTICIPATING_COMPANY_APPLICATION_REVIEW') ? (
              <PermissionDeniedNotice permission="PARTICIPATING_COMPANY_APPLICATION_REVIEW" />
            ) : (
              <ParticipationReviewListPanel
                projectId={projectId}
                reviewsByStatus={membershipReviews.reviewsByStatus}
                isLoading={membershipReviews.isLoading}
                isError={membershipReviews.isError}
                isForbidden={membershipReviews.isForbidden}
                onRetry={() => void membershipReviews.refetch()}
              />
            )}
          </ProjectDetailTabCard>
        </TabsContent>

        <TabsContent value="companies" className="m-0 pt-5">
          <ProjectDetailTabCard
            title="参加企業一覧"
            description="このプロジェクトに参加している企業を確認します。"
          >
            {!permissions.hasPermission('PROJECT_READ') ? (
              <PermissionDeniedNotice permission="PROJECT_READ" />
            ) : (
              <ProjectCompaniesPanel projectId={projectId} />
            )}
          </ProjectDetailTabCard>
        </TabsContent>

        <TabsContent value="documents" className="m-0 pt-5">
          {/* No card heading: the page tab already says 書類, and the sub-tabs lead the card. */}
          <Card className="rounded-xl shadow-app-sm">
            <CardContent className="pt-[var(--card-padding)]">
              {!permissions.hasPermission('PROJECT_READ') ? (
                <PermissionDeniedNotice permission="PROJECT_READ" />
              ) : (
                <ProjectDocumentsTab projectId={projectId} />
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </AdminProjectDetailLayout>
  );
}
