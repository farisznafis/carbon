import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import {
  useLocation,
  useNavigate,
  useParams,
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
} from '@/api/monitoring';
import {
  updateAdminProjectStatus,
  type ProjectStatus,
} from '@/api/admin/projects';
import type { Project } from '@/api/projects';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { ErrorBoundary } from '@/components/feedback/ErrorBoundary';
import {
  adminPageContainerClassName,
  dashboardEmptyStateClassName,
} from '@/components/layout/DashboardLayout';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent } from '@/components/ui/tabs';
import { useAuth } from '@/contexts/AuthContext';
import { CancelRequestHistoryTable } from '@/features/admin/dashboard/tables/CancelRequestHistoryTable';
import { BatchHistoryTable } from '@/features/admin/dashboard/tables/BatchHistoryTable';
import { AdminCompanyApplicationsTable } from '@/features/admin/dashboard/tables/AdminCompanyApplicationsTable';
import { PendingCompanyActionDialog } from '@/features/admin/dashboard/tables/PendingCompanyActionDialog';
import { PendingCompanyApplicationDetail } from '@/features/admin/dashboard/tables/PendingCompanyApplicationDetail';
import { ProjectTable } from '@/features/admin/dashboard/tables/ProjectTable';
import { AdminDashboardOverview } from './AdminDashboardOverview';
import { PageSkeleton, SkeletonTable } from '@/components/feedback/loading';
import { useProjectsQuery } from '@/hooks/projects/useProjectsQuery';
import { useToast } from '@/hooks/use-toast';
import { usePagination } from '@/hooks/usePagination';
import { paths } from '@/routes/paths';
import { getAdminRouteFromLegacyTab } from '@/features/admin/navigation/adminNavigationConfig';
import { useNotificationStore } from '@/stores/notificationStore';
import { AlertTriangle, Plus } from 'lucide-react';

type AdminDashboardTabValue =
  | 'overview'
  | 'companies'
  | 'projects'
  | 'operators'
  | 'methodologies'
  | 'monitoring';

type PendingCompanyActionMode = 'approve' | 'reject';

const dashboardContainer = adminPageContainerClassName;

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
  // { value: 'overview', label: '概要' },
  { value: 'companies', label: '企業一覧・申請管理' },
  { value: 'projects', label: 'プロジェクト管理' },
  { value: 'operators', label: '運用者一覧' },
  { value: 'methodologies', label: '方法論一覧' },
  { value: 'monitoring', label: 'モニタリングデータ管理' },
];

/** Resolves the route-driven dashboard section while legacy tab URLs redirect. */
function getAdminDashboardSection(
  pathname: string,
  legacyTab: string | null,
): AdminDashboardTabValue {
  if (pathname === paths.adminProjectList) return 'projects';
  if (pathname === paths.adminCompanyList) return 'companies';
  if (pathname === paths.adminMethodologyList) return 'methodologies';
  if (pathname === paths.adminMonitoring) return 'monitoring';

  if (
    pathname === paths.adminDashboard &&
    (legacyTab === 'overview' ||
      adminDashboardTabs.some((tab) => tab.value === legacyTab))
  ) {
    return legacyTab as AdminDashboardTabValue;
  }

  return 'overview';
}

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
          <p className="text-subtle text-foreground">{description}</p>
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
  const { applicationId } = useParams();
  const { toast } = useToast();
  const { user } = useAuth();
  const projectsQuery = useProjectsQuery();
  const projects = projectsQuery.data ?? [];
  const loadNotifications = useNotificationStore(
    (state) => state.loadNotifications,
  );
  const notifications = useNotificationStore((state) => state.notifications);
  const notificationsLoading = useNotificationStore((state) => state.loading);
  const notificationsError = useNotificationStore((state) => state.error);

  const {
    currentPage: projectPage,
    totalPages: projectTotalPages,
    totalItems: projectTotalItems,
    itemsPerPage: projectItemsPerPage,
    paginatedItems: paginatedProjects,
    goToPage: goToProjectPage,
    setItemsPerPage: setProjectItemsPerPage,
  } = usePagination(projects);

  const [applicationCompaniesLoading, setApplicationCompaniesLoading] = useState(true);
  const [applicationCompaniesError, setApplicationCompaniesError] = useState<string | null>(null);
  const [participatingCompaniesLoading, setParticipatingCompaniesLoading] = useState(true);
  const [participatingCompaniesError, setParticipatingCompaniesError] = useState<string | null>(null);
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

  const requestedTab = new URLSearchParams(location.search).get('tab');
  const activeSection = getAdminDashboardSection(
    location.pathname,
    requestedTab,
  );

  useEffect(() => {
    if (location.pathname !== paths.adminDashboard || !requestedTab) return;

    const legacyRoute = getAdminRouteFromLegacyTab(requestedTab);
    if (legacyRoute) {
      navigate(legacyRoute, { replace: true });
    }
  }, [location.pathname, navigate, requestedTab]);

  /** Reloads the application-company list independently from participating companies. */
  const reloadApplicationCompanies = useCallback(async () => {
    setApplicationCompaniesLoading(true);
    setApplicationCompaniesError(null);

    try {
      const applications = await getAdminCompanies({
        limit: companyLimit,
        status: APPLICATION_STATUSES,
        page: applicationPage,
      });
      setApplicationCompanies(applications.companies);
      setApplicationPagination(applications.pagination);
    } catch (error) {
      setApplicationCompaniesError(
        getApiMessage(error, '企業一覧の取得に失敗しました。'),
      );
    } finally {
      setApplicationCompaniesLoading(false);
    }
  }, [applicationPage, companyLimit]);

  /** Reloads participating companies without blocking application reviews. */
  const reloadParticipatingCompanies = useCallback(async () => {
    setParticipatingCompaniesLoading(true);
    setParticipatingCompaniesError(null);

    try {
      const participating = await getAdminCompanies({
        limit: companyLimit,
        status: PARTICIPATING_STATUSES,
        page: participatingPage,
      });
      setParticipatingCompanies(participating.companies);
      setParticipatingPagination(participating.pagination);
    } catch (error) {
      setParticipatingCompaniesError(
        getApiMessage(error, '参加企業一覧の取得に失敗しました。'),
      );
    } finally {
      setParticipatingCompaniesLoading(false);
    }
  }, [companyLimit, participatingPage]);

  /** Refreshes both company groups after a company mutation. */
  const reloadCompanyLists = useCallback(async () => {
    await Promise.all([
      reloadApplicationCompanies(),
      reloadParticipatingCompanies(),
    ]);
  }, [reloadApplicationCompanies, reloadParticipatingCompanies]);

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
    void reloadApplicationCompanies();
  }, [reloadApplicationCompanies]);

  useEffect(() => {
    void reloadParticipatingCompanies();
  }, [reloadParticipatingCompanies]);

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
      setApplicationCompaniesError(
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
          applicationCompaniesLoading ? (
            <PageSkeleton />
          ) : (
            <div className="flex min-h-screen items-center justify-center bg-background px-6 text-small text-foreground">
              申請情報を取得できませんでした。
            </div>
          )
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
    applicationCompaniesLoading || Boolean(applicationCompaniesError) || applicationCompanies.length > 0;
  const showParticipatingSection =
    !participatingCompaniesError &&
    (participatingCompaniesLoading ||
      participatingCompanies.length > 0 ||
      !showApplicationSection);
  const projectsError = projectsQuery.error instanceof Error
    ? projectsQuery.error.message
    : projectsQuery.isError
      ? 'プロジェクトを取得できませんでした。'
      : null;

  return (
    <div className="flex min-h-[calc(100dvh-5rem)] flex-col bg-background">
      <Tabs value={activeSection} className="flex flex-1 flex-col bg-background">
        <main className={`${dashboardContainer} flex-1 space-y-10 py-8`}>
          {/* <TabsContent
            value="overview"
            className="m-0 animate-in fade-in-0 slide-in-from-bottom-4 duration-500"
          >
            <AdminDashboardOverview
              projects={projects}
              projectsLoading={projectsQuery.isLoading}
              projectsError={projectsError}
              applications={applicationCompanies}
              applicationTotal={applicationPagination.totalItems}
              applicationsLoading={applicationCompaniesLoading}
              applicationsError={applicationCompaniesError}
              participatingCompanyTotal={participatingPagination.totalItems}
              participatingCompaniesLoading={participatingCompaniesLoading}
              participatingCompaniesError={participatingCompaniesError}
              cancelRequests={cancelRequestItems}
              cancelRequestsLoading={cancelRequestLoading}
              cancelRequestsError={cancelRequestError}
              batchHistory={batchHistoryItems}
              batchHistoryLoading={batchHistoryLoading}
              notifications={notifications}
              notificationsLoading={notificationsLoading}
              notificationsError={notificationsError}
            />
          </TabsContent> */}

          <TabsContent
            value="overview"
            className="m-0 animate-in fade-in-0 slide-in-from-bottom-4 duration-500"
          >
            <AdminDashboardOverview
              projects={projects}
              projectsLoading={projectsQuery.isLoading}
              projectsError={projectsError}
              applications={applicationCompanies}
              applicationTotal={applicationPagination.totalItems}
              applicationsLoading={applicationCompaniesLoading}
              applicationsError={applicationCompaniesError}
              participatingCompanyTotal={participatingPagination.totalItems}
              participatingCompaniesLoading={participatingCompaniesLoading}
              participatingCompaniesError={participatingCompaniesError}
              cancelRequests={cancelRequestItems}
              cancelRequestsLoading={cancelRequestLoading}
              cancelRequestsError={cancelRequestError}
              batchHistory={batchHistoryItems}
              batchHistoryLoading={batchHistoryLoading}
              notifications={notifications}
              notificationsLoading={notificationsLoading}
              notificationsError={notificationsError}
            />
          </TabsContent>

          <TabsContent
            value="companies"
            className="m-0 space-y-10 animate-in fade-in-0 slide-in-from-bottom-4 duration-500"
          >
            {showApplicationSection && (
              <AdminDashboardSection
                title="参加申請中の企業"
                description="参加申請を受け付けた企業です。内容を確認し、承認または差し戻しを行ってください。"
              >
                {applicationCompaniesLoading ? (
                  <SkeletonTable columns={5} rows={6} minWidth={980} />
                ) : applicationCompaniesError ? (
                  <AdminDashboardEmptyState>
                    {applicationCompaniesError}
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
                {participatingCompaniesLoading ? (
                  <SkeletonTable columns={5} rows={6} minWidth={980} />
                ) : participatingCompaniesError ? (
                  <AdminDashboardEmptyState>{participatingCompaniesError}</AdminDashboardEmptyState>
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
              {projectsQuery.isLoading ? (
                <SkeletonTable columns={6} rows={6} minWidth={980} />
              ) : projectsError ? (
                <AdminDashboardEmptyState>{projectsError}</AdminDashboardEmptyState>
              ) : (
                <>
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
                </>
              )}
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
                      <p className="text-subtle text-foreground">
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
