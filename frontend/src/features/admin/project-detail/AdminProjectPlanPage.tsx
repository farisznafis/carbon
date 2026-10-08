import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { getProjectPlanForm } from '@/api/admin/projects';
import { PageSkeleton, SkeletonCard } from '@/components/feedback/loading';
import { FormCard, FormSection } from '@/components/forms/form-layout';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { PlanFileList } from '@/features/admin/project-plan/PlanFileList';
import { PlanSummaryView } from '@/features/admin/project-plan/PlanSummaryView';
import { PLAN_SUMMARY_SLUG, fromPlanPageData } from '@/features/admin/project-plan/planSummary';
import { useAdminProjectDetailQuery } from '@/hooks/admin/useAdminProjectDetailQuery';
import { paths } from '@/routes/paths';
import { AdminProjectDetailLayout } from './AdminProjectDetailLayout';

/**
 * プロジェクト計画書, read-only: the uploaded plan files and the plan values
 * the system keeps. 更新 opens the project edit page, where both are changed.
 */
export default function AdminProjectPlanPage() {
  const { projectId } = useParams();
  const projectQuery = useAdminProjectDetailQuery(projectId);
  const project = projectQuery.data;

  const planQuery = useQuery({
    queryKey: ['admin', 'projects', projectId ?? '', 'plan-summary'],
    queryFn: () => getProjectPlanForm(projectId as string, PLAN_SUMMARY_SLUG),
    enabled: Boolean(projectId),
    retry: false,
  });

  if (projectQuery.isLoading) {
    return (
      <PageSkeleton>
        <div className="mx-auto w-full max-w-admin space-y-5">
          <SkeletonCard className="min-h-24" />
          <SkeletonCard className="min-h-[24rem]" />
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

  const renderPlanValues = () => {
    if (planQuery.isLoading) {
      return (
        <div role="status" aria-label="認証対象期間・会員数計画を読み込んでいます">
          <SkeletonCard className="min-h-32" />
        </div>
      );
    }
    if (planQuery.isError) {
      return (
        <p role="alert" className="text-sm text-destructive">
          {planQuery.error instanceof Error
            ? planQuery.error.message
            : '認証対象期間・会員数計画を取得できませんでした。'}
        </p>
      );
    }
    return <PlanSummaryView values={fromPlanPageData(planQuery.data?.data).values} />;
  };

  return (
    <AdminProjectDetailLayout
      projectId={projectId}
      projectName={project.projectName}
      title="プロジェクト計画書"
      description="アップロードされた計画書ファイルと、認証対象期間・会員数計画を確認します。"
      currentBreadcrumb="計画書"
    >
      <FormCard>
        <FormSection
          title="計画書ファイル"
          description="Excelで作成したプロジェクト計画書です。新しいものから順に表示します。"
        >
          <PlanFileList projectId={projectId} />
        </FormSection>
      </FormCard>

      <FormCard>
        <FormSection
          title="認証対象期間・会員数計画"
          description="計画書に記載した認証対象期間と、年度ごとの会員数（計画値）です。"
        >
          {renderPlanValues()}
        </FormSection>
      </FormCard>
    </AdminProjectDetailLayout>
  );
}
