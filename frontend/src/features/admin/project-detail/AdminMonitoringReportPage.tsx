import { Link, useParams } from 'react-router-dom';
import { FileText } from 'lucide-react';
import { PageSkeleton, SkeletonCard } from '@/components/feedback/loading';
import { FormCard } from '@/components/forms/form-layout';
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

/**
 * モニタリング報告書. No report is generated yet, so the page says so and points
 * to the monitoring tab, where the submitted data already lives.
 */
export default function AdminMonitoringReportPage() {
  const { projectId } = useParams();
  const projectQuery = useAdminProjectDetailQuery(projectId);
  const project = projectQuery.data;

  if (projectQuery.isLoading) {
    return (
      <PageSkeleton>
        <div className="mx-auto w-full max-w-admin space-y-5">
          <SkeletonCard className="min-h-24" />
          <SkeletonCard className="min-h-64" />
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
      title="モニタリング報告書"
      description="認証機関へ提出するモニタリング報告書を確認します。"
      currentBreadcrumb="モニタリング報告書"
    >
      <FormCard>
        <div className="flex flex-col items-center gap-4 py-10 text-center">
          <span className="flex h-12 w-12 items-center justify-center rounded-full bg-secondary text-primary">
            <FileText className="h-6 w-6" aria-hidden="true" />
          </span>
          <div className="space-y-1">
            <p className="font-bold text-foreground">報告書はまだ作成されていません</p>
            <p className="text-sm text-muted-foreground">
              提出済みのモニタリングデータは、プロジェクト詳細のモニタリングタブで確認できます。
            </p>
          </div>
          <Button asChild variant="outline">
            <Link to={paths.adminProjectDetailTab(projectId, 'monitoring')}>
              モニタリングデータを見る
            </Link>
          </Button>
        </div>
      </FormCard>
    </AdminProjectDetailLayout>
  );
}
