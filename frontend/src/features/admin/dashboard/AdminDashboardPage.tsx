import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import {
  useLocation,
  useNavigate,
  useParams,
  useSearchParams,
} from 'react-router-dom';
import { ApiError } from '@/api/_shared/errors';
import type {
  AdminCompanyListItem,
  CompanyStatus,
  Pagination,
} from '@/api/admin';
import {
  approveAdminCompany,
  getAdminCompanies,
  rejectAdminCompany,
} from '@/api/admin';
import {
  getBatchHistory,
  type BatchHistoryItem,
  type BatchHistoryPagination,
} from '@/api/batch';
import {
  getMonitoringCancelRequests,
  type MonitoringRecordListItem,
} from '@/api/monitoring';
import {
  updateAdminProjectStatus,
  type ProjectStatus,
} from '@/api/admin/projects';
import type { Project } from '@/api/projects';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { ErrorBoundary } from '@/components/feedback/ErrorBoundary';
import {
  dashboardEmptyStateClassName,
  dashboardPageContainerClassName,
  dashboardPageHeaderClassName,
  dashboardTabsListClassName,
  dashboardTabTriggerClassName,
} from '@/components/layout/DashboardLayout';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useAuth } from '@/contexts/AuthContext';
import { CancelRequestHistoryTable } from '@/features/admin/dashboard/tables/CancelRequestHistoryTable';
import { BatchHistoryTable } from '@/features/admin/dashboard/tables/BatchHistoryTable';
import { AdminCompanyApplicationsTable } from '@/features/admin/dashboard/tables/AdminCompanyApplicationsTable';
import { PendingCompanyActionDialog } from '@/features/admin/dashboard/tables/PendingCompanyActionDialog';
import { PendingCompanyApplicationDetail } from '@/features/admin/dashboard/tables/PendingCompanyApplicationDetail';
import { ProjectTable } from '@/features/admin/dashboard/tables/ProjectTable';
import { ProjectMonitoringTable } from '@/features/admin/dashboard/tables/ProjectMonitoringTable';
import { useProjectsQuery } from '@/hooks/projects/useProjectsQuery';
import { useToast } from '@/hooks/use-toast';
import { usePagination } from '@/hooks/usePagination';
import { cn } from '@/lib/utils';
import { paths } from '@/routes/paths';
import { useNotificationStore } from '@/stores/notificationStore';
import { AlertTriangle, Plus } from 'lucide-react';

type AdminDashboardTabValue =
  | 'companies'
  | 'projects'
  | 'operators'
  | 'methodologies'
  | 'monitoring';

type PendingCompanyActionMode = 'approve' | 'reject';

const dashboardContainer = dashboardPageContainerClassName;

const APPLICATION_STATUSES: CompanyStatus[] = ['PENDING', 'REJECTED'];
const PARTICIPATING_STATUSES: CompanyStatus[] = ['ACTIVE', 'SUSPENDED'];

const DEFAULT_PAGINATION: Pagination = {
  page: 1,
  limit: 20,
  totalItems: 0,
  totalPages: 1,
  nextPageToken: null,
  hasMore: false,
};

const DEFAULT_BATCH_HISTORY_PAGINATION: BatchHistoryPagination = {
  page: 1,
  limit: 10,
  totalItems: 0,
  totalPages: 1,
  nextPageToken: null,
  hasMore: false,
};

const adminDashboardTabs: Array<{
  value: AdminDashboardTabValue;
  label: string;
}> = [
  { value: 'companies', label: '企業一覧・申請管理' },
  { value: 'projects', label: 'プロジェクト管理' },
  { value: 'operators', label: '運用者一覧' },
  { value: 'methodologies', label: '方法論一覧' },
  { value: 'monitoring', label: 'モニタリングデータ管理' },
];

function isStringRecord(value: unknown): value is Record<string, string> {
  return (
    Boolean(value) &&
    typeof value === 'object' &&
    !Array.isArray(value) &&
    Object.values(value as Record<string, unknown>).every(
      (entry) => typeof entry === 'string',
    )
  );
}

function getApiMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message || fallback;
  if (error instanceof Error) return error.message || fallback;
  return fallback;
}

function getApiFieldErrors(error: unknown) {
  if (error instanceof ApiError && isStringRecord(error.payload?.errors)) {
    return error.payload.errors;
  }

  return {};
}

function AdminDashboardHero() {
  return (
    <header className="border-b border-border bg-background">
      <div
        className={dashboardPageHeaderClassName}
      >
        <h1 className="text-foreground">
          運営者・管理者ダッシュボード
        </h1>
      </div>
    </header>
  );
}

function AdminDashboardTabs() {
  const triggerClassName = dashboardTabTriggerClassName;

  return (
    <div className="bg-card pt-6">
      <TabsList className={dashboardTabsListClassName}>
        {adminDashboardTabs.map((tab) => (
          <TabsTrigger
            key={tab.value}
            value={tab.value}
            className={triggerClassName}
          >
            {tab.label}
          </TabsTrigger>
        ))}
      </TabsList>
    </div>
  );
}

function AdminDashboardSection({
  title,
  description,
  action,
  children,
}: {
  title: string;
  description: string;
  action?: ReactNode;
  children: ReactNode;
}) {
  return (
    <section className="space-y-4">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div className="space-y-2">
          <h2 className="text-foreground">{title}</h2>
          <p className="text-subtle text-muted-foreground">{description}</p>
        </div>
        {action}
      </div>
      {children}
    </section>
  );
}

function AdminDashboardEmptyState({ children }: { children: ReactNode }) {
  return (
    <div className={dashboardEmptyStateClassName}>
      {children}
    </div>
  );
}

const AdminDashboard = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const { applicationId } = useParams();
  const { toast } = useToast();
  const { user } = useAuth();
  const projectsQuery = useProjectsQuery();
  const projects = projectsQuery.data ?? [];
  const loadNotifications = useNotificationStore(
    (state) => state.loadNotifications,
  );

  const {
    currentPage: projectPage,
    totalPages: projectTotalPages,
    totalItems: projectTotalItems,
    itemsPerPage: projectItemsPerPage,
    paginatedItems: paginatedProjects,
    goToPage: goToProjectPage,
    setItemsPerPage: setProjectItemsPerPage,
  } = usePagination(projects);

  const [companyListLoading, setCompanyListLoading] = useState(true);
  const [companyListError, setCompanyListError] = useState<string | null>(null);
  const [cancelRequestItems, setCancelRequestItems] = useState<
    Awaited<ReturnType<typeof getMonitoringCancelRequests>>
  >([]);
  const [cancelRequestLoading, setCancelRequestLoading] = useState(true);
  const [cancelRequestError, setCancelRequestError] = useState<string | null>(
    null,
  );
  const [batchHistoryItems, setBatchHistoryItems] = useState<BatchHistoryItem[]>(
    [],
  );
  const [batchHistoryPagination, setBatchHistoryPagination] =
    useState<BatchHistoryPagination>(DEFAULT_BATCH_HISTORY_PAGINATION);
  const [batchHistoryLoading, setBatchHistoryLoading] = useState(true);
  const [batchHistoryError, setBatchHistoryError] = useState<string | null>(
    null,
  );
  const [applicationCompanies, setApplicationCompanies] = useState<
    AdminCompanyListItem[]
  >([]);
  const [participatingCompanies, setParticipatingCompanies] = useState<
    AdminCompanyListItem[]
  >([]);
  const [applicationPagination, setApplicationPagination] =
    useState<Pagination>(DEFAULT_PAGINATION);
  const [participatingPagination, setParticipatingPagination] =
    useState<Pagination>(DEFAULT_PAGINATION);
  const [applicationPage, setApplicationPage] = useState(1);
  const [participatingPage, setParticipatingPage] = useState(1);
  const [companyLimit, setCompanyLimit] = useState(20);
  const [submitting, setSubmitting] = useState(false);
  const [actionFieldErrors, setActionFieldErrors] = useState<
    Record<string, string>
  >({});
  const [updatingProjectStatusId, setUpdatingProjectStatusId] = useState<
    string | null
  >(null);
  const [selectedMonitoringProjectId, setSelectedMonitoringProjectId] =
    useState(searchParams.get('projectId') ?? '');

  const [pendingDialogOpen, setPendingDialogOpen] = useState(false);
  const [pendingDialogMode, setPendingDialogMode] =
    useState<PendingCompanyActionMode | null>(null);
  const [selectedCompany, setSelectedCompany] =
    useState<AdminCompanyListItem | null>(null);
  const [detailCompany, setDetailCompany] =
    useState<AdminCompanyListItem | null>(
      (location.state as { company?: AdminCompanyListItem } | null)?.company ??
        null,
    );
  const [detailReviewMode, setDetailReviewMode] = useState(
    Boolean(
      (location.state as { reviewMode?: boolean } | null)?.reviewMode ??
        applicationId,
    ),
  );
  const [rejectReason, setRejectReason] = useState('');

  const requestedTab = searchParams.get('tab');
  const initialTab: AdminDashboardTabValue = adminDashboardTabs.some(
    (tab) => tab.value === requestedTab,
  )
    ? (requestedTab as AdminDashboardTabValue)
    : 'companies';

  const reloadCompanyLists = useCallback(async () => {
    setCompanyListLoading(true);
    setCompanyListError(null);

    try {
      const commonParams = {
        limit: companyLimit,
      };

      const [applications, participating] = await Promise.all([
        getAdminCompanies({
          ...commonParams,
          status: APPLICATION_STATUSES,
          page: applicationPage,
        }),
        getAdminCompanies({
          ...commonParams,
          status: PARTICIPATING_STATUSES,
          page: participatingPage,
        }),
      ]);

      setApplicationCompanies(applications.companies);
      setApplicationPagination(applications.pagination);
      setParticipatingCompanies(participating.companies);
      setParticipatingPagination(participating.pagination);
    } catch (error) {
      setCompanyListError(
        getApiMessage(error, '企業一覧の取得に失敗しました。'),
      );
    } finally {
      setCompanyListLoading(false);
    }
  }, [
    applicationPage,
    companyLimit,
    participatingPage,
  ]);

  useEffect(() => {
    void loadNotifications();
  }, [loadNotifications]);

  useEffect(() => {
    let isActive = true;

    const preloadCancelRequests = async () => {
      setCancelRequestLoading(true);
      setCancelRequestError(null);

      try {
        const response = await getMonitoringCancelRequests();
        if (!isActive) return;
        setCancelRequestItems(response);
      } catch (error) {
        if (!isActive) return;
        setCancelRequestError(
          getApiMessage(error, '取消一覧の取得に失敗しました。'),
        );
      } finally {
        if (isActive) {
          setCancelRequestLoading(false);
        }
      }
    };

    void preloadCancelRequests();

    return () => {
      isActive = false;
    };
  }, []);

  useEffect(() => {
    let isActive = true;

    const preloadBatchHistory = async () => {
      setBatchHistoryLoading(true);
      setBatchHistoryError(null);

      try {
        const response = await getBatchHistory(
          DEFAULT_BATCH_HISTORY_PAGINATION.page,
          DEFAULT_BATCH_HISTORY_PAGINATION.limit,
        );
        if (!isActive) return;
        setBatchHistoryItems(response.items);
        setBatchHistoryPagination(response.pagination);
      } catch (error) {
        if (!isActive) return;
        setBatchHistoryError(
          getApiMessage(error, 'バッチ履歴の取得に失敗しました。'),
        );
      } finally {
        if (isActive) setBatchHistoryLoading(false);
      }
    };

    void preloadBatchHistory();

    return () => {
      isActive = false;
    };
  }, []);

  useEffect(() => {
    void reloadCompanyLists();
  }, [reloadCompanyLists]);

  const resetCompanyPages = () => {
    setApplicationPage(1);
    setParticipatingPage(1);
  };

  const handleCompanyLimitChange = (value: number) => {
    setCompanyLimit(value);
    resetCompanyPages();
  };

  const handleEditProject = (project: Project) => {
    navigate(paths.adminProjectDetail(project.id));
  };

  const handleUpdateProject = (project: Project) => {
    navigate(paths.adminProjectEdit(project.id), { state: { project } });
  };

  const handleChangeProjectStatus = async (
    project: Project,
    status: ProjectStatus,
  ) => {
    if (project.projectStatus === status || updatingProjectStatusId) return;

    setUpdatingProjectStatusId(project.id);
    try {
      const companyId =
        project.companyId ?? project.operatorCompanyId ?? user?.companyId ?? '';
      const projectStatusPayload = {
        id: project.id,
        processedBy: user?.id ? String(user.id) : 'admin',
        companyId: String(companyId),
        projectStatus: status,
      };

      await updateAdminProjectStatus(project.id, projectStatusPayload);
      await projectsQuery.refetch();
      toast({
        title: 'ステータスを更新しました',
        description: `${project.projectName}: ${status}`,
        variant: 'success',
      });
    } catch (error) {
      toast({
        title: 'ステータス更新に失敗しました',
        description: getApiMessage(
          error,
          'プロジェクトステータスを更新できませんでした。',
        ),
        variant: 'destructive',
      });
    } finally {
      setUpdatingProjectStatusId(null);
    }
  };

  const routeApplicationCompany = useMemo(() => {
    if (!applicationId) return null;
    return (
      applicationCompanies.find((company) => company.id === applicationId) ??
      null
    );
  }, [applicationCompanies, applicationId]);

  useEffect(() => {
    if (!applicationId || detailCompany) return;
    if (routeApplicationCompany) {
      setDetailCompany(routeApplicationCompany);
      setDetailReviewMode(true);
    }
  }, [applicationId, detailCompany, routeApplicationCompany]);

  const openCompanyDetails = (item: AdminCompanyListItem) => {
    if (item.status === 'ACTIVE' || item.status === 'SUSPENDED') {
      navigate(paths.adminCompanyDetail(item.companyId));
      return;
    }

    if (item.id) {
      navigate(paths.adminApplicationDetail(item.id), {
        state: { company: item, reviewMode: false },
      });
    }
    setDetailCompany(item);
    setDetailReviewMode(false);
  };

  const openCompanyReview = (item: AdminCompanyListItem) => {
    if (item.id) {
      navigate(paths.adminApplicationDetail(item.id), {
        state: { company: item, reviewMode: true },
      });
    }
    setDetailCompany(item);
    setDetailReviewMode(true);
  };

  const closeCompanyDetail = () => {
    setDetailCompany(null);
    setDetailReviewMode(false);
    if (applicationId) {
      navigate(paths.adminDashboard);
    }
  };

  const openApproveCompany = (item: AdminCompanyListItem) => {
    setSelectedCompany(item);
    setPendingDialogMode('approve');
    setRejectReason('');
    setActionFieldErrors({});
    setPendingDialogOpen(true);
  };

  const openRejectCompany = (item: AdminCompanyListItem) => {
    setSelectedCompany(item);
    setPendingDialogMode('reject');
    setRejectReason('');
    setActionFieldErrors({});
    setPendingDialogOpen(true);
  };

  const openCancelRequestDetail = (
    request: Awaited<ReturnType<typeof getMonitoringCancelRequests>>[number],
  ) => {
    navigate(paths.adminCancelRequestDetail(request.id), {
      state: { cancelRequest: request },
    });
  };

  const handleMonitoringProjectChange = (projectId: string) => {
    setSelectedMonitoringProjectId(projectId);
    setSearchParams(
      {
        tab: 'monitoring',
        projectId,
      },
      { replace: true },
    );
  };

  const openMonitoringDetail = (
    record: MonitoringRecordListItem,
    page: number,
    limit: number,
  ) => {
    if (!selectedMonitoringProjectId) return;

    navigate(
      `${paths.adminProjectMonitoringDetail(
        selectedMonitoringProjectId,
        record.id,
      )}?page=${page}&limit=${limit}`,
      { state: { monitoringRecord: record } },
    );
  };

  const handlePendingDialogOpenChange = (open: boolean) => {
    setPendingDialogOpen(open);

    if (!open) {
      setSelectedCompany(null);
      setPendingDialogMode(null);
      setRejectReason('');
      setActionFieldErrors({});
    }
  };

  const handleConfirmPendingCompany = async () => {
    if (!selectedCompany || !pendingDialogMode) return;

    setSubmitting(true);
    setActionFieldErrors({});
    try {
      if (pendingDialogMode === 'approve') {
        await approveAdminCompany(selectedCompany.companyId);
      } else {
        await rejectAdminCompany(selectedCompany.companyId, rejectReason);
      }
      await reloadCompanyLists();
      handlePendingDialogOpenChange(false);
      closeCompanyDetail();
      toast({
        title:
          pendingDialogMode === 'approve'
            ? '申請を承認しました'
            : '申請を却下しました',
        variant: pendingDialogMode === 'approve' ? 'success' : 'warning',
      });
    } catch (error) {
      setActionFieldErrors(getApiFieldErrors(error));
      setCompanyListError(
        getApiMessage(
          error,
          pendingDialogMode === 'approve'
            ? '承認に失敗しました。'
            : '却下に失敗しました。',
        ),
      );
    } finally {
      setSubmitting(false);
    }
  };

  if (projectsQuery.isLoading) {
    return (
      <div className="text-small flex min-h-screen items-center justify-center bg-background px-6 text-center text-muted-foreground">
        読み込み中...
      </div>
    );
  }

  if (projectsQuery.isError) {
    return (
      <div className="text-small flex min-h-screen items-center justify-center bg-background px-6 text-center text-destructive">
        プロジェクトを取得できませんでした。
      </div>
    );
  }

  if (detailCompany || applicationId) {
    return (
      <>
        {detailCompany ? (
          <PendingCompanyApplicationDetail
            item={detailCompany}
            reviewMode={detailReviewMode}
            onBack={closeCompanyDetail}
            onApprove={openApproveCompany}
            onReject={openRejectCompany}
          />
        ) : (
          <div className="flex min-h-screen items-center justify-center bg-background px-6 text-small text-muted-foreground">
            {companyListLoading
              ? '読み込み中...'
              : '申請情報を取得できませんでした。'}
          </div>
        )}
        <PendingCompanyActionDialog
          open={pendingDialogOpen}
          mode={pendingDialogMode}
          item={selectedCompany}
          rejectReason={rejectReason}
          fieldErrors={actionFieldErrors}
          onOpenChange={handlePendingDialogOpenChange}
          onRejectReasonChange={setRejectReason}
          onConfirm={handleConfirmPendingCompany}
          isSubmitting={submitting}
        />
      </>
    );
  }

  const showApplicationSection =
    companyListLoading || Boolean(companyListError) || applicationCompanies.length > 0;
  const showParticipatingSection =
    !companyListError &&
    (companyListLoading ||
      participatingCompanies.length > 0 ||
      !showApplicationSection);

  return (
    <div className="flex min-h-screen flex-col bg-card">
      <AdminDashboardHero />

      <Tabs defaultValue={initialTab} className="flex flex-1 flex-col bg-card">
        <AdminDashboardTabs />

        <main className={cn('flex-1 space-y-10 py-8', dashboardContainer)}>
          <TabsContent
            value="companies"
            className="m-0 space-y-10 animate-in fade-in-0 slide-in-from-bottom-4 duration-500"
          >
            {showApplicationSection && (
              <AdminDashboardSection
                title="参加申請中の企業"
                description="参加申請を受け付けた企業です。内容を確認し、承認または差し戻しを行ってください。"
              >
                {companyListLoading ? (
                  <AdminDashboardEmptyState>読み込み中...</AdminDashboardEmptyState>
                ) : companyListError ? (
                  <AdminDashboardEmptyState>
                    {companyListError}
                  </AdminDashboardEmptyState>
                ) : (
                  <>
                    <AdminCompanyApplicationsTable
                      items={applicationCompanies}
                      emptyMessage="参加申請中の企業はありません。"
                      onDetails={openCompanyDetails}
                      onReview={openCompanyReview}
                    />
                    <PaginationControls
                      currentPage={applicationPagination.page}
                      totalPages={applicationPagination.totalPages}
                      totalItems={applicationPagination.totalItems}
                      itemsPerPage={companyLimit}
                      rowsPerPageOptions={[10, 20, 50, 100]}
                      onPageChange={setApplicationPage}
                      onItemsPerPageChange={handleCompanyLimitChange}
                      className="mt-4"
                    />
                  </>
                )}
              </AdminDashboardSection>
            )}

            {showParticipatingSection && (
              <AdminDashboardSection
                title="参加企業"
                description="承認済みの参加企業です。企業情報と運用状況を確認できます。"
              >
                {companyListLoading ? (
                  <AdminDashboardEmptyState>読み込み中...</AdminDashboardEmptyState>
                ) : (
                  <>
                    <AdminCompanyApplicationsTable
                      items={participatingCompanies}
                      emptyMessage="参加企業はありません。"
                      onDetails={openCompanyDetails}
                      onReview={openCompanyReview}
                    />
                    <PaginationControls
                      currentPage={participatingPagination.page}
                      totalPages={participatingPagination.totalPages}
                      totalItems={participatingPagination.totalItems}
                      itemsPerPage={companyLimit}
                      rowsPerPageOptions={[10, 20, 50, 100]}
                      onPageChange={setParticipatingPage}
                      onItemsPerPageChange={handleCompanyLimitChange}
                      className="mt-4"
                    />
                  </>
                )}
              </AdminDashboardSection>
            )}
          </TabsContent>

          <TabsContent
            value="projects"
            className="m-0 animate-in fade-in-0 slide-in-from-bottom-4 duration-500"
          >
            <AdminDashboardSection
              title="プロジェクト管理"
              description="登録済みプロジェクトの詳細確認、更新、ステータス変更を行えます。"
              action={
                <Button
                  type="button"
                  size="md"
                  onClick={() => navigate('/admin/project-register')}
                  className="self-start sm:self-auto"
                >
                  <Plus className="h-4 w-4" />
                  プロジェクトを追加
                </Button>
              }
            >
              <ProjectTable
                projects={paginatedProjects}
                onEdit={handleEditProject}
                onUpdate={handleUpdateProject}
                onStatusChange={handleChangeProjectStatus}
                updatingProjectStatusId={updatingProjectStatusId}
              />
              <PaginationControls
                currentPage={projectPage}
                totalPages={projectTotalPages}
                totalItems={projectTotalItems}
                itemsPerPage={projectItemsPerPage}
                onPageChange={goToProjectPage}
                onItemsPerPageChange={setProjectItemsPerPage}
                className="mt-4"
              />
            </AdminDashboardSection>
          </TabsContent>

          <TabsContent
            value="operators"
            className="m-0 animate-in fade-in-0 slide-in-from-bottom-4 duration-500"
          >
            <AdminDashboardSection
              title="運用者一覧"
              description="登録済み運用者のアカウント情報と所属拠点を確認できます。"
            >
              <AdminDashboardEmptyState>
                表示できる運用者データがありません。
              </AdminDashboardEmptyState>
            </AdminDashboardSection>
          </TabsContent>

          <TabsContent
            value="methodologies"
            className="m-0 animate-in fade-in-0 slide-in-from-bottom-4 duration-500"
          >
            <AdminDashboardSection
              title="方法論一覧"
              description="利用可能な方法論の一覧をここで管理できるようにします。"
            >
              <AdminDashboardEmptyState>
                表示できる方法論データがありません。
              </AdminDashboardEmptyState>
            </AdminDashboardSection>
          </TabsContent>

          <TabsContent
            value="monitoring"
            className="m-0 space-y-10 animate-in fade-in-0 slide-in-from-bottom-4 duration-500"
          >
            <AdminDashboardSection
              title="モニタリングデータ一覧"
              description="プロジェクトを選択して、登録済みのモニタリングデータを確認できます。"
            >
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
                  projects={projects}
                  projectsLoading={projectsQuery.isLoading}
                  projectsError={
                    projectsQuery.error instanceof Error
                      ? projectsQuery.error.message
                      : null
                  }
                  selectedProjectId={selectedMonitoringProjectId}
                  initialPage={Math.max(
                    Number(searchParams.get('monitoringPage')) || 1,
                    1,
                  )}
                  initialLimit={Math.max(
                    Number(searchParams.get('monitoringLimit')) || 10,
                    1,
                  )}
                  onProjectChange={handleMonitoringProjectChange}
                  onViewDetail={openMonitoringDetail}
                />
              </ErrorBoundary>
            </AdminDashboardSection>
            <AdminDashboardSection
              title="取消一覧"
              description="取消対象のモニタリングデータを確認し、取消申請を送信できます。"
            >
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
                    <div className="space-y-1">
                      <p className="text-sm font-medium text-foreground">
                        モニタリングデータを表示できませんでした。
                      </p>
                      <p className="text-subtle text-muted-foreground">
                        もう一度お試しください。
                      </p>
                    </div>
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
                <CancelRequestHistoryTable
                  initialRequests={cancelRequestItems}
                  initialLoading={cancelRequestLoading}
                  initialLoadError={cancelRequestError}
                  onViewRequest={openCancelRequestDetail}
                />
              </ErrorBoundary>
            </AdminDashboardSection>
            <AdminDashboardSection
              title="バッチ履歴"
              description="バッチ処理の実行状況と処理履歴を確認できます。"
            >
              <BatchHistoryTable
                initialItems={batchHistoryItems}
                initialPagination={batchHistoryPagination}
                initialLoading={batchHistoryLoading}
                initialLoadError={batchHistoryError}
              />
            </AdminDashboardSection>
          </TabsContent>
        </main>
      </Tabs>

      <PendingCompanyActionDialog
        open={pendingDialogOpen}
        mode={pendingDialogMode}
        item={selectedCompany}
        rejectReason={rejectReason}
        fieldErrors={actionFieldErrors}
        onOpenChange={handlePendingDialogOpenChange}
        onRejectReasonChange={setRejectReason}
        onConfirm={handleConfirmPendingCompany}
        isSubmitting={submitting}
      />

    </div>
  );
};

export default AdminDashboard;
