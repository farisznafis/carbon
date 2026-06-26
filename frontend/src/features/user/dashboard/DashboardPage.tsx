import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { InlineHelp } from '@/components/feedback/InlineHelp/InlineHelp';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { ScrollArea } from '@/components/ui/scroll-area';
import { getSiteData, getSiteProjects, type SiteProject } from '@/api/site';
import { useToast } from '@/hooks/use-toast';
import { useDashboardStore } from '@/stores/dashboardStore';
import { useNotificationStore } from '@/stores/notificationStore';

const Dashboard = () => {
  const { siteId } = useParams();
  const navigate = useNavigate();
  const setCurrentProject = useDashboardStore(
    (state) => state.setCurrentProject,
  );
  const loadNotifications = useNotificationStore(
    (state) => state.loadNotifications,
  );
  const { user } = useAuth();
  const { toast } = useToast();

  const [isLoading, setIsLoading] = useState(true);
  const [site, setSite] = useState<{
    id: string;
    siteName: string;
    picFirstName: string;
    picLastName: string;
    address?: string;
  } | null>(null);
  const [projects, setProjects] = useState<SiteProject[]>([]);

  useEffect(() => {
    if (!user) {
      navigate('/login');
      return;
    }

    if (!siteId) {
      toast({
        title: 'エラー',
        description: 'siteId が見つかりません',
        variant: 'destructive',
      });
      return;
    }

    let isMounted = true;
    void loadNotifications();

    const loadDashboardData = async () => {
      try {
        setIsLoading(true);

        const [siteResponse, projectData] = await Promise.all([
          getSiteData(siteId),
          getSiteProjects(siteId),
        ]);

        if (!isMounted) return;

        setSite(siteResponse.site);
        setProjects(projectData);
      } catch (error) {
        toast({
          title: 'エラー',
          description:
            error instanceof Error
              ? error.message
              : 'データの読み込みに失敗しました',
          variant: 'destructive',
        });
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    void loadDashboardData();

    return () => {
      isMounted = false;
    };
  }, [toast, user, siteId, navigate, loadNotifications]);

  useEffect(() => {
    setCurrentProject(null);
  }, [setCurrentProject]);

  const handleClick = (project: SiteProject) => {
    setCurrentProject(project);
    navigate(`/projects/${project.id}/register`, { state: { siteId } });
  };

  return (
    <div className='min-h-screen py-8 bg-slate-50'>
      <div className='container mx-auto px-4'>
        {user?.role === 'master' && (
          <Button
            variant='ghost'
            onClick={() => navigate('/master/dashboard')}
            className='mb-4'
          >
            <ArrowLeft className='h-4 w-4 mr-2' />
            マスターダッシュボードに戻る
          </Button>
        )}

        <div className='mb-8 mx-8 animate-slide-up'>
          <div className='space-y-1'>
            <h2 className='mb-2'>{site?.siteName || '拠点ダッシュボード'}</h2>
            <div className='space-y-1 text-sm text-muted-foreground'>
              <p>
                    担当者:{' '}
              {`${site?.picLastName ?? ''} ${site?.picFirstName ?? ''}`.trim() ||
                '-'}
              </p>
                  <p>住所: {site?.address || '-'}</p>
          </div>
        </div>

        </div>
        <div className='gap-6'>
          <div className='space-y-6'>
            <Card>
              <CardHeader className='flex flex-row gap-2'>
                <CardTitle className='text-3xl font-bold'>
                  プロジェクト
                </CardTitle>
                <InlineHelp />
              </CardHeader>
              <CardContent>
                <ScrollArea className='max-h-[600px] pr-4 overflow-auto'>
                  <div className='space-y-4'>
                    {isLoading ? (
                      <div className='text-center py-8 text-muted-foreground'>
                        読み込み中...
                      </div>
                    ) : projects.length === 0 ? (
                      <div className='text-center py-8 text-muted-foreground'>
                        プロジェクトが見つかりません
                      </div>
                    ) : (
                      projects.map((project) => (
                        <Card
                          key={project.id}
                          className='transition-all animate-slide-up'
                        >
                          <div className='grid grid-cols-1 lg:grid-cols-3 gap-3 p-4'>
                            <div className='lg:col-span-2 lg:row-span-2'>
                              <h3 className='text-lg font-semibold mb-1'>
                                {project.projectName}
                              </h3>
                              <div className='w-full h-32 flex items-center justify-center bg-muted/30 rounded-lg'>
                                <span className='text-muted-foreground text-sm'>
                                  グラフエリア
                                </span>
                              </div>
                            </div>

                            <div className='p-3 border rounded-md grid grid-cols-2 gap-y-1 gap-x-0 text-sm'>
                              <span className='text-muted-foreground'>方法論</span>
                              <span>{project.methodName}</span>
                              <span className='text-muted-foreground'>方法論番号:</span>
                              <span>
                                {project.methodNumber} {project.methodologyVersion}
                              </span>
                              <span className='text-muted-foreground'>ステータス:</span>
                              <span>{project.isJoined ? '参加中' : '未参加'}</span>
                            </div>

                            <div className='flex items-center gap-2'>
                              {project.isJoined ? (
                                <Button
                                  onClick={() =>
                                    navigate(
                                      `/sites/${encodeURIComponent(siteId)}/projects/${project.id}`,
                                    )
                                  }
                                  className='w-full h-10 bg-muted text-foreground hover:bg-muted/80'
                                  size='lg'
                                >
                                  詳細
                                </Button>
                              ) : (
                                <Button
                                  onClick={() => handleClick(project)}
                                  className='w-full h-10 bg-primary text-primary-foreground hover:bg-primary/90'
                                  size='lg'
                                >
                                  参加
                                </Button>
                              )}
                            </div>
                          </div>
                        </Card>
                      ))
                    )}
                  </div>
                </ScrollArea>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
