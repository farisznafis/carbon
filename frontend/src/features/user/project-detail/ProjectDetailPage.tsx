import { useEffect, useState, type ReactNode } from 'react';
import { getProjectDetails, type ProjectDetail as ProjectDetailApi } from '@/api/projects';
import {
  getSiteProjectEquipment,
  type SiteProjectEquipment,
} from '@/api/site';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { getMonitoringRecent } from '@/api/monitoring';
import type { MonitoringData } from '@/api/monitoring';
import {
  getEmissionTrend,
  getProjectEmissionSummary,
  type EmissionTrendPoint,
  type ProjectEmissionBreakdown,
} from '@/api/batch';
import { logger } from '@/lib/logger';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import {
  dashboardPageContainerClassName,
  dashboardTabsListClassName,
  dashboardTabTriggerClassName,
} from '@/components/layout/DashboardLayout';
import { EquipmentList } from './components/EquipmentList';
import { MonitoringCancellationDialog } from './components/MonitoringCancellationDialog';
import { EmissionTrendChart } from './components/EmissionTrendChart';
import { EmissionBreakdownChart } from './components/EmissionBreakdownChart';
import { PROJECT_EMISSION_REDUCTION_TARGET } from './components/emissionReductionProgressUtils';
import { projectDetailCardClassName } from './projectDetailStyles';
import { cn } from '@/lib/utils';
import { paths } from '@/routes/paths';

function formatNumber(n: number | null | undefined) {
  if (n == null || Number.isNaN(n)) return '-';
  return new Intl.NumberFormat().format(n);
}

type MonitoringDisplayRow = Pick<
  MonitoringData,
  | 'projectId'
  | 'siteId'
  | 'dateRecorded'
  | 'ELpv_evidence'
  | 'ELPJ_battery_evidence'
> & {
  ELpv_value: number | null;
  ELpvr_value: number | null;
  ELPJ_battery_value: number | null;
  ELPJ_accuracy: number | null;
  ELPJ_control_value: number | null;
  changedBattery: boolean | null;
};

const fallbackMonitoringRow: MonitoringDisplayRow = {
  projectId: 0,
  siteId: null,
  dateRecorded: '-',
  ELpv_value: null,
  ELpvr_value: null,
  ELPJ_battery_value: null,
  ELPJ_accuracy: null,
  ELPJ_control_value: null,
  changedBattery: null,
};

type ProjectDetailTabValue = 'overview' | 'equipment';

function ProjectDetailTabs() {
  return (
    <TabsList className={dashboardTabsListClassName}>
      <TabsTrigger value='overview' className={dashboardTabTriggerClassName}>
        概要
      </TabsTrigger>
      <TabsTrigger value='equipment' className={dashboardTabTriggerClassName}>
        設備一覧
      </TabsTrigger>
    </TabsList>
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

type RecentMetricCardProps = {
  icon?: ReactNode;
  label: string;
  value: string;
  unit?: string;
};

function RecentMetricCard({
  icon,
  label,
  value,
  unit,
}: RecentMetricCardProps) {
  return (
    <div className='flex h-24 min-w-0 flex-col items-center justify-center gap-1 rounded-lg border border-border bg-card px-3 py-3 text-center shadow-app-sm'>
      <div className='text-table flex min-w-0 items-center justify-center gap-1 font-bold text-muted-foreground'>
        {icon}
        <span className='min-w-0 truncate'>{label}</span>
      </div>
      <div className='flex items-end justify-center gap-1'>
        <span className='text-2xl font-bold leading-none text-foreground'>
          {value}
        </span>
        {unit ? (
          <span className='text-table pb-0.5 text-muted-foreground'>
            {unit}
          </span>
        ) : null}
      </div>
    </div>
  );
}

type RecentDataBarProps = {
  row: MonitoringDisplayRow;
  cancellationListDisabled: boolean;
  onOpenCancellationList: () => void;
  onRegister: () => void;
};

function RecentDataBar({
  row,
  cancellationListDisabled,
  onOpenCancellationList,
  onRegister,
}: RecentDataBarProps) {
  // If battery was not replaced (false), show 0 for battery discharge value
  const batteryDischargeValue = row.changedBattery === false ? 0 : row.ELPJ_battery_value;
  
  return (
    <section className={`${projectDetailCardClassName} p-5`}>
      <div className='mb-3 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between'>
        <div className='text-table inline-flex w-fit items-center gap-1.5 rounded-full border border-border px-4 py-1.5 font-bold text-foreground'>
          <span className='text-muted-foreground'>記録日</span>
          {formatMonitoringDate(row.dateRecorded)}
        </div>
        <div className='flex flex-wrap items-center gap-2'>
          <Button
            type='button'
            variant='outline'
            onClick={onOpenCancellationList}
            size='md'
            disabled={cancellationListDisabled}
          >
            取消一覧
          </Button>
          <Button type='button' onClick={onRegister} size='md'>
            モニタリングデータを登録
          </Button>
        </div>
      </div>

      <div className='grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-5'>
        <RecentMetricCard
          icon={<Cpu className='h-5 w-5 shrink-0 text-primary' />}
          label='制御機器稼働時間'
          value={formatNumber(row.ELPJ_control_value)}
          unit='時間'
        />
        <RecentMetricCard
          icon={<Zap className='h-5 w-5 shrink-0 text-primary' />}
          label='発電量'
          value={formatNumber(row.ELpv_value)}
          unit='kWh'
        />
        <RecentMetricCard
          icon={<HousePlug className='h-5 w-5 shrink-0 text-primary' />}
          label='自家消費量'
          value={formatNumber(row.ELpvr_value)}
          unit='kWh'
        />
        <RecentMetricCard
          icon={<BatteryCharging className='h-5 w-5 shrink-0 text-primary' />}
          label='蓄電池放電量'
          value={formatNumber(batteryDischargeValue)}
          unit='kWh'
        />
        <RecentMetricCard
          icon={<ChartNoAxesColumn className='h-5 w-5 shrink-0 text-primary' />}
          label='測定精度'
          value={formatNumber(row.ELPJ_accuracy)}
          unit='%'
        />
      </div>
    </section>
  );
}

const ProjectDetail = () => {
  const { id, projectId: routeProjectId, siteId: routeSiteId } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const projectId = routeProjectId || id || '';
  const siteId = routeSiteId || searchParams.get('siteId') || '';

  type ProjectView = {
    name: string;
    description: string;
    methodology: string;
    projectNumber: string;
    membersTotal: number | null;
    status: 'active' | 'inactive';
    createdDate: string;
    location: string;
    devices: Array<{
      id: string;
      name: string;
      type: string;
      status: 'online' | 'offline';
    }>;
    recentData: Array<{
      date: string;
      parameter: string;
      value: string;
      status: 'compliant' | 'non-compliant';
    }>;
    documents: Array<{
      id: string;
      name: string;
      type: string;
      uploadDate: string;
      size: string;
    }>;
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
  const [equipmentList, setEquipmentList] = useState<SiteProjectEquipment[]>([]);
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
          projectNumber: result.projectNumber,
          name: result.projectName,
          description: result.methodDetails,
          methodology: `${result.methodologyCategory} ${result.methodologyNumber} ${result.methodologyVersion}`,
          status:
            String(result.projectStatus).toLowerCase() === 'active'
              ? 'active'
              : 'inactive',
          createdDate: result.createdAt ?? '',
          location: result.companyName || '-',
          membersTotal: result.membersTotal ?? null,
          devices: [],
          recentData: [],
          documents: [],
        });
      } catch (error) {
        logger.error('Error loading project detail:', error);
        setProject(null);
      } finally {
        setLoading(false);
      }
    };

    loadProject();
  }, [projectId]);

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
    recentMonitoring.length > 0 ? recentMonitoring : [fallbackMonitoringRow];

  if (loading) {
    return (
      <div className='min-h-screen flex items-center justify-center'>
        読み込み中...
      </div>
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
                navigate(siteId ? `/sites/${siteId}/dashboard` : '/master/dashboard')
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

  return (
    <div className='relative min-h-screen bg-background py-8'>
      <div className={dashboardPageContainerClassName}>
        <PageBackButton
          onClick={() => {
            if (siteId) {
              navigate(`/sites/${siteId}/dashboard`);
            } else {
              navigate(-1);
            }
          }}
          label=''
          className='absolute left-4 top-10 h-12 w-12 rounded-full p-0 sm:left-10 lg:left-16'
        />
        
        <div className='w-full space-y-7'>
          <header className='min-h-[66px] animate-slide-up'>
            <h1 className='text-foreground'>{project.name}</h1>
            <p className='text-body mt-2 text-muted-foreground'>{project.description}</p>
          </header>

          <Tabs
            defaultValue='overview'
            className='space-y-6'
            value={tab}
            onValueChange={(v) => setTab(v as ProjectDetailTabValue)}
          >
            <ProjectDetailTabs />

            <TabsContent
              value='overview'
              className='m-0 space-y-6'
            >
              <Card className={projectDetailCardClassName}>
                <CardHeader className='px-7 pb-3 pt-7'>
                  <CardTitle className='leading-8 text-foreground'>
                    プロジェクト概要
                  </CardTitle>
                </CardHeader>
                <CardContent className='px-7 pb-7'>
                  <dl className='grid grid-cols-1 gap-x-16 gap-y-5 md:grid-cols-2'>
                    <div className='flex items-start gap-3'>
                      <span className='mt-1 inline-flex h-5 w-5 shrink-0 items-center justify-center text-foreground'>
                        <MapPin className='h-5 w-5' />
                      </span>
                      <div>
                        <dt className='text-small text-muted-foreground'>
                          拠点
                        </dt>
                        <dd className='font-bold text-foreground'>
                          {project.location || '-'}
                        </dd>
                      </div>
                    </div>

                    <div className='flex items-start gap-3'>
                      <span className='mt-1 inline-flex h-5 w-5 shrink-0 items-center justify-center text-foreground'>
                        <Layers className='h-5 w-5' />
                      </span>
                      <div>
                        <dt className='text-small text-muted-foreground'>
                          方法論
                        </dt>
                        <dd className='break-words font-bold text-foreground'>
                          {project.methodology || '-'}
                        </dd>
                      </div>
                    </div>

                    <div className='flex items-start gap-3'>
                      <span className='mt-1 inline-flex h-5 w-5 shrink-0 items-center justify-center text-foreground'>
                        <Hash className='h-5 w-5' />
                      </span>
                      <div>
                        <dt className='text-small text-muted-foreground'>
                          プロジェクト番号
                        </dt>
                        <dd className='font-bold text-foreground'>
                          {project.projectNumber || '-'}
                        </dd>
                      </div>
                    </div>

                    <div className='flex items-start gap-3'>
                      <span className='mt-1 inline-flex h-5 w-5 shrink-0 items-center justify-center text-foreground'>
                        <Calendar className='h-5 w-5' />
                      </span>
                      <div>
                        <dt className='text-small text-muted-foreground'>
                          認証期間
                        </dt>
                        <dd className='font-bold text-foreground'>
                          {formatCreatedDate(project.createdDate)}
                        </dd>
                      </div>
                    </div>
                  </dl>
                </CardContent>
              </Card>

              {recentLoading ? (
                <div className={`${projectDetailCardClassName} text-subtle p-5 text-muted-foreground`}>
                  読み込み中...
                </div>
              ) : (
                <RecentDataBar
                  row={monitoringRows[0]}
                  cancellationListDisabled={!siteId || !projectId}
                  onOpenCancellationList={() =>
                    setMonitoringCancellationOpen(true)
                  }
                  onRegister={() =>
                    navigate('/monitoring/register/solar', {
                      state: { projectId, siteId },
                    })
                  }
                />
              )}

              <section className='grid grid-cols-1 items-start gap-6 lg:grid-cols-[1.35fr_0.95fr]'>
                <div className='h-full animate-slide-up'>
                  <EmissionTrendChart
                    data={emissionTrend}
                    isLoading={emissionTrendLoading}
                    error={emissionTrendError}
                    className={projectDetailCardClassName}
                  />
                </div>

                <div className='h-full animate-slide-up'>
                  <EmissionBreakdownChart
                    data={emissionBreakdown}
                    isLoading={emissionBreakdownLoading}
                    error={emissionBreakdownError}
                    reductionProgress={{
                      baselineEmission: projectBaselineEmission,
                      projectEmission: projectTotalEmission,
                      targetReduction: PROJECT_EMISSION_REDUCTION_TARGET,
                    }}
                    className={projectDetailCardClassName}
                  />
                </div>
              </section>
            </TabsContent>

            <TabsContent value='equipment' className='m-0 space-y-6'>
              <Card className={projectDetailCardClassName}>
                <CardHeader>
                  <div className='flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between'>
                    <div className='flex items-start gap-3'>
                      <span className='flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-secondary text-primary'>
                        <PackageCheck className='h-5 w-5' />
                      </span>
                      <div>
                        <CardTitle>設備一覧</CardTitle>
                        <CardDescription className='text-body mt-2'>
                          このプロジェクトに紐づく設備情報です。運用確認で必要になりやすい仕様、稼働日、蓄電池、補助金情報をまとめています。
                        </CardDescription>
                      </div>
                    </div>
                    <Button
                      type='button'
                      className='shrink-0 self-start sm:self-auto'
                      disabled={!siteId || !projectId}
                      onClick={() =>
                        navigate(paths.projectEquipment(siteId, projectId), {
                          state: {
                            returnTo: `${paths.siteProjectDetail(
                              siteId,
                              projectId,
                            )}?${new URLSearchParams({ tab: 'equipment' })}`,
                          },
                        })
                      }
                    >
                      <Plus className='h-4 w-4' />
                      設備追加
                    </Button>
                  </div>
                </CardHeader>
              </Card>

              {equipmentLoading ? (
                <div className={`${projectDetailCardClassName} text-subtle p-5 text-muted-foreground`}>
                  読み込み中...
                </div>
              ) : equipmentError ? (
                <div
                  className={`${projectDetailCardClassName} text-subtle p-5 text-destructive`}
                  role='alert'
                >
                  {equipmentError}
                </div>
              ) : equipmentList.length > 0 ? (
                <EquipmentList equipments={equipmentList} />
              ) : (
                <Card className={projectDetailCardClassName}>
                  <CardContent className='p-8 text-center text-muted-foreground'>
                    設備情報がありません。
                  </CardContent>
                </Card>
              )}
            </TabsContent>

            <TabsContent
              value='data'
              className='m-0 space-y-6'
            >
            <Card className={projectDetailCardClassName}>
              <CardHeader>
                <CardTitle>最近のモニタリングデータ</CardTitle>
                <CardDescription>直近の記録を表示します。</CardDescription>
              </CardHeader>
              <CardContent>
                {recentLoading ? (
                  <div className='text-table text-muted-foreground'>
                    Loading...
                  </div>
                ) : (
                  <ul className='divide-y rounded-lg border'>
                    {monitoringRows.map((row, index) => (
                      <li
                        key={`${row.projectId}-${row.siteId ?? 'na'}-${row.dateRecorded}-${index}`}
                        className='p-4'
                      >
                        <div className='flex flex-wrap items-center justify-between gap-3'>
                          <div className='text-table text-muted-foreground'>
                            {row.dateRecorded}
                          </div>
                          <div className='text-table flex flex-wrap gap-4'>
                            <span>
                              PV Gen:{' '}
                              <strong>
                                {formatNumber(row.ELpv_value)} kWh
                              </strong>
                            </span>
                            <span>
                              Provided:{' '}
                              <strong>
                                {formatNumber(row.ELpvr_value)} kWh
                              </strong>
                            </span>
                            <span>
                              Battery Discharge:{' '}
                              <strong>
                                {formatNumber(row.ELPJ_battery_value)} kWh
                              </strong>
                            </span>
                            <span>
                              Accuracy:{' '}
                              <strong>
                                {formatNumber(row.ELPJ_accuracy)} %
                              </strong>
                            </span>
                          </div>
                        </div>
                        <div className='text-subtle mt-2 grid grid-cols-2 gap-2 text-muted-foreground md:grid-cols-4'>
                          <div>
                            Control Runtime:{' '}
                            {formatNumber(row.ELPJ_control_value)} hours
                          </div>
                          <div>
                            Battery Replaced:{' '}
                            {row.changedBattery == null
                              ? '-'
                              : row.changedBattery
                                ? 'Yes'
                                : 'No'}
                          </div>
                          {row.ELpv_evidence && (
                            <div className='truncate'>
                              PV Evidence: {row.ELpv_evidence}
                            </div>
                          )}
                          {row.ELPJ_battery_evidence && (
                            <div className='truncate'>
                              Battery Evidence: {row.ELPJ_battery_evidence}
                            </div>
                          )}
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </CardContent>
            </Card>
            </TabsContent>

            <TabsContent value='documents' className='m-0 py-8'>
            <Card>
              <CardHeader>
                <CardTitle>関連書類</CardTitle>
                <CardDescription>
                  このプロジェクトに紐づく関連書類です。
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className='text-table text-muted-foreground'>
                  資料はまだありません。
                </div>
              </CardContent>
            </Card>
          </TabsContent>

        </Tabs>

        <MonitoringCancellationDialog
          open={monitoringCancellationOpen}
          siteId={siteId}
          projectId={projectId}
          onOpenChange={setMonitoringCancellationOpen}
        />
        </div>
      </div>
    </div>
  );
};

export default ProjectDetail;

