import { useEffect } from 'react';
import { useForm, type FieldPath } from 'react-hook-form';
import { z } from 'zod';
import { FileSelectButton } from '@/components/inputs/FileSelectButton/FileSelectButton';
import {
  FormCard,
  FormPageContent,
  FormPageTitle,
  FormSection,
  FormSectionStack,
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
import { emptyFormData } from './emptyFormData';

const base = {
  projectId: z.string().optional(),
  siteId: z.string().optional(),
  dateRecorded: z.string().optional(),
  ELpv_value: z.union([z.string(), z.number(), z.null()]).optional(),
  ELpvr_value: z.union([z.string(), z.number(), z.null()]).optional(),
  ELPJ_control_value: z.union([z.string(), z.number(), z.null()]).optional(),
  ELPJ_accuracy: z.union([z.string(), z.number(), z.null()]).optional(),
  ELpv_evidence: z.any().optional(),
  ELpvr_evidence: z.any().optional(),
  ELPJ_battery_evidence: z.any().optional(),
  ELPJ_control_equipment_spec_files: z.any().optional(),
  description: z.string().optional(),
};

export const MonitoringSolarFormSchema = z.object({
  ...base,
  changedBattery: z.boolean(),
  ELPJ_battery_value: z.union([z.string(), z.number(), z.null()]).optional(),
});

export type MonitoringSolarFormData = {
  projectId?: string;
  siteId?: string;
  dateRecorded?: string;
  ELpv_value?: string | number | null;
  ELpvr_value?: string | number | null;
  changedBattery: boolean;
  ELPJ_battery_value?: string | number | null;
  ELPJ_control_value?: string | number | null;
  ELPJ_accuracy?: string | number | null;
  ELpv_evidence?: File | string | null;
  ELpvr_evidence?: File | string | null;
  ELPJ_battery_evidence?: File | string | null;
  ELPJ_control_equipment_spec_files?: File | string | null;
  description?: string;
};

const MAX_UPLOAD_FILE_SIZE_BYTES = 10 * 1024 * 1024;
const MAX_UPLOAD_FILE_SIZE_MESSAGE = 'ファイルサイズは10MB以下にしてください。';
const FILE_TYPE_ERROR_MESSAGE = '対応していないファイル形式です。';
const ELPV_FILE_EXTENSIONS = ['.pdf', '.csv'];
const DEFAULT_EVIDENCE_FILE_EXTENSIONS = ['.pdf', '.jpg', '.jpeg', '.png'];

interface MonitoringSolarFormProps {
  mode: 'create' | 'edit';
  value: MonitoringSolarFormData;
  previousData?: Partial<MonitoringSolarFormData>;
  serverErrors?: Record<string, string>;
  onChange: (data: MonitoringSolarFormData) => void;
  onNext: (data: MonitoringSolarFormData) => void | Promise<void>;
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
    defaultValues: value ?? emptyFormData,
    mode: 'onSubmit',
    reValidateMode: 'onSubmit',
  });
  const isBatteryChanged = form.watch('changedBattery');

  const handleFileChange = (
    fieldName: FieldPath<MonitoringSolarFormData>,
    onChange: (file: File | null) => void,
    file: File | null,
    allowedExtensions: string[],
  ) => {
    if (file) {
      const lowerFileName = file.name.toLowerCase();
      const isAllowedFileType = allowedExtensions.some((extension) =>
        lowerFileName.endsWith(extension),
      );

      if (!isAllowedFileType) {
        onChange(null);
        form.setError(fieldName, {
          type: 'validate',
          message: `${FILE_TYPE_ERROR_MESSAGE}（${allowedExtensions.join(', ')}）`,
        });
        return;
      }
    }

    if (file && file.size > MAX_UPLOAD_FILE_SIZE_BYTES) {
      onChange(null);
      form.setError(fieldName, {
        type: 'validate',
        message: MAX_UPLOAD_FILE_SIZE_MESSAGE,
      });
      return;
    }

    form.clearErrors(fieldName);
    onChange(file);
  };

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

        <Form {...form}>
          <form
            onSubmit={form.handleSubmit(async (values) => {
              onChange(values);
              await onNext(values);
            })}
          >
            <FormCard>
              <FormSectionStack spacing="relaxed">
                <FormSection number={1} title="モニタリング情報">
              <FormField
                control={form.control}
                name="dateRecorded"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>記録日</FormLabel>
                    <p className="text-sm text-muted-foreground mt-1">
                      モニタリングを実施した日を入力してください。申請日ではありません。
                    </p>
                    <FormControl>
                      <Input
                        type="date"
                        value={field.value ?? ''}
                        onChange={(event) => field.onChange(event.target.value === '' ? null : event.target.value)}
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
                        onChange={(event) => {
                          const value = event.target.value;
                          field.onChange(value === '' || value === '0' ? null : value);
                        }}
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
                        onChange={(event) => {
                          const value = event.target.value;
                          field.onChange(value === '' || value === '0' ? null : value);
                        }}
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
                          onChange={(event) => {
                            const value = event.target.value;
                            field.onChange(value === '' || value === '0' ? null : value);
                          }}
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
                        onChange={(event) => {
                          const value = event.target.value;
                          field.onChange(value === '' || value === '0' ? null : value);
                        }}
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
                            onChange={(event) => {
                              const value = event.target.value;
                              field.onChange(value === '' || value === '0' ? null : value);
                            }}
                            placeholder="精度を入力"
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </FormSection>

                <FormSection number={2} title="資料アップロード">
                  <p className="text-sm text-muted-foreground -mt-4 mb-4">
                    アップロード可能なファイルサイズは最大10MBです。
                  </p>
                  <FormField
                    control={form.control}
                    name="ELpv_evidence"
                    render={({ field }) => (
                      <FormItem>
                        <FormControl>
                          <FileSelectButton
                            id="elpv-file"
                            label="発電量 (ELPV) エビデンス"
                            variant="design-system-pro"
                            supportedFormats="(.pdf, .csv)"
                            value={(field.value as File) ?? null}
                            onChange={(file) =>
                              handleFileChange(
                                'ELpv_evidence',
                                field.onChange,
                                file,
                                ELPV_FILE_EXTENSIONS,
                              )
                            }
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
                        <FormControl>
                          <FileSelectButton
                            id="elpvr-file"
                            label="自家消費量 (ELPVR) エビデンス"
                            variant="design-system-pro"
                            supportedFormats="(.pdf, .jpg, .jpeg, .png)"
                            value={(field.value as File) ?? null}
                            onChange={(file) =>
                              handleFileChange(
                                'ELpvr_evidence',
                                field.onChange,
                                file,
                                DEFAULT_EVIDENCE_FILE_EXTENSIONS,
                              )
                            }
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
                        <FormControl>
                          <FileSelectButton
                            id="elpj-battery-file"
                            label="蓄電池エビデンス"
                            variant="design-system-pro"
                            supportedFormats="(.pdf, .jpg, .jpeg, .png)"
                            value={(field.value as File) ?? null}
                            onChange={(file) =>
                              handleFileChange(
                                'ELPJ_battery_evidence',
                                field.onChange,
                                file,
                                DEFAULT_EVIDENCE_FILE_EXTENSIONS,
                              )
                            }
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
                        <FormControl>
                          <FileSelectButton
                            id="elpj-control-file"
                            label="制御機器仕様 (ELPJ) エビデンス"
                            variant="design-system-pro"
                            supportedFormats="(.pdf, .jpg, .jpeg, .png)"
                            value={(field.value as File) ?? null}
                            onChange={(file) =>
                              handleFileChange(
                                'ELPJ_control_equipment_spec_files',
                                field.onChange,
                                file,
                                DEFAULT_EVIDENCE_FILE_EXTENSIONS,
                              )
                            }
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </FormSection>
              </FormSectionStack>

              <div className="flex justify-end gap-4 pt-6">
                <Button type="button" variant="outline" onClick={onBack} size="md">
                  キャンセル
                </Button>
                <Button type="submit" disabled={form.formState.isSubmitting} size="md">
                  確認
                </Button>
              </div>
            </FormCard>
          </form>
        </Form>
      </FormPageContent>
    </FormStandalonePage>
  );
}

export default MonitoringSolarForm;
