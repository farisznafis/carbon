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

  if (loading || loadingStore) {
    return (
      <div className="text-small flex min-h-screen items-center justify-center bg-background px-6 text-center text-muted-foreground">
        読み込み中...
      </div>
    );
  }

  const companyName = user?.companyName ?? user?.siteName ?? '-';

  return (
    <div className="flex min-h-screen flex-col bg-card">
      <MasterDashboardHero
        companyName={companyName}
        action={
          <Button
            type="button"
            size="md"
            disabled={sortedSites.length === 0}
            onClick={() =>
              navigate(paths.projectMembership(sortedSites[0].siteId), {
                state: masterDashboardProjectMembershipState,
              })
            }
          >
            プロジェクト入会届
          </Button>
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
              onAdd={() => navigate(paths.masterUserRegister)}
              addDisabled={users.length > 49}
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
                      onEdit={(selectedUser) =>
                        navigate(paths.userEdit(selectedUser.id), {
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
              description="各店舗のカーボンクレジット、削減活動、レポート進捗を一か所で確認できます。"
              addLabel="拠点を追加"
              onAdd={() => navigate(paths.masterSiteRegister)}
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
                <DashboardEmptyState>
                  まだ拠点が登録されていません
                </DashboardEmptyState>
              )}
            </DashboardSection>
          </TabsContent>
        </main>
      </Tabs>
      <FloatingHelpButton context="master-dashboard" />
    </div>
  );
};

export default MasterDashboard;
