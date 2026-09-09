import type { FieldPath, UseFormReturn } from 'react-hook-form';
import type {
  Site,
  SiteProjectEquipmentFile,
  SiteProjectEquipmentFileType,
} from '@/api/site';
import { RequiredLabel } from '@/components/forms/shared';
import { PresignedFileLink } from '@/components/files/PresignedFileLink';
import { FileSelectButton } from '@/components/inputs/FileSelectButton/FileSelectButton';
import {
  FormGrid,
  FormSection,
  FormSectionStack,
} from '@/components/forms/form-layout';
import { Input } from '@/components/ui/input';
import {
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormMessage,
} from '@/components/ui/form';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectSeparator,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import type { SolarPowerFormData } from '@/lib/equipmentValidations';
import {
  DEFAULT_UPLOAD_FILE_EXTENSIONS,
  getUploadFileValidationError,
} from '@/lib/uploadFileValidation';
import { Plus } from 'lucide-react';

const EQUIPMENT_FILE_ACCEPT = DEFAULT_UPLOAD_FILE_EXTENSIONS.join(',');
const EQUIPMENT_FILE_FORMATS = `(${DEFAULT_UPLOAD_FILE_EXTENSIONS.join(', ')} / 最大10MB)`;
const ADD_SITE_OPTION_VALUE = '__add_site__';

type SolarPowerEquipmentFormProps = Readonly<{
  form: UseFormReturn<SolarPowerFormData>;
}> &
  Readonly<
    | {
        mode?: 'registration';
        sites: Site[];
        isSiteOptionsLoading: boolean;
        siteOptionsError: string | null;
        /** Opens the site-registration flow from the project application form. */
        onAddSite?: (draft: SolarPowerFormData) => void;
      }
    | {
        mode: 'edit';
        existingFiles?: SiteProjectEquipmentFile[];
      }
  >;

type FileUploadControlProps = Readonly<{
  id: string;
  label: string;
  labelDescription?: string;
  fieldName: FieldPath<SolarPowerFormData>;
  form: UseFormReturn<SolarPowerFormData>;
  value?: File | null;
  onChange: (file: File | null) => void;
  required?: boolean;
  existingFile?: SiteProjectEquipmentFile | null;
}>;

function FileUploadControl({
  id,
  label,
  labelDescription,
  fieldName,
  form,
  value,
  onChange,
  required = true,
  existingFile,
}: FileUploadControlProps) {
  const existingFileName = existingFile
    ? getExistingFileName(existingFile)
    : null;

  return (
    <div className="space-y-2">
      <FileSelectButton
        id={id}
        accept={EQUIPMENT_FILE_ACCEPT}
        label={label}
        labelDescription={labelDescription}
        variant="design-system-pro"
        supportedFormats={EQUIPMENT_FILE_FORMATS}
        required={required}
        value={value ?? null}
        onChange={(file) => {
          const validationError = getUploadFileValidationError(
            file,
            DEFAULT_UPLOAD_FILE_EXTENSIONS,
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
        }}
        helperText={
          existingFileName || required
            ? undefined
            : '変更する場合のみファイルを選択してください。'
        }
      />
      {existingFileName && (
        <p className="flex flex-wrap items-center gap-1 text-sm text-foreground">
          <span>登録済み：</span>
          {existingFile?.s3Key ? (
            <PresignedFileLink
              s3Key={existingFile.s3Key}
              label={existingFileName}
            />
          ) : (
            <span>{existingFileName}</span>
          )}
        </p>
      )}
    </div>
  );
}

function getExistingFile(
  files: SiteProjectEquipmentFile[],
  fileType: SiteProjectEquipmentFileType,
) {
  return files.find((candidate) => candidate.fileType === fileType) ?? null;
}

function getExistingFileName(file: SiteProjectEquipmentFile) {
  if (file.fileName?.trim()) return file.fileName.trim();

  const lastS3Segment = file.s3Key?.split('/').at(-1)?.trim();
  if (lastS3Segment) {
    try {
      return decodeURIComponent(lastS3Segment);
    } catch {
      return lastS3Segment;
    }
  }

  return file.fileId?.trim() || '登録済みファイル';
}

type YesNoSelectProps = Readonly<{
  value: '' | 'yes' | 'no';
  onValueChange: (value: 'yes' | 'no') => void;
}>;

function YesNoSelect({ value, onValueChange }: YesNoSelectProps) {
  return (
    <Select
      value={value}
      onValueChange={(nextValue) =>
        onValueChange(nextValue as 'yes' | 'no')
      }
    >
      <SelectTrigger>
        <SelectValue placeholder="選択してください" />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value="yes">あり</SelectItem>
        <SelectItem value="no">なし</SelectItem>
      </SelectContent>
    </Select>
  );
}

export function SolarPowerEquipmentForm(props: SolarPowerEquipmentFormProps) {
  const { form } = props;
  const isEditMode = props.mode === 'edit';
  const existingFiles = isEditMode ? props.existingFiles ?? [] : [];
  const sites = isEditMode ? [] : props.sites;
  const isSiteOptionsLoading = isEditMode
    ? false
    : props.isSiteOptionsLoading;
  const siteOptionsError = isEditMode ? null : props.siteOptionsError;
  const onAddSite = isEditMode ? undefined : props.onAddSite;
  const hasBattery = form.watch('hasBattery') === 'yes';
  const hasSubsidy = form.watch('hasSubsidy') === 'yes';

  return (
    <FormSectionStack spacing="relaxed">
      {!isEditMode && (
        <FormField
          control={form.control}
          name="siteId"
          render={({ field }) => (
            <FormItem>
              <RequiredLabel>対象拠点</RequiredLabel>
              <FormDescription className="text-foreground">
                プロジェクトへの参加を申請する拠点を選択してください。
              </FormDescription>
              <Select
                value={field.value}
                onValueChange={(value) => {
                  if (value === ADD_SITE_OPTION_VALUE) {
                    onAddSite?.(form.getValues());
                    return;
                  }

                  field.onChange(value);
                }}
                disabled={
                  !onAddSite &&
                  (isSiteOptionsLoading ||
                    Boolean(siteOptionsError) ||
                    sites.length === 0)
                }
              >
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="拠点を選択してください" />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  {sites.map((site) => (
                    <SelectItem key={site.siteId} value={site.siteId}>
                      {site.siteName}
                    </SelectItem>
                  ))}
                  {onAddSite && (
                    <>
                      {sites.length > 0 && <SelectSeparator />}
                      <SelectItem
                        value={ADD_SITE_OPTION_VALUE}
                        className="font-medium text-primary focus:text-primary"
                      >
                        <span className="flex items-center gap-2">
                          <Plus className="h-4 w-4" aria-hidden="true" />
                          拠点を追加
                        </span>
                      </SelectItem>
                    </>
                  )}
                </SelectContent>
              </Select>
              {siteOptionsError && (
                <p className="text-sm text-destructive">{siteOptionsError}</p>
              )}
              <FormMessage />
            </FormItem>
          )}
        />
      )}

      <FormSection number={1} title="太陽光発電設備の情報">
        {isEditMode ? (
          <FormField
            control={form.control}
            name="changeDate"
            render={({ field }) => (
              <FormItem>
                <RequiredLabel>変更日</RequiredLabel>
                <FormDescription className="text-foreground">
                  変更後の設備のモニタリング開始日になります。
                </FormDescription>
                <FormControl>
                  <Input type="date" {...field} value={field.value ?? ''} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        ) : (
          <FormField
            control={form.control}
            name="equipmentOpDate"
            render={({ field }) => (
              <FormItem>
                <RequiredLabel>稼働開始日（電力受給開始日）</RequiredLabel>
                <FormControl>
                  <Input type="date" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        )}
        <FormField
          control={form.control}
          name="operationStartDateFile"
          render={({ field }) => (
            <FormItem>
              <FormControl>
                <FileUploadControl
                  id="equipment-op-date-file"
                  fieldName="operationStartDateFile"
                  form={form}
                  label="太陽光発電所・発電設備を特定できる資料"
                  value={field.value}
                  onChange={field.onChange}
                  required
                  existingFile={getExistingFile(existingFiles, 'solar_contract')}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      </FormSection>

      <FormSection number={2} title="太陽光パネルの情報">
        <FormField
          control={form.control}
          name="nominalMaxOutput"
          render={({ field }) => (
            <FormItem>
              <RequiredLabel>公称最大出力 (kW)</RequiredLabel>
              <FormControl>
                <Input
                  type="number"
                  min="0"
                  step="any"
                  placeholder="数値を入力"
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="nominalMaxOutputFile"
          render={({ field }) => (
            <FormItem>
              <FormControl>
                <FileUploadControl
                  id="nominal-max-output-file"
                  fieldName="nominalMaxOutputFile"
                  form={form}
                  label="太陽光パネルの仕様書・カタログ"
                  value={field.value}
                  onChange={field.onChange}
                  required
                  existingFile={getExistingFile(existingFiles, 'panel_catalog')}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      </FormSection>

      <FormSection number={3} title="パワーコンディショナーの情報">
        <FormGrid columns={1}>
          <FormField
            control={form.control}
            name="pcsManufacturer"
            render={({ field }) => (
              <FormItem>
                <RequiredLabel>メーカー</RequiredLabel>
                <FormControl>
                  <Input placeholder="メーカー名を入力" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="pcsModel"
            render={({ field }) => (
              <FormItem>
                <RequiredLabel>型式</RequiredLabel>
                <FormControl>
                  <Input placeholder="型式を入力" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="deviceNo"
            render={({ field }) => (
              <FormItem>
                <RequiredLabel>機器固有番号（製造番号）</RequiredLabel>
                <FormControl>
                  <Input placeholder="機器固有番号を入力" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="powerCondStandby"
            render={({ field }) => (
              <FormItem>
                <RequiredLabel>
                  夜間待機電力 (W)
                </RequiredLabel>
                <FormControl>
                  <Input
                    type="number"
                    min="0"
                    step="any"
                    placeholder="数値を入力"
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </FormGrid>
        <FormField
          control={form.control}
          name="pcsFile"
          render={({ field }) => (
            <FormItem>
              <FormControl>
                <FileUploadControl
                  id="pcs-file"
                  fieldName="pcsFile"
                  form={form}
                  label="パワーコンディショナーの仕様書"
                  value={field.value}
                  onChange={field.onChange}
                  required
                  existingFile={getExistingFile(existingFiles, 'pcs_spec')}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      </FormSection>

      <FormSection number={4} title="蓄電池の情報">
        <FormField
          control={form.control}
          name="hasBattery"
          render={({ field }) => (
            <FormItem>
              <RequiredLabel>蓄電池設置の有無</RequiredLabel>
              <FormControl>
                <YesNoSelect
                  value={field.value}
                  onValueChange={field.onChange}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        {hasBattery && (
          <div className="space-y-4">
            <FormGrid columns={1}>
              <FormField
                control={form.control}
                name="batteryMaker"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>メーカー</RequiredLabel>
                    <FormControl>
                      <Input placeholder="メーカー名を入力" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="batteryModel"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>型式</RequiredLabel>
                    <FormControl>
                      <Input placeholder="型式を入力" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="batteryCapacity"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>容量 (kWh)</RequiredLabel>
                    <FormControl>
                      <Input
                        type="number"
                        min="0"
                        step="any"
                        placeholder="数値を入力"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </FormGrid>
            <FormField
              control={form.control}
              name="batteryFile"
              render={({ field }) => (
                <FormItem>
                  <FormControl>
                    <FileUploadControl
                      id="battery-file"
                      fieldName="batteryFile"
                      form={form}
                      label="蓄電池の仕様書・カタログ"
                      value={field.value}
                      onChange={field.onChange}
                      required
                      existingFile={getExistingFile(existingFiles, 'battery_catalog')}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>
        )}
      </FormSection>

      <FormSection number={5} title="追加性評価に必要な情報">
        <FormField
          control={form.control}
          name="totalInvestment"
          render={({ field }) => (
            <FormItem>
              <RequiredLabel>設備の総投資額 (円)</RequiredLabel>
              <FormControl>
                <Input
                  type="number"
                  min="0"
                  step="1"
                  placeholder="数値を入力"
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="totalInvestmentFile"
          render={({ field }) => (
            <FormItem>
              <FormControl>
                <FileUploadControl
                  id="total-investment-file"
                  fieldName="totalInvestmentFile"
                  form={form}
                  label="設備の総投資額を確認できる資料"
                  labelDescription="例：工事請負契約書、請求書、領収書など"
                  value={field.value}
                  onChange={field.onChange}
                  required
                  existingFile={getExistingFile(existingFiles, 'construction_invoice')}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="hasSubsidy"
          render={({ field }) => (
            <FormItem>
              <RequiredLabel>補助金受給の有無</RequiredLabel>
              <FormControl>
                <YesNoSelect
                  value={field.value}
                  onValueChange={field.onChange}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        {hasSubsidy && (
          <div className="space-y-4">
            <FormGrid columns={1}>
              <FormField
                control={form.control}
                name="subsidyName"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>補助金名称</RequiredLabel>
                    <FormControl>
                      <Input placeholder="補助金名称を入力" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="subsidyAmount"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>補助金額 (円)</RequiredLabel>
                    <FormControl>
                      <Input
                        type="number"
                        min="0"
                        step="any"
                        placeholder="数値を入力"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </FormGrid>
            <FormField
              control={form.control}
              name="subsidyNameFile"
              render={({ field }) => (
                <FormItem>
                  <FormControl>
                    <FileUploadControl
                      id="subsidy-name-file"
                      fieldName="subsidyNameFile"
                      form={form}
                      label="補助金交付決定を確認できる資料"
                      labelDescription="例：補助金交付決定通知書"
                      value={field.value}
                      onChange={field.onChange}
                      required
                      existingFile={getExistingFile(existingFiles, 'subsidy_notice')}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>
        )}

        <FormField
          control={form.control}
          name="preProjectUnitPrice"
          render={({ field }) => (
            <FormItem>
              <RequiredLabel>
                プロジェクト実施前の電力購入単価 (円/kWh)
              </RequiredLabel>
              <FormControl>
                <Input
                  type="number"
                  min="0"
                  step="any"
                  placeholder="数値を入力"
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="preProjectUnitPriceFile"
          render={({ field }) => (
            <FormItem>
              <FormControl>
                <FileUploadControl
                  id="pre-project-unit-price-file"
                  fieldName="preProjectUnitPriceFile"
                  form={form}
                  label="電力購入単価を確認できる資料"
                  labelDescription="例：電力会社の請求明細書など"
                  value={field.value}
                  onChange={field.onChange}
                  required
                  existingFile={getExistingFile(existingFiles, 'electricity_bill')}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      </FormSection>
    </FormSectionStack>
  );
}
