import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent } from '@/components/ui/tabs';
import { useAuth } from '@/contexts/AuthContext';
import {
  DashboardEmptyState,
  DashboardSection,
  MasterDashboardHero,
  MasterDashboardTabs,
  SiteDashboardCard,
  UserDashboardCard,
} from '@/features/master/components/MasterDashboardComponents';
import {
  sortMasterUsersFirst,
  sortSitesById,
} from '@/features/master/components/masterDashboardUtils';
import { masterDashboardProjectMembershipState } from '@/features/user/project-registration/projectMembershipNavigation';
import { paths } from '@/routes/paths';
import { useNotificationStore } from '@/stores/notificationStore';
import { useDashboardStore } from '@/stores/dashboardStore';
import { FloatingHelpButton } from '@/features/master/help/FloatingHelpButton';
import { PageLoadingOverlay } from '@/components/feedback/loading';
import { UserInvitationDialog } from '@/features/master/user-invitations/UserInvitationDialog';
import { Plus } from 'lucide-react';

const MasterDashboard = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { user } = useAuth();
  
  const activeTab = searchParams.get('tab') === 'users' ? 'users' : 'sites';

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

  if (loading || loadingStore) {
    // Skeleton dashboard non active first
    return <PageLoadingOverlay message="ダッシュボードを読み込んでいます" />;
  }

  const companyName = user?.companyName ?? user?.siteName ?? '-';

  return (
    <div className="flex min-h-screen flex-col bg-card">
      <MasterDashboardHero
        companyName={companyName}
        action={
          <div className="flex flex-col items-end gap-2">
            <Button
              type="button"
              size="md"
              disabled={!hasRegisteredSites}
              aria-describedby={
                hasRegisteredSites
                  ? undefined
                  : 'project-membership-disabled-description'
              }
              onClick={() =>
                navigate(paths.projectMembership(sortedSites[0].siteId), {
                  state: masterDashboardProjectMembershipState,
                })
              }
            >
              プロジェクト入会届
            </Button>
            {!hasRegisteredSites && (
              <p
                id="project-membership-disabled-description"
                className="text-small text-muted-foreground"
              >
                拠点登録後に利用できます。
              </p>
            )}
          </div>
        }
      />

      <Tabs 
        value={activeTab} 
        onValueChange={(value) => setSearchParams({ tab: value })}
        className="flex flex-1 flex-col bg-card"
      >
        <MasterDashboardTabs />

        <main className="flex-1 bg-card">
          <TabsContent value="users" className="m-0 animate-in fade-in-0 slide-in-from-bottom-4 duration-500">
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
          </TabsContent>

          <TabsContent value="sites" className="m-0 animate-in fade-in-0 slide-in-from-bottom-4 duration-500">
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
                  description="プロジェクト入会届を提出するには、対象となる拠点の登録が必要です。まず拠点を登録してください。"
                  action={
                    <Button
                      type="button"
                      size="md"
                      onClick={() => navigate(paths.masterSiteRegister)}
                    >
                      <Plus className="h-4 w-4" aria-hidden="true" />
                      拠点を追加
                    </Button>
                  }
                />
              )}
            </DashboardSection>
          </TabsContent>
        </main>
      </Tabs>
      <UserInvitationDialog
        open={isInvitationDialogOpen}
        onOpenChange={setIsInvitationDialogOpen}
      />
      <FloatingHelpButton context="master-dashboard" />
    </div>
  );
};

export default MasterDashboard;
