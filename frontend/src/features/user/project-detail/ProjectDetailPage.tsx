import { useEffect, useState, type ReactNode } from 'react';
import { getProjectDetails, type ProjectDetail as ProjectDetailApi } from '@/api/projects';
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
  Zap,
} from 'lucide-react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';
import { Textarea } from '@/components/ui/textarea';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { mockRegistrationHistory } from './mockProjectDetailData';
import { getMonitoringRecent } from '@/api/monitoring';
import type { MonitoringData } from '@/api/monitoring';
import ContributionTimeline from '@/components/data-display/ContributionTimeline';
import { logger } from '@/lib/logger';
import { usePagination } from '@/hooks/usePagination';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { cn } from '@/lib/utils';

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

const projectDetailCardClassName =
  'rounded-2xl border border-border bg-card shadow-app-sm';

type ProjectDetailTabValue = 'overview' | 'data' | 'documents' | 'registration-history';

function ProjectDetailTabs() {
  const triggerClassName = cn(
    'h-9 rounded px-5 text-sm font-medium text-primary-foreground shadow-none transition-colors',
    'hover:bg-white/10 hover:text-white',
    'data-[state=active]:bg-card data-[state=active]:text-foreground data-[state=active]:shadow-app-sm',
  );

  return (
    <div className='flex justify-center'>
      <TabsList className='h-12 rounded-lg bg-primary p-1.5 text-primary-foreground'>
        <TabsTrigger value='overview' className={triggerClassName}>
          概要
        </TabsTrigger>
        <TabsTrigger value='data' className={triggerClassName}>
          モニタリングデータ
        </TabsTrigger>
      </TabsList>
    </div>
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
  onRegister: () => void;
};

function RecentDataBar({ row, onRegister }: RecentDataBarProps) {
  // If battery was not replaced (false), show 0 for battery discharge value
  const batteryDischargeValue = row.changedBattery === false ? 0 : row.ELPJ_battery_value;
  
  return (
    <section className={`${projectDetailCardClassName} p-5`}>
      <div className='mb-3 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between'>
        <div className='text-table inline-flex w-fit items-center rounded-full border border-border px-4 py-1.5 font-bold text-foreground'>
          {formatMonitoringDate(row.dateRecorded)}
        </div>
        <Button
          type='button'
          onClick={onRegister}
          size='sm'
        >
          モニタリングデータを登録
        </Button>
      </div>

      <div className='grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-5'>
        <RecentMetricCard
          icon={<Cpu className='h-5 w-5 shrink-0 text-[#699267]' />}
          label='制御機器稼働時間'
          value={formatNumber(row.ELPJ_control_value)}
          unit='時間'
        />
        <RecentMetricCard
          icon={<Zap className='h-5 w-5 shrink-0 text-[#699267]' />}
          label='発電量'
          value={formatNumber(row.ELpv_value)}
          unit='kWh'
        />
        <RecentMetricCard
          icon={<HousePlug className='h-5 w-5 shrink-0 text-[#699267]' />}
          label='自家消費量'
          value={formatNumber(row.ELpvr_value)}
          unit='kWh'
        />
        <RecentMetricCard
          icon={<BatteryCharging className='h-5 w-5 shrink-0 text-[#699267]' />}
          label='蓄電池放電量'
          value={formatNumber(batteryDischargeValue)}
          unit='kWh'
        />
        <RecentMetricCard
          icon={<ChartNoAxesColumn className='h-5 w-5 shrink-0 text-[#699267]' />}
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

  const [tab, setTab] = useState<
    ProjectDetailTabValue
  >('overview');
  const [recentMonitoring, setRecentMonitoring] = useState<MonitoringData[]>(
    [],
  );
  const [recentLoading, setRecentLoading] = useState(false);
  const [recentHasFetched, setRecentHasFetched] = useState(false);

  useEffect(() => {
    setRecentHasFetched(false);
    setRecentMonitoring([]);
  }, [projectId, siteId]);

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

  type RegistrationStatus = 'ACTIVE' | 'APPROVED' | 'REJECTED' | 'PENDING';

  type RegistrationHistoryItem = {
    id: string;
    projectName: string;
    siteName: string;
    dateTime: string;
    status: RegistrationStatus;
    cancelReason?: string;
  };

  const [registrationHistory, setRegistrationHistory] = useState(
    mockRegistrationHistory,
  );

  const [cancelDialogOpen, setCancelDialogOpen] = useState(false);
  const [selectedHistoryId, setSelectedHistoryId] = useState<string | null>(
    null,
  );
  const [cancelReasonInput, setCancelReasonInput] = useState('');

  const openCancelDialog = (historyId: string) => {
    setSelectedHistoryId(historyId);
    setCancelReasonInput('');
    setCancelDialogOpen(true);
  };

  const submitCancelRequest = () => {
    if (!selectedHistoryId || !cancelReasonInput.trim()) return;

    setRegistrationHistory((prev) =>
      prev.map((item) =>
        item.id === selectedHistoryId
          ? {
              ...item,
              status: 'PENDING',
              cancelReason: cancelReasonInput.trim(),
            }
          : item,
      ),
    );

    setCancelDialogOpen(false);
    setSelectedHistoryId(null);
    setCancelReasonInput('');
  };

  const getStatusClassName = (status: RegistrationStatus) => {
    switch (status) {
      case 'APPROVED':
        return 'bg-emerald-100 text-emerald-800 border-emerald-300';
      case 'REJECTED':
        return 'bg-red-100 text-red-800 border-red-300';
      case 'ACTIVE':
        return 'bg-sky-100 text-sky-800 border-sky-300';
      case 'PENDING':
      default:
        return 'bg-amber-100 text-amber-800 border-amber-300';
    }
  };

  const getStatusLabel = (status: RegistrationStatus) => {
    switch (status) {
      case 'APPROVED':
        return 'APPROVED';
      case 'REJECTED':
        return 'REJECTED';
      case 'ACTIVE':
        return 'ACTIVE';
      case 'PENDING':
      default:
        return 'PENDING';
    }
  };

  const getReasonDisplay = (item: RegistrationHistoryItem) => {
    if (item.status === 'ACTIVE') return '-';
    return item.cancelReason || '-';
  };

  const {
    currentPage,
    totalPages,
    totalItems,
    itemsPerPage,
    paginatedItems: paginatedHistory,
    goToPage,
    setItemsPerPage,
  } = usePagination(registrationHistory);

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
      <div className='container mx-auto px-4'>
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
        
        <div className='mx-auto w-full max-w-[1280px] space-y-7'>
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
                      <span className='mt-1 inline-flex h-5 w-5 shrink-0 items-center justify-center text-[#1F2628]'>
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
                      <span className='mt-1 inline-flex h-5 w-5 shrink-0 items-center justify-center text-[#1F2628]'>
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
                      <span className='mt-1 inline-flex h-5 w-5 shrink-0 items-center justify-center text-[#1F2628]'>
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
                      <span className='mt-1 inline-flex h-5 w-5 shrink-0 items-center justify-center text-[#1F2628]'>
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
                  onRegister={() =>
                    navigate('/monitoring/register/solar', {
                      state: { projectId, siteId },
                    })
                  }
                />
              )}

              <section className='grid grid-cols-1 items-stretch gap-6 lg:grid-cols-[1.35fr_0.95fr]'>
                <div className='h-full animate-slide-up'>
                  <ContributionTimeline className={`${projectDetailCardClassName} min-h-[360px]`} />
                </div>

                <Card className={`${projectDetailCardClassName} flex h-full min-h-[360px] flex-col`}>
                  <CardHeader className='px-7 pb-1 pt-7'>
                    <CardTitle className='leading-8 text-foreground'>
                      見積 (Estimation Trend)
                    </CardTitle>
                  </CardHeader>
                  <CardContent className='flex flex-1 flex-col space-y-4 px-7 pb-7'>
                    <div className='flex min-h-[210px] flex-1 flex-col items-center justify-center rounded-xl border-2 border-dashed border-border bg-muted text-center'>
                      <ChartNoAxesColumn className='mb-3 h-8 w-8 text-primary' />
                      <p className='font-semibold'>Estimation Graphic (推定グラフ)</p>
                      <p className='text-subtle text-muted-foreground'>
                        機械学習モデルによる今後の削減予測
                      </p>
                    </div>

                    <div className='space-y-3'>
                      <div className='flex items-center justify-between gap-4'>
                        <span className='text-muted-foreground'>排出量削減目標</span>
                        <span className='font-bold text-foreground'>1,000 tCO2</span>
                      </div>
                      <div className='h-2 rounded-full bg-muted'>
                        <div className='h-2 w-5/6 rounded-full bg-primary' />
                      </div>
                      <p className='text-subtle text-right font-bold text-primary'>達成率 83%</p>
                    </div>
                  </CardContent>
                </Card>
              </section>
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

            {/* 登録履歴 content */}
            {/* <TabsContent value='registration-history' className='m-0 py-8'>
            <Card>
              <CardHeader>
                <CardTitle>登録履歴</CardTitle>
                <CardDescription>
                  このプロジェクトに紐づく拠点登録の履歴です。
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className='overflow-x-auto'>
                  <table className='text-table w-full table-fixed border-collapse'>
                    <thead>
                      <tr className='border-b'>
                        <th className='text-left p-3 w-[18%]'>拠点名</th>
                        <th className='text-left p-3 w-[14%]'>申請日時</th>
                        <th className='text-left p-3 w-[10%]'>ステータス</th>
                        <th className='text-left p-3 w-[48%]'>取り消し理由</th>
                        <th className='text-left p-3 w-[10%]'>操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      {paginatedHistory.map((item) => (
                        <tr
                          key={item.id}
                          className='border-b align-top'
                        >
                          <td className='p-3'>{item.siteName}</td>
                          <td className='p-3'>{item.dateTime}</td>
                          <td className='p-3'>
                            <Badge className={getStatusClassName(item.status)}>
                              {getStatusLabel(item.status)}
                            </Badge>
                          </td>
                          <td className='p-3 text-muted-foreground'>
                            {getReasonDisplay(item)}
                          </td>
                          <td className='p-3'>
                            {item.status === 'ACTIVE' ? (
                              <Button
                                variant='outline'
                                size='sm'
                                onClick={() => openCancelDialog(item.id)}
                              >
                                取り消し依頼
                              </Button>
                            ) : (
                              <Button
                                variant='outline'
                                size='sm'
                                disabled
                              >
                                申請中
                              </Button>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <div className='mt-4'>
                  <PaginationControls
                    currentPage={currentPage}
                    totalPages={totalPages}
                    totalItems={totalItems}
                    itemsPerPage={itemsPerPage}
                    onPageChange={goToPage}
                    onItemsPerPageChange={setItemsPerPage}
                  />
                </div>
              </CardContent>
            </Card>
            </TabsContent> */}
        </Tabs>

        <AlertDialog
          open={cancelDialogOpen}
          onOpenChange={setCancelDialogOpen}
        >
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>取り消し依頼を送信しますか？</AlertDialogTitle>
              <AlertDialogDescription>
                このプロジェクト参加の取り消し依頼を送信します。理由を入力してください。
              </AlertDialogDescription>
            </AlertDialogHeader>

            <div className='space-y-2'>
              <p className='text-sm font-medium'>
                理由 <span className='text-destructive'>*</span>
              </p>
              <Textarea
                value={cancelReasonInput}
                onChange={(e) => setCancelReasonInput(e.target.value)}
                placeholder='理由を入力してください'
                rows={4}
              />
            </div>

            <AlertDialogFooter>
              <AlertDialogCancel>キャンセル</AlertDialogCancel>
              <AlertDialogAction
                onClick={submitCancelRequest}
                disabled={!cancelReasonInput.trim()}
              >
                依頼を送信
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
        </div>
      </div>
    </div>
  );
};

export default ProjectDetail;

