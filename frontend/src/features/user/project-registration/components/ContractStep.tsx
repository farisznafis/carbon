import { forwardRef, useEffect, useImperativeHandle, useRef } from 'react';
import {
  RequiredBadge,
  RequiredLabel,
} from '@/components/forms/shared/RequiredLabel';
import { Card, CardContent } from '@/components/ui/card';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import {
  Form,
  FormItem,
  FormField,
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
      <div className="space-y-4">
        <header>
          <h2 className="inline-flex flex-wrap items-center gap-1 text-foreground">
            環境社会配慮を行い持続可能性を確保することの同意書
            <RequiredBadge />
          </h2>
          <p className="text-small mt-2 text-foreground">
            以下の内容をご確認の上、該当する項目にご入力いただき、すべての必須項目に同意いただいた場合のみ、次のステップへ進むことができます。
          </p>
        </header>

        <Form {...form}>
          <Card className="rounded-lg">
            <CardContent className="space-y-6 p-5 sm:p-8">
              <p className="text-small text-foreground">
                （プログラム型運営・管理者）　御中
              </p>

              <div className="space-y-3">
                <FormField
                  control={form.control}
                  name="businessName"
                  render={({ field }) => (
                    <FormItem className="grid gap-2 sm:grid-cols-[120px_minmax(0,1fr)] sm:items-start">
                      <RequiredLabel htmlFor="businessName" className="sm:pt-3">
                        事業者名
                      </RequiredLabel>
                      <div>
                        <FormControl>
                          <Input
                            id="businessName"
                            placeholder="事業者名を入力"
                            aria-required="true"
                            {...field}
                          />
                        </FormControl>
                        <FormMessage className="mt-1" />
                      </div>
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="representativeName"
                  render={({ field }) => (
                    <FormItem className="grid gap-2 sm:grid-cols-[120px_minmax(0,1fr)] sm:items-start">
                      <RequiredLabel
                        htmlFor="representativeName"
                        className="sm:pt-3"
                      >
                        代表者名
                      </RequiredLabel>
                      <div>
                        <FormControl>
                          <Input
                            id="representativeName"
                            placeholder="代表者名を入力"
                            aria-required="true"
                            {...field}
                          />
                        </FormControl>
                        <FormMessage className="mt-1" />
                      </div>
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="representativePosition"
                  render={({ field }) => (
                    <FormItem className="grid gap-2 sm:grid-cols-[120px_minmax(0,1fr)] sm:items-start">
                      <RequiredLabel
                        htmlFor="representativePosition"
                        className="sm:pt-3"
                      >
                        代表者役職
                      </RequiredLabel>
                      <div>
                        <FormControl>
                          <Input
                            id="representativePosition"
                            placeholder="役職を入力"
                            aria-required="true"
                            {...field}
                          />
                        </FormControl>
                        <FormMessage className="mt-1" />
                      </div>
                    </FormItem>
                  )}
                />
              </div>

              <div className="space-y-4 text-sm leading-6 text-foreground">
                <p>
                  国内における地球温暖化対策のための排出削減・吸収量認証制度（Ｊ－クレジット制度）に、方法論EN-R-002（太陽光発電設備の導入）に基づくプロジェクトを登録して実施するにあたり、人間の健康と安全、自然環境、社会への影響を回避または最小化し、受け入れることができないような影響をもたらすことがないよう、環境社会配慮を行い持続可能性を確保するため、
                </p>
                <ul className="space-y-1 pl-4">
                  <li>・エネルギーの使用の合理化及び非化石エネルギーへの転換等に関する法律</li>
                  <li>・特定工場における公害防止組織の整備に関する法律</li>
                  <li>・環境基本法・建築基準法・電気事業法・景観法</li>
                  <li>・消防法・労働安全衛生法・地球温暖化対策の推進に関する法律</li>
                </ul>
                <p>その他関連法令等を遵守することを誓約します。</p>
                <p className="text-right">以上</p>
              </div>

              <FormField
                control={form.control}
                name="contractAgreed"
                render={({ field }) => (
                  <FormItem className="rounded-lg border border-border p-4">
                    <div className="flex items-center gap-3">
                      <FormControl>
                        <Checkbox
                          id="contractAgreed"
                          checked={field.value}
                          onCheckedChange={(checked) =>
                            field.onChange(checked === true)
                          }
                          aria-required="true"
                        />
                      </FormControl>
                      <RequiredLabel
                        htmlFor="contractAgreed"
                        className="cursor-pointer"
                      >
                        同意する
                      </RequiredLabel>
                    </div>
                    <FormMessage className="mt-2" />
                  </FormItem>
                )}
              />
            </CardContent>
          </Card>
        </Form>
      </div>
    );
  },
);

ContractStep.displayName = 'ContractStep';
