import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { FileText } from 'lucide-react';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import {
  getAdminProjectDetail,
  type AdminProjectDetail,
} from '@/api/admin/projects';
import { FormPageWrapper } from '@/components/layout/FormPageWrapper/FormPageWrapper';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { ProjectDetailItem } from './ProjectDetailItem';
import { logger } from '@/lib/logger';
import { paths } from '@/routes/paths';

const formatValue = (value: unknown) => {
  if (value == null || value === '') return '-';
  return String(value);
};

const formatNumber = (value: unknown) => {
  if (value == null || value === '') return '-';
  const numberValue = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(numberValue)
    ? new Intl.NumberFormat().format(numberValue)
    : '-';
};

function DetailSection({
  title,
  description,
  children,
}: {
  title: string;
  description?: string;
  children: React.ReactNode;
}) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
        {description && <CardDescription>{description}</CardDescription>}
      </CardHeader>
      <CardContent className='grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-2'>
        {children}
      </CardContent>
    </Card>
  );
}

export default function ProjectDetailAdmin() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [project, setProject] = useState<AdminProjectDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      if (!projectId) {
        setError('projectId が見つかりません。');
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError(null);

        const result = await getAdminProjectDetail(projectId);
        setProject(result);
      } catch (e) {
        logger.error('Error loading admin project detail:', e);
        setProject(null);
        setError(e instanceof Error ? e.message : '取得に失敗しました。');
      } finally {
        setLoading(false);
      }
    };

    void load();
  }, [projectId]);

  if (loading) {
    return (
      <div className='flex min-h-screen items-center justify-center'>
        読み込み中...
      </div>
    );
  }

  if (!project || error || !projectId) {
    return (
      <div className='flex min-h-screen items-center justify-center p-6'>
        <Card className='max-w-md'>
          <CardHeader>
            <CardTitle>プロジェクトが見つかりません</CardTitle>
            <CardDescription>
              {error || 'データを取得できませんでした。'}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <PageBackButton onClick={() => navigate(-1)} />
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <FormPageWrapper
      pageName='プロジェクト詳細'
      title=''
      onBack={() => navigate(paths.adminDashboard)}
      backText='戻る'
      contentMaxWidth='max-w-5xl'
      wrapInCard={false}
    >
      <Card>
        <CardHeader className='space-y-4'>
          <div>
            <CardTitle>{formatValue(project.projectName)}</CardTitle>
            <CardDescription>
              プロジェクト番号: {formatValue(project.projectNumber)}
            </CardDescription>
          </div>
          <div className='flex flex-col gap-3 sm:flex-row'>
            <Button
              variant='outline'
              onClick={() => navigate(paths.adminProjectPlan(projectId))}
            >
              <FileText className='mr-2 h-4 w-4' />
              プロジェクト計画書へ
            </Button>
            <Button
              variant='outline'
              onClick={() => navigate(paths.adminProjectMonitoringReport(projectId))}
            >
              <FileText className='mr-2 h-4 w-4' />
              モニタリングデータ報告書
            </Button>
          </div>
        </CardHeader>
      </Card>

      <DetailSection
        title='基本情報'
        description='プロジェクトの基本情報を確認できます。'
      >
        <ProjectDetailItem label='ステータス' value={formatValue(project.projectStatus)} />
        <ProjectDetailItem label='会社名' value={formatValue(project.companyName)} />
        <ProjectDetailItem label='地域区分' value={formatValue(project.regionalCategory)} />
        <ProjectDetailItem label='管理担当' value={formatValue(project.dataManagementPic)} />
      </DetailSection>

      <DetailSection
        title='排出量'
        description='排出量関連の指標を確認できます。'
      >
        <ProjectDetailItem label='削減量' value={formatNumber(project.emissionReduction)} />
        <ProjectDetailItem
          label='ベースライン'
          value={formatNumber(project.baselineEmissions ?? project.baselineEmission)}
        />
        <ProjectDetailItem
          label='事後排出量'
          value={formatNumber(project.postProjectEmissions ?? project.postProjectEmission)}
        />
      </DetailSection>

      <DetailSection
        title='代表者情報'
        description='代表者の連絡先と所属情報を確認できます。'
      >
        <ProjectDetailItem
          label='氏名'
          value={`${formatValue(project.repLastName)} ${formatValue(project.repFirstName)}`}
        />
        <ProjectDetailItem
          label='氏名カナ'
          value={`${formatValue(project.repLastNameKana)} ${formatValue(project.repFirstNameKana)}`}
        />
        <ProjectDetailItem label='メール' value={formatValue(project.repEmail)} />
        <ProjectDetailItem label='電話' value={formatValue(project.repPhoneNumber)} />
        <ProjectDetailItem label='役職' value={formatValue(project.repPosition)} />
      </DetailSection>

      <DetailSection
        title='担当者情報'
        description='担当者の連絡先と所属情報を確認できます。'
      >
        <ProjectDetailItem
          label='氏名'
          value={`${formatValue(project.picLastName)} ${formatValue(project.picFirstName)}`}
        />
        <ProjectDetailItem
          label='氏名カナ'
          value={`${formatValue(project.picLastNameKana)} ${formatValue(project.picFirstNameKana)}`}
        />
        <ProjectDetailItem label='メール' value={formatValue(project.picEmail)} />
        <ProjectDetailItem label='電話' value={formatValue(project.picPhoneNumber)} />
        <ProjectDetailItem label='役職' value={formatValue(project.picPosition)} />
      </DetailSection>

      <DetailSection
        title='会員数'
        description='登録・脱退・累計会員数を確認できます。'
      >
        <ProjectDetailItem label='新規会員数' value={formatNumber(project.newMembersTotal)} />
        <ProjectDetailItem label='脱退数' value={formatNumber(project.withdrawnMembers)} />
        <ProjectDetailItem label='累計会員数' value={formatNumber(project.cumulativeMembers)} />
        <ProjectDetailItem label='会員数合計' value={formatNumber(project.membersTotal)} />
      </DetailSection>

      <DetailSection title='方法論' description='適用方法論の詳細です。'>
        <ProjectDetailItem label='方法論区分' value={formatValue(project.methodologyCategory)} />
        <ProjectDetailItem label='方法論番号' value={formatValue(project.methodologyNumber)} />
        <ProjectDetailItem label='バージョン' value={formatValue(project.methodologyVersion)} />
        <ProjectDetailItem label='方法論名称' value={formatValue(project.methodologyName)} />
        <ProjectDetailItem label='排出源活動' value={formatValue(project.methodologyMainEmission)} />
        <ProjectDetailItem
          label='組み合わせ可否'
          value={project.methodCombiFeasibility ? '可' : '不可'}
        />
        <ProjectDetailItem
          label='詳細'
          value={formatValue(project.methodDetails)}
          fullWidth
        />
      </DetailSection>

      <DetailSection
        title='管理情報'
        description='管理用のメタ情報を確認できます。'
      >
        <ProjectDetailItem label='運営会社ID' value={formatValue(project.operatorCompanyId)} />
        <ProjectDetailItem
          label='参加状態'
          value={project.isJoined == null ? '-' : project.isJoined ? '参加中' : '未参加'}
        />
        <ProjectDetailItem label='作成日' value={formatValue(project.createdAt)} />
        <ProjectDetailItem label='更新日' value={formatValue(project.updatedAt)} />
      </DetailSection>
    </FormPageWrapper>
  );
}
