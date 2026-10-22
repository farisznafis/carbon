import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Building2, ChevronRight, Factory, Leaf } from 'lucide-react';
import { getProfile } from '@/api/profile';
import {
  getSiteData,
  getSiteProjects,
  joinProjectSite,
  type Site,
  type SiteProject,
  type SiteProjectParticipationStatus,
} from '@/api/site';
import {
  InlineLoader,
  PageSkeleton,
  SkeletonCard,
} from '@/components/feedback/loading';
import { usePageBreadcrumb } from '@/components/navigation/pageBreadcrumbContext';
import {
  dashboardPageContainerClassName,
  dashboardPageHeaderClassName,
} from '@/components/layout/DashboardLayout';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/hooks/use-toast';
import { storage, storageKeys } from '@/lib/storage';
import { cn } from '@/lib/utils';
import { paths } from '@/routes/paths';
import { useDashboardStore } from '@/stores/dashboardStore';
import { useNotificationStore } from '@/stores/notificationStore';
import { DashboardEmissionReductionProgress } from './components/DashboardEmissionReductionProgress';

const pageContainer = dashboardPageContainerClassName;
const PARTICIPATION_STATUS_ORDER: Record<
  SiteProjectParticipationStatus,
  number
> = {
  APPROVED: 0,
  REJECTED: 1,
  OPEN: 2,
};

/** Sorts projects by membership UX priority without mutating API data. */
function sortProjectsByParticipationStatus(
  projects: SiteProject[],
): SiteProject[] {
  return [...projects].sort(
    (firstProject, secondProject) =>
      (PARTICIPATION_STATUS_ORDER[firstProject.participationStatus ?? ''] ??
        Number.MAX_SAFE_INTEGER) -
      (PARTICIPATION_STATUS_ORDER[secondProject.participationStatus ?? ''] ??
        Number.MAX_SAFE_INTEGER),
  );
}

function formatNumber(value?: string | number | null) {
  if (value === null || value === undefined || value === '') return '-';

  const parsed = typeof value === 'number' ? value : Number(value);
  if (Number.isNaN(parsed)) return String(value);

  return new Intl.NumberFormat('ja-JP').format(parsed);
}

function formatProjectDate(project: SiteProject) {
  const value = project.updatedAt || project.createdAt;
  if (!value) return '-';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`;
}

function getMethodologyLabel(project: SiteProject) {
  const parts = [
    // project.methodNumber,
    // project.methodologyVersion,
    project.methodName ? `${project.methodName}` : '',
  ].filter(Boolean);

  return parts.join(' ') || '-';
}

function DummyAreaChart({ muted = false }: { muted?: boolean }) {
  return (
    <svg
      viewBox='0 0 420 190'
      className='h-full w-full'
      aria-hidden='true'
    >
      <defs>
        <linearGradient
          id={muted ? 'area-muted' : 'area-primary'}
          x1='0'
          x2='0'
          y1='0'
          y2='1'
        >
          <stop
            offset='0%'
            stopColor='hsl(var(--primary))'
            stopOpacity={muted ? 0.18 : 0.32}
          />
          <stop
            offset='100%'
            stopColor='hsl(var(--primary))'
            stopOpacity={0.04}
          />
        </linearGradient>
      </defs>
      {[40, 90, 140].map((y) => (
        <line
          key={y}
          x1='20'
          x2='400'
          y1={y}
          y2={y}
          stroke='hsl(var(--border))'
          strokeDasharray='8 8'
        />
      ))}
      <path
        d='M20 150 L120 142 L210 80 L300 132 L400 92 L400 170 L20 170 Z'
        fill={`url(#${muted ? 'area-muted' : 'area-primary'})`}
      />
      <path
        d='M20 150 L120 142 L210 80 L300 132 L400 92'
        fill='none'
        stroke='hsl(var(--primary))'
        strokeWidth='2'
      />
      <path
        d='M20 82 L150 56 L240 138 L400 82'
        fill='none'
        stroke='hsl(var(--foreground))'
        strokeOpacity='0.35'
        strokeWidth='2'
      />
      <line
        x1='20'
        x2='400'
        y1='170'
        y2='170'
        stroke='hsl(var(--border))'
        strokeWidth='2'
      />
      {['2013', '2014', '2015', '2016'].map((label, index) => (
        <text
          key={label}
          x={55 + index * 105}
          y='188'
          fill='hsl(var(--muted-foreground))'
          fontSize='14'
          textAnchor='middle'
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
    <svg
      viewBox='0 0 420 190'
      className='h-full w-full'
      aria-hidden='true'
    >
      {[45, 105].map((y) => (
        <line
          key={y}
          x1='18'
          x2='392'
          y1={y}
          y2={y}
          stroke='hsl(var(--border))'
          strokeDasharray='8 8'
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
            width='28'
            height={height}
            rx='14'
            fill={isCurrent ? 'hsl(var(--primary))' : 'hsl(var(--secondary))'}
          />
        );
      })}
      {['1月', '2月', '3月', '4月', '5月'].map((label, index) => (
        <text
          key={label}
          x={34 + index * 86}
          y='188'
          fill='hsl(var(--muted-foreground))'
          fontSize='14'
          textAnchor='middle'
        >
          {label}
        </text>
      ))}
      <text
        x='386'
        y='30'
        fill='hsl(var(--muted-foreground))'
        fontSize='13'
      >
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
        'relative h-full min-h-[200px] overflow-hidden rounded-lg border-border',
        variant === 'primary'
          ? 'border-primary-strong bg-primary-strong text-primary-foreground'
          : 'bg-card text-foreground',
      )}
    >
      <CardContent className='relative z-10 p-4'>
        <p
          className={cn(
            'text-small',
            variant === 'primary' ? 'text-primary-foreground' : 'text-foreground',
          )}
        >
          {title}
        </p>
        <p className='mt-2 text-[1.75rem] font-normal leading-9 tracking-normal'>
          {value}
        </p>
        <p
          className={cn(
            'text-subtle',
            variant === 'primary' ? 'text-primary-foreground' : 'text-foreground',
          )}
        >
          {unit}
        </p>
      </CardContent>

      {variant === 'primary' ? (
        <>
          <Leaf
            className='absolute right-7 top-5 h-24 w-24 text-white/20'
            strokeWidth={1.5}
          />
          <div className='absolute -bottom-8 left-0 h-20 w-full bg-secondary/45 [clip-path:polygon(0_70%,38%_65%,74%_0,100%_35%,100%_100%,0_100%)]' />
        </>
      ) : variant === 'line' ? (
        <svg
          viewBox='0 0 320 132'
          className='absolute bottom-0 left-0 h-28 w-full'
          aria-hidden='true'
        >
          <defs>
            <linearGradient
              id='kpi-line-fill'
              x1='0'
              x2='0'
              y1='0'
              y2='1'
            >
              <stop
                offset='0%'
                stopColor='hsl(var(--primary))'
                stopOpacity='0.22'
              />
              <stop
                offset='100%'
                stopColor='hsl(var(--primary))'
                stopOpacity='0.04'
              />
            </linearGradient>
          </defs>
          <path
            d='M0 98 L120 90 L205 28 L320 78 L320 132 L0 132 Z'
            fill='url(#kpi-line-fill)'
          />
          <path
            d='M0 98 L120 90 L205 28 L320 78'
            fill='none'
            stroke='hsl(var(--primary))'
            strokeWidth='2'
          />
        </svg>
      ) : (
        <div className='absolute -bottom-8 left-0 h-28 w-full bg-muted/45 [clip-path:polygon(0_70%,36%_52%,72%_90%,100%_62%,100%_100%,0_100%)]' />
      )}
    </Card>
  );
}

type ProjectState = 'joined' | 'pending' | 'notJoined';

const projectStateBadge: Record<
  ProjectState,
  { label: string; tone: 'success' | 'warning' | 'neutral' }
> = {
  joined: { label: '入会済み', tone: 'success' },
  pending: { label: '申請中', tone: 'warning' },
  notJoined: { label: '未入会', tone: 'neutral' },
};

// One label/value style for every project surface on this page (rows, 未入会
// cards, progress), so the same field never renders at two sizes.
const metaLabelClassName = 'text-label';
const metaValueClassName = 'text-small font-medium text-foreground';
const projectSurfaceClassName =
  'rounded-lg border border-border bg-card p-5 lg:px-[var(--card-padding)]';

function ProjectIcon() {
  return (
    <span className='flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-secondary text-primary-strong'>
      <Building2 className='h-5 w-5' aria-hidden='true' />
    </span>
  );
}

/**
 * Joined-row summary: name on its own line, then status badge + 方法論番号 +
 * 方法論 on one line. The badge sits at the start of the second line so a
 * long name never pushes it to a different place. 方法論 truncates (full
 * text in the tooltip) to keep every row the same height.
 */
function ProjectSummary({
  project,
  state,
  titleSlot,
}: {
  project: SiteProject;
  state: ProjectState;
  titleSlot?: ReactNode;
}) {
  const badge = projectStateBadge[state];
  const methodology = getMethodologyLabel(project);

  return (
    <div className='flex min-w-0 items-start gap-3'>
      <ProjectIcon />
      <div className='min-w-0 flex-1'>
        <h3 className='break-words text-subsection-title text-foreground'>
          {titleSlot ?? project.projectName ?? '-'}
        </h3>
        {/* Dividers + smaller muted labels keep each label/value pair readable as its own unit. */}
        <div className='mt-1.5 flex min-w-0 items-center'>
          <StatusBadge tone={badge.tone} className='shrink-0'>
            {badge.label}
          </StatusBadge>
          <dl className='ml-3 flex min-w-0 items-baseline divide-x divide-border border-l border-border'>
            <div className='flex shrink-0 items-baseline gap-1.5 px-3'>
              <dt className={metaLabelClassName}>方法論番号</dt>
              <dd className={cn(metaValueClassName, 'tabular-nums')}>
                {project.methodNumber} {project.methodologyVersion}
              </dd>
            </div>
            <div className='flex min-w-0 items-baseline gap-1.5 px-3'>
              <dt className={cn(metaLabelClassName, 'shrink-0')}>方法論</dt>
              <dd
                className={cn(metaValueClassName, 'min-w-0 truncate')}
                title={methodology}
              >
                {methodology}
              </dd>
            </div>
          </dl>
        </div>
      </div>
    </div>
  );
}

/**
 * 未入会 card: header (icon, name, badge) is separated from the methodology
 * details by a divider, and the details are a label/value list with labels
 * in their own column so they don't blend into the header.
 */
function AvailableProjectCard({ project }: { project: SiteProject }) {
  const badge = projectStateBadge.notJoined;
  const methodology = getMethodologyLabel(project);

  return (
    <li className={cn(projectSurfaceClassName, 'flex flex-col')}>
      <div className='flex min-w-0 items-start gap-3'>
        <ProjectIcon />
        <div className='min-w-0 flex-1'>
          <h3 className='break-words text-subsection-title text-foreground'>
            {project.projectName || '-'}
          </h3>
          <StatusBadge tone={badge.tone} className='mt-1.5'>
            {badge.label}
          </StatusBadge>
        </div>
      </div>

      <dl className='mt-4 grid grid-cols-[max-content_minmax(0,1fr)] items-baseline gap-x-4 gap-y-2 border-t border-border pt-4'>
        <dt className={metaLabelClassName}>方法論番号</dt>
        <dd className={cn(metaValueClassName, 'tabular-nums')}>
          {project.methodNumber} {project.methodologyVersion}
        </dd>
        <dt className={metaLabelClassName}>方法論</dt>
        <dd className={metaValueClassName}>{methodology}</dd>
      </dl>
    </li>
  );
}

/**
 * A joined project as one row: summary | progress or note | action.
 * Rows that open the detail page are clickable as a whole (a stretched
 * button on the title), with a chevron as the visible affordance. The
 * プロジェクト参加 row keeps an explicit button because it opens a dialog.
 */
function JoinedProjectRow({
  project,
  requiresSiteParticipation,
  onAction,
}: {
  project: SiteProject;
  requiresSiteParticipation: boolean;
  onAction: () => void;
}) {
  const isPendingReview = project.participationStatus === 'OPEN';
  const opensDetail = !requiresSiteParticipation && !isPendingReview;

  return (
    <li
      className={cn(
        projectSurfaceClassName,
        'relative grid gap-4 lg:grid-cols-[minmax(0,1.15fr)_minmax(0,1fr)_9.5rem] lg:items-center lg:gap-6',
        opensDetail &&
          'transition-colors hover:border-primary/40 hover:bg-secondary/30 has-[button:focus-visible]:ring-2 has-[button:focus-visible]:ring-ring',
      )}
    >
      <ProjectSummary
        project={project}
        state={isPendingReview ? 'pending' : 'joined'}
        titleSlot={
          opensDetail ? (
            <button
              type='button'
              onClick={onAction}
              className='text-left focus-visible:outline-none after:absolute after:inset-0 after:rounded-lg after:content-[""]'
            >
              {project.projectName || '-'}
            </button>
          ) : undefined
        }
      />

      <div className='min-w-0'>
        {opensDetail ? (
          <DashboardEmissionReductionProgress projectId={project.id} />
        ) : (
          <p className='text-small text-muted-foreground'>
            {isPendingReview
              ? '入会申請を審査中です。承認後に排出削減量を確認できます。'
              : 'この拠点はまだ参加していません。参加すると排出削減量を確認できます。'}
          </p>
        )}
      </div>

      <div className='flex lg:justify-end'>
        {opensDetail ? (
          <span className='hidden items-center gap-1 text-small font-medium text-primary-strong lg:inline-flex'>
            詳細
            <ChevronRight className='h-4 w-4' aria-hidden='true' />
          </span>
        ) : requiresSiteParticipation ? (
          <Button
            type='button'
            variant='outline'
            size='md'
            className='w-full lg:w-auto'
            onClick={onAction}
          >
            プロジェクト参加
          </Button>
        ) : null}
      </div>
    </li>
  );
}

/** Renders the site dashboard with responsive project sections. */
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

  const [isSiteLoading, setIsSiteLoading] = useState(true);
  const [isProjectsLoading, setIsProjectsLoading] = useState(true);
  const [siteError, setSiteError] = useState<string | null>(null);
  const [projectsError, setProjectsError] = useState<string | null>(null);
  const [site, setSite] = useState<Site | null>(null);
  const [projects, setProjects] = useState<SiteProject[]>([]);
  const [projectToJoin, setProjectToJoin] = useState<SiteProject | null>(null);
  const [isJoiningProject, setIsJoiningProject] = useState(false);

  const sortedProjects = useMemo(
    () => sortProjectsByParticipationStatus(projects),
    [projects],
  );

  // 入会中 / 未入会 are split by isCompanyJoined alone; projectStatus does not
  // hide a project here (the API sends Japanese statuses such as 準備中).
  const displayableProjects = sortedProjects;

  const companyJoinedProjects = useMemo(
    () =>
      displayableProjects.filter(
        (project) =>
          project.isCompanyJoined ||
          project.participationStatus === 'OPEN',
      ),
    [displayableProjects],
  );

  const availableProjects = useMemo(
    () =>
      displayableProjects.filter(
        (project) =>
          project.participationStatus !== 'OPEN' &&
          !project.isCompanyJoined &&
          !project.hasSiteRelation,
      ),
    [displayableProjects],
  );

  // const sortedProjects = useMemo(
  //   () => sortProjectsByParticipationStatus(projects),
  //   [projects],
  // );
  // const displayableProjects = useMemo(
  //   () => sortedProjects.filter(isDisplayableSiteProject),
  //   [sortedProjects],
  // );

  // const companyJoinedProjects = useMemo(
  //   () =>
  //     displayableProjects.filter(
  //       (project) =>
  //         project.isCompanyJoined || project.participationStatus === 'OPEN',
  //     ),
  //   [displayableProjects],
  // );
  // const availableProjects = useMemo(
  //   () =>
  //     displayableProjects.filter(
  //       (project) =>
  //         project.participationStatus !== 'OPEN' &&
  //         !project.isCompanyJoined &&
  //         !project.hasSiteRelation,
  //     ),
  //   [displayableProjects],
  // );

  useEffect(() => {
    if (
      !user ||
      user.role === 'master' ||
      user.role === 'admin' ||
      userSiteId
    ) {
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
      setIsSiteLoading(false);
      setIsProjectsLoading(false);
      return;
    }

    if (!activeSiteId) {
      setIsSiteLoading(false);
      setIsProjectsLoading(false);
      toast({
        title: 'エラー',
        description: 'siteId が見つかりません',
        variant: 'destructive',
      });
      return;
    }

    let isMounted = true;
    void loadNotifications();

    setIsSiteLoading(true);
    setIsProjectsLoading(true);
    setSiteError(null);
    setProjectsError(null);

    void getSiteData(activeSiteId)
      .then((siteResponse) => {
        if (!isMounted) return;
        setSite(siteResponse.site);
      })
      .catch((error: unknown) => {
        if (!isMounted) return;
        setSite(null);
        setSiteError(
          error instanceof Error
            ? error.message
            : '拠点情報を取得できませんでした。',
        );
      })
      .finally(() => {
        if (isMounted) setIsSiteLoading(false);
      });

    void getSiteProjects(activeSiteId)
      .then((projectData) => {
        if (!isMounted) return;
        setProjects(projectData);
      })
      .catch((error: unknown) => {
        if (!isMounted) return;
        setProjects([]);
        setProjectsError(
          error instanceof Error
            ? error.message
            : 'プロジェクト情報を取得できませんでした。',
        );
      })
      .finally(() => {
        if (isMounted) setIsProjectsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [
    toast,
    user,
    activeSiteId,
    navigate,
    loadNotifications,
    isUserWithoutSite,
  ]);

  useEffect(() => {
    setCurrentProject(null);
  }, [setCurrentProject]);

  // Opens the detail page for a project already related to the current site.
  const handleOpenProject = (project: SiteProject) => {
    navigate(paths.siteProjectDetail(activeSiteId, project.id), {
      state: {
        siteName: site?.siteName ?? '',
      },
    });
  };

  // Opens the confirmation dialog before relating the current site to the project.
  const handleParticipateProject = (project: SiteProject) => {
    setProjectToJoin(project);
  };

  /** Prevents dismissing the join confirmation while its request is running. */
  const handleJoinDialogOpenChange = (open: boolean) => {
    if (!open && !isJoiningProject) setProjectToJoin(null);
  };

  /** Relates the active site to the selected project and refreshes dashboard data. */
  const handleConfirmProjectJoin = async () => {
    if (!projectToJoin || !activeSiteId || isJoiningProject) return;

    setIsJoiningProject(true);

    try {
      await joinProjectSite(projectToJoin.id, activeSiteId);
      setCurrentProject(projectToJoin);
      setProjects((currentProjects) =>
        currentProjects.map((project) =>
          project.id === projectToJoin.id
            ? { ...project, hasSiteRelation: true }
            : project,
        ),
      );
      setProjectToJoin(null);
      toast({
        title: 'プロジェクトに参加しました',
        description:
          'プロジェクト詳細の確認や設備の追加・管理ができるようになりました。',
      });

      try {
        setProjects(await getSiteProjects(activeSiteId));
      } catch (error) {
        toast({
          title: '最新のプロジェクト情報を取得できませんでした',
          description: error instanceof Error ? error.message : undefined,
          variant: 'destructive',
        });
      }
    } catch (error) {
      toast({
        title: 'プロジェクトに参加できませんでした',
        description: error instanceof Error ? error.message : undefined,
        variant: 'destructive',
      });
    } finally {
      setIsJoiningProject(false);
    }
  };

  const picName =
    `${site?.picLastName ?? ''} ${site?.picFirstName ?? ''}`.trim() || '-';
  const address = site?.address || '-';

  // Users land here as their home; masters drill in from the 拠点 tab.
  usePageBreadcrumb(
    user?.role === 'master'
      ? [
          { label: '拠点', to: `${paths.masterDashboard}?tab=sites` },
          { label: site?.siteName || '拠点ダッシュボード' },
        ]
      : [],
  );

  if (isRefreshingUserSite) {
    return <PageSkeleton />;
  }

  if (isUserWithoutSite) {
    return (
      <div className='min-h-screen bg-background py-8'>
        <div className={pageContainer}>
          <Card className='mx-auto mt-16 max-w-2xl'>
            <CardContent className='p-8 text-center text-foreground'>
              所属している拠点がありません。マスターにお問い合わせください。
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div className='relative min-h-screen bg-background'>
      <header className='relative border-b border-border bg-background' data-tour='site-header'>
        <div
          className={cn(
            dashboardPageHeaderClassName,
            'flex-col items-start justify-between gap-5 py-5 text-left sm:flex-row sm:items-center sm:py-0',
          )}
        >
          <div className='min-w-0'>
            {/* <span className='text-small pl-[60px] text-muted-foreground'>
              拠点ダッシュボード
            </span> */}
            <div className='mt-1 flex min-w-0 items-center gap-3'>
              <span className='flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-secondary text-primary-strong'>
                <Factory className='h-6 w-6' />
              </span>
              {isSiteLoading ? (
                <InlineLoader label='拠点情報を読み込んでいます' />
              ) : (
                <h1 className='min-w-0 break-words text-metric leading-[1.111] text-foreground'>
                  {site?.siteName || '株式会社ABC'}
                </h1>
              )}
            </div>
            <dl className='ml-[60px] mt-2 grid gap-x-10 gap-y-3 sm:grid-cols-[max-content_minmax(0,1fr)]'>
              <div>
                <dt className='text-label'>責任者</dt>
                <dd className='mt-1 font-medium'>
                  {isSiteLoading ? (
                    <InlineLoader label='責任者を読み込んでいます' />
                  ) : (
                    picName
                  )}
                </dd>
              </div>
              <div className='min-w-0'>
                <dt className='text-label'>所在地</dt>
                <dd className='mt-1 break-words font-medium'>
                  {isSiteLoading ? (
                    <InlineLoader label='所在地を読み込んでいます' />
                  ) : (
                    address
                  )}
                </dd>
              </div>
            </dl>
          </div>
          {siteError ? (
            <p
              role='alert'
              className='mt-4 text-small text-destructive-strong'
            >
              {siteError}
            </p>
          ) : null}

          {user?.role === 'master' && activeSiteId && (
            <Button
              type='button'
              variant='outline'
              size='md'
              className='shrink-0'
              data-tour='site-edit'
              onClick={() => navigate(`/master/site-edit/${activeSiteId}`)}
            >
              拠点情報を更新
            </Button>
          )}
        </div>
      </header>

      <section className='bg-card py-12 lg:py-[calc(var(--section-gap)*1.5)]' data-tour='joined-projects'>
        <div className={pageContainer}>
          <div className='mb-6'>
            <h2 className='text-foreground'>入会中のプロジェクト</h2>
            <p className='text-small mt-2 text-foreground'>
              企業が入会済みのプロジェクトです。拠点に設備が登録されている場合は、進捗や排出量の詳細を確認できます。
            </p>
          </div>

          {isProjectsLoading ? (
            <div className='space-y-3'>
              {Array.from({ length: 2 }, (_, index) => (
                <SkeletonCard
                  key={index}
                  className='min-h-24 rounded-lg'
                />
              ))}
            </div>
          ) : projectsError ? (
            <Card className='rounded-lg border-border'>
              <CardContent
                className='p-10 text-center text-destructive-strong'
                role='alert'
              >
                {projectsError}
              </CardContent>
            </Card>
          ) : companyJoinedProjects.length > 0 ? (
            <ul className='space-y-3'>
              {companyJoinedProjects.map((project) => (
                <JoinedProjectRow
                  key={project.id}
                  project={project}
                  requiresSiteParticipation={!project.hasSiteRelation}
                  onAction={() =>
                    project.hasSiteRelation
                      ? handleOpenProject(project)
                      : handleParticipateProject(project)
                  }
                />
              ))}
            </ul>
          ) : (
            <Card className='rounded-lg border-border'>
              <CardContent className='p-10 text-center text-foreground'>
                入会中のプロジェクトがありません
              </CardContent>
            </Card>
          )}
        </div>
      </section>

      <section className='bg-background py-12 lg:py-[calc(var(--section-gap)*1.5)]' data-tour='available-projects'>
        <div className={pageContainer}>
          <div className='mb-6 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between'>
            <div>
              <h2 className='text-foreground'>未入会プロジェクト</h2>
              <p className='text-small mt-2 whitespace-pre-line text-foreground'>
                {user?.role === 'master'
                  ? 'プロジェクトへの入会をご希望の場合は、「プロジェクトに入会する」ボタンから、入会するプロジェクトを選択してください。'
                  : 'この拠点は、以下のプロジェクトにまだ入会していません。\n入会をご希望の場合は、マスター権限を持つ責任者へお問い合わせください。'}
              </p>
            </div>
            {user?.role === 'master' && activeSiteId && (
              <Button
                type='button'
                size='md'
                className='shrink-0'
                data-tour='join-project'
                onClick={() => navigate(paths.projectMembership(activeSiteId))}
              >
                プロジェクトに入会する
              </Button>
            )}
          </div>

          {isProjectsLoading ? (
            <div className='grid gap-3 md:grid-cols-2 xl:grid-cols-3'>
              {Array.from({ length: 3 }, (_, index) => (
                <SkeletonCard
                  key={index}
                  className='min-h-24 rounded-lg'
                />
              ))}
            </div>
          ) : projectsError ? (
            <Card className='rounded-lg border-border'>
              <CardContent
                className='p-10 text-center text-destructive-strong'
                role='alert'
              >
                {projectsError}
              </CardContent>
            </Card>
          ) : availableProjects.length > 0 ? (
            <ul className='grid gap-3 md:grid-cols-2 xl:grid-cols-3'>
              {availableProjects.map((project) => (
                <AvailableProjectCard key={project.id} project={project} />
              ))}
            </ul>
          ) : (
            <Card className='rounded-lg border-border'>
              <CardContent className='p-10 text-center text-foreground'>
                未入会のプロジェクトがありません
              </CardContent>
            </Card>
          )}
        </div>
      </section>
      <AlertDialog
        open={Boolean(projectToJoin)}
        onOpenChange={handleJoinDialogOpenChange}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>プロジェクトに参加しますか？</AlertDialogTitle>
            <AlertDialogDescription className='space-y-2'>
              <span className='block font-medium text-foreground'>
                {projectToJoin?.projectName}
              </span>
              <span className='block'>
                参加後は、プロジェクト詳細の確認、設備の追加・管理、モニタリングデータの登録などができるようになります。
              </span>
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={isJoiningProject}>
              キャンセル
            </AlertDialogCancel>
            <Button
              type='button'
              disabled={isJoiningProject}
              onClick={() => void handleConfirmProjectJoin()}
            >
              {isJoiningProject ? '入会中...' : '入会する'}
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
};

export default Dashboard;
