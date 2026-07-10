import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Building2, Factory, Leaf } from 'lucide-react';
import { getProfile } from '@/api/profile';
import {
  getSiteData,
  getSiteProjects,
  type SiteDashboardMetrics,
  type SiteProject,
} from '@/api/site';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/hooks/use-toast';
import { storage, storageKeys } from '@/lib/storage';
import { cn } from '@/lib/utils';
import { useDashboardStore } from '@/stores/dashboardStore';
import { useNotificationStore } from '@/stores/notificationStore';

const pageContainer = 'mx-auto w-full max-w-[1200px] px-5 lg:px-8 xl:px-0';

function formatNumber(value?: string | number | null) {
  if (value === null || value === undefined || value === '') return '-';

  const parsed = typeof value === 'number' ? value : Number(value);
  if (Number.isNaN(parsed)) return String(value);

  return new Intl.NumberFormat('ja-JP').format(parsed);
}

function formatProjectDate(project: SiteProject) {
  const value = project.updatedAt || project.createdAt;
  if (!value) return '2026/01/01';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return `${date.getFullYear()}/${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`;
}

function getMethodologyLabel(project: SiteProject) {
  const parts = [
    project.methodNumber,
    project.methodologyVersion,
    project.methodName ? `(${project.methodName})` : '',
  ].filter(Boolean);

  return parts.join(' ') || '-';
}

function DummyAreaChart({ muted = false }: { muted?: boolean }) {
  return (
    <svg viewBox="0 0 420 190" className="h-full w-full" aria-hidden="true">
      <defs>
        <linearGradient id={muted ? 'area-muted' : 'area-primary'} x1="0" x2="0" y1="0" y2="1">
          <stop offset="0%" stopColor="hsl(var(--primary))" stopOpacity={muted ? 0.18 : 0.32} />
          <stop offset="100%" stopColor="hsl(var(--primary))" stopOpacity={0.04} />
        </linearGradient>
      </defs>
      {[40, 90, 140].map((y) => (
        <line
          key={y}
          x1="20"
          x2="400"
          y1={y}
          y2={y}
          stroke="hsl(var(--border))"
          strokeDasharray="8 8"
        />
      ))}
      <path
        d="M20 150 L120 142 L210 80 L300 132 L400 92 L400 170 L20 170 Z"
        fill={`url(#${muted ? 'area-muted' : 'area-primary'})`}
      />
      <path
        d="M20 150 L120 142 L210 80 L300 132 L400 92"
        fill="none"
        stroke="hsl(var(--primary))"
        strokeWidth="2"
      />
      <path
        d="M20 82 L150 56 L240 138 L400 82"
        fill="none"
        stroke="hsl(var(--foreground))"
        strokeOpacity="0.35"
        strokeWidth="2"
      />
      <line x1="20" x2="400" y1="170" y2="170" stroke="hsl(var(--border))" strokeWidth="2" />
      {['2013', '2014', '2015', '2016'].map((label, index) => (
        <text
          key={label}
          x={55 + index * 105}
          y="188"
          fill="hsl(var(--muted-foreground))"
          fontSize="14"
          textAnchor="middle"
        >
          {label}
        </text>
      ))}
    </svg>
  );
}

function DummyBarChart() {
  const bars = [44, 64, 86, 54, 118, 150, 122, 82, 116, 116, 78];

  return (
    <svg viewBox="0 0 420 190" className="h-full w-full" aria-hidden="true">
      {[45, 105].map((y) => (
        <line
          key={y}
          x1="18"
          x2="392"
          y1={y}
          y2={y}
          stroke="hsl(var(--border))"
          strokeDasharray="8 8"
        />
      ))}
      {bars.map((height, index) => {
        const isCurrent = index === bars.length - 1;
        const x = 22 + index * 34;
        const y = 164 - height;

        return (
          <rect
            key={`${height}-${index}`}
            x={x}
            y={y}
            width="28"
            height={height}
            rx="14"
            fill={isCurrent ? 'hsl(var(--primary))' : 'hsl(var(--secondary))'}
          />
        );
      })}
      {['1月', '2月', '3月', '4月', '5月'].map((label, index) => (
        <text
          key={label}
          x={34 + index * 86}
          y="188"
          fill="hsl(var(--muted-foreground))"
          fontSize="14"
          textAnchor="middle"
        >
          {label}
        </text>
      ))}
      <text x="386" y="30" fill="hsl(var(--muted-foreground))" fontSize="13">
        ベース
      </text>
    </svg>
  );
}

function KpiCard({
  title,
  value,
  unit = 'k/CO2',
  variant = 'default',
}: {
  title: string;
  value: string;
  unit?: string;
  variant?: 'default' | 'primary' | 'line';
}) {
  return (
    <Card
      className={cn(
        'relative h-full min-h-[250px] overflow-hidden rounded-2xl border-border shadow-app-sm',
        variant === 'primary'
          ? 'border-primary bg-primary text-primary-foreground'
          : 'bg-card text-foreground',
      )}
    >
      <CardContent className="relative z-10 p-5">
        <p className={cn('text-small', variant === 'primary' ? 'text-white/85' : 'text-muted-foreground')}>
          {title}
        </p>
        <p className="mt-2 text-[34px] font-normal leading-10 tracking-normal">
          {value}
        </p>
        <p className={cn('text-subtle', variant === 'primary' ? 'text-white/80' : 'text-muted-foreground')}>
          {unit}
        </p>
      </CardContent>

      {variant === 'primary' ? (
        <>
          <Leaf className="absolute right-7 top-5 h-24 w-24 text-white/20" strokeWidth={1.5} />
          <div className="absolute -bottom-8 left-0 h-20 w-full bg-secondary/45 [clip-path:polygon(0_70%,38%_65%,74%_0,100%_35%,100%_100%,0_100%)]" />
        </>
      ) : variant === 'line' ? (
        <svg viewBox="0 0 320 132" className="absolute bottom-0 left-0 h-28 w-full" aria-hidden="true">
          <defs>
            <linearGradient id="kpi-line-fill" x1="0" x2="0" y1="0" y2="1">
              <stop offset="0%" stopColor="hsl(var(--primary))" stopOpacity="0.22" />
              <stop offset="100%" stopColor="hsl(var(--primary))" stopOpacity="0.04" />
            </linearGradient>
          </defs>
          <path d="M0 98 L120 90 L205 28 L320 78 L320 132 L0 132 Z" fill="url(#kpi-line-fill)" />
          <path d="M0 98 L120 90 L205 28 L320 78" fill="none" stroke="hsl(var(--primary))" strokeWidth="2" />
        </svg>
      ) : (
        <div className="absolute -bottom-8 left-0 h-28 w-full bg-muted/45 [clip-path:polygon(0_70%,36%_52%,72%_90%,100%_62%,100%_100%,0_100%)]" />
      )}
    </Card>
  );
}

function ProjectStatus({ joined }: { joined: boolean }) {
  return (
    <span className="inline-flex items-center gap-2 font-bold">
      <span
        className={cn(
          'h-3 w-3 rounded-full',
          joined ? 'bg-green-500' : 'bg-amber-500',
        )}
      />
      {joined ? '実施中' : '非参加'}
    </span>
  );
}

function JoinedProjectCard({
  project,
  onDetail,
}: {
  project: SiteProject;
  onDetail: () => void;
}) {
  return (
    <Card className="rounded-2xl border-border bg-card shadow-none">
      <CardContent className="p-5 md:p-6">
        <div className="mb-5 flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-full bg-secondary text-primary">
            <Building2 className="h-5 w-5" />
          </span>
          <h3 className="text-foreground">{project.projectName}</h3>
        </div>

        <div className="grid gap-6 xl:grid-cols-[400px_minmax(0,1fr)]">
          <div className="space-y-4">
            <div className="rounded-2xl bg-muted/30 p-5">
              <dl className="space-y-4">
                <div>
                  <dt className="text-small text-muted-foreground">方法論</dt>
                  <dd className="mt-1 font-bold">{getMethodologyLabel(project)}</dd>
                </div>
                <div>
                  <dt className="text-small text-muted-foreground">方法論番号</dt>
                  <dd className="mt-1 font-bold">{project.methodNumber} {project.methodologyVersion}</dd>
                </div>
                <div>
                  <dt className="text-small text-muted-foreground">ステータス</dt>
                  <dd className="mt-1">
                    <ProjectStatus joined />
                  </dd>
                </div>
              </dl>
            </div>

            <Button type="button" size="md" className="w-full text-base" onClick={onDetail}>
              プロジェクト詳細
            </Button>
          </div>

          <div className="flex min-h-[220px] items-center justify-center rounded-2xl bg-muted/20 text-muted-foreground">
            グラフデータ準備中
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

function AvailableProjectCard({
  project,
  onJoin,
}: {
  project: SiteProject;
  onJoin: () => void;
}) {
  return (
    <Card className="rounded-2xl border-border bg-card shadow-none">
      <CardContent className="flex h-full min-h-[300px] flex-col p-6">
        <h3 className="text-foreground">{project.projectName || 'PROJECT ABC'}</h3>

        <dl className="mt-6 space-y-4">
          <div>
            <dt className="text-small text-muted-foreground">方法論</dt>
            <dd className="mt-1 font-bold">{getMethodologyLabel(project)}</dd>
          </div>
          <div>
            <dt className="text-small text-muted-foreground">最新更新日</dt>
            <dd className="mt-1 font-bold">{formatProjectDate(project)}</dd>
          </div>
          <div>
            <dt className="text-small text-muted-foreground">ステータス</dt>
            <dd className="mt-1">
              <ProjectStatus joined={false} />
            </dd>
          </div>
        </dl>

        <Button
          type="button"
          variant="outline"
          size="md"
          className="mt-auto w-full border-primary text-primary hover:bg-secondary/60"
          onClick={onJoin}
        >
          参加する
        </Button>
      </CardContent>
    </Card>
  );
}

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
  const [refreshedSiteId, setRefreshedSiteId] = useState('');
  const [isRefreshingUserSite, setIsRefreshingUserSite] = useState(false);
  const userSiteId = user?.siteId ? String(user.siteId).trim() : '';
  const activeSiteId = siteId || userSiteId || refreshedSiteId;
  const isUserWithoutSite =
    user?.role !== 'master' &&
    user?.role !== 'admin' &&
    !userSiteId &&
    !refreshedSiteId;

  const [isLoading, setIsLoading] = useState(true);
  const [site, setSite] = useState<{
    id: string;
    siteName: string;
    picFirstName: string;
    picLastName: string;
    address?: string;
  } | null>(null);
  const [metrics, setMetrics] = useState<SiteDashboardMetrics | null>(null);
  const [projects, setProjects] = useState<SiteProject[]>([]);

  const joinedProjects = useMemo(
    () => projects.filter((project) => project.isJoined),
    [projects],
  );
  const availableProjects = useMemo(
    () => projects.filter((project) => !project.isJoined),
    [projects],
  );

  useEffect(() => {
    if (!user || user.role === 'master' || user.role === 'admin' || userSiteId) {
      return;
    }

    let isMounted = true;

    const refreshUserSite = async () => {
      setIsRefreshingUserSite(true);

      try {
        const profile = await getProfile();
        const latestSiteId = profile.siteId?.trim() ?? '';

        if (!isMounted || !latestSiteId) return;

        const nextUser = {
          ...user,
          siteId: latestSiteId,
          siteName: profile.siteName ?? user.siteName,
        };

        storage.setJson(storageKeys.user, nextUser);
        storage.set(storageKeys.siteId, latestSiteId);
        setRefreshedSiteId(latestSiteId);
      } catch {
        // Keep the existing no-site message when the profile refresh cannot resolve a site.
      } finally {
        if (isMounted) {
          setIsRefreshingUserSite(false);
        }
      }
    };

    void refreshUserSite();

    return () => {
      isMounted = false;
    };
  }, [user, userSiteId]);

  useEffect(() => {
    if (!user) {
      navigate('/login');
      return;
    }

    if (isUserWithoutSite) {
      setIsLoading(false);
      return;
    }

    if (!activeSiteId) {
      setIsLoading(false);
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
          getSiteData(activeSiteId),
          getSiteProjects(activeSiteId),
        ]);

        if (!isMounted) return;

        setSite(siteResponse.site);
        setMetrics(siteResponse.metrics);
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
  }, [toast, user, activeSiteId, navigate, loadNotifications, isUserWithoutSite]);

  useEffect(() => {
    setCurrentProject(null);
  }, [setCurrentProject]);

  const handleJoinProject = (project: SiteProject) => {
    setCurrentProject(project);
    navigate(`/projects/${project.id}/register`, {
      state: { siteId: activeSiteId },
    });
  };

  const handleOpenProject = (project: SiteProject) => {
    navigate(`/sites/${encodeURIComponent(activeSiteId)}/projects/${project.id}`);
  };

  const picName =
    `${site?.picLastName ?? ''} ${site?.picFirstName ?? ''}`.trim() || '-';
  const address = site?.address || '-';

  if (isRefreshingUserSite) {
    return (
      <div className="min-h-screen bg-background py-8">
        <div className={pageContainer}>
          <div className="py-16 text-center text-muted-foreground">
            読み込み中...
          </div>
        </div>
      </div>
    );
  }

  if (isUserWithoutSite) {
    return (
      <div className="min-h-screen bg-background py-8">
        <div className={pageContainer}>
          <Card className="mx-auto mt-16 max-w-2xl">
            <CardContent className="p-8 text-center text-muted-foreground">
              所属している拠点がありません。マスターにお問い合わせください。
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div className="relative min-h-screen bg-background">
      {user?.role === 'master' && (
        <PageBackButton
          onClick={() => navigate('/master/dashboard?tab=sites')}
          label=""
          className="absolute left-4 top-10 z-10 h-12 w-12 rounded-full p-0 sm:left-10 lg:left-16"
        />
      )}

      <section className={cn('relative pb-12 pt-14', pageContainer)}>
        <div className="grid items-stretch gap-6 ">
          <div className="relative">
            <div>
              <p className="text-small pl-[60px] text-muted-foreground">
                {user?.companyName || '株式会社ABC'}
              </p>
              <div className="flex items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                  <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-secondary text-primary">
                    <Factory className="h-6 w-6" />
                  </span>
                  <h1 className="mt-1 text-[36px] leading-10 text-foreground">{site?.siteName || '拠点ダッシュボード'}</h1>
                </div>
                {user?.role === 'master' && activeSiteId && (
                  <Button
                    type="button"
                    variant="outline"
                    size="md"
                    onClick={() => navigate(`/master/site-edit/${activeSiteId}`)}
                  >
                    拠点情報を編集
                  </Button>
                )}
              </div>
              <div className="ml-[60px] mt-5 grid gap-4 sm:grid-cols-[minmax(0,1fr)_auto] lg:grid-cols-1">
              {/* <dl className="space-y-4 rounded-2xl bg-card/70 p-5 shadow-app-sm"> */}
                {/* <div>
                  <dt className="text-small text-muted-foreground">プロジェクト数：</dt>
                  <dd className="mt-1 font-bold">{joinedProjects.length}</dd>
                </div> */}
              <div>
                <dt className="text-small text-muted-foreground">担当者</dt>
                <dd className="mt-1 font-bold">{picName}</dd>
              </div>
              <div>
                <dt className="text-small text-muted-foreground">所在地</dt>
                <dd className="mt-1 font-bold">{address}</dd>
              </div>
              {/* </dl> */}
              </div>
            </div>
          </div>

          {/* <div className="grid h-full gap-3 md:grid-cols-2">
            <KpiCard
              title="排出総量"
              value={formatNumber(metrics?.sumEmission)}
              variant="primary"
            />
            <KpiCard
              title="排出削減総量"
              value={formatNumber(metrics?.sumEmissionReduction)}
            />
          </div> */}
        </div>
      </section>

      <section className="bg-card py-12">
        <div className={pageContainer}>
          <div className="mb-6">
            <h2 className="text-foreground">参加中のプロジェクト</h2>
            <p className="text-small mt-2 text-foreground">
              この拠点が現在参加しているプロジェクトです。進捗や排出量の詳細を確認できます。
            </p>
          </div>

          {isLoading ? (
            <Card className="rounded-2xl border-border">
              <CardContent className="p-10 text-center text-muted-foreground">
                読み込み中...
              </CardContent>
            </Card>
          ) : joinedProjects.length > 0 ? (
            <div className="space-y-4">
              {joinedProjects.map((project) => (
                <JoinedProjectCard
                  key={project.id}
                  project={project}
                  onDetail={() => handleOpenProject(project)}
                />
              ))}
            </div>
          ) : (
            <Card className="rounded-2xl border-border">
              <CardContent className="p-10 text-center text-muted-foreground">
                参加中のプロジェクトがありません
              </CardContent>
            </Card>
          )}
        </div>
      </section>

      <section className="bg-background py-12">
        <div className={pageContainer}>
          <div className="mb-6">
            <h2 className="text-foreground">非参加プロジェクト</h2>
            <p className="text-small mt-2 text-foreground">
              この拠点は以下のプロジェクトにまだ参加していません。参加するには参加手続きを行ってください。
            </p>
          </div>

          {isLoading ? (
            <Card className="rounded-2xl border-border">
              <CardContent className="p-10 text-center text-muted-foreground">
                読み込み中...
              </CardContent>
            </Card>
          ) : availableProjects.length > 0 ? (
            <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
              {availableProjects.map((project) => (
                <AvailableProjectCard
                  key={project.id}
                  project={project}
                  onJoin={() => handleJoinProject(project)}
                />
              ))}
            </div>
          ) : (
            <Card className="rounded-2xl border-border">
              <CardContent className="p-10 text-center text-muted-foreground">
                非参加のプロジェクトがありません
              </CardContent>
            </Card>
          )}
        </div>
      </section>
    </div>
  );
};

export default Dashboard;
