import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { ArrowLeft } from 'lucide-react';
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
} from '@/components/ui/card';
import {
  Form,
  FormField,
  FormItem,
  FormControl,
  FormMessage,
} from '@/components/ui/form';
import { RequiredLabel } from '@/components/forms/shared';
import { useProfileStore } from '@/stores/ProfileStore';
import { CompanyFormValues, companySchema } from '@/lib/schemas/companySchema';

export interface CompanyFormData {
  phoneNumber: string;
  fax: string;
  email: string;
}

interface CompanyFormProps {
  defaultValues?: Partial<CompanyFormData>;
  onSubmit: (values: CompanyFormValues) => void;
  onCancel: () => void;
}

export const CompanyForm: React.FC<CompanyFormProps> = ({
  defaultValues,
  onSubmit,
  onCancel,
}) => {
  const form = useForm<CompanyFormValues>({
    // resolver: zodResolver(companySchema),
    defaultValues,
  });

  const { company } = useProfileStore();

  const isDirty = form.formState.isDirty;

  return (
    <div className='min-h-screen bg-background py-8'>
      <div className='bg-primary text-primary-foreground py-6 px-4 mb-8'>
        <div className='max-w-md mx-auto'>
          <h2 className='text-3xl font-bold text-center'>法人情報の更新</h2>
        </div>
      </div>

      <div className='container mx-auto max-w-xl px-4'>
        <Button
          variant='ghost'
          onClick={onCancel}
          className='mb-6'
        >
          <ArrowLeft className='mr-2 h-4 w-4' />
          プロファイルに戻る
        </Button>

        <Card>
          <CardHeader>
            <CardTitle>法人情報</CardTitle>
            <CardDescription>すべての項目は必須です。</CardDescription>
          </CardHeader>
          <CardContent>
            <Form {...form}>
              <form
                onSubmit={form.handleSubmit(onSubmit)}
                className='space-y-6'
              >
                <div className='flex flex-col items-start'>
                  <p className='text-sm font-medium text-foreground'>法人番号</p>
                  <p className='text-base font-medium'>
                    {company?.corpNumber ?? ''}
                  </p>
                </div>

                {/* 企業名　*/}
                <div className='flex flex-col items-start'>
                  <p className='text-sm font-medium text-foreground'>企業名</p>
                  <p className='text-base font-medium'>
                    {company?.nameJA ?? ''}
                  </p>
                </div>

                {/* 企業名（カナ） */}
                <div className='flex flex-col items-start'>
                  <p className='text-sm font-medium text-foreground'>
                    企業名（カナ）
                  </p>
                  <p className='text-base font-medium'>
                    {company?.nameFurigana ?? ''}
                  </p>
                </div>

                {/* 企業名（英語） */}
                <div className='flex flex-col items-start'>
                  <p className='text-sm font-medium text-foreground'>
                    企業名（英語）
                  </p>
                  <p className='text-base font-medium'>
                    {company?.nameEN ?? ''}
                  </p>
                </div>

                {/* 所在地 */}
                <div className='flex flex-col items-start'>
                  <p className='text-sm font-medium text-foreground'>所在地</p>
                  <p className='text-base font-medium'>
                    {company?.address ?? ''}
                  </p>
                </div>

                <FormField
                  control={form.control}
                  name='phoneNumber'
                  render={({ field }) => (
                    <FormItem>
                      <RequiredLabel required={false}>電話番号</RequiredLabel>
                      <FormControl>
                        <Input
                          placeholder='03-1234-5678'
                          {...field}
                          value={field.value ?? ''}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name='fax'
                  render={({ field }) => (
                    <FormItem>
                      <RequiredLabel required={false}>FAX番号（任意）</RequiredLabel>
                      <FormControl>
                        <Input
                          placeholder='03-1234-5678'
                          {...field}
                          value={field.value ?? ''}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name='email'
                  render={({ field }) => (
                    <FormItem>
                      <RequiredLabel required={false}>メールアドレス</RequiredLabel>
                      <FormControl>
                        <Input
                          type='email'
                          placeholder='example@company.com'
                          {...field}
                          value={field.value ?? ''}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <div className='flex justify-end gap-4'>
                  <Button
                    type='button'
                    variant='outline'
                    onClick={onCancel}
                  >
                    キャンセル
                  </Button>
                  <Button
                    type='submit'
                    disabled={!isDirty}
                  >
                    更新
                  </Button>
                </div>
              </form>
            </Form>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};
