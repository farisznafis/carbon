import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Plus } from 'lucide-react';
import { useDashboardStore } from '@/stores/dashboardStore';
import type { Site } from '@/api/site';
import type { UserList } from '@/api/users';
import { Card, CardContent } from '@/components/ui/card';
import { SiteTable } from '@/components/data-display/tables/SiteTable';
import { UserTable } from '@/components/data-display/tables/UserTable';
import { useNotificationStore } from '@/stores/notificationStore';
import { paths } from '@/routes/paths';

const MasterDashboard = () => {
  const navigate = useNavigate();

  const sites = useDashboardStore((state) => state.sites) ?? [];
  const users = useDashboardStore((state) => state.users) ?? [];
  const loadingStore = useDashboardStore((state) => state.loading);
  const fetchDashboardData = useDashboardStore(
    (state) => state.fetchDashboardData,
  );
  const loadNotifications = useNotificationStore((state) => state.loadNotifications);

  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    async function loadData() {
      setLoading(true);
      void loadNotifications();
      await fetchDashboardData(true);

      if (isMounted) {
        setLoading(false);
      }
    }

    void loadData();

    return () => {
      isMounted = false;
    };
  }, [fetchDashboardData, loadNotifications]);

  if (loading || loadingStore) {
    return (
      <div className='container mx-auto p-6 text-center'>
        読み込み中...
      </div>
    );
  }

  const handleRegisterSite = () => {
    navigate('/master/site-register');
  };

  const handleEditSite = (site: Site) => {
    navigate(`/master/site-edit/${site.id}`, { state: { site } });
  };

  const handleEditUser = (user: UserList) => {
    navigate(`/user-edit/${user.id}`, { state: { user } });
  };

  const companyName = '株式会社サンプル';

  return (
    <div className='min-h-screen bg-slate-50'>
      <div className='container mx-auto p-6 space-y-6 w-full'>
        <div className='flex'>
          <div className='flex flex-col py-8 px-8'>
            <h3 className='font-bold'>{companyName}</h3>
          </div>
        </div>

        <Card>
          <CardContent className='p-8'>
            <SiteTable
              sites={sites}
              onEdit={handleEditSite}
              onRowClick={(site) => navigate(`/sites/${site.id}/dashboard`)}
            />
            <div className='mt-6 flex justify-center'>
              <Button
                onClick={handleRegisterSite}
                className='w-full max-w-lg bg-primary/100 hover:bg-primary/60'
              >
                <Plus className='h-4 w-4 mr-2' />
                拠点を追加
              </Button>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className='p-8'>
            <UserTable
              users={users}
              onEdit={handleEditUser}
              onRowClick={(user) =>
                navigate(paths.masterUserProfile(user.id), { state: { user } })
              }
            />
            <div className='mt-6 flex justify-center'>
              <Button
                onClick={() => navigate('/master/user-register')}
                disabled={sites.length < users.length}
                className='w-full max-w-lg bg-primary/100 hover:bg-primary/60'
              >
                <Plus className='h-4 w-4 mr-2' />
                ユーザーを追加
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default MasterDashboard;
