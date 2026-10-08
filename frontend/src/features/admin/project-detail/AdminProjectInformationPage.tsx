import { Link, useParams } from 'react-router-dom';
import { ProfileDetailField } from '@/components/data-display/ProfileDetailField';
import { PageSkeleton, SkeletonCard } from '@/components/feedback/loading';
import {
  FormCard,
  FormSection,
  FormSectionStack,
} from '@/components/forms/form-layout';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { useAdminProjectDetailQuery } from '@/hooks/admin/useAdminProjectDetailQuery';
import { paths } from '@/routes/paths';
import { AdminProjectDetailLayout } from './AdminProjectDetailLayout';
import {
  formatProjectDetailDateTime,
  formatProjectDetailNumber,
  formatProjectDetailValue,
  formatProjectEmission,
  formatProjectPersonName,
} from './projectDetailDisplay';

/** Converts a nullable relationship flag into a readable Japanese status. */
function formatRelationshipStatus(
  value: boolean | null,
  enabledLabel: string,
  disabledLabel: string,
) {
  if (value == null) return '—';
  return value ? enabledLabel : disabledLabel;
}

/** Renders all read-only registration information for one admin project. */
export default function AdminProjectInformationPage() {
  const { projectId } = useParams();
  const projectQuery = useAdminProjectDetailQuery(projectId);
  const project = projectQuery.data;

  if (projectQuery.isLoading) {
    return (
      <PageSkeleton>
        <div className="mx-auto w-full max-w-admin space-y-5">
          <SkeletonCard className="min-h-24" />
          <SkeletonCard className="min-h-[40rem]" />
        </div>
      </PageSkeleton>
    );
  }

  if (!projectId || projectQuery.isError || !project) {
    return (
      <div className="flex min-h-screen items-center justify-center p-6">
        <Card className="w-full max-w-md">
          <CardHeader>
            <CardTitle>プロジェクトが見つかりません</CardTitle>
            <CardDescription>
              {projectQuery.error instanceof Error
                ? projectQuery.error.message
                : 'プロジェクト情報を取得できませんでした。'}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button asChild variant="outline">
              <Link to={paths.adminProjectList}>プロジェクト管理に戻る</Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <AdminProjectDetailLayout
      projectId={projectId}
      projectName={project.projectName}
      title="プロジェクト詳細情報"
      description="プロジェクトの登録情報を確認します。"
      currentBreadcrumb="詳細情報"
    >
      <FormCard>
        <FormSectionStack spacing="relaxed">
          <FormSection number={1} title="基本情報">
            <dl className="grid gap-x-10 gap-y-6 md:grid-cols-2">
              <ProfileDetailField
                label="プロジェクト名"
                value={formatProjectDetailValue(project.projectName)}
              />
              <ProfileDetailField
                label="プロジェクト番号"
                value={formatProjectDetailValue(project.projectNumber)}
              />
              <ProfileDetailField
                label="ステータス"
                value={formatProjectDetailValue(project.projectStatus)}
              />
              <ProfileDetailField
                label="会社名"
                value={formatProjectDetailValue(project.companyName)}
              />
              <ProfileDetailField
                label="地域区分"
                value={formatProjectDetailValue(project.regionalCategory)}
              />
              <ProfileDetailField
                label="管理担当"
                value={formatProjectDetailValue(project.dataManagementPic)}
              />
            </dl>
          </FormSection>

          <FormSection number={2} title="排出量">
            <dl className="grid gap-x-10 gap-y-6 md:grid-cols-2">
              <ProfileDetailField
                label="削減量"
                value={formatProjectEmission(project.emissionReduction)}
              />
              <ProfileDetailField
                label="ベースライン排出量"
                value={formatProjectEmission(
                  project.baselineEmissions ?? project.baselineEmission,
                )}
              />
              <ProfileDetailField
                label="プロジェクト実施後排出量"
                value={formatProjectEmission(
                  project.postProjectEmissions ?? project.postProjectEmission,
                )}
              />
            </dl>
          </FormSection>

          <FormSection number={3} title="代表者情報">
            <dl className="grid gap-x-10 gap-y-6 md:grid-cols-2">
              <ProfileDetailField
                label="氏名"
                value={formatProjectPersonName(
                  project.repLastName,
                  project.repFirstName,
                )}
              />
              <ProfileDetailField
                label="氏名（カナ）"
                value={formatProjectPersonName(
                  project.repLastNameKana,
                  project.repFirstNameKana,
                )}
              />
              <ProfileDetailField
                label="メールアドレス"
                value={formatProjectDetailValue(project.repEmail)}
              />
              <ProfileDetailField
                label="電話番号"
                value={formatProjectDetailValue(project.repPhoneNumber)}
              />
              <ProfileDetailField
                label="役職"
                value={formatProjectDetailValue(project.repPosition)}
              />
            </dl>
          </FormSection>

          <FormSection number={4} title="責任者情報">
            <dl className="grid gap-x-10 gap-y-6 md:grid-cols-2">
              <ProfileDetailField
                label="氏名"
                value={formatProjectPersonName(
                  project.picLastName,
                  project.picFirstName,
                )}
              />
              <ProfileDetailField
                label="氏名（カナ）"
                value={formatProjectPersonName(
                  project.picLastNameKana,
                  project.picFirstNameKana,
                )}
              />
              <ProfileDetailField
                label="メールアドレス"
                value={formatProjectDetailValue(project.picEmail)}
              />
              <ProfileDetailField
                label="電話番号"
                value={formatProjectDetailValue(project.picPhoneNumber)}
              />
              <ProfileDetailField
                label="役職"
                value={formatProjectDetailValue(project.picPosition)}
              />
            </dl>
          </FormSection>

          <FormSection number={5} title="社員数">
            <dl className="grid gap-x-10 gap-y-6 md:grid-cols-2">
              <ProfileDetailField
                label="新規社員数"
                value={formatProjectDetailNumber(project.newMembersTotal)}
              />
              <ProfileDetailField
                label="脱退数"
                value={formatProjectDetailNumber(project.withdrawnMembers)}
              />
              <ProfileDetailField
                label="累計社員数"
                value={formatProjectDetailNumber(project.cumulativeMembers)}
              />
              <ProfileDetailField
                label="社員数合計"
                value={formatProjectDetailNumber(project.membersTotal)}
              />
            </dl>
          </FormSection>

          <FormSection number={6} title="方法論">
            <dl className="grid gap-x-10 gap-y-6 md:grid-cols-2">
              <ProfileDetailField
                label="方法論区分"
                value={formatProjectDetailValue(project.methodologyCategory)}
              />
              <ProfileDetailField
                label="方法論番号"
                value={formatProjectDetailValue(project.methodologyNumber)}
              />
              <ProfileDetailField
                label="バージョン"
                value={formatProjectDetailValue(project.methodologyVersion)}
              />
              <ProfileDetailField
                label="方法論名称"
                value={formatProjectDetailValue(project.methodologyName)}
              />
              <ProfileDetailField
                label="排出源活動"
                value={formatProjectDetailValue(
                  project.methodologyMainEmission,
                )}
              />
              <ProfileDetailField
                label="組み合わせ可否"
                value={project.methodCombiFeasibility ? '可' : '不可'}
              />
              <div className="md:col-span-2">
                <ProfileDetailField
                  label="詳細"
                  value={formatProjectDetailValue(project.methodDetails)}
                />
              </div>
            </dl>
          </FormSection>

          <FormSection number={7} title="管理情報">
            <dl className="grid gap-x-10 gap-y-6 md:grid-cols-2">
              <ProfileDetailField
                label="運営会社ID"
                value={formatProjectDetailValue(project.operatorCompanyId)}
              />
              <ProfileDetailField
                label="企業入会状態"
                value={formatRelationshipStatus(
                  project.isCompanyJoined,
                  '入会済み',
                  '未入会',
                )}
              />
              <ProfileDetailField
                label="拠点関連状態"
                value={formatRelationshipStatus(
                  project.hasSiteRelation,
                  '関連あり',
                  '関連なし',
                )}
              />
              <ProfileDetailField
                label="作成日"
                value={formatProjectDetailDateTime(project.createdAt)}
              />
              <ProfileDetailField
                label="更新日"
                value={formatProjectDetailDateTime(project.updatedAt)}
              />
            </dl>
          </FormSection>
        </FormSectionStack>
      </FormCard>
    </AdminProjectDetailLayout>
  );
}
