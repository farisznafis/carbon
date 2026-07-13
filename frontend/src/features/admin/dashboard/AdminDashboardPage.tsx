import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '@/api/_shared/errors';
import type {
  AdminCompanyListItem,
  CompanyStatus,
  CompanySuspension,
  Pagination,
} from '@/api/admin';
import {
  approveAdminCompany,
  getAdminCompanies,
  getCompanySuspensions,
  rejectAdminCompany,
  resolveCompanySuspension,
} from '@/api/admin';
import {
  updateAdminProjectStatus,
  type ProjectStatus,
} from '@/api/admin/projects';
import type { Project } from '@/api/projects';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useAuth } from '@/contexts/AuthContext';
import { CancelRequestHistoryTable, type CancelRequestItem } from '@/features/admin/dashboard/tables/CancelRequestHistoryTable';
import { AdminCompanyApplicationsTable } from '@/features/admin/dashboard/tables/AdminCompanyApplicationsTable';
import { CompanySuspensionResolveDialog } from '@/features/admin/dashboard/tables/CompanySuspensionResolveDialog';
import { PendingCompanyActionDialog } from '@/features/admin/dashboard/tables/PendingCompanyActionDialog';
import { PendingCompanyApplicationDetail } from '@/features/admin/dashboard/tables/PendingCompanyApplicationDetail';
import { ProjectTable } from '@/features/admin/dashboard/tables/ProjectTable';
import { useProjectsQuery } from '@/hooks/projects/useProjectsQuery';
import { useToast } from '@/hooks/use-toast';
import { usePagination } from '@/hooks/usePagination';
import { cn } from '@/lib/utils';
import { paths } from '@/routes/paths';
import { useNotificationStore } from '@/stores/notificationStore';
import { Plus } from 'lucide-react';

type AdminDashboardTabValue =
  | 'companies'
  | 'projects'
  | 'operators'
  | 'methodologies'
  | 'monitoring';

type PendingCompanyActionMode = 'approve' | 'reject';

const dashboardContainer = 'mx-auto w-full max-w-[1200px] px-4 sm:px-6 lg:px-0';

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
        className={cn(
          'flex min-h-[140px] items-center justify-center text-center',
          dashboardContainer,
        )}
      >
        <h1 className="leading-[48px] text-foreground">
          運営者・管理者ダッシュボード
        </h1>
      </div>
    </header>
  );
}

function AdminDashboardTabs() {
  const triggerClassName = cn(
    'h-12 rounded-xl border border-border bg-card px-4 py-2',
    'text-body font-bold text-foreground shadow-app-sm transition-all duration-200',
    'hover:border-primary/60 hover:bg-secondary/50',
    'data-[state=active]:border-[#0d9488] data-[state=active]:bg-primary data-[state=active]:text-primary-foreground',
    'data-[state=active]:hover:bg-primary/90',
  );

  return (
    <div className="bg-card pt-6">
      <TabsList className="mx-auto flex h-auto w-fit max-w-full flex-wrap items-center justify-center gap-2 rounded-none bg-transparent p-0">
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
    <div className="rounded-xl border border-dashed border-border bg-card px-6 py-10 text-center text-small text-muted-foreground">
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
  const [cancelRequestDialogOpen, setCancelRequestDialogOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');

  const [resolveDialogOpen, setResolveDialogOpen] = useState(false);
  const [resolveLoading, setResolveLoading] = useState(false);
  const [activeSuspensions, setActiveSuspensions] = useState<
    CompanySuspension[]
  >([]);
  const [selectedSuspensionId, setSelectedSuspensionId] = useState<
    number | null
  >(null);
  const [resolutionNote, setResolutionNote] = useState('');

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
      const numericProjectId = Number(project.id);
      const companyId =
        project.companyId ?? project.operatorCompanyId ?? user?.companyId ?? '';
      const projectStatusPayload = {
        id: Number.isFinite(numericProjectId) ? numericProjectId : project.id,
        processedBy: user?.id ? String(user.id) : 'admin',
        companyId: String(companyId),
        projectStatus: status,
      };

      await updateAdminProjectStatus(project.id, projectStatusPayload);
      await projectsQuery.refetch();
      toast({
        title: 'ステータスを更新しました',
        description: `${project.projectName}: ${status}`,
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

  const cancelRequestHistory: CancelRequestItem[] = [
    {
      id: 'REG-123456',
      userName: '山田 太郎',
      siteName: 'ABC工場',
      reason: '誤って登録したため、解除を希望します。',
      requestedAt: '2026/04/27 10:15',
      status: 'PENDING',
    },
    {
      id: 'REG-123457',
      userName: '佐藤 花子',
      siteName: 'DEF支店',
      reason: '責任者変更のため、一度解除したいです。',
      requestedAt: '2026/04/26 14:40',
      status: 'PENDING',
    },
  ];

  const handleApproveCancelRequest = (_id: string) => {
    setCompanyListError(null);
  };

  const handleRejectCancelRequest = (_id: string) => {
    setCompanyListError(null);
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

  const loadActiveSuspensions = async (companyId: string) => {
    setResolveLoading(true);
    setActionFieldErrors({});
    try {
      const response = await getCompanySuspensions(companyId, {
        activeOnly: true,
        page: 1,
        limit: 20,
      });
      setActiveSuspensions(response.suspensions);
      setSelectedSuspensionId(response.suspensions[0]?.id ?? null);
    } catch (error) {
      setCompanyListError(
        getApiMessage(error, '停止理由の取得に失敗しました。'),
      );
    } finally {
      setResolveLoading(false);
    }
  };

  const openResolveSuspension = (item: AdminCompanyListItem) => {
    setSelectedCompany(item);
    setActiveSuspensions([]);
    setSelectedSuspensionId(null);
    setResolutionNote('');
    setActionFieldErrors({});
    setResolveDialogOpen(true);
    void loadActiveSuspensions(item.companyId);
  };

  const handleResolveDialogOpenChange = (open: boolean) => {
    setResolveDialogOpen(open);

    if (!open) {
      setSelectedCompany(null);
      setActiveSuspensions([]);
      setSelectedSuspensionId(null);
      setResolutionNote('');
      setActionFieldErrors({});
    }
  };

  const handleConfirmResolveSuspension = async () => {
    if (!selectedCompany || selectedSuspensionId === null) return;

    const suspension = activeSuspensions.find(
      (item) => item.id === selectedSuspensionId,
    );
    if (!suspension) return;

    setSubmitting(true);
    setActionFieldErrors({});
    try {
      await resolveCompanySuspension(
        selectedCompany.companyId,
        selectedSuspensionId,
        {
          resolutionNote,
          ...(suspension.reasonCode === 'BILLING'
            ? { paymentMethod: 'CREDIT_CARD' as const }
            : {}),
        },
      );
      await reloadCompanyLists();
      handleResolveDialogOpenChange(false);
      toast({ title: '停止を解除しました' });
    } catch (error) {
      setActionFieldErrors(getApiFieldErrors(error));
      setCompanyListError(
        getApiMessage(error, '停止解除に失敗しました。'),
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

      <Tabs defaultValue="companies" className="flex flex-1 flex-col bg-card">
        <AdminDashboardTabs />

        <main className={cn('flex-1 space-y-10 py-6', dashboardContainer)}>
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
                      onResolveSuspension={openResolveSuspension}
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
            className="m-0 animate-in fade-in-0 slide-in-from-bottom-4 duration-500"
          >
            <AdminDashboardSection
              title="モニタリングデータ管理"
              description="提出されたモニタリングデータの確認と管理を行います。"
            >
              <AdminDashboardEmptyState>
                表示できるモニタリングデータがありません。
              </AdminDashboardEmptyState>
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

      <CompanySuspensionResolveDialog
        open={resolveDialogOpen}
        item={selectedCompany}
        suspensions={activeSuspensions}
        selectedSuspensionId={selectedSuspensionId}
        resolutionNote={resolutionNote}
        fieldErrors={actionFieldErrors}
        isLoading={resolveLoading}
        isSubmitting={submitting}
        onOpenChange={handleResolveDialogOpenChange}
        onSelectedSuspensionIdChange={setSelectedSuspensionId}
        onResolutionNoteChange={setResolutionNote}
        onConfirm={handleConfirmResolveSuspension}
      />

      <Dialog
        open={cancelRequestDialogOpen}
        onOpenChange={setCancelRequestDialogOpen}
      >
        <DialogContent className="max-h-[85vh] w-[calc(100vw-2rem)] max-w-6xl overflow-y-auto">
          <DialogHeader>
            <DialogTitle>取り消し一覧</DialogTitle>
          </DialogHeader>
          <CancelRequestHistoryTable
            items={cancelRequestHistory}
            onApprove={handleApproveCancelRequest}
            onReject={handleRejectCancelRequest}
            showTitle={false}
          />
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default AdminDashboard;
