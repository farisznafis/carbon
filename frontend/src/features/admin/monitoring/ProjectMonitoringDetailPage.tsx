import { useCallback, useEffect, useState } from 'react';
import {
  useLocation,
  useNavigate,
  useParams,
  useSearchParams,
} from 'react-router-dom';
import {
  Activity,
  ExternalLink,
  FileText,
  RefreshCw,
} from 'lucide-react';
import {
  getAdminProjectMonitoringRecords,
  type MonitoringRecordListItem,
} from '@/api/monitoring';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { FormPageWrapper } from '@/components/layout/FormPageWrapper/FormPageWrapper';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { logger } from '@/lib/logger';
import { paths } from '@/routes/paths';
import { ProjectDetailItem } from '../project-detail/ProjectDetailItem';
import {
  formatMonitoringDate,
  formatMonitoringDateTime,
  formatMonitoringNumber,
  formatMonitoringValue,
  isMatchingMonitoringRecord,
} from './monitoringRecordDisplay';

type MonitoringDetailLocationState = {
  monitoringRecord?: MonitoringRecordListItem;
};

function EvidenceLink({
  label,
  url,
}: {
  label: string;
  url: string | null;
}) {
  if (!url) {
    return <ProjectDetailItem label={label} value="-" />;
  }

  return (
    <ProjectDetailItem
      label={label}
      value={
        <Button asChild type="button" variant="outline" size="sm">
          <a href={url} target="_blank" rel="noreferrer">
            <ExternalLink />
            ファイルを開く
          </a>
        </Button>
      }
    />
  );
}

function DetailSection({
  title,
  description,
  icon,
  children,
}: {
  title: string;
  description: string;
  icon: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-secondary text-primary">
            {icon}
          </span>
          <div>
            <CardTitle>{title}</CardTitle>
            <CardDescription>{description}</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <dl className="grid grid-cols-1 gap-x-8 gap-y-5 md:grid-cols-2">
          {children}
        </dl>
      </CardContent>
    </Card>
  );
}

export default function ProjectMonitoringDetailPage() {
  const { projectId, monitoringId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const page = Math.max(Number(searchParams.get('page')) || 1, 1);
  const limit = Math.max(Number(searchParams.get('limit')) || 10, 1);
  const locationRecord = (
    location.state as MonitoringDetailLocationState | null
  )?.monitoringRecord;
  const [record, setRecord] = useState<MonitoringRecordListItem | null>(
    locationRecord && monitoringId && projectId
      ? isMatchingMonitoringRecord(locationRecord, monitoringId) &&
        String(locationRecord.projectID) === projectId
        ? locationRecord
        : null
      : null,
  );
  const [isLoading, setIsLoading] = useState(!record);
  const [error, setError] = useState<string | null>(null);

  const loadMonitoringRecord = useCallback(async () => {
    if (!projectId || !monitoringId) {
      setError('モニタリングIDまたはプロジェクトIDが見つかりません。');
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      const result = await getAdminProjectMonitoringRecords({
        projectId,
        page,
        limit,
      });
      const matchedRecord =
        result.items.find((item) =>
          isMatchingMonitoringRecord(item, monitoringId),
        ) ?? null;

      setRecord(matchedRecord);
      if (!matchedRecord) {
        setError('対象のモニタリングデータが見つかりません。');
      }
    } catch (loadError) {
      logger.error('Failed to load monitoring record detail:', loadError);
      setRecord(null);
      setError(
        loadError instanceof Error
          ? loadError.message
          : 'モニタリングデータの取得に失敗しました。',
      );
    } finally {
      setIsLoading(false);
    }
  }, [limit, monitoringId, page, projectId]);

  useEffect(() => {
    if (!record) {
      void loadMonitoringRecord();
    }
  }, [loadMonitoringRecord, record]);

  const returnToList = () => {
    if (!projectId) {
      navigate(paths.adminDashboard);
      return;
    }

    navigate(
      `${paths.adminDashboard}?tab=monitoring&projectId=${encodeURIComponent(
        projectId,
      )}&monitoringPage=${page}&monitoringLimit=${limit}`,
    );
  };

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        読み込み中...
      </div>
    );
  }

  if (!record || error) {
    return (
      <FormPageWrapper
        pageName="モニタリング詳細"
        title=""
        onBack={returnToList}
        contentMaxWidth="max-w-3xl"
        wrapInCard={false}
      >
        <Card>
          <CardHeader>
            <CardTitle>データを表示できません</CardTitle>
            <CardDescription>
              {error || '対象のモニタリングデータが見つかりません。'}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button
              type="button"
              variant="outline"
              onClick={() => void loadMonitoringRecord()}
            >
              <RefreshCw />
              再読み込み
            </Button>
          </CardContent>
        </Card>
      </FormPageWrapper>
    );
  }

  return (
    <FormPageWrapper
      pageName="モニタリング詳細"
      title=""
      onBack={returnToList}
      contentMaxWidth="max-w-5xl"
      wrapInCard={false}
    >
      <Card>
        <CardHeader>
          <CardTitle>モニタリング詳細</CardTitle>
          <CardDescription>
            モニタリングID: {formatMonitoringValue(record.id)}
          </CardDescription>
        </CardHeader>
      </Card>

      <DetailSection
        title="基本情報"
        description="対象レコードと処理状況を確認できます。"
        icon={<Activity className="h-5 w-5" />}
      >
        <ProjectDetailItem label="モニタリングID" value={formatMonitoringValue(record.id)} />
        <ProjectDetailItem label="プロジェクトID" value={formatMonitoringValue(record.projectID)} />
        <ProjectDetailItem label="拠点ID" value={formatMonitoringValue(record.siteID)} />
        <ProjectDetailItem label="参加ユーザID" value={formatMonitoringValue(record.participatingUserID)} />
        <ProjectDetailItem label="方法論" value={formatMonitoringValue(record.methodologyType)} />
        <ProjectDetailItem label="記録日" value={formatMonitoringDate(record.dateRecorded)} />
        <ProjectDetailItem label="更新日時" value={formatMonitoringDateTime(record.timeUpdated)} />
        <ProjectDetailItem label="処理日時" value={formatMonitoringDateTime(record.processedAt)} />
        <ProjectDetailItem label="蓄電池変更" value={record.changedBattery ? 'あり' : 'なし'} />
        <ProjectDetailItem
          label="状態"
          value={
            <StatusBadge tone={record.isActive ? 'success' : 'neutral'}>
              {record.isActive ? '有効' : '無効'}
            </StatusBadge>
          }
        />
      </DetailSection>

      <DetailSection
        title="モニタリング値"
        description="登録された測定値を確認できます。"
        icon={<Activity className="h-5 w-5" />}
      >
        <ProjectDetailItem
          label="制御機器稼働時間 (ELPJ)"
          value={formatMonitoringNumber(record.elpjControlValue, '時間')}
        />
        <ProjectDetailItem
          label="蓄電池放電量 (ELPJ)"
          value={formatMonitoringNumber(record.elpjBatteryValue, 'kWh')}
        />
        <ProjectDetailItem
          label="測定精度 (ELPJ)"
          value={formatMonitoringNumber(record.elpjAccuracy, '%')}
        />
        <ProjectDetailItem
          label="発電量 (ELPV)"
          value={formatMonitoringNumber(record.elPv, 'kWh')}
        />
        <ProjectDetailItem
          label="自家消費量 (ELPVR)"
          value={formatMonitoringNumber(record.elPvr, 'kWh')}
        />
      </DetailSection>

      <DetailSection
        title="エビデンス情報"
        description="登録されたファイル情報と保存先を確認できます。"
        icon={<FileText className="h-5 w-5" />}
      >
        <ProjectDetailItem
          label="制御機器仕様ファイル"
          value={formatMonitoringValue(record.elpjControlEquipmentSpecFiles)}
        />
        <ProjectDetailItem
          label="蓄電池エビデンス"
          value={formatMonitoringValue(record.elpjBatteryEvidence)}
        />
        <ProjectDetailItem
          label="発電量エビデンス"
          value={formatMonitoringValue(record.elPvEvidence)}
        />
        <ProjectDetailItem
          label="自家消費量エビデンス"
          value={formatMonitoringValue(record.elPvrEvidence)}
        />
        <EvidenceLink label="発電量エビデンスURL" url={record.elPvEvidenceUrl} />
        <EvidenceLink label="自家消費量エビデンスURL" url={record.elPvrEvidenceUrl} />
        <EvidenceLink
          label="蓄電池仕様エビデンスURL"
          url={record.elpjBatterySpecificationUrl}
        />
        <EvidenceLink
          label="制御機器エビデンスURL"
          url={record.elpjControlEvidenceUrl}
        />
      </DetailSection>
    </FormPageWrapper>
  );
}
