import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  DashboardEmptyState,
  DashboardSection,
  NoSiteEmptyState,
  SiteDashboardCard,
  UserDashboardCard,
} from '@/features/master/components/MasterDashboardComponents';
import { adminPageContainerClassName } from '@/components/layout/DashboardLayout';
import { getMasterDashboardTab } from '@/features/master/navigation/masterSidebarConfig';
import { cn } from '@/lib/utils';
import {
  sortMasterUsersFirst,
  sortSitesById,
} from '@/features/master/components/masterDashboardUtils';
import { ProjectMembershipContent } from '@/features/user/project-registration/ProjectMembershipPage';
import { paths } from '@/routes/paths';
import { useNotificationStore } from '@/stores/notificationStore';
import { useDashboardStore } from '@/stores/dashboardStore';
import { PageSkeleton, Skeleton, SkeletonList } from '@/components/feedback/loading';
import { UserInvitationDialog } from '@/features/master/user-invitations/UserInvitationDialog';
import { MasterHomeSection } from '@/features/master/home/MasterHomeSection';
import { MasterAnalyticsSection } from '@/features/master/analytics/MasterAnalyticsSection';

/** Displays one Master dashboard destination selected through the URL. */
const MasterDashboard = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const activeTab = getMasterDashboardTab(searchParams.get('tab'));

  const sites = useDashboardStore((state) => state.sites);
  const users = useDashboardStore((state) => state.users);
  const loadingStore = useDashboardStore((state) => state.loading);
  const fetchDashboardData = useDashboardStore(
    (state) => state.fetchDashboardData,
  );
  const loadNotifications = useNotificationStore(
    (state) => state.loadNotifications,
  );

  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isInvitationDialogOpen, setIsInvitationDialogOpen] = useState(false);

  useEffect(() => {
    let isMounted = true;

    async function loadData() {
      setLoading(true);
      setErrorMessage(null);
      void loadNotifications();

      try {
        await fetchDashboardData(true);
      } catch (error) {
        if (isMounted) {
          setErrorMessage(
            error instanceof Error
              ? error.message
              : 'ダッシュボードデータの取得に失敗しました',
          );
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    void loadData();

    return () => {
      isMounted = false;
    };
  }, [fetchDashboardData, loadNotifications]);

  const sortedUsers = useMemo(() => sortMasterUsersFirst(users), [users]);
  const sortedSites = useMemo(() => sortSitesById(sites), [sites]);
  const hasRegisteredSites = sortedSites.length > 0;

  if ((activeTab === 'sites' || activeTab === 'users' || activeTab === 'membership') && (loading || loadingStore)) {
    return (
      <PageSkeleton
        label="ダッシュボードを読み込んでいます"
        className={cn(adminPageContainerClassName, 'py-page-y')}
      >
        <div className="space-y-5">
          <Skeleton className="h-7 w-32" />
          <SkeletonList items={3} />
        </div>
      </PageSkeleton>
    );
  }

  return (
    <div className="flex min-h-[calc(100dvh-var(--app-header-height))] flex-col bg-background">
      <main className={cn(adminPageContainerClassName, 'flex-1 py-page-y')}>
        {activeTab === 'home' && <MasterHomeSection />}
        {activeTab === 'analytics' && <MasterAnalyticsSection />}
        {activeTab === 'membership' && (
          errorMessage ? (
            <DashboardEmptyState>{errorMessage}</DashboardEmptyState>
          ) : hasRegisteredSites ? (
            <ProjectMembershipContent
              siteId={sortedSites[0].siteId}
              embedded
            />
          ) : (
            <section className="space-y-5">
              <h2 className="text-foreground">入会</h2>
              <NoSiteEmptyState description="プロジェクトに入会するには、対象となる拠点の登録が必要です。まず拠点を登録してください。" />
            </section>
          )
        )}
        {activeTab === 'users' && (
          <div className="animate-in fade-in-0 slide-in-from-bottom-4 duration-500">
            <DashboardSection
              title="社員"
              description="行をクリックすると、プロファイル詳細へ移動できます。"
              addLabel="社員を追加"
              onAdd={() => setIsInvitationDialogOpen(true)}
            >
              {errorMessage ? (
                <DashboardEmptyState>{errorMessage}</DashboardEmptyState>
              ) : (
                <div className="space-y-3">
                  {sortedUsers.map((dashboardUser) => (
                    <UserDashboardCard
                      key={dashboardUser.id}
                      user={dashboardUser}
                      onProfile={(selectedUser) =>
                        navigate(paths.masterUserProfile(selectedUser.id), {
                          state: { user: selectedUser },
                        })
                      }
                    />
                  ))}
                  {/* The master account always exists, so "empty" means no employee yet. */}
                  {sortedUsers.some((dashboardUser) => dashboardUser.role !== 'master') ? null : (
                    <DashboardEmptyState
                      title="まだ社員が登録されていません"
                      description="「社員を追加」から招待すると、社員がここに表示されます。"
                    />
                  )}
                </div>
              )}
            </DashboardSection>
          </div>
        )}

        {activeTab === 'sites' && (
          <div className="animate-in fade-in-0 slide-in-from-bottom-4 duration-500">
            <DashboardSection
              title="拠点"
              description="各拠点のカーボンクレジット、削減活動、レポート進捗を一箇所で確認できます。"
              addLabel="拠点を追加"
              onAdd={() => navigate(paths.masterSiteRegister)}
              showAddAction={hasRegisteredSites || Boolean(errorMessage)}
            >
              {errorMessage ? (
                <DashboardEmptyState>{errorMessage}</DashboardEmptyState>
              ) : sortedSites.length > 0 ? (
                <div className="space-y-3">
                  {sortedSites.map((site) => (
                    <SiteDashboardCard
                      key={site.siteId}
                      site={site}
                      onOpen={(selectedSite) =>
                        navigate(paths.siteDashboard(selectedSite.siteId))
                      }
                    />
                  ))}
                </div>
              ) : (
                <NoSiteEmptyState description="プロジェクトに入会するには、対象となる拠点の登録が必要です。まず拠点を登録してください。" />
              )}
            </DashboardSection>
          </div>
        )}
      </main>
      <UserInvitationDialog
        open={isInvitationDialogOpen}
        onOpenChange={setIsInvitationDialogOpen}
      />
    </div>
  );
};

export default MasterDashboard;
