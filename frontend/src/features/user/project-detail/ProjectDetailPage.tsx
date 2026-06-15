import { useEffect, useState } from 'react';
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
  Calendar,
  Hash,
  Info,
  Layers,
  MapPin,
  Users,
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
    'overview' | 'data' | 'documents' | 'registration-history'
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
              ダッシュボードに戻る
            </Button>
            <Button
              onClick={() =>
                navigate('/monitoring/register/solar', {
                  state: { projectId, siteId },
                })
              }
            >
              <ArrowRight className='h-4 w-4 mr-2' />
              モニタリング登録テスト（太陽光）
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className='min-h-screen py-8'>
      <div className='container mx-auto px-4'>
        <PageBackButton
          onClick={() => {
            if (siteId) {
              navigate(`/sites/${siteId}/dashboard`);
            } else {
              navigate(-1);
            }
          }}
          label='戻る'
          className='mb-4 rounded-full'
        />
        <div className='mb-6 animate-slide-up'>
          <div className='flex items-start justify-between mb-4'>
            <div>
              <h1 className='mb-2'>{project.name}</h1>
              <p className='text-muted-foreground text-lg'>
                {project.description}
              </p>
            </div>
          </div>
        </div>

        {/* Contribution Timeline Graph */}
        {/* <div className='mb-12 animate-slide-up'>
          <ContributionTimeline />
        </div> */}

        <Tabs
          defaultValue='overview'
          className='space-y-6'
          value={tab}
          onValueChange={(v) => setTab(v as typeof tab)}
        >
          <div className='flex w-full justify-center'>
            <TabsList className='flex items-center justify-center gap-2 rounded-lg bg-muted p-1.5 shadow-sm'>
              <TabsTrigger
                value='overview'
                className='min-w-[140px] rounded-md px-6 py-2.5 text-sm font-medium transition-all data-[state=active]:bg-background data-[state=active]:text-foreground data-[state=active]:shadow'
              >
                概要
              </TabsTrigger>

              <TabsTrigger
                value='registration-history'
                className='min-w-[140px] rounded-md px-6 py-2.5 text-sm font-medium transition-all data-[state=active]:bg-background data-[state=active]:text-foreground data-[state=active]:shadow'
              >
                登録履歴
              </TabsTrigger>
            </TabsList>
          </div>

          <TabsContent
            value='overview'
            className='space-y-6'
          >
            <Card>
              <CardHeader className='space-y-2'>
                <div className='flex items-start justify-between gap-4'>
                  <div>
                    <CardTitle className='text-xl'>プロジェクト概要</CardTitle>
                    <CardDescription>
                      プロジェクトの基本情報と詳細を確認できます。
                    </CardDescription>
                  </div>
                </div>

                <div className='mt-2 border rounded-lg p-4 bg-muted/30'>
                  <dl className='grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-4'>
                    <div className='flex items-start gap-3'>
                      <span className='mt-1 inline-flex h-7 w-7 items-center justify-center rounded bg-muted'>
                        <MapPin className='h-4 w-4 text-muted-foreground' />
                      </span>
                      <div>
                        <dt className='text-xs text-muted-foreground tracking-wide'>
                          拠点
                        </dt>
                        <dd className='font-medium'>
                          {project.location || '-'}
                        </dd>
                      </div>
                    </div>

                    <div className='flex items-start gap-3'>
                      <span className='mt-1 inline-flex h-7 w-7 items-center justify-center rounded bg-muted'>
                        <Layers className='h-4 w-4 text-muted-foreground' />
                      </span>
                      <div>
                        <dt className='text-xs text-muted-foreground tracking-wide'>
                          方法論
                        </dt>
                        <dd className='font-medium break-words'>
                          {project.methodology || '-'}
                        </dd>
                      </div>
                    </div>

                    <div className='flex items-start gap-3'>
                      <span className='mt-1 inline-flex h-7 w-7 items-center justify-center rounded bg-muted'>
                        <Hash className='h-4 w-4 text-muted-foreground' />
                      </span>
                      <div>
                        <dt className='text-xs text-muted-foreground tracking-wide'>
                          プロジェクト番号
                        </dt>
                        <dd className='font-medium'>
                          {project.projectNumber || '-'}
                        </dd>
                      </div>
                    </div>

                    <div className='flex items-start gap-3'>
                      <span className='mt-1 inline-flex h-7 w-7 items-center justify-center rounded bg-muted'>
                        <Calendar className='h-4 w-4 text-muted-foreground' />
                      </span>
                      <div>
                        <dt className='text-xs text-muted-foreground tracking-wide'>
                          認証期間
                        </dt>
                        <dd className='font-medium'>
                          {project.createdDate ? project.createdDate : '-'}
                          {/* <span className="text-muted-foreground"> ~（後で調整）</span> */}
                        </dd>
                      </div>
                    </div>

                    <div className='md:col-span-2'>
                      <div className='flex items-start gap-3'>
                        <span className='mt-1 inline-flex h-7 w-7 items-center justify-center rounded bg-muted'>
                          <Info className='h-4 w-4 text-muted-foreground' />
                        </span>
                        <div className='w-full'>
                          <dt className='text-xs text-muted-foreground tracking-wide'>
                            概要（削減方法）
                          </dt>
                          <dd className='font-medium text-sm leading-6 mt-1 line-clamp-3'>
                            {project.description || '-'}
                          </dd>
                        </div>
                      </div>
                    </div>

                    {/* <div className='flex items-start gap-3'>
                      <span className='mt-1 inline-flex h-7 w-7 items-center justify-center rounded bg-muted'>
                        <Users className='h-4 w-4 text-muted-foreground' />
                      </span>
                      <div>
                        <dt className='text-xs text-muted-foreground tracking-wide'>
                          会員数
                        </dt>
                        <dd className='font-medium'>
                          {project.membersTotal ?? '-'}
                        </dd>
                      </div>
                    </div> */}
                  </dl>
                </div>
              </CardHeader>

              {/* Recent Monitoring Data */}
              <CardHeader>
                <CardTitle>最近のモニタリングデータ</CardTitle>
                <CardDescription>直近の記録を表示します。</CardDescription>
              </CardHeader>
              <CardContent>
                {recentLoading ? (
                  <div className='text-sm text-muted-foreground'>
                    読み込み中...
                  </div>
                ) : (
                  <ul className='divide-y rounded-lg border'>
                    {monitoringRows.map((row, index) => (
                      <li
                        key={`${row.projectId}-${row.siteId ?? 'na'}-${row.dateRecorded}-${index}`}
                        className='p-4'
                      >
                        <div className='flex flex-wrap items-center justify-between gap-3'>
                          <div className='text-sm text-muted-foreground'>
                            {row.dateRecorded}
                          </div>
                          <div className='flex flex-wrap gap-4 text-sm'>
                            <span>
                              発電量:{' '}
                              <strong>
                                {formatNumber(row.ELpv_value)} kWh
                              </strong>
                            </span>
                            <span>
                              自家消費量:{' '}
                              <strong>
                                {formatNumber(row.ELpvr_value)} kWh
                              </strong>
                            </span>
                            <span>
                              蓄電池放電量:{' '}
                              <strong>
                                {formatNumber(row.ELPJ_battery_value)} kWh
                              </strong>
                            </span>
                            <span>
                              測定精度:{' '}
                              <strong>
                                {formatNumber(row.ELPJ_accuracy)} %
                              </strong>
                            </span>
                          </div>
                        </div>
                        <div className='mt-2 grid grid-cols-2 gap-2 text-xs text-muted-foreground md:grid-cols-4'>
                          <div>
                            制御機器稼働時間:{' '}
                            {formatNumber(row.ELPJ_control_value)} 時間
                          </div>
                          <div>
                            蓄電池交換:{' '}
                            {row.changedBattery == null
                              ? '-'
                              : row.changedBattery
                                ? '有'
                                : '無'}
                          </div>
                          {row.ELpv_evidence && (
                            <div className='truncate'>
                              発電量エビデンス: {row.ELpv_evidence}
                            </div>
                          )}
                          {row.ELPJ_battery_evidence && (
                            <div className='truncate'>
                              蓄電池エビデンス: {row.ELPJ_battery_evidence}
                            </div>
                          )}
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </CardContent>

              {/* Monitoring Data Registration Button */}
              <CardContent>
                <div className='flex justify-center'>
                  <Button
                    onClick={() =>
                      navigate('/monitoring/register/solar', {
                        state: { projectId, siteId },
                      })
                    }
                  >
                    モニタリングデータを登録
                  </Button>
                </div>
              </CardContent>

              {/* Dummy Graph */}
              <CardHeader>
                <section className='grid grid-cols-1 lg:grid-cols-2 gap-6'>
                  {/* Contribution Timeline Graph */}
                  <div className='mb-12 animate-slide-up'>
                    <ContributionTimeline />
                  </div>
                  {/* <Card>
                    <CardHeader>
                      <CardTitle>排出削減量</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className='h-[280px] rounded-lg border bg-muted/30 flex items-center justify-center'>
                        Chart Area (チャートエリア)
                      </div>
                    </CardContent>
                  </Card> */}

                  <Card>
                    <CardHeader>
                      <CardTitle>見積</CardTitle>
                    </CardHeader>
                    <CardContent className='space-y-4'>
                      <div className='h-[180px] rounded-lg border bg-muted/30 flex items-center justify-center'>
                        Estimation Graphic (推定グラフ)
                      </div>

                      <div className='space-y-1'>
                        <div>排出量削減： 1,000 tCO2</div>
                        <div>取得クレジット（再エネ）</div>
                        <div className='text-4xl font-bold text-cyan-500'>
                          ~100
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                </section>
              </CardHeader>
            </Card>
          </TabsContent>

          <TabsContent
            value='data'
            className='space-y-6'
          >
            <Card>
              <CardHeader>
                <CardTitle>最近のモニタリングデータ</CardTitle>
                <CardDescription>直近の記録を表示します。</CardDescription>
              </CardHeader>
              <CardContent>
                {recentLoading ? (
                  <div className='text-sm text-muted-foreground'>
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
                          <div className='text-sm text-muted-foreground'>
                            {row.dateRecorded}
                          </div>
                          <div className='flex flex-wrap gap-4 text-sm'>
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
                        <div className='mt-2 grid grid-cols-2 gap-2 text-xs text-muted-foreground md:grid-cols-4'>
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

          <TabsContent value='documents'>
            <Card>
              <CardHeader>
                <CardTitle>関係書類</CardTitle>
                <CardDescription>
                  このプロジェクトに紐づく関連書類です。
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className='text-sm text-muted-foreground'>
                  資料はまだありません。
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value='registration-history'>
            <Card>
              <CardHeader>
                <CardTitle>登録履歴</CardTitle>
                <CardDescription>
                  このプロジェクトに紐づく拠点登録の履歴です。
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className='overflow-x-auto'>
                  <table className='w-full table-fixed border-collapse text-sm'>
                    <thead>
                      <tr className='border-b'>
                        <th className='text-left p-3 w-[18%]'>拠点名</th>
                        <th className='text-left p-3 w-[14%]'>申請日時</th>
                        <th className='text-left p-3 w-[10%]'>ステータス</th>
                        <th className='text-left p-3 w-[48%]'>取り消す理由</th>
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
                                取り消す依頼
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
          </TabsContent>
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
              <p className='text-sm font-medium'>理由（必須）</p>
              <Textarea
                value={cancelReasonInput}
                onChange={(e) => setCancelReasonInput(e.target.value)}
                placeholder='例）誤った値で登録したため、再登録したい。'
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
  );
};

export default ProjectDetail;
