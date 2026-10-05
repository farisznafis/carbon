import { useEffect } from 'react';
import { useForm, type FieldPath } from 'react-hook-form';
import { z } from 'zod';
import type { SiteProjectEquipment } from '@/api/site';
import { FileSelectButton } from '@/components/inputs/FileSelectButton/FileSelectButton';
import { InlineLoader } from '@/components/feedback/loading';
import {
  FormActions,
  FormCard,
  FormPageContent,
  FormPageTitle,
  FormSection,
  FormSectionStack,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import { RequiredLabel } from '@/components/forms/shared';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
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
import { getUploadFileValidationError } from '@/lib/uploadFileValidation';
import { emptyFormData } from './emptyFormData';
import { ActionButton } from '@/components/actions';

const base = {
  projectId: z.string().optional(),
  siteId: z.string().optional(),
  equipmentId: z.number().nullable().optional(),
  equipmentHasBattery: z.boolean().optional(),
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
  changedBattery: z.boolean().nullable(),
  ELPJ_battery_value: z.union([z.string(), z.number(), z.null()]).optional(),
});

export type MonitoringSolarFormData = {
  projectId?: string;
  siteId?: string;
  equipmentId?: number | null;
  equipmentHasBattery?: boolean;
  dateRecorded?: string;
  ELpv_value?: string | number | null;
  ELpvr_value?: string | number | null;
  changedBattery: boolean | null;
  ELPJ_battery_value?: string | number | null;
  ELPJ_control_value?: string | number | null;
  ELPJ_accuracy?: string | number | null;
  ELpv_evidence?: File | string | null;
  ELpvr_evidence?: File | string | null;
  ELPJ_battery_evidence?: File | string | null;
  ELPJ_control_equipment_spec_files?: File | string | null;
  description?: string;
};

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
  equipmentList?: SiteProjectEquipment[];
  isEquipmentLoading?: boolean;
  equipmentLoadError?: string | null;
  onEquipmentRetry?: () => void;
}

interface NumericInputWithUnitProps {
  value: string | number | null | undefined;
  onChange: (value: string | null) => void;
  placeholder: string;
  unit: string;
}

function NumericInputWithUnit({
  value,
  onChange,
  placeholder,
  unit,
}: Readonly<NumericInputWithUnitProps>) {
  return (
    <div className="relative w-full">
      <Input
        className="w-full pr-16 [appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none"
        type="number"
        value={String(value ?? '')}
        onChange={(event) => {
          const nextValue = event.target.value;
          onChange(nextValue === '' || nextValue === '0' ? null : nextValue);
        }}
        placeholder={placeholder}
      />
      <span className="pointer-events-none absolute inset-y-0 right-4 flex items-center text-sm text-foreground">
        {unit}
      </span>
    </div>
  );
}

export function MonitoringSolarForm({
  value,
  serverErrors,
  onChange,
  onNext,
  onBack,
  equipmentList = [],
  isEquipmentLoading = false,
  equipmentLoadError,
  onEquipmentRetry,
}: Readonly<MonitoringSolarFormProps>) {
  const form = useForm<MonitoringSolarFormData>({
    defaultValues: value ?? emptyFormData,
    mode: 'onSubmit',
    reValidateMode: 'onSubmit',
  });
  const isBatteryChanged = form.watch('changedBattery') === true;
  const selectedEquipmentId = form.watch('equipmentId');
  const selectedEquipment = equipmentList.find(
    (equipment) => equipment.equipmentId === selectedEquipmentId,
  );
  const equipmentHasBattery = selectedEquipment?.hasBattery === true;

  const handleFileChange = (
    fieldName: FieldPath<MonitoringSolarFormData>,
    onChange: (file: File | null) => void,
    file: File | null,
    allowedExtensions: string[],
  ) => {
    const validationError = getUploadFileValidationError(
      file,
      allowedExtensions,
    );
    if (validationError) {
      onChange(null);
      form.setError(fieldName, {
        type: 'validate',
        message: validationError,
      });
      return;
    }

    form.clearErrors(fieldName);
    onChange(file);
  };

  useEffect(() => {
    if (!equipmentHasBattery || !isBatteryChanged) {
      form.setValue('ELPJ_battery_value', null, { shouldValidate: false });
    }
  }, [equipmentHasBattery, form, isBatteryChanged]);

  useEffect(() => {
    if (equipmentList.length !== 1 || form.getValues('equipmentId') != null) {
      return;
    }

    const equipment = equipmentList[0];
    if (equipment.equipmentId == null) return;

    form.setValue('equipmentId', equipment.equipmentId, {
      shouldDirty: false,
      shouldValidate: false,
    });
    form.setValue('equipmentHasBattery', equipment.hasBattery === true);
  }, [equipmentList, form]);

  useEffect(() => {
    form.setValue('equipmentHasBattery', equipmentHasBattery, {
      shouldValidate: false,
    });

    if (equipmentHasBattery) return;

    form.setValue('changedBattery', null, { shouldValidate: false });
    form.setValue('ELPJ_battery_value', null, { shouldValidate: false });
    form.setValue('ELPJ_battery_evidence', null, { shouldValidate: false });
    form.clearErrors([
      'changedBattery',
      'ELPJ_battery_value',
      'ELPJ_battery_evidence',
    ]);
  }, [equipmentHasBattery, form]);

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
                name="equipmentId"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>設備ID</RequiredLabel>
                    <p className="mt-1 text-sm text-foreground">
                      モニタリング対象の設備を選択してください。
                    </p>
                    <FormControl>
                      <Select
                        value={field.value == null ? '' : String(field.value)}
                        disabled={isEquipmentLoading || equipmentList.length === 0}
                        onValueChange={(nextValue) => {
                          const equipmentId = Number(nextValue);
                          const equipment = equipmentList.find(
                            (candidate) => candidate.equipmentId === equipmentId,
                          );
                          field.onChange(equipmentId);
                          form.setValue(
                            'equipmentHasBattery',
                            equipment?.hasBattery === true,
                            { shouldDirty: true },
                          );
                          form.clearErrors('equipmentId');
                        }}
                      >
                        <SelectTrigger aria-label="設備ID">
                          <SelectValue
                            placeholder={
                              isEquipmentLoading
                                ? '設備を読み込んでいます...'
                                : '設備を選択してください'
                            }
                          />
                        </SelectTrigger>
                        <SelectContent>
                          {equipmentList.map((equipment) => {
                            if (equipment.equipmentId == null) return null;
                            const details = [
                              equipment.deviceNo,
                              equipment.pcsManufacturer,
                              equipment.pcsModel,
                            ].filter(Boolean);

                            return (
                              <SelectItem
                                key={equipment.equipmentId}
                                value={String(equipment.equipmentId)}
                              >
                                {`設備ID：${equipment.equipmentId}${
                                  details.length ? `（${details.join(' / ')}）` : ''
                                }`}
                              </SelectItem>
                            );
                          })}
                        </SelectContent>
                      </Select>
                    </FormControl>
                    {isEquipmentLoading ? (
                      <InlineLoader label="設備を読み込んでいます" />
                    ) : null}
                    {equipmentLoadError ? (
                      <div className="space-y-2" role="alert">
                        <p className="text-sm text-destructive">
                          {equipmentLoadError}
                        </p>
                        {onEquipmentRetry ? (
                          <ActionButton kind="retry" onClick={onEquipmentRetry}>
                            再読み込み
                          </ActionButton>
                        ) : null}
                      </div>
                    ) : null}
                    {!isEquipmentLoading &&
                    !equipmentLoadError &&
                    equipmentList.length === 0 ? (
                      <p className="text-sm text-muted-foreground">
                        選択できる設備がありません。
                      </p>
                    ) : null}
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="dateRecorded"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>測定日</RequiredLabel>
                    <p className="text-sm text-foreground mt-1">
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
                    <RequiredLabel>発電量 (ELPV)</RequiredLabel>
                    <p className="text-sm text-foreground mt-1">
                      対象期間中に太陽光発電設備で発電した総電力量を入力してください。
                    </p>
                    <FormControl>
                      <NumericInputWithUnit
                        value={field.value}
                        onChange={field.onChange}
                        placeholder="数値を入力"
                        unit="kWh"
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
                    <RequiredLabel>自家消費量 (ELPVR)</RequiredLabel>
                    <p className="text-sm text-foreground mt-1">
                      対象期間中に発電した電力量のうち、売電または他者へ提供した電力量を入力してください。
                    </p>
                    <FormControl>
                      <NumericInputWithUnit
                        value={field.value}
                        onChange={field.onChange}
                        placeholder="数値を入力"
                        unit="kWh"
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {equipmentHasBattery ? (
                <FormField
                control={form.control}
                name="changedBattery"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>蓄電池交換の有無</RequiredLabel>
                    <p className="text-sm text-foreground mt-1">
                      対象期間中に蓄電池を交換したかどうかを選択してください。
交換した場合は、交換後の蓄電池に関する証憑資料をアップロードしてください。
                    </p>
                    <FormControl>
                      <Select
                        onValueChange={(nextValue) => {
                          const hasBatteryChanged = nextValue === 'true';

                          form.setValue('changedBattery', hasBatteryChanged, {
                            shouldValidate: true,
                            shouldDirty: true,
                          });

                          if (!hasBatteryChanged) {
                            form.setValue('ELPJ_battery_evidence', null, {
                              shouldDirty: true,
                            });
                            form.clearErrors('ELPJ_battery_evidence');
                          }
                        }}
                        value={
                          field.value == null
                            ? undefined
                            : field.value
                              ? 'true'
                              : 'false'
                        }
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
              ) : null}

              {equipmentHasBattery && isBatteryChanged ? (
                <FormField
                  control={form.control}
                  name="ELPJ_battery_value"
                  render={({ field }) => (
                    <FormItem>
                      <RequiredLabel>蓄電池放電量 (ELPJ)</RequiredLabel>
                      <p className="text-sm text-foreground mt-1">
                      対象期間中に蓄電池から放電された電力量を入力してください。
                      </p>
                      <FormControl>
                        <NumericInputWithUnit
                          value={field.value}
                          onChange={field.onChange}
                          placeholder="数値を入力"
                          unit="kWh"
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              ) : null}

              {/* 蓄電池エビデンス / Battery Changed Evidence */}
              {equipmentHasBattery && isBatteryChanged ? (
                <FormField
                  control={form.control}
                  name="ELPJ_battery_evidence"
                  render={({ field }) => (
                    <FormItem>
                      <FormControl>
                        <FileSelectButton
                          id="elpj-battery-file"
                          accept={DEFAULT_EVIDENCE_FILE_EXTENSIONS.join(',')}
                          label="蓄電池エビデンス"
                          required
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
              ) : null}

              <FormField
                control={form.control}
                name="ELPJ_control_value"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>制御機器稼働時間 (ELPJ)</RequiredLabel>
                    <p className="text-sm text-foreground mt-1">
                      対象期間中に制御機器が稼働した合計時間を入力してください。
                    </p>
                    <FormControl>
                      <NumericInputWithUnit
                        value={field.value}
                        onChange={field.onChange}
                        placeholder="数値を入力"
                        unit="時間"
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
                        <RequiredLabel>測定精度</RequiredLabel>
                        <p className="text-sm text-foreground mt-1">
                          モニタリングデータの測定に使用した機器の測定精度を入力してください。
                        </p>
                        <FormControl>
                          <NumericInputWithUnit
                            value={field.value}
                            onChange={field.onChange}
                            placeholder="精度を入力"
                            unit="%"
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </FormSection>

                <FormSection number={2} title="資料アップロード">
                  <p className="text-sm text-foreground -mt-4 mb-4">
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
                            accept={ELPV_FILE_EXTENSIONS.join(',')}
                            label="発電量 (ELPV) エビデンス"
                            required
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
                            accept={DEFAULT_EVIDENCE_FILE_EXTENSIONS.join(',')}
                            label="自家消費量 (ELPVR) エビデンス"
                            required
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
                    name="ELPJ_control_equipment_spec_files"
                    render={({ field }) => (
                      <FormItem>
                        <FormControl>
                          <FileSelectButton
                            id="elpj-control-file"
                            accept={DEFAULT_EVIDENCE_FILE_EXTENSIONS.join(',')}
                            label="制御機器仕様 (ELPJ) エビデンス"
                            required
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

              <FormActions
                className="mt-8"
                disabled={form.formState.isSubmitting}
                pendingActionId={form.formState.isSubmitting ? 'confirm' : null}
                actions={[
                  {
                    id: 'cancel',
                    label: 'キャンセル',
                    variant: 'outline',
                    onClick: onBack,
                  },
                  {
                    id: 'confirm',
                    label: '確認',
                    pendingLabel: '確認しています...',
                    type: 'submit',
                  },
                ]}
              />
            </FormCard>
          </form>
        </Form>
      </FormPageContent>
    </FormStandalonePage>
  );
}

export default MonitoringSolarForm;
