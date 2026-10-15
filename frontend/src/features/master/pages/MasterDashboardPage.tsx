import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  DashboardEmptyState,
  DashboardSection,
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
import { PageLoadingOverlay } from '@/components/feedback/loading';
import { UserInvitationDialog } from '@/features/master/user-invitations/UserInvitationDialog';
import { ActionButton } from '@/components/actions';
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
    return <PageLoadingOverlay message="ダッシュボードを読み込んでいます" />;
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
              <DashboardEmptyState
                title="まだ拠点が登録されていません"
                description="拠点登録後に利用できます。"
              />
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
              ) : sortedUsers.length > 0 ? (
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
                </div>
              ) : (
                <DashboardEmptyState>
                  まだ社員が登録されていません
                </DashboardEmptyState>
              )}
            </DashboardSection>
          </div>
        )}

        {activeTab === 'sites' && (
          <div className="animate-in fade-in-0 slide-in-from-bottom-4 duration-500">
            <DashboardSection
              title="拠点"
              description="各店舗のカーボンクレジット、削減活動、レポート進捗を一箇所で確認できます。"
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
                <DashboardEmptyState
                  title="まだ拠点が登録されていません"
                  description="プロジェクトに入会するには、対象となる拠点の登録が必要です。まず拠点を登録してください。"
                  action={
                    <ActionButton
                      kind="add"
                      onClick={() => navigate(paths.masterSiteRegister)}
                    >
                      拠点を追加
                    </ActionButton>
                  }
                />
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
