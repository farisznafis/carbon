//運用・管理者　ツールページ・ダッシュボード
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Plus } from 'lucide-react';
import { useDashboardStore } from '@/stores/dashboardStore';
import type { Project } from '@/api/projects';
import type { Site } from '@/api/site';
import type { UserList } from '@/api/users';
import { useSiteFormStore } from '@/stores/FormStore';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ScrollTableCard } from '@/components/layout/ScrollTableCard/ScrollTableCard';
import { SiteTable } from '@/components/data-display/tables/SiteTable';
import { UserTable } from '@/components/data-display/tables/UserTable';
import { CompanyTable } from '@/features/admin/dashboard/tables/CompanyTable';
import { ProjectTable } from '@/features/admin/dashboard/tables/ProjectTable';
import { useProjectsQuery } from '@/hooks/projects/useProjectsQuery';
import {
  CancelRequestHistoryTable,
  CancelRequestItem,
} from '@/features/admin/dashboard/tables/CancelRequestHistoryTable';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { usePagination } from '@/hooks/usePagination';
import {
  getPendingCompanies,
  type PendingCompanyItem,
  approvePendingCompany,
  rejectPendingCompany, // later
} from '@/api/admin/pendingCompanies';
import { PendingCompanyTable } from '@/features/admin/dashboard/tables/PendingCompanyTable';
import { PendingCompanyActionDialog } from '@/features/admin/dashboard/tables/PendingCompanyActionDialog';
import { paths } from '@/routes/paths';
import {
  updateAdminProjectStatus,
  type ProjectStatus,
} from '@/api/admin/projects';
import { useToast } from '@/hooks/use-toast';
import { useAuth } from '@/contexts/AuthContext';
import { useNotificationStore } from '@/stores/notificationStore';

type ErrorWithApiMessage = {
  message?: string;
  response?: {
    data?: {
      message?: string;
    };
  };
};

const AdminDashboard = () => {
  const navigate = useNavigate();
  const { toast } = useToast();
  const { user } = useAuth();
  const projectsQuery = useProjectsQuery();
  const projects = projectsQuery.data ?? [];
  const [ submitting, setSubmitting] = useState(false);
  const [updatingProjectStatusId, setUpdatingProjectStatusId] = useState<string | null>(null);
  const loadNotifications = useNotificationStore((state) => state.loadNotifications);

  const {
    currentPage: projectPage,
    totalPages: projectTotalPages,
    totalItems: projectTotalItems,
    itemsPerPage: projectItemsPerPage,
    paginatedItems: paginatedProjects,
    goToPage: goToProjectPage,
    setItemsPerPage: setProjectItemsPerPage,
  } = usePagination(projects);

  const users = useDashboardStore((state) => state.users) ?? [];
  const loadingStore = useDashboardStore((state) => state.loading);

  const [loading, setLoading] = useState(true);
  const [pendingCompanies, setPendingCompanies] = useState<PendingCompanyItem[]>([]);
  const [pendingLoading, setPendingLoading] = useState(true);
  const [pendingError, setPendingError] = useState<string | null>(null);

  const [pendingDialogOpen, setPendingDialogOpen] = useState(false);
  const [pendingDialogMode, setPendingDialogMode] = useState<'approve' | 'reject' | null>(null);
  const [selectedPendingCompany, setSelectedPendingCompany] = useState<PendingCompanyItem | null>(null);
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
        error instanceof Error ? error.message : '申請中アカウントの取得に失敗しました',
      );
    } finally {
      setPendingLoading(false);
    }
  };
  
  useEffect(() => {
    let isMounted = true;
    (async () => {
      if (!isMounted) return;
      await reloadPendingCompanies();
    })();
    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    void loadNotifications();
  }, [loadNotifications]);

  if (projectsQuery.isLoading) {
    return <div>読み込み中...</div>;
  }

  if (projectsQuery.isError) {
    return <div>プロジェクトを取得できませんでした</div>;
  }

  const handleRegisterSite = () => {
    navigate('/master/site-register');
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
      const projectStatusPayload = {
        id: Number.isFinite(numericProjectId) ? numericProjectId : project.id,
        processedBy: user?.id ? String(user.id) : 'admin',
        companyId: project.companyId ?? project.operatorCompanyId ?? user?.companyId ?? '',
        projectStatus: status,
      };

      await updateAdminProjectStatus(project.id, projectStatusPayload);
      await projectsQuery.refetch();
      toast({
        title: 'ステータスを更新しました',
        description: `${project.projectName}：${status}`,
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

  const handleEditUser = (user: UserList) => {
    navigate(`/user-edit/${user.id}`, { state: { user } });
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
      reason: '担当者変更のため、一度解除したいです。',
      requestedAt: '2026/04/26 14:40',
      status: 'PENDING',
    },
    {
      id: 'REG-123458',
      userName: '鈴木 一郎',
      siteName: 'GHI工場',
      reason: 'サイト利用を終了したいため。',
      requestedAt: '2026/04/25 09:05',
      status: 'APPROVED',
    },
    {
      id: 'REG-123459',
      userName: '鈴木 一郎',
      siteName: 'GHI工場',
      reason: 'サイト利用を終了したいため。',
      requestedAt: '2026/04/25 09:05',
      status: 'APPROVED',
    },
    {
      id: 'REG-123460',
      userName: '鈴木 一郎',
      siteName: 'GHI工場',
      reason: 'サイト利用を終了したいため。',
      requestedAt: '2026/04/25 09:05',
      status: 'APPROVED',
    },
    {
      id: 'REG-123461',
      userName: '鈴木 一郎',
      siteName: 'GHI工場',
      reason: 'サイト利用を終了したいため。',
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
    } catch (e) {
      const error = e as ErrorWithApiMessage;
      setPendingError(
        error.response?.data?.message ||
        error.message ||
        (pendingDialogMode === 'approve' ? '承認に失敗しました' : '却下に失敗しました'),
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className='min-h-screen bg-slate-50'>
      <div className='container mx-auto p-6 space-y-6 w-full'>
        <div className='flex px-4 py-8'>
          <div className='flex w-full items-center justify-between'>
            <h3 className='font-bold'>運営者・管理者ツールページ</h3>
            <div className='mb-0 flex justify-end'>
              <Button
                onClick={() => setCancelRequestDialogOpen(true)}
                className='bg-slate-950 hover:bg-slate-800 text-white w-auto whitespace-nowrap'
              >
                取り消し一覧
                <span className='hidden'>
                {/* <Plus className='h-4 w-4 mr-2' /> */}
                取り消し一覧
                </span>
              </Button>
            </div>
          </div>
        </div>

        {/* Pending Account Table */}
        <Card>
          <CardContent className='p-8'>
            {pendingLoading ? (
              <div>読み込み中...</div>
            ) : pendingError ? (
              <div className='text-red-600'>{pendingError}</div>
            ) : (
              <PendingCompanyTable
                items={pendingCompanies}
                onApprove={openApprovePendingCompany}
                onReject={openRejectPendingCompany}
              />
            )}
          </CardContent>
        </Card>

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
          <DialogContent className='max-h-[85vh] w-[calc(100vw-2rem)] max-w-6xl overflow-y-auto'>
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

        <Card>
          <CardContent className='p-8'>
            <ProjectTable
              projects={paginatedProjects}
              onEdit={handleEditProject}
              onUpdate={handleUpdateProject}
              onStatusChange={handleChangeProjectStatus}
              updatingProjectStatusId={updatingProjectStatusId}
            />
            <div className='mt-4'>
              <PaginationControls
                currentPage={projectPage}
                totalPages={projectTotalPages}
                totalItems={projectTotalItems}
                itemsPerPage={projectItemsPerPage}
                onPageChange={goToProjectPage}
                onItemsPerPageChange={setProjectItemsPerPage}
              />
            </div>
            <div className='mt-6 flex justify-center'>
              <Button
                onClick={() => navigate('/admin/project-register')}
                className='w-full max-w-lg bg-primary/100 hover:bg-primary/60'
              >
                <Plus className='h-4 w-4 mr-2' />
                プロジェクトを追加する
              </Button>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className='p-8'>
            <CompanyTable companies={[]} />
            <div className='mt-6 flex justify-center'></div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default AdminDashboard;
