import { useEffect, useState, type ReactNode } from 'react';
import {
  getProjectDetails,
  type ProjectDetail as ProjectDetailApi,
} from '@/api/projects';
import { getSiteProjectEquipment, type SiteProjectEquipment } from '@/api/site';
import {
  useLocation,
  useParams,
  useNavigate,
  useSearchParams,
} from 'react-router-dom';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import {
  ArrowLeft,
  ArrowRight,
  BatteryCharging,
  Calendar,
  ChartNoAxesColumn,
  Cpu,
  Hash,
  HousePlug,
  Layers,
  MapPin,
  PackageCheck,
  Plus,
  Zap,
} from 'lucide-react';
import { Tabs, TabsContent } from '@/components/ui/tabs';
import { getMonitoringRecent } from '@/api/monitoring';
import type { MonitoringData } from '@/api/monitoring';
import {
  getEmissionTrend,
  getProjectEmissionSummary,
  type EmissionTrendPoint,
  type ProjectEmissionBreakdown,
} from '@/api/batch';
import { logger } from '@/lib/logger';
import {
  PageBackButton,
  // floatingPageBackButtonClassName,
} from '@/components/navigation/PageBackButton';
import {
  PageSkeleton,
  SkeletonCard,
  SkeletonChart,
  SkeletonMetricCard,
} from '@/components/feedback/loading';
import {
  dashboardWidePageContainerClassName,
  dashboardPageContainerClassName,
} from '@/components/layout/DashboardLayout';
import { EquipmentList } from './components/EquipmentList';
import { MonitoringCancellationDialog } from './components/MonitoringCancellationDialog';
import { EmissionTrendChart } from './components/EmissionTrendChart';
import { EmissionBreakdownChart } from './components/EmissionBreakdownChart';
import { PROJECT_EMISSION_REDUCTION_TARGET } from './components/emissionReductionProgressUtils';
import { projectDetailCardClassName } from './projectDetailStyles';
import { cn } from '@/lib/utils';
import { paths } from '@/routes/paths';
import { FloatingHelpButton } from '@/features/master/help/FloatingHelpButton';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { EmissionReductionProgressCard } from './components/EmissionReductionProgressCard';
import { PageTabsList, PageTabsTrigger } from '@/components/navigation/PageTabs';

function formatNumber(n: number | null | undefined) {
  if (n == null || Number.isNaN(n)) return '-';
  return new Intl.NumberFormat().format(n);
}

function toNumberOrNull(
  value: string | number | null | undefined,
): number | null {
  if (value === null || value === undefined || value === '') {
    return null;
  }

  const parsed = typeof value === 'number' ? value : Number(value);

  return Number.isFinite(parsed) ? parsed : null;
}

function calculateReductionRate(
  baselineEmission: number | null,
  emissionReduction: number | null,
) {
  if (
    baselineEmission === null ||
    emissionReduction === null ||
    baselineEmission <= 0
  ) {
    return null;
  }

  return (emissionReduction / baselineEmission) * 100;
}

type MonitoringDisplayRow = Pick<
  MonitoringData,
  | 'projectId'
  | 'siteId'
  | 'dateRecorded'
  | 'elpv_evidence'
  | 'elpj_battery_evidence'
> & {
  ELpv_value: number | null;
  ELpvr_value: number | null;
  ELPJ_battery_value: number | null;
  ELPJ_accuracy: number | null;
  ELPJ_control_value: number | null;
};

const fallbackMonitoringRow: MonitoringDisplayRow = {
  projectId: '',
  siteId: null,
  dateRecorded: '-',
  ELpv_value: null,
  ELpvr_value: null,
  ELPJ_battery_value: null,
  ELPJ_accuracy: null,
  ELPJ_control_value: null,
};

type ProjectDetailTabValue = 'overview' | 'equipment';

type ProjectDetailNavigationState = {
  siteName?: string;
};

function ProjectDetailTabs() {
  return (
    <PageTabsList aria-label='プロジェクト詳細の表示'>
      <PageTabsTrigger value='overview'>概要</PageTabsTrigger>
      <PageTabsTrigger value='equipment'>設備一覧</PageTabsTrigger>
    </PageTabsList>
  );
}

function formatMonitoringDate(value: string | null | undefined) {
  if (!value || value === '-') return '-';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return `${date.getFullYear()}年${String(date.getMonth() + 1).padStart(2, '0')}月${String(date.getDate()).padStart(2, '0')}日`;
}

function formatCreatedDate(value: string | null | undefined) {
  if (!value || value === '-') return '-';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  return `${year}年${month}月${day}日 ${hours}:${minutes}`;
}

type ProjectMetaItemProps = {
  icon: ReactNode;
  label: string;
  value: ReactNode;
  className?: string;
};

function ProjectMetaItem({
  icon,
  label,
  value,
  className,
}: ProjectMetaItemProps) {
  return (
    <div
      className={cn(
        'flex min-w-0 items-start gap-3 px-5 py-4 sm:px-6',
        className,
      )}
    >
      <span className='mt-0.5 shrink-0 text-muted-foreground'>{icon}</span>

      <div className='min-w-0'>
        <p className='text-small text-muted-foreground'>{label}</p>

        <div className='mt-1 break-words font-bold text-foreground'>
          {value || '-'}
        </div>
      </div>
    </div>
  );
}

type SummaryMetricProps = {
  label: string;
  value: string;
  unit?: string;
  emphasis?: boolean;
};

/** Renders one headline project metric with an optional emphasis color. */
function SummaryMetric({
  label,
  value,
  unit,
  emphasis = false,
}: SummaryMetricProps) {
  return (
    <div className='min-w-0 bg-card px-5 py-5 sm:px-6 sm:py-6'>
      <p className='text-small text-muted-foreground'>{label}</p>

      <div className='mt-2 flex flex-wrap items-baseline gap-x-1.5'>
        <span
          className={cn(
            'text-form-title font-bold tabular-nums',
            emphasis ? 'text-primary' : 'text-foreground',
          )}
        >
          {value}
        </span>

        {unit && value !== '-' ? (
          <span className='text-small text-muted-foreground'>{unit}</span>
        ) : null}
      </div>
    </div>
  );
}

type MonitoringMetricProps = {
  icon: ReactNode;
  label: string;
  value: string;
  unit?: string;
};

function MonitoringMetric({ icon, label, value, unit }: MonitoringMetricProps) {
  return (
    <div className='min-w-0 bg-card px-4 py-5'>
      <div className='flex min-w-0 items-center gap-2'>
        <span className='shrink-0 text-primary'>{icon}</span>

        <span className='truncate text-table font-bold text-foreground'>
          {label}
        </span>
      </div>

      <div className='mt-3 flex items-baseline gap-1.5'>
        <span className='text-2xl font-bold tabular-nums text-foreground'>
          {value}
        </span>

        {unit && value !== '-' ? (
          <span className='text-table text-muted-foreground'>{unit}</span>
        ) : null}
      </div>
    </div>
  );
}

type RecentDataBarProps = {
  row: MonitoringDisplayRow;
};

/** Displays the latest monitoring values without the removed battery replacement flag. */
function RecentDataBar({ row }: RecentDataBarProps) {
  const batteryDischargeValue = row.ELPJ_battery_value;

  return (
    <section className={cn(projectDetailCardClassName, 'overflow-hidden')}>
      <div className='flex flex-col gap-3 border-b border-border px-5 py-5 sm:flex-row sm:items-center sm:justify-between sm:px-6'>
        <div>
          <p className='text-small text-muted-foreground'>最新モニタリング</p>

          <h2 className='mt-1 text-xl font-bold text-foreground'>
            モニタリング状況
          </h2>
        </div>

        <div className='inline-flex w-fit items-center gap-2 rounded-lg bg-muted px-3 py-2 text-small text-foreground'>
          <Calendar className='h-4 w-4 text-muted-foreground' />

          <span>記録日</span>

          <span className='font-bold'>
            {formatMonitoringDate(row.dateRecorded)}
          </span>
        </div>
      </div>

      <div className='grid gap-px bg-border sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5'>
        <MonitoringMetric
          icon={<Cpu className='h-5 w-5' />}
          label='制御機器稼働時間'
          value={formatNumber(row.ELPJ_control_value)}
          unit='時間'
        />

        <MonitoringMetric
          icon={<Zap className='h-5 w-5' />}
          label='発電量'
          value={formatNumber(row.ELpv_value)}
          unit='kWh'
        />

        <MonitoringMetric
          icon={<HousePlug className='h-5 w-5' />}
          label='自家消費量'
          value={formatNumber(row.ELpvr_value)}
          unit='kWh'
        />

        <MonitoringMetric
          icon={<BatteryCharging className='h-5 w-5' />}
          label='蓄電池放電量'
          value={formatNumber(batteryDischargeValue)}
          unit='kWh'
        />

        <MonitoringMetric
          icon={<ChartNoAxesColumn className='h-5 w-5' />}
          label='測定精度'
          value={formatNumber(row.ELPJ_accuracy)}
          unit='%'
        />
      </div>
    </section>
  );
}

/** Renders project status, equipment, and monitoring details for a site. */
const ProjectDetail = () => {
  const { id, projectId: routeProjectId, siteId: routeSiteId } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const navigationState = useLocation()
    .state as ProjectDetailNavigationState | null;

  const projectId = routeProjectId || id || '';
  const siteId = routeSiteId || searchParams.get('siteId') || '';
  const siteName = navigationState?.siteName?.trim() || '-';

  type ProjectView = {
    name: string;
    description: string;
    methodology: string;
    methodologyName: string;
    projectNumber: string;
    status: 'active' | 'inactive';
    createdDate: string;
    location: string;

    emissionReduction: number | null;
    baselineEmission: number | null;
    postProjectEmission: number | null;
  };

  const [project, setProject] = useState<ProjectView | null>(null);
  const [loading, setLoading] = useState(true);

  const [tab, setTab] = useState<ProjectDetailTabValue>(() =>
    searchParams.get('tab') === 'equipment' ? 'equipment' : 'overview',
  );
  const [recentMonitoring, setRecentMonitoring] = useState<MonitoringData[]>(
    [],
  );
  const [recentLoading, setRecentLoading] = useState(false);
  const [recentHasFetched, setRecentHasFetched] = useState(false);
  const [equipmentList, setEquipmentList] = useState<SiteProjectEquipment[]>(
    [],
  );
  const [equipmentLoading, setEquipmentLoading] = useState(false);
  const [equipmentHasFetched, setEquipmentHasFetched] = useState(false);
  const [equipmentError, setEquipmentError] = useState<string | null>(null);
  const [emissionTrend, setEmissionTrend] = useState<EmissionTrendPoint[]>([]);
  const [emissionTrendLoading, setEmissionTrendLoading] = useState(false);
  const [emissionTrendError, setEmissionTrendError] = useState<string | null>(
    null,
  );
  const [emissionBreakdown, setEmissionBreakdown] =
    useState<ProjectEmissionBreakdown | null>(null);
  const [emissionBreakdownLoading, setEmissionBreakdownLoading] =
    useState(false);
  const [emissionBreakdownError, setEmissionBreakdownError] = useState<
    string | null
  >(null);
  const [projectTotalEmission, setProjectTotalEmission] = useState<
    number | null
  >(null);
  const [projectBaselineEmission, setProjectBaselineEmission] = useState<
    number | null
  >(null);
  const [monitoringCancellationOpen, setMonitoringCancellationOpen] =
    useState(false);

  useEffect(() => {
    setRecentHasFetched(false);
    setRecentMonitoring([]);
    setEquipmentHasFetched(false);
    setEquipmentList([]);
    setEquipmentError(null);
  }, [projectId, siteId]);

  useEffect(() => {
    let isCancelled = false;

    const fetchEmissionTrend = async () => {
      if (!projectId || !siteId) {
        setEmissionTrend([]);
        setEmissionTrendError(null);
        setEmissionTrendLoading(false);
        return;
      }

      try {
        setEmissionTrendLoading(true);
        setEmissionTrendError(null);
        const trend = await getEmissionTrend(siteId, projectId);

        if (!isCancelled) {
          setEmissionTrend(trend);
        }
      } catch (error) {
        logger.error('Error loading project emission trend:', error);

        if (!isCancelled) {
          setEmissionTrend([]);
          setEmissionTrendError('排出量データの取得に失敗しました。');
        }
      } finally {
        if (!isCancelled) {
          setEmissionTrendLoading(false);
        }
      }
    };

    void fetchEmissionTrend();

    return () => {
      isCancelled = true;
    };
  }, [projectId, siteId]);

  useEffect(() => {
    let isCancelled = false;

    const fetchProjectEmissionSummary = async () => {
      if (!projectId) {
        setEmissionBreakdown(null);
        setProjectTotalEmission(null);
        setProjectBaselineEmission(null);
        setEmissionBreakdownError(null);
        setEmissionBreakdownLoading(false);
        return;
      }

      try {
        setEmissionBreakdownLoading(true);
        setEmissionBreakdownError(null);

        const summary = await getProjectEmissionSummary(projectId);

        if (!isCancelled) {
          setEmissionBreakdown(summary?.breakdown ?? null);
          setProjectTotalEmission(summary?.projectEmission ?? null);
          setProjectBaselineEmission(summary?.baselineEmission ?? null);
        }
      } catch (error) {
        logger.error('Error loading project emission summary:', error);

        if (!isCancelled) {
          setEmissionBreakdown(null);
          setProjectTotalEmission(null);
          setProjectBaselineEmission(null);
          setEmissionBreakdownError('排出量内訳データの取得に失敗しました。');
        }
      } finally {
        if (!isCancelled) {
          setEmissionBreakdownLoading(false);
        }
      }
    };

    void fetchProjectEmissionSummary();

    return () => {
      isCancelled = true;
    };
  }, [projectId]);

  useEffect(() => {
    const loadProject = async () => {
      if (!projectId) return;

      try {
        setLoading(true);
        const result = (await getProjectDetails(projectId)) as ProjectDetailApi;

        setProject({
          projectNumber: result.projectNumber ?? '',
          name: result.projectName ?? '',
          description: result.methodDetails ?? '',

          methodology: [
            result.methodologyCategory,
            result.methodologyNumber,
            result.methodologyVersion,
          ]
            .filter(Boolean)
            .join(' '),

          methodologyName: result.methodologyName ?? '',

          status:
            String(result.projectStatus).toLowerCase() === 'active'
              ? 'active'
              : 'inactive',

          createdDate: result.createdAt ?? '',
          location: siteName,

          emissionReduction: toNumberOrNull(result.emissionReduction),

          baselineEmission: toNumberOrNull(result.baselineEmission),

          postProjectEmission: toNumberOrNull(result.postProjectEmission),
        });
      } catch (error) {
        logger.error('Error loading project detail:', error);
        setProject(null);
      } finally {
        setLoading(false);
      }
    };

    loadProject();
  }, [projectId, siteName]);

  useEffect(() => {
    const fetchRecent = async () => {
      if (!projectId) return;
      if (!siteId) return;
      if (recentHasFetched) return;

      try {
        setRecentLoading(true);

        const rows = await getMonitoringRecent({
          projectId,
          siteId,
          limit: 10,
        });

        setRecentMonitoring(rows ?? []);
        setRecentHasFetched(true);
      } catch (e) {
        logger.error('Error loading recent monitoring:', e);
        setRecentMonitoring([]);
        setRecentHasFetched(true);
      } finally {
        setRecentLoading(false);
      }
    };

    fetchRecent();
  }, [projectId, siteId, recentHasFetched]);

  useEffect(() => {
    const fetchEquipment = async () => {
      if (tab !== 'equipment') return;
      if (!projectId || !siteId) return;
      if (equipmentHasFetched) return;

      try {
        setEquipmentLoading(true);
        setEquipmentError(null);
        const rows = await getSiteProjectEquipment(siteId, projectId);
        setEquipmentList(rows);
        setEquipmentHasFetched(true);
      } catch (error) {
        logger.error('Error loading site project equipment:', error);
        setEquipmentList([]);
        setEquipmentError('設備情報の取得に失敗しました。');
        setEquipmentHasFetched(true);
      } finally {
        setEquipmentLoading(false);
      }
    };

    void fetchEquipment();
  }, [equipmentHasFetched, projectId, siteId, tab]);

  const monitoringRows: MonitoringDisplayRow[] =
    recentMonitoring.length > 0
      ? recentMonitoring.map((row) => ({
          ...row,
          ELpv_value: row.ELpv_value ?? null,
          ELpvr_value: row.ELpvr_value ?? null,
          ELPJ_battery_value: row.ELPJ_battery_value ?? null,
          ELPJ_accuracy: row.ELPJ_accuracy ?? null,
          ELPJ_control_value: row.ELPJ_control_value ?? null,
        }))
      : [fallbackMonitoringRow];

  if (loading) {
    return (
      <PageSkeleton className='py-8'>
        <div className='mx-auto w-full max-w-page space-y-6'>
          <SkeletonCard className='min-h-28' />
          <div className='grid gap-4 sm:grid-cols-2 xl:grid-cols-4'>
            {Array.from({ length: 4 }, (_, index) => (
              <SkeletonMetricCard key={index} />
            ))}
          </div>
          <div className='grid gap-6 lg:grid-cols-2'>
            <SkeletonChart />
            <SkeletonChart />
          </div>
        </div>
      </PageSkeleton>
    );
  }

  if (!project) {
    return (
      <div className='min-h-screen flex items-center justify-center'>
        <Card className='max-w-md'>
          <CardHeader>
            <CardTitle>プロジェクトが見つかりません</CardTitle>
            <CardDescription>
              お探しのプロジェクトは存在しません。
            </CardDescription>
          </CardHeader>
          <CardContent className='flex gap-3'>
            <Button
              onClick={() =>
                navigate(
                  siteId ? `/sites/${siteId}/dashboard` : '/master/dashboard',
                )
              }
            >
              <ArrowLeft className='h-4 w-4 mr-2' />
              ダッシュボードへ戻る
            </Button>
            <Button
              onClick={() =>
                navigate('/monitoring/register/solar', {
                  state: { projectId, siteId },
                })
              }
            >
              <ArrowRight className='h-4 w-4 mr-2' />
              モニタリング登録へ
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  const effectiveBaselineEmission =
    projectBaselineEmission ?? project.baselineEmission;

  const effectivePostProjectEmission =
    projectTotalEmission ?? project.postProjectEmission;

  const calculatedReduction =
    effectiveBaselineEmission !== null && effectivePostProjectEmission !== null
      ? effectiveBaselineEmission - effectivePostProjectEmission
      : null;

  const effectiveEmissionReduction =
    project.emissionReduction ?? calculatedReduction;

  const reductionRate = calculateReductionRate(
    effectiveBaselineEmission,
    effectiveEmissionReduction,
  );

  const methodologyDisplay =
    [project.methodology, project.methodologyName]
      .filter(Boolean)
      .join(' / ') || '-';
  return (
    <div className='relative min-h-screen bg-background py-6 sm:py-8'>
      <PageBackButton
        onClick={() => {
          if (siteId) {
            navigate(`/sites/${siteId}/dashboard`);
          } else {
            navigate(-1);
          }
        }}
        placement='floating'
      />
      <div className={dashboardPageContainerClassName}>
        <div className='space-y-6'>
          {/* =========================
              PROJECT HEADER
          ========================== */}
          <header
            className={cn(
              projectDetailCardClassName,
              'overflow-hidden animate-slide-up',
            )}
          >
            <div className='px-5 py-6 sm:px-7 sm:py-7'>
              <div className='flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between'>
                <div className='flex min-w-0 items-start gap-4'>
                  <div className='min-w-0'>
                    <div className='mb-3 flex flex-wrap items-center gap-2'>
                      <StatusBadge
                        tone={
                          project.status === 'active' ? 'success' : 'neutral'
                        }
                      >
                        {project.status === 'active' ? '実施中' : '停止中'}
                      </StatusBadge>

                      <span className='inline-flex rounded-full border border-border bg-background px-2.5 py-1 text-xs font-medium text-muted-foreground'>
                        {project.projectNumber || '-'}
                      </span>
                    </div>

                    <h1 className='max-w-4xl text-2xl font-bold leading-tight text-foreground sm:text-form-title'>
                      {project.name}
                    </h1>

                    {project.description ? (
                      <p className='mt-3 max-w-5xl text-body leading-7 text-muted-foreground'>
                        {project.description}
                      </p>
                    ) : null}
                  </div>
                </div>

                <div className='flex shrink-0 flex-col gap-2 sm:flex-row'>
                  <Button
                    type='button'
                    variant='outline'
                    size='md'
                    disabled={!siteId || !projectId}
                    onClick={() => setMonitoringCancellationOpen(true)}
                  >
                    取消一覧
                  </Button>

                  <Button
                    type='button'
                    size='md'
                    disabled={!siteId || !projectId}
                    onClick={() =>
                      navigate('/monitoring/register/solar', {
                        state: {
                          projectId,
                          siteId,
                        },
                      })
                    }
                  >
                    モニタリングデータを登録
                  </Button>
                </div>
              </div>
            </div>

            {/* Project metadata */}
            <div className='grid gap-px border-t border-border bg-neutral-100 md:grid-cols-3'>
              <ProjectMetaItem
                icon={<MapPin className='h-5 w-5' />}
                label='拠点'
                value={project.location || '-'}
                className='bg-muted/30'
              />

              <ProjectMetaItem
                icon={<Layers className='h-5 w-5' />}
                label='方法論'
                value={methodologyDisplay}
                className='bg-muted/30'
              />

              <ProjectMetaItem
                icon={<Calendar className='h-5 w-5' />}
                label='登録日時'
                value={formatCreatedDate(project.createdDate)}
                className='bg-muted/30'
              />
            </div>
          </header>

          {/* =========================
              TABS
          ========================== */}
          <Tabs
            defaultValue='overview'
            value={tab}
            onValueChange={(value) => setTab(value as ProjectDetailTabValue)}
            className='space-y-6'
          >
            <ProjectDetailTabs />

            {/* =========================
                OVERVIEW
            ========================== */}
            <TabsContent
              value='overview'
              className='m-0 space-y-7'
            >
              {/* Reduction KPI */}
              {/* <section aria-labelledby='reduction-summary-title'>
                <div className='mb-4'>
                  <h2
                    id='reduction-summary-title'
                    className='text-xl font-bold text-foreground'
                  >
                    排出削減サマリー
                  </h2>

                  <p className='mt-1 text-small text-muted-foreground'>
                    プロジェクトの排出量と削減実績を確認できます。
                  </p>
                </div>

                <Card
                  className={cn(projectDetailCardClassName, 'overflow-hidden')}
                >
                  <CardContent className='p-0'>
                    <div className='grid gap-px bg-border sm:grid-cols-2 xl:grid-cols-4'>
                      <SummaryMetric
                        label='削減量'
                        value={formatNumber(effectiveEmissionReduction)}
                        unit='tCO₂'
                        emphasis
                      />

                      <SummaryMetric
                        label='ベースライン排出量'
                        value={formatNumber(effectiveBaselineEmission)}
                        unit='tCO₂'
                      />

                      <SummaryMetric
                        label='プロジェクト実施後排出量'
                        value={formatNumber(effectivePostProjectEmission)}
                        unit='tCO₂'
                      />

                      <SummaryMetric
                        label='削減率'
                        value={
                          reductionRate === null
                            ? '-'
                            : reductionRate.toLocaleString('ja-JP', {
                                maximumFractionDigits: 1,
                              })
                        }
                        unit='%'
                      />
                    </div>
                  </CardContent>
                </Card>
              </section> */}

              {/* Latest monitoring */}
              {recentLoading ? (
                <SkeletonCard className='min-h-44 rounded-2xl' />
              ) : (
                <RecentDataBar row={monitoringRows[0]} />
              )}

              {/* Analytics */}
              <section aria-labelledby='emission-analysis-title'>
                <div className='mb-4'>
                  <h2
                    id='emission-analysis-title'
                    className='text-xl font-bold text-foreground'
                  >
                    排出量分析
                  </h2>

                  <p className='mt-1 text-small text-foreground'>
                    排出量の推移と内訳から、プロジェクトの状況を確認できます。
                  </p>
                </div>

                <div className='space-y-6'>
                  <div className='grid grid-cols-1 gap-6 xl:grid-cols-2'>
                    <EmissionBreakdownChart
                      data={emissionBreakdown}
                      isLoading={emissionBreakdownLoading}
                      error={emissionBreakdownError}
                      className={projectDetailCardClassName}
                    />

                    <EmissionReductionProgressCard
                      baselineEmission={projectBaselineEmission}
                      projectEmission={projectTotalEmission}
                      targetReduction={PROJECT_EMISSION_REDUCTION_TARGET}
                      className={projectDetailCardClassName}
                    />
                  </div>
                  <EmissionTrendChart
                    data={emissionTrend}
                    isLoading={emissionTrendLoading}
                    error={emissionTrendError}
                    className={projectDetailCardClassName}
                  />
                </div>
              </section>
            </TabsContent>

            {/* =========================
                EQUIPMENT
            ========================== */}
            <TabsContent
              value='equipment'
              className='m-0 space-y-6'
            >
              <section className='flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between'>
                <div className='flex items-center gap-3'>
                  <span className='flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-secondary text-primary'>
                    <PackageCheck className='h-5 w-5' />
                  </span>

                  <div>
                    <h2 className='text-xl font-bold text-foreground'>
                      設備一覧
                    </h2>

                    <p className='mt-1 max-w-3xl text-body leading-6 text-muted-foreground'>
                      このプロジェクトに紐づく設備情報です。
                      仕様、稼働日、蓄電池、補助金情報を確認できます。
                    </p>
                  </div>
                </div>

                <Button
                  type='button'
                  className='shrink-0'
                  disabled={!siteId || !projectId}
                  onClick={() =>
                    navigate(paths.projectEquipment(siteId, projectId), {
                      state: {
                        submissionMode: 'create',
                        returnTo: `${paths.siteProjectDetail(
                          siteId,
                          projectId,
                        )}?${new URLSearchParams({
                          tab: 'equipment',
                        })}`,
                      },
                    })
                  }
                >
                  <Plus className='h-4 w-4' />
                  設備追加
                </Button>
              </section>

              {equipmentLoading ? (
                <SkeletonCard className='min-h-40 rounded-2xl' />
              ) : equipmentError ? (
                <Card className={projectDetailCardClassName}>
                  <CardContent
                    className='p-6 text-center text-destructive'
                    role='alert'
                  >
                    {equipmentError}
                  </CardContent>
                </Card>
              ) : equipmentList.length > 0 ? (
                <EquipmentList
                  siteId={siteId}
                  equipments={equipmentList}
                  onEditEquipment={(equipmentId) =>
                    navigate(
                      paths.siteProjectEquipmentEdit(
                        siteId,
                        projectId,
                        equipmentId,
                      ),
                    )
                  }
                />
              ) : (
                <Card className={projectDetailCardClassName}>
                  <CardContent className='p-10 text-center text-muted-foreground'>
                    設備情報がありません。
                  </CardContent>
                </Card>
              )}
            </TabsContent>
          </Tabs>

          <MonitoringCancellationDialog
            open={monitoringCancellationOpen}
            siteId={siteId}
            projectId={projectId}
            onOpenChange={setMonitoringCancellationOpen}
          />

          <FloatingHelpButton context='project-detail' />
        </div>
      </div>
    </div>
  );
};

export default ProjectDetail;
