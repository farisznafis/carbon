import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { PageBackButton } from '@/components/navigation/PageBackButton';

export default function AdminMonitoringReportPage() {
  const { projectId } = useParams();
  const navigate = useNavigate();

  return (
    <div className='min-h-screen py-8'>
      <div className='container mx-auto px-4 space-y-6'>
        <PageBackButton
          onClick={() => navigate(-1)}
          label='戻る'
          className='rounded-full'
        />
        <Button
          variant='ghost'
          onClick={() => navigate(-1)}
          className='hidden'
        >
          <ArrowLeft className='h-4 w-4 mr-2' />
          戻る
        </Button>

        <Card>
          <CardHeader>
            <CardTitle>モニタリングデータ報告書</CardTitle>
            <CardDescription>
              projectId: {projectId ?? '-'}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className='text-sm text-muted-foreground'>
              報告書データはまだありません。
            </div>
          </CardContent>
          <CardFooter className='justify-end'>
            <Button disabled>更新</Button>
          </CardFooter>
        </Card>
      </div>
    </div>
  );
}
