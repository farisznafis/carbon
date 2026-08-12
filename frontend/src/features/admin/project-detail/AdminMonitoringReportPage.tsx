import { useNavigate, useParams } from 'react-router-dom';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import {
  floatingPageBackButtonClassName,
  PageBackButton,
} from '@/components/navigation/PageBackButton';

export default function AdminMonitoringReportPage() {
  const { projectId } = useParams();
  const navigate = useNavigate();

  return (
    <main className='relative min-h-screen bg-background px-4 py-8 sm:px-6'>
      <PageBackButton
        onClick={() => navigate(-1)}
        label=''
        aria-label='プロジェクト詳細へ戻る'
        className={floatingPageBackButtonClassName}
      />

      <div className='container mx-auto space-y-6 pt-16'>
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
            <Button disabled>更新</Button>
          </CardFooter>
        </Card>
      </div>
    </main>
  );
}
