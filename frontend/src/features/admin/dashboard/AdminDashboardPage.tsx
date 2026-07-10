import { useEffect, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Project } from '@/api/projects';
import type { UserList } from '@/api/users';
import {
  approvePendingCompany,
  getPendingCompanies,
  rejectPendingCompany,
  type PendingCompanyItem,
} from '@/api/admin/pendingCompanies';
import {
  updateAdminProjectStatus,
  type ProjectStatus,
} from '@/api/admin/projects';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { UserTable } from '@/components/data-display/tables/UserTable';
import { ProjectTable } from '@/features/admin/dashboard/tables/ProjectTable';
import {
  CancelRequestHistoryTable,
  type CancelRequestItem,
} from '@/features/admin/dashboard/tables/CancelRequestHistoryTable';
import { AdminCompanyApplicationsTable } from '@/features/admin/dashboard/tables/AdminCompanyApplicationsTable';
import { PendingCompanyActionDialog } from '@/features/admin/dashboard/tables/PendingCompanyActionDialog';
import { PendingCompanyApplicationDetail } from '@/features/admin/dashboard/tables/PendingCompanyApplicationDetail';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/hooks/use-toast';
import { usePagination } from '@/hooks/usePagination';
import { useProjectsQuery } from '@/hooks/projects/useProjectsQuery';
import { cn } from '@/lib/utils';
import { paths } from '@/routes/paths';
import { useDashboardStore } from '@/stores/dashboardStore';
import { useNotificationStore } from '@/stores/notificationStore';
import { Plus } from 'lucide-react';

type ErrorWithApiMessage = {
  message?: string;
  response?: {
    data?: {
      message?: string;
    };
  };
};

type AdminDashboardTabValue =
  | 'companies'
  | 'projects'
  | 'operators'
  | 'methodologies'
  | 'monitoring';

const dashboardContainer = 'mx-auto w-full max-w-[1200px] px-4 sm:px-6 lg:px-0';

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

function AdminDashboardHero() {
  return (
    <header className="border-b border-border bg-background">
      <div
        className={cn(
          'flex min-h-[140px] items-center justify-center text-center',
          dashboardContainer,
        )}
      >
        <h1 className="text-[40px] leading-[48px] text-foreground">
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
  const { toast } = useToast();
  const { user } = useAuth();
  const projectsQuery = useProjectsQuery();
  const projects = projectsQuery.data ?? [];
  const loadNotifications = useNotificationStore(
    (state) => state.loadNotifications,
  );

  const users = useDashboardStore((state) => state.users) ?? [];
  const loadingStore = useDashboardStore((state) => state.loading);
  const fetchDashboardData = useDashboardStore(
    (state) => state.fetchDashboardData,
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

  const [dashboardLoading, setDashboardLoading] = useState(true);
  const [dashboardError, setDashboardError] = useState<string | null>(null);
  const [pendingCompanies, setPendingCompanies] = useState<
    PendingCompanyItem[]
  >([]);
  const [pendingLoading, setPendingLoading] = useState(true);
  const [pendingError, setPendingError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [updatingProjectStatusId, setUpdatingProjectStatusId] = useState<
    string | null
  >(null);

  const [pendingDialogOpen, setPendingDialogOpen] = useState(false);
  const [pendingDialogMode, setPendingDialogMode] = useState<
    'approve' | 'reject' | null
  >(null);
  const [selectedPendingCompany, setSelectedPendingCompany] =
    useState<PendingCompanyItem | null>(null);
  const [detailCompany, setDetailCompany] = useState<PendingCompanyItem | null>(
    null,
  );
  const [detailReviewMode, setDetailReviewMode] = useState(false);
  const [cancelRequestDialogOpen, setCancelRequestDialogOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');

  const reloadPendingCompanies = async () => {
    setPendingLoading(true);
    setPendingError(null);

    try {
      const items = await getPendingCompanies();
      setPendingCompanies(items);
    } catch (error) {
      setPendingError(
        error instanceof Error
          ? error.message
          : '申請中アカウントの取得に失敗しました。',
      );
    } finally {
      setPendingLoading(false);
    }
  };

  useEffect(() => {
    let isMounted = true;

    async function loadDashboardData() {
      setDashboardLoading(true);
      setDashboardError(null);
      void loadNotifications();

      try {
        await Promise.all([fetchDashboardData(true), reloadPendingCompanies()]);
      } catch (error) {
        if (isMounted) {
          setDashboardError(
            error instanceof Error
              ? error.message
              : 'ダッシュボードデータの取得に失敗しました。',
          );
        }
      } finally {
        if (isMounted) {
          setDashboardLoading(false);
        }
      }
    }

    void loadDashboardData();

    return () => {
      isMounted = false;
    };
  }, [fetchDashboardData, loadNotifications]);

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
        description:
          error instanceof Error
            ? error.message
            : 'プロジェクトステータスを更新できませんでした。',
        variant: 'destructive',
      });
    } finally {
      setUpdatingProjectStatusId(null);
    }
  };

  const handleEditUser = (dashboardUser: UserList) => {
    navigate(`/user-edit/${dashboardUser.id}`, { state: { user: dashboardUser } });
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
    {
      id: 'REG-123458',
      userName: '鈴木 一郎',
      siteName: 'GHI工場',
      reason: 'サイト利用を終了しました。',
      requestedAt: '2026/04/25 09:05',
      status: 'APPROVED',
    },
  ];

  const handleApproveCancelRequest = (_id: string) => {
    setPendingError(null);
  };

  const handleRejectCancelRequest = (_id: string) => {
    setPendingError(null);
  };

  const openPendingCompanyDetails = (item: PendingCompanyItem) => {
    setDetailCompany(item);
    setDetailReviewMode(false);
  };

  const openPendingCompanyReview = (item: PendingCompanyItem) => {
    setDetailCompany(item);
    setDetailReviewMode(true);
  };

  const closePendingCompanyDetail = () => {
    setDetailCompany(null);
    setDetailReviewMode(false);
  };

  const handleDeleteRejectedCompany = (item: PendingCompanyItem) => {
    setPendingCompanies((currentItems) =>
      currentItems.filter((company) => company.id !== item.id),
    );
    toast({
      title: '削除しました',
      description: `${item.nameJA} を一覧から削除しました。`,
    });
  };

  const openApprovePendingCompany = (item: PendingCompanyItem) => {
    setSelectedPendingCompany(item);
    setPendingDialogMode('approve');
    setRejectReason('');
    setPendingDialogOpen(true);
  };

  const openRejectPendingCompany = (item: PendingCompanyItem) => {
    setSelectedPendingCompany(item);
    setPendingDialogMode('reject');
    setRejectReason('');
    setPendingDialogOpen(true);
  };

  const handlePendingDialogOpenChange = (open: boolean) => {
    setPendingDialogOpen(open);

    if (!open) {
      setSelectedPendingCompany(null);
      setPendingDialogMode(null);
      setRejectReason('');
    }
  };

  const handleConfirmPendingCompany = async () => {
    if (!selectedPendingCompany || !pendingDialogMode) return;

    setSubmitting(true);
    try {
      const id = selectedPendingCompany.id;
      if (pendingDialogMode === 'approve') {
        await approvePendingCompany(id);
      } else {
        await rejectPendingCompany(id, rejectReason);
      }
      await reloadPendingCompanies();
      handlePendingDialogOpenChange(false);
      closePendingCompanyDetail();
    } catch (error) {
      const apiError = error as ErrorWithApiMessage;
      setPendingError(
        apiError.response?.data?.message ||
          apiError.message ||
          (pendingDialogMode === 'approve'
            ? '承認に失敗しました。'
            : '却下に失敗しました。'),
      );
    } finally {
      setSubmitting(false);
    }
  };

  if (projectsQuery.isLoading || dashboardLoading || loadingStore) {
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

  if (detailCompany) {
    return (
      <>
        <PendingCompanyApplicationDetail
          item={detailCompany}
          reviewMode={detailReviewMode}
          onBack={closePendingCompanyDetail}
          onApprove={openApprovePendingCompany}
          onReject={openRejectPendingCompany}
        />
        <PendingCompanyActionDialog
          open={pendingDialogOpen}
          mode={pendingDialogMode}
          item={selectedPendingCompany}
          rejectReason={rejectReason}
          onOpenChange={handlePendingDialogOpenChange}
          onRejectReasonChange={setRejectReason}
          onConfirm={handleConfirmPendingCompany}
          isSubmitting={submitting}
        />
      </>
    );
  }

  const applicationCompanies = pendingCompanies.filter(
    (company) => company.status === '申請受付' || company.status === '却下',
  );
  const participatingCompanies = pendingCompanies.filter(
    (company) => company.status === '承認済み' || company.status === '停止',
  );

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
            <AdminDashboardSection
              title="参加申請中の企業"
              description="参加申請を受け付けた企業です。内容を確認し、承認または差し戻しを行ってください。"
              // action={
              //   <Button
              //     type="button"
              //     variant="outline"
              //     onClick={() => setCancelRequestDialogOpen(true)}
              //     className="self-start sm:self-auto"
              //   >
              //     取消申請一覧
              //   </Button>
              // }
            >
              {pendingLoading ? (
                <AdminDashboardEmptyState>読み込み中...</AdminDashboardEmptyState>
              ) : pendingError ? (
                <AdminDashboardEmptyState>{pendingError}</AdminDashboardEmptyState>
              ) : (
                <AdminCompanyApplicationsTable
                  items={applicationCompanies}
                  emptyMessage="参加申請中の企業はありません。"
                  onDetails={openPendingCompanyDetails}
                  onReview={openPendingCompanyReview}
                  onDeleteRejected={handleDeleteRejectedCompany}
                />
              )}
            </AdminDashboardSection>

            <AdminDashboardSection
              title="参加企業"
              description="承認済みの参加企業です。企業情報と運用状況を確認できます。"
            >
              <AdminCompanyApplicationsTable
                items={participatingCompanies}
                emptyMessage="参加企業はありません。"
                onDetails={openPendingCompanyDetails}
                onReview={openPendingCompanyReview}
              />
            </AdminDashboardSection>
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
              <div className="mt-4">
                <PaginationControls
                  currentPage={projectPage}
                  totalPages={projectTotalPages}
                  totalItems={projectTotalItems}
                  itemsPerPage={projectItemsPerPage}
                  onPageChange={goToProjectPage}
                  onItemsPerPageChange={setProjectItemsPerPage}
                />
              </div>
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
              {dashboardError ? (
                <AdminDashboardEmptyState>{dashboardError}</AdminDashboardEmptyState>
              ) : (
                <UserTable users={users} onEdit={handleEditUser} />
              )}
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
        item={selectedPendingCompany}
        rejectReason={rejectReason}
        onOpenChange={handlePendingDialogOpenChange}
        onRejectReasonChange={setRejectReason}
        onConfirm={handleConfirmPendingCompany}
        isSubmitting={submitting}
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
