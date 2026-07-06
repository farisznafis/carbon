import { useEffect } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm, type FieldPath } from 'react-hook-form';
import { z } from 'zod';
import { FileSelectButton } from '@/components/inputs/FileSelectButton/FileSelectButton';
import {
  FormCard,
  FormPageContent,
  FormPageTitle,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { Button } from '@/components/ui/button';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Separator } from '@/components/ui/separator';
import { requiredFileSchema, requiredNumberSchema } from '@/lib/validations';
import { emptyFormData } from './emptyFormData';

const base = {
  projectId: z.string().optional(),
  siteId: z.string().optional(),
  dateRecorded: z.string().min(1, '記録日を入力してください'),
  ELpv_value: requiredNumberSchema('発電量 (ELPV)'),
  ELpvr_value: requiredNumberSchema('自家消費量 (ELPVR)'),
  ELPJ_control_value: requiredNumberSchema('制御機器稼働時間 (ELPJ)'),
  ELPJ_accuracy: requiredNumberSchema('測定精度'),
  ELpv_evidence: requiredFileSchema('発電量 (ELPV) エビデンス'),
  ELpvr_evidence: requiredFileSchema('自家消費量 (ELPVR) エビデンス'),
  ELPJ_battery_evidence: requiredFileSchema('蓄電池エビデンス'),
  ELPJ_control_equipment_spec_files: requiredFileSchema(
    '制御機器仕様 (ELPJ) エビデンス',
  ),
  description: z.string().optional(),
};

export const MonitoringSolarFormSchema = z.discriminatedUnion('changedBattery', [
  z.object({
    ...base,
    changedBattery: z.literal(true),
    ELPJ_battery_value: requiredNumberSchema('蓄電池放電量 (ELPJ)'),
  }),
  z.object({
    ...base,
    changedBattery: z.literal(false),
    ELPJ_battery_value: z.preprocess(
      (value) => (value === '' || value == null ? null : Number(value)),
      z.number().nullable(),
    ),
  }),
]);

export type MonitoringSolarFormData = z.infer<typeof MonitoringSolarFormSchema>;

interface MonitoringSolarFormProps {
  mode: 'create' | 'edit';
  value: MonitoringSolarFormData;
  previousData?: Partial<MonitoringSolarFormData>;
  serverErrors?: Record<string, string>;
  onChange: (data: MonitoringSolarFormData) => void;
  onNext: () => void;
  onBack: () => void;
  onDelete?: () => void;
}

export function MonitoringSolarForm({
  value,
  serverErrors,
  onChange,
  onNext,
  onBack,
}: Readonly<MonitoringSolarFormProps>) {
  const form = useForm<MonitoringSolarFormData>({
    resolver: zodResolver(MonitoringSolarFormSchema),
    defaultValues: value ?? emptyFormData,
    mode: 'onSubmit',
    reValidateMode: 'onSubmit',
  });
  const isBatteryChanged = form.watch('changedBattery');

  useEffect(() => {
    if (!isBatteryChanged) {
      form.setValue('ELPJ_battery_value', null, { shouldValidate: false });
    }
  }, [form, isBatteryChanged]);

  useEffect(() => {
    Object.entries(serverErrors ?? {}).forEach(([fieldName, message]) => {
      if (!message) return;

      form.setError(fieldName as FieldPath<MonitoringSolarFormData>, {
        type: 'server',
        message,
      });
    });
  }, [form, serverErrors]);

  return (
    <FormStandalonePage>
      <PageBackButton
        onClick={onBack}
        label=""
        className="absolute left-4 top-10 h-12 w-12 rounded-full p-0 sm:left-10 lg:left-16"
      />

      <FormPageContent>
        <FormPageTitle>モニタリング登録</FormPageTitle>

        <FormCard>
          <Form {...form}>
            <form
              onSubmit={form.handleSubmit((values) => {
                form.clearErrors();
                onChange(values);
                onNext();
              })}
              className="space-y-8"
            >
              <FormField
                control={form.control}
                name="dateRecorded"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>記録日</FormLabel>
                    <FormControl>
                      <Input
                        type="date"
                        value={field.value ?? ''}
                        onChange={(event) => field.onChange(event.target.value)}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="ELpv_value"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>発電量 (ELPV)</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        value={String(field.value ?? '')}
                        onChange={(event) => field.onChange(event.target.value)}
                        placeholder="数値を入力"
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="ELpvr_value"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>自家消費量 (ELPVR)</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        value={String(field.value ?? '')}
                        onChange={(event) => field.onChange(event.target.value)}
                        placeholder="数値を入力"
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="changedBattery"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>蓄電池交換の有無</FormLabel>
                    <FormControl>
                      <Select
                        onValueChange={(nextValue) => {
                          form.setValue('changedBattery', nextValue === 'true', {
                            shouldValidate: true,
                            shouldDirty: true,
                          });
                        }}
                        value={field.value ? 'true' : 'false'}
                      >
                        <SelectTrigger>
                          <SelectValue placeholder="選択してください" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="true">あり</SelectItem>
                          <SelectItem value="false">なし</SelectItem>
                        </SelectContent>
                      </Select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <fieldset
                disabled={!isBatteryChanged}
                className={!isBatteryChanged ? 'opacity-50' : ''}
              >
                <FormField
                  control={form.control}
                  name="ELPJ_battery_value"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>蓄電池放電量 (ELPJ)</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          value={field.value == null ? '' : String(field.value)}
                          onChange={(event) => field.onChange(event.target.value)}
                          placeholder="数値を入力"
                          disabled={!isBatteryChanged}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </fieldset>

              <FormField
                control={form.control}
                name="ELPJ_control_value"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>制御機器稼働時間 (ELPJ)</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        value={String(field.value ?? '')}
                        onChange={(event) => field.onChange(event.target.value)}
                        placeholder="数値を入力"
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="ELPJ_accuracy"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>測定精度</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        value={String(field.value ?? '')}
                        onChange={(event) => field.onChange(event.target.value)}
                        placeholder="精度を入力"
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <Separator />

              <div className="space-y-6">
                <FormLabel className="text-xl">資料アップロード</FormLabel>

                <div className="grid grid-cols-1 gap-6">
                  <FormField
                    control={form.control}
                    name="ELpv_evidence"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>発電量 (ELPV) エビデンス</FormLabel>
                        <FormControl>
                          <FileSelectButton
                            id="elpv-file"
                            value={(field.value as File) ?? null}
                            onChange={field.onChange}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="ELpvr_evidence"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>自家消費量 (ELPVR) エビデンス</FormLabel>
                        <FormControl>
                          <FileSelectButton
                            id="elpvr-file"
                            value={(field.value as File) ?? null}
                            onChange={field.onChange}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="ELPJ_battery_evidence"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>蓄電池エビデンス</FormLabel>
                        <FormControl>
                          <FileSelectButton
                            id="elpj-battery-file"
                            value={(field.value as File) ?? null}
                            onChange={field.onChange}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="ELPJ_control_equipment_spec_files"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>制御機器仕様 (ELPJ) エビデンス</FormLabel>
                        <FormControl>
                          <FileSelectButton
                            id="elpj-control-file"
                            value={(field.value as File) ?? null}
                            onChange={field.onChange}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>
              </div>

              <div className="flex justify-end gap-4">
                <Button type="button" variant="outline" onClick={onBack}>
                  キャンセル
                </Button>
                <Button type="submit" disabled={form.formState.isSubmitting}>
                  確認
                </Button>
              </div>
            </form>
          </Form>
        </FormCard>
      </FormPageContent>
    </FormStandalonePage>
  );
}

export default MonitoringSolarForm;
