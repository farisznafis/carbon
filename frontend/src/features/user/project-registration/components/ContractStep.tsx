import { forwardRef, useEffect, useImperativeHandle, useRef } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import {
  Form,
  FormItem,
  FormField,
  FormLabel,
  FormControl,
  FormMessage,
} from '@/components/ui/form';

const contractStepSchema = z.object({
  businessName: z.string().nonempty('事業者名を入力してください'),
  representativeName: z.string().nonempty('代表者名を入力してください'),
  representativePosition: z.string().nonempty('代表者役職を入力してください'),
  contractAgreed: z.boolean().refine((val) => val === true, '同意してください'),
});

type ContractStepData = z.infer<typeof contractStepSchema>;

export interface ContractStepRef {
  triggerValidation: () => Promise<boolean>;
  getValues: () => ContractStepFormValues;
}

export type ContractStepFormValues = {
  contractAgreed: boolean;
  businessName: string;
  representativePosition: string;
  representativeName: string;
};

interface ContractStepProps {
  formValues: Partial<ContractStepFormValues>;
  onValueChange: (values: Partial<ContractStepFormValues>) => void;
  errors?: Record<string, string>;
}

const defaultContractStepValues: ContractStepFormValues = {
  contractAgreed: false,
  businessName: '',
  representativePosition: '',
  representativeName: '',
};

export const ContractStep = forwardRef<ContractStepRef, ContractStepProps>(
  ({ formValues, onValueChange, errors = {} }, ref) => {
    const form = useForm<ContractStepData>({
      mode: 'onChange',
      defaultValues: {
        ...defaultContractStepValues,
        ...formValues,
      },
    });
    const watchedValues = form.watch();
    const serverErrorFieldsRef = useRef<Array<keyof ContractStepFormValues>>(
      [],
    );

    useImperativeHandle(
      ref,
      () => ({
        triggerValidation: () => form.trigger(),
        getValues: () => form.getValues(),
      }),
      [form],
    );

    // Reset form with saved values when component mounts or formValues changes
    useEffect(() => {
      form.reset({
        ...defaultContractStepValues,
        ...formValues,
      });
    }, [form, formValues]);

    useEffect(() => {
      serverErrorFieldsRef.current.forEach((fieldName) => {
        form.clearErrors(fieldName);
      });

      const serverErrorFields = Object.entries(errors).flatMap(
        ([fieldName, message]) => {
          if (!(fieldName in form.getValues()) || !message) return [];

          const contractFieldName =
            fieldName as keyof ContractStepFormValues;
          form.setError(contractFieldName, {
            type: 'server',
            message,
          });
          return [contractFieldName];
        },
      );
      serverErrorFieldsRef.current = serverErrorFields;
    }, [errors, form]);

    const prevValuesRef = useRef(watchedValues);

    useEffect(() => {
      if (
        JSON.stringify(prevValuesRef.current) !== JSON.stringify(watchedValues)
      ) {
        prevValuesRef.current = watchedValues;
        onValueChange(watchedValues);
      }
    }, [watchedValues, onValueChange]);

    return (
      <div className='space-y-6'>
        <div>
          <h2 className='text-2xl font-bold mb-2'>
            環境社会配慮を行い持続可能性を確保することの同意書{' '}
            <span className='ml-1 text-destructive'>*</span>
          </h2>
          <p className='text-muted-foreground'>
            以下の内容をご確認の上、同意してください
          </p>
        </div>

        <Card>
          <CardContent className='pt-6'>
            <div className='p-6 border rounded-lg space-y-4'>
              <div className='text-sm leading-relaxed space-y-1'>
                <p>(プログラム型運営・管理者)　御中</p>
                <br />

                <div className='grid grid-cols-1 md:grid-cols-[auto_1fr] gap-2 items-center'>
                  <Label
                    htmlFor='businessName'
                    className='text-sm font-medium whitespace-nowrap'
                  >
                    事業者名
                  </Label>
                  <Form {...form}>
                    <FormField
                      control={form.control}
                      name='businessName'
                      render={({ field }) => (
                        <FormItem>
                          <FormControl>
                            <Input
                              id='businessName'
                              {...field} // connects RHF to the input
                              placeholder='事業者名を入力'
                              className='w-48 h-9'
                            />
                          </FormControl>
                          <FormMessage /> {/* displays validation error */}
                        </FormItem>
                      )}
                    />
                  </Form>

                  <Label
                    htmlFor='representativeName'
                    className='text-sm font-medium whitespace-nowrap'
                  >
                    代表者名
                  </Label>
                  <Form {...form}>
                    <FormField
                      control={form.control}
                      name='representativeName'
                      render={({ field }) => (
                        <FormItem>
                          <FormControl>
                            <Input
                              id='representativeName'
                              {...field}
                              placeholder='代表者名を入力'
                              className='w-48 h-9'
                            />
                          </FormControl>
                          <FormMessage /> {/* displays validation error */}
                        </FormItem>
                      )}
                    />
                  </Form>

                  <Label
                    htmlFor='representativePosition'
                    className='text-sm font-medium whitespace-nowrap'
                  >
                    代表者役職
                  </Label>
                  <Form {...form}>
                    <FormField
                      control={form.control}
                      name='representativePosition'
                      render={({ field }) => (
                        <FormItem>
                          <FormControl>
                            <Input
                              id='representativePosition'
                              {...field}
                              placeholder='役職を入力'
                              className='w-48 h-9'
                            />
                          </FormControl>
                          <FormMessage /> {/* displays validation error */}
                        </FormItem>
                      )}
                    />
                  </Form>
                </div>

                <br />
                <p>
                  国内における地球温暖化対策のための排出削減・吸収量認証制度（Ｊ－クレジット制度）に、方法論EN-R-002（太陽光発電設備の導入）に基づくプロジェクトを登録して実施するにあたり、人間の健康と安全、自然環境、社会への影響を回避または最小化し、受け入れることができないような影響をもたらすことがないよう、環境社会配慮を行い持続可能性を確保するため、
                </p>
                <br />
                <div className='pl-4 space-y-1'>
                  <p>
                    ・エネルギーの使用の合理化及び非化石エネルギーへの転換等に関する法律
                  </p>
                  <p>・特定工場における公害防止組織の整備に関する法律</p>
                  <p>・環境基本法</p>
                  <p>・建築基準法</p>
                  <p>・電気事業法</p>
                  <p>・景観法</p>
                  <p>・消防法</p>
                  <p>・労働安全衛生法</p>
                  <p>・地球温暖化対策の推進に関する法律</p>
                </div>
                <br />
                <p>その他関連法令等を遵守することを誓約します。</p>

                <p className='text-right pt-4'>以上</p>
              </div>
            </div>

            <div className='flex justify-center mt-6'>
              <Card className='max-w-sm mx-auto mt-6 p-6 rounded-lg'>
                <Form {...form}>
                  <FormField
                    control={form.control}
                    name='contractAgreed'
                    render={({ field }) => {
                      const { value, ...rest } = field;
                      return (
                        <FormItem className='flex items-center space-x-2'>
                          <FormControl>
                            <input
                              type='checkbox'
                              id='contractAgreed'
                              {...rest}
                              checked={value}
                              onChange={(e) => field.onChange(e.target.checked)}
                              className='h-5 w-5 rounded-sm border-input align-middle text-primary focus:ring-2 focus:ring-ring'
                            />
                          </FormControl>
                          <div className='flex flex-col'>
                            <FormLabel
                              htmlFor='contractAgreed'
                              className='text-sm font-medium leading-none'
                            >
                              同意します
                            </FormLabel>
                            <FormMessage className='mt-1 text-xs text-destructive' />{' '}
                            {/* displays validation error */}
                          </div>
                        </FormItem>
                      );
                    }}
                  />
                </Form>
              </Card>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  },
);

ContractStep.displayName = 'ContractStep';
