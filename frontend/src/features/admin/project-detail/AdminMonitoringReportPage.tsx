import { useParams } from 'react-router-dom';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { usePageBreadcrumb } from '@/components/navigation/pageBreadcrumbContext';
import { ActionButton } from '@/components/actions';
import { paths } from '@/routes/paths';
import { useAdminProjectDetailQuery } from '@/hooks/admin/useAdminProjectDetailQuery';

export default function AdminMonitoringReportPage() {
  const { projectId } = useParams();

  const projectName = useAdminProjectDetailQuery(projectId).data?.projectName;
  usePageBreadcrumb([
    { label: 'プロジェクト', to: paths.adminProjectList },
    {
      label: projectName || 'プロジェクト詳細',
      to: projectId ? paths.adminProjectDetail(projectId) : undefined,
    },
    { label: 'モニタリング報告書' },
  ]);

  return (
    <main className='relative min-h-screen bg-background px-4 py-8 sm:px-6'>
      <div className='container mx-auto space-y-6'>
        <Card>
          <CardHeader>
            <CardTitle>モニタリングデータ報告書</CardTitle>
            <CardDescription>
              projectId: {projectId ?? '-'}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className='text-sm text-foreground'>
              報告書データはまだありません。
            </div>
          </CardContent>
          <CardFooter className='justify-end'>
            <ActionButton kind="edit" disabled>
              更新
            </ActionButton>
          </CardFooter>
        </Card>
      </div>
    </main>
  );
}
